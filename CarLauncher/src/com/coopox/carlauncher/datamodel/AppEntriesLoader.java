package com.coopox.carlauncher.datamodel;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.util.Log;
import com.activeandroid.ActiveAndroid;
import com.coopox.carlauncher.activity.CarApplication;
import com.coopox.common.utils.Checker;
import com.coopox.common.utils.ThreadManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-15
 */
public enum AppEntriesLoader {
    INSTANCE;

    public interface LoadListener {
        void onLoaded(boolean useCache, List<AppEntry> allEntries);
    }

    private static final String TAG = "AppLoader";
    private volatile boolean mIsLoading;
    private volatile List<AppEntry> mAppEntries;
    private WeakReference<LoadListener> mListenerRef;

    public synchronized void setLoadListener(LoadListener listener) {
        if (null != listener) {
            mListenerRef = new WeakReference<LoadListener>(listener);
        } else {
            mListenerRef = null;
        }
    }

    // 因为是 post 一个消息到 I/O 线程，所以此方法不会重入
    public void postLoad() {
        ThreadManager.INSTANCE.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                load();
            }
        });
    }

    // 本方法很耗时，限制在 I/O 线程里执行，避免阻塞 UI 线程。
    private void load() {
        Log.i(TAG, "Start loading");
        mIsLoading = true;

        List<AppEntry> appEntries = loadAppEntriesFromDB();

        // 如果已经加载过，先将缓存内容发给监听器。再去读取最新的应用信息
        if (null == mAppEntries && !Checker.isEmpty(appEntries)) {
            Log.i(TAG, "Use app entries cache.");
            mAppEntries = appEntries;
            deliverResult(true);
        }

        List<ResolveInfo> appInfos = loadAppsFromSystem(CarApplication.getAppContext());

        mAppEntries = mergeAppEntries(appEntries, appInfos);

        Log.i(TAG, "Refresh app entries.");
        deliverResult(false);
        mIsLoading = false;
        Log.i(TAG, "Stop loading");
    }

    public boolean isLoading() {
        return mIsLoading;
    }

    // 将读取结果通过 I/O 线程发送给监听器（请注意重写 onLoaded 方法时的线程安全）
    public synchronized void deliverResult(boolean useCache) {
        if (null != mListenerRef) {
            LoadListener listener = mListenerRef.get();
            if (null != listener) {
                listener.onLoaded(useCache, mAppEntries);
            }
        }
    }

    // 将读取结果通过主线程发送给监听器
    private void deliverResultByMainThread(final boolean useCache) {
        ThreadManager.INSTANCE.getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                if (null != mListenerRef) {
                    LoadListener listener = mListenerRef.get();
                    if (null != listener) {
                        listener.onLoaded(useCache, mAppEntries);
                    }
                }
            }
        });
    }

    public synchronized List<AppEntry> getAppEntriesCache() {
        return mAppEntries;
    }

    // 对缓存的应用入口和全量应用信息做合并
    // 这个方法会把全量应用信息中多出来应用（新安装）加到缓存，
    // 把缓存中多出来的入口（已卸载）剔除掉。以保证两者一致。
    // 这里之所以采取合并而不是全部刷新缓存主要是考虑降低 I/O 的开销。
    private List<AppEntry> mergeAppEntries(List<AppEntry> appEntries,
                                           List<ResolveInfo> appInfos) {
        if (null == appInfos) return null;

        List<AppEntry> entriesCopy;
        if (null != appEntries) {
            entriesCopy = new ArrayList<AppEntry>(appEntries);
        } else {
            entriesCopy = new ArrayList<AppEntry>(0);
        }

        List<ResolveInfo> appInfosCopy = new ArrayList<ResolveInfo>(appInfos);

        List<AppEntry> results = new Vector<AppEntry>(appInfos.size());
        Iterator<AppEntry> itr = entriesCopy.iterator();
        while (itr.hasNext()) {
            AppEntry entry = itr.next();
            Iterator<ResolveInfo> infoItr = matchIntentInInfos(appInfosCopy, entry.intent);
            // 缓存中的应用入口在全量应用列表中匹配成功
            if (null != infoItr) {
                results.add(entry); // 将这个应用加入合并结果
                // 处理过的应用移除掉，遍历完后余下的就是全量应用里没有即已经卸载了的
                itr.remove();
                // 被匹配过的应用也移除掉，遍历后余下的就是新安装的
                infoItr.remove();
            }
        }

        ActiveAndroid.beginTransaction();
        try {
            for (ResolveInfo info : appInfosCopy) {
                AppEntry entry = new AppEntry(CarApplication.getAppContext(), info);
                results.add(entry); // 往合并结果里增加新安装的应用
                entry.save();   // 并将其保存到数据库
            }

            for (AppEntry entry : entriesCopy) {
                entry.delete(); // 从数据库删除已经卸载的应用
            }

            ActiveAndroid.setTransactionSuccessful();
        } finally {
            ActiveAndroid.endTransaction();
        }

        return results;
    }

    private Iterator<ResolveInfo> matchIntentInInfos(List<ResolveInfo> infos, Intent intent) {
        final ComponentName comp = intent.getComponent();
        final String packageName = comp.getPackageName();
        final String className = comp.getClassName();

        for (Iterator<ResolveInfo> it=infos.iterator(); it.hasNext(); /* null */) {
            final ResolveInfo info = it.next();
            // 只比对包名类名，未处理 Category 和 Action 等信息
            if (packageName.equals(info.activityInfo.packageName) && className.equals(info.activityInfo.name)) {
                return it;
            }
        }
        return null;
    }

    // 从系统的包管理器中获取所有的可执行应用的信息
    // 注意：该方法比较耗时，请勿在主线程里频繁调用。
    List<ResolveInfo> loadAppsFromSystem(Context context) {
        if (null != context) {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos =
                    context.getPackageManager().queryIntentActivities(mainIntent, 0);
            return infos;
        }
        return null;
    }

    // 从数据库中读取缓存的所有应用入口信息
    List<AppEntry> loadAppEntriesFromDB() {
        return AppEntry.getAll();
    }
}
