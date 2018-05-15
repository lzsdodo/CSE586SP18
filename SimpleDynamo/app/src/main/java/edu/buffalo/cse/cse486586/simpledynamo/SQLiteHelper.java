package edu.buffalo.cse.cse486586.simpledynamo;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * Reference:
 * - Android Dev Docs:
 *      - Cpmtext: https://developer.android.com/reference/android/content/Context.html
 *      - android.content: https://developer.android.com/reference/android/content/package-summary.html
 *      - Save Data using SQLite: https://developer.android.com/training/data-storage/sqlite.html#DbHelper
 *      - SQLiteOpenHelper: https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
 *      - SQLiteDatabase: https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html
 */

public class SQLiteHelper extends SQLiteOpenHelper {

    // database
    public static final String DB_NAME     = "SimpleDynamo";
    public static final int    DB_VERSION  = 2;

    // table
    public static final String LOCAL_TABLE       = "local_msg";
    public static final String TIMEOUT_TABLE     = "timeout_msg";

    public static final String COL_NAME_ID       = "_ID";
    public static final String COL_NAME_KEY      = "key";
    public static final String COL_NAME_VALUE    = "value";
    public static final String COL_NAME_PORT     = "port";
    //public static final String COL_NAME_VERSION  = "version";

    // sql
    private static final String SQL_CREATE_LOCAL_TABLE = "CREATE TABLE " +
            LOCAL_TABLE + " ( " +
            COL_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_NAME_KEY + " STRING NOT NULL UNIQUE, " +
            COL_NAME_VALUE + " STRING );";
            //COL_NAME_VERSION + " STRING );";

    private static final String SQL_CREATE_TIMEOUT_TABLE = "CREATE TABLE " +
            TIMEOUT_TABLE + " ( " +
            COL_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_NAME_KEY + " STRING NOT NULL UNIQUE, " +
            COL_NAME_VALUE + " STRING, " +
            COL_NAME_PORT + " STRING );";

    private static final String SQL_ALTER_LOCAL_TABLE = "ALTER TABLE " +
            LOCAL_TABLE + " ADD COLUMN " + COL_NAME_VALUE + " STRING;";

    private static final String SQL_ALTER_TIMEOUT_TABLE = "ALTER TABLE " +
            TIMEOUT_TABLE + " ADD COLUMN " + COL_NAME_VALUE + " STRING;";

    private static SQLiteHelper sqlHelper;      // main instance

    private SQLiteHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static SQLiteHelper getInstance(Context context) {
        if (sqlHelper == null)
            sqlHelper = new SQLiteHelper(context);
        return sqlHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_LOCAL_TABLE);
        db.execSQL(SQL_CREATE_TIMEOUT_TABLE);
        Log.v("DATABASE", SQL_CREATE_LOCAL_TABLE);
        Log.v("DATABASE", SQL_CREATE_TIMEOUT_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < DB_VERSION) {
            db.execSQL(SQL_ALTER_LOCAL_TABLE);
            db.execSQL(SQL_ALTER_TIMEOUT_TABLE);
        }
        // A cache database which its upgrade policy is to simply to discard the data and start over
        // db.execSQL(SQL_DROP_TABLE);
        // onCreate(db);
    }

}