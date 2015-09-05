package com.coopox.VoiceNow.TTS;

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/1/25
 */
public abstract class TTSPlayer {
    private WeakReference<TTSRawListener> mListenerRef;

    public abstract boolean setup(Context context);

    public void teardown() {

    }

    // 播放 content 合成的语音
    public abstract boolean speak(String content);

    // 暂停播放
    public abstract void pause();

    // 恢复播放
    public abstract void resume();

    // 停止播放
    public abstract void cancel();

    public abstract boolean isSpeaking();

    public void setTTSListener(TTSRawListener listener) {
        mListenerRef = new WeakReference<TTSRawListener>(listener);
    }

    public TTSRawListener getTTSListener() {
        return null != mListenerRef ? mListenerRef.get() : null;
    }
}
