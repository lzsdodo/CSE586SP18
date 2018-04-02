package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {

    private SQLiteHelper dbHelper;
    private SQLiteDatabase db;

    @Override
    public boolean onCreate() {
        dbHelper = new SQLiteHelper(getContext());
        dbHelper.getWritableDatabase().execSQL("DELETE FROM " + dbHelper.TABLE_NAME + ";"); // Clean Table
        return true;
    }
    
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        db = dbHelper.getWritableDatabase();

        long newRowId = db.insertWithOnConflict(dbHelper.TABLE_NAME,
                dbHelper.getNullColumnHack(), values, db.CONFLICT_IGNORE);

        Log.v("insert", "row=" + newRowId + "\t\'" + values.toString() + "\'");
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        db = dbHelper.getWritableDatabase();

        String newSelection = dbHelper.COLUMN_NAME_KEY + "=?";
        String[] selectedKey = new String[] {selection};
        int affectedRows = db.delete(dbHelper.TABLE_NAME, newSelection, selectedKey);

        Log.v("delete", "affected rows=" + affectedRows + "\t\'" + selection + "\'");
        return affectedRows;
    }

    public int deleteAll() {
        db = dbHelper.getWritableDatabase();

        int affectedRows = db.delete(dbHelper.TABLE_NAME, null, null);

        Log.v("delete all", "affected rows=" + affectedRows);
        return affectedRows;
    }

    // Special string: * / @ as a selection parameter which return all the result for entire DHT or local
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        db = dbHelper.getReadableDatabase();

        String[] columns = new String[] {dbHelper.COLUMN_NAME_KEY, dbHelper.COLUMN_NAME_VALUE};
        String newSelection = dbHelper.COLUMN_NAME_KEY + "=?";
        String[] selectedKey = new String[] {selection};

        Cursor c = db.query(dbHelper.TABLE_NAME, columns, newSelection, selectedKey,
                null, null, null, "1");
        c.moveToFirst();

        Log.v("query", selection);
        return c;
    }

    public Cursor queryAll() {
        db = dbHelper.getReadableDatabase();

        String[] columns = new String[] {dbHelper.COLUMN_NAME_KEY, dbHelper.COLUMN_NAME_VALUE};
        Cursor c = db.query(dbHelper.TABLE_NAME, columns, null, null,
                null, null, null, null);

        Log.v("query all", "");
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }
}
