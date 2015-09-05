package com.coopox.carlauncher.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.coopox.common.Constants;
import com.coopox.common.utils.HttpRequestQueue;
import com.coopox.common.utils.HttpRequestUtils;
import com.min.securereq.SecureJsonRequest;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/18
 */
public class NetworkStateReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkStateReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();

        // 已经连接网络，未注册过且本地已有用户信息，则发起注册
        if (isConnected && !isRegistry(context) && hasUserInfo(context)) {
            sendUserInfoToServer(context);
        }
    }

    // 如果用户未注册过，则将其信息发送到服务器
    private void sendUserInfoToServer(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.SP_USER_INFO,
                Context.MODE_MULTI_PROCESS);
        final SharedPreferences.Editor edit = sp.edit();

        JSONObject postBody = HttpRequestUtils.createCommonJSONPostBody(context);
        try {
            postBody.put(Constants.KEY_CAR_BRAND, sp.getString(Constants.KEY_CAR_BRAND, ""));
            postBody.put(Constants.KEY_CAR_FAMILY, sp.getString(Constants.KEY_CAR_FAMILY, ""));
            postBody.put(Constants.KEY_CAR_PLATE, sp.getString(Constants.KEY_CAR_PLATE, ""));
            postBody.put(Constants.KEY_CAR_PROVINCE, sp.getString(Constants.KEY_CAR_PROVINCE, ""));
            postBody.put(Constants.KEY_PROVINCE_SHORT, sp.getString(Constants.KEY_PROVINCE_SHORT, ""));
            postBody.put(Constants.KEY_ENGINE_CODE, sp.getString(Constants.KEY_ENGINE_CODE, ""));
            postBody.put(Constants.KEY_FRAME_NUM, sp.getString(Constants.KEY_FRAME_NUM, ""));

//            Log.d(TAG, postBody.toString());
        } catch (JSONException e) {
            e.printStackTrace();
            edit.putBoolean(Constants.KEY_IS_REGISTRY, false).commit();
            return;
        }

        SecureJsonRequest request = new SecureJsonRequest(Constants.URL_REGISTERY, postBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        edit.putBoolean(Constants.KEY_IS_REGISTRY, true).commit();
                        Log.i(TAG, "User registry success");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        edit.putBoolean(Constants.KEY_IS_REGISTRY, false).commit();
                        Log.e(TAG, "User registry failed: " + error);
                    }
                }
        );
        request.setKeys(Constants.AESKEY, Constants.HACKEY);

        HttpRequestQueue.INSTANCE.addRequest(request);
    }

    // 检查本地是否有用户信息
    private boolean hasUserInfo(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.SP_USER_INFO,
                Context.MODE_MULTI_PROCESS);
        return  (null != sp.getString(Constants.KEY_CAR_BRAND, null)
                && null != sp.getString(Constants.KEY_CAR_FAMILY, null));
    }

    // 检查是否已经注册
    private boolean isRegistry(Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.SP_USER_INFO,
                Context.MODE_MULTI_PROCESS);
        return (sp.getBoolean(Constants.KEY_IS_REGISTRY, false));
    }
}
