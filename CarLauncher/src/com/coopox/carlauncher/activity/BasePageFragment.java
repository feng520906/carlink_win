package com.coopox.carlauncher.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.coopox.carlauncher.view.HomePageCell;
import com.umeng.analytics.MobclickAgent;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-3
 */
public abstract class BasePageFragment extends Fragment{
    private static final String TAG = "BasePageFragment";
    protected List<HomePageCell> mAllCells = new ArrayList<HomePageCell>(4);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onPageStart(getClass().getSimpleName());
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPageEnd(getClass().getSimpleName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View contentView = getContentView(inflater, container);

        SparseArray<Class> entries = getEntryMap();
        int size = entries.size();
        for (int i = 0; i < size; ++i) {
            Class cls = entries.valueAt(i);
            if (HomePageCell.class.isAssignableFrom(cls)) {
                try {
                    Constructor constructor = cls.getConstructor(Context.class);
                    HomePageCell cell = (HomePageCell) constructor.newInstance(
                            getActivity());
                    ViewGroup root = (ViewGroup) contentView.findViewById(entries.keyAt(i));
                    if (null != root) {
                        View view = cell.onCreateView(inflater, root, savedInstanceState);
                        if (view != root) {
                            root.addView(view);
                        }
                        mAllCells.add(cell);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i(TAG, "BasePageFragment.onCreateView");
        return contentView;
    }

    @Override
    public void onDestroyView() {
        for (HomePageCell cell : mAllCells) {
            cell.onDestroyView();
        }
        mAllCells.clear();
        super.onDestroyView();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public abstract SparseArray<Class> getEntryMap();

    public abstract View getContentView(LayoutInflater inflater, ViewGroup container);
}
