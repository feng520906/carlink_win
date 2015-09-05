package com.coopox.carlauncher.network;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.coopox.carlauncher.activity.CarApplication;
import com.coopox.common.utils.StreamUtils;
import com.coopox.common.storage.InternalDownloadStorage;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-26
 */
public class FileRequest extends Request<File> {
    private Response.Listener<File> mListener;
    private String mUrl;

    public FileRequest(String url, Response.Listener<File> listener,
                       Response.ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        mUrl = url;
        mListener = listener;
    }

    @Override
    protected Response<File> parseNetworkResponse(NetworkResponse response) {
        InternalDownloadStorage storage = new InternalDownloadStorage(CarApplication.getAppContext(), mUrl);
        File file = null;
        BufferedOutputStream bos = null;
        try {
            file = storage.getFile();
            bos = new BufferedOutputStream(new FileOutputStream(file));
            bos.write(response.data);
            bos.flush();
            return Response.success(file, HttpHeaderParser.parseCacheHeaders(response));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        } catch (IOException e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        } finally {
            StreamUtils.closeStream(bos);
        }
    }

    @Override
    protected void deliverResponse(File response) {
        if (null != mListener) {
            mListener.onResponse(response);
        }
    }
}
