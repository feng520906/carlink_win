package com.coopox.common;

import com.min.securereq.Utils;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-1
 */
public class Constants {

    public static final String API_KEY = "txTVHlhOk58rImij6pmO0QMC";

    public static final String SECRET_KEY = "GG1b1sdY27Qs5clO1B7qGr8MmaNbqYwx";

    public static final String ACTION_TAKE_PHOTO = "com.coopox.DrivingRecorder.TakePicture";

    public static final String WECHAT_APP_ID = "wxde120f8289db6df2";
    public static final String IFLYTEK_APPID = "545eb296";
    // 云知声的 APP KEY
    public static final String APPKEY_FOR_HIVOICE = "ap3cen6ktyhm7h274un6ums56lfytikzy2awwnag";
    public static final String SECRET_FOR_HIVOICE = "1f192a89b39fc4655b7d43142d42559b";

    public static final byte[] AESKEY = Utils.hexStringToByteArray("8293A688957F6B21278764CDEC2CACF0");
    public static final byte[] HACKEY = Utils.hexStringToByteArray("DDE79425290B94AF6C6C6CB767A840CD");

    // Key for user register
    public static final String SP_USER_INFO = "UserInformation";
    public static final String KEY_USER_NAME = "kUserName";     // 用户姓名
    public static final String KEY_CAR_PLATE = "kCarPlate";     // 车牌号
    public static final String KEY_CAR_PROVINCE = "kCarProvince";   // 车辆注册地
    public static final String KEY_PROVINCE_SHORT = "kProvinceShort";   // 车牌缩写，如“湘”、“京”
    public static final String KEY_CAR_BRAND = "kCarBrand"; // 汽车品牌
    public static final String KEY_CAR_FAMILY = "kCarFamily";   // 洗车车系
    public static final String KEY_MILEAGE = "kMileage";    // 现有里程数
    public static final String KEY_USER_GENDER = "kUserGender"; // 用户性别，1男，0女
    public static final String KEY_FRAME_NUM = "kFrameNum"; // 车辆识别代号
    public static final String KEY_ENGINE_CODE = "kEngineCode";     // 发动机号
    public static final String KEY_IS_REGISTRY = "kIsRegistry";
    public static final String KEY_TTS_CONTENT = "kTTSContent";
    public static final String KEY_TTS_ALL_CLEAR = "kTTSAllClear";
    public static final String KEY_TTS_ERROR = "kTTSError";

    // Extras for Common Intent
    public static final String EXTRA_PAUSE = "extPause";
    public static final String EXTRA_RESUME = "extResume";
    public static final String EXTRA_TTS_CONTENT = "extContent";
    public static final String EXTRA_TTS_TO_HEAD = "extToHead";
    public static final String EXTRA_STOP = "extStop";
    public static final String EXTRA_FORCE_CANCEL = "extCancel";
    public static final String EXTRA_TTS_RECEIVER = "TTSReceiver";

    // CGI
    public static final String URL_REGISTERY = "http://ecs.4gcar.cn/apis/v1/device/register";

    // ID for IPC message.
    public static final int MSG_TTS_START = 0x1;
    public static final int MSG_TTS_PAUSED = 0x2;
    public static final int MSG_TTS_RESUMED = 0x3;
    public static final int MSG_TTS_STOPPED = 0x4;
    public static final int MSG_TTS_ERROR = 0x5;

    public static final int MILLIS_PER_SECOND = 1000;

    // 智键事件广播
    public final static String ACTION_SMART_KEY_EVENT =
            "com.coopox.SmartKey.smartkey.ACTION_SMART_KEY_EVENT";
}
