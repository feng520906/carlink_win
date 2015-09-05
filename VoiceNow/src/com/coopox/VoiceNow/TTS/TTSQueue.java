package com.coopox.VoiceNow.TTS;

import android.content.Context;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import com.coopox.common.Constants;
import com.coopox.common.tts.*;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-30
 * 语音合成管理器，目前简单得实现了语音队列，将来可能以通过优先级队列来优化。
 */
public class TTSQueue implements TTSRawListener {
    private static final String TAG = "TTSQueue";

    static class TTSTask {
        public TTSTask(String text, ResultReceiver resultReceiver) {
            mText = text;
            mReceiver = resultReceiver;
        }

        public String mText;
        public ResultReceiver mReceiver;
    }

    // 队列最大长度
    private static final int DEFAULT_LENGTH = 32;

    private int mLength = DEFAULT_LENGTH;
    ArrayList<TTSTask> mQueue = new ArrayList<TTSTask>();
    TTSTask mCurrentTask;
    private TTSPlayer mTTSPlayer;
    private WeakReference<TTSListener> mListenerRef = new WeakReference<TTSListener>(null);

    public TTSQueue(Context context) {
        setup(context);
    }

    public void setup(Context context) {
        mTTSPlayer = new HiVoiceOfflinePlayer();
        mTTSPlayer.setup(context);
        mTTSPlayer.setTTSListener(this);
    }

    public void teardown() {
        mTTSPlayer.teardown();
    }

    public void setListener(TTSListener listener) {
        mListenerRef = new WeakReference<TTSListener>(listener);
    }

    public boolean enqueueText(String text) {
        return enqueueTextToTail(text, null);
    }

    public boolean enqueueTextToTail(String text, ResultReceiver resultReceiver) {
        if (null == mTTSPlayer) {
            Log.e(TAG, "Please setup TTSManager first!");
            return false;
        }

        if (!TextUtils.isEmpty(text)) {
            if (null == mCurrentTask) {
                mCurrentTask = new TTSTask(text, resultReceiver);
                if (!mTTSPlayer.speak(text)) {
                    mCurrentTask = null;
                }
                Log.d(TAG, String.format("TTS queue is empty, play %s directly", text));
                return true;
            } else if (mQueue.size() < mLength) {
                mQueue.add(new TTSTask(text, resultReceiver));
                Log.d(TAG, String.format("Add %s to tail of TTS queue", text));
                return true;
            }
        }
        return false;
    }

    public boolean enqueueTextToHead(String text, ResultReceiver resultReceiver) {
        if (null == mTTSPlayer) {
            Log.e(TAG, "Please setup TTSManager first!");
            return false;
        }

        if (!TextUtils.isEmpty(text)) {
            if (null == mCurrentTask) {
                mCurrentTask = new TTSTask(text, resultReceiver);
                if (!mTTSPlayer.speak(text)) {
                    mCurrentTask = null;
                }
                Log.d(TAG, String.format("TTS queue is empty, play %s directly", text));
                return true;
            } else if (mQueue.size() < mLength) {
                mQueue.add(0, new TTSTask(text, resultReceiver));
                Log.d(TAG, String.format("Add %s to head of TTS queue", text));
                return true;
            }
        }
        return false;
    }

    // 删除第一个 text 对象
    public void removeText(String text) {
        if (null != text) {
            mQueue.remove(text);
        }
    }

    public void removeAll() {
        Log.d(TAG, "Clear TTS queue");
        mQueue.clear();
    }

    public void setQueueMaxLength(int length) {
        mLength = length;
    }

    // 暂停文本朗读，如果没有调用speak(String)方法或者合成器初始化失败，该方法将无任何效果
    public void pauseTTS() {
        mTTSPlayer.pause();
        Log.d(TAG, "Pause TTS speech");
    }

    // 继续文本朗读，如果没有调用speak(String)方法或者合成器初始化失败，该方法将无任何效果
    public void resumeTTS() {
        mTTSPlayer.resume();
        Log.d(TAG, "Resume TTS speech");
    }

    public void cancelAll() {
        removeAll();
        cancel();
    }

    // 取消本次合成、停止朗读，开始下一条TTS合成朗读
    public void cancel() {
        mTTSPlayer.cancel();
        Log.d(TAG, "Cancel TTS speech");
    }

    public boolean isSpeaking() {
        return mTTSPlayer.isSpeaking();
    }


    @Override
    public void onSpeechStart() {
        Log.i("TTSManager", "Speech Start");
        sendMsgToClient(Constants.MSG_TTS_START);
        TTSListener listener = mListenerRef.get();
        if (null != listener) {
            listener.onSpeechStart(null != mCurrentTask ? mCurrentTask.mText : null);
        }
    }

    @Override
    public void onSpeechPause() {
        sendMsgToClient(Constants.MSG_TTS_PAUSED);
        TTSListener listener = mListenerRef.get();
        if (null != listener) {
            listener.onSpeechPause(null != mCurrentTask ? mCurrentTask.mText : null);
        }
    }

    @Override
    public void onSpeechResume() {
        sendMsgToClient(Constants.MSG_TTS_RESUMED);
        TTSListener listener = mListenerRef.get();
        if (null != listener) {
            listener.onSpeechResume(null != mCurrentTask ? mCurrentTask.mText : null);
        }
    }

    @Override
    public void onSpeechStop() {
        Log.i("TTSManager", "Speech Finish");
        boolean allClear = mQueue.isEmpty();
        sendMsgToClient(Constants.MSG_TTS_STOPPED, allClear ? 1 : 0);
        TTSListener listener = mListenerRef.get();
        if (null != listener) {
            listener.onSpeechStop(null != mCurrentTask ? mCurrentTask.mText : null, allClear);
        }
        if (mQueue.isEmpty()) {
            mCurrentTask = null;
        } else {
            mCurrentTask = mQueue.remove(0);
            if (!mTTSPlayer.speak(mCurrentTask.mText)) {
                mCurrentTask = null;
            }
        }
    }

    @Override
    public void onSpeechError( int errorCode) {
        sendMsgToClient(Constants.MSG_TTS_ERROR, errorCode);
        cancel();

        TTSListener listener = mListenerRef.get();
        if (null != listener) {
            listener.onSpeechError(null != mCurrentTask ? mCurrentTask.mText : null, errorCode);
        }
        Log.e(TAG, "TTS speech error: " + errorCode);
    }

    private void sendMsgToClient(int msgID) {
        sendMsgToClient(msgID, 0);
    }

    private void sendMsgToClient(int msgID, int errCode) {
        if (null != mCurrentTask && null != mCurrentTask.mReceiver) {
            Bundle bundle = new Bundle();
            bundle.putString(Constants.KEY_TTS_CONTENT, mCurrentTask.mText);
            if (0 != errCode) {
                bundle.putInt(Constants.KEY_TTS_ERROR, errCode);
            }
            mCurrentTask.mReceiver.send(msgID, null);
        }
    }
}
