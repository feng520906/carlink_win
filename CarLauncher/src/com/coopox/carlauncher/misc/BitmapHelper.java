package com.coopox.carlauncher.misc;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import com.android.volley.Response;
import com.android.volley.toolbox.ImageRequest;
import com.coopox.common.utils.HttpRequestQueue;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-15
 */
public class BitmapHelper {
    public static Bitmap getBitmapFromDrawable(Drawable drawable)
    {
        Bitmap bitmap = null;
        if ((drawable instanceof BitmapDrawable)) {
            bitmap = ((BitmapDrawable)drawable).getBitmap();
            return bitmap;
        }
        else if (drawable != null){
            int w = drawable.getIntrinsicWidth();
            int h = drawable.getIntrinsicHeight();
            Bitmap.Config config = Bitmap.Config.ARGB_8888;
            try{
                bitmap = Bitmap.createBitmap(w, h, config);
                drawable.setBounds(0, 0, w, h);
                Canvas canvas = new Canvas(bitmap);
                drawable.draw(canvas);
            }
            catch (OutOfMemoryError e) {
                e.printStackTrace();
            }
            return bitmap;
        }
        return null;
    }

    public static Bitmap getRawDimensionBitmap(Resources res, int resID) {
        if (null !=res && 0 != resID) {
            InputStream is = res.openRawResource(resID);
            return BitmapFactory.decodeStream(is);
        }
        return null;
    }

    /** 启用新线程下载图片，用于下一些不用频繁更新的小图片 */
    public static void download(final String url,
                                final Response.Listener<Bitmap> listener,
                                final Response.ErrorListener errListener) {
        if (null != url && null != listener) {
            ImageRequest request = new ImageRequest(url, listener, 0, 0,
                    Bitmap.Config.ARGB_8888, errListener);
            HttpRequestQueue.INSTANCE.addRequest(request);
        }
    }
}
