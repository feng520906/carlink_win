package com.coopox.DrivingRecorder;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import com.umeng.analytics.MobclickAgent;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-14
 */
public class RecordPreviewActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "DrivingPreviewActivity";
    private PreviewMinimizeBroadcastReceiver mPreviewMinimizeBroadcastReceiver =
            new PreviewMinimizeBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preview);

        IntentFilter filter = new IntentFilter(DrivingRecordService.ACTION_PREVIEW_MINIMIZE);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mPreviewMinimizeBroadcastReceiver, filter);
    //    findViewById(R.id.btn_back_shadow).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mPreviewMinimizeBroadcastReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, DrivingRecordService.class);
        intent.putExtra(DrivingRecordService.EXTRA_FULL_SCREEN, true);
        startService(intent);
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

        Intent intent = new Intent(this, DrivingRecordService.class);
        intent.putExtra(DrivingRecordService.EXTRA_MINIMIZE, true);
        startService(intent);
    }

    @Override
    public void onClick(View v) {
      //  switch (v.getId()) {
      //      case R.id.btn_back_shadow:
      //          finish();
      //          break;
      //  }
    }

    class PreviewMinimizeBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(DrivingRecordService.ACTION_PREVIEW_MINIMIZE)) {
                finish();
            }
        }
    }
}
