package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;

public class MSG implements Comparable<MSG> {

    // Msg: "msgId::msgType::cmdPort::sndPort::tgtPort::msgBody"

    enum TYPE {
        RESTART, IS_ALIVE, RECOVERY,            // NEW MSG ID
        SIGNAL,                                 // SAME AS RECV MSG ID
        UPDATE_INSERT, UPDATE_COMPLETED,        // SAME AS RECV MSG ID
        INSERT, DELETE, QUERY,                  // NEW MSG ID
        RESULT_ONE, RESULT_ALL,                 // SAME AS RECV MSG ID
        RESULT_ALL_FLAG, RESULT_ALL_COMLETED,   // SAME AS RECV MSG ID
        LOST_INSERT,                            // SAME AS RECV MSG ID
    }

    //static int msgCounter = 10000;

    private String msgId   = null;
    private TYPE   msgType = null;
    private String cmdPort = null; // command port
    private String sndPort = null; // sender port
    private String tgtPort = null; // target port
    private String msgBody = null; // msgBody: "msgKey<>msgVal"
    private String msgKey  = null;
    private String msgVal  = null;

    private MSG() {this.sndPort = GV.MY_PORT;}

    private MSG(boolean isAccurated) {
        this();
        if (isAccurated) {
            this.msgId = GV.MY_PORT.substring(2) + "-" + System.currentTimeMillis();
            //this.msgId = GV.MY_PORT.substring(2) + "-" + msgCounter++;
        } else {
            this.msgId = "ERROR MSG-ID";
        }
    }

    public MSG(TYPE msgType, String cmdPort, String tgtPort) {
        // FLAG MSG: Sender & Target & Type are all concerned
        //      RESULT_ALL_FLAG, RESULT_ALL_
        //      COMLETED, RESTART, RECOVERY, IS_ALIVE,
        this();
        this.cmdPort = cmdPort;
        this.tgtPort = tgtPort;
        this.setMsgVal(msgType);
    }

    public MSG(TYPE msgType, String cmdPort, String tgtPort, String key, String value) {
        this(true);
        this.msgType = msgType;
        this.cmdPort = cmdPort;
        this.tgtPort = tgtPort;
        this.msgKey = key;
        this.msgVal = value;
        this.msgBody = this.msgKey + "<>" + this.msgVal;
    }

    public MSG(TYPE msgType, String tgtPort, String key, String value) {
        this(true);
        this.msgType = msgType;
        this.cmdPort = GV.MY_PORT;
        this.tgtPort = tgtPort;
        this.msgKey = key;
        this.msgVal = value;
        this.msgBody = this.msgKey + "<>" + this.msgVal;
    }

    public MSG copy() {
        MSG newMsg = new MSG();
        newMsg.setMsgID(this.msgId);
        newMsg.setMsgType(this.msgType);
        newMsg.setCmdPort(this.cmdPort);
        newMsg.setSndPort(this.sndPort);
        newMsg.setTgtPort(this.tgtPort);
        newMsg.setMsgBody(this.msgBody);
        newMsg.setMsgKey(this.msgKey);
        newMsg.setMsgVal(this.msgVal);
        return newMsg;
    }

    static MSG parseMsg(String s) {
        MSG msg = new MSG();
        String[] msgInfo = s.split("::");

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
            msg.setMsgVal("");
        }
        return msg;
    }

    public String getMsgID()    { return this.msgId; }
    public TYPE getMsgType()    { return this.msgType; }
    public String getCmdPort()  { return this.cmdPort; }
    public String getSndPort()  { return this.sndPort; }
    public String getTgtPort()  { return this.tgtPort; }
    public String getMsgBody () { return this.msgBody; }
    public String getMsgKey()   { return this.msgKey; }
    public String getMsgVal()   { return this.msgVal; }

    public void setMsgID (String msgId)     { this.msgId = msgId; }
    public void setMsgType (TYPE msgType)   { this.msgType = msgType; }
    public void setCmdPort (String cmdPort) { this.cmdPort = cmdPort; }
    public void setSndPort (String sndPort) { this.sndPort = sndPort; }
    public void setTgtPort (String tgtPort) { this.tgtPort = tgtPort; }
    public void setMsgBody (String msgBody) { this.msgBody = msgBody; }
    public void setMsgKey (String msgKey)   { this.msgKey = msgKey; }
    public void setMsgVal (String msgVal) { this.msgVal = msgVal; }

    private void setMsgVal(TYPE type) {
        switch (type) {
            case RESULT_ALL_FLAG:
            case RESULT_ALL_COMLETED:
                this.msgBody = "_$,$_"; break;
            case RESTART:
            case IS_ALIVE:
            case RECOVERY:
            case UPDATE_COMPLETED:
                this.msgBody = "_LoL_"; break;
            case SIGNAL:
                this.msgVal = "_O.O_"; break;
            default:
                break;
        }
    }

    public String toString() {
        // this.msgId + "::" +
        return this.msgId + "::" + this.msgType.name() + "::" +
                this.cmdPort + "::" + this.sndPort + "::" + this.tgtPort + "::" +
                this.msgBody;
    }

    @Override
    public int compareTo (MSG another) {
        int delta = this.getPriorOfType(this) - this.getPriorOfType(another);
        if (delta != 0) {
            return delta;
        } else {
            int lhsMid = Integer.parseInt(this.msgId.substring(3));
            int rhsMid = Integer.parseInt(another.getMsgID().substring(3));
            return lhsMid - rhsMid;
        }
    }

    private int getPriorOfType(MSG msg) {
        switch (msg.getMsgType()) {
            case RESTART:
            case IS_ALIVE:
            case RECOVERY:
                return 1;
            case SIGNAL:
                return 2;
            case UPDATE_INSERT:
            case UPDATE_COMPLETED:
                return 3;
            case QUERY:
            case INSERT:
            case DELETE:
                return 4;
            case RESULT_ONE:
            case RESULT_ALL:
            case RESULT_ALL_FLAG:
            case RESULT_ALL_COMLETED:
                return 5;
            case LOST_INSERT:
                return 6;
            default:
                return 10;
        }
    }

}


