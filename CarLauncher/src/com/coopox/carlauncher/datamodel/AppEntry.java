package com.coopox.carlauncher.datamodel;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.BaseColumns;
import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.coopox.carlauncher.misc.ActivityInfoPicker;

import java.lang.ref.SoftReference;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-14
 */
@Table(name = "AppEntry", id = BaseColumns._ID)
public class AppEntry extends Model {
    @Column(name = "Name")
    public String name;

    @Column(name = "Intent")
    public Intent intent;

    @Column(name = "Icon")
    public Bitmap icon;

    @Column(name = "SystemApp")
    public boolean isSysApp;

    private SoftReference<BitmapDrawable> drawableCache;

    // 默认构造方法用于 Model 生成数据表
    public AppEntry() {

    }

    // 注意，该方法从系统包管理器获取应用名称和图标等信息，比较耗时。请不要在主线程里频繁调用！
    public AppEntry(Context context, ResolveInfo info) {
        this.intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(
                info.activityInfo.applicationInfo.packageName,
                info.activityInfo.name));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        this.name = ActivityInfoPicker.getActivityName(context, info);
        if (null != this.name) {
            // 去除名称首尾空格，避免影响语音控制的效果。
            this.name = this.name.trim();
        }
        this.isSysApp = (info.activityInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        this.icon = ActivityInfoPicker.getActivityIcon(context, info);
    }

    // 读取所有的应用入口信息
    public static List<AppEntry> getAll() {
        return new Select().from(AppEntry.class).execute();
    }

    @Deprecated
    public Drawable getIconDrawable() {
        return getIconDrawable(null);
    }

    public Drawable getIconDrawable(Context context) {
        if (null != drawableCache) {
            Drawable drawable = drawableCache.get();
            if (null != drawable) {
                return drawable;
            }
        }

        BitmapDrawable drawable;
        if (null != context) {
            drawable = new BitmapDrawable(context.getResources(), icon);
        } else {
            drawable = new BitmapDrawable(icon);
        }
        drawable.setBounds(0, 0, icon.getWidth(), icon.getHeight());
        drawableCache = new SoftReference<BitmapDrawable>(drawable);
        return drawable;
    }
}
