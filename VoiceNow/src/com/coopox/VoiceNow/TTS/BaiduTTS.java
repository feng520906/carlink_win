package com.coopox.VoiceNow.TTS;

import android.content.Context;
import com.baidu.speechsynthesizer.SpeechSynthesizer;
import com.baidu.speechsynthesizer.SpeechSynthesizerListener;
import com.baidu.speechsynthesizer.publicutility.SpeechError;
import com.baidu.speechsynthesizer.publicutility.SpeechLogger;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/1/25
 */
public class BaiduTTS extends TTSPlayer implements SpeechSynthesizerListener {
    private SpeechSynthesizer mSpeechSynthesizer;
    private volatile boolean mIsSpeaking;

    @Override
    public boolean setup(Context context) {
        mSpeechSynthesizer = new SpeechSynthesizer(context, "holder", this);
        // 注:your-apiKey 和 your-secretKey 需要换成在百度开发者中心注册应用得到的对应值
        mSpeechSynthesizer.setApiKey("txTVHlhOk58rImij6pmO0QMC", "GG1b1sdY27Qs5clO1B7qGr8MmaNbqYwx");

        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "10");
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER,
                SpeechSynthesizer.SPEAKER_FEMALE);
        SpeechLogger.setLogLevel(SpeechLogger.SPEECH_LOG_LEVEL_INFO);
        return true;
    }

    @Override
    public void teardown() {
        super.teardown();
        cancel();
    }

    @Override
    public boolean speak(String content) {
        return 0 == mSpeechSynthesizer.speak(content);
    }

    @Override
    public void pause() {
        mSpeechSynthesizer.pause();
    }

    @Override
    public void resume() {
        mSpeechSynthesizer.resume();
    }

    @Override
    public void cancel() {
        mSpeechSynthesizer.cancel();
    }

    @Override
    public boolean isSpeaking() {
        return mIsSpeaking;
    }

    @Override
    public void onStartWorking(SpeechSynthesizer speechSynthesizer) {

    }

    @Override
    public void onSpeechStart(SpeechSynthesizer speechSynthesizer) {
        mIsSpeaking = true;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechStart();
        }
    }

    @Override
    public void onNewDataArrive(SpeechSynthesizer speechSynthesizer, byte[] bytes, int i) {

    }

    @Override
    public void onBufferProgressChanged(SpeechSynthesizer speechSynthesizer, int i) {

    }

    @Override
    public void onSpeechProgressChanged(SpeechSynthesizer speechSynthesizer, int i) {

    }

    @Override
    public void onSpeechPause(SpeechSynthesizer speechSynthesizer) {
        mIsSpeaking = false;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechPause();
        }
    }

    @Override
    public void onSpeechResume(SpeechSynthesizer speechSynthesizer) {
        mIsSpeaking = true;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechResume();
        }
    }

    @Override
    public void onCancel(SpeechSynthesizer speechSynthesizer) {
        mIsSpeaking = false;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechStop();
        }
    }

    @Override
    public void onSpeechFinish(SpeechSynthesizer speechSynthesizer) {
        mIsSpeaking = false;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechStop();
        }
    }

    @Override
    public void onError(SpeechSynthesizer speechSynthesizer, SpeechError speechError) {
        mIsSpeaking = false;
        TTSRawListener listener = getTTSListener();
        if (null != listener) {
            listener.onSpeechError(speechError.errorCode);
        }
    }
}
