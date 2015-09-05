package com.coopox.common.utils;

import android.content.Context;
import android.telephony.TelephonyManager;
import com.google.gson.JsonObject;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/8
 */
public class HttpRequestUtils {
    private static volatile String IMEI = null;

    public static JsonObject createCommonJsonPostBody(Context context) {
        if (null == IMEI && null != context) {
//            IMEI = NetworkUtils.getIMEI(context);
            // 因为设备尚未入网没有 IMEI，暂时用 MAC 地址代替。
            IMEI = NetworkUtils.getWlanMacAddress(context);
        }

        JsonObject params = new JsonObject();
        params.addProperty("IMEI", null != IMEI ? IMEI : "");
        params.addProperty("SERIAL", null != android.os.Build.SERIAL ? android.os.Build.SERIAL : "");
        params.addProperty("ts", System.currentTimeMillis());

        return params;
    }

    public static JSONObject createCommonJSONPostBody(Context context) {
        if (null == IMEI) {
            if (null == IMEI && null != context) {
                TelephonyManager telephonyManager =
                        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (null != telephonyManager) {
                    IMEI = telephonyManager.getDeviceId();
                }
            }
        }

        JSONObject params = new JSONObject();
        try {
            params.put("IMEI", null == IMEI ? "" : IMEI);
            params.put("ts", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return params;
    }
}
