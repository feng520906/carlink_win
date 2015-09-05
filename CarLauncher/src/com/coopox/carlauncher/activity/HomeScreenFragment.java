package com.coopox.carlauncher.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.adapter.AdapterViewPagerAdapter;
import com.coopox.carlauncher.adapter.AppEntryAdapter;
import com.coopox.carlauncher.business.AppUpdateChecker;
import com.coopox.carlauncher.datamodel.*;
import com.coopox.carlauncher.misc.DateChecker;
import com.coopox.carlauncher.misc.PushCmd;
import com.coopox.carlauncher.misc.Utils;
import com.coopox.carlauncher.receiver.PackageManagerReceiver;
import com.coopox.carlauncher.receiver.PushMessageFilter;
import com.coopox.carlauncher.receiver.PushMsgLocalReceiver;
import com.coopox.carlauncher.view.MultiAdapterViewPager;
import com.coopox.common.tts.TTSClient;
import com.coopox.common.utils.ThreadManager;
import com.viewpagerindicator.CirclePageIndicator;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: lokii
 * Date: 15/2/10
 */
public class HomeScreenFragment extends Fragment implements AppEntriesLoader.LoadListener {

    private static final String CHECK_PUSHED_APP = "AppPushed";
    private static boolean sIsUpgradeChecked;   // 在 Launcher 启动后的整个生命周期内只自动触发一次更新检测

    class VoiceMessageFilter implements PushMessageFilter {

        @Override
        public boolean handleMessage(String content, Map<String, String> extras) {
            if (extras.containsKey(PushCmd.CMD_VOICE) &&
                    Boolean.parseBoolean(extras.get(PushCmd.CMD_VOICE))) {
                TTSClient.speak(getActivity(), content, null);
            }
            return false;
        }
    }
    private static final String ACTION_BAIDU_PUSH_MESSAGE = "com.baidu.android.pushservice.action.MESSAGE";
    private AppEntryAdapter mAppEntryAdapter;
    private FavoriteAppEntries mFavoriteEntries;
    public PushMsgLocalReceiver mPushMsgLocalReceiver;
    private VoiceMessageFilter mVoiceMessageFilter;
    private PackageManagerReceiver mPackageManagerReceiver = new PackageManagerReceiver();
    private AppUpdateChecker mAppUpdateChecker;

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.homescreen, container, false);
        mFavoriteEntries = new FavoriteAppEntries(getActivity());
        mAppEntryAdapter = new AppEntryAdapter(getActivity());
        AdapterViewPagerAdapter pagerAdapter = new AdapterViewPagerAdapter(
                getActivity(), mAppEntryAdapter, 15);
        pagerAdapter.setAdapterViewFactory(new AdapterViewPagerAdapter.AdapterViewFactory() {
            @Override
            public AdapterView create(Context context, int page) {
                GridView gridView =
                        (GridView) inflater.inflate(R.layout.app_cell_page, null);
                gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Object item = parent.getAdapter().getItem(position);
                        if (item instanceof AppEntry) {
                            AppEntry entry = (AppEntry) item;
                            Utils.startAppEntry(getActivity(), entry);
                        }
                    }
                });
                return gridView;
            }
        });

        MultiAdapterViewPager viewPager = (MultiAdapterViewPager) rootView.findViewById(R.id.viewpager);
        if (null != viewPager) {
            HomePageAdapter adapter = new HomePageAdapter(getChildFragmentManager());
            viewPager.addAdapter(pagerAdapter);
            viewPager.setAdapter(adapter);

            CirclePageIndicator indicator = (CirclePageIndicator) rootView.findViewById(R.id.indicator);
            if (null != indicator) {
                indicator.setViewPager(viewPager);
            }
        }
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // 应用更新是通过 Service 完成的，其生命周期和本 Activity 不一致，所以使用 Application
        // 作为 Context
        mAppUpdateChecker = new AppUpdateChecker(CarApplication.getAppContext());

//        Debug.startMethodTracing("carlauncher");
        AppEntriesLoader.INSTANCE.setLoadListener(this);
        AppEntriesLoader.INSTANCE.postLoad();

        // 在 AppEntry 开始加载后再注册 Package 消息监听器，不然在 onReceiver 后 AppEntry
        // 列表可能还未准备就绪，导致多执行一次 postLoad.
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mPackageManagerReceiver, filter);


        // 开始接受推送消息
        PushManager.startWork(getActivity(), PushConstants.LOGIN_TYPE_API_KEY,
                Utils.getMetaValue(getActivity(), "api_key"));

        mVoiceMessageFilter = new VoiceMessageFilter();
        mPushMsgLocalReceiver = new PushMsgLocalReceiver();
        PushMsgLocalReceiver.registerPushMsgFilter(mVoiceMessageFilter);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mPushMsgLocalReceiver,
                new IntentFilter(ACTION_BAIDU_PUSH_MESSAGE));
    }

    @Override
    public void onStart() {
        super.onStart();
        LocationManager.INSTANCE.startLocation();
    }

    @Override
    public void onStop() {
        super.onStop();
        LocationManager.INSTANCE.stopLocation();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        PushMsgLocalReceiver.unregisterPushMsgFilter(mVoiceMessageFilter);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mPushMsgLocalReceiver);
        getActivity().unregisterReceiver(mPackageManagerReceiver);
        AppEntriesLoader.INSTANCE.setLoadListener(null);
        // 停止接受推送消息
        PushManager.stopWork(getActivity());
        mFavoriteEntries = null;
    }

    @Override
    public void onLoaded(boolean useCache, List<AppEntry> allEntries) {

        Collection<AppEntry> favoriteEntries = mFavoriteEntries.getFavoriteAppEntries();
        List<AppEntry> nonFavoriteEntries = allEntries;
        if (null != favoriteEntries) {
            nonFavoriteEntries = new ArrayList<AppEntry>(allEntries);
            for (AppEntry favEntry : favoriteEntries) {
                Iterator<AppEntry> iterator = nonFavoriteEntries.iterator();
                while (iterator.hasNext()) {
                    AppEntry appEntry = iterator.next();
                    if (null != favEntry && null != appEntry) {
                        // 如果入口已经在前两屏的显示了，则不在后面的应用列表里再显示：
                        if (appEntry.intent.filterEquals(favEntry.intent)) {
                            iterator.remove();
                        }
                    }
                }
            }
        }

        final List<AppEntry>results = nonFavoriteEntries;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Debug.stopMethodTracing();
                mAppEntryAdapter.setAppEntries(results);
            }
        });

        if (!sIsUpgradeChecked
                && DateChecker.checkDays(getActivity(), CHECK_PUSHED_APP, 0/*早期版本可能不稳定，所以每次启动 Launcher 都自动检测更新*/)) {
            sIsUpgradeChecked = true;

            ThreadManager.INSTANCE.getIOThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mAppUpdateChecker.check();
                }
            }, 10000); // 隔几秒才做更新检测，以免刚开机时网络还未就绪导致检测不到更新。
        }
    }

    public FavoriteAppEntries getFavoriteAppEntries() {
        return mFavoriteEntries;
    }
}
