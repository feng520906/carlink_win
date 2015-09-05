package com.coopox.common.storage;

import android.content.Context;
import android.os.Environment;
import android.webkit.URLUtil;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 */
public class PublicDownloadStorage implements Storage {
    private File mFile;

    public PublicDownloadStorage(Context context, String url) {
        if (null != context && null != url) {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File dir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        return;
                    }
                }
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
