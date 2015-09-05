package com.coopox.VoiceNow;

import android.content.Context;
import com.coopox.VoiceNow.NLUProcessor.*;
import com.coopox.common.utils.Checker;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-10-7
 */
public class CmdDispatcher {

    public interface CmdProcessor {
        // TODO: Add param attachment match result
        // 处理指令成功返回非 null String，用于做语音反馈播报
        String fire(CmdDispatcher dispatcher, Context context/* 可能为null */, NLUQuery result);
    }

    private Map<String, CmdProcessor[]> sCmdProcessors =
            new HashMap<String, CmdProcessor[]>(8);
    private WeakReference<Context> mContextRef;

    static {
    }

    public CmdDispatcher(Context context) {
        if (null != context) {
            mContextRef = new WeakReference<Context>(context);

            // Key is service + . + code
            sCmdProcessors.put(MapRouteProcessor.KEY_MAP_ROUTE,
                    new CmdProcessor[]{new MapRouteProcessor()});

            sCmdProcessors.put(AppOpenProcessor.KEY_APP_LAUNCH, new CmdProcessor[]{
                    new SharePhotoProcessor(),
                    new PhotoProcessor(),
                    new AppOpenProcessor(),
            });

            sCmdProcessors.put(PoiProcessor.KEY_SEARCH_POI1, new CmdProcessor[]{new PoiProcessor()});
            sCmdProcessors.put(PoiProcessor.KEY_SEARCH_POI2, new CmdProcessor[]{new PoiProcessor()});
            sCmdProcessors.put(PoiProcessor.KEY_POSITION_POI, new CmdProcessor[]{new PoiProcessor()});

            sCmdProcessors.put(MatchSongProcessor.KEY_MATCH_SONG, new CmdProcessor[]{new MatchSongProcessor()});

//            sCmdProcessors.put("sns.share", new CmdProcessor[]{new SharePhotoProcessor()});
            sCmdProcessors.put(AppOpenProcessor.KEY_APP_EXIT, new CmdProcessor[]{new AppOpenProcessor()});

            sCmdProcessors.put(LocalNLUProcessor.KEY_NO_VALID_NLU, new CmdProcessor[]{new LocalNLUProcessor()});
        }
    }

    public String routeCmd(NLUQuery query) {
        if (null != query) {
            String key = String.format("%s.%s", query.service, query.code);
            CmdProcessor[] cmdProcessors = sCmdProcessors.get(key);
            Context context = mContextRef.get();
            if (!Checker.isEmpty(cmdProcessors)) {
                for (CmdProcessor processor : cmdProcessors) {
                    String ret = processor.fire(this, context, query);
                    if (null != ret) {
                        return ret;
                    }
                }
            }
        }
        return null;
    }

    public String routeCmd(QueryInfo queryInfo, String[] backup) {
/*        if (null != queryInfo) {
            QueryInfo.Result[] results = queryInfo.results;
            if (null != results && results.length > 0) {
                for (QueryInfo.Result result : results) {
                    String key = String.format("%s.%s",
                            result.domain, result.intent);
                    CmdProcessor[] cmdProcessors = sCmdProcessors.get(key);
                    Context context = mContextRef.get();
                    if (!Checker.isEmpty(cmdProcessors)) {
                        for (CmdProcessor processor : cmdProcessors) {
                            String ret = processor.fire(context, result, backup);
                            if (null != ret) {
                                return ret;
                            }
                        }
                    }
                }
            }
        } else {
        }*/

        return null;
    }

}
