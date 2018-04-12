package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Log;

public class QueueTask extends AsyncTask<Void, Void, Void> {

    static final String TAG = "QUEUE";

    static Chord chord;
    static long lastTime;

    @Override
    protected Void doInBackground (Void...voids) {
        Log.d(TAG, "START QueueTask");

        chord = Chord.getInstance();
        lastTime = System.currentTimeMillis();

        while (true) {
            try {
                // 0: Build the chord ring at first
                if (chord.getSuccPort() == null) {
                    if (System.currentTimeMillis() - lastTime > 1000) {
                        lastTime = System.currentTimeMillis();
                        if (!GV.MY_PORT.equals("5554"))
                            GV.msgSendQueue.offer(new Message(Message.TYPE.JOIN,
                                    chord.getPort(), "5554", chord.getPort()));
                    }
                }

                // 1. Handle Receive Message
                if (!(GV.msgRecvQueue.peek() == null)) {
                    Message msg = GV.msgRecvQueue.poll(); // with Remove
                    Log.e("HANDLE RECV MSG", "" + msg.toString());

                    String cmdPort = msg.getCmdPort();
                    chord.updateFingerTable(cmdPort);
                    
                    switch (msg.getMsgType()) {
                        case JOIN:
                            Log.e(TAG, "HANDLE JOIN FOR: " + msg.getMsgBody());
                            chord.getJoin(msg.getMsgBody());
                            break;

                        case NOTYFY:
                            Log.e(TAG, "HANDLE NOTIFY FOR: " + msg.getMsgBody());
                            chord.getNotify(msg.getMsgBody());
                            break;

                        case INSERT_ONE:
                            Log.e("HANDLE INSERT ONE", msg.getMsgBody());
                            this.handleInsertOne(msg.getMsgKey(), msg.getMsgValue());
                            break;

                        case DELETE_ONE:
                            Log.e("HANDLE DELETE ONE", msg.toString());
                            GV.dbCR.delete(GV.dbUri, msg.getMsgKey(), null);
                            break;

                        case DELETE_ALL:
                            Log.e("HANDLE DELETE ALL", msg.toString());
                            GV.dbCR.delete(GV.dbUri, "*", new String[] {cmdPort});
                            break;

                        case QUERY_ONE:
                            Log.e("HANDLE QUERY ONE", msg.toString());
                            GV.dbCR.query(GV.dbUri, null, msg.getMsgKey(), new String[] {cmdPort}, null);
                            break;

                        case QUERY_ALL:
                            Log.e("HANDLE QUERY ALL", msg.toString());
                            GV.dbCR.query(GV.dbUri, null, "*", new String[] {cmdPort}, null);
                            break;

                        case QUERY_COMLETED:
                            Log.e("HANDLE QUERY COMPLETED", msg.toString());
                            GV.dbIsWaiting = false;
                            break;

                        case RESULT_ONE:
                            Log.e("HANDLE RESULT ONE", msg.getMsgBody());
                            GV.resultOneMap.put(msg.getMsgKey(), msg.getMsgValue());
                            break;

                        case RESULT_ALL:
                            Log.e("HANDLE RESULT ALL", msg.getMsgBody());
                            GV.resultAllMap.put(msg.getMsgKey(), msg.getMsgValue());
                            break;

                        default: break;
                    }

                }

                // 2. Handle Send Message
                if (!(GV.msgSendQueue.peek() == null)) {
                    Message msg = GV.msgSendQueue.poll(); // with Remove
                    Log.e("HANDLE SEND MSG", "" + msg.toString());
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg.toString(), msg.getTgtPort());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInsertOne(String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value);
        GV.dbCR.insert(GV.dbUri, cv);
        cv.clear();

    }

}
