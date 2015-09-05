package com.coopox.common.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/18
 */
public class NetworkUtils {
    public static String getWlanMacAddress(Context context) {
        String mac = "";
        if (null != context) {
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = wifi.getConnectionInfo();
            mac = info.getMacAddress();
        }
        return mac;
    }

    public static String getIMEI(Context context) {
        String IMEI = "";
        if (null != context) {
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (null != telephonyManager) {
                IMEI = telephonyManager.getDeviceId();
            }
        }
        return IMEI;
    }
}
