package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

/*
 * Reference:
 *  Dev Docs:
 *      HashMap: https://docs.oracle.com/javase/9/docs/api/java/util/HashMap.html
 *      Entry: https://docs.oracle.com/javase/9/docs/api/java/util/Map.Entry.html
 *      Iterator: https://docs.oracle.com/javase/9/docs/api/java/util/Iterator.html
 */

public class NewMessage {

    enum TYPE {
        NONE,
        // Chord
        JOIN, NOTIFY,
        // Database
        INSERT_ONE,
        DELETE_ONE, DELETE_ALL,
        QUERY_ONE, QUERY_ALL, QUERY_COMLETED,
        RESULT_ONE, RESULT_ALL,
    }

    private String cmdPort = null; // command port
    private String tgtPort = null; // target port
    private TYPE msgType = TYPE.NONE;
    private String msgBody = null;
    private String msgKey = null;
    private String msgValue = null;

    public NewMessage() {}

    public NewMessage(TYPE msgType, String cmdPort, String tgtPort, String key, String value) {
        this();
        this.msgType = msgType;
        this.cmdPort = cmdPort;
        this.tgtPort = tgtPort;
        this.msgKey = key;
        this.msgValue = value;
        this.msgBody = this.msgKey + "<>" + this.msgValue;
    }

    public NewMessage(TYPE msgType, String cmdPort, String tgtPort, String msgBody) {
        this();
        this.msgType = msgType;
        this.cmdPort = cmdPort;
        this.tgtPort = tgtPort;
        this.msgBody = msgBody;
        this.msgKey = null;
        this.msgValue = null;
    }

    public TYPE getMsgType() {return this.msgType;}
    public String getCmdPort() {return this.cmdPort;}
    public String getTgtPort() {return this.tgtPort;}
    public String getMsgBody () {return this.msgBody;}
    public String getMsgKey() {return this.msgKey;}
    public String getMsgValue() {return this.msgValue;}

    public void setCmdPort (String cmdPort) {this.cmdPort = cmdPort;}
    public void setTgtPort (String tgtPort) {this.tgtPort = tgtPort;}
    public void setMsgType (TYPE msgType) {this.msgType = msgType;}
    public void setMsgBody (String msgBody) {this.msgBody = msgBody;}
    public void setMsgKey (String msgKey) {this.msgKey = msgKey;}
    public void setMsgValue (String msgValue) {this.msgValue = msgValue;}

    public static NewMessage parseMsg(String s) {
        Log.v("PARSE MSG", s);
        String[] msgInfo = s.split("::");
        NewMessage msg = new NewMessage();

        msg.setCmdPort(msgInfo[0]);
        msg.setTgtPort(msgInfo[1]);
        msg.setMsgType(TYPE.valueOf(msgInfo[2]));
        msg.setMsgBody(msgInfo[3]);

        String[] kvPairs = msg.getMsgBody().split("<>");
        if (kvPairs.length == 2) {
            msg.setMsgKey(kvPairs[0]);
            msg.setMsgValue(kvPairs[1]);
        } else {
            msg.setMsgKey(kvPairs[0]);
            msg.setMsgValue(null);
        }

        return msg;
    }

    public String toString() {
        // senderPort::msgType::msgKey::msgVal
        return this.cmdPort + "::" + this.tgtPort + "::" +
                this.msgType.name() + "::" + this.msgBody;
    }


    /*
    // Don't optimize too early!
    // send back result one by one first

    public static String makeStrKVPairs(HashMap<String, String> kvPairsMap) {
        // key1<>val1,key2<>val2,key3<>val3
        String strKVPairs = "";
        for (Map.Entry<String, String> entry: kvPairsMap.entrySet()) {
            strKVPairs += entry.getKey() + "<>" + entry.getValue() + ",";
        }
        return strKVPairs.substring(0, -1);
    }

    public static HashMap<String, String> parseStrKVPairs(String strKVPairs) {
        HashMap<String, String> kvPairsMap = new HashMap<String, String>();
        for (String kvPair: strKVPairs.split(",")) {
            String[] parts = kvPair.split("<>");
            kvPairsMap.put(parts[0], parts[1]);
        }
        return kvPairsMap;
    }
     */

}
