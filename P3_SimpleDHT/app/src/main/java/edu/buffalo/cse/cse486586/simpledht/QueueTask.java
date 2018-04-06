package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class QueueTask extends AsyncTask<Void, Void, Void> {

    static final String TAG = "QUEUE";

    @Override
    protected Void doInBackground (Void...voids) {
        Log.d(TAG, "START QueueTask");

        while (true) {
            try {
                // 1. Handle Receive Message
                GV.isEmptyMRQ = GV.msgRecvQueue.peek() == null;
                if (!GV.isEmptyMRQ) {
                    Message msg = GV.msgRecvQueue.poll(); // with Remove

                    // TODO: Deal with recv msg

                    GV.isEmptyMRQ = GV.msgRecvQueue.peek() == null;
                }

                // 2. Handle Send Message
                GV.isEmptyMSQ = GV.msgSendQueue.peek() == null;
                if (!GV.isEmptyMSQ) {
                    Message msg = GV.msgSendQueue.poll(); // with Remove

                    // TODO: Deal with send msg

                    GV.isEmptyMSQ = GV.msgSendQueue.peek() == null;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
