package com.coopox.carlauncher.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-30
 * 支持多种 Adapter 的 ViewPager。目前的主屏的前两页与后面的应用列表就是不同的Adapter.
 */
public class MultiAdapterViewPager extends ViewPager {
    private static final String TAG = "MultiAdapterViewPager";

    /**
     * 由于本组件可以容纳多种 Adapter，所以在调用 isViewFromObject 方法之前要先判断哪个
     * Adapter 能支持这组 View 和 Object，所以引入本接口让 Adapter 实现并检查自己能否处理
     * 这组参数。*/
    public interface MultiAdapterDelegate {
        boolean supportViewAndObject(View view, Object object);
    }

    private GroupPagerAdapter mWrapperAdapter = new GroupPagerAdapter();

    public MultiAdapterViewPager(Context context) {
        super(context);
    }

    public MultiAdapterViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (!(adapter instanceof MultiAdapterDelegate)) {
            throw new IllegalArgumentException("Adapter must implements MultiAdapterDelegate!");
        }
        addAdapter(0, adapter);
        if (getAdapter() != mWrapperAdapter) {
            super.setAdapter(mWrapperAdapter);
        }
    }

    public void addAdapter(PagerAdapter adapter) {
        if (!(adapter instanceof MultiAdapterDelegate)) {
            throw new IllegalArgumentException("Adapter must implements MultiAdapterDelegate!");
        }
        mWrapperAdapter.addAdapter(adapter);
    }

    public void addAdapter(int index, PagerAdapter adapter) {
        if (!(adapter instanceof MultiAdapterDelegate)) {
            throw new IllegalArgumentException("Adapter must implements MultiAdapterDelegate!");
        }
        mWrapperAdapter.addAdapter(index, adapter);
    }

    public void clear() {
        mWrapperAdapter.clearAdapter();
    }

    public void removeAdapter(PagerAdapter adapter) {
        mWrapperAdapter.removeAdapter(adapter);
    }

    public void removeAdapter(int index) {
        mWrapperAdapter.removeAdapter(index);
    }

    static class GroupPagerAdapter extends PagerAdapter {
        private List<PagerAdapter> mPagerAdapters = new ArrayList<PagerAdapter>(4);

        public void addAdapter(PagerAdapter adapter) {
            addAdapter(mPagerAdapters.size(), adapter);
        }

        public void addAdapter(int index, PagerAdapter adapter) {
            if (null != adapter) {
                mPagerAdapters.add(index, adapter);
                adapter.registerDataSetObserver(new DataSetObserver() {
                    @Override
                    public void onChanged() {
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onInvalidated() {
                        super.onInvalidated();
                    }
                });

                notifyDataSetChanged();
            }
        }

        public void removeAdapter(PagerAdapter adapter) {
            mPagerAdapters.remove(adapter);
            notifyDataSetChanged();
        }

        public void removeAdapter(int index) {
            mPagerAdapters.remove(index);
            notifyDataSetChanged();
        }

        public void clearAdapter() {
            mPagerAdapters.clear();
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            int count = 0;
            for (PagerAdapter adapter : mPagerAdapters) {
                count += adapter.getCount();
            }
            return count;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            for (PagerAdapter adapter : mPagerAdapters) {
                MultiAdapterDelegate delegate = (MultiAdapterDelegate)adapter;
                if (delegate.supportViewAndObject(view, o)) {
                    return (adapter.isViewFromObject(view, o));
                }
            }
            Log.e(TAG, "It hasn't any adapter support View and Object");
            return false;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            int total = 0;
            for (PagerAdapter adapter : mPagerAdapters) {
                int count = adapter.getCount();
                if (position < count + total) {
                    int index = position - total;
                    return adapter.instantiateItem(container, index);
                }
                total += count;
            }
            return null;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            int total = 0;
            for (PagerAdapter adapter : mPagerAdapters) {
                int count = adapter.getCount();
                if (position < count + total) {
                    int index = position - total;
                    adapter.destroyItem(container, index, object);
                    break;
                }
                total += count;
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            int total = 0;
            for (PagerAdapter adapter : mPagerAdapters) {
                int count = adapter.getCount();
                if (position < count + total) {
                    int index = position - total;
                    adapter.setPrimaryItem(container, index, object);
                    break;
                }
                total += count;
            }
        }

        @Override
        public void startUpdate(ViewGroup container) {
            for (PagerAdapter adapter : mPagerAdapters) {
                adapter.startUpdate(container);
            }
        }

        @Override
        public void finishUpdate(ViewGroup container) {
            for (PagerAdapter adapter : mPagerAdapters) {
                adapter.finishUpdate(container);
            }
        }
    }
}
