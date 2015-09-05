package com.coopox.carlauncher.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import com.activeandroid.query.Select;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.datamodel.PreferencesEntity;
import com.coopox.common.utils.Checker;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/3/31
 */
public class SettingsActivity extends PreferenceActivity  {

    private SharedPreferences mDefaultPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(
                android.R.id.content, new MyPreferenceFragment()).commit();
        mDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mDefaultPrefs.registerOnSharedPreferenceChangeListener(mSPChangedListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDefaultPrefs.unregisterOnSharedPreferenceChangeListener(mSPChangedListener);
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener mSPChangedListener = new
            SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sp,
                                                      String key) {
                    List<PreferencesEntity> preferencesEntities =
                            new Select().from(PreferencesEntity.class).orderBy("_id ASC").limit(1).execute();
                    PreferencesEntity preferences = null;
                    if (Checker.isEmpty(preferencesEntities)) {
                        preferences = new PreferencesEntity();
                    } else {
                        preferences = preferencesEntities.get(0);
                    }

                    if (key.equals(getString(R.string.switch_voice_now))) {
                        Intent serviceIntent = new Intent("com.coopox.service.action.VOICE_NOW");
                        stopService(serviceIntent);
                        if (sp.getBoolean(key, true)) {
                            preferences.swVoiceNow = true;
                            startService(serviceIntent);
                        } else {
                            preferences.swVoiceNow = false;
                            stopService(serviceIntent);
                        }
                    } else if (key.equals(getString(R.string.switch_smart_key))) {
                        Intent serviceIntent = new Intent("com.coopox.service.action.START_KEY_SERVICE");
                        if (sp.getBoolean(key, true)) {
                            preferences.swSmartKey = true;
                            startService(serviceIntent);
                        } else {
                            preferences.swSmartKey = false;
                            stopService(serviceIntent);
                        }
                    }

                    // 保存设置参数
                    preferences.save();
                }
            };
}
