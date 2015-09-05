package com.coopox.DrivingRecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.umeng.analytics.MobclickAgent;

public class DrivingRecorderActivity extends Activity implements View.OnClickListener {

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        View btn = findViewById(R.id.start_recording);
        btn.setOnClickListener(this);

        btn = findViewById(R.id.stop_recording);
        btn.setOnClickListener(this);

        btn = findViewById(R.id.take_photo);
        btn.setOnClickListener(this);

        btn = findViewById(R.id.share_photo);
        btn.setOnClickListener(this);

        btn = findViewById(R.id.voice_now);
        btn.setOnClickListener(this);

        btn = findViewById(R.id.voice_disable);
        btn.setOnClickListener(this);

        btn = findViewById(R.id.voice_control);
        btn.setOnClickListener(this);

        btn = findViewById(R.id.smartkey_off);
        btn.setOnClickListener(this);

        btn = findViewById(R.id.smartkey_on);
        btn.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_recording:
                startService(new Intent(this, DrivingRecordService.class));
                break;
            case R.id.stop_recording:
                // TODO: 停止时保存最后一段
                stopService(new Intent(this, DrivingRecordService.class));
                break;
            case R.id.take_photo:
                sendBroadcast(new Intent(DrivingRecordService.ACTION_TAKE_PHOTO));
                break;
            case R.id.voice_now: {
                // TODO: Test Voice Now Service.
//        Intent intent = new Intent("com.coopox.service.voicenow.action.START");
//        mContext.sendBroadcast(intent);
                Intent serviceIntent = new Intent("com.coopox.service.action.VOICE_NOW");
                startService(serviceIntent);
            }
                break;
            case R.id.voice_disable: {
                Intent serviceIntent = new Intent("com.coopox.service.action.VOICE_NOW");
//                serviceIntent.putExtra("stop", true);
//                startService(serviceIntent);
                stopService(serviceIntent);
            }
            break;

            case R.id.voice_control: {
                Intent serviceIntent = new Intent("com.coopox.service.action.VOICE_NOW");
                serviceIntent.putExtra("extra_cmd", 4);
                startService(serviceIntent);
            }
                break;

            case R.id.smartkey_on: {
                Intent serviceIntent = new Intent("com.coopox.service.action.START_KEY_SERVICE");
                startService(serviceIntent);
            }
                break;
            case R.id.smartkey_off: {
                Intent serviceIntent = new Intent("com.coopox.service.action.START_KEY_SERVICE");
//                serviceIntent.putExtra("stop", true);
//                startService(serviceIntent);
                stopService(serviceIntent);
            }
            break;

            case R.id.share_photo:
                Intent serviceIntent = new Intent("com.coopox.service.action.SHARE2WX");
                serviceIntent.putExtra("share", true);
                startService(serviceIntent);
/*                Intent activityIntent = new Intent(Intent.ACTION_MAIN);;
                activityIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                activityIntent.setClassName("com.coopox.VoiceNow", "com.coopox.VoiceNow.wxapi.WXEntryActivity");
                ActivityUtils.startActivity(this, activityIntent);*/
                break;
        }
    }
}
