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

                        case INSERT:
                            Log.e("HANDLE INSERT", msg.getMsgBody());
                            this.handleInsertOne(msg.getMsgKey(), msg.getMsgVal(), cmdPort);
                            break;

                        case DELETE:
                            Log.e("HANDLE DELETE", msg.toString());
                            this.qCR.delete(GV.dbUri, msg.getMsgKey(), new String[] {cmdPort});
                            break;


                        case QUERY:
                            Log.e("HANDLE QUERY", msg.toString());
                            this.qCR.query(GV.dbUri, null, msg.getMsgKey(), new String[] {cmdPort}, null);
                            break;

                        case RESULT_ONE:
                            Log.e("HANDLE RESULT ONE", msg.getMsgBody());
                            GV.resultOneMap.put(msg.getMsgKey(), msg.getMsgVal());
                            this.releaseLock("ONE");
                            break;

                        case RESULT_ALL:
                            Log.e("HANDLE RESULT ALL", msg.getMsgBody());
                            GV.resultAllMap.put(msg.getMsgKey(), msg.getMsgVal());
                            break;

                        case RESULT_ALL_COMLETED:
                            Log.e("HANDLE QUERY COMPLETED", msg.toString());
                            this.releaseLock("ALL");
                            break;

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


    private boolean isFailedSituation() {
        // INSERT/DELETE:
        //      sender and tgt's position in perferlist
        //      final msg send failed
        // QUERY:
        //      it is not last in perferList
        return false;
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

    private void handleInsertOne(String key, String value, String cmdPort) {
        ContentValues cv = new ContentValues();
        cv.put("cmdPort", cmdPort);
        cv.put("key", key);
        cv.put("value", value);
        this.qCR.insert(GV.dbUri, cv);
        cv.clear();
    }

    private void handleQueryOne(String key) {
    }

    private void handleDeleteOne(String key) {

    }

}
