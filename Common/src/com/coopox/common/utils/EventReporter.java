package com.coopox.common.utils;

import android.content.Context;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.coopox.common.Constants;
import com.min.securereq.SecureJsonRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 */
public enum EventReporter {
    INSTANCE;

    private static final String TAG = "EventReporter";
    private static final String URL = "http://ecs.4gcar.cn/apis/v1/ugc/report";
    private Context mContext;

    public void init(Context context) {
        mContext = context;
    }

    public void report(String id, String value) {
        if (null != id && null != value) {
            Map<String, String> kv = new HashMap<String, String>();
            report(id, value, kv);
            Log.i(id, value);
        }
    }

    public void report(String id, String value, Map<String, String> kvs) {
        if (null == mContext) {
            Log.e(TAG, "Please call init method first!");
            return;
        }
        if (null == id) return;

        JSONObject params = HttpRequestUtils.createCommonJSONPostBody(mContext);
        JSONArray events = new JSONArray();
        try {
            params.put("events", events);

//            JSONArray data = new JSONArray();
            JSONObject event = new JSONObject();
            event.put("id", id);
            event.put("ts", System.currentTimeMillis());
            event.put("value", value);
//            event.put("other", data);
            if (null != kvs) {
                for (Map.Entry<String, String> entry : kvs.entrySet()) {
                    if (null != entry.getKey() && null != entry.getValue()) {
//                        JSONObject kvJson = new JSONObject();
                        event.put(entry.getKey(), entry.getValue());
//                        data.put(kvJson);
                    }
                }
            }
            events.put(event);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SecureJsonRequest request = new SecureJsonRequest(URL, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "Report event successfully");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Report event failed: " + error);
                    }
                });

        request.setKeys(Constants.AESKEY, Constants.HACKEY);
        HttpRequestQueue.INSTANCE.addRequest(request);
    }
}
