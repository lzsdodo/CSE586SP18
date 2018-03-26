package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/*
 * Reference:
 *  ISIS Algorithm form Textbook and slides.
 */

public class QueueTask extends AsyncTask<ContentResolver, Void, Void> {

    static final String TAG = QueueTask.class.getSimpleName();


    static int msgReceivedNum = 0;

    private Uri sUri = new Uri.Builder().scheme("content").authority(GV.URI).build();
    private ContentValues sCV = new ContentValues();
    private ContentResolver sCR;

    private ArrayList<MessageInfo> infoList = new ArrayList<MessageInfo>();

    @Override
    protected Void doInBackground(ContentResolver... cr) {
        this.sCR = cr[0];

        Log.d(TAG, "START QueueTask");
        // new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);

        while (true) {
            try {
                // 1. Handle Receive Message
                GV.isEmptyMsgRecvQueue = GV.msgRecvQueue.peek() == null;
                if(!GV.isEmptyMsgRecvQueue) {
                    Message msg = GV.msgRecvQueue.poll(); // with Remove
                    Log.d("RECV QUEUE", msg.toString() + "\n"
                            + "SIZE OF MSG RECV QUEUE: " + GV.msgRecvQueue.size());

                    int fromPID = msg.getSenderPID();

                    switch (msg.getMsgType()) {
                        case INIT:
                            Log.e("RECV INIT", msg.toString() + " receive from " + msg.getFromPID());
                            // 1. save to deliver queue
                            Message replyMsg = new Message(msg.getMsgContent(),
                                    GV.MsgTypeEnum.REPLY, GV.MsgTargetTypeEnum.PID);
                            replyMsg.setFromPID(fromPID);
                            GV.msgSendQueue.offer(replyMsg);

                            Message heartSignal = new Message(GV.MsgTypeEnum.HEART, fromPID);
                            GV.msgSendQueue.offer(heartSignal);
                            break;

                        case REPLY:
                            Log.e("RECV REPLY", msg.toString() + " receive from " + msg.getFromPID());
                            break;

                        case DELIVER:
                            Log.e("RECV DELIVER", msg.toString() + " receive from " + msg.getFromPID());
                            break;

                        case HEART:
                            Log.e("RECV HEART", msg.toString() + " receive from " + msg.getFromPID());
                            Message aliveSignal = new Message(GV.MsgTypeEnum.ALIVE, fromPID);
                            GV.msgSendQueue.offer(aliveSignal);
                            // Log.e("RECV HEART", msg.getFromPID() + " send me heartbeat signal.");
                            break;

                        case ALIVE:
                            Log.e("RECV ALIVE", msg.toString() + " receive from " + msg.getFromPID());
                            Utils.updateHeartbeat(fromPID);
                            Utils.updateDevStatus();
                            // Log.e("RECV ALIVE", msg.getFromPID() + " send me alive signal.");
                            break;

                        default:
                            Log.e("MSG TYPE ERROR", msg.toString());
                            break;
                    }

                    GV.isEmptyMsgRecvQueue = GV.msgRecvQueue.peek() == null;
                }

                // 2. Handle Send Message
                GV.isEmptyMsgSendQueue = GV.msgSendQueue.peek() == null;
                if(!GV.isEmptyMsgSendQueue) {
                    Message msg = GV.msgSendQueue.poll(); // with Remove
                    msg.setSenderPID(GV.MY_PID);

                    switch (msg.getMsgType()) {
                        case INIT:
                            Log.e("SEND INIT", msg.toString() + " send to " + msg.getFromPID());
                            this.sendMsgToGroup(msg.toString()); break;

                        case DELIVER:
                            // Make sure the msg have been constructed right
                            this.sendMsgToGroup(msg.toString()); break;

                        // To specific pid
                        case REPLY:
                            // this.sendMsgToDev(msg.toString(), msg.getFromPID());
                            break;

                        case HEART:
                            // Remember the heart singal can be sent to group or pid
                            // send to pid needs the msg.getFromPID();
                            Log.e("SEND HEART", msg.toString() + " send to " + msg.getFromPID()
                                    + " target " + msg.getMsgTargetType().name() + " with targetID " + msg.getFromPID());
                            this.sendHeartSignal(msg);
                            Utils.recordHeartbeat(msg);
                            Utils.updateDevStatus();
                            break;

                        case ALIVE:
                            Log.e("SEND ALIVE", msg.toString() + " send to " + msg.getFromPID());
                            this.sendMsgToDev(msg.toString(), msg.getFromPID());
                            break;

                        default:
                            Log.e("MSG TYPE ERROR", msg.toString());
                            break;
                    }

                    Log.d("SEND QUEUE", msg.toString() + "\n"
                            + "SIZE OF MSG SEND QUEUE: " + GV.msgRecvQueue.size());
                    GV.isEmptyMsgSendQueue = GV.msgSendQueue.peek() == null;
                }

                // 3. Handle Deliver Message
                if (GV.isEmptyMsgRecvQueue && GV.isEmptyMsgSendQueue) {
                    //

                    Message deliverMsg = GV.msgDeliverQueue.poll(); // with remove
                    if (deliverMsg != null) {
                        Log.d("DELIVER QUEUE", deliverMsg.toString() + "\n"
                                + "SIZE OF MSG SEND QUEUE: " + GV.msgRecvQueue.size());

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 1. Functions of handling receive msg


    // 2. Functions of sending msg
    private void sendMsgToDev(String string, int targetPID) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                string, GV.MsgTargetTypeEnum.PID.name(), String.valueOf(targetPID));
    }

    private void sendMsgToGroup(String string) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                string, GV.MsgTargetTypeEnum.GROUP.name(), "-1");
    }

    private void sendHeartSignal(Message msg) {
        switch (msg.getMsgTargetType()) {
            case GROUP: this.sendMsgToGroup(msg.toString()); break;
            case PID: this.sendMsgToDev(msg.toString(), msg.getFromPID()); break;
            default:break;
        }
    }


    // 3. Functions of handling deliver msg

}




/*
// 1: handle receive msg
                GV.isEmptyRecvMsgQueue = GV.msgQueue.isEmpty();
                if (!GV.isEmptyRecvMsgQueue) {
                    Message msg = GV.msgQueue.get(0);
                    Log.w("NEW RECV MSG", msg.getMsgToSend() + "\n"
                            + "SIZE OF MSG RECV QUEUE: " + GV.msgQueue.size());

                    if (msg.getMsgType().equals("INIT")) {
                        Log.e("CHECK", "1 RECV INIT MSG" );
                        this.handleRecvInit(msg);

                    } else if (msg.getMsgType().equals("REPLY")) {
                        Log.e("CHECK", "3 RECV REPLY MSG" );
                        // Put {<msgID>, [<pid, propSeqPrior>, ...]>}
                        this.handleCollectReply(msg);
                        GV.isEmptyStatusOne = false;

                    } else if (msg.getMsgType().equals("DELIVER")) {
                        Log.d("CHECK", "X RECV DELIVER MSG");
                        Integer mid = msg.getMsgID();
                        this.handleRecvDeliver(msg);

                        // store to diveriable queue and sort
                        GV.msgOrderList.add(new MessageInfo(msg));
                        Collections.sort(GV.msgOrderList);
                        GV.msgIDMap.put(mid, 3);
                        GV.deliverableMsgNum = GV.deliverableMsgNum + 1;

                        // Print Info
                        Log.e(TAG, GV.msgIDMap.toString());
                        Log.e(TAG, GV.msgOrderList.toString());
                        this.infoList.add(GV.msgInfoMap.get(msg.getMsgID()));
                        Log.e(TAG, this.infoList.toString());

                    } else if (msg.getMsgType().equals("HEART")) {
                        this.rtnAliveMsg(msg.getSPID());

                    } else if (msg.getMsgType().equals("ALIVE")) {
                        this.checkDeathDev(msg);

                    }
                    GV.msgQueue.remove(0);
                }

                // 2. handle reply msg
                if (!GV.isEmptyReplyMsgQueue) {
                    Log.e("CHECK", "2 SCAN REPLY QUEUE" );
                    Message msg = GV.msgReplyQueue.get(0);
                    GV.msgReplyQueue.remove(0);

                    GV.msgIDMap.put(msg.getMsgID(), 1);
                    this.handleToReply(msg);

                    GV.isEmptyReplyMsgQueue = GV.msgReplyQueue.isEmpty();

                } else {
                    // 3. handle msg that receive from all other device
                    if (!GV.isEmptyStatusOne) {

                        Log.e("CHECK", "4 SCAN COLLECT REPLY LIST" );
                        // Deal with waiting msg
                        for(Map.Entry<Integer, Integer> msgIDStats: GV.msgIDMap.entrySet()) {
                            if (msgIDStats.getValue() == 1) {
                                int mid = msgIDStats.getKey();
                                // 2. Compare the proposed proirty
                                this.handleRtnCompare(mid);
                            }
                        }
                        GV.isEmptyStatusOne = !GV.msgIDMap.containsValue(1);
                    }
                }

                // Order Msg and save to DBS
                if (GV.isEmptyReplyMsgQueue && GV.isEmptyStatusOne) {
                    if (GV.msgOrderList.size()>10) {
                        if (msgReceivedNum < GV.msgOrderList.size()) {

                            Collections.sort(GV.msgOrderList);
                            Log.e("DATABASE", GV.msgOrderList.toString());
                            // save to database
                            MessageInfo msgInfo = GV.msgOrderList.get(msgReceivedNum);
                            this.sCV.put("key", Integer.toString(msgReceivedNum));
                            this.sCV.put("value", msgInfo.getMsg());
                            this.sCR.insert(this.sUri, this.sCV);
                            this.sCV.clear();
                            msgReceivedNum = msgReceivedNum + 1;
                            Log.d(TAG, "Saved MSG " + msgInfo.getMsg() + " to DB.");
                        }
                    }
                }
*/


/*
    // INIT
    private void handleRecvInit(Message msg) {
        Log.e("CHECK", "1-2 FROM INIT ENTER HANDLE PUT" );
        Integer mid = msg.getMsgID();
        MessageInfo msgInfo = new MessageInfo(msg);
        // 1. Put it into hold-back queue
        GV.msgReplyQueue.add(msg);
        GV.isEmptyReplyMsgQueue = false;

        GV.msgIDMap.put(mid, 0); // 0: first time received
        GV.msgInfoMap.put(mid, msgInfo);

        Log.e("PUT", "MSG TO REPLY QUEUE: " + msgInfo.toString() + "\n"
                + "PUT <" + mid + "-0> TO MSG-ID LIST.\n"
                + "NEW MSG-ID LIST: " + GV.msgIDMap.toString() + "\n"
                + "NUM OF MSG RECV: " + GV.msgIDMap.size());
    }

    private void handleToReply(Message msg) {
        Log.e("CHECK", "2-1 HANDLE TO REPLY" );
        // 2. Reply <mid, si> to sPID
        msg.setMsgType("REPLY");
        msg.rebuildMsgToSend();
        // BUG!!!
        Log.e("TO REPLY", "SEND BACK TO "
                + msg.getRPID() + ": " + msg.getMsgToSend());
        this.sendMsgWithHeartBeat(msg, String.valueOf(msg.getRPID()));
    }

    // REPLY
    private void handleCollectReply(Message msg) {
        Log.e("CHECK", "3-1 HANDLE COLLECT REPLY" );
        // 1. Put {<msgID>, [<pid, propSeqPrior>, ...]>}
        Integer mid = msg.getMsgID();

        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        arrayList.add(msg.getSPID());
        arrayList.add(msg.getPropSeqPrior());

        ArrayList<ArrayList<Integer>> propSeqProirList = GV.msgPropSeqProirListMap.get(mid);
        if(propSeqProirList == null || propSeqProirList.isEmpty())
            propSeqProirList = new ArrayList<ArrayList<Integer>>();

        if (!propSeqProirList.contains(arrayList)) {
            propSeqProirList.add(arrayList);
            GV.msgPropSeqProirListMap.put(msg.getMsgID(), propSeqProirList);
            Log.e("COLLECT", mid + ": " + propSeqProirList.toString());
        } else {
            Log.e("COLLECT", "RECEIVE SAME REPLY ???????");
        }

    }

    private void handleRtnCompare(Integer mid) {
        Log.e("CHECK", "4-1 HANDLE COMPARE" );
        ArrayList<ArrayList<Integer>> propProirList = GV.msgPropSeqProirListMap.get(mid);

        if (propProirList != null) {
            ClientTask.updateDevConnNum();
            if (propProirList.size() < GV.devConnNum) {
                Log.e("CHECK", "4-1-2 MSG " + mid + " IS WAITING.");
            } else {
                Log.e("CHECK", "4-1-2 HANDLE TO DELIVER\n"
                        + "GET ALL REPLY FOR MSG: " + mid);

                ArrayList<Integer> maxSeqPrior = this.getMaxPropPrior(propProirList); // Get max proir
                this.updateMsgInfoList(mid, maxSeqPrior); // Update msgInfoList

                GV.msgIDMap.put(mid, 2);
                Log.e(TAG, "MSG-STATUS: " + mid + " - 2 and B-Multicast now.");
                Log.e(TAG, GV.msgIDMap.toString() );

                // B-Multicast DELIVER msg
                MessageInfo msgInfo = GV.msgInfoMap.get(mid);
                Message newMsg = new Message(msgInfo, "DELIVER");
                newMsg.rebuildMsgToSend();
                this.sendMsgWithHeartBeat(newMsg, "GROUP");
            }
        } else {
            GV.msgIDMap.put(mid, 2);
        }
    }

    // MAX
    private void handleRecvDeliver(Message msg) {
        // 1. calculate max(GV.pSeqPriorNum, revcSeqPriorNum)
        // 2. Modify msgInfo by <pSeqPriorNum, suggestedPID, IsDiverable>
        int mid = msg.getMsgID();
        int newPropSeqProir = msg.getPropSeqPrior();

        MessageInfo msgInfo = GV.msgInfoMap.get(mid);
        int oldPropSeqProir = msgInfo.getPropSeqPrior(); // get old prior

        if (newPropSeqProir > oldPropSeqProir) {
            GV.propSeqPriorNum = newPropSeqProir;
            msgInfo.setPropPID(mid);
            msgInfo.setPropSeqPrior(newPropSeqProir);
        }

        GV.agreeSeqPriorNum = GV.propSeqPriorNum;
        msgInfo.setDiliverable(1);
        GV.msgInfoMap.put(mid, msgInfo);
    }

    private ArrayList<Integer> getMaxPropPrior(ArrayList<ArrayList<Integer>> propSeqProirList) {
        Log.e("CHECK", "7-1-1 GET MAX PROP PRIOR" );
        // Structure: [<pid, propSeqPrior>, ...]
        ArrayList<Integer> maxPropSeqProir = propSeqProirList.get(0);

        for (ArrayList<Integer> propSeqProir: propSeqProirList) {
            Integer tPID = propSeqProir.get(0);
            Integer tPropSeqProir = propSeqProir.get(1);

            // choose highest sequence number first
            if(maxPropSeqProir.get(1) < tPropSeqProir) {
                maxPropSeqProir.set(1, tPropSeqProir);
            } else if (maxPropSeqProir.get(1) == tPropSeqProir) {
                // if sequence number ==, choose smallest pid
                if(maxPropSeqProir.get(0) > tPID) {
                    maxPropSeqProir.set(1, tPropSeqProir);
                }
            }
        }
        return maxPropSeqProir;
    }

    private void updateMsgInfoList(Integer mid, ArrayList<Integer> highestSeqPrior) {
        MessageInfo msgInfo = GV.msgInfoMap.get(mid);
        msgInfo.setPropPID(highestSeqPrior.get(0)); // maxPID
        msgInfo.setPropSeqPrior(highestSeqPrior.get(1)); // maxSeqPriorNum
        GV.msgPropSeqProirListMap.remove(mid);
        GV.msgInfoMap.put(mid, msgInfo);
    }

    private void sendMsgWithHeartBeat(Message msg, String targetPID) {
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                msg.getMsgToSend(), targetPID);
        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                new Message("=.=", "HEART", false).getMsgToSend(),
                targetPID);
        Log.e("SEND WITH HEARTBEAT",  msg.getMsgToSend());
        if (targetPID.equals("GROUP")) {
            for(int i=0; i<5; i++) {
                int times = GV.devNotReplyTimes.get(i) + 1;
                GV.devNotReplyTimes.set(i, times);
            }
        } else {
            int tPID = Integer.valueOf(targetPID);
            int times = GV.devNotReplyTimes.get(tPID) + 1;
            GV.devNotReplyTimes.set(tPID, times);
        }

        Log.e("HEARTBEAT", "DEV STATUS: " + GV.devNotReplyTimes.toString());
    }


    */