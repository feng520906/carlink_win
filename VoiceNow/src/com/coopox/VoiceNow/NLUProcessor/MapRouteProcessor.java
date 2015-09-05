package com.coopox.VoiceNow.NLUProcessor;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import com.coopox.VoiceNow.CmdDispatcher;
import com.coopox.VoiceNow.NLUQuery;
import com.coopox.common.utils.AppUtils;
import com.coopox.common.utils.Checker;

import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/10/20
 */
// 处理导致语音指令
public class MapRouteProcessor implements CmdDispatcher.CmdProcessor {
        private static final String TAG = "MapRoute";
    public static final String KEY_MAP_ROUTE = "cn.yunzhisheng.map.ROUTE";

        public static final String LOCATION_URI =
                "content://com.coopox.carlauncher/locationmodel";

        @Override
        public String fire(CmdDispatcher dispatcher, Context context, NLUQuery result) {
            if (null != result && null != result.semantic && null != result.semantic.intent) {
                Map<String, String> attachment = result.semantic.intent;
                String arrival = attachment.get("toPOI");
                Log.i(TAG, arrival);
                if (Checker.isEmpty(arrival) || arrival.equals("CURRENT_LOC")) {
                    return "无效的目的地，无法导航";
                }

                if (null != context) {
                    ContentResolver cr = context.getContentResolver();
                    Cursor cursor = cr.query(Uri.parse(LOCATION_URI),
                            null, null, null, "Timestamp DESC");
                    if (null != cursor && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int cityIdx = cursor.getColumnIndex("City");
                        int latIdx = cursor.getColumnIndex("Latitude");
                        int lngIdx = cursor.getColumnIndex("Longitude");
                        int addrIdx = cursor.getColumnIndex("Address");
                        double lat = cursor.getDouble(latIdx);
                        double lng = cursor.getDouble(lngIdx);
                        String addr = cursor.getString(addrIdx);
                        String city = cursor.getString(cityIdx);
                        String fromCity = attachment.get("fromCity");
                        String toCity = attachment.get("toCity");

                        if (Checker.isEmpty(addr)) {
                            addr = "当前位置";
                        }
                        if (null != fromCity && fromCity.equals("CURRENT_CITY")) {
                            fromCity = city;
                        }
                        if (null != toCity && toCity.equals("CURRENT_CITY")) {
                            toCity = city;
                        }

                        String ret = null;
                        String uri = null;
                        StringBuilder sb = new StringBuilder("intent://map/direction?");
                        // 拼接起始地址信息
                        if (0f != lat || 0f != lng) {
                            // 虽然 (0, 0) 是有效经纬度，但此处仍将该位置视为未初始化的无效位置
                            sb.append(String.format("origin=latlng:%f,%f|name:%s&", lat, lng, addr));
                        } else {
                            sb.append(String.format("origin=name:%s&", addr));
                        }

                        // 拼接目的地信息
                        sb.append(String.format("destination=%s&", arrival));
                        // 出行方式
                        sb.append("mode=driving&");

                        if (!Checker.isEmpty(fromCity)) {
                            sb.append(String.format("origin_region=%s&", fromCity));
                        }

                        if (!Checker.isEmpty(toCity)) {
                            sb.append(String.format("destination_region=%s&", toCity));
                        }
//                        sb.append(String.format("region=%s&", "深圳市"));

                        sb.append("src=ZhiCheng|CarLink");
                        sb.append("#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end");

                        /*if (0f != lat || 0f != lng || !Checker.isEmpty(addr)) {
                            uri = String.format("intent://map/direction?origin=latlng:%f,%f|name=%s&destination=%s&mode=driving&region=%s&src=%s|%s#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end",
                                    lat, lng, addr, arrival, toCity, "ZhiCheng", "CarLink"); // TODO: 后续替换公司和产品名
                        } else {
                            // 如果出发地信息不详则采用高德地图导航
                            uri = String.format("androidamap://keywordNavi?sourceApplication=%s&keyword=%s&style=2",
                                    "CarLink", arrival);
                            Intent intent = new Intent("android.intent.action.VIEW",
                                    Uri.parse(uri));
                            intent.setPackage("com.autonavi.minimap");
                            intent.addCategory(Intent.CATEGORY_DEFAULT);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        }*/

                        // 如果已经启动过百度地图就先将其关闭，解决多次调用地图 URI 接口导致黑屏卡死问题。
                        AppUtils.forceStopPackage(context, "com.baidu.BaiduMap");

                        uri = sb.toString();
                        try {
                            Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                            AppUtils.startActivity(context, intent);
                            ret = String.format("将为您导航前往%s", arrival);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }

                        cursor.close();
                        return ret;
                    }
                }
            }
            return null;
        }
}
