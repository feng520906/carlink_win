package com.coopox.hwmsersor;

import java.util.Map;

import com.coopox.DrivingRecorder.DrivingRecorderSettings;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

public class DwmSensorEvent {
	
	private static final String TAG = "DwmSensorEvent";
	public static enum DRIVING_STATE{
		STATE_DRVING,
		STATE_SLEEP,
		STATE_NULL
	}
	DRIVING_STATE mState = DRIVING_STATE.STATE_NULL;
	
	private Context mContext;
	private SensorManager mSensorManager = null;
	private final Object mSyncObj = new Object();
	protected boolean mHasGyroSensor;
	protected Sensor mGyroSensor;
	
	
	private float mShold = 0.0f;
	private float mExceptionShold = 0.3f;
	private float mBrakingShold = 0.6f;
	private float mLastX = 0.0f, mLastY = 0.0f, mLastZ = 0.0f;
	
	private long mDuration = 1000;
	private long mlastBrakingTime = 0;
	
	private OnExBrakListener mListener = null;
	
	private Map<String, Object> mPreferences;
	
	public interface OnExBrakListener{
		void onExceptioning();
		void onBraking();
	}
	
	public DwmSensorEvent(Context context){
		mContext = context;
	}
	//int sensitivity = 
	public void setup(){
		mPreferences = DrivingRecorderSettings.getPreference(mContext);
		int sensit =  (Integer) mPreferences.get(DrivingRecorderSettings.KEY_SENSITIVITY);
		if(sensit == DrivingRecorderSettings.SENSITYIVITY_MAX){
			mBrakingShold = 255.0f;
		}else if(sensit == DrivingRecorderSettings.SENSITYIVITY_MID){
			mBrakingShold = 0.8f;
		}else if(sensit == DrivingRecorderSettings.SENSITYIVITY_MAX){
			mBrakingShold = 0.6f;
		}
		registerSensorListener();
		
	}
	
	public void setDrvingState(DRIVING_STATE state){
		mState = state;
		if(mState == DRIVING_STATE.STATE_DRVING)
			mShold = mBrakingShold;
		if(mState == DRIVING_STATE.STATE_SLEEP)
			mShold = mExceptionShold;
	}
	
	public void setOnEventListener(OnExBrakListener exceptionListener){
		this.mListener = exceptionListener;
	}
	
	private void registerSensorListener() {
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		mHasGyroSensor = (mGyroSensor != null);
		if (mHasGyroSensor) {
			mSensorManager.registerListener(sensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					SensorManager.SENSOR_DELAY_NORMAL);
		}
	}
	
	private void unregisterSensorListener(){
		if(mHasGyroSensor)
			mSensorManager.unregisterListener(sensorEventListener);
	}
	

	public void destory(){
		unregisterSensorListener();
	}
	
	public void startRecording(){
		if(mState == DRIVING_STATE.STATE_DRVING){
			mListener.onBraking();
		}else if(mState == DRIVING_STATE.STATE_SLEEP){
			mListener.onExceptioning();
		}
	}
	
	public void onGyroSensorChanged(SensorEvent event) {

		synchronized (mSyncObj) {
			int type = event.sensor.getType();
			if (type != Sensor.TYPE_GYROSCOPE) {
				return;
			}

			boolean toSaveRecording = false;

			float x = event.values[0];
			float y = event.values[1];
			float z = event.values[2];
			float absX = Math.abs(x);
			float absY = Math.abs(y);
			float absZ = Math.abs(z);

			if (absX > mShold) {
				if (Math.abs(x - mLastX) > absX) {
					toSaveRecording = true;
				}
			}

			if (absY > mShold) {
				if (Math.abs(y - mLastY) > absY) {
					toSaveRecording = true;
				}
			}

			if (absZ > mShold) {
				if (Math.abs(z - mLastZ) > absZ) {
					toSaveRecording = true;
				}
			}
			mLastX = x; mLastY = y; mLastZ = z;
			if (toSaveRecording) {
				Log.d(TAG, "mLastX = " + mLastX + "; mLastY + " + mLastY + "; mLastZ = " + mLastZ);
				long currentMills = SystemClock.uptimeMillis();
				if ((currentMills - mlastBrakingTime) > mDuration) {
					Log.d(TAG, "222222222222222");
					mlastBrakingTime = currentMills;
					if (mListener != null) {
						Log.d(TAG, "startRecording 3333333333333333");
						startRecording();
					}
				}
			}
		}
	}
	
	private SensorEventListener sensorEventListener = new SensorEventListener() {
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if(!mHasGyroSensor || (mState == DRIVING_STATE.STATE_NULL))
				return;
			onGyroSensorChanged(event);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
}
