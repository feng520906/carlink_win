package com.coopox.VoiceNow;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RemoteException;
import android.util.Log;
import com.coopox.common.Constants;
import com.coopox.common.utils.AppUtils;
import com.coopox.common.utils.Checker;
import com.iflytek.speech.*;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/11/9
 * 本地语音识别器
 */
public class LocalVoiceRecognizer {
    private final static String TAG = "LocalVoiceRecognizer";
    private static final String KEY_GRAMMAR_ABNF_ID = "grammar_abnf_id";
    private static final String GRAMMAR_TYPE = "abnf";
    private SpeechRecognizer mRecognizer;
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private String mEngineType = "local";
    private String mLocalGrammar;
    private boolean mFirstLaunch = true;

    public LocalVoiceRecognizer(Context context) {
        mContext = context;
    }

    public boolean setup() {
        String packageName = "com.iflytek.speechcloud";
        // 判断手机中是否安装了讯飞语音+
        if (!AppUtils.checkPackageInstallation(mContext, packageName)) {
            Log.e(TAG, "Speech Service Not Installed, local voice recognizer setup failed!");
            return false;
        }

        // 设置在讯飞申请的应用appid
        SpeechUtility.getUtility(mContext).setAppid(Constants.IFLYTEK_APPID);

        mSharedPreferences = mContext.getSharedPreferences(TAG, Context.MODE_PRIVATE);
        // 初始化识别对象
        mRecognizer = new SpeechRecognizer(mContext, mInitListener);
        mLocalGrammar = readFile("call.bnf", "utf-8");

        return true;
    }

    public void teardown() {
        if (null != mRecognizerListener) {
            mRecognizer.cancel(mRecognizerListener);
        }
        mRecognizer.destory();
    }

    public void setListener(RecognizerListener listener) {
        if (listener != mRecognizerListener) {
            if (null != mRecognizerListener && mRecognizer.isListening()) {
                mRecognizer.cancel(mRecognizerListener);
            }
            mRecognizerListener = listener;
        }
    }

    public boolean startRecogition() {
        mRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);

        mRecognizer.setParameter(SpeechConstant.VAD_BOS, "4400");
        mRecognizer.setParameter(SpeechConstant.VAD_EOS, "500");

        mRecognizer.setParameter(SpeechConstant.PARAMS, "local_grammar=voice,mixed_threshold=40");

//        String grammarId = mSharedPreferences.getString(KEY_GRAMMAR_ABNF_ID, null);
//        mRecognizer.setParameter(SpeechRecognizer.CLOUD_GRAMMAR, grammarId);
//        if(Checker.isEmpty(grammarId))
        // 只在启动时构建语法
        if (mFirstLaunch) {
            mFirstLaunch = false;
            buildGrammar();
        }

        int recode = mRecognizer.startListening(mRecognizerListener);
        if (recode != ErrorCode.SUCCESS) {
            showTip("本地识别启动失败：" + recode);
            return false;
        }
        return true;
    }

    private boolean buildGrammar() {
        String grammarContent;
            grammarContent = new String(mLocalGrammar);
            mRecognizer.setParameter(SpeechRecognizer.GRAMMAR_ENCODEING,"utf-8");

        mRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);

        int ret = mRecognizer.buildGrammar(GRAMMAR_TYPE, grammarContent, grammarListener);
        if (ret != ErrorCode.SUCCESS) {
            showTip("语法构建失败：" + ret);
            return false;
        }
        showTip("构建语法成功。");
        return true;
    }
    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(ISpeechModule arg0, int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code == ErrorCode.SUCCESS) {
            }
        }
    };

    private LexiconListener lexiconListener = new LexiconListener.Stub() {

        @Override
        public void onLexiconUpdated(String arg0, int arg1) throws RemoteException {
            if(ErrorCode.SUCCESS != arg1)
                showTip("词典更新失败，错误码："+arg1);
        }
    };

    private GrammarListener grammarListener = new GrammarListener.Stub() {

        @Override
        public void onBuildFinish(String grammarId, int errorCode) throws RemoteException {
            if(errorCode == ErrorCode.SUCCESS){
                String grammarID = new String(grammarId);
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                if(!Checker.isEmpty(grammarId))
                    editor.putString(KEY_GRAMMAR_ABNF_ID, grammarID);
                editor.commit();
                showTip("语法构建成功：" + grammarId);
            }else{
                showTip("语法构建失败，错误码：" + errorCode);
            }
        }
    };

    /**
     * 本地识别回调。
     */
    private RecognizerListener mRecognizerListener = null;
    /**
     * 读取语法文件。
     * @return
     */
    private String readFile(String file,String code)
    {
        int len = 0;
        byte []buf = null;
        String grammar = "";
        try {
            InputStream in = mContext.getAssets().open(file);
            len  = in.available();
            buf = new byte[len];
            in.read(buf, 0, len);

            grammar = new String(buf,code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return grammar;
    }

    private void showTip(final String str)
    {
        Log.d(TAG, str);
/*        mContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });*/
    }
}
