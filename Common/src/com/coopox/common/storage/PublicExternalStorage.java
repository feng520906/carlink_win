package com.coopox.common.storage;

import android.os.Environment;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-13
 */
public class PublicExternalStorage implements Storage {
    String mType;
    String mFileName;
    File mFile;

    public PublicExternalStorage(String type, String fileName) {
        mType = type;
        mFileName = fileName;
    }

    @Override
    public File getFile() {
        if (null == mFile) {
            File path = Environment.getExternalStoragePublicDirectory(mType);
            mFile = new File(path, mFileName);

            // Make sure the target directory exists.
            path.mkdirs();
        }

        return mFile;
    }
}
