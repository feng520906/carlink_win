package com.coopox.VoiceNow.TTS;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 */
public interface TTSRawListener {

    public void onSpeechStart();

    public void onSpeechStop();

    public void onSpeechPause();

    public void onSpeechResume();

    public void onSpeechError(int errCode);
}
