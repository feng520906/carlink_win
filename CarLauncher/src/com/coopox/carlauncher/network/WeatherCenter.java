package com.coopox.carlauncher.network;

import com.activeandroid.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.coopox.carlauncher.activity.CarApplication;
import com.coopox.carlauncher.datamodel.WeatherModel;
import com.coopox.carlauncher.datamodel.WeatherModelWrapper;
import com.coopox.carlauncher.misc.FileUtils;
import com.coopox.carlauncher.misc.SerializationHelper;
import com.coopox.common.Constants;
import com.coopox.common.storage.DataStorage;
import com.coopox.common.storage.Storage;
import com.coopox.common.utils.Checker;
import com.coopox.common.utils.HttpRequestQueue;
import com.coopox.common.utils.HttpRequestUtils;
import com.coopox.common.utils.ThreadManager;
import com.google.gson.JsonObject;
import com.min.securereq.SecureGsonRequest;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-2
 * 气象中心类，负责从网络更新本地天气信息并缓存。
 */
public enum WeatherCenter {
    INSTANCE;

    public static final int CANT_LOAD_WEATHER_DATA = 0xff;

    public interface WeatherDataListener {
        void onWeatherLoadSuccess(WeatherModel data);

        void onWeatherLoadFailed();
    }

    private static final String URL = "http://ecs.4gcar.cn/apis/v1/weather";
    private static final String TAG = "WEATHER_CENTER";
    private static final String CACHE_FILE = "Weather.data";
    private String mCity;
    private WeatherModel mData;
    private Set<WeatherDataListener> mListeners;

    WeatherCenter() {
        mListeners = new HashSet<WeatherDataListener>(4);
        // 为了让 getWeatherData() 能同步拿到数据，在主线程里加载天气信息缓存
        synchronized (this) {
            mData = loadWeatherDataCache();
        }
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String city) {
        mCity = city;
    }

    public void registerDataListener(WeatherDataListener listener) {
        if (null != listener) {
            mListeners.add(listener);
        }
    }

    public void unregisterDataListener(WeatherDataListener listener) {
        mListeners.remove(listener);
    }

    /**
     * 刷新天气信息 */
    public void refreshWeatherData() {
        requestWeatherData(null);
    }

    public void refreshWeatherData(String city) {
        requestWeatherData(city);
    }

    /** 读取缓存的天气信息 */
    public synchronized WeatherModel getWeatherData() {
        return mData;
    }

     void requestWeatherData(String city) {
        if (null != city) {
            mCity = city;
        }

        if (null != mCity) {

            Log.i(TAG, String.format("Request Weather for %s", mCity));
            JsonObject param = HttpRequestUtils.createCommonJsonPostBody(CarApplication.getAppContext());
            param.addProperty("location", mCity);
            SecureGsonRequest<WeatherModelWrapper> request = new SecureGsonRequest<WeatherModelWrapper>(URL, param,
                    WeatherModelWrapper.class,
                    new Response.Listener<WeatherModelWrapper>() {
                        @Override
                        public void onResponse(WeatherModelWrapper response) {
                            if (null != response && null != response.weather) {
                                response.weather.timestamp = System.currentTimeMillis();
                                dispatchResult(response.weather);
                            } else {
                                dispatchResult(null);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.e(TAG, String.format("Weather data request failed(%s)", error.getMessage()));
                            dispatchResult(null);
                        }
                    }
            );
            request.setKeys(Constants.AESKEY, Constants.HACKEY);

            HttpRequestQueue.INSTANCE.addRequest(request);
        }
    }

    private void dispatchResult(final WeatherModel data) {
        if (null != mListeners) {
            if (null != data) {
                synchronized (WeatherCenter.this) {
                    mData = data;
                }

                for (WeatherDataListener listener : mListeners) {
                    listener.onWeatherLoadSuccess(data);
                }

                ThreadManager.INSTANCE.getIOThreadHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        // 在 I/O 线程里读写天气数据
                        saveWeatherData(data);
                    }
                });
            } else {
                for (WeatherDataListener listener : mListeners) {
                    listener.onWeatherLoadFailed();
                }
            }
        }
    }

    private WeatherModel loadWeatherDataCache() {
        Storage storage = new DataStorage(CarApplication.getAppContext(), CACHE_FILE);
        File file = storage.getFile();
        if (!Checker.isEmpty(file)) {
            byte[] bytes = FileUtils.fileToBytes(file);
            if (null != bytes) {
                return SerializationHelper.deserialize(bytes);
            }
        }
        return null;
    }

    private void saveWeatherData(WeatherModel data) {
        if (null != data) {
            byte[] bytes = SerializationHelper.serialize(data);
            Storage storage = new DataStorage(CarApplication.getAppContext(), CACHE_FILE);
            File file = storage.getFile();
            if (null != file) {
                FileUtils.bytesToFile(bytes, file);
            }
        }
    }
}
