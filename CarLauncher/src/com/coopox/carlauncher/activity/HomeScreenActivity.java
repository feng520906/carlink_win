package com.coopox.carlauncher.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.datamodel.FavoriteAppEntries;
import com.coopox.common.Constants;
import com.umeng.analytics.MobclickAgent;


public class HomeScreenActivity extends FragmentActivity implements UserRegistryFragment.RegistrySubmitListener {

    private static final String TAG = "HomeScreenActivity";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // 更新友盟数据提交策略（可以在友盟后台“设置->发送策略”页面自定义数据发送的频率）
        MobclickAgent.updateOnlineConfig(this);

        setupFragment(savedInstanceState);
    }

    void setupFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            if (hasUserInfo()) {
                setupHomeScreenFragment();
            } else {
                setupUserRegistryFragment();
            }
        }
    }

    // 用户注册页
    void setupUserRegistryFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new UserRegistryFragment())
                .commit();
    }

    // Launcher 主屏页
    void setupHomeScreenFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeScreenFragment())
                .commit();
        // 如果本地有用户信息，但未成功注册过，则发起注册请求
        if (!isRegistry()) {
            UserRegistryActivity.sendUserInfoToServer(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    public void onBackPressed() {
        // Do nothing, just for avoid close by back key.
    }

    @Override
    public void onSubmit() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeScreenFragment())
                .commit();

        UserRegistryActivity.sendUserInfoToServer(this);
    }

    @Override
    public void onSkip() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeScreenFragment())
                .commit();
    }

    public FavoriteAppEntries getFavoriteAppEntries() {
        Fragment fragment =
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof HomeScreenFragment) {
            return ((HomeScreenFragment)fragment).getFavoriteAppEntries();
        }
        return null;
    }

    // 检查本地是否有用户信息
    protected boolean hasUserInfo() {
        SharedPreferences sp = getSharedPreferences(Constants.SP_USER_INFO,
                Context.MODE_MULTI_PROCESS);
        return  (null != sp.getString(Constants.KEY_CAR_BRAND, null)
                && null != sp.getString(Constants.KEY_CAR_FAMILY, null));
    }

    // 检查是否已经注册
    public boolean isRegistry() {
        SharedPreferences sp = getSharedPreferences(Constants.SP_USER_INFO,
                Context.MODE_MULTI_PROCESS);
        return (sp.getBoolean(Constants.KEY_IS_REGISTRY, false));
    }
}
