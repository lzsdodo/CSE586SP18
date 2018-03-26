package edu.buffalo.cse.cse486586.groupmessenger2;


import android.util.Log;
import java.lang.Math;

public class Message {

    static final String TAG = Message.class.getSimpleName();

    private int msgID = 0; // unique msgID: 1{0}{01}: 1-{pid}-{msg counter}
    private String msgContent = "";
    private GV.MsgTypeEnum msgType = GV.MsgTypeEnum.NONE;

    private int senderPID = 0; // Sender Process ID
    private int fromPID = 0; // Return to specific Process ID
    private int propPID = 0; // Proposed Sequnece Process ID
    private int propSeqPrior = 0; // Proposed sequence number
    private int isDeliverable = 0; // 1: diliverable; 0: undeliverable

    private GV.MsgTargetTypeEnum msgTargetType = GV.MsgTargetTypeEnum.NONE;

    public Message() {
        this("", GV.MsgTypeEnum.NONE);
    }

    public Message(String msgContent, GV.MsgTypeEnum msgType, GV.MsgTargetTypeEnum msgTargetType) {
        this(msgContent, msgType);
        this.msgTargetType = msgTargetType;
    }

    public Message(String msgContent, GV.MsgTypeEnum msgType) {
        this.msgContent = msgContent;
        this.msgType = msgType;
        switch (msgType) {
            case INIT:
                // p B-multicasts <msg, mid, RTS, mPID> to g
                this.senderPID = GV.MY_PID;
                GV.counter = GV.counter + 1;
                this.msgID = Integer.parseInt("1" + GV.MY_PID + "00") + GV.counter;
                GV.propSeqPriorNum = GV.propSeqPriorNum + 1;
                this.propSeqPrior = GV.propSeqPriorNum;
                break;

            case REPLY:
                break;

            case DELIVER:
                // Set pPID, isDeliverable, propSeqPrior
                this.isDeliverable = 1;
                break;

            case HEART:
                this.senderPID = GV.MY_PID;
                break;

            case ALIVE:
                this.senderPID = GV.MY_PID;
                break;

            // None
            default:break;
        }

        if (msgType.equals("REPLY")) {
            // Send to rPID
            // Reply <msg, REC, pSeqPriorNum, mPID> to sPID
            GV.propSeqPriorNum = Math.max(GV.agreeSeqPriorNum, GV.propSeqPriorNum) + 1;
            GV.agreeSeqPriorNum = GV.propSeqPriorNum;
            this.propSeqPrior = GV.propSeqPriorNum;
            this.senderPID = GV.MY_PID;

        }

        Log.d(TAG, "Build MSG: " + this.toString());
    }

    public Message(GV.MsgTypeEnum msgType, int targetPID) {
        this("", msgType);
        switch (msgType) {
            case HEART:
                this.msgContent = "---";
                this.senderPID = GV.MY_PID;
                if (targetPID != GV.GROUP_PID)
                    this.msgTargetType = GV.MsgTargetTypeEnum.PID;
                else
                    this.msgTargetType = GV.MsgTargetTypeEnum.GROUP;
                break;

            case ALIVE:
                this.msgContent = "^o^";
                this.senderPID = GV.MY_PID;
                this.msgTargetType = GV.MsgTargetTypeEnum.PID;
                this.fromPID = targetPID;
                break;

            default:break;
        }
    }

    public String getMsgContent() {return this.msgContent;}
    public GV.MsgTypeEnum getMsgType() {return this.msgType;}
    public int getMsgID() {return this.msgID;}
    public int getSenderPID() {return this.senderPID;}
    public int getFromPID() {return this.fromPID;}
    public int getPropPID() {return this.propPID;}
    public int getPropSeqPrior() {return this.propSeqPrior;}
    public int getIsDeliverable() {return this.isDeliverable;}
    public GV.MsgTargetTypeEnum getMsgTargetType() {return this.msgTargetType;}

    public void setMsgType(GV.MsgTypeEnum msgType) {this.msgType = msgType;}
    public void setSenderPID(int senderPID) {this.senderPID = senderPID;}
    public void setFromPID(int fromPID) {this.fromPID = fromPID;}

    public void parseMsg(String s) {
        Log.d(TAG, "Parsing: " + s);
        String[] msgInfoList = s.split("::");
        this.senderPID = Integer.parseInt(msgInfoList[0]);
        this.msgID = Integer.parseInt(msgInfoList[1]);
        this.msgType = GV.MsgTypeEnum.valueOf(msgInfoList[2]);
        this.msgContent = msgInfoList[3];
        this.propPID = Integer.parseInt(msgInfoList[4]);
        this.propSeqPrior = Integer.parseInt(msgInfoList[5]);
        this.isDeliverable = Integer.parseInt(msgInfoList[6]);
    }

    public void parseMsgInfo(MessageInfo msgInfo, GV.MsgTypeEnum msgType) {
        this.senderPID = msgInfo.getSenderID();
        this.msgID = msgInfo.getMsgID();
        this.msgType = msgType;
        this.msgContent = msgInfo.getMsgContent();
        this.propPID = msgInfo.getPropPID();
        this.propSeqPrior = msgInfo.getPropSeqPrior();
        this.isDeliverable = msgInfo.getIsDeliverable();
    }


    public String toString() {
        // msgID::msgRecv::msgType::pPrior::sPID::status
        return this.senderPID + "::" + this.msgID + "::"
                + this.msgType.name() + "::" + this.msgContent + "::"
                + this.propPID + "::" + this.propSeqPrior + "::" + this.isDeliverable;
    }

//    public String rebuildMsgToSend() {
//        buildMsg(this.msgContent, this.msgType);
//        return this.constructMsgToSend();
//    }

}

