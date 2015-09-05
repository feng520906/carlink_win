package com.coopox.carlauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.activeandroid.util.Log;
import com.baidu.android.pushservice.PushConstants;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-4
 * 用于处理转发自 BaiduPushMsgReceiver 的广播，因为该 Receiver 在 Launcher 的主
 * Activity 中注册，所以主要执行一些与 UI 强相关的任务
 */
public class PushMsgLocalReceiver extends BroadcastReceiver {
    private String TAG = "PushMsgLocalReceiver";
    private static Set<PushMessageFilter> sFilters = new HashSet<PushMessageFilter>(8);

    public static void registerPushMsgFilter(PushMessageFilter filter) {
        if (null != filter) {
            sFilters.add(filter);
        }
    }

    public static void unregisterPushMsgFilter(PushMessageFilter filter) {
        if (null != filter) {
            sFilters.remove(filter);
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String content = intent.getStringExtra(PushConstants.EXTRA_PUSH_MESSAGE_STRING);
        Log.d(TAG, String.format("Receiver a local push msg %s", content));
        Map<String, String> extras = BaiduPushMsgReceiver.pickMsgExtras(intent);

        for (PushMessageFilter filter : sFilters) {
            if (filter.handleMessage(content, extras)) {
                break;
            }
        }
    }
}
