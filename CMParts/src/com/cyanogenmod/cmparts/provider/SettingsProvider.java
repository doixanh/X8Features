package com.cyanogenmod.cmparts.provider;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

public class SettingsProvider extends ContentProvider {
    public static final String AUTHORITY = "com.cyanogenmod.cmparts.provider.Settings";
    public static final String TABLE_NAME = "settings";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);	 

    private static final int SETTINGS = 1;
    private static final int SETTINGS_ID = 2;

    static {
        URI_MATCHER.addURI(AUTHORITY, "settings", SETTINGS);
        URI_MATCHER.addURI(AUTHORITY, "settings/#", SETTINGS_ID);
    }

    private static final String TAG = "SettingsProvider";

    private SQLiteOpenHelper mOpenHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        switch (URI_MATCHER.match(uri)) {

        case SETTINGS:
            count = db.delete(TABLE_NAME, selection, selectionArgs);
            break;

        case SETTINGS_ID:
            String segment = uri.getPathSegments().get(1);

            if (TextUtils.isEmpty(selection)) {
                selection = "_id=" + segment;
            } else {
                selection = "_id=" + segment + " AND (" + selection + ")";
            }

            count = db.delete(TABLE_NAME, selection, selectionArgs);
            break;

        default:
            throw new IllegalArgumentException("Cannot delete from URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        int match = URI_MATCHER.match(uri);
        switch (match) {
        case SETTINGS:
            return Constants.CONTENT_TYPE;

        case SETTINGS_ID:
            return Constants.CONTENT_ITEM_TYPE;

        default:
            throw new IllegalArgumentException("Unknown URI");
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (URI_MATCHER.match(uri) != SETTINGS) {
            throw new IllegalArgumentException("Cannot insert into URI: " + uri);
        }

        ContentValues values = (initialValues != null) ? new ContentValues(initialValues) : new ContentValues();
        for (String colName : Constants.getRequiredColumns()) {
            if (values.containsKey(colName) == false) {
                throw new IllegalArgumentException("Missing column: " + colName);
            }
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, "", values);

        if (rowId < 0) {
            throw new SQLException("Failed to insert row into: " + uri);
        }
        Uri noteUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
        getContext().getContentResolver().notifyChange(noteUri, null);
        return noteUri;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {

        SQLiteQueryBuilder qBuilder = new SQLiteQueryBuilder();
        int match = URI_MATCHER.match(uri);

        switch (match) {

        case SETTINGS:
            qBuilder.setTables(TABLE_NAME);
            break;

        case SETTINGS_ID:
            qBuilder.setTables(TABLE_NAME);
            qBuilder.appendWhere("_id=");
            qBuilder.appendWhere(uri.getPathSegments().get(SETTINGS_ID));
            break;

        default:
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = Constants.DEFAULT_SORT_ORDER;
        } else {
            orderBy = sortOrder;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor ret = qBuilder.query(db, projection, selection, selectionArgs, null, null, orderBy);

        if (ret == null) {
            Log.i(TAG, "query failed");
        } else {
            ret.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return ret;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        int count;
        long rowId = 0;
        int match = URI_MATCHER.match(uri);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        switch (match) {

        case SETTINGS:
            count = db.update(TABLE_NAME, values, selection, null);
            break;
        case SETTINGS_ID:
            String segment = uri.getPathSegments().get(1);
            rowId = Long.parseLong(segment);
            count = db.update(TABLE_NAME, values, "_id=" + rowId, null);
            break;

        default:
            throw new UnsupportedOperationException("Cannot update URI: " + uri);
        }

        Log.i(TAG, "*** notifyChange() rowId: " + rowId + " url " + uri);

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    public static final class Constants implements BaseColumns {
        private Constants() {
        }

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.cyanogenmod.settings";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cyanogenmod.settings";
        public static final String DEFAULT_SORT_ORDER = Constants._ID + " ASC";

        public static final String _ID = "_id";
        public static final String KEY = "key";
        public static final String VALUE = "value";

        public static ArrayList<String> getRequiredColumns() {
            ArrayList<String> tmpList = new ArrayList<String>();
            tmpList.add(KEY);
            tmpList.add(VALUE);
            return tmpList;
        }
    }
}
