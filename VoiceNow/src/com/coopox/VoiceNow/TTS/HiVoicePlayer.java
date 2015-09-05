package com.coopox.VoiceNow.TTS;

import android.content.Context;
import android.util.Log;
import cn.yunzhisheng.common.USCError;
import cn.yunzhisheng.tts.online.basic.OnlineTTS;
import cn.yunzhisheng.tts.online.basic.TTSPlayerListener;
import com.coopox.common.Constants;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/13
 */
public class HiVoicePlayer extends TTSPlayer implements TTSPlayerListener {

    private static final String TAG = "HiVoicePlaer";

    private OnlineTTS mTTSPlayer;
    private boolean mIsSpeaking;

    @Override
    public boolean setup(Context context) {
        mTTSPlayer = new OnlineTTS(context, Constants.APPKEY_FOR_HIVOICE);
        mTTSPlayer.setTTSListener(this);
        return true;
    }

    @Override
    public boolean speak(String content) {
        mTTSPlayer.play(content);
        return true;
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {

    }

    @Override
    public void cancel() {
        mTTSPlayer.stop();
    }

    @Override
    public boolean isSpeaking() {
        return mIsSpeaking;
    }

    @Override
    public void onPlayBegin() {
        mIsSpeaking = true;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechStart();
        }
    }

    @Override
    public void onBuffer() {

    }

    @Override
    public void onEnd(USCError uscError) {
        if (null != uscError) {
            TTSRawListener listener = getTTSListener();
            if (null != listener) {
                Log.e(TAG, "TTS Error " + uscError.toString());
                listener.onSpeechError(uscError.code);
            }
        }
    }

    @Override
    public void onPlayEnd() {
        mIsSpeaking = false;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechStop();
        }
    }
}
