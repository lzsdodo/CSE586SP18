package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Log;



public class QueueTask extends AsyncTask<ContentResolver, Void, Void> {

    static final String TAG = "QUEUE";

    private ContentResolver qCR;
    private Dynamo dynamo;
    private long lastTime;

    @Override
    protected Void doInBackground (ContentResolver...cr) {
        Log.d(TAG, "START QueueTask");

        this.qCR = cr[0];
        this.dynamo = Dynamo.getInstance();
        this.lastTime = System.currentTimeMillis();

        while (true) {
            try {

                // 0: Update at first


                // 1. Handle Receive Message
                if (!(GV.msgRecvQueue.peek() == null)) {
                    NMessage msg = GV.msgRecvQueue.poll(); // with Remove
                    Log.e("HANDLE RECV MSG", "" + msg.toString());

                    String cmdPort = msg.getCmdPort();
                    switch (msg.getMsgType()) {
                        default: break;
                    }

                }

                if (!(GV.msgSendQueue.peek() == null)) {
                    if (System.currentTimeMillis() - this.lastTime > 100) {
                        this.lastTime = System.currentTimeMillis();

                        NMessage msg = GV.msgSendQueue.poll(); // with Remove
                        Log.e("HANDLE SEND MSG", "" + msg.toString());
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg.toString(), msg.getTgtPort());
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void releaseLock(String whichLock) {
        if (whichLock.equals("ONE")) {
            synchronized (GV.lockOne) {
                GV.lockOne.notifyAll();
            }
        }

        if (whichLock.equals("ALL")) {
            synchronized (GV.lockAll) {
                GV.lockAll.notifyAll();
            }
        }
    }

    private void handleInsertOne(String key, String value) {
        /*
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value);
        this.qCR.insert(GV.dbUri, cv);
        cv.clear();
        */
    }

    private void handleQueryOne(String key) {
    }

    private void handleDeleteOne(String key) {

    }

}
