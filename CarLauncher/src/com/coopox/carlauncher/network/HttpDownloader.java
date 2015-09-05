package com.coopox.carlauncher.network;

import com.android.volley.Response;
import com.android.volley.toolbox.RequestFuture;
import com.coopox.common.utils.HttpRequestQueue;

import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-8
 * 网络下载器，提供轻量、重量级下载方案。
 */
public enum  HttpDownloader {
    INSTANCE;

    // 使用 char 防止转型时符号扩展而导致问题
    public static final int ERR_UNKNOWN = Integer.MIN_VALUE;
    public static final int ERR_CANCELED = ERR_UNKNOWN + 1;
    private static final int IO_ERROR = ERR_CANCELED + 1;
    private static final String TEMP_DIR = "Download";

    public interface HttpDownloadListener {
        void onSuccess(File file, long length);
        void onFailed(int ret);
    }

/*    class FileDownloadTask extends AsyncTask<String, Long, File> {

        private long mLength;
        private WeakReference<HttpDownloadListener> mListenerRef;

        public FileDownloadTask(HttpDownloadListener listener) {
            mListenerRef = new WeakReference<HttpDownloadListener>(listener);
        }

        @Override
        protected File doInBackground(String... urls) {
            try {
                File file = null;
                if (null != urls && urls.length > 0) {
                    HttpRequest request = HttpRequest.get(urls[0]);
                    if (request.ok()) {
                        file = new TempStorage(TEMP_DIR).getFile();
                        if (null != file) {
                            request.receive(file);
                            publishProgress(file.length());
                        } else {
                            // 本地文件打开失败
                            publishProgress((long)IO_ERROR);
                        }
                    } else {
                        // 如果请求不成功，则将返回码作为进度传出去
                        publishProgress((long)request.code());
                    }
                }
                return file;
            } catch (Exception exception) {
                exception.printStackTrace();
                publishProgress((long) ERR_UNKNOWN);
                return null;
            }
        }

        protected void onProgressUpdate(Long... progress) {
            if (null != progress) {
                mLength = progress[0];
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();

            HttpDownloadListener listener = mListenerRef.get();
            if (null != listener) {
                listener.onFailed(ERR_CANCELED);
            }
        }

        protected void onPostExecute(File file) {
            HttpDownloadListener listener = mListenerRef.get();
            if (null != listener) {
                if (null != file) {
                    listener.onSuccess(file, mLength);
                } else {
                    listener.onFailed((int) mLength);
                }
            }
        }
    }

    // 轻量下载接口，用于下载类似图标之类的小文件，不支持断点续传等高级能力
    public void liteDownload(String url, HttpDownloadListener listener) {
        if (null != url && null != listener) {
            FileDownloadTask task = new FileDownloadTask(listener);
            task.execute(url);
        }
    }*/

    // 下载接口，支持断点续传等能力，用于下载大型文件。
    public void download(String url, Response.Listener<File> listener,
                         Response.ErrorListener errorListener) {
        FileRequest request = new FileRequest(url, listener, errorListener);
        HttpRequestQueue.INSTANCE.addRequest(request);
    }

    // 同步下载接口，下载完毕后返回，请勿在 UI 线程中使用！
    public File syncDownload(String url) {
        RequestFuture<File> requestFuture = RequestFuture.newFuture();
        FileRequest request = new FileRequest(url, requestFuture, requestFuture);
        HttpRequestQueue.INSTANCE.addRequest(request);

        try {
            return requestFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
