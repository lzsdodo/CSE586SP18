package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

class GV {

    static final String URI = "edu.buffalo.cse.cse486586.simpledht.provider";
    static TextView uiTV;
    static Uri dbUri;
    static ContentResolver dbCR;

    static final int SERVER_PORT = 10000;
    static final String REMOTE_ADDR = "10.0.2.2";
    static final ArrayList<String> REMOTE_PORTS = new ArrayList<String>(
            Arrays.asList("11108", "11112", "11116", "11120", "11124"));

    // Avd Order: 4 -> 1 -> 0 -> 2 -> 3
    static final HashMap<String, String> NODE_ID_MAP = createHashMap();
    static HashMap<String, String> createHashMap()
    {
        HashMap<String,String> hashMap = new HashMap<String,String>();
        hashMap.put("5554", "33d6357cfaaf0f72991b0ecd8c56da066613c089");
        hashMap.put("5556", "208f7f72b198dadd244e61801abe1ec3a4857bc9");
        hashMap.put("5558", "abf0fd8db03e5ecb199a9b82929e9db79b909643");
        hashMap.put("5560", "c25ddd596aa7c81fa12378fa725f706d54325d12");
        hashMap.put("5562", "177ccecaec32c54b82d5aaafc18a2dadb753e3b1");
        return hashMap;
    }

    static String MY_PORT = ""; // My Port
    static String MY_NID = "";

    static Queue<Message> msgRecvQueue = new LinkedList<Message>();
    static Queue<Message> msgSendQueue = new LinkedList<Message>();
    static Boolean isEmptyMRQ = true;
    static Boolean isEmptyMSQ = true;
}
