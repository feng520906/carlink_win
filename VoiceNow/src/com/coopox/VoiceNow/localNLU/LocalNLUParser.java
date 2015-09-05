package com.coopox.VoiceNow.localNLU;

import android.text.TextUtils;
import com.coopox.VoiceNow.NLUQuery;
import com.coopox.VoiceNow.QueryInfo;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/11/14
 */
public class LocalNLUParser {
    public interface Parser {
        NLUQuery parse(String text);
    }

    private static final Parser[] PARSERS = {
            new MapRouteParser(),
            new AppOpenParser(),
            new AppCloseParser(),
            new RecognitionMusicParser(),
    };

    public NLUQuery parse(String text) {
        for (Parser parser : PARSERS) {
            NLUQuery query = parser.parse(text);
            if (null != query) {
                return query;
            }
        }
        return null;
    }

    static class MapRouteParser implements Parser {

        private static final String PATTERN = ".*(去|到)(.+)";

        @Override
        public NLUQuery parse(String text) {
            if (!TextUtils.isEmpty(text)) {
                Pattern pattern = Pattern.compile(PATTERN);
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String target = matcher.group(2);
                    if (!TextUtils.isEmpty(target)) {
                        return new Gson().fromJson("{\n" +
                                "    \"rc\": 0,\n" +
                                "    \"text\": \"" + text + "\",\n" +
                                "    \"service\": \"cn.yunzhisheng.map\",\n" +
                                "    \"code\": \"ROUTE\",\n" +
                                "    \"semantic\": {\n" +
                                "        \"intent\": {\n" +
                                "            \"fromPOI\": \"CURRENT_LOC\",\n" +
                                "            \"fromCity\": \"CURRENT_CITY\",\n" +
                                "            \"toPOI\": \"" + target + "\",\n" +
                                "            \"toCity\": \"CURRENT_CITY\",\n" +
                                "            \"taskName\": \"query\"\n" +
                                "        },\n" +
                                "        \"normalHeader\": \"正在为您查找去世界之窗的路线\"\n" +
                                "    }\n" +
                                "}", NLUQuery.class);
                    }
                }
            }
            return null;
        }
    }

    static class AppOpenParser implements Parser {

        private static final String PATTERN = ".*(打开|启动|进入|运行)(.+)";

        @Override
        public NLUQuery parse(String rawText) {
            if (!TextUtils.isEmpty(rawText)) {
                Pattern pattern = Pattern.compile(PATTERN);
                Matcher matcher = pattern.matcher(rawText);
                if (matcher.find()) {
                    String target = matcher.group(2);
                    if (!TextUtils.isEmpty(target)) {
                        return new Gson().fromJson("{\n" +
                                "    \"rc\": 0,\n" +
                                "    \"text\": \"" + rawText + "\",\n" +
                                "    \"service\": \"cn.yunzhisheng.appmgr\",\n" +
                                "    \"code\": \"APP_LAUNCH\",\n" +
                                "    \"semantic\": {\n" +
                                "        \"intent\": {\n" +
                                "            \"name\": \"" + target + "\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}", NLUQuery.class);
                    }
                }
            }
            return null;
        }
    }

    static class AppCloseParser implements Parser {

        private static final String PATTERN = ".*(关闭|退出|停止)(.+)";

        @Override
        public NLUQuery parse(String rawText) {
            if (!TextUtils.isEmpty(rawText)) {
                Pattern pattern = Pattern.compile(PATTERN);
                Matcher matcher = pattern.matcher(rawText);
                if (matcher.find()) {
                    String target = matcher.group(2);
                    if (!TextUtils.isEmpty(target)) {
                        return new Gson().fromJson("{\n" +
                                "    \"rc\": 0,\n" +
                                "    \"text\": \"" + rawText + "\",\n" +
                                "    \"service\": \"cn.yunzhisheng.appmgr\",\n" +
                                "    \"code\": \"APP_EXIT\",\n" +
                                "    \"semantic\": {\n" +
                                "        \"intent\": {\n" +
                                "            \"name\": \"" + target + "\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "}", NLUQuery.class);
                    }
                }
            }
            return null;
        }
    }

    static class RecognitionMusicParser implements Parser {

        private static final String PATTERN = ".*(搜歌|什么歌|什么音乐|啥歌).*";

        @Override
        public NLUQuery parse(String rawText) {
            if (!TextUtils.isEmpty(rawText)) {
                Pattern pattern = Pattern.compile(PATTERN);
                Matcher matcher = pattern.matcher(rawText);
                if (matcher.find()) {
                    return new Gson().fromJson("{\n" +
                            "    \"rc\": 0,\n" +
                            "    \"text\": \"" + rawText + "\",\n" +
                            "    \"service\": \"cn.yunzhisheng.music\",\n" +
                            "    \"code\": \"SEARCH_BILLBOARD\",\n" +
                            "    \"semantic\": {\n" +
                            "        \"intent\": {\n" +
                            "            \"keyword\": \"HOT\"\n" +
                            "        }\n" +
                            "    }\n" +
                            "}", NLUQuery.class);
                }
            }
            return null;
        }
    }
}
