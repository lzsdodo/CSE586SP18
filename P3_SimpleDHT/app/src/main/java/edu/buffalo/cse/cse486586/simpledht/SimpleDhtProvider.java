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

    // API, with logic control in these functions
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // 1. get key
        // 2. get kid, genHash(key)
        // 3. lookup(kid)
        // 4. insert to local or send to other nodes
        this.insertOne(values);
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int affectedRows = 0;

        if (selection.equals("@")) {
            affectedRows = this.deleteAll();
        } else if (selection.equals("*")) {
            affectedRows = this.deleteAll();
            // Send msg to other nodes
        } else {
            // lookup(genHash(key))
            // Delete local or Send msg to other nodes
            affectedRows = this.deleteOne(selection);
        }

        return affectedRows;
    }

    // Special string: * / @ as a selection parameter which return all the result for entire DHT or local
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if (selection.equals("@")) {
            // By local? or other node?
            return this.queryAll();
        } else {
            if (selection.equals("*")) {
                return this.queryAll();
            } else {
                return this.queryOne(selection);
            }
        }
    }


    // Basic insert, query and delete operation on database
    private void insertOne(ContentValues values) {
        db = dbHelper.getWritableDatabase();

        long newRowId = db.insertWithOnConflict(dbHelper.TABLE_NAME,
                null, values, db.CONFLICT_IGNORE);

        Log.v("INSERT", "ROW=" + newRowId + "; KV=\'" +
                values.getAsString("key") + ", " +
                values.getAsString("value") + "\'");

    }

    private Cursor queryOne(String key) {
        String[] columns = new String[] {dbHelper.COLUMN_NAME_KEY, dbHelper.COLUMN_NAME_VALUE};
        String selection = dbHelper.COLUMN_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        Cursor c = dbHelper.getReadableDatabase().query(
                dbHelper.TABLE_NAME, columns, selection, selectedKey,
                null, null, null, "1");
        c.moveToFirst();

        Log.v("QUERY", "KV=\'" + key + ", " + c.getString(c.getColumnIndex("value")) + "\'");
        return c;
    }

    private Cursor queryAll() {
        String[] columns = new String[] {dbHelper.COLUMN_NAME_KEY, dbHelper.COLUMN_NAME_VALUE};

        Cursor c = dbHelper.getReadableDatabase().query(
                dbHelper.TABLE_NAME, columns, null, null,
                null, null, null);
        c.moveToFirst();

        Log.v("QUERY ALL", c.getCount() + " ROWS.");
        return c;
    }

    private int deleteOne(String key) {
        String selection = dbHelper.COLUMN_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        int affectedRows = dbHelper.getWritableDatabase().delete(
                dbHelper.TABLE_NAME, selection, selectedKey);

        Log.v("DELETE", "KEY=\'" + key + "\'");
        return affectedRows;
    }

    private int deleteAll() {
        int affectedRows = dbHelper.getWritableDatabase().delete(
                dbHelper.TABLE_NAME, null, null);
        Log.v("DELETE ALL", affectedRows + " ROWS.");
        this.countRows();
        return affectedRows;
    }

    private void countRows() {
        Cursor c = dbHelper.getReadableDatabase().query(
                dbHelper.TABLE_NAME, null, null, null,
                null, null, null);
        Log.v("COUNT", c.getCount() + "");
        c.close();
    }


    // Other basic functions
    @Override
    public boolean onCreate() {
        dbHelper = new SQLiteHelper(getContext());
        dbHelper.getWritableDatabase().execSQL("DELETE FROM " + dbHelper.TABLE_NAME + ";"); // Clean Table
        return true;
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