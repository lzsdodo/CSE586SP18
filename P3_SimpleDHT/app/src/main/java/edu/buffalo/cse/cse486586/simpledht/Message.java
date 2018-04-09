package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/*
 * Reference:
 *  Dev Docs:
 *      HashMap: https://docs.oracle.com/javase/9/docs/api/java/util/HashMap.html
 *      Entry: https://docs.oracle.com/javase/9/docs/api/java/util/Map.Entry.html
 *      Iterator: https://docs.oracle.com/javase/9/docs/api/java/util/Iterator.html
 */

public class Message {

    enum TYPE {
        NONE,
        // Chord
        JOIN, NOTYFY,
        // Database
        INSERT_ONE,
        DELETE_ONE, DELETE_ALL,
        QUERY_ONE, QUERY_ALL,
        RESULT, RESULT_ALL,
    }

    private String commandPort = "";
    private String senderPort = "";
    private TYPE msgType = TYPE.NONE;
    private String msgKVPairs = "";


    public Message() {
        this.commandPort = GV.MY_PORT;
        this.senderPort = GV.MY_PORT;
    }

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

    public static Message parseMsg(String s) {
        Log.v("PARSE MSG", s);
        String[] msgInfo = s.split("::");
        Message msg = new Message();
        msg.commandPort = msgInfo[0];
        msg.senderPort = msgInfo[1];
        msg.msgType = TYPE.valueOf(msgInfo[2]);
        msg.msgKVPairs = msgInfo[3];
        return msg;
    }

    public String toString() {
        // senderPort::msgType::msgKey::msgVal
        return this.commandPort + "::" + this.senderPort + "::" +
                this.msgType.name() + "::" + this.msgKVPairs;
    }

}
