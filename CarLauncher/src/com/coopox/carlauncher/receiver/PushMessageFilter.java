package com.coopox.carlauncher.receiver;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-4
 * 后台推送消息处理器
 */
public interface PushMessageFilter {
    boolean handleMessage(String content, Map<String, String> extras);
}
