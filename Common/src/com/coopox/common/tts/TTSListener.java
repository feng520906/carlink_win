package com.coopox.common.tts;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/11/9
 */
public interface TTSListener {
    void onSpeechStart(String text);
    /**
     * @allClear - 所有语音都播放完成 */
    void onSpeechStop(String text, boolean allClear);
    void onSpeechPause(String text);
    void onSpeechResume(String text);
    void onSpeechError(String text, int code);
}
