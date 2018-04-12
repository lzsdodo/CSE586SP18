package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class QueueTask extends AsyncTask<Void, Void, Void> {

    static final String TAG = "QUEUE";

    static Chord chord;
    static long lastTime;

    @Override
    protected Void doInBackground (Void...voids) {
        Log.d(TAG, "START QueueTask");

        GV.PORTS.remove(GV.PORTS.indexOf(GV.MY_PORT));

        chord = Chord.getInstance();
        lastTime = System.currentTimeMillis();

        while (true) {
            try {
                // 0: TODO: Build the chord ring at first
                if (chord.getSuccPort() == null) {
                    if (System.currentTimeMillis() - lastTime > 1000) {
                        lastTime = System.currentTimeMillis();
                        int random = (int) (Math.random()*4); // [0, 1, 2, 3]
                        String tgtNodePort = GV.PORTS.get(random);
                        Log.e(TAG, "Choose " + tgtNodePort);
                        GV.msgSendQueue.offer(new Message(Message.TYPE.JOIN,
                                chord.getPort(), tgtNodePort, tgtNodePort));
                    }
                }

                // 1. TODO: Handle Receive Message
                if (!(GV.msgRecvQueue.peek() == null)) {
                    Message msg = GV.msgRecvQueue.poll(); // with Remove

                    String cmdPort = msg.getCmdPort();
                    chord.updateFingerTable(cmdPort);
                    
                    switch (msg.getMsgType()) {
                        case JOIN:
                            chord.getJoin(msg.getMsgValue());
                            break;

                        case NOTYFY:
                            chord.getNotify(msg.getMsgValue());
                            break;

                        case INSERT_ONE:
                            this.handleInsertOne(msg.getMsgKey(), msg.getMsgValue());
                            break;

                        case DELETE_ONE:
                            GV.dbCR.delete(GV.dbUri, msg.getMsgKey(), null);
                            break;

                        case DELETE_ALL:
                            GV.dbCR.delete(GV.dbUri, "*", new String[] {cmdPort});
                            break;

                        case QUERY_ONE:
                            GV.dbCR.query(GV.dbUri, null, msg.getMsgKey(), new String[] {cmdPort}, null);
                            break;

                        case QUERY_ALL:
                            GV.dbCR.query(GV.dbUri, null, "*", new String[] {cmdPort}, null);
                            break;

                        case QUERY_COMLETED:
                            GV.dbIsWaiting = false;
                            break;

                        case RESULT_ONE:
                            GV.resultOneMap.put(msg.getMsgKey(), msg.getMsgValue());
                            break;

                        case RESULT_ALL:
                            GV.resultAllMap.put(msg.getMsgKey(), msg.getMsgValue());
                            break;

                        default: break;
                    }

                }

                // 2. Handle Send Message
                if (!(GV.msgSendQueue.peek() == null)) {
                    Message msg = GV.msgSendQueue.poll(); // with Remove
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
