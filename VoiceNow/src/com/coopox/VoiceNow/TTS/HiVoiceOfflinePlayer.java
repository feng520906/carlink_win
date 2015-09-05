package com.coopox.VoiceNow.TTS;

import android.content.Context;
import android.util.Log;
import cn.yunzhisheng.tts.offline.TTSPlayerListener;
import cn.yunzhisheng.tts.offline.basic.ITTSControl;
import cn.yunzhisheng.tts.offline.basic.TTSFactory;
import cn.yunzhisheng.tts.offline.common.USCError;
import com.coopox.common.Constants;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/3/8
 */
public class HiVoiceOfflinePlayer extends TTSPlayer implements TTSPlayerListener {
    private static final String TAG = "HiVoiceOfflinePlayer";
    private boolean mIsInitialized;
    private boolean mIsPlaying;
    private ITTSControl mTTSPlayer;

    @Override
    public boolean setup(Context context) {
        //初始化语音合成控件
        mTTSPlayer = TTSFactory.createTTSControl(context,
                Constants.APPKEY_FOR_HIVOICE); //初始化合成引擎
        mTTSPlayer.init();
        //设置回调监听
        mTTSPlayer.setTTSListener(this);
        mTTSPlayer.setDebug(true);  // Debug 模式会输出相关日志
        return true;
    }

    @Override
    public void teardown() {
        super.teardown();
        mTTSPlayer.release();
    }

    @Override
    public boolean speak(String content) {
        if (mIsInitialized) {
            mTTSPlayer.play(content);
            return true;
        }
        return false;
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void cancel() {
        if (mIsInitialized) {
            mTTSPlayer.cancel();
        }
    }

    @Override
    public boolean isSpeaking() {
        return mIsPlaying;
    }

    @Override
    public void onBuffer() {

    }

    @Override
    public void onPlayBegin() {
        mIsPlaying = true;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechStart();
        }
    }

    @Override
    public void onCancel() {
        mIsPlaying = false;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechStop();
        }
    }

    @Override
    public void onError(USCError uscError) {
        mIsPlaying = false;
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
        mIsPlaying = false;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechStop();
        }
    }

    @Override
    public void onInitFinish() {
        mIsInitialized = true;
    }
}
