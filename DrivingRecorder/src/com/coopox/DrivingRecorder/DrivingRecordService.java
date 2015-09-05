package com.coopox.DrivingRecorder;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.coopox.common.Constants;
import com.coopox.common.storage.ExternalTFCardStorage;
import com.coopox.common.storage.Storage;
import com.coopox.common.tts.TTSClient;
import com.coopox.common.utils.AppUtils;
import com.coopox.common.utils.Checker;
import com.coopox.common.utils.StreamUtils;
import com.coopox.common.utils.ThreadManager;
import com.coopox.hwmsersor.DwmSensorEvent;
import com.coopox.hwmsersor.DwmSensorEvent.OnExBrakListener;
import com.coopox.net.UpFileAsyncTask;
import com.coopox.receiver.DrivingRecorderReceiver;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-13
 */
public class DrivingRecordService extends Service implements SurfaceHolder.Callback, MediaRecorder.OnErrorListener ,MediaRecorder.OnInfoListener, Camera.PictureCallback, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "DrvingRecordService";

    public static final String ACTION_TAKE_PHOTO = "com.coopox.DrivingRecorder.TakePicture";
    public static final String EXTRA_TAKE_PHOTO = "extra_take_photo";
    public static final String EXTRA_FULL_SCREEN = "extra_fullscreen";
    public static final String EXTRA_MINIMIZE = "extra_minimize";
    public static final String EXTRA_AUTO_FOCUS = "extra_auto_focus";
    public static final String EXTRA_CONFIG_CHANGED = "ConfigChanged";
	public static final String EXTRA_START_RECORD = "extra_start_record";
	public static final String EXTRA_STOP_RECORD = "extra_stop_record";
	public static final int cameraIdZero = 0;
	public static final int cameraIdOne = 1;
	public int mCurCameraId = 0;
//EXTRA_START_RECORD
    private static final long MAX_FILE_SIZE = 200 * 1024L * 1024L;
    public static final String DRIVING_RECORD_ALL_VIDEO = "DrivingRecord/AllVideo";
    public static final String DRIVING_RECORD_PICTURES = "DrivingRecord/Pictures";
    public static final String DATE_FORMAT = "yyyyMMdd_kk-mm-ss";
    public static final String VIDEO_FILE_FORMAT = "%s.mp4";
    private static final long MINIMIZE_STORAGE_AVAILABLE_SIZE = 1 * 1024L * 1024L * 1024L;
    public static final int CAPTURE_RATE = 30;
    public static final int VIDEO_FRAME_RATE = 30;
    public static final String ACTION_PREVIEW_MINIMIZE =
            "com.coopox.DrivingRecord.preview.minimize";
    private WindowManager mWindowManager;
    private SurfaceView mSurfaceView;
    private Camera mCamera = null;
    private MediaRecorder mMediaRecorder = null;
    private SurfaceHolder mSurfaceHolder;
    private boolean mCanTakePhotoWhenRecording;
    private boolean mSupportAutoFocus;
    private File mVideoFile;
    private File mExceptionFile = null;
    private View mRecordView;
    private boolean mIsRecording;
    
    private BrakingChecker mBrakingChecker;
    private DwmSensorEvent mDwmSensorEvent;
    
    private boolean mLockCurrentVideo = false;
    private boolean mExceptionVideo = false;
    private Map<String, Object> mPreferences;

    private DwmSensorEvent.DRIVING_STATE mDrivingState;
    
    private Context mContext;
    private String switch_camera_title[] = new String[2];
    
    public static final int MSG_ON_DRIVING = 1001;
    public static final int MSG_OFF_DRIVING = 1002;
    public static final int MSG_UPLOAD_FILE = 1003;
    public static final int MSG_REINIT_RECORDING = 1004;
    
    public static final int MSG_SWITCH_CAMERA_ZERO = 1005;
    public static final int MSG_SWITCH_CAMERA_ONE = 1006;
    /*
     * else if(intent.getBooleanExtra(EXTRA_OPEN_CAMERA_ZERO,false)){
            	switchCamera(cameraIdZero);
            }else if(intent.getBooleanExtra(EXTRA_OPEN_CAMERA_ONE,false)){
            	switchCamera(cameraIdOne);
            }
     */
    
    private Handler mHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			switch(msg.what){
			case MSG_ON_DRIVING:
				stopRecording();
				if(initVideoRecorder())
					startRecording();
		    	setDrivingState(DwmSensorEvent.DRIVING_STATE.STATE_DRVING);
				break;
			case MSG_OFF_DRIVING:
				stopRecording();
				setDrivingState(DwmSensorEvent.DRIVING_STATE.STATE_SLEEP);
				break;
			case MSG_UPLOAD_FILE:
				if(mExceptionFile != null)
					new UpFileAsyncTask(mContext).execute(mExceptionFile);
				break;
			case MSG_REINIT_RECORDING:
//				if(initVideoRecorder())
//					startRecording();
				break;
			case MSG_SWITCH_CAMERA_ZERO:
				switchCamera(cameraIdZero);
				break;
			case MSG_SWITCH_CAMERA_ONE:
				switchCamera(cameraIdOne);
				break;
			}
		}
    };

    private void setDrivingState(DwmSensorEvent.DRIVING_STATE state){
    	mDrivingState = state;
    	mDwmSensorEvent.setDrvingState(mDrivingState);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();

        // Start foreground service to avoid unexpected kill
        mContext = this.getApplicationContext();
        
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("行车记录功能运行中……")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();
        startForeground(1000, notification);

        mPreferences = DrivingRecorderSettings.getPreference(this);

        // Create new SurfaceView, set its size to 1x1, move it to the top left corner and set this service as a callback
        mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        mRecordView = inflater.inflate(R.layout.view_recorder, null);
//        mSurfaceView = new SurfaceView(this);
        mSurfaceView = (SurfaceView) mRecordView.findViewById(R.id.surface);
        
        mSurfaceView.setOnClickListener(this);
   //     mRecordView.findViewById(R.id.btn_back).setOnClickListener(this);
        mRecordView.findViewById(R.id.btn_gallery).setOnClickListener(this);
        mRecordView.findViewById(R.id.btn_settings).setOnClickListener(this);
        mRecordView.findViewById(R.id.btn_video_playback).setOnClickListener(this);
        ToggleButton soundToggle = (ToggleButton) mRecordView.findViewById(R.id.toggle_sound);
        soundToggle.setOnCheckedChangeListener(this);

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        mWindowManager.addView(mRecordView, layoutParams);

        mSurfaceView.getHolder().addCallback(this);
    /*    int sensitivity = (Integer) mPreferences.get(DrivingRecorderSettings.KEY_SENSITIVITY);
        if (sensitivity > 0) {
            mBrakingChecker = new BrakingChecker(this, sensitivity);
            mBrakingChecker.setup();
            mBrakingChecker.setOnBrakingListener(new BrakingChecker.OnBrakingListener() {
                @Override
                public void onBraking() {
                    mLockCurrentVideo = true;
                }
            });
        }
     */
        init();
    }
    
    private void init(){
    	switch_camera_title[0] = mContext.getString(R.string.switch_front_camera);
    	switch_camera_title[1] = mContext.getString(R.string.switch_post_camera);
    	DrivingRecorderReceiver mDrivingRecorderReceiver = new DrivingRecorderReceiver(mHandler);
    	IntentFilter filter = new IntentFilter();
    	filter.addAction(DrivingRecorderReceiver.CAR_ACTION);
    	filter.addAction(DrivingRecorderReceiver.CAR_UPLOAD_FILE_MP4);
    	filter.addAction(DrivingRecorderReceiver.ACTION_BACK_CAMERA_SIGNAL);
    	registerReceiver(mDrivingRecorderReceiver, filter);
    	
    	mDwmSensorEvent = new DwmSensorEvent(mContext);
    	mDwmSensorEvent.setup();
    	setDrivingState(DwmSensorEvent.DRIVING_STATE.STATE_DRVING);    	
    	mDwmSensorEvent.setOnEventListener(new OnExBrakListener() {
			
			@Override
			public void onExceptioning() {
				// TODO Auto-generated method stub
				mExceptionVideo = true;
				stopRecording();
	            try {
	                mCamera.reconnect();
	                mCamera.unlock();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
				if (initVideoRecorder()) {
					startRecording();
				}
			}
			
			@Override
			public void onBraking() {
				// TODO Auto-generated method stub
				mLockCurrentVideo = true;
			}
		});
    }
    // Remove SurfaceView
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (null != mBrakingChecker) {
            mBrakingChecker.tearDown();
        }
        if(mDwmSensorEvent != null)
        	mDwmSensorEvent.destory();
        stopRecording();
        mWindowManager.removeView(mRecordView);
    }

	public void switchCamera(int cameraId){
		if(mCurCameraId == cameraId){
			return;
		}
		synchronized (ACCESSIBILITY_SERVICE) {
			stopRecording();
			if (mCamera != null) {
				mCamera.lock();
	            mCamera.stopPreview();
	            mCamera.release();
	            mCamera = null;
	        }
			Toast.makeText(mContext, switch_camera_title[cameraId], Toast.LENGTH_LONG).show();
			if(initCamera(cameraId)){
				if((cameraId != cameraIdOne) && initVideoRecorder()){
		  			startRecording();
				}
			}
			mCurCameraId = cameraId;
		}
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            if (intent.getBooleanExtra(EXTRA_TAKE_PHOTO, false)) {
                takePhoto();
            } else if (intent.getBooleanExtra(EXTRA_FULL_SCREEN, false)) {
                ViewGroup.LayoutParams lp = mRecordView.getLayoutParams();
                if (null != lp) {
                    lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
                    lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    mWindowManager.updateViewLayout(mRecordView, lp);
                }
            } else if (intent.getBooleanExtra(EXTRA_MINIMIZE, false)) {
                minimizePreview();
            } else if (intent.getBooleanExtra(EXTRA_AUTO_FOCUS, false)) {
                if (null != mCamera && mSupportAutoFocus) {
                    mCamera.autoFocus(null);
                }
            } else if (intent.getBooleanExtra(EXTRA_CONFIG_CHANGED, false)) {
                mPreferences = DrivingRecorderSettings.getPreference(this);
            }else if(intent.getBooleanExtra(EXTRA_STOP_RECORD, false)){
            	stopRecording();
            }else if(intent.getBooleanExtra(EXTRA_START_RECORD, false)){
            	startRecording();
            }
        }
        return START_STICKY;
    }

    private void minimizePreview() {
        ViewGroup.LayoutParams lp = mRecordView.getLayoutParams();
        if (null != lp) {
            lp.width = 1;
            lp.height = 1;
            mWindowManager.updateViewLayout(mRecordView, lp);
        }
    }

    private void takePhoto() {
        if (null != mCamera && mCanTakePhotoWhenRecording) {
            if (mSupportAutoFocus) {
                // 在拍照前先对焦
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {
                        // 不论对焦是否成功都进行拍照
                        mCamera.takePicture(null, null, DrivingRecordService.this);
                    }
                });
            } else {
            	try{
            		mCamera.takePicture(null, null, DrivingRecordService.this);
            	}catch(Exception e){
            		Log.d(TAG, "takepicture error = " + e.toString());
            	}
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Method called right after Surface created (initializing and starting MediaRecorder)
    @Override
	public void surfaceCreated(SurfaceHolder surfaceHolder) {
		mSurfaceHolder = surfaceHolder;
		mMediaRecorder = new MediaRecorder();
		if (!initCamera(cameraIdZero)){
			stopSelf();
		}
		if (initVideoRecorder()){
			startRecording();
		}
	}

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        mSurfaceHolder = surfaceHolder;
        if (mSupportAutoFocus) {
            mCamera.autoFocus(null);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (null != mCamera) {
            mCamera.lock();
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Toast.makeText(getBaseContext(), R.string.take_photo, Toast.LENGTH_SHORT).show();
        savePhoto(data);
    }
    
    @Override
    public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
        // 已经录完了一段
        if (MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED == what ||
                MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED == what) {

            // 停止并重新录制
            mediaRecorder.stop();
            mediaRecorder.reset();

            if (null != mVideoFile) {
                // 在工作线程中将发生过急刹车的视频拷贝到锁定文件夹，使其不参与淘汰
                ThreadManager.INSTANCE.runOnWorkerThread(new Runnable() {
                    @Override
                    public void run() {
                    	if(mLockCurrentVideo && (mDrivingState == DwmSensorEvent.DRIVING_STATE.STATE_DRVING)){
                    		lockVideo();
                    		mLockCurrentVideo = false;
                    	}else if(mExceptionVideo && (mDrivingState == DwmSensorEvent.DRIVING_STATE.STATE_SLEEP)){
                    		exceptionVieo();
                    		Intent intent = new Intent();
                    		intent.setAction(DrivingRecorderReceiver.ACTION_TO_UPLOAD_FILE);
                    		mContext.sendBroadcast(intent);
                       		mExceptionVideo = false;
                    	}
                    }
                });
            }

            // 发广播让系统重新扫描媒体文件。
            if (null != mVideoFile) {
                Uri uri = Uri.fromFile(mVideoFile);
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            }
            if(mCamera == null)
            	return;
            // 重置一下预览View
            try {
                mCamera.reconnect();
                mCamera.unlock();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            // 重新初始化并开始录制新的一段，一直反复录制下去
            if (initVideoRecorder()) {
                startRecording();
            }
        }
    }

    @Override
	public void onError(MediaRecorder mr, int what, int extra) {
		// TODO Auto-generated method stub

	}

	private void exceptionVieo(){
        File lastVideoFile = mVideoFile;
        InputStream in = null;
        OutputStream out = null;
        File exceptionFile = null;
        try {
            File parent = lastVideoFile.getParentFile();
            if (null == parent) return;

            parent = parent.getParentFile();
            if (null != parent) {
                String name = lastVideoFile.getName();
                File exceptionFilePath = new File(parent + "/Exception");
                if (!exceptionFilePath.exists() && !exceptionFilePath.mkdir()) {
                    return;
                }
                exceptionFile = new File(exceptionFilePath, name);
                mExceptionFile = exceptionFile;
                in = new FileInputStream(lastVideoFile);
                out = new FileOutputStream(exceptionFile);
                StreamUtils.copy(in, out);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeStream(in);
            StreamUtils.closeStream(out);
        }
    }
    
    private void lockVideo() {
    	if(mVideoFile == null)
    		return;
        File lastVideoFile = mVideoFile;
        InputStream in = null;
        OutputStream out = null;
        File lockedFile = null;
        try {
            File parent = lastVideoFile.getParentFile();
            if (null == parent) return;

            parent = parent.getParentFile();
            if (null != parent) {
                String name = lastVideoFile.getName();
                File lockedPath = new File(parent + "/Locked");
                if (!lockedPath.exists() && !lockedPath.mkdir()) {
                    return;
                }
                lockedFile = new File(lockedPath, name);
                in = new FileInputStream(lastVideoFile);
                out = new FileOutputStream(lockedFile);
                StreamUtils.copy(in, out);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeStream(in);
            StreamUtils.closeStream(out);
        }
    }

    private boolean initCamera(int cameraId) {
        if (null == mCamera) {
            try {
                mCamera = Camera.open(cameraId);
            } catch (RuntimeException e) {
            //    Toast.makeText(getBaseContext(), "无法连接摄像头！", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
                return false;
            }

            if (null == mCamera) return false; // If There hasn't back-facing camera.
            
            Camera.Parameters params = mCamera.getParameters();

            // 检查并设置视频连续对焦
            List<String> supportedFocus = params.getSupportedFocusModes();
            if (!Checker.isEmpty(supportedFocus)) {
                for (String mode : supportedFocus) {
                    Log.w(TAG, "Support focus mode = " + mode);
                }

                if (supportedFocus.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (supportedFocus.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
            }
            // 计算并设置最合适的录制屏幕预览尺寸
            // 将视频尺寸设置为屏幕尺寸。
            int w = getResources().getDisplayMetrics().widthPixels;
            int h = getResources().getDisplayMetrics().heightPixels;
            Camera.Size size = getCameraPreviewSize(w, h);
            if (null != size) {
                params.setPreviewSize(size.width, size.height);
          //      Toast.makeText(getBaseContext(), String.format("录制预览尺寸：%dx%d", size.width, size.height), Toast.LENGTH_SHORT).show();
            }

            // 检查设备是否支持录制视频的同时拍照
            mCanTakePhotoWhenRecording = params.isVideoSnapshotSupported();
            if (mCanTakePhotoWhenRecording) {
                // 计算并设置最合适的拍照相片尺寸
                size = getCameraPictureSize();
                if (null != size) {
                    params.setPictureSize(size.width, size.height);
                }
            }
			
            // set Camera parameters
            mCamera.setParameters(params);
            mCamera.enableShutterSound(true);

            params = mCamera.getParameters();
            String focusMode = params.getFocusMode();

            Log.w(TAG, "Current focus mode = " + focusMode);
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                if (focusMode.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    mCamera.autoFocus(null);
                    mSupportAutoFocus = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        mCamera.unlock();

        return true;
    }

    private Camera.Size getCameraPreviewSize(int minWidth, int minHeight) {
        if (null != mCamera) {
            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> supportedSizes = params.getSupportedPreviewSizes();
//            Collections.sort(supportedSizes, mSizeComparator);

            for (Camera.Size size : supportedSizes) {
                if (size.width >= minWidth || size.height >= minHeight) {
                    return size;
                }
            }

            return supportedSizes.get(0);
        }
        return null;
    }

    private Camera.Size getCameraPictureSize() {
        if (null != mCamera) {
            int minWidth = (Integer)mPreferences.get(DrivingRecorderSettings.KEY_PHOTO_WIDTH);
            int minHeight = (Integer)mPreferences.get(DrivingRecorderSettings.KEY_PHOTO_HEIGHT);

            Camera.Parameters params = mCamera.getParameters();
            List<Camera.Size> supportedSizes = params.getSupportedPictureSizes();

            for (Camera.Size size : supportedSizes) {
                if (size.width >= minWidth && size.height >= minHeight) {
                    return size;
                }
            }

            return supportedSizes.get(0);
        }
        return null;
    }

    private String savePhoto(byte[] data) {
        if (null != data) {
            String filename = String.format("%s.jpg",
                    DateFormat.format(DATE_FORMAT, new Date().getTime()));
            Storage storage = new ExternalTFCardStorage(
                    DRIVING_RECORD_PICTURES, filename);

            File file = storage.getFile();
            if (null != file) {
                FileOutputStream fos = null;
                try {
                    if (file.createNewFile() || file.exists()) {
                        fos = new FileOutputStream(file);
                        fos.write(data);
                    }

                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                            Uri.parse("file://" + file.getPath())));
                    return file.getPath();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != fos) StreamUtils.closeStream(fos);
                }
            }
        }

        return null;
    }

	public long getAvailableExternalStorageSize(){
		Method sGetExternalStoragePath = null;
		Class sStorageManagerExClazz = null;
        if (null == sStorageManagerExClazz || null == sGetExternalStoragePath) {
            try {
                sStorageManagerExClazz = Class.forName("com.mediatek.storage.StorageManagerEx");
                sGetExternalStoragePath = sStorageManagerExClazz.getDeclaredMethod("getExternalStoragePath");
                if (!sGetExternalStoragePath.isAccessible()) {
                    sGetExternalStoragePath.setAccessible(true);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
		String externalPath = null;
		try {
			externalPath = (String) sGetExternalStoragePath.invoke(null);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		if(externalPath!=null){
        	StatFs stat = new StatFs(externalPath);
        	long blockSize = stat.getBlockSize();
        	long availableBlocks = stat.getAvailableBlocks();
			return availableBlocks * blockSize;
		}
        return -1;
	}

	private boolean initVideoRecorder() {
		if(mMediaRecorder == null)
			mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(mCamera);
        boolean isSoundon = (Boolean)mPreferences.get(DrivingRecorderSettings.KEY_SOUND_ON);
        if (isSoundon) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(/*MediaRecorder.OutputFormat.MPEG_4*/1);
        
        if (isSoundon) {
            // Must be called after setOutputFormat.
            mMediaRecorder.setAudioEncoder(/*MediaRecorder.OutputFormat.DEFAULT*/3);
            mMediaRecorder.setAudioEncodingBitRate(128000);
            mMediaRecorder.setAudioChannels(2);
            mMediaRecorder.setAudioSamplingRate(48000);
        }
        mMediaRecorder.setVideoEncoder(/*MediaRecorder.VideoEncoder.H264*/2);
        // 这个方法在车机上调用会影响回放速率（变快不少），暂时注释掉
//        mMediaRecorder.setCaptureRate(CAPTURE_RATE);
        mMediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE);
        mMediaRecorder.setVideoEncodingBitRate((Integer) mPreferences.get(DrivingRecorderSettings.KEY_BITS_RATE));
        mMediaRecorder.setVideoSize(
                (Integer)mPreferences.get(DrivingRecorderSettings.KEY_VIDEO_WIDTH),
                (Integer)mPreferences.get(DrivingRecorderSettings.KEY_VIDEO_HEIGHT));

        String filename = String.format(VIDEO_FILE_FORMAT,
                DateFormat.format(DATE_FORMAT, new Date().getTime()));
        Storage storage = new ExternalTFCardStorage(DRIVING_RECORD_ALL_VIDEO, filename);
        mVideoFile = storage.getFile();
        if (null == mVideoFile) { // 外部存储设备未挂载
            TTSClient.speak(getBaseContext(), getString(R.string.no_external_storage));
            return false;
        }
    //    long freeSize = StorageUtils.getAvailableExternalStorageSize();
    	long freeSize = getAvailableExternalStorageSize();
        // 最小可用空间等于最小预留空间加一个视频的最大空间，这样录制完下一个视频后才会不超过
        // 最小预留空间。
        long mini_storage_available_size = MINIMIZE_STORAGE_AVAILABLE_SIZE + MAX_FILE_SIZE;
        
        if (freeSize < mini_storage_available_size) {
            long cleanSize = mini_storage_available_size - freeSize;
            storage = new ExternalTFCardStorage(DRIVING_RECORD_ALL_VIDEO, "");
            // 如果存储空间不足，则清除最早的视频文件
            // 若释放的空间不够，退出录制
            if (cleanOldestFile(storage.getFile(), cleanSize) < cleanSize) {
                // TODO: 如果马上 StopSelf 则 Toast 不显示，这里需要做延时 StopSelf
                TTSClient.speak(getBaseContext(), getString(R.string.not_enough_storage));
                return false;
            }
        }
        
        mMediaRecorder.setOutputFile(mVideoFile.getPath());
        
		if (mDrivingState == DwmSensorEvent.DRIVING_STATE.STATE_DRVING) {
			mMediaRecorder.setMaxDuration((Integer) mPreferences.get(DrivingRecorderSettings.KEY_VIDEO_DURATION)
					* Constants.MILLIS_PER_SECOND);
		} else {
			mMediaRecorder.setMaxDuration(DrivingRecorderSettings.EXCEPTION_DURING * Constants.MILLIS_PER_SECOND);
		}
        mMediaRecorder.setMaxFileSize(MAX_FILE_SIZE);
        mMediaRecorder.setOnInfoListener(this);
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

    private void startRecording() {
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            mIsRecording = true;
        } catch (Exception e) {
            e.printStackTrace();
            mMediaRecorder.release();
        }
    }

    private void stopRecording() {
        // Stop recording
        if (null != mMediaRecorder && mIsRecording) {
            mIsRecording = false;
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        mSupportAutoFocus = false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_gallery: {
                Intent intent = new Intent();
                intent.setClassName("com.android.gallery3d",
                        "com.android.gallery3d.app.GalleryActivity");
				intent.setType("AllVideo/mp4");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                AppUtils.startActivity(this, intent);
            }
                break;
            case R.id.surface:
                takePhoto();
                break;
            case R.id.btn_settings:
                startActivity(new Intent(this, DrivingRecorderSettings.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case R.id.btn_video_playback:
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.toggle_sound:
                boolean isSoundOn;
                if (isChecked) {
                    isSoundOn = false;
                } else {
                    isSoundOn = true;
                }
				
                mPreferences.put(DrivingRecorderSettings.KEY_SOUND_ON, isSoundOn);
                SharedPreferences sp = getSharedPreferences(
                        DrivingRecorderSettings.SETTINGS_NAME, MODE_PRIVATE);
                sp.edit().putBoolean(DrivingRecorderSettings.KEY_SOUND_ON, isSoundOn).commit();
                Log.d(TAG, "Turn sound " + (isSoundOn ? "On" : "Off"));
                break;
        }
    }
}
