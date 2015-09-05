package com.coopox.carlauncher.datamodel;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.View;
import com.coopox.carlauncher.activity.HomePageFragmentB;
import com.coopox.carlauncher.activity.HomePageFragmentA;
import com.coopox.carlauncher.view.MultiAdapterViewPager;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-5
 * 提供桌面的每个页面
 */
public class HomePageAdapter extends FragmentPagerAdapter implements MultiAdapterViewPager.MultiAdapterDelegate {
    // 记录 Home 页对应的 class
    private static final Class[] PAGES = {
            HomePageFragmentA.class,
            HomePageFragmentB.class
    };

    // Home 页 cache,避免反复创建
    private Fragment[] mPageCache;

    public HomePageAdapter(FragmentManager fm) {
        super(fm);
        mPageCache = new Fragment[PAGES.length];
    }

    @Override
    public Fragment getItem(int pos) {
        if (null == mPageCache[pos]) {
            try {
                mPageCache[pos] = (Fragment) PAGES[pos].newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return mPageCache[pos];
    }

    @Override
    public int getCount() {
        return PAGES.length;
    }

    @Override
    public boolean supportViewAndObject(View view, Object object) {
        return object instanceof Fragment;
    }
}
