package edu.buffalo.cse.cse486586.simpledynamo;

import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
// PriorityBlockingQueue <impl Comparable>

public class GV {

    // Basic Info
    static Uri dbUri = null;

    static String PRED_ID = null;
    static String PRED_PORT = null;
    static String MY_ID = null;
    static String MY_PORT = null;
    static String SUCC_ID = null;
    static String SUCC_PORT = null;
    static ArrayList<String> REPLICA_PORTS = new ArrayList<String>();

    static ArrayList<String> NODE_ID_LIST = new ArrayList<String>(0);
    static HashMap<String, String> ID_PORT_MAP = new HashMap<String, String>(0);

    // Basic Queue
    static Queue<NMessage> msgRecvQ = new LinkedBlockingQueue<NMessage>();
    static Queue<NMessage> msgSendQ = new LinkedBlockingQueue<NMessage>();

    static Queue<NMessage> msgUpdateSendQ = new LinkedBlockingQueue<NMessage>();
    static Queue<NMessage> msgUpdateRecvQ = new LinkedBlockingQueue<NMessage>();
    static Queue<NMessage> msgSignalSendQ = new LinkedBlockingQueue<NMessage>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();
    static ArrayList<String> queryAllReturnPort = new ArrayList<String>(0);
    static Lock queryLock = new ReentrantLock();
    static boolean needWaiting;

    // Stored info for failed node
    static HashMap<String, LinkedBlockingQueue<NMessage>> backupMsgQMap =
            new HashMap<String, LinkedBlockingQueue<NMessage>>();


    // Msg that wait signal to confirm
    static Queue<String> waitMsgIdQueue = new LinkedBlockingQueue<String>();
    static Set<String> waitMsgIdSet = new HashSet<String>();
    static HashMap<String, NMessage> waitMsgMap = new HashMap<String, NMessage>();
    static HashMap<String, Integer> waitMsgTimeMap = new HashMap<String, Integer>();

    static Queue<NMessage> resendQueue = new LinkedBlockingQueue<NMessage>();

    static boolean deleteTable = false;

}

