package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;


public class ServiceTask extends AsyncTask<ContentResolver, Void, Void> {

    static String TAG = "SERVICE";

    private ContentResolver qCR;

    @Override
    protected Void doInBackground (ContentResolver... cr) {
        Log.v(TAG, "Start ServiceTask.");

        this.qCR = cr[0];


        while (true) {

            try {

                while (!GV.updateRecvQueue.isEmpty()) {
                    NMessage msg = GV.updateRecvQueue.poll(); // with Remove
                    Log.e("HANDLE UPDATE MSG", msg.toString());
                    String cmdPort = msg.getCmdPort();

                    switch (msg.getMsgType()) {
                        case RECOVERY:
                            Log.e("RECOVERY", "PORT: " + msg.getSndPort());
                            if ((GV.lostPort == null) || (GV.lostPort.equals(msg.getSndPort()))) {
                                this.prepareUpdate(msg.getSndPort());
                                GV.lostPort = null;
                            }
                            break;

                        case LOST:
                            Log.e("LOST", "PORT: " +  cmdPort);
                            GV.lostPort = cmdPort;
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
                                this.refreshUI("UPDATE COMPLETED PRED: " + dynamo.getPredPort());
                                GV.updatePred = true;
                            }
                            if (cmdPort.equals(dynamo.getSuccPort())) {
                                GV.updateSucc = true;
                                this.refreshUI("UPDATE COMPLETED SUCC: " + dynamo.getSuccPort());
                            }
                            if (GV.updatePred && GV.updateSucc) {
                                this.refreshUI("UPDATE COMPLETED BOTH");
                                GV.updateCompleted = true;
                                GV.lostPort = null;
                            }
                            break;

                        default:
                            Log.e("UPDATE ERROR", "SHOULD NOT HAPPEN!!!!!!!!!!!!");
                            break;
                    }
                }

                // Handle Receive Message
                if ((!GV.msgRecvQueue.isEmpty()) && GV.updateCompleted) {
                    NMessage msg = GV.msgRecvQueue.poll(); // with Remove
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
                            Log.e("HANDLE ERROR", "SHOULD NOT HAPPEN!!!!!!!!!!!!");
                            break;
                    }


                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void prepareUpdate(String sndPort) {
        Dynamo dynamo = Dynamo.getInstance();

        if (sndPort.equals(dynamo.getPredPort())) {
            this.refreshUI("RECOVERING PRED: " + dynamo.getPredPort());
            while (!GV.notifyPredNode.isEmpty()) {
                NMessage notifyMsg = GV.notifyPredNode.poll();
                GV.updateSendQueue.offer(notifyMsg);
            }
            GV.updateSendQueue.offer(new NMessage(NMessage.TYPE.UPDATE_COMPLETED,
                    GV.MY_PORT, sndPort, "$$$", "FROM SUCC PORT"));
        }

        if (sndPort.equals(dynamo.getSuccPort())) {
            this.refreshUI("RECOVERING SUCC: " + dynamo.getSuccPort());
            while (!GV.notifySuccNode.isEmpty()) {
                NMessage notifyMsg = GV.notifySuccNode.poll();
                GV.updateSendQueue.offer(notifyMsg);
            }
            GV.updateSendQueue.offer(new NMessage(NMessage.TYPE.UPDATE_COMPLETED,
                    GV.MY_PORT, sndPort, "$$$", "FROM PRED PORT"));
        }
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
