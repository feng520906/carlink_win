package com.coopox.carlauncher.datamodel;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 */
public class AppPushedResp implements Serializable {
    public static class AppInfo implements Serializable {
        public String appName;
        public String packageName;
        public int versionCode;
        public String versionName;
        public String apkUrl;
    }

    public static class Data implements Serializable {
        @SerializedName("new")
        public AppInfo[] newApps;

        public AppInfo[] upgrade;

        public AppInfo[] remove;
    }

    public int ret;
    public Data data;
}
