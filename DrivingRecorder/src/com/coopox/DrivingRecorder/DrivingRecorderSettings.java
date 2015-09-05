package com.coopox.DrivingRecorder;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/10
 */
public class DrivingRecorderSettings extends Activity implements
        RadioGroup.OnCheckedChangeListener, View.OnClickListener {

	public static final int EXCEPTION_DURING = 10;
	
    public static final String KEY_VIDEO_WIDTH = "kVideoWidth";
    public static final String KEY_VIDEO_HEIGHT = "kVideoHeight";
    public static final String KEY_VIDEO_DURATION = "kVideoDuration";
    public static final String KEY_SENSITIVITY = "kSensitivity";
    public static final String KEY_PHOTO_WIDTH = "kPhotoWidth";
    public static final String KEY_PHOTO_HEIGHT = "kPhotoHeight";
    public static final String KEY_BITS_RATE = "kBitsRate";
    public static final String KEY_SOUND_ON = "kSoundOn";

    public static final String SETTINGS_NAME = "Settings";

    public static final int SENSITYIVITY_MIN = Integer.MAX_VALUE; //sensitivity
    public static final int SENSITYIVITY_MID = 20;
    public static final int SENSITYIVITY_MAX = 15;
    
    RadioGroup mVideoQualityGroup;

    RadioGroup mDurationGroup;

    RadioGroup mPhotoQualityGroup;

    RadioGroup mSensitivityGroup;

    private int mBackDoorKnock = 0;
    private int mVideoWidth = 1280;
    private int mVideoHeight = 720;
    private int mVideoDuration = 60;
    private int mSensitivity = 10;
    private int mBitsRate = 6800000;
    /**
     * All Support picture size:
     *320x240
     640x480
     1024x768
     1280x720
     1280x768
     1280x960
     1440x960
     1600x960
     1600x1200
     2048x1152
     2048x1360
     2048x1536
     2560x1440
     2560x1712
     2560x1920
     2880x1728
     3264x2448
     3328x1872
     3600x2160 */
    private int mPhotoWidth = 3600;
    private int mPhotoHeight = 2160;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder_settings);
        setupViews();
    }

    void setupViews() {
        ActionBar actionBar = getActionBar();
        if (null != actionBar) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mVideoQualityGroup = (RadioGroup) findViewById(R.id.video_quality_group);
        mDurationGroup = (RadioGroup) findViewById(R.id.duration_group);
        mPhotoQualityGroup = (RadioGroup) findViewById(R.id.photo_quality_group);
        mSensitivityGroup = (RadioGroup) findViewById(R.id.sensitivity_group);

        mSharedPreferences = getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);

        mVideoWidth = mSharedPreferences.getInt(KEY_VIDEO_WIDTH, 1280);
        mVideoHeight = mSharedPreferences.getInt(KEY_VIDEO_HEIGHT, 720);
        mVideoQualityGroup.check(720 == mVideoHeight ? R.id.v720p : R.id.v1080p);

        final SparseIntArray durationToId = new SparseIntArray(4);
        durationToId.put(60, R.id.radio1Minus);
        durationToId.put(180, R.id.radio3Minus);
        durationToId.put(300, R.id.radio5Minus);

        mVideoDuration = mSharedPreferences.getInt(KEY_VIDEO_DURATION, 60);
        int id = durationToId.get(mVideoDuration);
        if (0 == id) id = R.id.radio1Minus;
        mDurationGroup.check(id);

        final SparseIntArray sensiToId = new SparseIntArray(4);
        sensiToId.put(Integer.MIN_VALUE, R.id.radio_no_sens);
        sensiToId.put(20, R.id.radio_mid_sens);
        sensiToId.put(15, R.id.radio_high_sens);
        mSensitivity = mSharedPreferences.getInt(KEY_SENSITIVITY, 10);
        id = sensiToId.get(mSensitivity);
        if (0 == id) id = R.id.radio_mid_sens;
        mSensitivityGroup.check(id);

        final SparseIntArray photoSizeToId = new SparseIntArray(4);
        photoSizeToId.put(2048, R.id.radio_low);
        photoSizeToId.put(2880, R.id.radio_mid);
        photoSizeToId.put(3600, R.id.radio_high);
        mPhotoWidth = mSharedPreferences.getInt(KEY_PHOTO_WIDTH, 3600);
        mPhotoHeight = mSharedPreferences.getInt(KEY_PHOTO_HEIGHT, 2160);
        id = photoSizeToId.get(mPhotoWidth);
        if (0 == id) id = R.id.radio_high;
        mPhotoQualityGroup.check(id);

        mVideoQualityGroup.setOnCheckedChangeListener(this);
        mDurationGroup.setOnCheckedChangeListener(this);
        mPhotoQualityGroup.setOnCheckedChangeListener(this);
        mSensitivityGroup.setOnCheckedChangeListener(this);
        // 隐藏的调试界面入口
        findViewById(R.id.radio_no_sens).setOnClickListener(this);
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (group.getId()) {
            case R.id.duration_group:
                switch (checkedId) {
                    case R.id.radio1Minus:
                        mVideoDuration = 60;
                        break;
                    case R.id.radio3Minus:
                        mVideoDuration = 180;
                        break;
                    case R.id.radio5Minus:
                        mVideoDuration = 300;
                        break;
                }
                break;
            case R.id.video_quality_group:
                switch (checkedId) {
                    case R.id.v720p:
                        mVideoWidth = 1280;
                        mVideoHeight = 720;
                        mBitsRate = 6800000;
                        break;
                    case R.id.v1080p:
                        mVideoWidth = 1920;
                        mVideoHeight = 1080;
                        mBitsRate = 17000000;
                        break;
                }
                break;
            case R.id.photo_quality_group:
                switch (checkedId) {
                    case R.id.radio_low:
                        mPhotoWidth = 2048;
                        mPhotoHeight = 1536;
                        break;
                    case R.id.radio_mid:
                        mPhotoWidth = 2880;
                        mPhotoHeight = 1728;
                        break;
                    case R.id.radio_high:
                        mPhotoWidth = 3600;
                        mPhotoHeight = 2160;
                        break;
                }
                break;
            case R.id.sensitivity_group:
                switch (checkedId) {
                    case R.id.radio_no_sens:
                        mSensitivity = SENSITYIVITY_MIN;
                        break;
                    case R.id.radio_mid_sens:
                        mSensitivity = SENSITYIVITY_MID;
                        break;
                    case R.id.radio_high_sens:
                        mSensitivity = SENSITYIVITY_MAX;
                        break;
                }
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                storeSettingsAndNotify();

                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        storeSettingsAndNotify();
    }

    private void storeSettingsAndNotify() {
        savePreferences();

        Intent intent = new Intent(this, DrivingRecordService.class);
        intent.putExtra(DrivingRecordService.EXTRA_CONFIG_CHANGED, true);
        startService(intent);
    }

    public void onClick(View v) {
        if (++mBackDoorKnock == 8) {
            startActivity(new Intent(this, DrivingRecorderActivity.class));
            mBackDoorKnock = 0;
        }
    }

    private void savePreferences() {
        SharedPreferences.Editor edit = mSharedPreferences.edit();
        edit.putInt(KEY_VIDEO_DURATION, mVideoDuration);
        edit.putInt(KEY_SENSITIVITY, mSensitivity);
        edit.putInt(KEY_PHOTO_WIDTH, mPhotoWidth);
        edit.putInt(KEY_PHOTO_HEIGHT, mPhotoHeight);
        edit.putInt(KEY_VIDEO_WIDTH, mVideoWidth);
        edit.putInt(KEY_VIDEO_HEIGHT, mVideoHeight);
        edit.putInt(KEY_BITS_RATE, mBitsRate);

        edit.commit();
    }

    public static Map<String, Object> getPreference(Context context) {
        if (null == context) return null;
        SharedPreferences sp = context.getSharedPreferences(SETTINGS_NAME, MODE_PRIVATE);
        Map<String, Object> preferences = new HashMap<String, Object>();

        preferences.put(KEY_VIDEO_DURATION, sp.getInt(KEY_VIDEO_DURATION, 60));
        preferences.put(KEY_SENSITIVITY, sp.getInt(KEY_SENSITIVITY, 15));

        preferences.put(KEY_VIDEO_WIDTH, sp.getInt(KEY_VIDEO_WIDTH, 1280));
        preferences.put(KEY_VIDEO_HEIGHT, sp.getInt(KEY_VIDEO_HEIGHT, 720));
        preferences.put(KEY_PHOTO_WIDTH, sp.getInt(KEY_PHOTO_WIDTH, 3600));
        preferences.put(KEY_PHOTO_HEIGHT, sp.getInt(KEY_PHOTO_HEIGHT, 2160));

        preferences.put(KEY_BITS_RATE, sp.getInt(KEY_BITS_RATE, 6800000));
        preferences.put(KEY_SOUND_ON, sp.getBoolean(KEY_SOUND_ON, true));

        return preferences;
    }
}
