package com.coopox.SmartKey;

import android.app.Application;
import com.coopox.common.utils.EventReporter;
import com.coopox.common.utils.HttpRequestQueue;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15-5-2
 */
public class SmartKeyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        HttpRequestQueue.INSTANCE.init(this);
        EventReporter.INSTANCE.init(this);
    }
}
