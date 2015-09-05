package com.coopox.VoiceNow.NLUProcessor;

import android.content.Context;
import android.content.Intent;
import com.coopox.VoiceNow.CmdDispatcher;
import com.coopox.VoiceNow.NLUQuery;
import com.coopox.common.utils.AppUtils;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/3/10
 */
public class MatchSongProcessor implements CmdDispatcher.CmdProcessor {
    public static final String KEY_MATCH_SONG = "cn.yunzhisheng.music.SEARCH_BILLBOARD";

    @Override
    public String fire(CmdDispatcher dispatcher, Context context, NLUQuery result) {
        if (null != context && null != result) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            // 打开音乐雷达搜歌
            intent.setClassName("com.voicedragon.musicclient.car",
                    "com.voicedragon.musicclient.car.MainActivity");
            AppUtils.startActivity(context, intent);
            return "";
        }
        return null;
    }
}
