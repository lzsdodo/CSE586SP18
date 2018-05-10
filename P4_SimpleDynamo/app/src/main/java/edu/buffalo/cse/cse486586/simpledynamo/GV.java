package edu.buffalo.cse.cse486586.simpledynamo;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class GV {

    static final String URI = "edu.buffalo.cse.cse486586.simpledynamo.provider";

    static Uri dbUri = null;

    static final Object lockOne = new Object();
    static final Object lockAll = new Object();

    static final int SERVER_PORT = 10000;
    static final String REMOTE_ADDR = "10.0.2.2";
    static final ArrayList<String> PORTS = new ArrayList<String>(
            Arrays.asList("5554", "5556", "5558", "5560", "5562"));
    static String MY_PORT = null; // My Port

    static Queue<NMessage> msgRecvQueue = new LinkedList<NMessage>();
    static Queue<NMessage> msgSendQueue = new LinkedList<NMessage>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();

}
