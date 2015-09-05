package com.coopox.carlauncher.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.datamodel.AppEntry;

import java.util.List;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-8-29
 */
public class AppEntryAdapter extends BaseAdapter {
    private static final int[] CELL_BACKGROUNDS = {
            R.drawable.bg_page1_cell1,
            R.drawable.bg_page1_cell2,
            R.drawable.bg_page2_cell1,
            R.drawable.bg_page2_cell2,
            R.drawable.bg_page2_cell3,
            R.drawable.bg_page2_cell5,
            R.drawable.bg_page2_cell7,
    };

    private final Context mContext;
    private List<AppEntry> mEntries;
    private Random mRandom;

    public AppEntryAdapter(Context context) {
        mContext = context;
        mRandom = new Random();
    }

    public void setAppEntries(List<AppEntry> entries) {
        mEntries = entries;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        if (null != mEntries) {
            return mEntries.size();
        }
        return 0;
    }

    @Override
    public Object getItem(int i) {
        if (null != mEntries && i < getCount()) {
            return mEntries.get(i);
        }
        return null;
    }

    @Override
    public long getItemId(int i) {
        if (null != mEntries && i < getCount()) {
            return i;
        }
        return -1;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (null == view) {
            view = LayoutInflater.from(mContext).inflate(R.layout.app_entry_cell,
                    viewGroup, false);
        }

        Object item = getItem(i);
        if (item instanceof AppEntry) {
            AppEntry entry = (AppEntry)item;
            TextView entryView = (TextView)view;
            entryView.setText(entry.name);
            entryView.setCompoundDrawables(null,
                    entry.getIconDrawable(mContext), null, null);
            int index = mRandom.nextInt(CELL_BACKGROUNDS.length);
            entryView.setBackgroundResource(CELL_BACKGROUNDS[index]);
        }
        return view;
    }
}
