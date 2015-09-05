package com.coopox.carlauncher.datamodel;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import com.activeandroid.query.Select;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dk
 * Date: 14/10/20
 */
public class LocationProvider extends ContentProvider {
    public static final String AUTHORITY = "com.coopox.provider.LauncherProvider";
    // Creates a UriMatcher object.
    private static final UriMatcher sUriMatcher;

    private static final int LOCATIONS_INDICATOR = 1;
    private static final int APPENTIES_INDICATOR = 2;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, "locations", LOCATIONS_INDICATOR);
        sUriMatcher.addURI(AUTHORITY, "appEntries", APPENTIES_INDICATOR);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (sUriMatcher.match(uri)) {
            case LOCATIONS_INDICATOR:
                List<LocationModel> models =
                        new Select().from(LocationModel.class).orderBy("Timestamp").execute();

                break;
            case APPENTIES_INDICATOR:
                break;
        }
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
