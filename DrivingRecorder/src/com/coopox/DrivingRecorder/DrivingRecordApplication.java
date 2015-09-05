package com.coopox.DrivingRecorder;

import android.app.Application;
import com.coopox.common.utils.EventReporter;
import com.coopox.common.utils.HttpRequestQueue;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 */
public class DrivingRecordApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        EventReporter.INSTANCE.init(this);
        HttpRequestQueue.INSTANCE.init(this);
    }
}
