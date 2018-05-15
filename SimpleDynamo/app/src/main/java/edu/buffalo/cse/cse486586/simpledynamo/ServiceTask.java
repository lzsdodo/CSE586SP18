package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import junit.framework.Assert;

import java.util.Queue;


public class ServiceTask extends AsyncTask<ContentResolver, Void, Void> {

    static String TAG = "SERVICE";

    private ContentResolver qCR;
    private long lastCheckTime;
    private long deltaCheckTime;
    private int confirmLostTime;

    @Override
    protected Void doInBackground (ContentResolver... cr) {
        Log.v(TAG, "Start ServiceTask.");

        this.qCR = cr[0];
        this.lastCheckTime = System.currentTimeMillis();
        this.deltaCheckTime = 100;
        this.confirmLostTime = 1000;

        while (true) {
            try {
                if (System.currentTimeMillis() - this.lastCheckTime > this.deltaCheckTime) {
                    this.deltaCheckTime = this.checkFailureMsg();
                    this.lastCheckTime = System.currentTimeMillis();
                }

                if (!GV.msgRecvQ.isEmpty()) {
                    MSG msgRecv = GV.msgRecvQ.poll();

                    switch (msgRecv.getMsgType()) {
                        case SIGNAL:
                            this.handleSignal(msgRecv.getMsgKey());
                            break;

                        case INSERT:
                        case QUERY:
                        case DELETE:
                            GV.msgSignalSendQ.offer(msgRecv);
                            this.normalSerive(msgRecv);
                            break;

                        case RESULT_ONE:
                        case RESULT_ALL:
                        case RESULT_ALL_FLAG:
                        case RESULT_ALL_COMLETED:
                            this.normalSerive(msgRecv);
                            break;

                        case RESTART:
                        case IS_ALIVE:
                        case RECOVERY:
                        case LOST_INSERT:
                        case UPDATE_INSERT:
                        case UPDATE_COMPLETED:
                            this.updateSerive(msgRecv);
                            break;

                        default:
                            Log.e(TAG, "handleMsg -> SWITCH DEFAULT CASE ERROR: " + msgRecv.toString());
                            break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Main Service */
    // Detect Failure
    private long checkFailureMsg() {
        while (!GV.waitMsgIdQueue.isEmpty()) {
            String msgId = GV.waitMsgIdQueue.peek();

            if (!GV.waitMsgIdSet.contains(msgId)) {
                // Already recvive signal for that msg
                GV.waitMsgIdQueue.poll(); // Remove and check next
                Log.v("RECEIVE SIGNAL", "ALREADY DELETE FROM WAIT QUEUE: " + msgId);

            } else {
                // TODO Handle MSG THAT MAYBE LOST
                int lastTime = GV.waitMsgTimeMap.get(msgId);
                int deltaTime = (int) System.currentTimeMillis() - lastTime;

                if (deltaTime > this.confirmLostTime) {
                    MSG msg = GV.waitMsgMap.get(msgId);
                    this.refreshUI("TIMEOUT MSG: " + msg.toString());
                    Log.e("TIMEOUT MSG", lastTime + " + " + deltaTime + " (ms) the msg: " + msg.toString());

                    /* HANDLE TIMEOUT MSG */
                    //GV.resendQ.offer(msg);        // Try resend
                    this.storeLostMsg(msg);         // Store
                    this.handleLostMsg(msg);        // Handle: tSend to next and

                    // Delete
                    GV.waitMsgIdQueue.poll();
                    GV.waitMsgIdSet.remove(msgId);
                    GV.waitMsgTimeMap.remove(msgId);
                    GV.waitMsgMap.remove(msgId);

                } else {
                    Log.d(TAG, "NOT TIMEOUT YET: " + this.confirmLostTime + " - " + deltaTime);
                    return this.confirmLostTime - deltaTime;
                }
            }
        }
        Log.d(TAG, "NO MSG TO CHECK TIMEOUT: ALL SIGNAL RETURN");
        return this.confirmLostTime;
    }

    // Normal Service
    private void normalSerive(MSG msg) {
        Log.d("HANDLE RECV MSG", "" + msg.toString());

        String cmdPort = msg.getCmdPort();
        switch (msg.getMsgType()) {

            case QUERY:
                Log.d("HANDLE QUERY", msg.toString());
                qCR.query(GV.dbUri, null, msg.getMsgKey(),
                        new String[] {cmdPort}, null);
                break;

            case INSERT:
                Log.d("HANDLE INSERT", msg.getMsgBody());
                this.detectSkipMsg(msg);
                this.insert(msg.getMsgKey(), msg.getMsgVal(),
                        new String[] {cmdPort});
                break;

            case DELETE:
                Log.d("HANDLE DELETE", msg.toString());
                qCR.delete(GV.dbUri, msg.getMsgKey(),
                        new String[] {cmdPort});
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

    private void updateSerive(MSG msg) {
        Log.e("HANDLE UPDATE MSG", msg.toString());
        String cmdPort = msg.getCmdPort();
        switch (msg.getMsgType()) {

            case RESTART:
                Log.e("RESTART", "SEND BACK TO " + msg.getSndPort());
                this.prepareUpdate(msg.getSndPort());
                break;

            case IS_ALIVE:
                GV.msgUpdateSendQ.offer(new MSG(MSG.TYPE.RECOVERY,
                        GV.MY_PORT, msg.getSndPort()));
                break;

            case RECOVERY:
                this.prepareUpdate(msg.getSndPort());
                break;

            case LOST_INSERT:
                this.saveLostMsg(msg);
                break;

            case UPDATE_INSERT:
                Log.d("UPDATE_INSERT", msg.toString());
                this.insert(msg.getMsgKey(), msg.getMsgVal(),
                        new String[] {cmdPort, "notAllowSend"});
                break;

            case UPDATE_COMPLETED:
                if (cmdPort.equals(GV.PRED_PORT)) {
                    Log.e(TAG, "PRED\nUPDATE_COMPLETED\n UPDATE_COMPLETED" );
                }
                if (cmdPort.equals(GV.SUCC_PORT)) {
                    Log.e(TAG, "SUCC\nUPDATE_COMPLETED\n UPDATE_COMPLETED" );
                }
                break;

            default:
                Log.e("SERVICE UPDATE ERROR", "SHOULD NOT HAPPEN!!!!!!!!!!!!");
                break;
        }
    }

    private void backupService(MSG msg) {}

    /* Support Functions */
    // Support Signal Service
    private void handleLostMsg(MSG msg) {
        // 1. Set lost
        String lostPort = msg.getTgtPort();
        Log.e("LOST PORT", "LOST PORT: " + lostPort);

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
                } else {

                }
                this.sendLostMsg(msg, lostPort);
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

    private void sendLostMsg(MSG msg, String lostPort) {

        // send to second port
        if (!GV.MY_PORT.equals(Dynamo.getPredPortOfPort(lostPort))) {
            MSG nMsg = msg.copy();
            nMsg.setMsgType(MSG.TYPE.LOST_INSERT);
            nMsg.setCmdPort(lostPort);
            nMsg.setSndPort(GV.MY_PORT);
            nMsg.setTgtPort(Dynamo.getPredPortOfPort(lostPort));
            GV.msgSendQ.offer(nMsg);
        } else {
            MSG nMsg = msg.copy();
            nMsg.setMsgType(MSG.TYPE.LOST_INSERT);
            nMsg.setCmdPort(lostPort);
            nMsg.setSndPort(GV.MY_PORT);
            nMsg.setTgtPort(GV.PRED_PORT);
            GV.msgSendQ.offer(nMsg);
        }

        if (!GV.MY_PORT.equals(Dynamo.getSuccPortOfPort(lostPort))) {
            MSG nMsg = msg.copy();
            nMsg.setMsgType(MSG.TYPE.LOST_INSERT);
            nMsg.setCmdPort(lostPort);
            nMsg.setSndPort(GV.MY_PORT);
            nMsg.setTgtPort(Dynamo.getSuccPortOfPort(lostPort));
            GV.msgSendQ.offer(nMsg);
        } else {
            MSG nMsg = msg.copy();
            nMsg.setMsgType(MSG.TYPE.LOST_INSERT);
            nMsg.setCmdPort(lostPort);
            nMsg.setSndPort(GV.MY_PORT);
            nMsg.setTgtPort(GV.SUCC_PORT);
            GV.msgSendQ.offer(nMsg);
        }

    }


    // Support Update Service
    private void prepareUpdate(String sndPort) {
        Queue<MSG> portQ = GV.backupMsgQMap.get(sndPort);
        Log.e("PREPARE UPDATE", "RECOVERING PORT: " + sndPort + " with size " + portQ.size());

        while (portQ.peek()!=null) {GV.msgUpdateSendQ.offer(portQ.poll());}
        GV.msgUpdateSendQ.offer(new MSG(MSG.TYPE.UPDATE_COMPLETED,
                GV.MY_PORT, sndPort, "FROM "+GV.MY_PORT));
    }

    private void saveLostMsg(MSG msg) {
        msg.setMsgType(MSG.TYPE.UPDATE_INSERT);
        String tgtPort = msg.getCmdPort();
        msg.setTgtPort(tgtPort);
        msg.setSndPort(GV.MY_PORT);

        Queue<MSG> portQ = GV.backupMsgQMap.get(tgtPort);
        portQ.offer(msg);
    }

    // Support Backup Service
    private void storeLostMsg(MSG msg) {
        MSG nMsg = msg.copy();
        if (nMsg.getMsgType().equals(MSG.TYPE.INSERT)) {
            nMsg.setMsgType(MSG.TYPE.UPDATE_INSERT);
            Queue<MSG> portQ = GV.backupMsgQMap.get(nMsg.getTgtPort());
            portQ.offer(nMsg);
        }
    }


    // Support Normal Service
    private void detectSkipMsg(MSG msg) {
        // skip pred port: [0]/[1]
        if (Dynamo.detectSkipMsg(msg.getMsgKey(), msg.getSndPort(), msg.getTgtPort())) {
            MSG nmsg = msg.copy();
            nmsg.setMsgType(MSG.TYPE.UPDATE_INSERT);
            nmsg.setSndPort(GV.MY_PORT);
            nmsg.setTgtPort(GV.PRED_PORT);
            // TODO:
            //      Store in database ?
            //      Save this msg to memory ?
            Queue<MSG> backupQ = GV.backupMsgQMap.get(msg.getSndPort());
            backupQ.offer(msg);
            Log.e("DETECT SKIP MSG", "LOST PRED PORT " + GV.PRED_PORT);
        }

    }

    private void insert(String key, String value, String[] insertArgs) {
        Assert.assertNotNull(insertArgs);

        ContentValues cv = new ContentValues();
        cv.put("key", key);
        cv.put("value", value);

        cv.put("cmdPort", insertArgs[0]);
        cv.put("location", 0);
        switch (insertArgs.length) {
            case 2:
                cv.put("sendFlag", insertArgs[1]); // For Update/Backup
                break;
            case 3:
                cv.put("sendFlag", insertArgs[1]);
                cv.put("location", insertArgs[2]); // For Backup
                break;
            default: break;
        }

        qCR.insert(GV.dbUri, cv);
        cv.clear();
    }


    private void handleSignal(String msgId) {
        if (GV.waitMsgIdSet.contains(msgId)) {
            GV.waitMsgIdSet.remove(msgId);
            GV.waitMsgMap.remove(msgId);
            GV.waitMsgTimeMap.remove(msgId);
        } else {
            Log.e("RECV SIGNAL", "ALREADY DELETED FOR TIMEOUT ???");
        }
    }

    private void refreshUI(String str) {
        Message uiMsg = new Message();
        uiMsg.what = SimpleDynamoActivity.UI;
        uiMsg.obj = str;
        SimpleDynamoActivity.uiHandler.sendMessage(uiMsg);
    }

}
