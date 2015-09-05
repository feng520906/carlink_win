package com.min.securereq;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/8
 */
public class SecureGsonRequest<T> extends JsonRequest<T> {
    private byte[] mAesKey;
    private byte[] mHmacKey;
    private final Class<T> mClazz;

    public SecureGsonRequest(String url, JsonObject requestParams, Class<T> clazz,
                             Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(url, requestParams.toString(), listener, errorListener);
        this.mClazz = clazz;
    }

    public SecureGsonRequest(String url, String requestBody, Class<T> clazz,
                             Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(url, requestBody, listener, errorListener);
        this.mClazz = clazz;
    }

    public void setKeys(byte[] aesKey, byte[] hmacKey) {
        mAesKey = aesKey;
        mHmacKey = hmacKey;
    }

    @Override
    public byte[] getBody() {
        byte[] data = super.getBody();
        return Utils.pack_msg(mAesKey, mHmacKey, data);
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            byte[] rawData = Utils.unpack_msg(mAesKey, mHmacKey, response.data);
            String charset = HttpHeaderParser.parseCharset(response.headers);
            String jsonString =
                    new String(rawData, charset);
            return Response.success(new Gson().fromJson(jsonString, mClazz),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (InvalidResponseException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.error(new ParseError());
    }
}
