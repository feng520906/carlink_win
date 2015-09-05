package com.coopox.carlauncher.view;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.volley.Response;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.activity.HomeScreenActivity;
import com.coopox.carlauncher.datamodel.AppEntry;
import com.coopox.carlauncher.datamodel.LocationManager;
import com.coopox.carlauncher.datamodel.LocationModel;
import com.coopox.carlauncher.datamodel.WeatherModel;
import com.coopox.carlauncher.misc.BitmapHelper;
import com.coopox.common.utils.Checker;
import com.coopox.carlauncher.misc.Utils;
import com.coopox.carlauncher.network.WeatherCenter;
import com.coopox.carlauncher.receiver.TimeChangedReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-3
 */
public class WeatherCell extends HomePageCell implements
        LocationManager.LocationListener,
        WeatherCenter.WeatherDataListener,
        TimeChangedReceiver.TimeChangedListener {
    private static final String TAG = "HomePageFragmentA";
    public static final String[] WEEK_DAYS =
            {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
    public static final int UPDATE_INTERVAL = 1000 * 3600 * 4;
    private static final int DAY = 0;
    private static final int NIGHT = 1;
    private LayoutInflater mInflater;
    private WeatherModel mWeatherData;
    private ViewGroup mCellView;
    private TimeChangedReceiver mTimeChangedReceiver = new TimeChangedReceiver();
    private TextView mClock;
    private TextView mDate;
    private TextView mWeek;
    private Bitmap[] mWeatherIcon = new Bitmap[2];
    private int mLastHour = -1;
    private TextView mWeather;
    private String mDateString = "";
    private int mWeekDay;

    public WeatherCell(Context context) {
        super(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        mInflater = inflater;
        setupCalendarCell(root);

        // 更新定位信息是为了获取天气，二者都与 UI 显示相关，因此在 onCreateView/onDestroyView
        // 里注册和反注册：
        LocationManager.INSTANCE.registerLocationListener(this);
        WeatherCenter.INSTANCE.registerDataListener(this);
        mTimeChangedReceiver.setTimeChangedListener(this);
        mContext.registerReceiver(mTimeChangedReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        root.setOnClickListener(this);
        return root;
    }

    @Override
    public void onDestroyView() {
        WeatherCenter.INSTANCE.unregisterDataListener(this);
        LocationManager.INSTANCE.unregisterLocationListener(this);
        mTimeChangedReceiver.removeTimeChangedListener();
        mContext.unregisterReceiver(mTimeChangedReceiver);
    }

    @Override
    public void onClick(View v) {
        // TODO: 这个逻辑最好重构到父类里做
        if (mContext instanceof HomeScreenActivity) {
            String clockName = mContext.getString(R.string.clock);
            AppEntry entry = ((HomeScreenActivity) mContext).getFavoriteAppEntries().getEntryByName(clockName);
            Utils.startAppEntry(mContext, entry);
        }
    }

    private void setupCalendarCell(ViewGroup contentView) {
        mCellView = contentView;
        if (null != contentView) {
            mInflater.inflate(R.layout.cell_weather, contentView);
            mWeather = (TextView) contentView.findViewById(R.id.weather);
            mClock = (TextView) contentView.findViewById(R.id.time_label);
            mDate = (TextView) contentView.findViewById(R.id.date_label);
            mWeek = (TextView) contentView.findViewById(R.id.week_label);
            onTimeChanged();

            mWeatherData = WeatherCenter.INSTANCE.getWeatherData();
            // 如果有缓存则直接使用，没有则请求最新的天气信息
            if (null != mWeatherData) {
                onWeatherLoadSuccess(mWeatherData);
            } else {
                updateWeather();
            }
        }
    }

    @Override
    public void onLocationUpdate(LocationModel data) {
        // 不必太频繁得刷新天气信息，每隔几小时刷新
        if (null != mWeatherData &&
                mWeatherData.timestamp - System.currentTimeMillis() < UPDATE_INTERVAL) {
            return;
        }
        updateWeather();
    }

    @Override
    public void onCityChanged(String oldCity, String newCity) {
        // 城市改变后需要刷新天气信息
        updateWeather();
    }

    private void updateWeather() {
        LocationModel data = LocationManager.INSTANCE.getLastLocation();
        if (null != data && !Checker.isEmpty(data.city)) {
            WeatherCenter.INSTANCE.setCity(data.city);
            WeatherCenter.INSTANCE.refreshWeatherData();
        }
    }

    /** 天气数据获取成功，更新 UI */
    @Override
    public void onWeatherLoadSuccess(WeatherModel data) {
        mWeatherData = data;
        if (null != mCellView) {
            TextView moreWeather = (TextView) mCellView.findViewById(R.id.more_weather_info);
            if (null != mWeather && null != moreWeather) {
                if (null != data && null != data.results && null != data.results[0]) {
                    WeatherModel.Result result = data.results[0];
                    if (null != result.weatherData) {
                        WeatherModel.Result.WeatherData weatherData = result.weatherData[0];
                        mWeather.setText(String.format("%s\n%s\n%s",
                                weatherData.temperature,
                                result.city,
                                weatherData.weather));

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
                        moreWeather.setText(String.format("PM 2.5：%s\n%s洗车",
                                result.pm25, carWashing));

                        // download 方法会对参数判空
                        BitmapHelper.download(weatherData.dayPicUrl,
                                new Response.Listener<Bitmap>() {
                                    @Override
                                    public void onResponse(Bitmap response) {
                                        if (null != response) {
                                            mWeatherIcon[DAY] = response;
                                            updateWeatherIcon(-1);
                                        }
                                    }
                                }, null);
                        BitmapHelper.download(weatherData.nightPicUrl,
                                new Response.Listener<Bitmap>() {
                                    @Override
                                    public void onResponse(Bitmap response) {
                                        if (null != response) {
                                            mWeatherIcon[NIGHT] = response;
                                            updateWeatherIcon(-1);
                                        }
                                    }
                                }, null);
                    }
                }
            }
        }
    }

    @Override
    public void onWeatherLoadFailed() {
    }

    // 检查是否发生了昼夜变化，以更新天气图标
    private void updateWeatherIcon(int currentHour) {
        int hour = currentHour;
        if (currentHour < 0 || currentHour > 23) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH");
            String hourStr = simpleDateFormat.format(new Date());
            hour = Integer.parseInt(hourStr);
        }

        Bitmap icon = null;
        // 判断是昼夜，并获取相应的天气图标
        // TODO: 白天和夜晚的区间最好根据时区调整或做成可设置的。
        if (hour >= 6 && hour < 18) {
            icon = mWeatherIcon[DAY];
        } else {
            icon = mWeatherIcon[NIGHT];
        }
        if (null != icon) {
            Resources res = mContext.getResources();
            int w = res.getDimensionPixelSize(R.dimen.weather_icon_width);
            int h = res.getDimensionPixelSize(R.dimen.weather_icon_height);
            Drawable drawable = new BitmapDrawable(res, icon);
            drawable.setBounds(0, 0, w, h);
            mWeather.setCompoundDrawables(drawable,
                    null, null, null);
        }
    }

    @Override
    public void onTimeChanged() {
        Date date = new Date();
        if (null != mClock) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
            String time = simpleDateFormat.format(date);
            mClock.setText(time);
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH");
        String hourStr = simpleDateFormat.format(date);
        int hour = Integer.parseInt(hourStr);

        // 首次进来或者日期发生了变化
        if (-1 == mLastHour || (hour != mLastHour && 0 == hour)) {
            if (null != mDate) {
                simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
                mDateString = simpleDateFormat.format(date);
                mDate.setText(mDateString);
            }

            if (null != mWeek) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);

                mWeekDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
                if (mWeekDay < 0) mWeekDay = 0;
                mWeek.setText(WEEK_DAYS[mWeekDay]);
            }
        } else { // 虽然日期未变化，但有可能 View 被 ViewPager 重新创建过，设置其内容
            mDate.setText(mDateString);
            mWeek.setText(WEEK_DAYS[mWeekDay]);
        }

        if (hour != mLastHour) { // 小时发生了改变
            mLastHour = hour;
            // 每小时检查是否需要更新天气图标
            updateWeatherIcon(hour);
        }
    }
}
