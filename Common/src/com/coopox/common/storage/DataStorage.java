package com.coopox.common.storage;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;

public class DataStorage implements Storage {

    private String mFileName;
    private File mFile;
    private Context mContext;

    public DataStorage(Context context, String fileName) {
        if (null == context || TextUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("Context or file name cannot be empty!");
        }

        mContext = context;
        mFileName = fileName;
    }

    @Override
    public File getFile() {
        if (mFile == null) {
            mFile = new File(getDataHome(), mFileName);
        }
        return mFile;
    }

    protected File getDataHome() {
        return mContext.getFilesDir();
    }
}
