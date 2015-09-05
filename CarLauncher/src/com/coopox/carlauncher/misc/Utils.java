package com.coopox.carlauncher.misc;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import com.coopox.carlauncher.datamodel.AppEntry;
import com.coopox.common.utils.AppUtils;
import com.umeng.analytics.MobclickAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-5
 */
public class Utils {

    private static Map<String, String> mAppInfo = new HashMap<String, String>();

    public static String getMetaValue(Context context, String metaKey) {
        Bundle metaData = null;
        String apiKey = null;
        if (context == null || metaKey == null) {
            return null;
        }
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (null != ai) {
                metaData = ai.metaData;
            }
            if (null != metaData) {
                apiKey = metaData.getString(metaKey);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return apiKey;
    }

    public static void startAppEntry(Context context, AppEntry entry) {
        if (null != entry) {
            if (AppUtils.startActivity(context, entry.intent)) {
                mAppInfo.clear();
                mAppInfo.put(StatisticEvent.KEY_APP_NAME, entry.name);
                MobclickAgent.onEvent(context, StatisticEvent.OPEN_APP, mAppInfo);
                Log.i(StatisticEvent.OPEN_APP, entry.intent.toURI());
            }
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        return (null != networkInfo && networkInfo.isAvailable());
    }
}
