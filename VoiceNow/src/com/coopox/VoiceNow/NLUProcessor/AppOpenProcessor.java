package com.coopox.VoiceNow.NLUProcessor;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import com.coopox.VoiceNow.CmdDispatcher;
import com.coopox.VoiceNow.NLUQuery;
import com.coopox.VoiceNow.R;
import com.coopox.common.utils.AppUtils;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: lokii
* Date: 15/2/11
*/ // 处理打开应用的语音指令
public class AppOpenProcessor implements CmdDispatcher.CmdProcessor {
    public static final String APP_ENTRY_URI =
            "content://com.coopox.carlauncher/appentry";

    private static final String APP_LAUNCH = "APP_LAUNCH";
    private static final String APP_EXIT = "APP_EXIT";
    public static final String KEY_APP_LAUNCH = "cn.yunzhisheng.appmgr.APP_LAUNCH";
    public static final String KEY_APP_EXIT = "cn.yunzhisheng.appmgr.APP_EXIT";

    private final String[] PROJECTION = {
            "Intent"};
    private final String SELECTION = "Name=?";
    // 为部分应用起的别名
    private static final Map<String, String> APP_ALIAS = new HashMap<String, String>();
    static {
        APP_ALIAS.put("路况", "路况电台");
        APP_ALIAS.put("导航", "百度地图");
        APP_ALIAS.put("胎压检测", "TPMS");
        APP_ALIAS.put("自驾游", "自驾助手");
        APP_ALIAS.put("爱车体验", "优驾");
        APP_ALIAS.put("FM", "FM发射");
        APP_ALIAS.put("收音机", "FM发射");
        APP_ALIAS.put("搜歌", "音乐雷达");
        APP_ALIAS.put("拨号", "蓝牙");  // 蓝牙拨号器的应用名太奇葩了，居然就叫蓝牙
        APP_ALIAS.put("电话", "蓝牙");
        APP_ALIAS.put("音乐", "天天动听");
    }

    @Override
    public String fire(CmdDispatcher dispatcher, Context context, NLUQuery result) {
        if (null != result) {
            Map<String, String> attachment = result.semantic.intent;
            String appName = attachment.get("name");
                if (null != context && null != appName) {
                    String[] args = new String[] {appName};
                    ContentResolver cr = context.getContentResolver();
                    Cursor cursor = cr.query(Uri.parse(APP_ENTRY_URI),
                            PROJECTION, SELECTION, args, null);

                    // 直接找没找到则通过别名查找
                    if (null == cursor || cursor.getCount() == 0) {
                        String realName = APP_ALIAS.get(appName);
                        if (null != realName) {
                            args = new String[] {realName};
                            cr = context.getContentResolver();
                            cursor = cr.query(Uri.parse(APP_ENTRY_URI),
                                    PROJECTION, SELECTION, args, null);
                        }
                    }

                    if (null != cursor && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int columnIndex = cursor.getColumnIndex("Intent");
                        String uri = cursor.getString(columnIndex);
                        String ret = null;
                        Intent intent = null;
                        try {
                            intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                            cursor.close();
                            return context.getString(R.string.app_data_error);
                        }

                        if (result.code.equals(APP_LAUNCH)) { // 打开应用
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            AppUtils.startActivity(context, intent);
                            ret = String.format(context.getString(R.string.will_launch_app), appName);
                        } else if (result.code.equals(APP_EXIT)) { // 关闭应用
                            int retCode = AppUtils.stopAppByIntent(context, intent);
                            switch (retCode) {
                                case 0:
                                    ret = String.format(context.getString(R.string.will_stop_app), appName);
                                    break;
                                case -1:
                                    ret = context.getString(R.string.can_not_stop_app);
                                    break;
                                case -2:
                                default:
                                    ret = context.getString(R.string.app_data_error);
                                    break;
                            }
                        }

                        cursor.close();

                        return ret;
                    } else {
                        return String.format(context.getString(R.string.can_not_find_app), appName);
                    }
                }
            }
        return null;
    }

}
