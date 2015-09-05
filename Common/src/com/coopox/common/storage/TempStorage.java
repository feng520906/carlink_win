package com.coopox.common.storage;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-2
 */
public class TempStorage implements Storage {
    private String mTempDir = "Temp";

    public TempStorage(String tempDir) {
        if (null != tempDir) {
            mTempDir = tempDir;
        }
    }

    public TempStorage() {
    }

    @Override
    public File getFile() {
        try {
            return File.createTempFile(mTempDir, ".tmp");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
