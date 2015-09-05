package com.coopox.carlauncher.adapter;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.v4.view.PagerAdapter;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import com.coopox.carlauncher.view.MultiAdapterViewPager;

/**
 * Created by kanedong on 14-3-4.
 * 结合 PagerBaseAdapterWrapper 使 ViewPager 控件支持每页都显示一个 AdapterView（如 GridView）
 * 而所有页面的 AdapterView 都共享同一个 BaseAdapter.
 */
public class AdapterViewPagerAdapter extends PagerAdapter implements MultiAdapterViewPager.MultiAdapterDelegate {

    public interface AdapterViewFactory {
        AdapterView create(Context context, int page);
    }

    // ViewPager 单页构建 Delegate, 用于将构建每一页的逻辑委托到外部对象。
    public interface PageInflateDelegate {
        // container: ViewPager 对象引用
        // adapterView: 通过 AdapterViewFactory 创建的 AdapterView
        // page: 当前构建的是哪一页
        // 返回值，请返回对于每页唯一的对象作为 Key
        View inflate(ViewGroup container, AdapterView adapterView, int page);

        // container: ViewPager 对象引用
        // view: 通过 AdapterViewFactory 创建的 AdapterView
        // page: 当前销毁的是哪一页
        void deflate(ViewGroup container, View view, int page);
    }

    private AdapterViewFactory mAdapterViewFactory;
    private PagerBaseAdapterWrapper mPagerBaseAdapter;
    private SparseArray<AdapterView> mAdapterViewCache;
    private Context mContext;
    private PageInflateDelegate mInflateDelegate;

    public AdapterViewPagerAdapter(Context context, BaseAdapter adapter, int pageItemNum) {
        mAdapterViewCache = new SparseArray<AdapterView>();
        mContext = context;
        mPagerBaseAdapter = new PagerBaseAdapterWrapper(adapter, pageItemNum);
        mPagerBaseAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                super.onInvalidated();
            }
        });
    }

    public void setAdapterViewFactory(AdapterViewFactory factory) {
        mAdapterViewFactory = factory;
    }

    public void setInflateDelegate(PageInflateDelegate delegate) {
        mInflateDelegate = delegate;
    }

    @Override
    public int getCount() {
        return mPagerBaseAdapter.getMaxPageNum();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        AdapterView adapterView = obtainAdapterView(position);
        if (null != mInflateDelegate) {
            return mInflateDelegate.inflate(container, adapterView, position);
        } else {
            container.addView(adapterView);
            return adapterView;
        }
    }

    public AdapterView obtainAdapterView(int page) {
        AdapterView adapterView =  mAdapterViewCache.get(page);
        if (null == adapterView) {
            if (null != mAdapterViewFactory) {
                adapterView = mAdapterViewFactory.create(mContext, page);
            } else {
                throw new IllegalArgumentException("setAdapterViewFactory should be call!");
            }

            PagerBaseAdapterWrapper adapterWrapper =
                    new PagerBaseAdapterWrapper(mPagerBaseAdapter.getWrapperAdapter(),
                            mPagerBaseAdapter.getPageItemNum());
            adapterWrapper.setCurrentPage(page);
            adapterView.setAdapter(adapterWrapper);

            mAdapterViewCache.put(page, adapterView);
        }
        return adapterView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        if (null != mInflateDelegate) {
            mInflateDelegate.deflate(container, (View)object, position);
        } else {
            container.removeView((View)object);
        }
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public boolean supportViewAndObject(View view, Object object) {
        return view instanceof AdapterView;
    }
}
