package edu.buffalo.cse.cse486586.simpledynamo;

import java.util.HashMap;
import java.util.Map;
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

    static String TAG = "DB";

    private SQLiteHelper dbHelper;

    private String waitingKey = null;


    // API just used to send msg to target
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Dynamo dynamo = Dynamo.getInstance();
        String key = values.getAsString("key");
        String value = values.getAsString("value");
        String cmdPort = values.getAsString("cmdPort");

        if (cmdPort == null) {
            // 本地命令
            Log.d("INSERT", "LOCAL INSERT CMD - " + key);

            // send msg to target
            String tgtPort = dynamo.getWriteTgtPort(key);
            if (tgtPort.equals(dynamo.getPort())) {
                // 本地就是开头
                Log.d("INSERT", "START INSERT FROM LOCAL - " + key);
                this.insertOne(values);
                GV.msgSendQueue.offer(new NMessage(
                        NMessage.TYPE.INSERT,
                        dynamo.getPort(), dynamo.getPort(), dynamo.getSuccPort(),
                        key, value));

            } else {
                Log.d("INSERT", "Belongs to others: " + tgtPort);
                GV.msgSendQueue.offer(new NMessage(
                        NMessage.TYPE.INSERT,
                        dynamo.getPort(), dynamo.getPort(), tgtPort,
                        key, value));
            }

            // TODO: Maybe Can not send to fail node

        } else {
            // 外部命令
            Log.d("INSERT", "OUTSIDE INSERT CMD - " + key);

            int index = dynamo.getPerferIdList(key).indexOf(dynamo.getId());
            Log.d("INSERT", "No." + index + " - " + dynamo.getPort() + " -> " + dynamo.getSuccPort());

            // Insert
            values.remove("cmdPort");
            this.insertOne(values);

            // NOT FINAL NODE
            if (!dynamo.isLastNodeToWrite(key)) {
                GV.msgSendQueue.offer(new NMessage(
                        NMessage.TYPE.INSERT,
                        cmdPort, dynamo.getPort(), dynamo.getSuccPort(),
                        key, value));
            } else {
                if (this.waitingKey != null && this.waitingKey.equals(key)) {
                    this.waitingKey = null;
                    GV.lockOne.notify();
                }
            }
        }

        return uri;
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int affectedRows = 0;
        Dynamo dynamo = Dynamo.getInstance();
        String cmdPort = (selectionArgs == null) ? null : selectionArgs[0];
        Log.d("DELETE", selection + "<>");

        if (cmdPort == null) {
            // 本地命令

            // "@" or "*"
            if (selection.equals("@") || selection.equals("*")) {
                // Send to self
                GV.msgSendQueue.offer(new NMessage(
                        NMessage.TYPE.DELETE,
                        dynamo.getPort(), dynamo.getPort(), dynamo.getPort(),
                        selection, selection));

            } else {
                // not "@" or "*": send to target node
                String tgtPort = dynamo.getWriteTgtPort(selection);
                GV.msgSendQueue.offer(new NMessage(
                        NMessage.TYPE.DELETE,
                        dynamo.getPort(), dynamo.getPort(), tgtPort,
                        selection, selection));
            }



        } else {
            // 外部命令
            // "@" or "*"
            if (selection.equals("@")) {
                affectedRows = this.deleteAll();
            }
            if (selection.equals("*")) {
                affectedRows = this.deleteAll();

                if (cmdPort.equals(dynamo.getSuccPort())) {
                    // DONE: last node command
                    Log.d("DELETE ALL", "DONE");
                } else {
                    // GO ON, Send to next node
                    GV.msgSendQueue.offer(new NMessage(
                            NMessage.TYPE.DELETE,
                            cmdPort, dynamo.getPort(), dynamo.getSuccPort(),
                            "*", "---"));
                    Log.d("DELETE ALL", "CONTINUE");
                }

                // not "@" or "*"
            } else {
                affectedRows = this.deleteOne(selection);
                if (!dynamo.isLastNodeToWrite(selection)) {
                    // go on insert
                    GV.msgSendQueue.offer(new NMessage(
                            NMessage.TYPE.DELETE,
                            cmdPort, dynamo.getPort(), dynamo.getSuccPort(),
                            selection, "---"));
                }

            }
        }
        return affectedRows;
    }


    // TODO
	@Override
    public synchronized Cursor query(Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) {

        Cursor c = null;
        Dynamo dynamo = Dynamo.getInstance();
        String cmdPort = (selectionArgs == null) ? null : selectionArgs[0];
        Log.e(TAG, "QUERY " + cmdPort + ": " + selection );

        // "@"
        if (selection.equals("@")) {
            c = this.queryAll();
            c.moveToFirst();
            return c;
        }

        // "*"
        if (selection.equals("*")) {
            c = this.queryAll();
            c.moveToFirst();

            if (cmdPort == null) {
                // 本地指令
                synchronized (GV.lockAll) {
                    GV.resultAllMap.clear();
                    GV.resultAllMap = this.cursorToHashMap(c);

                    // 1. tell succ nodes
                    GV.msgSendQueue.offer(new NMessage(
                            NMessage.TYPE.QUERY,
                            dynamo.getPort(), dynamo.getPort(), dynamo.getSuccPort(),
                            "*", "???"));

                    // 2. wait for all nodes result
                    try {
                        Log.e("LOCK ALL", "WAITING FOR ALL RESULT");
                        GV.lockAll.wait();
                        Log.e("LOCK ALL", "END LOCK ALL");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // 3. combine all the result
                    c = this.makeCursor(GV.resultAllMap);
                    GV.resultAllMap.clear();
                }

            } else {
                // 外部指令
                for (Map.Entry entry : this.cursorToHashMap(c).entrySet()) {
                    GV.msgSendQueue.offer(new NMessage(
                            NMessage.TYPE.RESULT_ALL,
                            cmdPort, dynamo.getPort(), cmdPort,
                            entry.getKey().toString(), entry.getValue().toString()));
                }
                // 判断是否最后节点
                if (cmdPort.equals(dynamo.getSuccPort())) {
                    // DONE: this node is the pred node of the cmd node
                    GV.msgSendQueue.offer(new NMessage(
                            NMessage.TYPE.RESULT_ALL_COMLETED,
                            cmdPort, dynamo.getPort(), cmdPort,
                            "*", "???"));
                    Log.d("QUERY ALL", "DONE");

                } else {
                    GV.msgSendQueue.offer(new NMessage(
                            NMessage.TYPE.QUERY,
                            cmdPort, dynamo.getPort(), dynamo.getSuccPort(),
                            "*", "???"));
                    Log.d("QUERY ALL", "CONTINUE");
                }
            }
            return c;
        }


        // not equal to "@" or "*"
        String tgtPort = dynamo.getQueryTgtPort(selection);

        if (cmdPort == null) {
            // 本地命令

            if (tgtPort.equals(dynamo.getPort())) {
                // 本地就是 perferlist 最后一个
                c = this.queryOne(selection);

                if (c.getCount() == 0) {
                    this.waitingKey = selection;
                    // 还没插入到本地，等待
                    synchronized (GV.lockOne) {}
                    try {
                        Log.e("LOCK ONE", "WAITING FOR RESULT");
                        GV.lockOne.wait();
                        Log.e("LOCK ONE", "END LOCK ONE");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    c = this.queryOne(selection);
                }

            } else {
                // 本地命令，节点非 perferlist 最后
                synchronized (GV.lockOne) {}
                GV.resultOneMap.clear();
                // 向目标查找
                GV.msgSendQueue.offer(new NMessage(
                        NMessage.TYPE.QUERY,
                        dynamo.getPort(), dynamo.getPort(), tgtPort,
                        selection, "???"));
                // wait for one node result
                try {
                    Log.e("LOCK ONE", "WAITING FOR RESULT");
                    GV.lockOne.wait();
                    Log.e("LOCK ONE", "END LOCK ONE");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // get result and deal with it
                c = this.makeCursor(GV.resultOneMap);
                GV.resultOneMap.clear();

            }
            c.moveToFirst();
            return c;

        } else {
            // 接收到的查询命令
            if (tgtPort.equals(dynamo.getPort())) {
                // locate at local
                c = this.queryOne(selection);
                if (c.getCount() == 0) {
                    Log.e("QUERY WAIT", "WAIT FOR INSERT!!!");
                    this.waitingKey = selection;
                    // 还没插入到本地，等待
                    synchronized (GV.lockOne) {}
                    try {
                        Log.e("LOCK ONE", "WAITING FOR RESULT");
                        GV.lockOne.wait();
                        Log.e("LOCK ONE", "END LOCK ONE");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    c = this.queryOne(selection);
                    if (c.getCount() != 0)
                        Log.e("QUERY WAIT", "INSERT COMPLETED");
                }
                // 数据存在
                Log.e(TAG, "FOUND THE RESULT");
                c.moveToFirst();
                String resKey = c.getString(c.getColumnIndex("key"));
                String resVal = c.getString(c.getColumnIndex("value"));
                GV.msgSendQueue.offer(new NMessage(
                        NMessage.TYPE.RESULT_ONE,
                        cmdPort, dynamo.getPort(), cmdPort,
                        resKey, resVal));
            }

        }

        return c;
	}




    // Basic insert, query and delete operation on database
    private void insertOne(ContentValues values) {
        long newRowId = this.dbHelper.getWritableDatabase().insertWithOnConflict(
                this.dbHelper.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);

        Log.v("INSERT ONE", "ROW= " + newRowId + "; KV= " +
                values.getAsString("key") + "<>" + values.getAsString("value"));
    }

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
            Log.v("QUERY", "KV=\'" + key + ", " + c.getString(c.getColumnIndex("value")) + "\'");
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

    private int deleteOne(String key) {
        String selection = this.dbHelper.COL_NAME_KEY + "=?";
        String[] selectedKey = new String[] {key};

        int affectedRows = this.dbHelper.getWritableDatabase().delete(
                this.dbHelper.TABLE_NAME, selection, selectedKey);

        Log.v("DELETE ONE", "K= " + key);
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
            // Clean Table
            this.dbHelper.getWritableDatabase().execSQL("DELETE FROM " + dbHelper.TABLE_NAME + ";");
        }
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

}
