package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;



public class SimpleDhtProvider extends ContentProvider {

    private SQLiteHelper dbHelper;

    // MAIN API, with logic control in these functions

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        GV.dbIsBusy = true;
        Chord chord = Chord.getInstance();

        String key = values.getAsString("key");
        String value = values.getAsString("value");

        String kid = Utils.genHash(key);
        String targetPort = Chord.getInstance().lookup(kid);

        if(targetPort.equals(chord.getNPort())) {
            // insert one on local
            this.insertOne(values);

        } else {
            // tell the specific node to insert
            GV.msgSendQueue.offer(new Message(Message.TYPE.INSERT_ONE,
                    chord.getNPort(), targetPort, key, value));
        }

        GV.dbIsBusy = false;
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int affectedRows = 0;
        GV.dbIsBusy = true;
        Chord chord = Chord.getInstance();

        if (selection.equals("@")) {
            // Delete all on local
            affectedRows = this.deleteAll();

        } else if (selection.equals("*")) {
            affectedRows = this.deleteAll();
            // Delete all on local and tell to succ node
            if (chord.getSuccPort() != null) {
                GV.msgSendQueue.offer(new Message(Message.TYPE.DELETE_ALL,
                        chord.getNPort(), chord.getSuccPort(), "*", null));
            }

        } else {
            String kid = Utils.genHash(selection);
            String targetPort = chord.lookup(kid);

            if(targetPort.equals(chord.getNPort())) {
                // delete one on local
                affectedRows = this.deleteOne(selection);
            } else {
                // tell the specific node to delete
                GV.msgSendQueue.offer(new Message(Message.TYPE.DELETE_ONE,
                        chord.getNPort(), targetPort, selection, null));
            }
        }

        GV.dbIsBusy = false;
        return affectedRows;
    }

    // Special string: * / @ as a selection parameter which return all the result for entire DHT or local
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor c = null;
        GV.dbIsBusy = true;
        Chord chord = Chord.getInstance();

        if (selection.equals("@")) {
            // query all on local
            c = this.queryAll();

        } else if (selection.equals("*")) {
            c = this.queryAll();

            if(chord.getSuccPort() != null) {
                GV.resultAllMap.clear();
                GV.resultAllMap = Utils.cursorToHashMap(c);

                // 1. tell succ nodes
                GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_ALL,
                        chord.getNPort(), chord.getSuccPort(), "*", null));

                // 2. wait for all nodes result
                GV.dbIsWaiting = true;
                while (GV.dbIsWaiting); // Just blocked anb wait

                // 3. combine all the result
                c = Utils.makeCursor(GV.resultAllMap);
                GV.resultAllMap.clear();
            }

        } else {
            String kid = Utils.genHash(selection);
            String targetPort = chord.lookup(kid);

            if(targetPort.equals(chord.getNPort())) {
                c = this.queryOne(selection);

            } else {
                if (GV.dbIsOtherQuery) {
                    GV.dbIsOtherQuery = false;
                    c = null;

                } else {
                    GV.resultOneMap.clear();
                    // tell specific node
                    GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_ONE,
                            chord.getNPort(), targetPort, selection, null));
                    // wait for one node result
                    while (GV.resultOneMap.isEmpty()); // Just blocked anb wait
                    c = Utils.makeCursor(GV.resultOneMap);
                    GV.resultOneMap.clear();
                }
            }
        }

        GV.dbIsBusy = false;
        return c;
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
