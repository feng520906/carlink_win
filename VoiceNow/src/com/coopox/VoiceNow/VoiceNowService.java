package com.coopox.VoiceNow;

import android.app.IntentService;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import com.baidu.voicerecognition.android.Candidate;
import com.baidu.voicerecognition.android.VoiceRecognitionClient;
import com.baidu.voicerecognition.android.VoiceRecognitionClient.VoiceClientStatusChangeListener;
import com.baidu.voicerecognition.android.VoiceRecognitionConfig;
import com.coopox.common.Constants;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-1
 */
public class VoiceNowService extends IntentService {
    private static final String TAG = "VoiceNowService";
    private VoiceRecognitionConfig mConfig;
    private CmdDispatcher mCmdDispatcher;

    public VoiceNowService() {
        this(TAG);
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public VoiceNowService(String name) {
        super(name);
        mCmdDispatcher = new CmdDispatcher(this);
    }

    private VoiceRecognitionClient mASREngine;

    /**
     * 正在识别中
     */
    private boolean isRecognition = false;

    /**
     * 音量更新间隔
     */
    private static final int POWER_UPDATE_INTERVAL = 100;

    /**
     * 识别回调接口
     */
    private final MyVoiceRecogListener mListener = new MyVoiceRecogListener();

    @Override
    public void onCreate() {
        super.onCreate();

        mASREngine = VoiceRecognitionClient.getInstance(this);
        mASREngine.setTokenApis(Constants.API_KEY, Constants.SECRET_KEY);
        uploadContacts();

        mConfig = new VoiceRecognitionConfig();
        mConfig.setLanguage(VoiceRecognitionConfig.LANGUAGE_CHINESE);
        mConfig.enableNLU();
        mConfig.setProp(VoiceRecognitionConfig.PROP_MAP);
        mConfig.enableVoicePower(true); // 音量反馈。
        mConfig.enableBeginSoundEffect(R.raw.bdspeech_recognition_start); // 设置识别开始提示音
        mConfig.enableEndSoundEffect(R.raw.bdspeech_speech_end); // 设置识别结束提示音
        mConfig.setSampleRate(VoiceRecognitionConfig.SAMPLE_RATE_8K); // 设置采样率,需要与外部音频一致
//        mConfig.enableContacts(); // 启用通讯录，前提是要先上传通讯录
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (null == mASREngine || null == mListener || null == mConfig) {
            Log.w(TAG, "VoiceNow Service not initialized!!");
            return;
        }

        // 下面发起识别
        int code = mASREngine.startVoiceRecognition(mListener, mConfig);
        if (code != VoiceRecognitionClient.START_WORK_RESULT_WORKING) {
            Log.e(TAG, String.format("Voice Recognition Failed(%d)", code));
        } else {
            Log.i(TAG, "Voice Recognition Started.");

            synchronized (mListener) {
                try {
                    mListener.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.i(TAG, "Voice Recognition End.");
        }
    }

    @Override
    public void onDestroy() {
        VoiceRecognitionClient.releaseInstance();
        super.onDestroy();
    }

    /**
     * 重写用于处理语音识别回调的监听器
     */
    class MyVoiceRecogListener implements VoiceClientStatusChangeListener {

        @Override
        public void onClientStatusChange(int status, Object obj) {
            switch (status) {
                // 语音识别实际开始，这是真正开始识别的时间点，需在界面提示用户说话。
                case VoiceRecognitionClient.CLIENT_STATUS_START_RECORDING:
                    isRecognition = true;
//                    mHandler.removeCallbacks(mUpdateVolume);
//                    mHandler.postDelayed(mUpdateVolume, POWER_UPDATE_INTERVAL);
//                    mControlPanel.statusChange(ControlPanelFragment.STATUS_RECORDING_START);
                    break;
                case VoiceRecognitionClient.CLIENT_STATUS_SPEECH_START: // 检测到语音起点
//                    mControlPanel.statusChange(ControlPanelFragment.STATUS_SPEECH_START);
                    break;
                // 已经检测到语音终点，等待网络返回
                case VoiceRecognitionClient.CLIENT_STATUS_SPEECH_END:
//                    mControlPanel.statusChange(ControlPanelFragment.STATUS_SPEECH_END);
                    break;
                // 语音识别完成，显示obj中的结果
                case VoiceRecognitionClient.CLIENT_STATUS_FINISH:
//                    mControlPanel.statusChange(ControlPanelFragment.STATUS_FINISH);
                    isRecognition = false;
                    updateRecognitionResult(obj);
                    synchronized (mListener) {
                        mListener.notify();
                    }
                    Log.i(TAG, "Voice Recognition Finish.");
                    break;
                // 处理连续上屏
                case VoiceRecognitionClient.CLIENT_STATUS_UPDATE_RESULTS:
//                    updateRecognitionResult(obj);
                    break;
                // 用户取消
                case VoiceRecognitionClient.CLIENT_STATUS_USER_CANCELED:
//                    mControlPanel.statusChange(ControlPanelFragment.STATUS_FINISH);
                    isRecognition = false;
                    synchronized (mListener) {
                        mListener.notify();
                    }
                    Log.i(TAG, "Voice Recognition Canceled.");
                    break;
                default:
                    break;
            }

        }

        @Override
        public void onError(int errorType, int errorCode) {
//            TTSManager.INSTANCE.enqueueText("语音识别失败，请再试一次。");
            isRecognition = false;
            synchronized (mListener) {
                mListener.notify();
            }
            Log.i(TAG, String.format("Voice Recognition Error(%x, %x).", errorType, errorCode));
//            mResult.setText(getString(R.string.error_occur, Integer.toHexString(errorCode)));
//            mControlPanel.statusChange(ControlPanelFragment.STATUS_FINISH);
        }

        @Override
        public void onNetworkStatusChange(int status, Object obj) {
            // 这里不做任何操作不影响简单识别
        }
    }

    /**
     * 将识别结果更新到UI上，搜索模式结果类型为List<String>,输入模式结果类型为List<List<Candidate>>
     *
     * @param result
     */
    private void updateRecognitionResult(Object result) {
        String str = null;
        if (result != null && result instanceof List) {
            List results = (List) result;
            if (results.size() > 0) {
                if (results.get(0) instanceof List) {
                    List<List<Candidate>> sentences = (List<List<Candidate>>) result;
                    StringBuffer sb = new StringBuffer();
                    for (List<Candidate> candidates : sentences) {
                        if (candidates != null && candidates.size() > 0) {
                            sb.append(candidates.get(0).getWord());
                        }
                    }
                    str = sb.toString();
                    Log.i(TAG, String.format("List<List> %s", str));
                } else {
                    str = results.get(0).toString();
                    Log.i(TAG, str);
                }
            }

            if (null != str) {
                try {
                    Gson gson = new Gson();
                    JSONObject jsonObject = new JSONObject(str);
                    String queryString = jsonObject.getString("json_res");
                    String[] backup = gson.fromJson(jsonObject.optString("item"), String[].class);
                    String ret = null;
                    if (!TextUtils.isEmpty(queryString)) {
                        QueryInfo queryInfo = gson.fromJson(queryString, QueryInfo.class);
                        ret = mCmdDispatcher.routeCmd(queryInfo, backup); // TODO: 提取 item 字段作为备选
                    } else {
                        ret = mCmdDispatcher.routeCmd(null, backup); // TODO: 提取 item 字段作为备选
                    }
                    if (!TextUtils.isEmpty(ret)) {
//                        TTSManager.INSTANCE.enqueueText(ret);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 上传通讯录
     * */
    private void uploadContacts(){
/*        DataUploader dataUploader = new DataUploader(this);
        dataUploader.setApiKey(Constants.API_KEY, Constants.SECRET_KEY);

        String jsonString = "[{\"name\":\"兆维\", \"frequency\":1}, {\"name\":\"林新汝\", \"frequency\":2}]";
        try{
            dataUploader.uploadContactsData(jsonString.getBytes("utf-8"));
        }catch (Exception e){
            e.printStackTrace();
        }*/
    }
}
