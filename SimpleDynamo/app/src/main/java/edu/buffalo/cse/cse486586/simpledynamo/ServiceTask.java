package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
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
                if (System.currentTimeMillis() - this.lastCheckTime > 500) {
                    this.signalService();
                    this.lastCheckTime = System.currentTimeMillis();
                }

                if (System.currentTimeMillis() - this.lastDetectTime > 1000) {
                    this.detectAlives();
                    this.lastDetectTime = System.currentTimeMillis();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void storeLostMsg(NMessage msg) {
        if (msg.getMsgType().equals(NMessage.TYPE.INSERT)) {
            msg.setMsgType(NMessage.TYPE.UPDATE_INSERT);
            Queue<NMessage> portQ = GV.notifyPortQueueM.get(msg.getTgtPort());
            portQ.offer(msg);
        }
    }

    private void handleLostMsg(NMessage msg) {
        // 1. Set lost
        String lostPort = msg.getTgtPort();
        Log.e("LOST PORT", "LOST PORT: " + lostPort);
        this.refreshUI("===== =====\nLOST PORT: " + lostPort + "\n===== =====");

        String kid = msg.getMsgKey();
        String firstPort = Dynamo.getFirstPort(kid);
        String lastPort = Dynamo.getLastPort(kid);

        // 2. Store in memory (if last to insert/delete)
        // 3. Send to other node (first/second to insert/delete) / local (last to query)
        Log.e("SKIP LOST PORT", "BEFORE SKIP MSG: " + msg.toString());
        switch (msg.getMsgType()) {
            case INSERT:
                if (!lostPort.equals(lastPort)) {
                    msg.setTgtPort(Dynamo.getSuccPortOfPort(lostPort));
                    msg.setSndPort(GV.MY_PORT);
                    GV.msgSendQ.offer(msg);
                    // GO ON SEND MSG
                }
                this.sendLostMsg(msg, lostPort, lastPort);
                break;

            case QUERY:
                if (lostPort.equals(firstPort)) {
                    msg.setTgtPort(lastPort);
                } else {
                    msg.setTgtPort(Dynamo.getPredPortOfPort(lostPort));
                }
                msg.setSndPort(GV.MY_PORT);
                GV.msgSendQ.offer(msg);
                break;

            default:
                break;
        }
        Log.e("SKIP LOST PORT", "AFTER SKIP MSG: " + msg.toString());
    }

    private void sendLostMsg(NMessage msg, String lostPort, String lastPort) {
        // send to second port
        if (lostPort.equals(GV.SUCC_PORT)) {
            NMessage nMsg = msg.copy();
            nMsg.setMsgType(NMessage.TYPE.LOST_INSERT);
            nMsg.setTgtPort(GV.PRED_PORT);
            nMsg.setCmdPort(lostPort);
            nMsg.setSndPort(GV.MY_PORT);
            GV.msgSendQ.offer(nMsg);

        } else {
            NMessage nMsg = msg.copy();
            nMsg.setMsgType(NMessage.TYPE.LOST_INSERT);
            nMsg.setTgtPort(GV.SUCC_PORT);
            nMsg.setCmdPort(lostPort);
            nMsg.setSndPort(GV.MY_PORT);
            GV.msgSendQ.offer(nMsg);
        }
    }

    private void detectAlives() {
        ArrayList<String> ports = Dynamo.PORTS;
        ports.remove(GV.MY_PORT);
        for (String port: ports) {
            Queue<NMessage> portQ = GV.notifyPortQueueM.get(port);
            if (!portQ.isEmpty()) {
                GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.IS_ALIVE,
                        GV.MY_PORT, port, "___"));
            }
        }
    }


    private void signalService() {
        if (!(GV.waitMsgIdQueue.isEmpty())) {
            String msgId = GV.waitMsgIdQueue.peek();

            if (!GV.waitMsgIdSet.contains(msgId)) {
                // Not contain this wait msg anymore
                GV.waitMsgIdQueue.poll(); // Remove and check next
                Log.v("RECEIVE SIGNAL", "ALREADY DELETE FROM WAIT QUEUE: " + msgId);

            } else {
                // TODO Handle LOST MSG
                NMessage msg = GV.waitMsgMap.get(msgId);
                // Handle: Send to next and
                this.handleLostMsg(msg);
                // Store
                this.storeLostMsg(msg);
                // Delete
                GV.waitMsgIdSet.remove(msgId);
                GV.waitMsgMap.remove(msgId);
                GV.waitMsgIdQueue.poll();

            }
        }
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
                Log.e("RESTART", "SEND BACK TO " + msg.getSndPort());
                this.prepareUpdate(msg.getSndPort());
                break;

            case IS_ALIVE:
                GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.RECOVERY,
                        GV.MY_PORT, msg.getSndPort(), "___"));
                break;

            case RECOVERY:
                this.prepareUpdate(msg.getSndPort());
                break;

            case UPDATE_INSERT:
                Log.d("UPDATE_INSERT", msg.toString());
                this.updateInsert(msg.getMsgKey(), msg.getMsgVal(), cmdPort);
                break;

            case UPDATE_DELETE:
                Log.d("UPDATE_DELETE", msg.toString());
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
        Queue<NMessage> portQ = GV.notifyPortQueueM.get(sndPort);
        this.refreshUI("RECOVERING PORT: " + sndPort + " with size " + portQ.size());

        while (!portQ.isEmpty()) {GV.msgUpdateSendQ.offer(portQ.poll());}
        GV.msgUpdateSendQ.offer(new NMessage(NMessage.TYPE.UPDATE_COMPLETED,
                GV.MY_PORT, sndPort, "FROM "+GV.MY_PORT));
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
