package com.coopox.carlauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.coopox.common.utils.ThreadManager;

import java.lang.ref.WeakReference;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-2
 */
public class TimeChangedReceiver extends BroadcastReceiver {
    public interface TimeChangedListener {
        void onTimeChanged();
    }

    private WeakReference<TimeChangedListener> mListenerRef;

    public void setTimeChangedListener(TimeChangedListener listener) {
        if (null != listener) {
            mListenerRef = new WeakReference<TimeChangedListener>(listener);
        }
    }

    public void removeTimeChangedListener() {
        mListenerRef.clear();
        mListenerRef = null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
            ThreadManager.INSTANCE.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (null != mListenerRef) {
                        TimeChangedListener listener = mListenerRef.get();
                        if (null != listener) {
                            listener.onTimeChanged();
                        }
                    }
                }
            });
        }
    }
}
