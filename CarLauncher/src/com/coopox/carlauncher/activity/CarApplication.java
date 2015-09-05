package com.coopox.carlauncher.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;
import com.activeandroid.ActiveAndroid;
import com.baidu.frontia.FrontiaApplication;
import com.coopox.carlauncher.business.CarInfoReminder;
import com.coopox.common.utils.EventReporter;
import com.coopox.common.utils.HttpRequestQueue;
import com.coopox.common.utils.ThreadManager;
import com.coopox.network.http.DownloadClient;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-15
 */
public class CarApplication extends FrontiaApplication {
    private static final String TAG = "CAR_LAUNCHER";
    private static Context sContext;
//    private static ImageManager sImageManager;
    private String mPackageName;
    private String mProcessName;
    private CarInfoReminder mCarInfoReminder;
    private static DownloadClient sDownloadClient;

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        // 初始化日志上报系统
        EventReporter.INSTANCE.init(this);
        // 初始化 Http 请求队列
        HttpRequestQueue.INSTANCE.init(this);

        mPackageName = getPackageName();
        int pid = android.os.Process.myPid();
        mProcessName = getProcessName(pid);
        Log.i(TAG, String.format("Application process name = %s", mProcessName));

        // 只在 UI 进程里执行下列逻辑，百度 Push 进程不用
        if (null != mProcessName && mProcessName.equals(mPackageName)) {
            initForUIProcess();
        }
    }

    private void initForUIProcess() {
        ActiveAndroid.initialize(this);
        // 依赖数据库，所以先初始化 ActiveAndroid.
        mCarInfoReminder = new CarInfoReminder(this);
        mCarInfoReminder.start();

        sDownloadClient = new DownloadClient(this, ThreadManager.INSTANCE.getIOThreadHandler());

/*        LoaderSettings settings = new LoaderSettings.SettingsBuilder()
                .withDisconnectOnEveryCall(true)
                .withCacheManager(new LruBitmapCache(this))
                .withConnectionTimeout(20000)
                .withReadTimeout(30000)
                .build(this);
        sImageManager = new ImageManager(this, settings);*/
    }

/*    public static ImageManager getImageManager() {
        return sImageManager;
    }*/

    public static Context getAppContext() {
        return sContext;
    }

    public static DownloadClient getDownloadClient() {
        return sDownloadClient;
    }

    private String getProcessName(int pID)
    {
        String processName = "";
        ActivityManager am = (ActivityManager)this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
        if (null != list) {
            for (Object aList : list) {
                ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo) (aList);
                try {
                    if (info.pid == pID) {
                        processName = info.processName;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return processName;
    }

}
