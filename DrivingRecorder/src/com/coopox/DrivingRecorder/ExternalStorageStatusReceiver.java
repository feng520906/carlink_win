package com.coopox.DrivingRecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.coopox.common.storage.ExternalTFCardStorage;
import com.coopox.common.tts.TTSClient;


/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/1/10
 */
public class ExternalStorageStatusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Uri uri = intent.getData();
        if (uri.getPath().equals(ExternalTFCardStorage.getExternalStoragePath())) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {
                context.startService(new Intent(context, DrivingRecordService.class));
                TTSClient.speak(context, "检测到存储卡，重新打开行车记录功能。");
            } else {
                // TF 卡被卸载，停止录制服务
                context.stopService(new Intent(context, DrivingRecordService.class));
                Toast.makeText(context, "TF 卡被取出，行车记录功能将无法使用！", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
