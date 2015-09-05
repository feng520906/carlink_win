package com.coopox.VoiceNow;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;
import cn.yunzhisheng.common.USCError;
import cn.yunzhisheng.nlu.basic.USCSpeechUnderstander;
import cn.yunzhisheng.nlu.basic.USCSpeechUnderstanderListener;
import cn.yunzhisheng.understander.USCUnderstanderResult;
import cn.yunzhisheng.wakeup.basic.WakeUpRecognizer;
import cn.yunzhisheng.wakeup.basic.WakeUpRecognizerListener;
import com.baidu.voicerecognition.android.Candidate;
import com.baidu.voicerecognition.android.VoiceRecognitionClient;
import com.coopox.VoiceNow.TTS.HiVoiceOfflinePlayer;
import com.coopox.VoiceNow.TTS.TTSPlayer;
import com.coopox.VoiceNow.TTS.TTSRawListener;
import com.coopox.common.Constants;
import com.coopox.common.utils.*;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/11/7
 */
public class VoiceNowServiceEx extends Service implements TTSRawListener {
    private static final String TAG = "VoiceNowServiceEx";

    private static final int LOCAL_NLU_PARSER = 1;
    private static final int ONLINE_NLU_PARSER = 2;

    private CmdDispatcher mCmdDispatcher;
    private static final SparseArray<String> sErrorMsg;
    static {
        sErrorMsg = new SparseArray<String>();
        sErrorMsg.put(-10001, "服务器通讯错误");
        sErrorMsg.put(-10002, "服务器连接失败");
        sErrorMsg.put(-20001, "服务器验证错误");
        sErrorMsg.put(-30002, "你说的太复杂了，我理解不了");
        sErrorMsg.put(-30003, "数据压缩错误");
        sErrorMsg.put(-30009, "对不起，我没听清楚"); // 自定义
        sErrorMsg.put(-61001, "无法开始录音");

        sErrorMsg.put(-61002, "录音出现异常");
        sErrorMsg.put(-62001, "语音识别异常");

        sErrorMsg.put(-63001, "上传个性化数据被服务器拒绝");
        sErrorMsg.put(-63002, "上传个性化数据网络连接失败");
        sErrorMsg.put(-63003, "上传个性化数据不能为空");
        sErrorMsg.put(-63007, "上传的个性化数据的内容太多");
        sErrorMsg.put(-63009, "上传个性化数据过于频繁");
        sErrorMsg.put(-63010, "上传个性化数据编码失败");

        sErrorMsg.put(-63011, "上传场景数据被服务器拒绝");
        sErrorMsg.put(-63012, "上传场景数据网络连接失败");
        sErrorMsg.put(-63013, "上传场景数据不能为空");
        sErrorMsg.put(-63017, "上传场景数据的内容太多");
        sErrorMsg.put(-63019, "上传场景数据编码失败");
        sErrorMsg.put(-63020, "上传场景数据过于频繁");

        sErrorMsg.put(-101, "语音播放异常");

        sErrorMsg.put(-400, "App Key 验证不通过");
        sErrorMsg.put(-401, "App Key 验证不通过");
        sErrorMsg.put(-402, "服务器出错");
        sErrorMsg.put(-403, "语音合成文本太长");
        sErrorMsg.put(-404, "服务器认证失败");
        sErrorMsg.put(-405, "服务器繁忙");
    }

    private TTSPlayer mTTSPlayer;
    private Toast mToast;
//    private LocalNLUParser mLocalNLUParser;
    private WakeUpRecognizer mWakeUpRecognizer;
    private USCSpeechUnderstander mUnderstander;
    private String mLastRecognizerResult;

    public VoiceNowServiceEx() {
        super();
        mCmdDispatcher = new CmdDispatcher(this);
    }

    private VoiceStateMachine mStateMachine = new VoiceStateMachine("VoiceStateMachine");

    @Override
    public void onSpeechStart() {
        Log.d(TAG, "TTS speak start");
    }

    @Override
    public void onSpeechStop() {
        Log.d(TAG, "TTS speak finish");
        mStateMachine.sendMessage(VoiceStateMachine.CMD_SPEECH_OVER);
    }

    @Override
    public void onSpeechPause() {

    }

    @Override
    public void onSpeechResume() {

    }

    @Override
    public void onSpeechError(int code) {
        Log.d(TAG, "TTS speak error");
    }

    class VoiceStateMachine extends StateMachine {
        public static final int CMD_NOT_INIT = 1;
        public static final int CMD_ENV_ERROR = 2;
        public static final int CMD_START = 3;
        public static final int CMD_OVER = 4;
        public static final int CMD_CANCEL = 5;
        public static final int CMD_REC_FAILED = 6;
        public static final int CMD_GET_CIPHER = 7;
        public static final int CMD_GET_QUERY = 8;
        public static final int CMD_RAW_CONTENT = 9;
        private static final int CMD_TIMEOUT = 10;
        public static final int CMD_SPEECH_OVER = 11;
        private static final int CMD_WAITING = 12;
        private static final int CMD_READY = 13;
        private static final int CMD_REC_TIMEOUT = 14;
        private static final int CMD_REC_READY = 15;
        private static final int CMD_RECOGNITION = 16;
        public static final int CMD_DAEMON_FAILED = 17;

        protected VoiceStateMachine(String name) {
            super(name, Looper.getMainLooper());
            addState(mDaemonState);
            addState(mRecognitionState);
            addState(mRouteCmdState);
            addState(mDisableState);

            setInitialState(mDaemonState);
        }

        @Override
        protected void haltedProcessMessage(Message msg) {
            super.haltedProcessMessage(msg);
        }

        private RecognitionState mRecognitionState = new RecognitionState();
        private RouteCmdState mRouteCmdState = new RouteCmdState();
        private DaemonState mDaemonState = new DaemonState();
        private TemporaryDisableState mDisableState = new TemporaryDisableState();

        class StateImpl extends State {
            @Override
            public void enter() {
                Log.d("SM", String.format("%s.enter", getClass().getSimpleName()));
            }

            @Override
            public void exit() {
                Log.d("SM", String.format("%s.exit", getClass().getSimpleName()));
            }
        }

        // 监听激活指令状态
        class DaemonState extends StateImpl {
            @Override
            public void enter() {
                super.enter();
                if (null != mWakeUpRecognizer) {
                    showTip("离线激活启用");
                    mWakeUpRecognizer.start();
                } else {
                    showTip("离线激活模块尚未初始化！");
                }
            }

            @Override
            public void exit() {
                super.exit();
                if (null != mWakeUpRecognizer) {
                    showTip("离线激活禁用");
                    mWakeUpRecognizer.cancel();
                }
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case CMD_NOT_INIT:
                        transitionToHaltingState();
                        return HANDLED;
                    case CMD_OVER:
                        updateRecognitionResult(msg.obj);
                        return HANDLED;

                    case CMD_GET_CIPHER:
                        speak("收到，请指示");
                        return HANDLED;

                    case CMD_RECOGNITION:
                    case CMD_SPEECH_OVER:
                        transitionTo(mRecognitionState);
                        return HANDLED;

                    case CMD_ENV_ERROR: // 在线识别启动出错（本地识别不可用时）
                    transitionTo(mDisableState);
                        return HANDLED;

                    case CMD_DAEMON_FAILED:// 本地识别启动出错
                    case CMD_REC_FAILED:// 在线识别结果出错
                        return HANDLED;
                }
                return super.processMessage(msg);
            }
        }

        // 语音指令识别状态
        class RecognitionState extends StateImpl {

            @Override
            public void enter() {
                super.enter();
                sendMessage(VoiceStateMachine.CMD_REC_READY);
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case CMD_ENV_ERROR:
                        transitionTo(mDisableState);
                        return HANDLED;

                    case CMD_OVER:
                        updateRecognitionResult(msg.obj);
                        return HANDLED;

                    case CMD_GET_QUERY:
                        deferMessage(msg);
                        transitionTo(mRouteCmdState);
                        return HANDLED;

                    case CMD_RAW_CONTENT:
                        deferMessage(msg);
                        transitionTo(mRouteCmdState);
                        return HANDLED;

                    case CMD_REC_FAILED:
                        if (0 != msg.arg1) {
                            String errMsg = sErrorMsg.get(msg.arg1);
                            if (null == errMsg) {
                                errMsg = "出现未知错误";
                            }
                            if (!speak(errMsg)) {
                                transitionTo(mDaemonState);
                            }

                            return HANDLED;
                        }
                        return HANDLED;

                    case CMD_REC_READY:
//                        if (TTSManager.INSTANCE.isSpeaking()) {
////                            sendMessageDelayed(VoiceStateMachine.CMD_REC_READY, RETRY_DELAY);
//                        } else {
                            startRecognition();
//                        }
                        return HANDLED;

                    case CMD_REC_TIMEOUT:
                    case CMD_SPEECH_OVER:
                        transitionTo(mDaemonState);
                        return HANDLED;
                }

                return super.processMessage(msg);
            }
        }

        // 语音指令处理状态
        class RouteCmdState extends StateImpl {
            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case CMD_GET_QUERY:
                        if (msg.obj instanceof NLUQuery) {
                            NLUQuery queryInfo = (NLUQuery) msg.obj;
                            String result = mCmdDispatcher.routeCmd(queryInfo);
                            if (null != result) {
                                if (!speak(result)) {
                                    transitionTo(mDaemonState);
                                }
                            } else {
                                if (!speak("对不起，我不懂你的意思")) {
                                    transitionTo(mDaemonState);
                                }
                            }
                        } else {
                            transitionTo(mDaemonState);
                        }
                        return HANDLED;

                    case CMD_RAW_CONTENT:
                        if (msg.obj.getClass().isArray()) {
                            String result = mCmdDispatcher.routeCmd(null, (String[]) msg.obj);

                            boolean speakRet;
                            if (null != result) {
                                speakRet = speak(result);
                            } else {
                                speakRet = speak("对不起，我不懂你的意思");
                            }
                            if (speakRet) return HANDLED;
                        }
                        transitionTo(mDaemonState);
                        return HANDLED;

                    // 播报完指令处理结果后再转到 Idle 状态，这样不会影响 Daemon 状态的音频采集
                    case CMD_TIMEOUT:
                    case CMD_SPEECH_OVER:
                        transitionTo(mDaemonState);
                        return HANDLED;
                }
                return super.processMessage(msg);
            }
        }

        // 无法启动语音识别功能（例如未连接网络）时将延时一段时间再重试，避免过于频繁而消耗资源
        class TemporaryDisableState extends StateImpl {

            public static final int DELAY_MILLIS = 3000;

            @Override
            public void enter() {
                super.enter();
                showTip("暂时无法识别语音，请检查网络。");
                // 延迟一段时间再开始监听，避免消耗资源
                sendMessageDelayed(VoiceStateMachine.CMD_WAITING, DELAY_MILLIS);
            }

            @Override
            public boolean processMessage(Message msg) {
                switch (msg.what) {
                    case CMD_WAITING:
                        transitionTo(mDaemonState);
                        return HANDLED;
                }

                return super.processMessage(msg);
            }
        }
    }

    class VoiceUnderstandListener implements USCSpeechUnderstanderListener {

        @Override
        public void onSpeechStart() {
            //用户开始说话回调
            Log.d(TAG, "User Speech Start");
        }

        @Override
        public void onRecordingStart() {
            //录音设备打开识别开始,用户可以开始说话
            mStateMachine.sendMessage(VoiceStateMachine.CMD_START);
            showTip("开始录音");
        }

        @Override
        public void onRecordingStop() {
            //停止录音,等待识别结果回调
            Log.d(TAG, "Stop Record");
        }

        @Override
        public void onVADTimeout() {
            //用户停止说话回调
            mUnderstander.stop();

        }

        @Override
        public void onRecognizerResult(String s, boolean isLast) {
            //语音识别结果实时返回,保留识别结果组成完整的识别内容。
            if (isLast) {
                mLastRecognizerResult = s;
                showTip("识别到：" + s);
            }
        }

        @Override
        public void onUpdateVolume(int i) {
            //实时返回说话音量 0~100
        }

        @Override
        public void onUnderstanderResult(USCUnderstanderResult uscUnderstanderResult) {
            //语义结果返回
            String result = uscUnderstanderResult.getStringResult();
            if (!Checker.isEmpty(result)) {
                Log.d(TAG, result);
                NLUQuery query = new Gson().fromJson(result, NLUQuery.class);
                if (null != query) {
                    mStateMachine.sendMessage(VoiceStateMachine.CMD_OVER, query);
                }
            }
            showTip("语义解析完成");
        }

        @Override
        public void onEnd(USCError uscError) {
            //语音理解结束
            if (null != uscError) {
                mStateMachine.sendMessage(VoiceStateMachine.CMD_REC_FAILED, uscError.code);
                String result = String.format("语音识别出错(%s, %x).", uscError.msg, uscError.code);
                showTip(result);
            } else {
                // 识别结束但未识别到任何内容（用户未说话）
                if (TextUtils.isEmpty(mLastRecognizerResult)) {
                    mStateMachine.sendMessage(VoiceStateMachine.CMD_REC_FAILED, -30009);
                }
                showTip("语音识别结束");
            }
        }
    }

    private VoiceUnderstandListener mListener = new VoiceUnderstandListener();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Start foreground service to avoid unexpected kill
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("语音控制服务运行中……")
                .setContentText("")
                .setSmallIcon(R.drawable.ic_launcher)
                .build();
        startForeground(1001, notification);

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        initWakeUp();

        mTTSPlayer = new HiVoiceOfflinePlayer();
        mTTSPlayer.setup(this);
        mTTSPlayer.setTTSListener(this);

        mUnderstander = new USCSpeechUnderstander(this,
                Constants.APPKEY_FOR_HIVOICE,
                Constants.SECRET_FOR_HIVOICE);
        // 车载领域
        mUnderstander.setEngine("poi");
        // 设置等待用户开始说话的超时时间为 3 秒；用户说话停止 2 秒（默认值为1秒太短）后视为说话结束。
        mUnderstander.setVADTimeout(3000, 2000);
        // 车载专属语义解析
//        mUnderstander.setNluScenario("incar");
        mUnderstander.setListener(mListener);

        mStateMachine.start();
    }

    @Override
    public void onDestroy() {
        VoiceRecognitionClient.releaseInstance();

        deinitWakeUp();

        if (null != mTTSPlayer) {
            mTTSPlayer.teardown();
        }

        super.onDestroy();
        Log.d("VoiceNowService", "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (null != intent) {
            boolean willStop = intent.getBooleanExtra("stop", false);
            if (willStop) {
                Log.d(TAG, "Stop VoiceNow by user.");
                stopSelf();
                return START_NOT_STICKY;
            }

            // 收到来自智键的直接激活语音控制指令
            int keycode = intent.getIntExtra("extra_cmd", 0);
            Log.d(TAG, "Receive a event from smart key, code = " + keycode);
            switch (keycode) {
                case 4:
                    if (null != mStateMachine) {
                        mStateMachine.sendMessage(VoiceStateMachine.CMD_RECOGNITION);

                        showTip("智键激活语音控制");
                    }
                    break;

                case 6:
                    if (null != mCmdDispatcher) {
                        NLUQuery query = new Gson().fromJson("{\n" +
                                "    \"rc\": 0,\n" +
                                "    \"text\": \"这是啥歌\",\n" +
                                "    \"service\": \"cn.yunzhisheng.music\",\n" +
                                "    \"code\": \"SEARCH_BILLBOARD\",\n" +
                                "    \"semantic\": {\n" +
                                "        \"intent\": {\n" +
                                "            \"keyword\": \"HOT\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}", NLUQuery.class);
                        mCmdDispatcher.routeCmd(query);

                        showTip("智键打开搜歌");
                    }
                    break;

                case 1:
                    if (null != mCmdDispatcher) {
                        NLUQuery query = new Gson().fromJson("{\n" +
                                "    \"rc\": 0,\n" +
                                "    \"text\": \"拍照\",\n" +
                                "    \"service\": \"cn.yunzhisheng.appmgr\",\n" +
                                "    \"code\": \"APP_LAUNCH\",\n" +
                                "    \"semantic\": {\n" +
                                "        \"intent\": {\n" +
                                "            \"name\": \"拍照\",\n" +
                                "            \"function\": \"FUNC_IMAGE_CAPTURE\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}", NLUQuery.class);
                        mCmdDispatcher.routeCmd(query);

                        showTip("智键控制拍照");
                    }
                    break;

                case 3:
                    if (null != mCmdDispatcher) {
                       NLUQuery query = new Gson().fromJson("{\n" +
                                "    \"rc\": 0,\n" +
                                "    \"text\": \"分享照片\",\n" +
                                "    \"service\": \"cn.yunzhisheng.appmgr\",\n" +
                                "    \"code\": \"APP_LAUNCH\",\n" +
                                "    \"semantic\": {\n" +
                                "        \"intent\": {\n" +
                                "            \"name\": \"照片\",\n" +
                                "            \"function\": \"FUNC_IMAGE_VIEW\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}", com.coopox.VoiceNow.NLUQuery.class);
                        mCmdDispatcher.routeCmd(query);

                        showTip("智键分享照片");
                    }
                    break;
            }

        }
        if (null != mStateMachine) {
            IState state = mStateMachine.getCurrentState();
            if (null != state) {
                showTip(String.format("当前状态：%s", state.getName()));
            }
        }
        return START_STICKY;
    }

    void startRecognition() {
        startOnlineRecognition();
    }

    /**
     * 初始化本地离线唤醒
     */
    private void initWakeUp() {
        showTip("初始化本地离线唤醒模块");

        mWakeUpRecognizer = new WakeUpRecognizer(this, Constants.APPKEY_FOR_HIVOICE);
        mWakeUpRecognizer.setListener(new WakeUpRecognizerListener() {

            @Override
            public void onWakeUpRecognizerStart() {
                showTip("语音唤醒功能已开启");
            }

            @Override
            public void onWakeUpError(USCError error) {
                if (error != null) {
                    showTip("语音唤醒服务异常：" + error.toString());
                }
                mStateMachine.sendMessage(VoiceStateMachine.CMD_DAEMON_FAILED);
            }

            @Override
            public void onWakeUpRecognizerStop() {
                showTip("语音唤醒功能已关闭");
            }

            @Override
            public void onWakeUpResult(boolean succeed, String text, float score) {
                showTip(String.format("Wake Up result: %s, score = %f, %s", text, score, succeed));
                if (succeed) {
                    mStateMachine.sendMessage(VoiceStateMachine.CMD_GET_CIPHER, text);
                } else {
                    mStateMachine.sendMessage(VoiceStateMachine.CMD_DAEMON_FAILED);
                }
            }
        });
    }

    // 释放语音唤醒服务
    private void deinitWakeUp() {
        if (null != mWakeUpRecognizer) {
            mWakeUpRecognizer.stop();
            mWakeUpRecognizer.release();
        }
    }

    private void startOnlineRecognition() {
        Log.d(TAG, "startOnlineRecognition");
        mUnderstander.start();
    }

    /**
     * 将识别结果更新到UI上，搜索模式结果类型为List<String>,输入模式结果类型为List<List<Candidate>>
     *
     * @param result
     */
    private void updateRecognitionResult(Object result) {
        if (null == result) return;
        String str = null;
        if (result instanceof List) { // 处理百度语义解析结果
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
                    String[] backups = gson.fromJson(jsonObject.optString("item"), String[].class);

                    if (!TextUtils.isEmpty(queryString)) {
                        QueryInfo queryInfo = gson.fromJson(queryString, QueryInfo.class);
                        queryInfo.backups = backups;
                        int nluType = 0;

                        // 检查是否存在语义识别结果，若无则采用本地解析逻辑
                        if (Checker.isEmpty(queryInfo.results)) {
/*                            if (null != mLocalNLUParser) {
                                if (mLocalNLUParser.parse(queryInfo, backups)) {
                                    nluType = LOCAL_NLU_PARSER;
                                }
                            }*/
                        } else {
                            nluType = ONLINE_NLU_PARSER;
                        }

                        if (!Checker.isEmpty(queryInfo.results)) {
                            mStateMachine.sendMessage(VoiceStateMachine.CMD_GET_QUERY, queryInfo);

                            QueryInfo.Result r = queryInfo.results[0];
                            showTip(String.format("识别结果：%s。%s语义：%s.%s",
                                    queryInfo.parsedText,
                                    LOCAL_NLU_PARSER == nluType ? "本地" : "在线",
                                    r.domain, r.intent));
                        } else {
                            mStateMachine.sendMessage(VoiceStateMachine.CMD_RAW_CONTENT, backups);

                            if (!Checker.isEmpty(backups)) {
                                showTip(String.format("识别结果：%s。未解析到语义", backups[0]));
                            }
                        }
                    } else {
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else if (result instanceof NLUQuery) {
            NLUQuery query = (NLUQuery)result;
            mStateMachine.sendMessage(VoiceStateMachine.CMD_GET_QUERY, query);
        } else {
            Log.w(TAG, "未知的语义格式！");
            mStateMachine.sendMessage(VoiceStateMachine.CMD_REC_TIMEOUT, null);
        }
    }

    private boolean speak(String text) {
        if (!Checker.isEmpty(text)) {
            if (null != mTTSPlayer && mTTSPlayer.speak(text)) {
                return true;
            }
        }
        return false;
    }

    private void showTip(final String str)
    {
        Log.d(TAG, str);
        ThreadManager.INSTANCE.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                if (null == mToast) {
                    mToast = Toast.makeText(VoiceNowServiceEx.this, "", Toast.LENGTH_SHORT);
                }
                mToast.setText(str);
                mToast.show();
            }
        });
    }
}
