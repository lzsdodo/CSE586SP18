package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/*
 * Reference:
 * - Android Dev Docs:
 *      Cpmtext: https://developer.android.com/reference/android/content/Context.html
 *      android.content: https://developer.android.com/reference/android/content/package-summary.html
 *      Save Data using SQLite: https://developer.android.com/training/data-storage/sqlite.html#DbHelper
 *      SQLiteOpenHelper: https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
 *      SQLiteDatabase: https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html
 */

public class SQLiteHelper extends SQLiteOpenHelper {

    // database values
    public static final String DB_NAME     = "GroupMessenger";
    public static final int    DB_VERSION  = 2;

    // table values
    public static final String TABLE_NAME        = "entry";
    public static final String COLUMN_NAME_ID   = "_ID";
    public static final String COLUMN_NAME_KEY   = "key";
    public static final String COLUMN_NAME_VALUE = "value";

    // SQL statement
    // Table's name and the columns in the table
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " ( " +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_NAME_KEY + " STRING NOT NULL UNIQUE, " +
                    COLUMN_NAME_VALUE + " STRING" + " );";

    private static final String SQL_ALTER_ENTRIES = "ALTER TABLE " + TABLE_NAME +
            " ADD COLUMN " + COLUMN_NAME_VALUE + " STRING;";

    // private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";

    public SQLiteHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < DB_VERSION) {db.execSQL(SQL_ALTER_ENTRIES);}
        // A cache database which its upgrade policy is to simply to discard the data and start over
        // db.execSQL(SQL_DROP_TABLE);
        // onCreate(db);
    }

    public String getNullColumnHack() {
        return COLUMN_NAME_ID;
    }

    /*
    public Void readAllFromTable() {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = new String[] {COLUMN_NAME_ID, COLUMN_NAME_KEY, COLUMN_NAME_VALUE};
        Cursor c = db.query(TABLE_NAME, columns, null, null,
                null, null, null, null);
        c.moveToFirst();
        while (!c.isLast()) {
            Log.v("SQLiteHelper", c.getString(0) + "\t" +
                    c.getString(1) + "\t" + c.getString(2));
            c.moveToNext();
        }
        return null;
    }
    */
}

