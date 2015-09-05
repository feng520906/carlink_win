package com.coopox.VoiceNow.NLUProcessor;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import com.coopox.VoiceNow.CmdDispatcher;
import com.coopox.VoiceNow.NLUQuery;
import com.coopox.common.utils.AppUtils;

import java.net.URISyntaxException;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/11/8
 */
public class PoiProcessor implements CmdDispatcher.CmdProcessor {
    public static final String LOCATION_URI =
            "content://com.coopox.carlauncher/locationmodel";

    public static final String KEY_SEARCH_POI1 = "cn.yunzhisheng.localsearch.BUSINESS_SEARCH";
    public static final String KEY_SEARCH_POI2 = "cn.yunzhisheng.localsearch.NONBUSINESS_SEARCH";
    public static final String KEY_POSITION_POI = "cn.yunzhisheng.map.POSITION";

    @Override
    public String fire(CmdDispatcher dispatcher, Context context, NLUQuery result) {
        if (null != context && null != result) {
            ContentResolver cr = context.getContentResolver();
            Cursor cursor = cr.query(Uri.parse(LOCATION_URI),
                    null, null, null, "Timestamp DESC");
            if (null != cursor && cursor.getCount() > 0) {
                cursor.moveToFirst();
                int cityIdx = cursor.getColumnIndex("City");
                int latIdx = cursor.getColumnIndex("Altitude");
                int lngIdx = cursor.getColumnIndex("Longitude");
                int addrIdx = cursor.getColumnIndex("Address");
                double lat = cursor.getDouble(latIdx);
                double lng = cursor.getDouble(lngIdx);
                String addr = cursor.getString(addrIdx);
                String currentCity = cursor.getString(cityIdx);
                String ret = null;
                String keyword = null;
                String city = null;
                int radius = -1;
                if (result.code.equals("POSITION")) {
                    keyword = result.getIntent("toPOI", "");
                    city = result.getIntent("toCity", "CURRENT_CITY");
                } else {
                    keyword = result.getIntent("keyword", null);
                    city = result.getIntent("city", "CURRENT_CITY");
                    // 默认搜索两公里以内的
                    radius = Integer.parseInt(result.getIntent("radius", "2000"));
                    if (null == keyword) {
                        keyword = result.getIntent("category", null);
                    }
                }
                if (!city.equals("CURRENT_CITY")) {
                    // 指定搜索城市
                    currentCity = city;
                }
                if (!TextUtils.isEmpty(currentCity)) {
                    String uri;
                    if (radius > 0) {
                        uri = String.format("intent://map/place/search?query=%s&location=%f,%f&radius=%d&region=%s&src=%s|%s#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end",
                                keyword, lat, lng, radius, currentCity, "Coopox", "CarLink"); // TODO: 后续替换公司和产品名
                    } else {
                        uri = String.format("intent://map/place/search?query=%s&location=%f,%f&region=%s&src=%s|%s#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end",
                                keyword, lat, lng, currentCity, "Coopox", "CarLink"); // TODO: 后续替换公司和产品名
                    }
                    try {
                        Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        AppUtils.startActivity(context, intent);
                        String normalHeader = result.getNormalHeader();
                        ret = (null != normalHeader ? normalHeader : String.format("将为您查找%s", keyword));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }

                cursor.close();
                return ret;
            }
        }
        return null;
    }
}
