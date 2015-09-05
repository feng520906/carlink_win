package com.coopox.carlauncher.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.datamodel.NewsModel;
import com.coopox.carlauncher.misc.MiscConstants;
import com.coopox.carlauncher.misc.PushCmd;
import com.coopox.carlauncher.receiver.PushMessageFilter;
import com.coopox.carlauncher.receiver.PushMsgLocalReceiver;
import com.coopox.common.tts.TTSClient;
import com.coopox.common.utils.Checker;
import com.coopox.common.utils.ThreadManager;

import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-5
 */
public class MessageCell extends HomePageCell implements AdapterView.OnItemClickListener {
    private LayoutInflater mInflater;
    private ViewGroup mCellView;
    private ListView mListView;
    private Dialog mNewsDialog;

    // TODO: 为了方便暂时使用 ArrayAdapter，之后需要扩展自定义 Adapter
    private ArrayAdapter<String> mAdapter;
    private PushMessageFilter mNewsFilter = new PushMessageFilter() {
        @Override
        public boolean handleMessage(String content, Map<String, String> extras) {
            if (extras.containsKey(PushCmd.CMD_NEWS)) {
                NewsModel news = new NewsModel();
                news.title = extras.get(PushCmd.CMD_NEWS);
                news.content = content;
                long currentTime = System.currentTimeMillis();
                news.begin = currentTime;
                news.playBack = 1;  // 消息默认可报读
                news.end = currentTime + MiscConstants.MILLIS_PER_DAY; // 默认自收到消息起 24 小时内有效
                try {
                    if (extras.containsKey(PushCmd.CMD_PLAY_BACK)) {
                        news.playBack = Integer.parseInt(extras.get(PushCmd.CMD_PLAY_BACK));
                    }
                    if (extras.containsKey(PushCmd.CMD_START)) {
                        news.begin = Long.parseLong(extras.get(PushCmd.CMD_START));
                    }
                    if (extras.containsKey(PushCmd.CMD_STOP)) {
                        news.end = Long.parseLong(extras.get(PushCmd.CMD_STOP));
                        if (news.end < currentTime) {
                            return true;    // 消息已经过期，丢弃
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // 消息去重
                NewsModel existNews =
                        new Select().from(NewsModel.class).where("Title = ?", news.title).executeSingle();
                if (null != existNews && existNews.end > currentTime) {
                    return true;
                }
                news.save();    // 保存消息
                reloadNews();   // 重新加载消息
                return true;
            }
            return false;
        }
    };

    public MessageCell(Context context) {
        super(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        mInflater = inflater;
        mCellView = root;
        if (null != root) {
            mInflater.inflate(R.layout.cell_message, root);
            mListView = (ListView) root.findViewById(R.id.news_list);
            mAdapter = new ArrayAdapter<String>(mContext, R.layout.news_item);
            mListView.setAdapter(mAdapter);
            mListView.setOnItemClickListener(this);

            root.setOnClickListener(this);
            PushMsgLocalReceiver.registerPushMsgFilter(mNewsFilter);

            reloadNews();
        }

        return root;
    }

    @Override
    public void onDestroyView() {
        if (null != mNewsDialog) {
            mNewsDialog.cancel();
            mNewsDialog = null;
        }

        PushMsgLocalReceiver.unregisterPushMsgFilter(mNewsFilter);
        super.onDestroyView();
    }

    @Override
    public void onClick(View v) {
        // TODO: 这个逻辑最好重构到父类里做
/*        if (mContext instanceof HomeScreenActivity) {
            String appName = mContext.getString(R.string.dial);
            AppEntry entry = ((HomeScreenActivity) mContext).getFavoriteAppEntries().getEntryByName(appName);
            Utils.startAppEntry(mContext, entry);
        }*/
    }

    /**
     * 重新加载新闻类消息，将时间段符合的消息显示出来，并将已经过期的消息删除 */
    private void reloadNews() {
        // 数据库相关的操作都辨到工作线程，避免第一次安装时被耗时的 AppEntry 搜集流程阻塞
        ThreadManager.INSTANCE.runOnWorkerThread(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                // 先将已经过期的新闻清除
                new Delete().from(NewsModel.class).where("EndTime<?", currentTime).execute();

                // 再筛选时间符合的新闻用于显示
                String condition = String.format("BeginTime<=%d AND EndTime>%d",
                        currentTime, currentTime);
                List<NewsModel> news =
                        new Select().from(NewsModel.class).orderBy("BeginTime").where(condition).execute();

                updateNewsListView(news);
            }
        });
    }

    private void updateNewsListView(final List<NewsModel> newsModels) {
        ThreadManager.INSTANCE.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!Checker.isEmpty(newsModels) && null != mAdapter) {
                    mAdapter.clear();
                    for (NewsModel item : newsModels) {
                        mAdapter.add(item.title);
                    }
                }
            }
        });
    }

    // 点击报读具体内容
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Adapter adapter = parent.getAdapter();
        Object obj = adapter.getItem(position);
        if (obj instanceof String) {
            String title = (String)obj;
            List<NewsModel> news =
                    new Select().from(NewsModel.class).orderBy("BeginTime").where("Title=?", title).execute();
            if (!Checker.isEmpty(news)) {
                long currentTime = System.currentTimeMillis();
                for (NewsModel item : news) {
                    if (item.begin <= currentTime && item.end > currentTime &&
                            !Checker.isEmpty(item.content) && 0 != item.playBack) {
                        // 停止正在报读的内容，并开始读本条内容
                        TTSClient.speakNow(getActivity(), item.content, null);

                        mNewsDialog = createNewsDialog(item);
                        mNewsDialog.show();
                        break;
                    }
                }
            }
        }
    }

    private Dialog createNewsDialog(NewsModel item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(item.title).setMessage(item.content).
                setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
//                TTSManager.INSTANCE.cancel();
            }
        });
        return builder.create();
    }
}
