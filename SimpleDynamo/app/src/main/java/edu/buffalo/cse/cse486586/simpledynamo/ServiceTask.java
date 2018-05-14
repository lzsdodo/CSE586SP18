package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;


public class ServiceTask extends AsyncTask<ContentResolver, Void, Void> {

    static String TAG = "SERVICE";

    private ContentResolver qCR;
    private long lastCheckTime;

    @Override
    protected Void doInBackground (ContentResolver... cr) {
        Log.v(TAG, "Start ServiceTask.");

        this.qCR = cr[0];
        this.lastCheckTime = System.currentTimeMillis();

        while (true) {
            try {

                // TODO Update Service
                while (!GV.msgUpdateRecvQ.isEmpty()) {
                    NMessage msg = GV.msgUpdateRecvQ.poll();
                    this.updateSerive(msg);
                }

                // TODO Normal Service
                if (!(GV.msgRecvQ.isEmpty())) {
                    NMessage msg = GV.msgRecvQ.poll();
                    this.normalSerive(msg);
                }

                // TODO Signal Service
                // Check latest msg that wait to confirm
                if (System.currentTimeMillis() - lastCheckTime > 200) {
                    this.lastCheckTime = this.signalService();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private long signalService() {
        if (!(GV.waitMsgQueue.isEmpty())) {
            NMessage checkMsg = GV.waitMsgQueue.peek();
            String msgId = checkMsg.getMsgID();

            if (GV.waitMsgIdSet.contains(msgId)) {
                int lastTime = GV.waitTimeMap.get(msgId);
                int nowTime = (int) System.currentTimeMillis();
                int deltaTime = nowTime - lastTime;

                if (deltaTime > 1000) {
                    // TODO Handle Lost
                    NMessage msg = GV.waitMsgQueue.poll(); // Remove
                    this.handleLostMsg(msg); // Handle
                    Log.e("SINGAL TIMEOUT", lastTime + " - " + nowTime + " = " + deltaTime +
                            " (delta) for msg: " + msg.toString());
                } else {
                    Log.v("SERVICE SIGNAL", "CONTINUE WAIT: delta time = " + deltaTime);
                    return System.currentTimeMillis();
                }
            } else {
                // Not contain this wait msg anymore
                GV.waitMsgQueue.poll(); // Remove and check next
                Log.v("SERVICE SIGNAL", "NOT EXIST");
            }
        }
        return System.currentTimeMillis();
    }

    private void handleLostMsg(NMessage msg) {
        Dynamo dynamo = Dynamo.getInstance();
        // 1. Set lost
        // 2. Send to other node
        // 3. Send to local
        // 4. Store in memory
    }


    private void normalSerive(NMessage msg) {
        Log.d("HANDLE RECV MSG", "" + msg.toString());

        String cmdPort = msg.getCmdPort();
        switch (msg.getMsgType()) {

            case QUERY:
                Log.d("HANDLE QUERY", msg.toString());
                qCR.query(GV.dbUri, null, msg.getMsgKey(), new String[] {cmdPort}, null);
                break;

            case INSERT:
                Log.d("HANDLE INSERT", msg.getMsgBody());
                this.normalInsert(msg.getMsgKey(), msg.getMsgVal(), cmdPort);
                break;

            case DELETE:
                Log.d("HANDLE DELETE", msg.toString());
                qCR.delete(GV.dbUri, msg.getMsgKey(), new String[] {cmdPort});
                break;

            case RESULT_ONE:
                Log.d("HANDLE RESULT ONE", msg.getMsgBody());
                if (GV.needWaiting) {
                    GV.resultOneMap.put(msg.getMsgKey(), msg.getMsgVal());
                    GV.needWaiting = false;
                }
                break;

            case RESULT_ALL:
                if (GV.needWaiting) {
                    Log.d("HANDLE RESULT ALL", msg.getMsgBody());
                    GV.resultAllMap.put(msg.getMsgKey(), msg.getMsgVal());
                }
                break;

            case RESULT_ALL_FLAG:
                GV.queryAllReturnPort.add(msg.getSndPort());
                // Means some node already completed
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

    private void updateSerive(NMessage msg) {
        Log.e("HANDLE UPDATE MSG", msg.toString());
        String cmdPort = msg.getCmdPort();
        switch (msg.getMsgType()) {
            case RESTART:
                GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.IS_ALIVE,
                        GV.MY_PORT, msg.getSndPort(), "___"));
                break;
            case IS_ALIVE:
                GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.RECOVERY,
                        GV.MY_PORT, msg.getSndPort(), "___"));
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
                if (cmdPort.equals(GV.PRED_PORT)) {
                    this.refreshUI("UPDATE COMPLETED PRED " + GV.PRED_PORT);
                }
                if (cmdPort.equals(GV.SUCC_PORT)) {
                    this.refreshUI("UPDATE COMPLETED SUCC " + GV.SUCC_PORT);
                }
                break;

            default:
                Log.e("SERVICE UPDATE ERROR", "SHOULD NOT HAPPEN!!!!!!!!!!!!");
                break;
        }
    }

    // TEST
    private void prepareUpdate(String sndPort) {
        if (sndPort.equals(GV.PRED_PORT)) {
            this.refreshUI("RECOVERING PRED: " + GV.PRED_PORT +
                    " with size " + GV.notifyPredMsgL.size());
            for (NMessage msg: GV.notifyPredMsgL) {
                GV.msgUpdateSendQ.offer(msg);
            }
            GV.notifyPredMsgL.clear();
            GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.UPDATE_COMPLETED,
                        GV.MY_PORT, sndPort, "FROM SUCC PORT"));
        }

        if (sndPort.equals(GV.SUCC_PORT)) {
            this.refreshUI("RECOVERING SUCC: " + GV.SUCC_PORT +
                    " with size " + GV.notifySuccMsgL.size());
            for (NMessage msg: GV.notifySuccMsgL) {
                GV.msgUpdateSendQ.offer(msg);
            }
            GV.notifySuccMsgL.clear();
            GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.UPDATE_COMPLETED,
                    GV.MY_PORT, sndPort, "FROM PRED PORT"));
        }
        this.refreshUI("PREPARE UPDATE SEND QUEUE SIZE: " + GV.msgUpdateSendQ.size());
    }

    private void normalInsert(String key, String value, String cmdPort) {
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
