package com.coopox.carlauncher.activity;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.coopox.carlauncher.R;
import com.coopox.common.Constants;
import com.coopox.common.utils.HttpRequestQueue;
import com.coopox.common.utils.HttpRequestUtils;
import com.min.securereq.SecureJsonRequest;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15-5-16
 * 用户信息注册界面
 */
public class UserRegistryActivity extends FragmentActivity implements UserRegistryFragment.RegistrySubmitListener {
    private static final String TAG = "UserRegistryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        setupUserRegistryFragment();
    }

    void setupUserRegistryFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new UserRegistryFragment())
                .commit();
    }

    @Override
    public void onSubmit() {
        sendUserInfoToServer(this);
        finish();
    }

    @Override
    public void onSkip() {
        finish();
    }

    // 如果用户未注册过，则将其信息发送到服务器
    public static void sendUserInfoToServer(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(Constants.SP_USER_INFO,
                Context.MODE_MULTI_PROCESS);
        final SharedPreferences.Editor edit = sp.edit();

        JSONObject postBody = HttpRequestUtils.createCommonJSONPostBody(CarApplication.getAppContext());
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
}
