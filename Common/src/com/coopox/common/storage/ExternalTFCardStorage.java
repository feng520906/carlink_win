package com.coopox.common.storage;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/1/10
 */
public class ExternalTFCardStorage implements Storage {

    private File mFile;
    String mPath;
    String mFileName;
    private static Method sGetExternalStoragePath;
    private static Class sStorageManagerExClazz;

    public ExternalTFCardStorage(String path, String file) {
        mPath = path;
        mFileName = file;
        if (null == sStorageManagerExClazz || null == sGetExternalStoragePath) {
            try {
                sStorageManagerExClazz = Class.forName("com.mediatek.storage.StorageManagerEx");
                sGetExternalStoragePath = sStorageManagerExClazz.getDeclaredMethod("getExternalStoragePath");
                if (!sGetExternalStoragePath.isAccessible()) {
                    sGetExternalStoragePath.setAccessible(true);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public File getFile() {
        if (null == mFile) {
            if (null != mFileName) {
                if (available()) {
                    String externalPath = null;
                    try {
                        externalPath = (String) sGetExternalStoragePath.invoke(null);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                    if (null != externalPath && null != mPath) {
                        File externalDir = new File(externalPath, mPath);
                        if ((!externalDir.exists() && !externalDir.mkdirs()) ||
                                !externalDir.isDirectory()) {
                            return null;
                        }

                        mFile = new File(externalDir, mFileName);
                    }
                }
            }
        }

        return mFile;
    }

    private static boolean available() {
        return null != sGetExternalStoragePath && null != sStorageManagerExClazz;
    }

    // TODO: 测试该方法在没有 TF 卡时调结果是啥，这影响到 ExternalStorageStatusReceiver 的执行逻辑
    public static String getExternalStoragePath() {
        if (available()) {
            try {
                return (String) sGetExternalStoragePath.invoke(null);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
