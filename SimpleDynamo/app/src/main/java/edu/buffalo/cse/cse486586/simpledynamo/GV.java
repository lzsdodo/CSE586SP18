package edu.buffalo.cse.cse486586.simpledynamo;

import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GV {

    // Basic Info
    static Uri dbUri = null;

    static String PRED_ID = null;
    static String PRED_PORT = null;
    static String MY_ID = null;
    static String MY_PORT = null;
    static String SUCC_ID = null;
    static String SUCC_PORT = null;
    static String[] REPLICA_PORTS = null;

    static ArrayList<String> NODE_ID_LIST = new ArrayList<String>(0);
    static HashMap<String, String> ID_PORT_MAP = new HashMap<String, String>(0);

    // Tcp Recv/Send Queue
    static PriorityBlockingQueue<MSG> msgRecvQ = new PriorityBlockingQueue<MSG>();
    static PriorityBlockingQueue<MSG> msgSendQ = new PriorityBlockingQueue<MSG>();
    static Queue<MSG> resendQ = new LinkedBlockingQueue<MSG>();


    static Queue<MSG> msgUpdateSendQ = new LinkedBlockingQueue<MSG>();
    static Queue<MSG> msgUpdateRecvQ = new LinkedBlockingQueue<MSG>();
    static Queue<MSG> msgSignalSendQ = new LinkedBlockingQueue<MSG>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();
    static ArrayList<String> queryAllReturnPort = new ArrayList<String>(0);
    static Lock queryLock = new ReentrantLock();
    static boolean needWaiting;

    // Stored info for failed node
    static HashMap<String, LinkedBlockingQueue<MSG>> backupMsgQMap =
            new HashMap<String, LinkedBlockingQueue<MSG>>();


    // Msg that wait signal to confirm
    static Queue<String> waitMsgIdQueue = new LinkedBlockingQueue<String>();
    static Set<String> waitMsgIdSet = new HashSet<String>();
    static HashMap<String, MSG> waitMsgMap = new HashMap<String, MSG>();
    static HashMap<String, Integer> waitMsgTimeMap = new HashMap<String, Integer>();



    static boolean deleteTable = false;

}

