package com.coopox.carlauncher.datamodel;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 * 因为不再直接从百度天气请求数据，而是通过我方服务器中转，服务器为了标识请求是否成功增加了一个
 * ret 字段，导致 GSON 反序列化 WeatherModel 失败，所以在原来的 Class 外部包装一层，加上 ret 字段。
 */
public class WeatherModelWrapper implements Serializable {
    public int ret;
    public WeatherModel weather;
}
