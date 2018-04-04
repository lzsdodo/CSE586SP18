package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;

public class Message {

    public enum TYPE {
        NONE,
        INIT, REPLY, DELIVER,
        HEART, ALIVE
    }

    public enum TARGET_TYPE {
        NONE, GROUP, PID
    }

    private int msgID = 0; // unique msgID: 1{0}{01}: 1-{pid}-{msg counter}
    private TYPE msgType = TYPE.NONE;
    private String msgContent = "";

    private int senderPID = -1; // Sender Process ID
    private int fromPID = -1; // Return to specific Process ID
    private int propPID = 0; // Proposed Sequnece Process ID
    private int propSeqPrior = 0; // Proposed sequence number

    private TARGET_TYPE msgTargetType = Message.TARGET_TYPE.NONE;

    public Message() {}

    // For INIT, REPLY, DELIVER msg
    public Message(String msgContent, TYPE msgType) {
        this.msgContent = msgContent;
        this.msgType = msgType;
        this.senderPID = GV.MY_PID;

        switch (msgType) {
            case INIT:
                GV.counter += 1;
                this.msgID = Integer.parseInt("1" + GV.MY_PID + "00") + GV.counter;
                this.msgTargetType = TARGET_TYPE.GROUP;
                break;
            case REPLY:
                this.msgTargetType = TARGET_TYPE.PID;
            case DELIVER:
                this.msgTargetType = TARGET_TYPE.GROUP;
                break;
            default: break;
        }
        Log.d("BUILD MSG",  this.toString());
    }

    // For HEART, ALIVE signal
    public Message(TYPE msgType, int targetPID) {
        this("-", msgType);
        this.fromPID = targetPID;
        switch (msgType) {
            case HEART:
                this.msgTargetType = TARGET_TYPE.GROUP;
                break;
            case ALIVE:
                this.msgTargetType = TARGET_TYPE.PID;
                break;
            default: break;
        }
    }

    // BUILD MSG with Target
    public Message(String msgContent, TYPE msgType, TARGET_TYPE msgTargetType) {
        this(msgContent, msgType);
        this.msgTargetType = msgTargetType;
    }

    public int getMsgID() {return this.msgID;}
    public TYPE getMsgType() {return this.msgType;}
    public String getMsgContent() {return this.msgContent;}
    public int getSenderPID() {return this.senderPID;}
    public int getFromPID() {return this.fromPID;}
    public int getPropPID() {return this.propPID;}
    public int getPropSeqPrior() {return this.propSeqPrior;}
    public TARGET_TYPE getMsgTargetType() {return this.msgTargetType;}

    public void setMsgID(int msgID) {this.msgID = msgID;}
    public void setFromPID(int fromPID) {this.fromPID = fromPID;}
    public void setPropPID(int propPID) {this.propPID = propPID;}
    public void setPropSeqPrior(int propSeqPrior) {this.propSeqPrior = propSeqPrior;}

    public static Message parseMsg(String s) {
        Message msg = new Message();
        Log.d("PARSE MSG", s);
        String[] msgInfoList = s.split("::");
        msg.senderPID = Integer.parseInt(msgInfoList[0]);
        msg.msgID = Integer.parseInt(msgInfoList[1]);
        msg.msgType = TYPE.valueOf(msgInfoList[2]);
        msg.msgContent = msgInfoList[3];
        msg.propPID = Integer.parseInt(msgInfoList[4]);
        msg.propSeqPrior = Integer.parseInt(msgInfoList[5]);
        return msg;
    }

    public String toString() {
        // msgID::msgRecv::msgType::pPrior::sPID::status
        return this.senderPID + "::" + this.msgID + "::"
                + this.msgType.name() + "::" + this.msgContent + "::"
                + this.propPID + "::" + this.propSeqPrior;
    }

}

