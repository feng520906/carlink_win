package com.coopox.common.storage;

import android.content.Context;
import android.os.Environment;
import android.webkit.URLUtil;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-5
 */
public class InternalDownloadStorage implements Storage {
    private File mFile;
    public InternalDownloadStorage(Context context, String url) {
        if (null != context && null != url) {
            File dir = context.getExternalCacheDir();
            if (null == dir || !dir.exists()) {
                dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                if (null == dir || !dir.exists()) {
                    dir = context.getCacheDir();
                }
            }

            if (null != dir) {
                String fileName = URLUtil.guessFileName(url, null, null);
                mFile = new File(dir, fileName);
            }
        }
    }

    @Override
    public File getFile() {
        return mFile;
    }
}
