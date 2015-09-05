package com.coopox.carlauncher.datamodel;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.coopox.carlauncher.R;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-3
 * 显示在桌面前两页的预置应用入口，预置入口不再显示到应用列表页中（所以该类起了过滤去重的作用）
 */
public class FavoriteAppEntries {
    private Map<String, AppEntry> mAppEntries;
    private static final RawEntry[] RAW_ENTRIES = {
            new RawEntry(R.drawable.ic_car_record, R.string.car_record, "com.coopox.DrivingRecorder", "com.coopox.DrivingRecorder.RecordPreviewActivity"),
            new RawEntry(R.drawable.ic_gallery, R.string.gallery, "com.android.gallery3d", "com.android.gallery3d.app.GalleryActivity"),
            new RawEntry(R.drawable.ic_traffic_status, R.string.traffic_status, "com.ilukuang", "com.ilukuang.activity.SplashActivity"),
            new RawEntry(R.drawable.ic_navgation, R.string.navgation, "com.baidu.BaiduMap", "com.baidu.baidumaps.WelcomeScreen"),
            new RawEntry(R.drawable.ic_music, R.string.music, "com.sds.android.ttpod", "com.sds.android.ttpod.EntryActivity"),
            new RawEntry(R.drawable.ic_wechat, R.string.wechat, "com.tencent.mm", "com.tencent.mm.ui.LauncherUI"),
            new RawEntry(R.drawable.ic_dial, R.string.dial, "com.ls.android.phone", "com.ls.android.phone.StartBT"),
            new RawEntry(R.drawable.ic_search_song, R.string.search_song, "com.voicedragon.musicclient.car", "com.voicedragon.musicclient.car.MainActivity"),
            new RawEntry(R.drawable.ic_radio, R.string.radio_settings, "com.gilda.fmupdate", "com.gilda.fmupdate.MainA"),
            new RawEntry(R.drawable.ic_obd, R.string.obd, "com.comit.gooddriver", "com.comit.gooddriver.LoadingActivity"),
            new RawEntry(R.drawable.ic_nav2, R.string.navgation2, "com.autonavi.minimap", "com.autonavi.map.activity.SplashActivity"),
            new RawEntry(R.drawable.ic_self_driving, R.string.self_driving, "com.zijiazhushou.android", "com.zijiazhushou.android.activity.LanucherActivity"),
            new RawEntry(R.drawable.ic_settings, R.string.settings, "com.coopox.carlauncher", "com.coopox.carlauncher.activity.SettingsActivity"),
            new RawEntry(0, R.string.clock, "com.android.deskclock", "com.android.deskclock.DeskClock"),
            new RawEntry(R.drawable.ic_launcher, R.string.car_link, "com.coopox.carlauncher", "com.coopox.carlauncher.activity.HomeScreenActivity"),
            new RawEntry(R.drawable.ic_pressure, R.string.pressure, "com.naruto.tpms.app", "com.naruto.tpms.app.activity.WelcomeActivity"),
    };

    public FavoriteAppEntries(Context context) {
        if (null != context) {
            mAppEntries = new HashMap<String, AppEntry>(RAW_ENTRIES.length);

            Resources res = context.getResources();
            for (RawEntry rawEntry : RAW_ENTRIES) {
                AppEntry appEntry = new AppEntry();
                appEntry.name = res.getString(rawEntry.nameId);
                appEntry.isSysApp = true;
                appEntry.intent = new Intent(Intent.ACTION_MAIN);
                appEntry.intent.addCategory(Intent.CATEGORY_LAUNCHER);
                appEntry.intent.setClassName(rawEntry.packageName, rawEntry.className);

                if (0 != rawEntry.iconId) {
                    Drawable icon = res.getDrawable(rawEntry.iconId);
                    if (icon instanceof BitmapDrawable) {
                        appEntry.icon = ((BitmapDrawable) icon).getBitmap();
                    }
                }

                mAppEntries.put(appEntry.name, appEntry);
            }
        }
    }

    public AppEntry getEntryByName(String name) {
        return mAppEntries.get(name);
    }

    public Collection<AppEntry> getFavoriteAppEntries() {
        return mAppEntries.values();
    }

    static class RawEntry {
        public RawEntry(int iconId, int nameId, String packageName, String className) {
            this.packageName = packageName;
            this.className = className;
            this.iconId = iconId;
            this.nameId = nameId;
        }

        public String packageName;
        public String className;
        public int iconId;
        public int nameId;
    }
}
