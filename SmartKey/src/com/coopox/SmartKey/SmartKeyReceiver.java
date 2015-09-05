package com.coopox.SmartKey;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15-5-2
 */
public class SmartKeyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent smartKeyIntent = new Intent(context, SmartKeyService.class);
        context.startService(smartKeyIntent);
    }
}
