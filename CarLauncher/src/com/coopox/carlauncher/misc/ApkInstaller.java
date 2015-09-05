package com.coopox.carlauncher.misc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import com.coopox.common.utils.AppUtils;
import com.coopox.common.utils.StreamUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-4
 */
public class ApkInstaller {

    private static final String TAG = "ApkInstaller";

    // 静默安装，需要 System 权限和签名。成功返回 true
    public static boolean silentInstall(String apkPath) {
        String[] args = { "pm", "install", "-r", apkPath };
        return checkResult(args);
    }

    public static boolean silentUninstall(Context context, String pkgName) {
        String permission = "android.permission.DELETE_PACKAGES";
        int res = context.checkCallingOrSelfPermission(permission);
        if (res == PackageManager.PERMISSION_GRANTED) {
            String[] args = {"pm", "uninstall", pkgName};
            return checkResult(args);
        }
        return false;
    }

    // 从终端读取命令执行结果
    private static boolean checkResult(String[] args) {
        String result = "";
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        Process process = null;
        InputStream errIs = null;
        InputStream inIs = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int read = -1;
            process = processBuilder.start();
            errIs = process.getErrorStream();
            while ((read = errIs.read()) != -1) {
                baos.write(read);
            }
            baos.write('.');
            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }
            byte[] data = baos.toByteArray();
            result = new String(data);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeStream(errIs);
            StreamUtils.closeStream(inIs);
            if (process != null) {
                process.destroy();
            }
        }

        if (result.contains("Success")) {
            return true;
        } else {
            Log.e(TAG, "Install Failed:" + result);
            return false;
        }
    }

    // 标准安装，会呼起系统的安装确认对话框
    public static void install(Context context, String path) {
        if (null != context && null != path) {
            Uri uri = Uri.parse("file://" + path);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            AppUtils.startActivity(context, intent);
        }
    }
}
