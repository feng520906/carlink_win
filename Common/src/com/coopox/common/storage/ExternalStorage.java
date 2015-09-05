package com.coopox.common.storage;

import android.os.Environment;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-13
 */
public class ExternalStorage implements Storage {
    String mPath;
    String mFileName;
    File mFile;

    public ExternalStorage(String path, String file) {
        mPath = path;
        mFileName = file;
    }

    @Override
    public File getFile() {
        if (null == mFile) {
            if (null != mFileName) {
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    File externalPath = Environment.getExternalStorageDirectory();

                    if (null != mPath) {
                        String absExternalPath = externalPath.getAbsolutePath();
                        externalPath = new File(absExternalPath, mPath);
                        if (!externalPath.mkdirs() && !externalPath.isDirectory()) {
                            return null;
                        }
                    }

                    mFile = new File(externalPath, mFileName);
                }
            }
        }

        return mFile;
    }
}
