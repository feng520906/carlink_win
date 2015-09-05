package com.coopox.VoiceNow;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-7
 */
public class QueryInfo implements Serializable {
    @SerializedName("raw_text")
    public String rawText;  // 原始文本

    @SerializedName("parsed_text")
    public String parsedText;   // 分词结果

    public static class Result {
        public int demand;
        public String domain;   // 领域
        public String intent;   // 意图
        public float score;     // 置信度
        @SerializedName("object")
        public Map<String, String> attachment;   // 意图附加信息对象
    }

    public Result[] results;    // 意图列表

    public String[] backups;

    public static QueryInfo obtain(String text, String domain, String intent, Map<String, String>attachment) {
        QueryInfo queryInfo = new QueryInfo();
        queryInfo.rawText = text;
        queryInfo.parsedText = text;
        queryInfo.results = new Result[1];
        queryInfo.results[0] = new Result();
        queryInfo.results[0].domain = domain;
        queryInfo.results[0].intent = intent;
        queryInfo.results[0].attachment = attachment;

        return queryInfo;
    }
}
