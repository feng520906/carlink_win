package com.coopox.carlauncher.misc;

import android.util.Log;
import com.coopox.common.utils.Checker;
import com.coopox.common.utils.StreamUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class FileUtils {

    private static final String TAG = "FileUtils";

    private FileUtils() {
    }

    /**
     * 获取文件大小的描述文本，如1.6K  2M
     *
     * @param filePath 文件路径
     * @return 返回文件大小描述文本
     */
    public static String getFileSizeString(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        long fileSize = file.length();
        return getFileSizeString(fileSize);
    }

    /**
     * 字节数转换为KB/MB/...
     *
     * @param fileLength
     * @return
     */
    public static String getFileSizeString(long fileLength) {
        String[] posFixes = new String[]{"B", "KB", "MB", "GB","TB"};
        for (int i = 0; i < posFixes.length; i++) {
            long top = (long) Math.pow(1024, (i + 1));
            long scale = (long) Math.pow(1024, i);
            if (fileLength < top) {
                String result = String.format("%1$.2f", (double)fileLength / scale);
                if (result.endsWith(".00")) {
                    result = result.replace(".00", "");
                }
                return result + posFixes[i];
            }
        }
        return null;
    }

    public static List<File> listFiles(File dir) {
        if (!dir.isDirectory()) return new ArrayList<File>();

        List<File> files = new ArrayList<File>();
        File[] fileArray = dir.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                if (!file.isDirectory()) {
                    files.add(file);
                } else {
                    List<File> subFiles = listFiles(file);
                    files.addAll(subFiles);
                }
            }
        }
        return files;
    }

    public static byte[] fileToBytes(File file) {
        if (Checker.isEmpty(file)) return null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            long length = file.length();
            if (length > Integer.MAX_VALUE) throw new RuntimeException("File is too large");

            // Create the byte array to hold the data
            byte[] bytes = new byte[(int) length];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)
                offset += numRead;

            // Ensure all the bytes have been read in
            if (offset < bytes.length)
                Log.w(TAG, "Could not completely read file " + file);
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            StreamUtils.closeStream(is);
        }
    }

    public static String fileToString(File file) {
        if (Checker.isEmpty(file)) return null;
        return new String(fileToBytes(file));
    }

    public static File bytesToFile(byte[] bytes, File file) {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        try {
            if (!file.exists()) {
                if (file.getParentFile() != null && !file.getParentFile().exists())
                    file.getParentFile().mkdirs();
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(bytes);
            bos.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            StreamUtils.closeStream(fos);
            StreamUtils.closeStream(bos);
        }
        return file;
    }

    public static File stringToFile(String string, File file) {
        Writer out = null;
        try {
            if (!file.exists()) {
                if (file.getParentFile() != null && !file.getParentFile().exists())
                    file.getParentFile().mkdirs();
                file.createNewFile();
            }
            out = new OutputStreamWriter(new FileOutputStream(file));
            out.write(string);
            out.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            StreamUtils.closeStream(out);
        }
        return file;
    }

    public static File appendStringToFile(String string, File file) {
        Writer out = null;
        try {
            if (!file.exists()) {
                if (file.getParentFile() != null && !file.getParentFile().exists())
                    file.getParentFile().mkdirs();
                file.createNewFile();
            }
            out = new OutputStreamWriter(new FileOutputStream(file, true));
            out.write(string, 0, string.length());
            out.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            StreamUtils.closeStream(out);
        }
        return file;
    }

    public static boolean copyFile(File src, File dst) {
        if (!Checker.isExistedFile(src)) return false;
        FileInputStream is = null;
        FileOutputStream os = null;
        try {
            is = new FileInputStream(src);
            os = new FileOutputStream(dst);

            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) {
                os.write(buf, 0, len);
            }
            os.flush();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy file from " + src + " to " + dst, e);
        } finally {
            StreamUtils.closeStream(is);
            StreamUtils.closeStream(os);
        }
        return false;
    }

    public static boolean copyFolder(File src, File dst) {
        if (!Checker.isExistedFile(src)
                || !src.isDirectory()) return false;
        boolean result = true;
        if (src.exists()) dst.mkdirs();
        File[] fileArray = src.listFiles();
        if (fileArray != null) {
            for (File file : src.listFiles()) {
                File dstFile = new File(dst, file.getName());
                if (file.isDirectory()) {
                    result = copyFolder(file, dstFile);
                    if (!result) {
                        break;
                    }
                } else {
                    result = copyFile(file, dstFile);
                    if (!result) {
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static void deleteFolder(File dir) {
        deleteFolder(dir, null);
    }

    public static void deleteFolder(File dir, String skipFilePattern) {
        if (!Checker.isExistedFile(dir)) return;
        if (!dir.isDirectory()) throw new IllegalArgumentException();

        File[] fileArray = dir.listFiles();
        if (fileArray != null) {
            for (File fileToBeDeleted : dir.listFiles()) {
                if (fileToBeDeleted.isDirectory()) deleteFolder(fileToBeDeleted, skipFilePattern);
                if (!Checker.isEmpty(skipFilePattern)
                        && fileToBeDeleted.getName().matches(skipFilePattern)) continue;
                fileToBeDeleted.delete();
            }
        }

        // ref: http://stackoverflow.com/questions/11539657
//        final File tmp = new File(dir.getAbsolutePath() + System.currentTimeMillis());
//        dir.renameTo(tmp);
//        tmp.delete();
        dir.delete();
    }

    public static String getFileNameWithoutExt(File file) {
        if (file == null) return null;
        return getFileNameWithoutExt(file.getName());
    }

    public static String getFileNameWithoutExt(String filename) {
        if (Checker.isEmpty(filename)) return null;
        if (filename.lastIndexOf('.') < 0) return filename;
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    public static String getFileExtName(File file) {
        if (file == null) return null;
        String fileName = file.getName();
        if (fileName.lastIndexOf('.') < 0) return fileName;
        return fileName.substring(fileName.lastIndexOf('.') + 1, fileName.length());
    }

    public static File addSuffixToFileName(File file, String suffix) {
        if (file == null) return null;
        return new File(file.getParent(),
                getFileNameWithoutExt(file) + suffix + "." + getFileExtName(file));
    }

    public static String getFileNameFromUrl(String url) {
        if (Checker.isEmpty(url)) return null;
        return url.substring(url.lastIndexOf('/') + 1, url.length());
    }

    public static String getFilePathFromDir(File file, File dir) {
        if (Checker.isExistedFile(file)) return null;
        return file.getAbsolutePath().substring(dir.getAbsolutePath().length());
    }


}
