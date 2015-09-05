/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coopox.DrivingRecorder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Surface;
import com.coopox.common.storage.ExternalStorage;
import com.coopox.common.storage.Storage;
import com.coopox.common.storage.StorageUtils;
import com.coopox.common.utils.Checker;
import com.coopox.common.utils.ThreadManager;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Encodes video in a fixed-size circular buffer.
 * <p>
 * The obvious way to do this would be to store each packet in its own buffer and hook it
 * into a linked list.  The trouble with this approach is that it requires constant
 * allocation, which means we'll be driving the GC to distraction as the frame rate and
 * bit rate increase.  Instead we create fixed-size pools for video data and metadata,
 * which requires a bit more work for us but avoids allocations in the steady state.
 * <p>
 * Video must always start with a sync frame (a/k/a key frame, a/k/a I-frame).  When the
 * circular buffer wraps around, we either need to delete all of the data between the frame at
 * the head of the list and the next sync frame, or have the file save function know that
 * it needs to scan forward for a sync frame before it can start saving data.
 * <p>
 * When we're told to save a snapshot, we create a MediaMuxer, write all the frames out,
 * and then go back to what we were doing.
 */
public class CircularEncoder {
    private static final String TAG = "CircularEncoder";
    private static final boolean VERBOSE = true;

    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int IFRAME_INTERVAL = 1;           // sync frame every second

    private EncoderThread mEncoderThread;
    private Surface mInputSurface;
    private MediaCodec mEncoder;

    /**
     * Callback function definitions.  CircularEncoder caller must provide one.
     */
    public interface Callback {
        /**
         * Called some time after saveVideo(), when all data has been written to the
         * output file.
         *
         * @param status Zero means success, nonzero indicates failure.
         */
        void fileSaveComplete(int status);

        /**
         * Called occasionally.
         *
         * @param totalTimeMsec Total length, in milliseconds, of buffered video.
         */
        void bufferStatus(long totalTimeMsec);
    }

    /**
     * Configures encoder, and prepares the input Surface.
     *
     * @param width Width of encoded video, in pixels.  Should be a multiple of 16.
     * @param height Height of encoded video, in pixels.  Usually a multiple of 16 (1080 is ok).
     * @param bitRate Target bit rate, in bits.
     * @param frameRate Expected frame rate.
     * @param desiredSpanSec How many seconds of video we want to have in our buffer at any time.
     */
    public CircularEncoder(int width, int height, int bitRate, int frameRate, int desiredSpanSec,
            Callback cb) throws IOException {
        // The goal is to size the buffer so that we can accumulate N seconds worth of video,
        // where N is passed in as "desiredSpanSec".  If the codec generates data at roughly
        // the requested bit rate, we can compute it as time * bitRate / bitsPerByte.
        //
        // Sync frames will appear every (frameRate * IFRAME_INTERVAL) frames.  If the frame
        // rate is higher or lower than expected, various calculations may not work out right.
        //
        // Since we have to start muxing from a sync frame, we want to ensure that there's
        // room for at least one full GOP in the buffer, preferrably two.
        if (desiredSpanSec < IFRAME_INTERVAL * 2) {
            throw new RuntimeException("Requested time span is too short: " + desiredSpanSec +
                    " vs. " + (IFRAME_INTERVAL * 2));
        }
        CircularEncoderBuffer[] encBuffers = new CircularEncoderBuffer[2];
        encBuffers[0] = new CircularEncoderBuffer(bitRate, frameRate, desiredSpanSec);
        encBuffers[1] = new CircularEncoderBuffer(bitRate, frameRate, desiredSpanSec);

        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);

        // Set some properties.  Failing to specify some of these can cause the MediaCodec
        // configure() call to throw an unhelpful exception.
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        if (VERBOSE) Log.d(TAG, "format: " + format);

        // Create a MediaCodec encoder, and configure it with our format.  Get a Surface
        // we can use for input and wrap it with a class that handles the EGL work.
        mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mInputSurface = mEncoder.createInputSurface();
        mEncoder.start();

        // Start the encoder thread last.  That way we're sure it can see all of the state
        // we've initialized.
        mEncoderThread = new EncoderThread(mEncoder, encBuffers, cb);
        mEncoderThread.start();
        mEncoderThread.waitUntilReady();
    }

    /**
     * Returns the encoder's input surface.
     */
    public Surface getInputSurface() {
        return mInputSurface;
    }

    /**
     * Shuts down the encoder thread, and releases encoder resources.
     * <p>
     * Does not return until the encoder thread has stopped.
     */
    public void shutdown() {
        if (VERBOSE) Log.d(TAG, "releasing encoder objects");

        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(EncoderThread.EncoderHandler.MSG_SHUTDOWN));
        try {
            mEncoderThread.join();
        } catch (InterruptedException ie) {
            Log.w(TAG, "Encoder thread join() was interrupted", ie);
        }

        if (mEncoder != null) {
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }

    /**
     * Notifies the encoder thread that a new frame will shortly be provided to the encoder.
     * <p>
     * There may or may not yet be data available from the encoder output.  The encoder
     * has a fair mount of latency due to processing, and it may want to accumulate a
     * few additional buffers before producing output.  We just need to drain it regularly
     * to avoid a situation where the producer gets wedged up because there's no room for
     * additional frames.
     * <p>
     * If the caller sends the frame and then notifies us, it could get wedged up.  If it
     * notifies us first and then sends the frame, we guarantee that the output buffers
     * were emptied, and it will be impossible for a single additional frame to block
     * indefinitely.
     */
    public void frameAvailableSoon() {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(
                EncoderThread.EncoderHandler.MSG_FRAME_AVAILABLE_SOON));
    }

    /**
     * Initiates saving the currently-buffered frames to the specified output file.  The
     * data will be written as a .mp4 file.  The call returns immediately.  When the file
     * save completes, the callback will be notified.
     * <p>
     * The file generation is performed on the encoder thread, which means we won't be
     * draining the output buffers while this runs.  It would be wise to stop submitting
     * frames during this time.
     */
    public void saveVideo(File outputFile) {
        Handler handler = mEncoderThread.getHandler();
        handler.sendMessage(handler.obtainMessage(
                EncoderThread.EncoderHandler.MSG_SAVE_VIDEO, outputFile));
    }

    /**
     * Object that encapsulates the encoder thread.
     * <p>
     * We want to sleep until there's work to do.  We don't actually know when a new frame
     * arrives at the encoder, because the other thread is sending frames directly to the
     * input surface.  We will see data appear at the decoder output, so we can either use
     * an infinite timeout on dequeueOutputBuffer() or wait() on an object and require the
     * calling app wake us.  It's very useful to have all of the buffer management local to
     * this thread -- avoids synchronization -- so we want to do the file muxing in here.
     * So, it's best to sleep on an object and do something appropriate when awakened.
     * <p>
     * This class does not manage the MediaCodec encoder startup/shutdown.  The encoder
     * should be fully started before the thread is created, and not shut down until this
     * thread has been joined.
     */
    private static class EncoderThread extends Thread {
        private static final String DRIVING_RECORD_ALL_VIDEO = "DrivingRecord/AllVideo";
        private static final String DATE_FORMAT = "yyyyMMdd_kk-mm-ss";
        private static final String VIDEO_FILE_FORMAT = "%s.mp4";
        private static final long MAX_FILE_SIZE = 50 * 1024L * 1024L;
        private static final long MINIMIZE_STORAGE_AVAILABLE_SIZE = 1 * 1024L * 1024L * 1024L;

        private MediaCodec mEncoder;
        private MediaFormat mEncodedFormat;
        private MediaCodec.BufferInfo mBufferInfo;

        private EncoderHandler mHandler;
        // 多个编码缓冲进行分段编码，解决分段视频之间可能丢帧的问题。
        private CircularEncoderBuffer[] mEncBuffers;
        private CircularEncoderBuffer mCurBuffer;
        private CircularEncoder.Callback mCallback;
        private int mFrameNum;

        private final Object mLock = new Object();
        private volatile boolean mReady = false;

        public EncoderThread(MediaCodec mediaCodec, CircularEncoderBuffer[] encBuffers,
                CircularEncoder.Callback callback) {
            if (Checker.isEmpty(encBuffers)) {
                throw new IllegalArgumentException("Encode Buffer can't be empty!");
            }
            mEncoder = mediaCodec;
            mEncBuffers = encBuffers;
            mCurBuffer = encBuffers[0];
            mCallback = callback;

            mBufferInfo = new MediaCodec.BufferInfo();
        }

        /**
         * Thread entry point.
         * <p>
         * Prepares the Looper, Handler, and signals anybody watching that we're ready to go.
         */
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new EncoderHandler(this);    // must create on encoder thread
            Log.d(TAG, "encoder thread ready");
            synchronized (mLock) {
                mReady = true;
                mLock.notify();    // signal waitUntilReady()
            }

            Looper.loop();

            synchronized (mLock) {
                mReady = false;
                mHandler = null;
            }
            Log.d(TAG, "looper quit");
        }

        /**
         * Waits until the encoder thread is ready to receive messages.
         * <p>
         * Call from non-encoder thread.
         */
        public void waitUntilReady() {
            synchronized (mLock) {
                while (!mReady) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ie) { /* not expected */ }
                }
            }
        }

        /**
         * Returns the Handler used to send messages to the encoder thread.
         */
        public EncoderHandler getHandler() {
            synchronized (mLock) {
                // Confirm ready state.
                if (!mReady) {
                    throw new RuntimeException("not ready");
                }
            }
            return mHandler;
        }

        /**
         * Drains all pending output from the decoder, and adds it to the circular buffer.
         */
        public void drainEncoder() {
            final int TIMEOUT_USEC = 0;     // no timeout -- check for buffers, bail if none

            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
            while (true) {
                int encoderStatus = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    break;
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not expected for an encoder
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    // Should happen before receiving buffers, and should only happen once.
                    // The MediaFormat contains the csd-0 and csd-1 keys, which we'll need
                    // for MediaMuxer.  It's unclear what else MediaMuxer might want, so
                    // rather than extract the codec-specific data and reconstruct a new
                    // MediaFormat later, we just grab it here and keep it around.
                    mEncodedFormat = mEncoder.getOutputFormat();
                    Log.d(TAG, "encoder output format changed: " + mEncodedFormat);
                } else if (encoderStatus < 0) {
                    Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
                            encoderStatus);
                    // let's ignore it
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + encoderStatus +
                                " was null");
                    }

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // The codec config data was pulled out when we got the
                        // INFO_OUTPUT_FORMAT_CHANGED status.  The MediaMuxer won't accept
                        // a single big blob -- it wants separate csd-0/csd-1 chunks --
                        // so simply saving this off won't work.
                        if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                        mBufferInfo.size = 0;
                    }

                    if (mBufferInfo.size != 0) {
                        // adjust the ByteBuffer values to match BufferInfo (not needed?)
                        encodedData.position(mBufferInfo.offset);
                        encodedData.limit(mBufferInfo.offset + mBufferInfo.size);

                        int size = encodedData.limit() - encodedData.position();
                        if (mCurBuffer.canAdd(size)) {
                            mCurBuffer.add(encodedData, mBufferInfo.flags,
                                    mBufferInfo.presentationTimeUs);
                        } else {
                            final CircularEncoderBuffer lastBuf = mCurBuffer;
                            mCurBuffer = (mCurBuffer == mEncBuffers[0] ? mEncBuffers[1] : mEncBuffers[0]);

                            /* ====== 将已满缓冲里的最后一个同步帧之后的内容移动到空缓冲以避免丢帧 ===== */
                            int firstIndex = lastBuf.getFirstIndex();
                            int lastIndex = lastBuf.getLastIndex();
                            if (VERBOSE) {
                                Log.d(TAG, String.format("First index = %d, Last index = %d", firstIndex, lastIndex));
                            }
                            if (lastIndex < 0) {
                                throw new RuntimeException("It hasn't any sync frame, may be the video is too short!");
                            }

                            int index = lastIndex;
                            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                            do {
                                ByteBuffer chunk = lastBuf.getChunk(index, info);
                                mCurBuffer.add(chunk, info);
                                if (VERBOSE) {
                                    Log.d(TAG, "COPY " + index + " flags=0x" + Integer.toHexString(info.flags));
                                }
                                index = lastBuf.getNextIndex(index);
                            } while (index >= 0);
                            lastBuf.setHeadIndex(lastIndex);
                            /* ======= 移动完毕，可以保存老缓冲和填充新缓冲了 ======= */

                            ThreadManager.INSTANCE.runOnWorkerThread(new Runnable() {
                                @Override
                                public void run() {
                                    saveVideo(lastBuf);
                                }
                            });
                            mCurBuffer.add(encodedData, mBufferInfo.flags,
                                    mBufferInfo.presentationTimeUs);
                        }

                        if (VERBOSE) {
//                            Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer, ts=" +
//                                    mBufferInfo.presentationTimeUs);
                        }
                    }

                    mEncoder.releaseOutputBuffer(encoderStatus, false);

                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                        break;      // out of while
                    }
                }
            }
        }

        /**
         * Drains the encoder output.
         * <p>
         * See notes for {@link CircularEncoder#frameAvailableSoon()}.
         */
        void frameAvailableSoon() {
            if (VERBOSE) Log.d(TAG, "frameAvailableSoon");
            drainEncoder();

            mFrameNum++;
            if ((mFrameNum % 10) == 0) {        // TODO: should base off frame rate or clock?
                mCallback.bufferStatus(mCurBuffer.computeTimeSpanUsec());
            }
        }

        /**
         * Saves the encoder output to a .mp4 file.
         * <p>
         * We'll drain the encoder to get any lingering data, but we're not going to shut
         * the encoder down or use other tricks to try to "flush" the encoder.  This may
         * mean we miss the last couple of submitted frames if they're still working their
         * way through.
         * <p>
         * We may want to reset the buffer after this -- if they hit "capture" again right
         * away they'll end up saving video with a gap where we paused to write the file.
         */
        void saveVideo(CircularEncoderBuffer buffer) {
            String filename = String.format(VIDEO_FILE_FORMAT,
                    DateFormat.format(DATE_FORMAT, new Date().getTime()));
            Storage storage = new ExternalStorage(DRIVING_RECORD_ALL_VIDEO, filename);
            File outputFile = storage.getFile();
            if (null == outputFile) { // 外部存储设备未挂载
                mCallback.fileSaveComplete(3);
                return;
            }

            if (!cleanStorageIfNeeded()) {
                mCallback.fileSaveComplete(4);
                return;
            }

            if (VERBOSE) Log.d(TAG, "saveVideo " + outputFile);

            int index = buffer.getFirstIndex();
            if (index < 0) {
                Log.w(TAG, "Unable to get first index");
                mCallback.fileSaveComplete(1);
                return;
            }

            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            MediaMuxer muxer = null;
            int result = -1;
            try {
                muxer = new MediaMuxer(outputFile.getPath(),
                        MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                int videoTrack = muxer.addTrack(mEncodedFormat);
                muxer.start();

                do {
                    ByteBuffer buf = buffer.getChunk(index, info);
                    if (VERBOSE) {
                        Log.d(TAG, "SAVE " + index + " flags=0x" + Integer.toHexString(info.flags));
                    }
                    muxer.writeSampleData(videoTrack, buf, info);
                    index = buffer.getNextIndex(index);
                } while (index >= 0);
                result = 0;
            } catch (IOException ioe) {
                Log.w(TAG, "muxer failed", ioe);
                result = 2;
            } finally {
                if (muxer != null) {
                    muxer.stop();
                    muxer.release();
                }
                buffer.clear();
            }

            if (VERBOSE) {
                Log.d(TAG, "muxer stopped, result=" + result);
            }
            mCallback.fileSaveComplete(result);
        }

        private boolean cleanStorageIfNeeded() {
            long freeSize = StorageUtils.getAvailableExternalStorageSize();
            // 最小可用空间等于最小预留空间加一个视频的最大空间，这样录制完下一个视频后才会不超过
            // 最小预留空间。
            long mini_storage_available_size = MINIMIZE_STORAGE_AVAILABLE_SIZE + MAX_FILE_SIZE;
            if (freeSize < mini_storage_available_size) {
                long cleanSize = mini_storage_available_size - freeSize;
                Storage storage = new ExternalStorage(DRIVING_RECORD_ALL_VIDEO, "");
                // 如果存储空间不足，则清除最早的视频文件
                // 若释放的空间不够，返回 false
                if (cleanOldestFile(storage.getFile(), cleanSize) < cleanSize) {
                    return false;
                }
            }
            return true;
        }

        /**
         * 清理最老的文件，至少减少 cleanSize 字节的空间占用
         * @return 实际清理的空间大小 */
        private long cleanOldestFile(File dir, long cleanSize) {
            if (null != dir && dir.isDirectory()) {
                File[] allVideoFiles = dir.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        String fileName = pathname.getName();
                        String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
                        return pathname.isFile() && prefix.equals("mp4");
                    }
                });

                if (!Checker.isEmpty(allVideoFiles)) {
                    List<File> allVideoList = Arrays.asList(allVideoFiles);
                    Collections.sort(allVideoList, new Comparator<File>() {
                        @Override
                        public int compare(File lhs, File rhs) {
                            return lhs.getName().compareToIgnoreCase(rhs.getName());
                        }
                    });

                    long sizeCount = 0;
                    for (File videoFile : allVideoList) {
                        long size = videoFile.length();
                        if (videoFile.delete()) {
                            sizeCount += size;
                        }

                        if (sizeCount >= cleanSize) {
                            break;
                        }
                    }
                    return sizeCount;
                }
            }
            return 0;
        }

        /**
         * Tells the Looper to quit.
         */
        void shutdown() {
            if (VERBOSE) Log.d(TAG, "shutdown");
            Looper.myLooper().quit();
        }

        /**
         * Handler for EncoderThread.  Used for messages sent from the UI thread (or whatever
         * is driving the encoder) to the encoder thread.
         * <p>
         * The object is created on the encoder thread.
         */
        private static class EncoderHandler extends Handler {
            public static final int MSG_FRAME_AVAILABLE_SOON = 1;
            public static final int MSG_SAVE_VIDEO = 2;
            public static final int MSG_SHUTDOWN = 3;

            // This shouldn't need to be a weak ref, since we'll go away when the Looper quits,
            // but no real harm in it.
            private WeakReference<EncoderThread> mWeakEncoderThread;

            /**
             * Constructor.  Instantiate object from encoder thread.
             */
            public EncoderHandler(EncoderThread et) {
                mWeakEncoderThread = new WeakReference<EncoderThread>(et);
            }

            @Override  // runs on encoder thread
            public void handleMessage(Message msg) {
                int what = msg.what;
                if (VERBOSE) {
                    Log.v(TAG, "EncoderHandler: what=" + what);
                }

                EncoderThread encoderThread = mWeakEncoderThread.get();
                if (encoderThread == null) {
                    Log.w(TAG, "EncoderHandler.handleMessage: weak ref is null");
                    return;
                }

                switch (what) {
                    case MSG_FRAME_AVAILABLE_SOON:
                        encoderThread.frameAvailableSoon();
                        break;
                    case MSG_SHUTDOWN:
                        encoderThread.shutdown();
                        break;
                    default:
                        throw new RuntimeException("unknown message " + what);
                }
            }
        }
    }
}
