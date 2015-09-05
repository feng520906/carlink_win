package com.coopox.carlauncher.business;

import android.content.Context;
import com.coopox.carlauncher.datamodel.LocationManager;
import com.coopox.carlauncher.datamodel.LocationModel;
import com.coopox.carlauncher.datamodel.WeatherModel;
import com.coopox.carlauncher.misc.DateChecker;
import com.coopox.carlauncher.network.WeatherCenter;
import com.coopox.carlauncher.view.WeatherCell;
import com.coopox.common.tts.TTSClient;
import com.coopox.common.utils.Checker;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-15
 * 与车辆相关信息（天气、洗车指数、违章、保养）的提醒器
 */
public class CarInfoReminder implements WeatherCenter.WeatherDataListener, LocationManager.LocationListener {
    boolean mWeatherUpdated;
    private WeakReference<Context> mContextRef;

    public CarInfoReminder(Context context) {
        mContextRef = new WeakReference<Context>(context);
    }

    public void start() {
        WeatherCenter.INSTANCE.registerDataListener(this);
        LocationManager.INSTANCE.registerLocationListener(this);
    }

    public void stop() {
        LocationManager.INSTANCE.unregisterLocationListener(this);
        WeatherCenter.INSTANCE.unregisterDataListener(this);
    }

    private void updateWeather() {
        LocationModel data = LocationManager.INSTANCE.getLastLocation();
        if (null != data && !Checker.isEmpty(data.city)) {
            WeatherCenter.INSTANCE.setCity(data.city);
        }
        WeatherCenter.INSTANCE.refreshWeatherData();
    }

    @Override
    public void onWeatherLoadSuccess(WeatherModel data) {
        // 保证一天内只调用一次语音播报
        if (!DateChecker.checkDays(mContextRef.get(), "WeatherRemind", 1)) {
            return;
        }

        if (null != data && null != data.results && null != data.results[0]) {
            WeatherModel.Result result = data.results[0];
            if (null != result.weatherData) {
                WeatherModel.Result.WeatherData weatherData = result.weatherData[0];

                String carWashing = "";
                if (null != result.index) {
                    // 从指数信息中查找洗车指数
                    for (WeatherModel.Result.Index index : result.index) {
                        if (null != index && null != index.title &&
                                index.title.equals("洗车")) {
                            carWashing = index.brief;
                            break;
                        }
                    }
                }

                Date date = new Date();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM月dd号");

                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                int weekDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (weekDay < 0) weekDay = 0;

                String temp = weatherData.temperature.replace("~", "至").replace("℃", "摄氏度");
                String weatherInfo = String.format("今天是%s，%s，%s天气%s，气温%s，PM2.5指数为%s，%s洗车。",
                        simpleDateFormat.format(date),
                        WeatherCell.WEEK_DAYS[weekDay],
                        result.city,
                        weatherData.weather,
                        temp,
                        result.pm25,
                        carWashing);
                Context context = mContextRef.get();
                TTSClient.speak(context, weatherInfo, null);
            }
        }
    }

    @Override
    public void onWeatherLoadFailed() {

    }

    @Override
    public void onLocationUpdate(LocationModel data) {
        if (!mWeatherUpdated) {
            updateWeather();
            mWeatherUpdated = true;
        }
    }

    @Override
    public void onCityChanged(String oldCity, String newCity) {

    }
}
