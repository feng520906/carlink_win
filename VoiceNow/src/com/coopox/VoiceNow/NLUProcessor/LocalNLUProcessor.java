package com.coopox.VoiceNow.NLUProcessor;

import android.content.Context;
import com.coopox.VoiceNow.CmdDispatcher;
import com.coopox.VoiceNow.NLUQuery;
import com.coopox.VoiceNow.localNLU.LocalNLUParser;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/3/11
 */
public class LocalNLUProcessor implements CmdDispatcher.CmdProcessor {
    public static final String KEY_NO_VALID_NLU = "cn.yunzhisheng.error.ANSWER";

    LocalNLUParser mLocalNLUParser = new LocalNLUParser();

    @Override
    public String fire(CmdDispatcher dispatcher, Context context, NLUQuery result) {
        if (null != result) {
            NLUQuery newQuery = mLocalNLUParser.parse(result.text);
            if (null != newQuery) {
                dispatcher.routeCmd(newQuery);
            } else {
                // TODO: return result.general.text
            }
        }
        return null;
    }
}
