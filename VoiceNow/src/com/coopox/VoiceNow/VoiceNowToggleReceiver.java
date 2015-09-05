package com.coopox.VoiceNow;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.coopox.common.Constants;
import com.coopox.common.utils.Checker;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-1
 */
public class VoiceNowToggleReceiver extends BroadcastReceiver {
    private static final String TAG = "VoiceNow";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "VoiceNow request was Received");
        Intent voiceNowIntent = new Intent(context, VoiceNowServiceEx.class);

        if (null != intent) {
            String action = intent.getAction();
            if (null != action) {
                if (action.equals(Constants.ACTION_SMART_KEY_EVENT)) {
                    byte[] values = intent.getByteArrayExtra("VALUE");
                    if (!Checker.isEmpty(values)) {
                        int keycode = values[0];
                        if (0 != keycode) {
                            voiceNowIntent.putExtra("extra_cmd", keycode); // 智键指令直接激活语音识别
                        }
                    }
                }
            }
        }

        context.startService(voiceNowIntent);
    }
}
