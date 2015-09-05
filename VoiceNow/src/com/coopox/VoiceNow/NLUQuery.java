package com.coopox.VoiceNow;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/3/9
 */
public class NLUQuery implements Serializable {
    @SerializedName("rc")
    public int responseCode;

    public String text;
    public String service;
    public String code;
    public String history;
    public String responseId;
    public String data;
    public Object general;
    public Map<String, String> error;

    public static class Semantic {
        public Map<String, String> intent;   // 意图附加信息对象
        public String normalHeader;
    }
    public Semantic semantic;

    public String getIntent(String key, String defaultValue) {
        if (null != key && null != semantic && null != semantic.intent
                && semantic.intent.containsKey(key)) {
            return semantic.intent.get(key);
        }
        return defaultValue;
    }

    public String getNormalHeader() {
        if (null != semantic) {
            return semantic.normalHeader;
        }
        return null;
    }
}
