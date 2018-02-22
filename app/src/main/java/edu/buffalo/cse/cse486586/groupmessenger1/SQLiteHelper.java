package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Zishan Liang on 2/20/18.
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
            "CREATE TABLE " + TABLE_NAME + " ( " + COLUMN_NAME_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_NAME_KEY + " STRING, " + COLUMN_NAME_VALUE + " STRING" + " );";

    private static final String SQL_ALTER_ENTRIES = "ALTER TABLE " + TABLE_NAME +
            " ADD COLUMN " + COLUMN_NAME_VALUE + " STRING;";

    // private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + TABLE_NAME;

    public SQLiteHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < DB_VERSION) {
            db.execSQL(SQL_ALTER_ENTRIES);
        }
        // A cache database which its upgrade policy is to simply to discard the data and start over
        // db.execSQL(SQL_DELETE_ENTRIES);
        // onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public String getNullColumnHack() {
        return COLUMN_NAME_ID;
    }

}

