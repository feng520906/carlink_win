package com.coopox.common.tts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import com.coopox.common.Constants;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 */
public class TTSClient {
    /**
     * 马上播报，如果已有语音正在播报则先将其停止
     * 不关心播报状态（如开始、暂停、结束等）*/
    public static void speakNow(Context context, String text) {
        speakNow(context, text, null);
    }

    /**
     * 马上播报，如果已有语音正在播报则先将其停止 */
    public static void speakNow(Context context, String text, TTSListener listener) {
        if (null != context) {
            Intent intent = createCommonIntent(text, listener);
            if (null != intent) {
                intent.putExtra(Constants.EXTRA_FORCE_CANCEL, true);
                context.startService(intent);
            }
        }
    }

    public static void speakAsSoonAsPossible(Context context, String text) {
        speakAsSoonAsPossible(context, text, null);
    }

    /**
     * 尽快播报，如果已有语音正在播报，则在队列中等其完成后立即播报 */
    public static void speakAsSoonAsPossible(Context context, String text, TTSListener listener) {
        if (null != context) {
            Intent intent = createCommonIntent(text, listener);
            if (null != intent) {
                intent.putExtra(Constants.EXTRA_TTS_TO_HEAD, true);
                context.startService(intent);
            }
        }
    }

    public static void speak(Context context, String text) {
        speak(context, text, null);
    }

    /**
     * 在合适时机播报，如果队列前方有其它内容需要播报，则等全部播报完再播报这一条，之后加进来的不影响这一条的播报 */
    public static void speak(Context context, String text, TTSListener listener) {
        if (null != context) {
            Intent intent = createCommonIntent(text, listener);
            if (null != intent) {
                context.startService(intent);
            }
        }
    }

    private static Intent createCommonIntent(String text, TTSListener listener) {
        if (null == text) return null;

        final WeakReference<TTSListener> listenerRef = new WeakReference<TTSListener>(listener);
        Intent intent = new Intent("com.coopox.service.action.TTSService");
        intent.putExtra(Constants.EXTRA_TTS_CONTENT, text);
        intent.putExtra(Constants.EXTRA_TTS_RECEIVER, new ResultReceiver(new Handler(Looper.getMainLooper())) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                TTSListener listener = listenerRef.get();
                if (null != listener) {
                    String text = "";
                    int errCode = 0;
                    boolean allClear = false;
                    if (null != resultData) {
                        text = resultData.getString(Constants.KEY_TTS_CONTENT);
                        errCode = resultData.getInt(Constants.KEY_TTS_ERROR);
                        allClear = resultData.getInt(Constants.KEY_TTS_ALL_CLEAR) == 1;
                    }
                    switch (resultCode) {
                        case Constants.MSG_TTS_START:
                            listener.onSpeechStart(text);
                            break;
                        case Constants.MSG_TTS_PAUSED:
                            listener.onSpeechPause(text);
                            break;
                        case Constants.MSG_TTS_RESUMED:
                            listener.onSpeechResume(text);
                            break;
                        case Constants.MSG_TTS_ERROR:
                            listener.onSpeechError(text, errCode);
                            break;
                        case Constants.MSG_TTS_STOPPED:
                            listener.onSpeechStop(text, allClear);
                            break;
                    }
                }
            }
        });
        return intent;
    }
}
