package com.coopox.common.utils;

import android.app.Application;
import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-26
 */
public enum HttpRequestQueue {
    INSTANCE;

    private Context sAppContext;
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;

    private HttpRequestQueue() {
    }

    public void init(Context context) {
        if (context instanceof Application) {
            // getAppContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            sAppContext = context;
            mRequestQueue = getRequestQueue();
        } else {
            throw new IllegalArgumentException("Please pass Application context for avoid memory leaking!");
        }
    }

    private RequestQueue getRequestQueue() {
        if (null == sAppContext) {
            throw new RuntimeException("Please call HttpRequestQueue.init() at first!");
        }

        if (mRequestQueue == null) {
            mRequestQueue = Volley.newRequestQueue(sAppContext);
        }
        mImageLoader = new ImageLoader(mRequestQueue,
                new LruBitmapCache(LruBitmapCache.getCacheSize(sAppContext)));
        return mRequestQueue;
    }

    public <T> void addRequest(Request<T> req) {
        getRequestQueue().add(req);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }
}
