package com.coopox.carlauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import com.coopox.carlauncher.datamodel.AppEntriesLoader;
import com.coopox.carlauncher.datamodel.AppEntry;
import com.coopox.carlauncher.misc.ActivityInfoPicker;
import com.coopox.common.utils.Checker;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-29
 * Package Manager 监听器，处理安装、卸载、和更新应用的广播。
 */
public class PackageManagerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final String packageName = intent.getData().getSchemeSpecificPart();

        if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
            if (!AppEntriesLoader.INSTANCE.isLoading()) {
                List<AppEntry> appEntries =
                        AppEntriesLoader.INSTANCE.getAppEntriesCache();

                if (null != appEntries) {
                    for (AppEntry entry : appEntries) {
                        if (null != entry) {
                            Intent entryIntent = entry.intent;
                            ComponentName componentName = entryIntent.getComponent();
                            if (Intent.ACTION_MAIN.equals(entryIntent.getAction()) &&
                                    null != componentName &&
                                    packageName.equals(componentName.getPackageName())) {
                                appEntries.remove(entry);
                                AppEntriesLoader.INSTANCE.deliverResult(false);
                                break;
                            }
                        }
                    }

                    return;
                }
            }

            // 正在加载 AppEntry 或者现有的 AppEntry Cache 为空时都会重新加载。
            AppEntriesLoader.INSTANCE.postLoad();
        } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
            if (!AppEntriesLoader.INSTANCE.isLoading()) {
                List<AppEntry> appEntries =
                        AppEntriesLoader.INSTANCE.getAppEntriesCache();
                List<ResolveInfo> infos =
                        ActivityInfoPicker.findMainActivitiesForPackage(context, packageName);
                if (null != appEntries) {
                    if (!Checker.isEmpty(infos)) {
                        for (ResolveInfo info : infos) {
                            AppEntry entry = new AppEntry(context, info);
                            appEntries.add(entry);
                        }
                        AppEntriesLoader.INSTANCE.deliverResult(false);
                    }

                    return;
                }
            }

            // 正在加载 AppEntry 或者现有的 AppEntry Cache 为空时都会重新加载。
            AppEntriesLoader.INSTANCE.postLoad();
        } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
            // TODO: 更新 APP 可能导致入口增加和图标变化
        }
    }
}
