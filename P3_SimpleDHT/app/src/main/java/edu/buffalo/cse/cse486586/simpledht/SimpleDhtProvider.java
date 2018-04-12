package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.util.Map;


public class SimpleDhtProvider extends ContentProvider {

    private SQLiteHelper dbHelper;

    // MAIN API, with logic control in these functions

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        GV.dbIsBusy = true;
        Chord chord = Chord.getInstance();

        String key = values.getAsString("key");
        String value = values.getAsString("value");

        String targetPort = Chord.getInstance().lookup(key);

        if(targetPort.equals(chord.getPort())) {
            // insert one on local
            this.insertOne(values);

        } else {
            // tell the specific node to insert
            GV.msgSendQueue.offer(new Message(Message.TYPE.INSERT_ONE,
                    chord.getPort(), targetPort, key, value));
        }

        GV.dbIsBusy = false;
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int affectedRows = 0;
        GV.dbIsBusy = true;
        Chord chord = Chord.getInstance();
        String cmdPort = (selectionArgs==null) ? null : selectionArgs[0];

        if (selection.equals("@")) {
            // Delete all on local
            affectedRows = this.deleteAll();
        }

        if (selection.equals("*")) {
            affectedRows = this.deleteAll();

            if (cmdPort==null) {
                // local command

                if (chord.getSuccPort() != null) {
                    // Not single node
                    // Delete all on local and tell to succ node
                    GV.msgSendQueue.offer(new Message(Message.TYPE.DELETE_ALL,
                            chord.getPort(), chord.getSuccPort(), "*", null));
                }

            } else {
                // other node command
                if (cmdPort.equals(chord.getSuccPort())) {
                    // DONE: this node is the pred node of the cmd node
                    Log.d("DELETE ALL", "Command port: " + cmdPort + "\n" +
                            "This port: " + GV.MY_PORT + "\n" +
                            "Succ port: " + chord.getSuccPort());
                } else {
                    // GO ON, Send to next node
                    GV.msgSendQueue.offer(new Message(Message.TYPE.DELETE_ALL,
                            cmdPort, chord.getSuccPort(), "*", null));
                    Log.d("DELETE ALL", "IN LOOP.");
                }
            }



            GV.dbIsBusy = false;
            return affectedRows;
        }

        // not "@" or "*"
        String targetPort = chord.lookup(selection);

        if(targetPort.equals(chord.getPort())) {
            // delete one on local
            affectedRows = this.deleteOne(selection);
        } else {
            // tell the specific node to delete
            GV.msgSendQueue.offer(new Message(Message.TYPE.DELETE_ONE,
                    chord.getPort(), targetPort, selection, null));
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
        String cmdPort = (selectionArgs==null) ? null : selectionArgs[0];

        if (selection.equals("@")) {
            // query all on local
            c = this.queryAll();
            GV.dbIsBusy = false;
            return c;
        }

        if (selection.equals("*")) {
            c = this.queryAll();

            if (cmdPort == null) {
                // local command
                if ((chord.getSuccPort() != null)) {
                    GV.resultAllMap.clear();
                    GV.resultAllMap = Utils.cursorToHashMap(c);

                    // 1. tell succ nodes
                    GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_ALL,
                            chord.getPort(), chord.getSuccPort(), "*", null));

                    // 2. wait for all nodes result
                    GV.dbIsWaiting = true;
                    while (GV.dbIsWaiting); // Just blocked anb wait

                    // 3. combine all the result
                    c = Utils.makeCursor(GV.resultAllMap);
                    GV.resultAllMap.clear();
                }

            } else {
                // other node command
                for (Map.Entry entry: Utils.cursorToHashMap(c).entrySet()) {
                    GV.msgSendQueue.offer(new Message(Message.TYPE.RESULT_ALL,
                            cmdPort, cmdPort, entry.getKey().toString(), entry.getValue().toString()));
                }

                if (cmdPort.equals(chord.getSuccPort())) {
                    // DONE: this node is the pred node of the cmd node
                    GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_COMLETED,
                            cmdPort, cmdPort, "*", null));
                    Log.d("QUERY ALL", "Command port: " + cmdPort + "\n" +
                            "This port: " + GV.MY_PORT + "\n" +
                            "Succ port: " + chord.getSuccPort());
                } else {
                    // GO ON, Send to next node
                    GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_ALL,
                            cmdPort, chord.getSuccPort(), "*", null));
                    Log.d("QUERY ALL", "IN LOOP.");
                }

            }

            GV.dbIsBusy = false;
            return c;
        }

        // not equal to "@" or "*"
        String targetPort = chord.lookup(selection);

        if(targetPort.equals(chord.getPort())) {
            // locate at local
            c = this.queryOne(selection);

            if (cmdPort != null) {
                // other node commands
                c.moveToFirst();
                String resKey = c.getString(c.getColumnIndex("key"));
                String resValue = c.getString(c.getColumnIndex("value"));
                GV.msgSendQueue.offer(new Message(Message.TYPE.RESULT_ONE,
                        cmdPort, cmdPort, resKey, resValue));
            }

        } else {
            // not locate at local
            if (cmdPort != null) {
                // other node command
                // Not in local, just tell target to query, no need to wait
                GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_ONE,
                        cmdPort, targetPort, selection, null));

            } else {
                // local command
                GV.resultOneMap.clear();
                // tell specific node
                GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_ONE,
                        chord.getPort(), targetPort, selection, null));
                // wait for one node result
                while (GV.resultOneMap.isEmpty()); // Just blocked anb wait
                c = Utils.makeCursor(GV.resultOneMap);
                GV.resultOneMap.clear();
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
