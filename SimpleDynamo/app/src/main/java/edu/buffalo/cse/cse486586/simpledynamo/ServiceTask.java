package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;


public class ServiceTask extends AsyncTask<ContentResolver, Void, Void> {

    static String TAG = "SERVICE";

    private ContentResolver qCR;
    private long lastCheck;

    @Override
    protected Void doInBackground (ContentResolver... cr) {
        Log.v(TAG, "Start ServiceTask.");

        this.qCR = cr[0];
        this.lastCheck = System.currentTimeMillis();

        while (true) {
            try {

                while (!GV.msgUpdateRecvQ.isEmpty()) {
                    NMessage msg = GV.msgUpdateRecvQ.poll(); // with Remove
                    Log.e("HANDLE UPDATE MSG", msg.toString());
                    String cmdPort = msg.getCmdPort();
                    switch (msg.getMsgType()) {
                        case ALIVE:
                            break;

                        case RECOVERY:
                            Log.e("RECOVERY", "SEND BACK TO " + msg.getSndPort());
                            if ((cmdPort.equals(GV.lostPort)) || GV.lostPort == null) {
                                this.prepareUpdate(msg.getSndPort());
                                GV.lostPort = null;
                            }
                            break;

                        case UPDATE_INSERT:
                            this.updateInsert(msg.getMsgKey(), msg.getMsgVal(), cmdPort);
                            break;

                        case UPDATE_DELETE:
                            qCR.delete(GV.dbUri, msg.getMsgKey(), new String[] {cmdPort, "notAllowToSend"});
                            break;

                        case UPDATE_COMPLETED:
                            Dynamo dynamo = Dynamo.getInstance();
                            if (cmdPort.equals(dynamo.getPredPort())) {
                                this.refreshUI("UPDATE COMPLETED PRED " + dynamo.getPredPort());
                            }
                            if (cmdPort.equals(dynamo.getSuccPort())) {
                                this.refreshUI("UPDATE COMPLETED SUCC " + dynamo.getSuccPort());
                            }
                            break;

                        default:
                            Log.e("SERVICE UPDATE ERROR", "SHOULD NOT HAPPEN!!!!!!!!!!!!");
                            break;
                    }
                }

                // Check latest msg that wait to confirm

                if (System.currentTimeMillis() - lastCheck > 200) {
                    this.lastCheck = System.currentTimeMillis();

                    if (!(GV.waitMsgQueue.isEmpty())) {
                        NMessage waitMsg = GV.waitMsgQueue.peek();
                        String waitMsgId = waitMsg.getMsgID();

                        if (GV.waitMsgIdSet.contains(waitMsgId)) {
                            int lastTime = GV.waitTimeMap.get(waitMsgId);
                            int nowTime = (int) System.currentTimeMillis();
                            int deltaTime = nowTime - lastTime;

                            if (deltaTime > 1000) {
                                // TODO Handle
                                this.handleWaitMsg(waitMsg);
                                // Remove
                                 GV.waitMsgQueue.poll();
                                Log.e("SINGAL TIMEOUT", lastTime + " - " + nowTime + " = " + deltaTime +
                                        " (delta) for msg: " + waitMsg.toString());
                            } else {
                                Log.v("SERVICE SIGNAL", "CONTINUE WAIT: delta time = " + deltaTime);
                            }
                        } else {
                            // Not contain this wait msg anymore
                            GV.waitMsgQueue.poll();
                            Log.v("SERVICE SIGNAL", "NOT EXIST");
                        }
                    }
                }

                // Handle Receive Message
                if (!(GV.msgRecvQ.isEmpty())) {
                    NMessage msg = GV.msgRecvQ.poll(); // with Remove
                    Log.d("HANDLE RECV MSG", "" + msg.toString());

                    String cmdPort = msg.getCmdPort();
                    switch (msg.getMsgType()) {

                        case QUERY:
                            Log.d("HANDLE QUERY", msg.toString());
                            qCR.query(GV.dbUri, null, msg.getMsgKey(), new String[] {cmdPort}, null);
                            break;

                        case INSERT:
                            Log.d("HANDLE INSERT", msg.getMsgBody());
                            this.handleInsert(msg.getMsgKey(), msg.getMsgVal(), cmdPort);
                            break;

                        case DELETE:
                            Log.d("HANDLE DELETE", msg.toString());
                            qCR.delete(GV.dbUri, msg.getMsgKey(), new String[] {cmdPort});
                            break;

                        case RESULT_ONE:
                            Log.d("HANDLE RESULT ONE", msg.getMsgBody());
                            GV.resultOneMap.put(msg.getMsgKey(), msg.getMsgVal());
                            GV.needWaiting = false;
                            break;

                        case RESULT_ALL:
                            Log.d("HANDLE RESULT ALL", msg.getMsgBody());
                            GV.resultAllMap.put(msg.getMsgKey(), msg.getMsgVal());
                            break;

                        case RESULT_ALL_COMLETED:
                            Log.e("HANDLE QUERY COMPLETED", msg.toString());
                            GV.needWaiting = false;
                            break;

                        default:
                            Log.e("SERVICE HANDLE ERROR", "SHOULD NOT HAPPEN!!!!!!!!!!!!");
                            break;
                    }


                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleWaitMsg(NMessage msg) {
        Dynamo dynamo = Dynamo.getInstance();
    }


    // TEST
    private void prepareUpdate(String sndPort) {
        Dynamo dynamo = Dynamo.getInstance();

        if (sndPort.equals(dynamo.getPredPort())) {
            this.refreshUI("RECOVERING PRED: " + dynamo.getPredPort() +
                    " with size " + GV.notifyPredMsgL.size());
            for (NMessage msg: GV.notifyPredMsgL) {
                GV.msgUpdateSendQ.offer(msg);
            }
            GV.notifyPredMsgL.clear();
            GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.UPDATE_COMPLETED,
                        GV.MY_PORT, sndPort, "-", "FROM SUCC PORT"));

        }

        if (sndPort.equals(dynamo.getSuccPort())) {
            this.refreshUI("RECOVERING SUCC: " + dynamo.getSuccPort() +
                    " with size " + GV.notifySuccMsgL.size());
            for (NMessage msg: GV.notifySuccMsgL) {
                GV.msgUpdateSendQ.offer(msg);
            }
            GV.notifySuccMsgL.clear();
            GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.UPDATE_COMPLETED,
                    GV.MY_PORT, sndPort, "~", "FROM PRED PORT"));

        }
        this.refreshUI("PREPARE UPDATE SEND QUEUE SIZE: " + GV.msgUpdateSendQ.size());
    }

    private void handleInsert(String key, String value, String cmdPort) {
        ContentValues cv = new ContentValues();
        cv.put("cmdPort", cmdPort);
        cv.put("key", key);
        cv.put("value", value);
        qCR.insert(GV.dbUri, cv);
        cv.clear();
    }

    private void updateInsert(String key, String value, String cmdPort) {
        ContentValues cv = new ContentValues();
        cv.put("cmdPort", cmdPort);
        cv.put("allowSend", "allowSend");
        cv.put("key", key);
        cv.put("value", value);
        qCR.insert(GV.dbUri, cv);
        cv.clear();
    }

    private void refreshUI(String str) {
        Message uiMsg = new Message();
        uiMsg.what = SimpleDynamoActivity.UI;
        uiMsg.obj = str;
        SimpleDynamoActivity.uiHandler.sendMessage(uiMsg);
    }

}
