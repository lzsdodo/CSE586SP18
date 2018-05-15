package edu.buffalo.cse.cse486586.simpledynamo;

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


    // API Layer
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        String cmdPort = null;
        String timeoutPort = null;
        if (selectionArgs!=null) {
            switch (selectionArgs.length) {
                case 1:
                    cmdPort = selectionArgs[0];
                    break;
                case 2:
                    cmdPort = selectionArgs[0];
                    timeoutPort = selectionArgs[1];
                    break;
                default:
                    Log.e(TAG, "query: selArgs ERROR");
                    break;
            }
        }

        Cursor c = this.dbQuery(selection, cmdPort, timeoutPort);
        return c;
    }

    @Override
    synchronized public Uri insert(Uri uri, ContentValues values) {
        String cmdPort = values.getAsString("cmdPort");
        values.remove("cmdPort");

        boolean allowSend = true;
        String flag = values.getAsString("sendFlag");
        if (flag != null) {
            values.remove("sendFlag");
            allowSend = false;
        }

        String timeoutPort = values.getAsString("timeoutPort");
        if (timeoutPort!=null) {
            values.remove(timeoutPort);
        }

        this.dbInsert(values, cmdPort, allowSend, timeoutPort);
        return uri;
    }

    @Override
    synchronized public int delete(Uri uri, String selection, String[] selectionArgs) {
        assert selectionArgs!=null;

        String cmdPort = null;
        String timeoutPort = null;
        boolean allowSend = true;

        switch (selectionArgs.length) {
            case 0: break;
            case 1:
                cmdPort = selectionArgs[0];
                break;
            case 2:
                cmdPort = selectionArgs[0];
                allowSend = false;
                break;
            case 3:
                cmdPort = selectionArgs[0];
                allowSend = false;
                timeoutPort = selectionArgs[2];
                break;
            default: break;
        }

        return this.dbDelete(selection, cmdPort, allowSend, timeoutPort);
    }


    // Logic layer
    public Cursor dbQuery(String key, String cmdPort, String timoutPort) {
        Cursor c = null;
        String kid = Dynamo.genHash(key);
        String lastPort = Dynamo.getLastPort(kid);
        String firstPort = Dynamo.getFirstPort(kid);

        /* Local Command */
        // Just send msg to tgt and do nothing
        // And we should lock the local query to wait the msg back
        if (cmdPort == null) {
            // 本地指令
            Log.d("L-QUERY", "1. " + Dynamo.getPerferPortList(kid) +
                    " ~~~ " + key + "::" + kid);

            if (key.equals("@")) {
                Log.d("L-QUERY", "2. QUERY @");
                if (timoutPort==null) {
                    c = this.queryAll();
                } else {
                    c = this.queryAllTT(timoutPort); // Timeout Port
                }
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
                    GV.msgSendQ.offer(new MSG(MSG.TYPE.QUERY,
                            GV.PRED_PORT, "*", "???"));
                    Log.d("L-QUERY", "3. SEND * TO PRED " + GV.PRED_PORT);

                    // 2. wait for all nodes result
                    GV.queryAllReturnPort.clear();
                    GV.queryAllReturnPort.add(GV.MY_PORT);
                    while (GV.needWaiting) {}

                    // 3. combine all the result
                    c = this.makeCursor(GV.resultAllMap);
                    GV.resultAllMap.clear();
                    Log.d("L-QUERY", "4. RECV * FROM ALL");

                }
                // not equal to "@" or "*"
                else {
                    Log.d("L-QUERY", "2. QUERY " + key);

                    if (lastPort.equals(GV.MY_PORT)) {
                        // 本地就是 perferlist 最后一个
                        c = this.queryOne(key);

                        if (c.getCount() == 0) {
                            GV.resultOneMap.clear();
                            // Tell pred node to search for me
                            GV.msgSendQ.offer(new MSG(MSG.TYPE.QUERY,
                                    GV.PRED_PORT, key, "???"));
                            Log.d("L-QUERY", "3. SEND TO PRED " + GV.PRED_PORT + " ~" + key);

                            // wait for result
                            while (GV.needWaiting) {}

                            // return result
                            c = this.makeCursor(GV.resultOneMap);
                            GV.resultOneMap.clear();
                        }

                    } else {
                        // 节点非 perferlist 最后
                        GV.resultOneMap.clear();
                        GV.msgSendQ.offer(new MSG(MSG.TYPE.QUERY,
                                lastPort, key, "???"));
                        Log.d("L-QUERY", "3. SEND TO LAST " + lastPort + " ~" + key);

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
            // 外部指令
            Log.d("E-QUERY", "1. " + Dynamo.getPerferPortList(kid) + " ~~~ " + key + "::" + kid);

            if (key.equals("*")) {
                c = this.queryAll();
                c.moveToFirst();

                for (Entry entry: this.cursorToHashMap(c).entrySet()) {
                    GV.msgSendQ.offer(new MSG(MSG.TYPE.RESULT_ALL, cmdPort, cmdPort,
                            entry.getKey().toString(), entry.getValue().toString()));
                }
                GV.msgSendQ.offer(new MSG(MSG.TYPE.RESULT_ALL_FLAG, cmdPort, cmdPort));

                // 判断是否最后节点
                if (cmdPort.equals(GV.PRED_PORT)) {
                    // DONE: this node is the pred node of the cmd node
                    GV.msgSendQ.offer(new MSG(MSG.TYPE.RESULT_ALL_COMLETED,
                            cmdPort, cmdPort, "*", "???"));
                    Log.d("E-QUERY", "2. QUERY * DONE");

                } else {
                    GV.msgSendQ.offer(new MSG(MSG.TYPE.QUERY,
                            cmdPort, GV.PRED_PORT,"*", "???"));
                    Log.d("E-QUERY", "2. QUERY * AND SEND TO PRED " + GV.PRED_PORT);
                }

            }
            // not "*"
            else {
                // 列表里 search local
                c = this.queryOne(key);
                if (c.getCount() == 0) {
                    Log.e("E-QUERY", "2. DID NOT INSERT YET FOR KEY " + key);

                    if (firstPort.equals(GV.MY_PORT)) {
                        // 到头了，重新搜索
                        GV.msgSendQ.offer(new MSG(MSG.TYPE.QUERY,
                                cmdPort, GV.MY_PORT, key, "???"));
                        Log.d("E-QUERY", "3. SEND TO LAST " + GV.MY_PORT + " ~" + key);

                    } else {
                        // 没到头，向前搜索
                        GV.msgSendQ.offer(new MSG(MSG.TYPE.QUERY,
                                cmdPort, GV.PRED_PORT, key, "???"));
                        Log.d("E-QUERY", "3. SEND TO PRED " + GV.PRED_PORT + " ~" + key);
                    }
                } else {
                    Log.e("E-QUERY", "2. FOUND RESULT FOR KEY " + key);
                    c.moveToFirst();
                    String resKey = c.getString(c.getColumnIndex("key"));
                    String resVal = c.getString(c.getColumnIndex("value"));
                    GV.msgSendQ.offer(new MSG(MSG.TYPE.RESULT_ONE,
                            cmdPort, cmdPort, resKey, resVal));
                    Log.d("E-QUERY", "3. SEND BACK RESULT TO " + cmdPort + " ~" + key);
                }
            }
            return c;
        }
    }

    public void dbInsert(ContentValues cv, String cmdPort, boolean allowSend, String timeoutPort) {
        String key = cv.getAsString("key");
        String val = cv.getAsString("value");
        String kid = Dynamo.genHash(key);
        String firstPort = Dynamo.getFirstPort(kid);

        /* Local Command */
        // Just send msg to tgt and do nothing
        if (cmdPort == null) {
            Log.d("L-INSERT", "1. " + Dynamo.getPerferPortList(kid) + " ~~~ " +  key + "::" + kid);

            if (firstPort.equals(GV.MY_PORT)) {
                this.insertOne(cv); // cv: key, value, port
                GV.msgSendQ.offer(new MSG(MSG.TYPE.INSERT,
                        GV.MY_PORT, GV.SUCC_PORT, key, val));
                Log.d("L-INSERT", "2. SEND TO SUCC " + GV.SUCC_PORT + " ~" + key);

            } else {
                Log.d("L-INSERT", "2. SEND TO FIRST " + firstPort + " ~" + key);
                GV.msgSendQ.offer(new MSG(MSG.TYPE.INSERT,
                        GV.MY_PORT, firstPort, key, val));
            }
        }
        /* External Command */
        else {
            // 外部命令
            // 列表中
            Log.d("E-INSERT", "1. " + Dynamo.getPerferPortList(kid) + " ~~~ " + key + "::" + kid);
            if (timeoutPort == null) {
                this.insertOne(cv); // cv: key, value
                if (allowSend) {
                    if (!Dynamo.isLastNode(kid)) {
                        // NOT FINAL NODE & SEND TO SUCC
                        GV.msgSendQ.offer(new MSG(MSG.TYPE.INSERT,
                                cmdPort, GV.SUCC_PORT, key, val));
                        Log.d("E-INSERT", "2. INSERT AND SEND TO SUCC " + GV.SUCC_PORT + " ~" + key);

                    } else {
                        Log.d("E-INSERT", "2. UPDATE INSERT");
                    }

                } else {
                    Log.d("E-INSERT", "2. INSERT LOCAL AS LAST");
                }

            } else {
                this.insertOneTT(cv); // cv: key, value, port
            }
        }
    }

    public int dbDelete(String key, String cmdPort, boolean allowSend, String timeoutPort) {
        int affectedRows = 0;
        String kid = Dynamo.genHash(key);
        String firstPort = Dynamo.getFirstPort(kid);

        /* Local Command */
        // Just send msg to tgt and do nothing
        if (cmdPort == null) {
            Log.d("L-DELETE", "1. " + Dynamo.getPerferPortList(kid) +
                    " ~~~ " + key + "::" + kid);

            // "@"
            if (key.equals("@")) {
                if (timeoutPort == null) {
                    affectedRows = this.deleteAll();
                } else {
                    affectedRows = this.deleteAllTT(timeoutPort);
                }
                Log.d("L-DELETE", "2. " + "EXECUTE @");
            }

            // "*"
            if (key.equals("*")) {
                affectedRows = this.deleteAll();
                GV.msgSendQ.offer(new MSG(MSG.TYPE.DELETE,
                        GV.MY_PORT, GV.SUCC_PORT, key, "xxx"));
                Log.d("L-DELETE", "2. SEND * TO SUCC " + GV.SUCC_PORT);

            }
            // not "@" or "*"
            else {
                if (firstPort.equals(GV.MY_PORT)) {
                    affectedRows = this.deleteOne(key);
                    GV.msgRecvQ.offer(new MSG(MSG.TYPE.DELETE,
                            GV.MY_PORT, GV.SUCC_PORT, key, "xxx"));
                    Log.d("L-DELETE", "2. SEND TO SUCC " + GV.SUCC_PORT + " ~ " + key);

                } else {
                    GV.msgSendQ.offer(new MSG(MSG.TYPE.DELETE,
                            GV.MY_PORT, firstPort, key, "xxx"));
                    Log.d("L-DELETE", "2. DELETE LOCAL & SEND TO SUCC " + GV.SUCC_PORT + " ~ " + key);
                }
            }
            return affectedRows;
        }
        /* External Command */
        else {
            // 外部命令
            Log.d("E-DELETE", "1. " + Dynamo.getPerferPortList(kid) +
                    " ~~~ " + key + "<>" + kid);

            // Do real delete
            // "*"
            if (key.equals("*")) {
                affectedRows = this.deleteAll();
                Log.d("E-DELETE", "2. DELETE *");

                if (cmdPort.equals(GV.SUCC_PORT)) {
                    // DONE: last node command
                    Log.d("E-DELETE", "3. DONE *");
                } else {
                    // GO ON, Send to next node
                    if (allowSend) {
                        GV.msgSendQ.offer(new MSG(MSG.TYPE.DELETE,
                                cmdPort, GV.SUCC_PORT,"*", "xxx"));
                        Log.d("E-DELETE", "3. SEND * TO SUCC " + GV.SUCC_PORT);
                    }
                }

            }
            // not "@" or "*"
            else {
                // 列表中
                affectedRows = this.deleteOne(key);
                Log.d("E-DELETE", "2. DELETE " + key);
                if (allowSend) {
                    if (!Dynamo.isLastNode(kid)) {
                        // SEND TO SUCC
                        GV.msgSendQ.offer(new MSG(MSG.TYPE.DELETE,
                                cmdPort, GV.SUCC_PORT, key, "xxx"));
                        Log.d("E-DELETE", "3. SEND TO SUCC " + GV.SUCC_PORT + " ~" + key);
                    } else {
                        Log.d("E-DELETE", "3. UPDATE DELETE " + key);
                    }
                } else {
                    Log.d("E-DELETE", "3. DELETE LOCAL AS LAST");
                }

            }
        }

        return affectedRows;
    }


    // Basic insert, query and delete operation on database
    private void insertOne(ContentValues values) {
        long newRowId = this.dbHelper.getWritableDatabase().insertWithOnConflict(
                SQLiteHelper.LOCAL_TABLE, null,
                values, SQLiteDatabase.CONFLICT_IGNORE);
        Log.v("INSERT ONE LOCAL", "ROW = " + newRowId + "; KV = " +
                values.getAsString("key") + "<>" + values.getAsString("value"));
    }

    private Cursor queryOne(String key) {
        String[] columns = new String[] {SQLiteHelper.COL_NAME_KEY, SQLiteHelper.COL_NAME_VALUE};
        String selection = SQLiteHelper.COL_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        Cursor c = this.dbHelper.getReadableDatabase().query(
                SQLiteHelper.LOCAL_TABLE, columns, selection, selectedKey,
                null, null, null, "1");
        c.moveToFirst();

        if (c.getCount() == 0) {
            Log.e("QUERY MISS", "SEARCH FROM PRED NODE");
        } else {
            Log.v("QUERY", "KV = " + key + ", " + c.getString(c.getColumnIndex("value")));
        }
        return c;
    }

    private Cursor queryAll() {
        String[] columns = new String[] {SQLiteHelper.COL_NAME_KEY, SQLiteHelper.COL_NAME_VALUE};
        Cursor c = this.dbHelper.getReadableDatabase().query(
                SQLiteHelper.LOCAL_TABLE, columns, null, null,
                null, null, null);
        c.moveToFirst();
        Log.v("QUERY ALL", c.getCount() + " ROWS.");
        return c;
    }

    private int deleteOne(String key) {
        String selection = SQLiteHelper.COL_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};
        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                SQLiteHelper.LOCAL_TABLE, selection, selectedKey);
        Log.v("DELETE ONE", "Key = " + key);
        return affectedRows;
    }

    private int deleteAll() {
        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                SQLiteHelper.LOCAL_TABLE, null, null);
        Log.v("DELETE ALL", affectedRows + " ROWS.");
        // this.countRows();
        return affectedRows;
    }

    // Timeout Table
    private void insertOneTT(ContentValues values) {
        // values: key, value, port
        long newRowId = this.dbHelper.getWritableDatabase().insertWithOnConflict(
                SQLiteHelper.TIMEOUT_TABLE, null,
                values, SQLiteDatabase.CONFLICT_IGNORE);
        Log.v("INSERT ONE", "ROW = " + newRowId +
                "; KVP = " + values.getAsString("key") +
                "<>" + values.getAsString("value") +
                "<>" + values.getAsString("port"));
    }

    private Cursor queryAllTT(String port) {
        String[] columns = new String[] {SQLiteHelper.COL_NAME_KEY, SQLiteHelper.COL_NAME_VALUE};
        String selection = SQLiteHelper.COL_NAME_PORT + "=?";
        Cursor c = this.dbHelper.getReadableDatabase().query(
                SQLiteHelper.TIMEOUT_TABLE, columns, selection, new String[] {port},
                null, null, SQLiteHelper.COL_NAME_KEY);
        c.moveToFirst();
        Log.v("QUERY ALL", c.getCount() + " ROWS IN PORT " + port);
        return c;
    }

    private int deleteAllTT(String port) {
        //Log.e("ORIGINAL ROWS", this.countRows() + " ROWS.");
        String selection = SQLiteHelper.COL_NAME_PORT + "=?";
        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                SQLiteHelper.TIMEOUT_TABLE, selection, new String[] {port});
        Log.v("DELETE ALL", affectedRows + " ROWS IN PORT " + port);
        return affectedRows;
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

    @Override
	public boolean onCreate() {
        this.dbHelper = SQLiteHelper.getInstance(getContext());
        if (GV.deleteTable) {
            this.dbHelper.getWritableDatabase().execSQL("DELETE FROM " + SQLiteHelper.LOCAL_TABLE + ";");
            this.dbHelper.getWritableDatabase().execSQL("DELETE FROM " + SQLiteHelper.TIMEOUT_TABLE + ";");
        }
        return true;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) { return 0; }

	@Override
	public String getType(Uri uri) { return null; }


}
