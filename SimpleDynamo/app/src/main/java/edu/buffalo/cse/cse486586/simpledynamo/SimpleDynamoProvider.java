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

public class SimpleDynamoProvider extends ContentProvider {

    static String TAG = "DB";

    private SQLiteHelper dbHelper;


    public Cursor dbQuery(String key, String cmdPort) {
        Cursor c = null;
        String kid = this.genHash(key);
        Dynamo dynamo = Dynamo.getInstance();

        /* Local Command */
        // Just send msg to tgt and do nothing
        // And we should lock the local query to wait the msg back
        if (cmdPort == null) {
            // We should lock here
            Log.e("LOCK", "WAITING FOR RESULT");

            if (key.equals("@")) {
                c = this.queryAll();
                c.moveToFirst();
            }

            if (key.equals("*")) {
                c = this.queryAll();
                c.moveToFirst();

                GV.resultAllMap.clear();
                GV.resultAllMap = this.cursorToHashMap(c);

                // 1. tell succ nodes
                GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                        dynamo.getPort(), dynamo.getSuccPort(), "*", "???"));

                // 2. wait for all nodes result
                boolean condition = false;
                while (condition) {}

                // 3. combine all the result
                c = this.makeCursor(GV.resultAllMap);
                GV.resultAllMap.clear();

            }
            // not equal to "@" or "*"
            else {
                String tgtPort = dynamo.getQueryTgtPort(kid);

                if (tgtPort.equals(dynamo.getPort())) {
                    // 本地就是 perferlist 最后一个
                    c = this.queryOne(key);

                    if (c.getCount() == 0) {
                        GV.resultOneMap.clear();
                        // Tell pred node to search for me
                        GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                                dynamo.getPort(), dynamo.getPredPort(), key, "???"));

                        // wait for result
                        boolean condition = false;
                        while (condition) {}

                        // return result
                        c = this.makeCursor(GV.resultOneMap);
                        GV.resultOneMap.clear();
                    }

                } else {
                    // 节点非 perferlist 最后
                    GV.resultOneMap.clear();
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                            dynamo.getPort(), tgtPort, key, "???"));

                    // wait for result
                    boolean condition = false;
                    while (condition) {}

                    // return result
                    c = this.makeCursor(GV.resultOneMap);
                    GV.resultOneMap.clear();
                }
            }
            c.moveToFirst();
            return c;
        }
        /* External Command */
        // Do real query
        else {
            if (key.equals("*")) {
                for (Entry entry : this.cursorToHashMap(c).entrySet()) {
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.RESULT_ALL,
                            cmdPort, cmdPort, entry.getKey().toString(), entry.getValue().toString()));
                }
                // 判断是否最后节点
                if (cmdPort.equals(dynamo.getSuccPort())) {
                    // DONE: this node is the pred node of the cmd node
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.RESULT_ALL_COMLETED,
                            cmdPort, cmdPort, "*", "???"));
                    Log.d("QUERY ALL", "DONE");

                } else {
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                            cmdPort, dynamo.getSuccPort(),
                            "*", "???"));
                    Log.d("QUERY ALL", "CONTINUE");
                }

            }
            // not "*"
            else {
                // search local
                c = this.queryOne(key);

                if (c.getCount() == 0) {
                    if (dynamo.isLastNodeToQuery(kid)) {
                        Log.e(TAG, "DID NOT INSERT TO THE FIRST NODE YET");
                        GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                                cmdPort, dynamo.getPort(), key, "???"));

                    } else {
                        // Tell pred node to search for cmd node
                        GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                                cmdPort, dynamo.getPredPort(), key, "???"));
                    }

                } else {
                    c.moveToFirst();
                    String resKey = c.getString(c.getColumnIndex("key"));
                    String resVal = c.getString(c.getColumnIndex("value"));
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.RESULT_ONE,
                            cmdPort, cmdPort, resKey, resVal));
                    Log.d(TAG, "FOUND THE RESULT");
                }
            }
            return c;
        }
    }

    public void dbInsert(ContentValues cv, String cmdPort) {
        this.countRows();
        Dynamo dynamo = Dynamo.getInstance();
        String key = cv.getAsString("key");
        String val = cv.getAsString("value");
        String kid = this.genHash(key);
        String tgtPort = dynamo.getWriteTgtPort(kid);

        /* Local Command */
        // Just send msg to tgt and do nothing
        if (cmdPort == null) {
            Log.d("INSERT", "LOCAL INSERT CMD - " + key);

            if (tgtPort.equals(dynamo.getPort())) {
                Log.d("INSERT", "Starts from self: " + tgtPort);
                GV.msgRecvQueue.offer(new NMessage(NMessage.TYPE.INSERT,
                        dynamo.getPort(), dynamo.getPort(), key, val));

            } else {
                Log.d("INSERT", "Starts from other: " + tgtPort);
                GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.INSERT,
                        dynamo.getPort(), tgtPort, key, val));
            }
            // TODO: Maybe Can not send to fail node

        }
        /* External Command */
        else {
            // Do real insert
            this.insertOne(cv);
            if (!dynamo.isLastNodeToWrite(key)) {
                // NOT FINAL NODE & GO ON INSERT
                GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.INSERT,
                        cmdPort, dynamo.getSuccPort(), key, val));
            }
        }

        this.countRows();
    }

    public int dbDelete(String key, String cmdPort) {
        int affectedRows = 0;
        Dynamo dynamo = Dynamo.getInstance();
        String kid = this.genHash(key);
        String tgtPort = dynamo.getWriteTgtPort(kid);
        Log.d("DELETE", key + "<>");

        /* Local Command */
        // Just send msg to tgt and do nothing
        if (cmdPort == null) {
            // "@"
            if (key.equals("@")) {
                affectedRows = this.deleteAll();
            }

            // "*"
            if (key.equals("*")) {
                GV.msgRecvQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                        dynamo.getPort(), dynamo.getPort(), key, "---"));

            }
            // not "@" or "*"
            else {
                if (tgtPort.equals(dynamo.getPort())) {
                    Log.d("DELETE", "Starts from self: " + tgtPort);
                    GV.msgRecvQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                            dynamo.getPort(), dynamo.getPort(), key, "---"));

                } else {
                    Log.d("DELETE", "Starts from other: " + tgtPort);
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                            dynamo.getPort(), tgtPort, key, "---"));
                }
            }

        }
        /* External Command */
        else {
            // Do real delete
            // "*"
            if (key.equals("*")) {
                affectedRows = this.deleteAll();

                if (cmdPort.equals(dynamo.getSuccPort())) {
                    // DONE: last node command
                    Log.d("DELETE ALL", "DONE");
                } else {
                    // GO ON, Send to next node
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                            cmdPort, dynamo.getSuccPort(),"*", "---"));
                    Log.d("DELETE ALL", "CONTINUE");
                }

            }
            // not "@" or "*"
            else {
                affectedRows = this.deleteOne(key);

                if (!dynamo.isLastNodeToWrite(kid)) {
                    // GO ON DELETE
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                            cmdPort, dynamo.getSuccPort(), key, "---"));
                }

            }
        }

        return affectedRows;
    }



    // Basic insert, query and delete operation on database
    synchronized private void insertOne(ContentValues values) {
        long newRowId = this.dbHelper.getWritableDatabase().insertWithOnConflict(
                this.dbHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        Log.v("INSERT ONE", "ROW= " + newRowId + "; KV= " +
                values.getAsString("key") + "<>" + values.getAsString("value"));
    }

    synchronized private Cursor queryOne(String key) {
        String[] columns = new String[] {this.dbHelper.COL_NAME_KEY, this.dbHelper.COL_NAME_VALUE};
        String selection = this.dbHelper.COL_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        Cursor c = this.dbHelper.getReadableDatabase().query(
                this.dbHelper.TABLE_NAME, columns, selection, selectedKey,
                null, null, null, "1");
        c.moveToFirst();

        if (c.getCount() == 0) {
            Log.e("QUERY MISS", "SEARCH FROM PRED NODE");
        } else {
            Log.v("QUERY", "KV=\'" + key + ", " + c.getString(c.getColumnIndex("value")) + "\'");
        }
        return c;
    }

    synchronized private Cursor queryAll() {
        String[] columns = new String[] {this.dbHelper.COL_NAME_KEY, this.dbHelper.COL_NAME_VALUE};

        Cursor c = this.dbHelper.getReadableDatabase().query(
                this.dbHelper.TABLE_NAME, columns, null, null,
                null, null, null);
        c.moveToFirst();

        Log.v("QUERY ALL", c.getCount() + " ROWS.");
        return c;
    }

    synchronized private int deleteOne(String key) {
        String selection = this.dbHelper.COL_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                this.dbHelper.TABLE_NAME, selection, selectedKey);

        Log.v("DELETE ONE", "K= " + key);
        return affectedRows;
    }

    synchronized private int deleteAll() {
        //Log.e("ORIGINAL ROWS", this.countRows() + " ROWS.");
        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                this.dbHelper.TABLE_NAME, null, null);
        Log.v("DELETE ALL", affectedRows + " ROWS.");
        return affectedRows;
    }

    private int countRows() {
        Cursor c = this.dbHelper.getReadableDatabase().query(
                this.dbHelper.TABLE_NAME, null, null, null,
                null, null, null);
        int rows = c.getCount();
        Log.e("COUNT", rows + "");
        return rows;
    }

    private Cursor makeCursor(HashMap<String, String> kvMap) {
        String[] attributes = {"_id", "key", "value"};
        MatrixCursor mCursor = new MatrixCursor(attributes);
        for (Entry entry: kvMap.entrySet()) {
            mCursor.addRow(
                    new Object[] {R.drawable.ic_launcher, entry.getKey(), entry.getValue()}
            );
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

    private String genHash(String input) {
        Formatter formatter = new Formatter();
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] sha1Hash = sha1.digest(input.getBytes());
            for (byte b : sha1Hash) {
                formatter.format("%02x", b);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "GEN HASH ERR" );
            e.printStackTrace();
        }
        return formatter.toString();
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                                     String[] selectionArgs, String sortOrder) {
        if (selection.equals("#")) {
            GV.dbRows = this.countRows();
            return null;
        }
        String cmdPort = (selectionArgs == null) ? null : selectionArgs[0];
        return this.dbQuery(selection, cmdPort);
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String cmdPort = values.getAsString("cmdPort");
        values.remove("cmdPort");
        this.dbInsert(values, cmdPort);
        return uri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        String cmdPort = (selectionArgs == null) ? null : selectionArgs[0];
        return this.dbDelete(selection, cmdPort);
    }

    @Override
	public boolean onCreate() {
        this.dbHelper = SQLiteHelper.getInstance(getContext());
        if (GV.deleteTable) {
            // Clean Table
            this.dbHelper.getWritableDatabase().execSQL("DELETE FROM " + dbHelper.TABLE_NAME + ";");
        }
        return true;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) { return 0; }

	@Override
	public String getType(Uri uri) { return null; }


}
