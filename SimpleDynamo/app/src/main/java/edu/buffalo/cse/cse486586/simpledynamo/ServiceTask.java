package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Log;


public class ServiceTask extends AsyncTask<ContentResolver, Void, Void> {

    static String TAG = "SERVICE";

    private ContentResolver qCR;

    @Override
    protected Void doInBackground (ContentResolver... cr) {
        Log.v(TAG, "Start ServiceTask.");

        this.qCR = cr[0];

        while (true) {
            try {
                // Handle Receive Message
                if (!(GV.msgRecvQueue.isEmpty())) {
                    NMessage msg = GV.msgRecvQueue.poll(); // with Remove
                    Log.d("HANDLE RECV MSG", "" + msg.toString());

                    String cmdPort = msg.getCmdPort();
                    switch (msg.getMsgType()) {

                        case QUERY:
                            Log.d("HANDLE QUERY", msg.toString());
                            qCR.query(GV.dbUri, null, msg.getMsgKey(), new String[] {cmdPort}, null);
                            break;

                        case INSERT:
                            Log.d("HANDLE INSERT", msg.getMsgBody());
                            this.handleInsertOne(msg.getMsgKey(), msg.getMsgVal(), cmdPort);
                            break;

                        case DELETE:
                            Log.d("HANDLE DELETE", msg.toString());
                            qCR.delete(GV.dbUri, msg.getMsgKey(), new String[] {cmdPort});
                            break;

                        case RESULT_ONE:
                            Log.d("HANDLE RESULT ONE", msg.getMsgBody());
                            GV.resultOneMap.put(msg.getMsgKey(), msg.getMsgVal());
                            // this.releaseLock("ONE");
                            GV.needWaiting = false;
                            break;

                        case RESULT_ALL:
                            Log.d("HANDLE RESULT ALL", msg.getMsgBody());
                            GV.resultAllMap.put(msg.getMsgKey(), msg.getMsgVal());
                            break;

                        case RESULT_ALL_COMLETED:
                            Log.e("HANDLE QUERY COMPLETED", msg.toString());
                            //this.releaseLock("ALL");
                            GV.needWaiting = false;
                            break;

                        case UPDATE_DATA:break;

                        case UPDATE_COMPLETED:break;

                        default:break;
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
        qCR.insert(GV.dbUri, cv);
        cv.clear();
    }

}
