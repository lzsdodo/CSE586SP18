package edu.buffalo.cse.cse486586.simpledynamo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;


/*
 * Reference
 * - Dev Docs:
 *      - ContentProvider: http://developer.android.com/guide/topics/providers/content-providers.html
 *      - ContentValues: https://developer.android.com/reference/android/content/ContentValues.html
 *      - Cursor: https://developer.android.com/reference/android/database/Cursor.html
 *      - SQLiteOpenHelper: https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
 *      - MatrixCursor: https://developer.android.com/reference/android/database/MatrixCursor.html
 */

public class SimpleDynamoProvider extends ContentProvider {

	private SQLiteHelper dbHelper;


	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO
		return null;
	}


	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO
		return 0;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO
		return null;
	}


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
        // this.countRows();
        return affectedRows;
    }

    private void countRows() {
        Cursor c = this.dbHelper.getReadableDatabase().query(
                this.dbHelper.TABLE_NAME, null, null, null,
                null, null, null);
        Log.v("COUNT", c.getCount() + "");
        c.close();
    }


    private Cursor makeCursor(HashMap<String, String> kvMap) {
        String[] attributes = {"_id", "key", "value"};
        MatrixCursor mCursor = new MatrixCursor(attributes);
        for (Entry entry: kvMap.entrySet()) {
            mCursor.addRow(new Object[] {
                    R.drawable.ic_launcher, entry.getKey(), entry.getValue()});
        }
        return mCursor;
    }

    private HashMap<String, String> cursorToHashMap(Cursor c) {
        HashMap<String, String> map = new HashMap<String, String>();

        c.moveToFirst();
        while (!c.isAfterLast()) {
            String k = c.getString(c.getColumnIndex("key"));
            String v = c.getString(c.getColumnIndex("value"));
            map.put(k, v);
            c.moveToNext();
        }
        c.close();

        return map;
    }


	@Override
	public boolean onCreate() {
		this.dbHelper = SQLiteHelper.getInstance(getContext());
		this.dbHelper.getWritableDatabase().execSQL("DELETE FROM " + dbHelper.TABLE_NAME + ";"); // Clean Table
		return true;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}

	private String genHash(String input) throws NoSuchAlgorithmException {
		MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
		byte[] sha1Hash = sha1.digest(input.getBytes());
		Formatter formatter = new Formatter();
		for (byte b : sha1Hash) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}

}
