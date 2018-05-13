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
            Log.d("L-QUERY", "1. " + key + "::" + kid);

            if (key.equals("@")) {
                Log.d("L-QUERY", "2. QUERY @");
                c = this.queryAll();
                c.moveToFirst();
                return c;
            }

            // We should lock here
            Log.e("LOCK", "WAITING FOR RESULT");
            GV.queryLock.lock();
            GV.needWaiting = true;
            try {
                if (key.equals("*")) {
                    Log.d("L-QUERY", "2. QUERY *");
                    c = this.queryAll();
                    c.moveToFirst();

                    GV.resultAllMap.clear();
                    GV.resultAllMap = this.cursorToHashMap(c);

                    // 1. tell succ nodes
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                            GV.MY_PORT, dynamo.getSuccPort(), "*", "???"));
                    Log.d("L-QUERY", "3. SEND * TO SUCC " + dynamo.getSuccPort());

                    // 2. wait for all nodes result
                    while (GV.needWaiting) {}

                    // 3. combine all the result
                    c = this.makeCursor(GV.resultAllMap);
                    GV.resultAllMap.clear();
                    Log.d("L-QUERY", "4. RECV * FROM ALL");

                }
                // not equal to "@" or "*"
                else {
                    String tgtPort = dynamo.getTgtPort(kid, "QUERY");
                    Log.d("L-QUERY", "2. QUERY " + key);

                    if (tgtPort.equals(GV.MY_PORT)) {
                        // 本地就是 perferlist 最后一个
                        c = this.queryOne(key);

                        if (c.getCount() == 0) {
                            GV.resultOneMap.clear();
                            // Tell pred node to search for me
                            GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                                    GV.MY_PORT, GV.MY_PORT, key, "???"));
                            Log.d("L-QUERY", "3. SEND TO PRED " + key);

                            // wait for result
                            while (GV.needWaiting) {}

                            // return result
                            c = this.makeCursor(GV.resultOneMap);
                            GV.resultOneMap.clear();
                        }

                    } else {
                        // 节点非 perferlist 最后
                        GV.resultOneMap.clear();
                        GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                                GV.MY_PORT, tgtPort, key, "???"));
                        Log.d("L-QUERY", "3. SEND TO TGT " + tgtPort + " ~" + key);

                        // wait for result
                        while (GV.needWaiting) {}

                        // return result
                        c = this.makeCursor(GV.resultOneMap);
                        GV.resultOneMap.clear();
                    }
                    Log.d("L-QUERY", "4. RECV RESULT FOR KEY " + key);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                GV.needWaiting = false;
                GV.queryLock.unlock();
            }
            c.moveToFirst();
            return c;
        }
        /* External Command */
        // Do real query
        else {
            Log.d("E-QUERY", "1. " + key + "::" + kid);

            if (key.equals("*")) {
                c = this.queryAll();
                c.moveToFirst();

                for (Entry entry: this.cursorToHashMap(c).entrySet()) {
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.RESULT_ALL,
                            cmdPort, cmdPort, entry.getKey().toString(), entry.getValue().toString()));
                }
                // 判断是否最后节点
                if (cmdPort.equals(dynamo.getSuccPort())) {
                    // DONE: this node is the pred node of the cmd node
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.RESULT_ALL_COMLETED,
                            cmdPort, cmdPort, "*", "???"));
                    Log.d("E-QUERY", "2. QUERY * DONE");

                } else {
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                            cmdPort, dynamo.getSuccPort(),"*", "???"));
                    Log.d("E-QUERY", "2. QUERY * AND SEND TO SUCC " + dynamo.getSuccPort());
                }

            }
            // not "*"
            else {
                // search local
                c = this.queryOne(key);

                if (c.getCount() == 0) {
                    Log.e("E-QUERY", "2. DID NOT INSERT YET FOR KEY " + key);

                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.QUERY,
                            cmdPort, GV.MY_PORT, key, "???"));
                    Log.d("E-QUERY", "3. SEND TO SELF " + dynamo.getPort() + " ~" + key);

                } else {
                    Log.e("E-QUERY", "2. FOUND RESULT FOR KEY " + key);
                    c.moveToFirst();
                    String resKey = c.getString(c.getColumnIndex("key"));
                    String resVal = c.getString(c.getColumnIndex("value"));
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.RESULT_ONE,
                            cmdPort, cmdPort, resKey, resVal));
                    Log.d("E-QUERY", "3. SEND BACK RESULT TO " + cmdPort + " ~" + key);
                }
            }
            return c;
        }
    }

    public void dbInsert(ContentValues cv, String cmdPort) {
        Dynamo dynamo = Dynamo.getInstance();
        String key = cv.getAsString("key");
        String val = cv.getAsString("value");
        String kid = this.genHash(key);
        String tgtPort = dynamo.getTgtPort(kid, "INSERT");

        /* Local Command */
        // Just send msg to tgt and do nothing
        if (cmdPort == null) {
            Log.d("L-INSERT", "1. " + key + "::" + kid);

            if (tgtPort.equals(GV.MY_PORT)) {
                this.insertOne(cv);
                GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.INSERT,
                        GV.MY_PORT, dynamo.getSuccPort(), key, val));
                Log.d("L-INSERT", "2. SEND TO SUCC " + dynamo.getSuccPort() + " ~" + key);

            } else {
                Log.d("L-INSERT", "2. SEND TO TGT " + tgtPort + " ~" + key);
                GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.INSERT,
                        GV.MY_PORT, tgtPort, key, val));
            }
            // TODO: Maybe Can not send to fail node

        }
        /* External Command */
        else {
            Log.d("E-INSERT", "1. " + key + "::" + kid);
            this.insertOne(cv);
            if (!dynamo.isLastNode(kid, "INSERT")) {
                // NOT FINAL NODE & SEND TO SUCC
                GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.INSERT,
                        cmdPort, dynamo.getSuccPort(), key, val));
                Log.d("E-INSERT", "2. INSERT AND SEND TO SUCC " + dynamo.getSuccPort() + " ~" + key);
            }
        }
    }

    public int dbDelete(String key, String cmdPort) {
        int affectedRows = 0;
        Dynamo dynamo = Dynamo.getInstance();
        String kid = this.genHash(key);
        String tgtPort = dynamo.getTgtPort(kid, "DELETE");

        /* Local Command */
        // Just send msg to tgt and do nothing
        if (cmdPort == null) {
            Log.d("L-DELETE", "1. " + key + "::" + kid);

            // "@"
            if (key.equals("@")) {
                affectedRows = this.deleteAll();
                Log.d("L-DELETE", "2. " + "EXECUTE @");
            }

            // "*"
            if (key.equals("*")) {
                affectedRows = this.deleteAll();
                GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                        GV.MY_PORT, dynamo.getSuccPort(), key, "---"));
                Log.d("L-DELETE", "2. SEND * TO SUCC " + dynamo.getSuccPort());

            }
            // not "@" or "*"
            else {
                if (tgtPort.equals(GV.MY_PORT)) {
                    affectedRows = this.deleteOne(key);
                    GV.msgRecvQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                            GV.MY_PORT, dynamo.getSuccPort(), key, "---"));
                    Log.d("L-DELETE", "2. SEND TO SUCC " + dynamo.getPort() + " ~" + key);

                } else {
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                            GV.MY_PORT, tgtPort, key, "---"));
                    Log.d("L-DELETE", "2. SEND TO TGT " + tgtPort + " ~" + key);
                }
            }
            return affectedRows;
        }
        /* External Command */
        else {
            Log.d("E-DELETE", "1. " + key + "::" + kid);
            // Do real delete
            // "*"
            if (key.equals("*")) {
                affectedRows = this.deleteAll();
                Log.d("E-DELETE", "2. DELETE *");

                if (cmdPort.equals(dynamo.getSuccPort())) {
                    // DONE: last node command
                    Log.d("E-DELETE", "3. DONE *");
                } else {
                    // GO ON, Send to next node
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                            cmdPort, dynamo.getSuccPort(),"*", "---"));
                    Log.d("E-DELETE", "3. SEND * TO SUCC " + dynamo.getSuccPort());
                }

            }
            // not "@" or "*"
            else {
                affectedRows = this.deleteOne(key);
                Log.d("E-DELETE", "2. DELETE " + key);

                if (!dynamo.isLastNode(kid, "DELETE")) {
                    // SEND TO SUCC
                    GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.DELETE,
                            cmdPort, dynamo.getSuccPort(), key, "---"));
                    Log.d("E-DELETE", "3. SEND TO SUCC " + dynamo.getSuccPort() + " ~" + key);
                }
            }
        }

        return affectedRows;
    }



    // Basic insert, query and delete operation on database
    private Cursor queryOne(String key) {
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
            Log.v("QUERY", "Key<>Val=" + key + "<>" + c.getString(c.getColumnIndex("value")));
        }
        return c;
    }

    private Cursor queryAll() {
        String[] columns = new String[] {this.dbHelper.COL_NAME_KEY, this.dbHelper.COL_NAME_VALUE};

        Cursor c = this.dbHelper.getReadableDatabase().query(
                this.dbHelper.TABLE_NAME, columns, null, null,
                null, null, null);
        c.moveToFirst();

        Log.v("QUERY ALL", c.getCount() + " ROWS.");
        return c;
    }

    private void insertOne(ContentValues values) {
        long newRowId = this.dbHelper.getWritableDatabase().insertWithOnConflict(
                this.dbHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        Log.v("INSERT ONE", "ROW=" + newRowId + "; Key<>Val=" +
                values.getAsString("key") + "<>" + values.getAsString("value"));
    }

    private int deleteOne(String key) {
        String selection = this.dbHelper.COL_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                this.dbHelper.TABLE_NAME, selection, selectedKey);

        Log.v("DELETE ONE", "Key=" + key);
        return affectedRows;
    }

    private int deleteAll() {
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
        Cursor c = null;
        if (selection.equals("#")) {
            GV.dbRows = this.countRows();
            return null;
        }
        String cmdPort = (selectionArgs == null) ? null : selectionArgs[0];
        c = this.dbQuery(selection, cmdPort);
        return c;
    }

    @Override
    synchronized public Uri insert(Uri uri, ContentValues values) {
        String cmdPort = values.getAsString("cmdPort");
        values.remove("cmdPort");
        this.dbInsert(values, cmdPort);
        return uri;
    }

    @Override
    synchronized public int delete(Uri uri, String selection, String[] selectionArgs) {
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
