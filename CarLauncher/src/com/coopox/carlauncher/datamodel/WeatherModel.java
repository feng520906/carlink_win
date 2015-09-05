package com.coopox.carlauncher.datamodel;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-7
 */
public class WeatherModel implements Serializable {
    private static final long serialVersionUID = 9146868478422860063L;

    public int error;
    public String status;
    public String date;       // 该天气信息对应的具体日期
    public long timestamp;

    public static class Result implements Serializable {
        @SerializedName("currentCity")
        public String city;     // 该天气信息对应的城市
        public int pm25;        // 当天的 PM2.5 参数

        // 当天天气指数
        public static class Index implements Serializable {
            public String title;    // 指数标题，如：洗车
            @SerializedName("zs")
            public String brief;    // 指数简介，如：不宜
            @SerializedName("tipt")
            public String name;     // 指数完整名称，如：洗车指数
            @SerializedName("des")
            public String desc;     // 详细描述，如：不宜洗车，未来24小时内有雨，如果在此期间洗车，雨水和路上的泥水可能会再次弄脏您的爱车。
        }

        public Index[] index;

        // 具体的天气数据
        public static class WeatherData implements Serializable {
            @SerializedName("date")
            public String weekDay;      // 当天周几，如：周一
            public String weather;      // 天气，如：多云
            public String wind;         // 风向及风力，如：西风微风
            public String temperature;  // 气温，如：8~17℃
            @SerializedName("dayPictureUrl")
            public String dayPicUrl;    // 白天的天气示意图片 URL
            @SerializedName("nightPictureUrl")
            public String nightPicUrl;  // 晚上的天气示意图片 URL
        }

        @SerializedName("weather_data")
        public WeatherData[] weatherData;
    }

    public Result[] results;
}
