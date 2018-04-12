package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */

/*
 * Reference:
 * - Android Dev Docs
 *      ContentResolver: https://developer.android.com/reference/android/content/ContentResolver.html
 *      ContentValues: https://developer.android.com/reference/android/content/ContentValues.html
 *      Cursor: https://developer.android.com/reference/android/database/Cursor.html
 *      SQLiteOpenHelper: https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
 */

public class GroupMessengerProvider extends ContentProvider {

    static final String TAG = GroupMessengerProvider.class.getSimpleName();

    private SQLiteHelper dbHelper;
    private SQLiteDatabase db;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        db = dbHelper.getWritableDatabase();
        long newRowId = db.insertWithOnConflict(dbHelper.TABLE_NAME,
                dbHelper.getNullColumnHack(), values, db.CONFLICT_IGNORE);
        Log.v("insert", "row=" + newRowId + "\t" + values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        dbHelper = new SQLiteHelper(getContext());
        // Clean Table
        dbHelper.getWritableDatabase().execSQL("DELETE FROM " + dbHelper.TABLE_NAME + ";");
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        db = dbHelper.getReadableDatabase();

        String[] columns = new String[] {dbHelper.COLUMN_NAME_KEY, dbHelper.COLUMN_NAME_VALUE};
        String newSelecttion = dbHelper.COLUMN_NAME_KEY + "=?";
        String[] keyToRead = new String[] {selection};

        Cursor c = db.query(dbHelper.TABLE_NAME, columns, newSelecttion, keyToRead,
                null, null, null, "1");
        c.moveToFirst();

        Log.v("query", selection);
        return c;
    }
}
