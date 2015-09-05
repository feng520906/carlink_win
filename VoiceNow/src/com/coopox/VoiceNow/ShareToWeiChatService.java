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

package com.coopox.VoiceNow;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import com.coopox.common.Constants;
import com.coopox.common.storage.PublicExternalStorage;
import com.coopox.common.storage.Storage;
import com.coopox.common.tts.TTSClient;
import com.coopox.common.utils.Checker;
import com.tencent.mm.sdk.openapi.*;
import com.tencent.mm.sdk.platformtools.Util;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: kanedong
 * Date: 14/11/12
 */
public class ShareToWeiChatService extends IntentService {
    private static final String TAG = "ShareWX";
    private static final int THUMB_SIZE = 150;

    private IWXAPI mApi;

    public ShareToWeiChatService() {
        this("ShareToWeiChat");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ShareToWeiChatService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApi = WXAPIFactory.createWXAPI(this, Constants.WECHAT_APP_ID, true);
        if (mApi.isWXAppInstalled() && mApi.isWXAppSupportAPI() &&
                mApi.getWXAppSupportAPI() >= 0x21020001) {
            boolean ret = mApi.registerApp(Constants.WECHAT_APP_ID);
            Log.d(TAG, "Weixin register result = " + ret);
        } else {
            Log.d(TAG, "WeChat not installed or to old version!");
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
//        mApi.unregisterApp();
        Log.d(TAG, "Weixin unregister");
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");
        if (null != intent && intent.hasExtra("share")) {
            Log.d(TAG, "Start share picture.");
            sharePictureToTimeLine();
        }
    }

    private void sharePictureToTimeLine() {
        File file = findLastPicture();
        if (null != file) {
            Log.d(TAG, String.format("Share picture %s To WeiChat", file.getPath()));
            String path = file.getPath();

            WXImageObject imgObj = new WXImageObject();
            imgObj.setImagePath(path);

            WXMediaMessage msg = new WXMediaMessage();
            msg.mediaObject = imgObj;

            Bitmap bmp = BitmapFactory.decodeFile(path);
            Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, THUMB_SIZE, THUMB_SIZE, true);
            bmp.recycle();
            msg.thumbData = Util.bmpToByteArray(thumbBmp, true);

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = buildTransaction("img");
            req.message = msg;
            req.scene = SendMessageToWX.Req.WXSceneTimeline;
            if (mApi.sendReq(req)) {
                Log.w(TAG, "Send Req to wechat success!");
            } else {
                Log.w(TAG, "send Req to wechat failed!");
            }

        } else {
            TTSClient.speakNow(this, "分享之前请先拍照", null);
        }
    }

    private File findLastPicture() {
        Storage storage = new PublicExternalStorage(
                Environment.DIRECTORY_PICTURES, "");
        File dir = storage.getFile();
        if (null != dir && dir.isDirectory()) {
            Log.d(TAG, String.format("Try to find last picture in %s", dir.getPath()));
            File[] allPicFiles = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
//                    Log.d(TAG, String.format("Check file %s", pathname));
                    String fileName = pathname.getName();
                    String prefix = fileName.substring(fileName.lastIndexOf(".") + 1);
                    return pathname.isFile() && prefix.equals("jpg");
                }
            });

            if (!Checker.isEmpty(allPicFiles)) {
                Log.d(TAG, String.format("Find %d pictures", allPicFiles.length));
                List<File> sortedPicList = Arrays.asList(allPicFiles);
                Collections.sort(sortedPicList, new Comparator<File>() {
                    @Override
                    public int compare(File lhs, File rhs) {
                        // 按时间降序排序，分享最新的一张图片
                        return rhs.getName().compareToIgnoreCase(lhs.getName());
                    }
                });

                if (!sortedPicList.isEmpty()) {
                    return sortedPicList.get(0);
                }
            }
        }
        return null;
    }

    private String buildTransaction(final String type) {
        return (type == null) ? String.valueOf(System.currentTimeMillis()) : type + System.currentTimeMillis();
    }
}
