package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class QueueTask extends AsyncTask<ContentResolver, Void, Void> {

    static final String TAG = "QUEUE";

    private Uri sUri = new Uri.Builder().scheme("content").authority(GV.URI).build();
    private ContentValues sCV = new ContentValues();
    private ContentResolver sCR;

    @Override
    protected Void doInBackground (ContentResolver... cr) {
        this.sCR = cr[0];

        Log.d(TAG, "START QueueTask");

        while (true) {
            try {
                // 1. Handle Receive Message
                GV.isEmptyMsgRecvQueue = GV.msgRecvQueue.peek() == null;
                if (!GV.isEmptyMsgRecvQueue) {
                    String msg = GV.msgRecvQueue.poll(); // with Remove

                    // TODO: Deal with recv msg

                    GV.isEmptyMsgRecvQueue = GV.msgRecvQueue.peek() == null;
                }

                // 2. Handle Send Message
                GV.isEmptyMsgSendQueue = GV.msgSendQueue.peek() == null;
                if (!GV.isEmptyMsgSendQueue) {
                    String msg = GV.msgSendQueue.poll(); // with Remove

                    // TODO: Deal with send msg

                    GV.isEmptyMsgSendQueue = GV.msgSendQueue.peek() == null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void queryOne() {}
    private void queryLocal() {}
    private void queryEntire() {}

    private void deleteOne() {}
    private void deleteLocal() {}
    private void deleteEntire() {}

    private void insertOne(String key, String value) {
        this.sCV.put("key", key);
        this.sCV.put("value", value);
        this.sCR.insert(this.sUri, this.sCV);
        this.sCV.clear();
    }
}
