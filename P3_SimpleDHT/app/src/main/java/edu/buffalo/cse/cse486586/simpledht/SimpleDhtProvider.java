package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/*
 * Reference
 *  Dev Docs:
 *      - MatrixCursor: https://developer.android.com/reference/android/database/MatrixCursor.html
 */

public class SimpleDhtProvider extends ContentProvider {

    private static boolean isBusy = false;

    private SQLiteHelper dbHelper;

    // MAIN API, with logic control in these functions

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        this.setIsBusy(true);

        String key = values.getAsString("key");
        String val = values.getAsString("value");

        String kid = Crypto.genHash(key);
        String port = Chord.getInstance().lookup(kid);

        if(port.equals(GV.MY_PORT)) {
            this.insertOne(values); // local

        } else {
            // TODO

            // insert to other node

        }

        this.setIsBusy(false);
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        this.setIsBusy(true);
        int affectedRows = 0;

        if (selection.equals("@")) {
            affectedRows = this.deleteAll();

        } else if (selection.equals("*")) {
            affectedRows = this.deleteAll();
            // TODO
            // Tell succ node

        } else {
            String kid = Crypto.genHash(selection);
            String port = Chord.getInstance().lookup(kid);

            if(port.equals(GV.MY_PORT)) {
                affectedRows = this.deleteOne(selection);
            } else {
                // TODO
                // Tell the node to delete

            }
        }

        this.setIsBusy(false);
        return affectedRows;
    }

    // Special string: * / @ as a selection parameter which return all the result for entire DHT or local
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        this.setIsBusy(true);
        Cursor c = null;

        if (selection.equals("@")) {
            return this.queryAll(); // local

        } else if (selection.equals("*")) {
            c = this.queryAll();
            // TODO
            // 1. tell succ nodes
            // 2. wait for all nodes result
            // 3. combine all the result

        } else {
            String kid = Crypto.genHash(selection);
            String port = Chord.getInstance().lookup(kid);

            if(port.equals(GV.MY_PORT)) {
                c = this.queryOne(selection);
            } else {
                // TODO
                // wait for one node result
            }
        }

        this.setIsBusy(false);
        return c;
    }

    private void insertNetwork() {}

    private void deleteNetwork() {}

    private void retrieveNetwork() {}


    public Cursor makeCursor(HashMap<String, String> kvMap) {
        String[] attributes = {"_id", "key", "value"};
        MatrixCursor mCursor = new MatrixCursor(attributes);
        for (Map.Entry entry: kvMap.entrySet()) {
            mCursor.addRow(new Object[] {
                    R.drawable.ic_launcher, entry.getKey(), entry.getValue()});
        }
        return mCursor;
    }

    public Cursor addRowsToCursor() {
        return null;
    }

    private void setIsBusy(boolean bool) {this.isBusy = bool;}



    // Basic insert, query and delete operation on database
    private void insertOne(ContentValues values) {
        long newRowId = this.dbHelper.getWritableDatabase().insertWithOnConflict(
                this.dbHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        Log.v("INSERT", "ROW=" + newRowId + "; KV=\'" +
                values.getAsString("key") + ", " +
                values.getAsString("value") + "\'");
    }

    private Cursor queryOne(String key) {
        String[] columns = new String[] {this.dbHelper.COLUMN_NAME_KEY, this.dbHelper.COLUMN_NAME_VALUE};
        String selection = this.dbHelper.COLUMN_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        Cursor c = this.dbHelper.getReadableDatabase().query(
                this.dbHelper.TABLE_NAME, columns, selection, selectedKey,
                null, null, null, "1");
        c.moveToFirst();

        Log.v("QUERY", "KV=\'" + key + ", " + c.getString(c.getColumnIndex("value")) + "\'");
        return c;
    }

    private Cursor queryAll() {
        String[] columns = new String[] {this.dbHelper.COLUMN_NAME_KEY, this.dbHelper.COLUMN_NAME_VALUE};

        Cursor c = this.dbHelper.getReadableDatabase().query(
                this.dbHelper.TABLE_NAME, columns, null, null,
                null, null, null);
        c.moveToFirst();

        Log.v("QUERY ALL", c.getCount() + " ROWS.");
        return c;
    }

    private int deleteOne(String key) {
        String selection = this.dbHelper.COLUMN_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                this.dbHelper.TABLE_NAME, selection, selectedKey);

        Log.v("DELETE", "KEY=\'" + key + "\'");
        return affectedRows;
    }

    private int deleteAll() {
        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                this.dbHelper.TABLE_NAME, null, null);
        Log.v("DELETE ALL", affectedRows + " ROWS.");
        this.countRows();
        return affectedRows;
    }

    private void countRows() {
        Cursor c = this.dbHelper.getReadableDatabase().query(
                this.dbHelper.TABLE_NAME, null, null, null,
                null, null, null);
        Log.v("COUNT", c.getCount() + "");
        c.close();
    }

    // Other basic functions
    @Override
    public boolean onCreate() {
        this.dbHelper = SQLiteHelper.getInstance(getContext());
        this.dbHelper.getWritableDatabase().execSQL("DELETE FROM " + dbHelper.TABLE_NAME + ";"); // Clean Table
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
