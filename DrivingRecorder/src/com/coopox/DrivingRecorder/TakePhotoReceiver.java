package com.coopox.DrivingRecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-14
 */
public class TakePhotoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("TakePhotoReceiver", "onReceive invoked.");
        Intent msgIntent = new Intent(context, DrivingRecordService.class);
        msgIntent.setAction(intent.getAction());
        if (intent.getAction().equals(DrivingRecordService.ACTION_TAKE_PHOTO)) {
            msgIntent.putExtra(DrivingRecordService.EXTRA_TAKE_PHOTO, true);
        }
        context.startService(msgIntent);
    }
}
