package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;


/*
 * Reference:
 *  Dev Docs:
 *  ISIS Algorithm form Textbook and slides.
 */

public class QueueTask extends AsyncTask<ContentResolver, Void, Void> {

    static final String TAG = "QUEUE";

    private Uri sUri = new Uri.Builder().scheme("content").authority(GV.URI).build();
    private ContentValues sCV = new ContentValues();
    private ContentResolver sCR;

    private int msgNum = 0;
    private int msgTONum = 0;

    @Override
    protected Void doInBackground(ContentResolver... cr) {
        this.sCR = cr[0];
        Log.d(TAG, "START QueueTask");

        // Init
        this.msgNum = 0;
        this.msgTONum = 0;

        GV.nowMs = System.currentTimeMillis();
        GV.lastMs = GV.nowMs;
        while (GV.nowMs < GV.lastMs + 2500 - GV.MY_PID * 500) {
            GV.nowMs = System.currentTimeMillis();
        }
        GV.nowMs = System.currentTimeMillis();
        GV.lastMs = GV.nowMs;

        while (true) {
            try {
                // 1. Handle Send Message
                if(!GV.isEmptyMSQ) {
                    // Log.d("SEND QUEUE", "SIZE: " + GV.msgRecvQueue.size());
                    Message msg = GV.msgSendQueue.poll(); // item removed
                    switch (msg.getMsgType()) {
                        case INIT:
                            // Log.e("SEND INIT", msg.toString() + " TO GROUP");
                            this.sendMsgToGroup(msg.toString()); break;
                        case REPLY:
                            // Log.e("SEND REPLY", msg.toString() + " TO " + msg.getFromPID());
                            this.sendMsgToPID(msg.toString(), msg.getFromPID()); break;
                        case DELIVER:
                            // Log.e("SEND DELIVER", msg.toString() + " TO GROUP");
                            this.sendMsgToGroup(msg.toString()); break;
                        case HEART:
                            // Log.e("SEND HEART", "To " + msg.getMsgTargetType().name() + " " + msg.getFromPID());
                            this.sendHeartSignal(msg);
                            GV.nowMs = System.currentTimeMillis();
                            GV.lastMs = System.currentTimeMillis();
                            Log.e("TIME", "Send a heart beat signal when ms=" + GV.lastMs);
                            break;
                        case ALIVE:
                            // Log.e("SEND ALIVE", " To PID " + msg.getFromPID());
                            this.sendMsgToPID(msg.toString(), msg.getFromPID()); break;
                        default:
                            Log.e("MSG SEND TYPE ERROR", msg.toString()); break;
                    }
                }

                // 2. Handle Receive Message
                if(!GV.isEmptyMRQ) {
                    Message msg = GV.msgRecvQueue.poll(); // item removed
                    // Log.d("RECV QUEUE", "SIZE: " + GV.msgRecvQueue.size());
                    int fromPID = msg.getSenderPID();
                    switch (msg.getMsgType()) {
                        case INIT:
                            // Log.e("RECV INIT", msg.toString() + " FROM " + msg.getFromPID());
                            this.handleRecvInit(msg, fromPID); break;
                        case REPLY:
                            // Log.e("RECV REPLY", msg.toString() + " FROM " + msg.getFromPID());
                            this.handleRecvReply(msg, fromPID); break;
                        case DELIVER:
                            // Log.e("RECV DELIVER", msg.toString() + " FROM " + msg.getFromPID());
                            this.handleRecvDeliver(msg); break;
                        case HEART:
                            // Log.e("RECV HEART", "FROM " + msg.getFromPID());
                            this.handleRecvHeart(fromPID); break;
                        case ALIVE:
                            // Log.e("RECV ALIVE", "FROM " + msg.getFromPID());
                            this.handleRecvAlive(fromPID); break;
                        default:
                            Log.e("MSG RECV TYPE ERROR", msg.toString()); break;
                    }
                }

                // 3. Update Flag
                GV.isEmptyMSQ = GV.msgSendQueue.peek() == null; // check if empty
                GV.isEmptyMRQ = GV.msgRecvQueue.peek() == null; // check if empty

                // 4. Handle recv all reply and storge
                if (GV.isEmptyMRQ && GV.isEmptyMSQ) {

                    // Deal with waiting msg when not empty
                    if(!GV.msgWaitList.isEmpty()) {
                        int waitMid = GV.msgWaitList.get(0);
                        ArrayList<ArrayList<Integer>> waitProirList = GV.msgWaitProirMap.get(waitMid);
                        if (waitProirList != null) {
                            if (waitProirList.size() >= GV.devConnNum) {
                                // Collect all reply msg
                                // 1. Cal max agree proir
                                ArrayList<Integer> maxPropProir = this.getMaxPropPrior(waitProirList);
                                // 2. Remove
                                GV.msgWaitList.remove(0);
                                GV.msgWaitProirMap.remove(waitMid);
                                // 3. Build Deliver Msg
                                GV.msgSendQueue.offer(makeDeliverMsg(waitMid, maxPropProir.get(0), maxPropProir.get(1)));
                            } else {
                                GV.nowMs = System.currentTimeMillis();
                                if(GV.nowMs > GV.lastMs + GV.timeDelta) {
                                    GV.msgSendQueue.offer(new Message(Message.TYPE.HEART, GV.GROUP_PID));
                                    GV.nowMs = System.currentTimeMillis();
                                    GV.lastMs = GV.nowMs;
                                }
                            }
                        }
                    }

                    // Store msg to DB
                    // A lost device send init msg and not gonna to send deliver msg
                    if ((this.msgTONum > 15) && (this.msgTONum >= this.msgNum + GV.devConnNum - 5)) {
                        if (GV.msgTOIndex < this.msgTONum) {
                            Collections.sort(GV.msgTOList);
                            // Save to database
                            MessageInfo msgInfo = GV.msgTOList.get(GV.msgTOIndex);
                            this.sCV.put("key", Integer.toString(GV.msgTOIndex));
                            this.sCV.put("value", msgInfo.getMsgContent());
                            this.sCR.insert(this.sUri, this.sCV);
                            this.sCV.clear();
                            Log.e("DB", "SAVED " + GV.msgTOIndex + "\t" + msgInfo.getMsgContent());
                            GV.msgTOIndex += 1;
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 1. Functions of sending msg
    // new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
    private void sendMsgToPID(String string, int targetPID) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                string, Message.TARGET_TYPE.PID.name(), String.valueOf(targetPID));
    }

    private void sendMsgToGroup(String string) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                string, Message.TARGET_TYPE.GROUP.name(), "-1");
    }

    private void sendHeartSignal(Message msg) {
        this.sendMsgToGroup(msg.toString());
        Utils.recordHeartbeat();
        Utils.updateDevStatus();
    }

    // 2. Functions of handling receive msg
    private void handleRecvInit(Message msg, int fromPID) {
        this.msgNum += 1;
        int mid = msg.getMsgID();
        // 1. Put it into priorQueue
        GV.msgRecvOrderList.add(mid);
        MessageInfo msgInfo = new MessageInfo(msg);
        GV.msgInfoMap.put(mid, msgInfo);

        // 2. Calculate propPrior for this msg
        int propPrior = GV.msgInfoMap.size();
        propPrior = Math.max(propPrior, GV.agreePrior);
        GV.maxPropPrior = Math.max(GV.maxPropPrior, propPrior);
        GV.maxPropPrior += 1;

        // 3. Build reply msg
        String content = "FROM PID " + GV.MY_PID;
        Message replyMsg = new Message(content, Message.TYPE.REPLY, Message.TARGET_TYPE.PID);
        replyMsg.setMsgID(mid);
        replyMsg.setFromPID(fromPID);
        replyMsg.setPropPID(GV.MY_PID);
        replyMsg.setPropSeqPrior(GV.maxPropPrior);

        // 4. Put replyMsg and heart signal into msgSendQueue
        GV.msgSendQueue.offer(replyMsg);

        // Log Info here
        Log.d("MSG ORIGINAL ORDER", GV.msgRecvOrderList.size()
                + " - " + GV.msgRecvOrderList.toString());
    }

    private void handleRecvReply(Message msg, int fromPID) {
        int mid = msg.getMsgID();
        int propPrior = msg.getPropSeqPrior();

        // Collect reply msg
        ArrayList<Integer> msgPriors = new ArrayList<Integer>();
        msgPriors.add(fromPID);
        msgPriors.add(propPrior);

        // {<msgID>, [<pid, propSeqPrior>, ...]>}
        ArrayList<ArrayList<Integer>> msgWaitProirList = GV.msgWaitProirMap.get(mid);
        if(msgWaitProirList == null || msgWaitProirList.isEmpty())
            msgWaitProirList = new ArrayList<ArrayList<Integer>>();

        if (!msgWaitProirList.contains(msgPriors)) {
            msgWaitProirList.add(msgPriors);
            GV.msgWaitProirMap.put(mid, msgWaitProirList);
            Log.e("COLLECT", mid + ": " + msgWaitProirList.toString());
        }
    }

    private void handleRecvDeliver(Message msg) {
        // 1. Get msg info
        int mid = msg.getMsgID();
        int proPID = msg.getPropPID();
        int propPrior = msg.getPropSeqPrior();
        MessageInfo msgInfo = GV.msgInfoMap.get(mid);

        // 2. Set agreeProir
        GV.agreePrior = Math.max(GV.agreePrior, propPrior);

        // 3. Set propPID, propPrior and isDeliverable to msgInfo
        msgInfo.setPropPID(proPID);
        msgInfo.setPropSeqPrior(propPrior);
        msgInfo.setDiliverable(1);
        GV.msgInfoMap.put(mid, msgInfo);

        // 4. Remove waiProirMap and put it into Total Order List and sort
        GV.msgTOList.add(msgInfo);
        this.msgTONum += 1;

        // Log Info
        ArrayList orderList = new ArrayList();
        for (MessageInfo tmpMsgInfo: GV.msgTOList) {
            orderList.add(tmpMsgInfo.getMsgID());
        }
        Log.e("TO ORDER", orderList.toString());
    }

    private void handleRecvHeart(int fromPID) {
        GV.msgSendQueue.offer(new Message(Message.TYPE.ALIVE, fromPID));
    }

    private void handleRecvAlive(int fromPID) {
        Utils.updateHeartbeat(fromPID);
        Utils.updateDevStatus();
    }

    // 3. Other support functinos
    private ArrayList<Integer> getMaxPropPrior(ArrayList<ArrayList<Integer>> propProirList) {
        // Structure: [<pid, propSeqPrior>, ...]
        ArrayList<Integer> arrMaxPropProir = propProirList.get(0);

        for (ArrayList<Integer> arrPropProir: propProirList) {
            int propPID = arrPropProir.get(0);
            int propProir = arrPropProir.get(1);

            // choose highest sequence number first
            if(arrMaxPropProir.get(1) < propProir) {
                arrMaxPropProir.set(0, propPID);
                arrMaxPropProir.set(1, propProir);

            } else if (arrMaxPropProir.get(1) == propProir) {
                // if sequence number ==, choose smallest pid
                if(arrMaxPropProir.get(0) > propPID) {
                    arrMaxPropProir.set(0, propPID);
                }
            }
        }

        return arrMaxPropProir;
    }

    private Message makeDeliverMsg(int mid, int propPID, int propProir) {
        MessageInfo dlvrMsgInfo = GV.msgInfoMap.get(mid);
        Message dlvrMsg = new Message(dlvrMsgInfo.getMsgContent(), Message.TYPE.DELIVER);
        dlvrMsg.setMsgID(mid);
        dlvrMsg.setPropPID(propPID);
        dlvrMsg.setPropSeqPrior(propProir);
        return dlvrMsg;
    }

}
