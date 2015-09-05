package com.coopox.VoiceNow.NLUProcessor;

import android.content.Context;
import android.content.Intent;
import com.coopox.VoiceNow.CmdDispatcher;
import com.coopox.VoiceNow.NLUQuery;
import com.coopox.common.Constants;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/11/7
 */
public class PhotoProcessor implements CmdDispatcher.CmdProcessor {
    @Override
    public String fire(CmdDispatcher dispatcher, Context context, NLUQuery result) {
        if (null != result && null != result.semantic && null != result.semantic.intent) {
            Map<String, String> attachment = result.semantic.intent;
            String name = attachment.get("name");
            String func = attachment.get("function");
            if (null != context && null != name && (name.equals("拍照") || name.equals("照相"))
                    && null != func && func.equals("FUNC_IMAGE_CAPTURE")) {
                context.sendBroadcast(new Intent(Constants.ACTION_TAKE_PHOTO));
                return "已拍照";
            }
        }
        return null;
    }
}
