package edu.buffalo.cse.cse486586.simpledht;


import android.util.Log;

public class Message {

    enum TYPE {
        NONE,
        JOIN, NOTYFY,
        INSERT, DELETE,
        QUERY, RESULT,
    }

    private String senderPort = "";
    private TYPE msgType = TYPE.NONE;
    private String msgKey = "";
    private String msgVal = "";

    public Message() {
        this.senderPort = GV.MY_PORT;
    }



    public static Message parseMsg(String s) {
        Log.v("PARSE MSG", s);
        String[] msgInfo = s.split("::");
        Message msg = new Message();
        msg.senderPort = msgInfo[0];
        msg.msgType = TYPE.valueOf(msgInfo[1]);
        msg.msgKey = msgInfo[2];
        msg.msgVal = msgInfo[3];
        return msg;
    }

    public String toString() {
        // senderPort::msgType::msgKey::msgVal
        return this.senderPort + "::" + this.msgType.name() + "::" +
                this.msgKey + "::" + this.msgVal;
    }

}
