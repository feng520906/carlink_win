package com.coopox.carlauncher.service;

import android.app.IntentService;
import android.content.Intent;
import com.activeandroid.util.Log;
import com.baidu.android.pushservice.PushConstants;
import com.coopox.carlauncher.misc.ApkInstaller;
import com.coopox.carlauncher.misc.PushCmd;
import com.coopox.carlauncher.network.HttpDownloader;
import com.coopox.carlauncher.receiver.BaiduPushMsgReceiver;

import java.io.File;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-4
 */
public class PushMessageService extends IntentService {
    private static final String TAG = "PushMessageService";

    public PushMessageService() {
        this(TAG);
    }
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public PushMessageService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String content = intent.getStringExtra(PushConstants.EXTRA_PUSH_MESSAGE_STRING);
        Log.d(TAG, String.format("Service for handle push msg %s", content));
        Map<String, String> extras = BaiduPushMsgReceiver.pickMsgExtras(intent);

        if (extras.containsKey(PushCmd.CMD_APP_UPGRADE)) {
            File file = HttpDownloader.INSTANCE.syncDownload(extras.get(PushCmd.CMD_APP_UPGRADE));
            if (null != file) {
                if (!ApkInstaller.silentInstall(file.getPath())) {
                    ApkInstaller.install(PushMessageService.this, file.getPath());
                } else {
                    Log.i(TAG, String.format("APK silent install success."));
                }
            } else {
                Log.e(TAG, String.format("APK download failed."));
            }
        }
    }
}
