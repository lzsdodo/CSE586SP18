package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;

import java.util.Comparator;

public class NMessage implements Comparator<NMessage> {
    // Msg: "msgID::msgType::cmdPort::sndPort::tgtPort::msgBody"

    enum TYPE {
        INSERT, DELETE, QUERY,                  // NEW MSG ID
        RESTART, IS_ALIVE, RECOVERY,

        SIGNAL, RESULT_ONE, RESULT_ALL,         // SAME AS RECV MSG ID
        RESULT_ALL_FLAG, RESULT_ALL_COMLETED,
        LOST_INSERT, UPDATE_INSERT,
        UPDATE_DELETE, UPDATE_COMPLETED,
    }

    static int msgCounter = 10000;

    private String msgID = null;
    private TYPE   msgType = null;
    private String cmdPort = null; // command port
    private String sndPort = null; // sender port
    private String tgtPort = null; // target port
    private String msgBody = null; // msgBody: "msgKey<>msgVal"
    private String msgKey  = null;
    private String msgVal  = null;

    public NMessage() {}

    public NMessage(boolean isAccurated) {
        this();
        this.sndPort = GV.MY_PORT;
        if (isAccurated) {
            //this.msgID = GV.MY_PORT.substring(2) + "-" + System.currentTimeMillis();
            this.msgID = GV.MY_PORT.substring(2) + "-" + msgCounter++;
        } else {
            this.msgID = "ERROR MSG-ID";
        }
    }

    public NMessage(TYPE msgType, String tgtPort, String key, String value) {
        this(true);
        this.msgType = msgType;
        this.cmdPort = GV.MY_PORT;
        this.tgtPort = tgtPort;
        this.msgKey = key;
        this.msgVal = value;
        this.msgBody = this.msgKey + "<>" + this.msgVal;
    }

    public NMessage(TYPE msgType, String[] strings) {
        this(false);
        this.msgType = msgType;
        this.msgID = strings[0];
        this.cmdPort = strings[1];
        this.tgtPort = strings[2];
        this.msgKey = strings[3];
        this.msgBody = this.msgKey + "<>" + this.msgVal;
    }

    public NMessage(TYPE msgType, String tgtPort) {
        // FLAG MSG: Sender & Target & Type are all concerned
        //      RESULT_ALL_FLAG, RESULT_ALL_
        //      COMLETED, RESTART, RECOVERY, IS_ALIVE,
        this();
        this.sndPort = this.cmdPort = GV.MY_PORT;
        this.tgtPort = tgtPort;
        this.setMsgVal(msgType);
    }

    public String getMsgID()    { return this.msgID; }
    public TYPE getMsgType()    { return this.msgType; }
    public String getCmdPort()  { return this.cmdPort; }
    public String getSndPort()  { return this.sndPort; }
    public String getTgtPort()  { return this.tgtPort; }
    public String getMsgBody () { return this.msgBody; }
    public String getMsgKey()   { return this.msgKey; }
    public String getMsgVal()   { return this.msgVal; }

    public void setMsgID (String msgID)     { this.msgID = msgID; }
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

    public NMessage copy() {
        NMessage newMsg = new NMessage();
        newMsg.setMsgID(this.msgID);
        newMsg.setMsgType(this.msgType);
        newMsg.setCmdPort(this.cmdPort);
        newMsg.setSndPort(this.sndPort);
        newMsg.setTgtPort(this.tgtPort);
        newMsg.setMsgBody(this.msgBody);
        newMsg.setMsgKey(this.msgKey);
        newMsg.setMsgVal(this.msgVal);
        return newMsg;
    }

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
            msg.setMsgVal("");
        }

        return msg;
    }

    public String toString() {
        // this.msgID + "::" +
        return this.msgID + "::" + this.msgType.name() + "::" +
                this.cmdPort + "::" + this.sndPort + "::" + this.tgtPort + "::" +
                this.msgBody;
    }

    @Override
    public int compare (NMessage lhs, NMessage rhs) {

        String lhsPort = lhs.getMsgID().substring(2);
        String rhsPort = lhs.getMsgID().substring(2);

        if (lhsPort.equals(rhsPort)) {
            int lhsMid = Integer.parseInt(lhs.getMsgID().substring(2));
            int rhsMid = Integer.parseInt(rhs.getMsgID().substring(2));
            return lhsMid - rhsMid;
        }

        return 0;
    }

}


