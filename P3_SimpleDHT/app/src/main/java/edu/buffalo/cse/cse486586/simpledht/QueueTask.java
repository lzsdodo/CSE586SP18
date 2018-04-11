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
                    if (System.currentTimeMillis() - lastTime > 500) {
                        lastTime = System.currentTimeMillis();
                        int random = (int) (Math.random()*4); // [0, 1, 2, 3]
                        Log.e(TAG, "Choose " + GV.PORTS.get(random));
                        chord.sendJoin(GV.MY_PORT, GV.PORTS.get(random));
                    }
                }

                // 1. TODO: Handle Receive Message
                if (!(GV.msgRecvQueue.peek() == null)) {
                    Message msg = GV.msgRecvQueue.poll(); // with Remove

                    String cmdPort = msg.getCommandPort();
                    String nID = Utils.genHash(cmdPort);
                    switch (msg.getMsgType()) {
                        case JOIN: // TODO
                            chord.join(nID, cmdPort, false);
                            break;

                        case NOTYFY: // TODO
                            break;

                        case INSERT_ONE:
                            this.handleInsertOne(msg.getMsgKey(), msg.getMsgValue());
                            break;

                        case DELETE_ONE:
                            GV.dbCR.delete(GV.dbUri, msg.getMsgKey(), null);
                            break;

                        case DELETE_ALL:
                            this.handleDeleteAll(cmdPort);
                            break;

                        case QUERY_ONE:
                            this.handleQueryOne(cmdPort, msg.getMsgKey());
                            break;

                        case QUERY_ALL:
                            this.handleQueryAll(cmdPort);
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
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg.toString(), msg.getTargetPort());
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

    private void handleDeleteAll(String cmdPort) {
        GV.dbCR.delete(GV.dbUri, "@", null);

        if (cmdPort.equals(chord.getSuccPort())) {
            // DONE: this node is the pred node of the cmd node
            Log.d("DELETE ALL", "Command port: " + cmdPort + "\n" +
                            "This port: " + GV.MY_PORT + "\n" +
                            "Succ port: " + chord.getSuccPort());
        } else {
            // GO ON, Send to next node
            GV.msgSendQueue.offer(new Message(Message.TYPE.DELETE_ALL,
                    cmdPort, chord.getSuccPort(), "*", null));
            Log.d("DELETE ALL", "IN LOOP.");
        }

    }

    private void handleQueryOne(String cmdPort, String key) {
        GV.dbIsOtherQuery = true;
        Cursor c = GV.dbCR.query(GV.dbUri, null, key, null, null);

        if (c == null) {
            // Not in local, just tell target to query, no need to wait
            String kid = Utils.genHash(key);
            String targetPort = chord.lookup(kid);
            GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_ONE,
                    cmdPort, targetPort, key, null));

        } else {
            // in local, return the result
            c.moveToFirst();
            String resKey = c.getString(c.getColumnIndex("key"));
            String resValue = c.getString(c.getColumnIndex("value"));
            c.close();
            GV.msgSendQueue.offer(new Message(Message.TYPE.RESULT_ONE,
                    cmdPort, cmdPort, resKey, resValue));
        }
        GV.dbIsOtherQuery = false;
    }

    private void handleQueryAll(String cmdPort) {
        Cursor c = GV.dbCR.query(GV.dbUri, null, "@", null, null);
        for (Map.Entry entry: Utils.cursorToHashMap(c).entrySet()) {
            GV.msgSendQueue.offer(new Message(Message.TYPE.RESULT_ALL,
                    cmdPort, cmdPort, entry.getKey().toString(), entry.getValue().toString()));
        }

        if (cmdPort.equals(chord.getSuccPort())) {
            // DONE: this node is the pred node of the cmd node
            GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_COMLETED,
                    cmdPort, cmdPort, "*", null));
            Log.d("QUERY ALL", "Command port: " + cmdPort + "\n" +
                    "This port: " + GV.MY_PORT + "\n" +
                    "Succ port: " + chord.getSuccPort());
        } else {
            // GO ON, Send to next node
            GV.msgSendQueue.offer(new Message(Message.TYPE.QUERY_ALL,
                    cmdPort, chord.getSuccPort(), "*", null));
            Log.d("QUERY ALL", "IN LOOP.");
        }
    }

}
