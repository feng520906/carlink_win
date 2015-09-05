package com.coopox.carlauncher.misc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-15
 */

public class ActivityInfoPicker {
    // 获取 Activity 名称
    public static String getActivityName(Context context, ResolveInfo info) {
        if (null != context && null != info) {
            PackageManager pm = context.getPackageManager();
            String name = info.loadLabel(pm).toString();
            if (name == null) {
                name = info.activityInfo.name;
            }
            return name;
        }
        return null;
    }

    // 获取 Activity 图标
    public static Bitmap getActivityIcon(Context context, String uri) {
        try {
            Intent intent = Intent.parseUri(uri, 0);
            return getActivityIcon(context, intent);
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getActivityIcon(Context context, Intent intent) {
        PackageManager pm = context.getPackageManager();
        final ResolveInfo resolveInfo = pm.resolveActivity(intent, 0);
        return getActivityIcon(context, resolveInfo);
    }

    public static Bitmap getActivityIcon(Context context, ResolveInfo info) {
        if (null != context && null != info) {
            PackageManager pm = context.getPackageManager();
            Bitmap icon = null;
            try {
                icon = BitmapHelper.getBitmapFromDrawable(info.activityInfo.loadIcon(pm));
            } catch (Exception e) {
                e.printStackTrace();
            }
            return icon;
        }
        return null;
    }

    /**
     * Query the package manager for MAIN/LAUNCHER activities in the supplied package.
     */
    public static List<ResolveInfo> findMainActivitiesForPackage(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        final List<ResolveInfo> matches = new ArrayList<ResolveInfo>();

        if (apps != null) {
            // Find all activities that match the packageName
            int count = apps.size();
            for (int i = 0; i < count; i++) {
                final ResolveInfo info = apps.get(i);
                final ActivityInfo activityInfo = info.activityInfo;
                if (packageName.equals(activityInfo.packageName)) {
                    matches.add(info);
                }
            }
        }

        return matches;
    }
}
