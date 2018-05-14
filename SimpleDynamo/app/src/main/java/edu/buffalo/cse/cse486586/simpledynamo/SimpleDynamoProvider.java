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

    public Cursor dbQuery(String key, String cmdPort) {
        Cursor c = null;
        String kid = Dynamo.genHash(key);
        String lastPort = Dynamo.getLastPort(kid);
        Dynamo dynamo = Dynamo.getInstance();

        /* Local Command */
        // Just send msg to tgt and do nothing
        // And we should lock the local query to wait the msg back
        if (cmdPort == null) {
            Log.d("L-QUERY", "1. " + " => " + Dynamo.getPerferPortList(kid) +
                    " ~~~ " + key + "<>" + kid);

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
                    GV.msgSendQ.offer(new NMessage(NMessage.TYPE.QUERY,
                            GV.MY_PORT, GV.SUCC_PORT, "*"));
                    Log.d("L-QUERY", "3. SEND * TO SUCC " + GV.SUCC_PORT);

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
                        // 本地就是列表最后一个
                        c = this.queryOne(key);

                        if (c.getCount() == 0) {
                            // 本地找不到
                            GV.resultOneMap.clear();
                            // 向前找 Tell pred node to search for me
                            GV.msgSendQ.offer(new NMessage(NMessage.TYPE.QUERY,
                                    GV.MY_PORT, GV.PRED_PORT, key));
                            Log.d("L-QUERY", "3. SEND TO PRED " + GV.PRED_PORT + " ~" + key);

                            // 等待 wait for result
                            while (GV.needWaiting) {}

                            // return result
                            c = this.makeCursor(GV.resultOneMap);
                            GV.resultOneMap.clear();
                        }

                    } else {
                        // 非列表最后一个
                        GV.resultOneMap.clear();
                        // 向目标发送查询 目标==列表最后一个
                        GV.msgSendQ.offer(new NMessage(NMessage.TYPE.QUERY,
                                GV.MY_PORT, lastPort, key));
                        Log.d("L-QUERY", "3. SEND TO LAST " + lastPort +
                                " in " + Dynamo.getPerferPortList(kid) + " ~" + key);

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
        // 外部命令
        // Do real query
        else {
            Log.d("E-QUERY", "1. " + " => " + Dynamo.getPerferPortList(kid) +
                    " ~~~ " + key + "<>" + kid);

            if (key.equals("*")) {
                c = this.queryAll();
                c.moveToFirst();

                for (Entry entry: this.cursorToHashMap(c).entrySet()) {
                    GV.msgSendQ.offer(new NMessage(NMessage.TYPE.RESULT_ALL,
                            cmdPort, cmdPort, entry.getKey().toString(), entry.getValue().toString()));
                }
                GV.msgSendQ.offer(new NMessage(NMessage.TYPE.RESULT_ALL_FLAG,
                        cmdPort, cmdPort, "___"));

                // 判断是否最后节点
                if (cmdPort.equals(dynamo.getSuccPort())) {
                    // DONE: this node is the pred node of the cmd node
                    GV.msgSendQ.offer(new NMessage(NMessage.TYPE.RESULT_ALL_COMLETED,
                            cmdPort, cmdPort, "*"));
                    Log.d("E-QUERY", "2. QUERY * DONE");

                } else {
                    GV.msgSendQ.offer(new NMessage(NMessage.TYPE.QUERY,
                            cmdPort, GV.SUCC_PORT,"*"));
                    Log.d("E-QUERY", "2. QUERY * AND SEND TO SUCC " + GV.SUCC_PORT);
                }

            }
            // not "*"
            else {
                // 判断是否在列表里面
                if (Dynamo.getPerferIdList(kid).indexOf(GV.MY_ID) == -1) {
                    // 不在列表里
                    GV.msgSendQ.offer(new NMessage(NMessage.TYPE.QUERY,
                            cmdPort, lastPort, key));
                    Log.d("E-QUERY", "3. SEND TO LAST " + lastPort + " ~" + key);

                } else {
                    // search local
                    // 在列表里
                    c = this.queryOne(key);

                    if (c.getCount() == 0) {
                        // 还是找不到
                        Log.e("E-QUERY", "2. DID NOT INSERT YET FOR KEY " + key);

                        if (Dynamo.isFirstNode(kid)) {
                            // 到头了，重新搜索
                            GV.msgSendQ.offer(new NMessage(NMessage.TYPE.QUERY,
                                    cmdPort, lastPort, key));
                            Log.d("E-QUERY", "3. SEND TO LAST " + lastPort + " ~" + key);

                        } else {
                            // 没到头，向前搜索
                            GV.msgSendQ.offer(new NMessage(NMessage.TYPE.QUERY,
                                    cmdPort, GV.PRED_PORT, key));
                            Log.d("E-QUERY", "3. SEND TO PRED " + GV.PRED_PORT + " ~" + key);
                        }

                    } else {
                        // 找到结果
                        Log.e("E-QUERY", "2. FOUND RESULT FOR KEY " + key);
                        c.moveToFirst();
                        String resKey = c.getString(c.getColumnIndex("key"));
                        String resVal = c.getString(c.getColumnIndex("value"));
                        GV.msgSendQ.offer(new NMessage(NMessage.TYPE.RESULT_ONE,
                                cmdPort, cmdPort, resKey, resVal));
                        Log.d("E-QUERY", "3. SEND BACK RESULT TO " + cmdPort + " ~" + key);
                    }
                }
            }
            return c;
        }
    }

    public void dbInsert(ContentValues cv, String cmdPort, boolean allowSend) {
        String key = cv.getAsString("key");
        String val = cv.getAsString("value");
        String kid = Dynamo.genHash(key);
        String firstPort = Dynamo.getFirstPort(kid);

        /* Local Command */
        // Just send msg to tgt and do nothing
        if (cmdPort == null) {
            Log.d("L-INSERT", "1. " + key + "::" + kid +
                    " => " + Dynamo.getPerferPortList(kid) + " ~~~ " + key + "<>" + kid);

            if (firstPort.equals(GV.MY_PORT)) {
                this.insertOne(cv);
                GV.msgSendQ.offer(new NMessage(NMessage.TYPE.INSERT,
                        GV.MY_PORT, GV.SUCC_PORT, key, val));
                Log.d("L-INSERT", "2. SEND TO SUCC " + GV.SUCC_PORT + " ~" + key);

            } else {
                Log.d("L-INSERT", "2. SEND TO TGT " + firstPort + " ~" + key);
                GV.msgSendQ.offer(new NMessage(NMessage.TYPE.INSERT,
                        GV.MY_PORT, firstPort, key, val));
            }
        }
        /* External Command */
        else {
            Log.d("E-INSERT", "1. " + " => " + Dynamo.getPerferPortList(kid) +
                    " ~~~ " + key + "<>" + kid);
            if (Dynamo.getPerferIdList(kid).indexOf(GV.MY_ID) == -1) {
                // 本地不在列表中
                if (allowSend) {
                    GV.msgSendQ.offer(new NMessage(NMessage.TYPE.INSERT,
                            cmdPort, firstPort, key, val));
                    Log.d("E-INSERT", "2. SEND TO TGT " + firstPort + " ~" + key +
                        "\nWith error, how can this happened? cmdPort = " + cmdPort);
                }
            } else {
                // 本地在列表中
                this.insertOne(cv);
                if (!Dynamo.isLastNode(kid)) {
                    // NOT FINAL NODE & SEND TO SUCC
                    if (allowSend) {
                        GV.msgSendQ.offer(new NMessage(NMessage.TYPE.INSERT,
                                cmdPort, GV.SUCC_PORT, key, val));
                        Log.d("E-INSERT", "2. INSERT AND SEND TO SUCC " + GV.SUCC_PORT + " ~" + key);
                    }
                }
            }
        }
    }

    public int dbDelete(String key, String cmdPort, boolean allowSend) {
        int affectedRows = 0;
        String kid = Dynamo.genHash(key);
        String firstPort = Dynamo.getFirstPort(kid);

        /* Local Command */
        // Just send msg to tgt and do nothing
        if (cmdPort == null) {
            Log.d("L-DELETE", "1. " + " => " + Dynamo.getPerferPortList(kid) +
                    " ~~~ " + key + "<>" + kid);

            // "@"
            if (key.equals("@")) {
                affectedRows = this.deleteAll();
                Log.d("L-DELETE", "2. " + "EXECUTE @");
            }

            // "*"
            if (key.equals("*")) {
                affectedRows = this.deleteAll();
                GV.msgSendQ.offer(new NMessage(NMessage.TYPE.DELETE,
                        GV.MY_PORT, GV.SUCC_PORT, key));
                Log.d("L-DELETE", "2. SEND * TO SUCC " + GV.SUCC_PORT);

            }
            // not "@" or "*"
            else {
                if (firstPort.equals(GV.MY_PORT)) {
                    // 本地为列表头
                    affectedRows = this.deleteOne(key);
                    GV.msgRecvQ.offer(new NMessage(NMessage.TYPE.DELETE,
                            GV.MY_PORT, GV.SUCC_PORT, key));
                    Log.d("L-DELETE", "2. DELETE LOCAL & SEND TO SUCC " + GV.SUCC_PORT + " ~" + key);

                } else {
                    // 本地非列表头
                    GV.msgSendQ.offer(new NMessage(NMessage.TYPE.DELETE,
                            GV.MY_PORT, firstPort, key));
                    Log.d("L-DELETE", "2. SEND TO FIRST " + firstPort + " ~" + key);
                }
            }
            return affectedRows;
        }
        /* External Command */
        // 外部命令
        else {
            Log.d("E-DELETE", "1. " + " => " + Dynamo.getPerferPortList(kid) +
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
                        GV.msgSendQ.offer(new NMessage(NMessage.TYPE.DELETE,
                                cmdPort, GV.SUCC_PORT,"*"));
                        Log.d("E-DELETE", "3. SEND * TO SUCC " + GV.SUCC_PORT);
                    }
                }

            }
            // not "@" or "*"
            else {
                if (Dynamo.getPerferIdList(kid).indexOf(GV.MY_ID) == -1) {
                    // 非列表中
                    if (!Dynamo.isLastNode(kid)) {
                        if (allowSend) {
                            GV.msgSendQ.offer(new NMessage(NMessage.TYPE.DELETE,
                                    cmdPort, firstPort, key));
                            Log.d("E-DELETE", "3. SEND TO TGT " + firstPort + " ~" + key +
                                    "\nWith error, how can this happened? cmdPort = " + cmdPort);
                        }
                    }

                } else {
                    // 列表中
                    affectedRows = this.deleteOne(key);
                    Log.d("E-DELETE", "2. DELETE " + key);

                    if (!Dynamo.isLastNode(kid)) {
                        if (allowSend) {
                            // SEND TO SUCC
                            GV.msgSendQ.offer(new NMessage(NMessage.TYPE.DELETE,
                                    cmdPort, GV.SUCC_PORT, key));
                            Log.d("E-DELETE", "3. SEND TO SUCC " + GV.SUCC_PORT + " ~" + key);
                        }
                    }
                }
            }
        }

        return affectedRows;
    }

    // Basic insert, query and delete operation on database
    private Cursor queryOne(String key) {
        String[] columns = new String[] {SQLiteHelper.COL_NAME_KEY, SQLiteHelper.COL_NAME_VALUE};
        String selection = SQLiteHelper.COL_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        Cursor c = this.dbHelper.getReadableDatabase().query(
                SQLiteHelper.TABLE_NAME, columns, selection, selectedKey,
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
        String[] columns = new String[] {SQLiteHelper.COL_NAME_KEY, SQLiteHelper.COL_NAME_VALUE};

        Cursor c = this.dbHelper.getReadableDatabase().query(
                SQLiteHelper.TABLE_NAME, columns, null, null,
                null, null, null);
        c.moveToFirst();

        Log.v("QUERY ALL", c.getCount() + " ROWS.");
        return c;
    }

    private void insertOne(ContentValues values) {
        long newRowId = this.dbHelper.getWritableDatabase().insertWithOnConflict(
                SQLiteHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        Log.v("INSERT ONE", "ROW=" + newRowId + "; Key<>Val=" +
                values.getAsString("key") + "<>" + values.getAsString("value"));
    }

    private int deleteOne(String key) {
        String selection = SQLiteHelper.COL_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                SQLiteHelper.TABLE_NAME, selection, selectedKey);

        Log.v("DELETE ONE", "Key=" + key);
        return affectedRows;
    }

    private int deleteAll() {
        //Log.e("ORIGINAL ROWS", this.countRows() + " ROWS.");
        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                SQLiteHelper.TABLE_NAME, null, null);
        Log.v("DELETE ALL", affectedRows + " ROWS.");
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
    public Cursor query(Uri uri, String[] projection, String selection,
                                     String[] selectionArgs, String sortOrder) {
        String cmdPort = (selectionArgs == null) ? null : selectionArgs[0];
        Cursor c = this.dbQuery(selection, cmdPort);
        return c;
    }

    @Override
    synchronized public Uri insert(Uri uri, ContentValues values) {
        String cmdPort = values.getAsString("cmdPort");
        values.remove("cmdPort");

        boolean allowSend = true;
        String flag = values.getAsString("allowSend");
        if (flag != null) {
            values.remove("allowSend");
            allowSend = false;
        }

        this.dbInsert(values, cmdPort, allowSend);
        return uri;
    }

    @Override
    synchronized public int delete(Uri uri, String selection, String[] selectionArgs) {
        String cmdPort = null;
        boolean allowSend = true;
        if (selectionArgs != null) {
            cmdPort = selectionArgs[0];
            if (selectionArgs.length == 2) {
                allowSend = false;
            }
        }
        return this.dbDelete(selection, cmdPort, allowSend);
    }

    @Override
	public boolean onCreate() {
        this.dbHelper = SQLiteHelper.getInstance(getContext());
        if (GV.deleteTable) {
            // Clean Table
            this.dbHelper.getWritableDatabase().execSQL("DELETE FROM " + SQLiteHelper.TABLE_NAME + ";");
        }
        return true;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) { return 0; }

	@Override
	public String getType(Uri uri) { return null; }


}
