package com.coopox.carlauncher.business;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.coopox.carlauncher.activity.CarApplication;
import com.coopox.carlauncher.datamodel.AppPushedResp;
import com.coopox.carlauncher.misc.ApkInstaller;
import com.coopox.common.Constants;
import com.coopox.common.storage.InternalDownloadStorage;
import com.coopox.common.storage.PublicDownloadStorage;
import com.coopox.common.storage.Storage;
import com.coopox.common.utils.*;
import com.coopox.network.http.DownloadClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.min.securereq.SecureGsonRequest;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 */
public class AppUpdateChecker implements DownloadClient.DownloadListener {
    private static final String TAG = "AppUpdateChecker";
    private WeakReference<Context> mContextRef;
    private static final String APP_PUSHED_URL = "http://ecs.4gcar.cn/apis/v1/applist";
    private DownloadClient mDownloadClient;

    public AppUpdateChecker(Context context) {
        mContextRef = new WeakReference<Context>(context);
        mDownloadClient = CarApplication.getDownloadClient();
    }

    // 检查是否有应用更新，或新推送应用、老应用需要卸载
    public void check() {
        Context context = mContextRef.get();
        if (null == context || null == mDownloadClient) return;
        checkAllInstalledApps(context);
    }

    /* 搜集已安装所有 App 的信息，并上报到后台。 */
    private void checkAllInstalledApps(final Context context) {
        if (null == context) return;

        final PackageManager pm = context.getPackageManager();
        //get a list of installed apps.
        final List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        if (null == packages) return;

        JsonObject params = HttpRequestUtils.createCommonJsonPostBody(context);
        JsonArray array = new JsonArray();
        params.add("packages", array);

        for (ApplicationInfo apkInfo : packages) {
            String pkgName = apkInfo.packageName;
            if (null == pkgName) continue;

            String versionName = null;
            int versionCode = 0;
            try {
                PackageInfo pkgInfo =
                        pm.getPackageInfo(pkgName, PackageManager.GET_META_DATA);
                versionCode = pkgInfo.versionCode;
                versionName = pkgInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                continue;
            }

            JsonObject pkgJson = new JsonObject();
            pkgJson.addProperty("packageName", pkgName);
            pkgJson.addProperty("versionCode", versionCode);
            pkgJson.addProperty("versionName", versionName);
            array.add(pkgJson);
        }

        SecureGsonRequest<AppPushedResp> request =
                new SecureGsonRequest<AppPushedResp>(APP_PUSHED_URL,
                        params,
                        AppPushedResp.class,
                        new Response.Listener<AppPushedResp>() {
                            @Override
                            public void onResponse(final AppPushedResp response) {
                                // 因为 Volley 默认将回复通过主线程派发，为了不阻塞这里在工作线程处理
                                ThreadManager.INSTANCE.runOnWorkerThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        handleResponse(context, response, packages);
                                    }
                                });
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e(TAG, "Request APP pushed list failed: " + error);
                            }
                        });
        request.setKeys(Constants.AESKEY, Constants.HACKEY);
        HttpRequestQueue.INSTANCE.addRequest(request);
    }

    private void handleResponse(Context context, AppPushedResp response, List<ApplicationInfo> packages) {
        if (null != response
                && 0 == response.ret
                && null != response.data) {

            AppPushedResp.AppInfo selfInfo = null;
            if (!Checker.isEmpty(response.data.upgrade)) {

                // 下载要更新的应用（不管本地装没装）
                for (AppPushedResp.AppInfo appInfo : response.data.upgrade) {
                    Log.d(TAG, String.format("%s need upgrade", appInfo.packageName));
                    if (appInfo.packageName.equals(context.getPackageName())) {
                        // 如果本应用也要更新，则将其记录下来放到队列最后下载，避免本应用更新时其它
                        // 应用还没下载完而中断更新流程
                        selfInfo = appInfo;
                    } else {
                        downloadApp(context, appInfo);
                    }
                }
            }

            if (!Checker.isEmpty(response.data.newApps)) {
                // 下载要新增的应用（只处理本地未安装的）
                for (AppPushedResp.AppInfo appInfo : response.data.newApps) {
                    boolean needInstall = true;
                    for (ApplicationInfo pkgInfo : packages) {
                        String pkgName = pkgInfo.packageName;
                        if (null != pkgName
                                && pkgName.equals(appInfo.packageName)) {
                            // 此应用已经安装
                            needInstall = false;
                            break;
                        }
                    }

                    if (needInstall) {
                        downloadApp(context, appInfo);
                    }
                }
            }

            if (null != selfInfo) {
                // 更新本应用放在下载队列最后执行。这么做的前置条件为下载任务是串行执行的。
                downloadApp(context, selfInfo);
                Log.d(TAG, String.format("%s will download at last.", selfInfo.packageName));
            }

            if (!Checker.isEmpty(response.data.remove)) {
                // 卸载指定应用
                for (AppPushedResp.AppInfo appInfo : response.data.remove) {
                    ApkInstaller.silentUninstall(context, appInfo.packageName);
                }
            }
        }
    }

    private HashMap<String, String> mDownloadingTasks = new HashMap<String, String>();
    private void downloadApp(Context context, AppPushedResp.AppInfo appInfo) {
        Storage storage = new PublicDownloadStorage(context, appInfo.apkUrl);
        if (null == storage.getFile()) {
            storage = new InternalDownloadStorage(context, appInfo.apkUrl);
            Log.w(TAG, "Can't take public storage, use the internal storage.");
        }
        if (null != storage.getFile()) {
            // 将每个下载任务都记录下来，以便监控需要下载后立即启动的服务（例如行车记录和语音控制）：
            mDownloadingTasks.put(appInfo.apkUrl, appInfo.packageName);

            mDownloadClient.startDownloadInSerial(appInfo.apkUrl,
                    storage.getFile().getPath(), this);
        }
    }

    @Override
    public void onDownloadWaiting(String s, String s1, Object o) {

    }

    @Override
    public void onDownloadStart(String url, String path, Object o) {
        Log.d(TAG, String.format("Start download %s to %s", url, path));
    }

    @Override
    public void onUpdateProgress(String s, String s1, int i, Object o) {

    }

    @Override
    public void onDownloadSuccess(String url, String outputPath, Object o) {
        if (!ApkInstaller.silentInstall(outputPath)) {
            ApkInstaller.install(mContextRef.get(), outputPath);
        } else {
            Log.i(TAG, "APK silent install success.");

            // 判断是否为安装后需要立即启动的组件
            String pkgName = mDownloadingTasks.get(url);
            if (null != pkgName) {
                Intent intent = null;
                if (pkgName.equals("com.coopox.DrivingRecorder")) {
                    // 如果下载安装的是行车记录
                    intent = new Intent("com.coopox.service.action.DrivingRecord");
                } else if (pkgName.equals("com.coopox.VoiceNow")) {
                    // 下载安装的是语音控制组件
                    intent = new Intent("com.coopox.service.action.VOICE_NOW");
                } else if (pkgName.equals("com.coopox.SmartKey")) {
                    // 下载安装的是智键控制组件
                    intent = new Intent("com.coopox.service.action.START_KEY_SERVICE");
                }

                Context context = mContextRef.get();
                if (null != intent && null != context) {
                    context.startService(intent);
                }
            }
        }
        // 下载结束后将任务从监控记录里移除
        mDownloadingTasks.remove(url);
    }

    @Override
    public void onDownloadCancelled(String url, String s1, Object o) {
        mDownloadingTasks.remove(url);
    }

    @Override
    public void onDownloadFailed(String url, String s1, int i, Object o) {
        mDownloadingTasks.remove(url);
    }
}
