package com.coopox.carlauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.baidu.android.pushservice.PushConstants;
import com.coopox.carlauncher.service.PushMessageService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: kanedong
 * Date: 14-8-5
 */
public class BaiduPushMsgReceiver extends BroadcastReceiver {
    private static final String TAG = "BaiduPushMsgReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(PushConstants.ACTION_MESSAGE)) {
            String content = intent.getStringExtra(PushConstants.EXTRA_PUSH_MESSAGE_STRING);
            Log.d(TAG, String.format("Got a push message: %s", content));

            // 转发给同进程的其它 Receiver，一般当 Launcher 启动时 Broadcast 与它同属
            // 一个进程，因此可以由 Launcher 处理
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

            // 同时也启动一个服务，用于处理一些与 Launcher 与关的任务，例如下载和安装应用
            // 这样一来 Launcher 进程未启动也不影响这些任务的执行。
            Intent serviceIntent = new Intent(context, PushMessageService.class);
            serviceIntent.putExtras(intent.getExtras());
            context.startService(serviceIntent);

        }

        else if (intent.getAction().equals(PushConstants.ACTION_RECEIVER_NOTIFICATION_CLICK)) {
            String content = intent.getExtras().getString(PushConstants.EXTRA_EXTRA);

/*            String city;
            String gender;
            try {
                JSONObject contentJson = new JSONObject(content);
                city = contentJson.getString("city");
                gender = contentJson.getString("gender");
            } catch (JSONException e) {
                Log.d(TAG, "parse message as json exception " + e);
            }*/
        }
    }

    public static Map<String, String> pickMsgExtras(Intent intent) {
        String extraStr = intent.getStringExtra(PushConstants.EXTRA_EXTRA);
        try {
            JSONObject extraJson = new JSONObject(extraStr);
            Map<String, String> extras =
                    new Hashtable<String, String>(extraJson.length());
            Iterator iterator = extraJson.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                if (null != key) {
                    extras.put(key, extraJson.optString(key, ""));
                }
            }

            return extras;
        } catch (JSONException e) {
            Log.d(TAG, "parse message as json exception " + e);
        }
        return null;
    }
}
