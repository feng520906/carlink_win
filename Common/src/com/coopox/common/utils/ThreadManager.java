package com.coopox.common.utils;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-8
 * Handler 管理器，提供I/O 及主线程对应的 Handler
 */
public enum ThreadManager {
    INSTANCE;

    private static final String IO_THREAD = "I/O";
    private HandlerThread mIOThread;
    // 用于执行 I/O 操作的 Handler
    private Handler mFileHandler;
    // 主线程 Handler
    private volatile Handler mMainThreadHandler;

    private ThreadManager() {
        init();
    }

    public Handler getIOThreadHandler() {
        return mFileHandler;
    }

    public void runOnWorkerThread(final Runnable runnable) {
        if (null != runnable) {
            ThreadManager.INSTANCE.getIOThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            });
        }
    }

    public Handler getMainThreadHandler() {
        if (null == mMainThreadHandler) {
            synchronized (ThreadManager.class) {
                if (null == mMainThreadHandler) {
                    mMainThreadHandler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return mMainThreadHandler;
    }

    public void runOnUiThread(final Runnable runnable) {
        if (null != runnable) {
            ThreadManager.INSTANCE.getMainThreadHandler().post(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                }
            });
        }
    }

    public void init() {
        mIOThread = new HandlerThread(IO_THREAD);
        mIOThread.setPriority(Thread.NORM_PRIORITY);
        mIOThread.start();
        mFileHandler = new Handler(mIOThread.getLooper());
    }

    public void dispose() {
        mIOThread.getLooper().quit();
        mIOThread = null;
        mFileHandler = null;
    }
}
