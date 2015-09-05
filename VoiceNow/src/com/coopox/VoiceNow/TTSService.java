package com.coopox.VoiceNow;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import com.coopox.common.Constants;
import com.coopox.common.tts.TTSListener;
import com.coopox.VoiceNow.TTS.TTSQueue;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/1/23
 */
// TODO: 完全可以改成 IntentService 实现
public class TTSService extends Service implements TTSListener {

    private static final String TAG = "TTSService";
    TTSQueue mTTSQueue;
    @Override
    public void onCreate() {
        super.onCreate();

        mTTSQueue = new TTSQueue(this);
        mTTSQueue.setListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mTTSQueue) {
            mTTSQueue.cancelAll();
            mTTSQueue.teardown();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent && null != intent.getExtras() && null != mTTSQueue) {
            // 先处理控制指令，再处理播放指令
            if (intent.getExtras().containsKey(Constants.EXTRA_PAUSE)) {
                if (mTTSQueue.isSpeaking()) {
                    mTTSQueue.pauseTTS();
                }
            } else if (intent.getExtras().containsKey(Constants.EXTRA_RESUME)) {
                if (!mTTSQueue.isSpeaking()) {
                    mTTSQueue.resumeTTS();
                }
            } else if (intent.getExtras().containsKey(Constants.EXTRA_STOP)) {
                stopSelf();
            } else {
                ResultReceiver resultReceiver =
                        intent.getParcelableExtra(Constants.EXTRA_TTS_RECEIVER);
                String content = intent.getStringExtra(Constants.EXTRA_TTS_CONTENT);
                if (null != content) {
                    if (intent.getBooleanExtra(Constants.EXTRA_TTS_TO_HEAD, false) ||
                            intent.getBooleanExtra(Constants.EXTRA_FORCE_CANCEL, false)) {
                        // EXTRA_FORCE_CANCEL 会取消正在播放的语音马上开始下条语音的效果。
                        if (mTTSQueue.isSpeaking() &&
                                intent.getBooleanExtra(Constants.EXTRA_FORCE_CANCEL, false)) {
                            mTTSQueue.cancel();
                        }
                        mTTSQueue.enqueueTextToHead(content, resultReceiver);
                    } else {
                        mTTSQueue.enqueueTextToTail(content, resultReceiver);
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onSpeechStart(String text) {

    }

    @Override
    public void onSpeechStop(String text, boolean allClear) {
        if (allClear) {
            Log.d(TAG, "All speech clear, stop myself");
            stopSelf();
        }
    }

    @Override
    public void onSpeechPause(String text) {

    }

    @Override
    public void onSpeechResume(String text) {

    }

    @Override
    public void onSpeechError(String text, int code) {

    }
}
