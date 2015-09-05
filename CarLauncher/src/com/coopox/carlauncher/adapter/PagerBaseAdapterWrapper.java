package com.coopox.carlauncher.adapter;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Created by kanedong on 14-3-4.
 * 对 BaseAdapter 进行包装，以提供翻页机制
 */
public class PagerBaseAdapterWrapper extends BaseAdapter {
    private int mPageItemNum = 1;
    private BaseAdapter mWrapperAdapter;
    private int mPage;

    public PagerBaseAdapterWrapper(BaseAdapter adapter, int pageItemNum) {
        mWrapperAdapter = adapter;
        mPageItemNum = pageItemNum > 0 ? pageItemNum : 1;
        if (null != mWrapperAdapter) {
            mWrapperAdapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    notifyDataSetChanged();
                }

                @Override
                public void onInvalidated() {
                    notifyDataSetInvalidated();
                }
            });
        }
    }

    public BaseAdapter getWrapperAdapter() {
        return mWrapperAdapter;
    }

    public int getPageItemNum() {
        return mPageItemNum;
    }

    public void setCurrentPage(int page) {
        int maxPage =  getMaxPageNum();

        if (page < 0) {
            mPage = 0;
        } else if (page >= maxPage) {
            mPage = maxPage - 1;
        } else {
            mPage = page;
        }
    }

    public int getCurrentPage() {
        return mPage;
    }

    public int getMaxPageNum() {
        int count = mWrapperAdapter.getCount();
        return count / mPageItemNum + (0 != count % mPageItemNum ? 1 : 0);
    }

    @Override
    public int getCount() {
        int count = mWrapperAdapter.getCount();
        return (mPage + 1) * mPageItemNum > count ? count % mPageItemNum : mPageItemNum;
    }

    @Override
    public Object getItem(int position) {
        return mWrapperAdapter.getItem(mPage * mPageItemNum + position);
    }

    @Override
    public long getItemId(int position) {
        return mWrapperAdapter.getItemId(mPage * mPageItemNum + position);
    }

/*    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        mWrapperAdapter.notifyDataSetChanged();
    }*/

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return mWrapperAdapter.getView(mPage * mPageItemNum + position,
                convertView, parent);
    }
}
