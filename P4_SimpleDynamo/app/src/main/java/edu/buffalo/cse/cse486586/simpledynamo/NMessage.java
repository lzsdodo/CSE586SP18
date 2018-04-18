package edu.buffalo.cse.cse486586.simpledynamo;


import android.util.Log;

public class NMessage {
    // Msg: "msgID::msgType::cmdPort::sndPort::tgtPort::msgBody"

    static int msgCounter = 0;

    enum TYPE {
        NONE,
        // Database
        INSERT_ONE,
        DELETE_ONE, DELETE_ALL,
        QUERY_ONE, QUERY_ALL, QUERY_COMLETED,
        RESULT_ONE, RESULT_ALL,
    }

    private String msgID = null;
    private TYPE msgType = TYPE.NONE;

    private String cmdPort = null; // command port
    private String sndPort = null; // sender port
    private String tgtPort = null; // target port

    // msgBody: "msgKey<>msgVal"
    private String msgBody = null;

    // msgBody: "Operation"
    private String msgKey = null;
    private String msgVal = null;

    public NMessage() {}

    public NMessage(TYPE msgType, String cmdPort, String tgtPort, String key, String value) {
        this();
        // this.msgID = Integer.parseInt("1" + sndPort + "000") + msgCounter++;
        this.msgType = msgType;
        this.cmdPort = cmdPort;
        // this.sndPort = null;
        this.tgtPort = tgtPort;
        this.msgKey = key;
        this.msgVal = value;
        this.msgBody = this.msgKey + "<>" + this.msgVal;
    }

    public NMessage(TYPE msgType, String cmdPort, String tgtPort, String msgBody) {
        this();
        // this.msgID = Integer.parseInt("1" + sndPort + "000") + msgCounter++;
        this.msgType = msgType;
        this.cmdPort = cmdPort;
        // this.sndPort = null;
        this.tgtPort = tgtPort;
        this.msgBody = msgBody;
        this.msgKey = null;
        this.msgVal = null;
    }

    public String getMsgID()    { return this.msgID; }
    public TYPE getMsgType()    { return this.msgType; }
    public String getCmdPort()  { return this.cmdPort; }
    public String getSndPort()  { return this.sndPort; }
    public String getTgtPort()  { return this.tgtPort; }
    public String getMsgBody () { return this.msgBody; }
    public String getMsgKey()   { return this.msgKey; }
    public String getMsgValue() { return this.msgVal; }

    public void setMsgID (String msgID)     { this.msgID = msgID; }
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

        msg.setMsgID(msgInfo[0]);
        msg.setMsgType(TYPE.valueOf(msgInfo[1]));
        msg.setCmdPort(msgInfo[2]);
        msg.setSndPort(msgInfo[3]);
        msg.setTgtPort(msgInfo[4]);
        msg.setMsgBody(msgInfo[5]);

        String[] kv = msg.getMsgBody().split("<>");
        if (kv.length == 2) {
            msg.setMsgKey(kv[0]);
            msg.setMsgVal(kv[1]);
        } else {
            msg.setMsgKey(kv[0]);
            msg.setMsgVal(null);
        }

        return msg;
    }

    public String toString() {
        return this.msgID + "::" + this.msgType.name() + "::" +
                this.cmdPort + "::" + this.sndPort + "::" + this.tgtPort + "::" +
                this.msgBody;
    }

}


