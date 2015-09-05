package com.coopox.carlauncher.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.coopox.carlauncher.R;
import com.coopox.carlauncher.activity.HomeScreenActivity;
import com.coopox.carlauncher.datamodel.AppEntry;
import com.coopox.carlauncher.datamodel.FavoriteAppEntries;
import com.coopox.carlauncher.misc.Utils;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14-9-3
 */
public class HomePageCell implements View.OnClickListener {
    protected Context mContext;

    public HomePageCell(Context context) {
        mContext = context;
    }

    public Activity getActivity() {
        return (Activity) mContext;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        if (mContext instanceof HomeScreenActivity) {
            HomeScreenActivity activity = (HomeScreenActivity)mContext;
            FavoriteAppEntries favoriteAppEntries = activity.getFavoriteAppEntries();

            Object tag = root.getTag();
            if (tag instanceof String) {
                String name = (String)tag;
                Resources res = mContext.getResources();
                AppEntry appEntry = favoriteAppEntries.getEntryByName(name);
                if (null != appEntry) {
                    TextView cellView = new TextView(mContext);
                    cellView.setText(appEntry.name);
                    cellView.setCompoundDrawables(null,
                            appEntry.getIconDrawable(mContext), null, null);
                    cellView.setTextColor(res.getColor(R.color.white));
                    cellView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            res.getDimensionPixelSize(R.dimen.favorite_cell_font_size));
                    cellView.setGravity(Gravity.CENTER);

                    root.setTag(appEntry);
                    root.setOnClickListener(this);
                    return cellView;
                }
            }
        }
        return null;
    }

    public void onDestroyView() {

    }

    @Override
    public void onClick(View v) {
        Object obj = v.getTag();
        if (obj instanceof AppEntry) {
            AppEntry entry = (AppEntry)obj;
            Utils.startAppEntry(mContext, entry);
        }
    }
}
