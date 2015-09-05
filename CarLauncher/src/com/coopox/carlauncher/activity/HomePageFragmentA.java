package com.coopox.carlauncher.activity;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.view.HomePageCell;
import com.coopox.carlauncher.view.MessageCell;
import com.coopox.carlauncher.view.WeatherCell;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-4
 */
public class HomePageFragmentA extends BasePageFragment {
    private static final SparseArray<Class> CELL_MAP =
            new SparseArray<Class>();
    static {
        CELL_MAP.put(R.id.page1_cell1, HomePageCell.class);
        CELL_MAP.put(R.id.page1_cell2, HomePageCell.class);
        CELL_MAP.put(R.id.page1_cell3, WeatherCell.class);
        CELL_MAP.put(R.id.page1_cell4, MessageCell.class);
        CELL_MAP.put(R.id.page1_cell5, HomePageCell.class);
        CELL_MAP.put(R.id.page1_cell6, HomePageCell.class);
        CELL_MAP.put(R.id.page1_cell7, HomePageCell.class);
        CELL_MAP.put(R.id.page1_cell8, HomePageCell.class);
    }

    @Override
    public SparseArray<Class> getEntryMap() {
        return CELL_MAP;
    }

    @Override
    public View getContentView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.homepage_a, container, false);
    }
}
