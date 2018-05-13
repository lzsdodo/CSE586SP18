package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;

public class NMessage {
    // Msg: "msgID::msgType::cmdPort::sndPort::tgtPort::msgBody"
    // Msg: "msgType::cmdPort::sndPort::tgtPort::msgBody"

    enum TYPE {
        NONE,
        INSERT, DELETE, QUERY,
        RESULT_ONE, RESULT_ALL, RESULT_ALL_COMLETED,
        LOST, RECOVERY,
        UPDATE_INSERT, UPDATE_DELETE, UPDATE_COMPLETED,
    }

    // static int msgCounter = 0;
    // private String msgID = null;
    private TYPE   msgType = TYPE.NONE;
    private String cmdPort = null; // command port
    private String sndPort = null; // sender port
    private String tgtPort = null; // target port
    private String msgBody = null; // msgBody: "msgKey<>msgVal"
    private String msgKey  = null;
    private String msgVal  = null;

    public NMessage() {
        // int mid = Integer.parseInt(this.sndPort.substring(2) + "1000") + msgCounter++;
        // this.msgID = mid + "";
        this.sndPort = GV.MY_PORT;
    }

    public NMessage(TYPE msgType, String cmdPort, String tgtPort, String key, String value) {
        this();
        this.msgType = msgType;
        this.cmdPort = cmdPort;
        this.tgtPort = tgtPort;
        this.msgKey = key;
        this.msgVal = value;
        this.msgBody = this.msgKey + "<>" + this.msgVal;
    }

    // public String getMsgID()    { return this.msgID; }
    public TYPE getMsgType()    { return this.msgType; }
    public String getCmdPort()  { return this.cmdPort; }
    public String getSndPort()  { return this.sndPort; }
    public String getTgtPort()  { return this.tgtPort; }
    public String getMsgBody () { return this.msgBody; }
    public String getMsgKey()   { return this.msgKey; }
    public String getMsgVal() { return this.msgVal; }

    // public void setMsgID (String msgID)     { this.msgID = msgID; }
    public void setMsgType (TYPE msgType)   { this.msgType = msgType; }
    public void setCmdPort (String cmdPort) { this.cmdPort = cmdPort; }
    public void setSndPort (String sndPort) { this.sndPort = sndPort; }
    public void setTgtPort (String tgtPort) { this.tgtPort = tgtPort; }
    public void setMsgBody (String msgBody) { this.msgBody = msgBody; }
    public void setMsgKey (String msgKey)   { this.msgKey = msgKey; }
    public void setMsgVal (String msgVal) { this.msgVal = msgVal; }

    public static NMessage parseMsg(String s) {
        Log.v("PARSE MSG", s);
        String[] msgInfo = s.split("::");
        NMessage msg = new NMessage();

        // msg.setMsgID(msgInfo[0]);
        msg.setMsgType(TYPE.valueOf(msgInfo[0]));
        msg.setCmdPort(msgInfo[1]);
        msg.setSndPort(msgInfo[2]);
        msg.setTgtPort(msgInfo[3]);
        msg.setMsgBody(msgInfo[4]);

        String[] kv = msg.getMsgBody().split("<>");
        if (kv.length == 2) {
            msg.setMsgKey(kv[0]);
            msg.setMsgVal(kv[1]);
        } else {
            msg.setMsgKey(kv[0]);
            msg.setMsgVal("");
        }

        return msg;
    }

    public String toString() {
        // this.msgID + "::" +
        return this.msgType.name() + "::" +
                this.cmdPort + "::" + this.sndPort + "::" + this.tgtPort + "::" +
                this.msgBody;
    }

}


