/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.coopox.VoiceNow.NLUProcessor;

import android.content.Context;
import android.content.Intent;
import com.coopox.VoiceNow.CmdDispatcher;
import com.coopox.VoiceNow.NLUQuery;

/**
 * Created with IntelliJ IDEA.
 * User: kanedong
 * Date: 14/11/12
 */
public class SharePhotoProcessor implements CmdDispatcher.CmdProcessor {
    @Override
    public String fire(CmdDispatcher dispatcher, Context context, NLUQuery result) {
        if (null != result && null != context) {
            if (result.text.equals("分享照片")) {
                Intent serviceIntent = new Intent("com.coopox.service.action.SHARE2WX");
                serviceIntent.putExtra("share", true);
                context.startService(serviceIntent);
                return "发起微信分享";
            }
        }
        return null;
    }
}
