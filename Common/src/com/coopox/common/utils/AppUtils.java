package com.coopox.common.utils;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.widget.Toast;
import com.coopox.common.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/10/20
 */
public class AppUtils {
    private static Method sForceStopPackageMethod;

    // 启动 Activity
    public static boolean startActivity(Context context, Intent intent) {
        if (null != context && null != intent) {
            try {
                context.startActivity(intent);
                if (null != intent.getComponent()) {
                    EventReporter.INSTANCE.report("StartActivity",
                            intent.getComponent().getClassName());
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, R.string.app_not_install, Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    // 检查应用是否安装
    public static boolean checkPackageInstallation(Context context, String packageName) {
        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
        if (!Checker.isEmpty(packages)) {
            for (PackageInfo packageInfo : packages) {
                if (packageInfo.packageName.equals(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 结束应用的所有进程
    // 需要权限: android.permission.FORCE_STOP_PACKAGES
    public static int stopAppByIntent(Context context, Intent intent) {
        int ret = -1;
        if (null == context || null == intent) return ret;

        ComponentName componentName = intent.getComponent();
        if (null != componentName) {
            String packageName = componentName.getPackageName();
            if (!Checker.isEmpty(packageName)) {
                return forceStopPackage(context, packageName) ? 0 : ret;
            } else {
                // Invalid package info.
                ret = -2;
            }
        }
        return ret;
    }

    public static boolean forceStopPackage(Context context, String packageName) {
        if (null == context || Checker.isEmpty(packageName)) return false;

        if (null == sForceStopPackageMethod) {
            try {
                sForceStopPackageMethod =
                        ActivityManager.class.getMethod("forceStopPackage", String.class);
                if (!sForceStopPackageMethod.isAccessible()) {
                    sForceStopPackageMethod.setAccessible(true);
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        if (null != sForceStopPackageMethod) {
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            try {
                sForceStopPackageMethod.invoke(activityManager, packageName);
                return true;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
