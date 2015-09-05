package com.coopox.common.utils;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/13
 */
public class AssetUtils {

    private static final int MAX_FILE_SIZE = 16 * 1024 * 1024;

    public static String loadJSONFromAsset(Context context, String path) {
        String json = null;
        if (null == context || null == path) return null;

        InputStream is = null;
        try {
            is = context.getAssets().open(path);

            int size = is.available();

            if (size < MAX_FILE_SIZE) {
                byte[] buffer = new byte[size];

                is.read(buffer);

                json = new String(buffer, "UTF-8");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        } finally {
            StreamUtils.closeStream(is);
        }
        return json;
    }
}
