package com.coopox.DrivingRecorder;

import static android.hardware.SensorManager.DATA_X;
import static android.hardware.SensorManager.DATA_Y;
import static android.hardware.SensorManager.DATA_Z;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/10
 */
public class BrakingChecker implements SensorEventListener {

    private static final String TAG = "BrakingChecker";
    public interface OnBrakingListener {
        void onBraking();
    }

    private float mThreshold = 10.0f;
    private long mBrakingDuration = 500;

    private SensorManager mSensorManager;
    private float mLastX, mLastY, mLastZ;
    private long mLastBrakingTime;
    private OnBrakingListener mListener;

    public BrakingChecker(Context context, float threshold) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mThreshold = threshold;
    }

    public void setOnBrakingListener(OnBrakingListener listener) {
        mListener = listener;
    }

    public void setup() {
        registerSensorListener();
    }

    public void tearDown() {
        unregisterSensorListener();
    }

    public void setAccelerationThreshold(float threshold) {
        if (threshold > 0f) {
            mThreshold = threshold;
        }
    }

    public void setBrakingDuration(long millis) {
        if (millis > 0) {
            mBrakingDuration = millis;
        }
    }

    private void unregisterSensorListener() {
        mSensorManager.unregisterListener(this);
        Log.v(TAG, "Unregister acceleration sensor.");
    }

    private void registerSensorListener() {
        Sensor sensor = null;
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Log.v(TAG, "Register accelerometer.");
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (null == mListener) return;

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        float absX = Math.abs(x);
        float absY = Math.abs(y);
        float absZ = Math.abs(z);

        boolean mayBraking = false;

        // 当加速度超过阈值并且方向发生改变时，计为一次急刹车
        if (absX > mThreshold) {
            if (Math.abs(x - mLastX) > absX) {
                Log.d(TAG, String.format("last x = %f, current x = %f", mLastX, x));
                mayBraking = true;
            }
        }

        if (absY > mThreshold) {
            if (Math.abs(y - mLastY) > absY) {
                Log.d(TAG, String.format("last y = %f, current y = %f", mLastY, y));
                mayBraking = true;
            }
        }

        if (absZ > mThreshold) {
            if (Math.abs(z - mLastZ) > absZ) {
                Log.d(TAG, String.format("last z = %f, current z = %f", mLastZ, z));
                mayBraking = true;
            }
        }
        mLastX = x; mLastY = y; mLastZ = z;

        if (mayBraking) {

            long currentMills = SystemClock.uptimeMillis();
            if (currentMills - mLastBrakingTime > mBrakingDuration) {
                mLastBrakingTime = currentMills;
                if (null != mListener) {
                    mListener.onBraking();
                    Log.d(TAG, "Braking event happen.");
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
