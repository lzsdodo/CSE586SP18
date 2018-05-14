package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import java.util.Queue;


public class ServiceTask extends AsyncTask<ContentResolver, Void, Void> {

    static String TAG = "SERVICE";

    private ContentResolver qCR;
    private long lastCheckTime;
    private long lastDetectTime;

    @Override
    protected Void doInBackground (ContentResolver... cr) {
        Log.v(TAG, "Start ServiceTask.");

        this.qCR = cr[0];
        this.lastCheckTime = System.currentTimeMillis();
        this.lastDetectTime = this.lastCheckTime;

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
                if (System.currentTimeMillis() - this.lastDetectTime > 100) {
                    this.signalService();
                    this.lastDetectTime = System.currentTimeMillis();
                }


                // TODO DETECT
                if (System.currentTimeMillis() - lastDetectTime > 500) {
                    if (!GV.notifyPredMsgL.isEmpty()) {
                        GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.IS_ALIVE,
                                GV.MY_PORT, GV.PRED_PORT, "___"));
                    }
                    if (!GV.notifySuccMsgL.isEmpty()) {
                        GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.IS_ALIVE,
                                GV.MY_PORT, GV.SUCC_PORT, "___"));
                    }
                    this.lastDetectTime = System.currentTimeMillis();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void signalService() {
        if (!(GV.waitMsgQueue.isEmpty())) {
            NMessage checkMsg = GV.waitMsgQueue.peek();
            String msgId = checkMsg.getMsgID();

            if (GV.waitMsgIdSet.contains(msgId)) {
                int lastTime = GV.waitTimeMap.get(msgId);
                int deltaTime = (int) System.currentTimeMillis() - lastTime;

                // TODO Handle Lost Msg
                NMessage msg = GV.waitMsgQueue.poll(); // Remove
                this.handleLostMsg(msg);    // Handle msg
                this.storeLostMsg(msg);     // Store msg
                Log.e("SIGNAL TIMEOUT", lastTime + " (delta) " + deltaTime +
                        " for msg: " + msg.toString());
            } else {
                // Not contain this wait msg anymore
                GV.waitMsgQueue.poll(); // Remove and check next
                Log.v("SERVICE SIGNAL", "NOT EXIST");
            }
        }
    }

    private void storeLostMsg(NMessage msg) {
        // 0. store
        Queue<NMessage> portQ = GV.storedMap.get(msg.getTgtPort());
        portQ.offer(msg);
    }

    private void handleLostMsg(NMessage msg) {
        // 1. Set lost
        Log.e("LOST PORT", "LOST PORT: " + GV.lostPort);
        this.refreshUI("===== =====\nLOST PORT: " + GV.lostPort + "\n===== =====");

        GV.lostPort = msg.getTgtPort();
        String tgtPort = msg.getTgtPort();

        String kid = msg.getMsgKey();
        String firstPort = Dynamo.getFirstPort(kid);
        String lastPort = Dynamo.getLastPort(kid);

        // 2. Store in memory (if last to insert/delete)
        // 3. Send to other node (first/second to insert/delete) / local (last to query)
        Log.e("SKIP LOST PORT", "BEFORE SKIP MSG: " + msg.toString());
        switch (msg.getMsgType()) {
            case INSERT:
                if (tgtPort.equals(lastPort)) {
                    msg.setMsgType(NMessage.TYPE.UPDATE_INSERT);
                    GV.notifySuccMsgL.add(msg);
                    // DO NOT SEND MSG BUT I CONTINUE
                } else {
                    msg.setTgtPort(Dynamo.getSuccPortOfPort(tgtPort));
                    msg.setSndPort(GV.MY_PORT);
                    GV.msgSendQ.offer(msg);
                }
                break;

            case DELETE:
                if (tgtPort.equals(lastPort)) {
                    msg.setMsgType(NMessage.TYPE.UPDATE_DELETE);
                    GV.notifySuccMsgL.add(msg);
                    // DO NOT SEND MSG
                } else {
                    msg.setTgtPort(Dynamo.getSuccPortOfPort(tgtPort));
                    msg.setSndPort(GV.MY_PORT);
                    GV.msgSendQ.offer(msg);
                    // GO ON SEND MSG
                }
                break;

            case QUERY:
                if (tgtPort.equals(firstPort)) {
                    msg.setTgtPort(lastPort);
                } else {
                    msg.setTgtPort(Dynamo.getPredPortOfPort(tgtPort));
                }
                msg.setSndPort(GV.MY_PORT);
                GV.msgSendQ.offer(msg);
                break;

            default:
                break;
        }
        Log.e("SKIP LOST PORT", "AFTER SKIP MSG: " + msg.toString());
    }

    private void normalSerive(NMessage msg) {
        Log.d("normalSerive", msg.toString());

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
        String sndPort = msg.getSndPort();

        switch (msg.getMsgType()) {
            case RESTART:
                if (msg.getSndPort().equals(GV.PRED_PORT) || msg.getSndPort().equals(GV.SUCC_PORT)) {
                    this.prepareUpdate(msg.getSndPort());
                }
                this.buildResendMsg(msg.getSndPort());
                break;

            case IS_ALIVE:
                Log.e("IS_ALIVE", "SEND BACK TO: " + msg.getSndPort());
                GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.RECOVERY,
                        GV.MY_PORT, msg.getSndPort(), "___"));
                break;

            case RECOVERY:
                this.prepareUpdate(msg.getSndPort());
                break;

            case UPDATE_INSERT:
                Log.d("UPDATE_INSERT", msg.toString());
                this.updateInsert(msg.getMsgKey(), msg.getMsgVal(), sndPort);
                break;

            case UPDATE_DELETE:
                Log.d("UPDATE_DELETE", msg.toString());
                qCR.delete(GV.dbUri, msg.getMsgKey(), new String[] {sndPort, "notAllowToSend"});
                break;

            case UPDATE_COMPLETED:
                if (sndPort.equals(GV.PRED_PORT)) {
                    this.refreshUI("UPDATE COMPLETED PRED " + GV.PRED_PORT);
                }
                if (sndPort.equals(GV.SUCC_PORT)) {
                    this.refreshUI("UPDATE COMPLETED SUCC " + GV.SUCC_PORT);
                }
                break;

            default:
                Log.e("", "SHOULD NOT HAPPEN!!!!!!!!!!!!");
                break;
        }
    }

    private void buildResendMsg(String restartPort) {
        Queue<NMessage> portQ = GV.storedMap.get(restartPort);
        while (!portQ.isEmpty()) {
            GV.msgResendQ.offer(portQ.poll());
        }

        while (!GV.waitMsgQueue.isEmpty()) {
            GV.msgResendQ.offer(GV.waitMsgQueue.poll());
        }
        GV.waitMsgIdSet.clear();
        GV.waitTimeMap.clear();
    }

    // TESTSERVICE UPDATE ERROR
    private void prepareUpdate(String sndPort) {

        if (sndPort.equals(GV.PRED_PORT)) {
            this.refreshUI("RECOVERING PRED: " + GV.PRED_PORT +
                    " with size " + GV.notifyPredMsgL.size());
            while (!GV.notifyPredMsgL.isEmpty()) {
                GV.msgUpdateSendQ.offer(GV.notifyPredMsgL.poll());
            }
            GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.UPDATE_COMPLETED,
                    GV.MY_PORT, sndPort, "FROM SUCC PORT"));
        }

        if (sndPort.equals(GV.SUCC_PORT)) {
            this.refreshUI("RECOVERING SUCC: " + GV.SUCC_PORT +
                    " with size " + GV.notifySuccMsgL.size());
            while (!GV.notifySuccMsgL.isEmpty()) {
                GV.msgUpdateSendQ.offer(GV.notifySuccMsgL.poll());
            }
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
