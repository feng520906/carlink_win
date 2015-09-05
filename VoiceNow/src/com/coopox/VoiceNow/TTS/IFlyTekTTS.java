package com.coopox.VoiceNow.TTS;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;
import com.coopox.common.utils.AppUtils;
import com.iflytek.speech.*;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/1/25
 */
public class IFlyTekTTS extends TTSPlayer {
    private static final String TAG = "IFlyTekTTS";

    private static final String IFLYTEK_PACKAGE = "com.iflytek.speechcloud";
    private SpeechSynthesizer mSpeechSynthesizer;
    private SharedPreferences mSharedPreferences;

    @Override
    public boolean setup(Context context) {
        String packageName = IFLYTEK_PACKAGE;
        // 检查讯飞语音+是否已经安装
        if (AppUtils.checkPackageInstallation(context, packageName)) {
            mSharedPreferences =
                    context.getSharedPreferences("LocalTTS", Context.MODE_PRIVATE);
            mSpeechSynthesizer = new SpeechSynthesizer(context, new InitListener() {
                @Override
                public void onInit(ISpeechModule iSpeechModule, int i) {
                    setParam();
                }
            });

            return true;
        }
        return false;
    }

    @Override
    public void teardown() {
        super.teardown();
        mSpeechSynthesizer.stopSpeaking(mTtsListener);
        // 退出时释放连接
        mSpeechSynthesizer.destory();
    }

    /**
     * 参数设置
     */
    private void setParam(){
        mSpeechSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE,
                mSharedPreferences.getString("engine_preference", "local"));

        if(mSharedPreferences.getString("engine_preference", "local").equalsIgnoreCase("local")){
            mSpeechSynthesizer.setParameter(SpeechSynthesizer.VOICE_NAME,
                    mSharedPreferences.getString("role_cn_preference", "xiaoyan"));
        }else{
            mSpeechSynthesizer.setParameter(SpeechSynthesizer.VOICE_NAME,
                    mSharedPreferences.getString("role_cn_preference", "xiaoyan"));
        }
        mSpeechSynthesizer.setParameter(SpeechSynthesizer.SPEED,
                mSharedPreferences.getString("speed_preference", "50"));

        mSpeechSynthesizer.setParameter(SpeechSynthesizer.PITCH,
                mSharedPreferences.getString("pitch_preference", "50"));

        mSpeechSynthesizer.setParameter(SpeechSynthesizer.VOLUME,
                mSharedPreferences.getString("volume_preference", "50"));
    }

    @Override
    public boolean speak(String content) {
        int code = mSpeechSynthesizer.startSpeaking(content, mTtsListener);
        if (code != 0) {
            Log.w(TAG, "start speak error : " + code);
            return false;
        }
        Log.w(TAG, "start speak success.");
        return true;
    }

    @Override
    public void pause() {
        mSpeechSynthesizer.pauseSpeaking(mTtsListener);
    }

    @Override
    public void resume() {
        mSpeechSynthesizer.resumeSpeaking(mTtsListener);
    }

    @Override
    public void cancel() {
        mSpeechSynthesizer.stopSpeaking(mTtsListener);
    }

    @Override
    public boolean isSpeaking() {
        return mSpeechSynthesizer.isSpeaking();
    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener.Stub() {
        @Override
        public void onBufferProgress(int progress) throws RemoteException {
//            Log.d(TAG, "onBufferProgress :" + progress);
        }

        @Override
        public void onCompleted(int code) throws RemoteException {
            Log.d(TAG, "onCompleted code =" + code);

            TTSRawListener listener = getTTSListener();
            if (null != listener) {
                if (code == ErrorCode.SUCCESS) {
                    listener.onSpeechStop();
                } else {
                    listener.onSpeechError(code);
                }
            }
        }

        @Override
        public void onSpeakBegin() throws RemoteException {
            Log.d(TAG, "onSpeakBegin");

            TTSRawListener listener = getTTSListener();
            if (null != listener) {
                listener.onSpeechStart();
            }
        }

        @Override
        public void onSpeakPaused() throws RemoteException {
            Log.d(TAG, "onSpeakPaused.");
            TTSRawListener listener = getTTSListener();
            if (null != listener) {
                listener.onSpeechPause();
            }
        }

        @Override
        public void onSpeakProgress(int progress) throws RemoteException {
//            Log.d(TAG, "onSpeakProgress :" + progress);
        }

        @Override
        public void onSpeakResumed() throws RemoteException {
            Log.d(TAG, "onSpeakResumed.");
            TTSRawListener listener = getTTSListener();
            if (null != listener) {
                listener.onSpeechResume();
            }
        }
    };
}
