package com.coopox.carlauncher.misc;

import android.content.Context;
import android.content.SharedPreferences;
import com.coopox.carlauncher.R;

import java.util.Calendar;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/9
 */
public class DateChecker {
    public static final String KEY_DATE_CHECK_DATE = "kDateCheckFor";

    /**
     * 检查以指定 Key 上次达到间隔日期为止，到今天之间的天数间隔是否达到 exceptDayAfterLastToggle */
    public static boolean checkDays(Context context, String key, int exceptDayAfterLastToggle) {
        if (null == context) return false;
        if (null == key) key = "";

        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
        SharedPreferences sp = context.getSharedPreferences(
                context.getString(R.string.app_name),
                Context.MODE_PRIVATE);
        key = KEY_DATE_CHECK_DATE + key;
        int lastCheckDay = sp.getInt(key, 0);
        // 检查天数差值
        if (dayOfYear - lastCheckDay >= exceptDayAfterLastToggle) {
            sp.edit().putInt(key, dayOfYear).commit();
            return true;
        }
        return false;
    }
}
