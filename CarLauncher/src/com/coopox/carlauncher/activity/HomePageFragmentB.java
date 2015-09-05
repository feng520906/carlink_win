package com.coopox.carlauncher.activity;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.view.HomePageCell;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-4
 */
public class HomePageFragmentB extends BasePageFragment {
    private static final SparseArray<Class> CELL_MAP =
            new SparseArray<Class>();
    static {
        CELL_MAP.put(R.id.page2_cell1, HomePageCell.class);
        CELL_MAP.put(R.id.page2_cell2, HomePageCell.class);
        CELL_MAP.put(R.id.page2_cell3, HomePageCell.class);
        CELL_MAP.put(R.id.page2_cell4, HomePageCell.class);
        CELL_MAP.put(R.id.page2_cell5, HomePageCell.class);
        CELL_MAP.put(R.id.page2_cell6, HomePageCell.class);
        CELL_MAP.put(R.id.page2_cell7, HomePageCell.class);
        CELL_MAP.put(R.id.page2_cell8, HomePageCell.class);
    }

    @Override
    public SparseArray<Class> getEntryMap() {
        return CELL_MAP;
    }

    @Override
    public View getContentView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.homepage_b, container, false);
    }
}
