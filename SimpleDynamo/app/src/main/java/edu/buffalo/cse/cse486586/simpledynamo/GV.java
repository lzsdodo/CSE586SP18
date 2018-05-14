package edu.buffalo.cse.cse486586.simpledynamo;

import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
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
    static String MY_PORT_INFO = null;
    static int msgCounter = 0;

    static ArrayList<String> nodeIdList = new ArrayList<String>(0);
    static HashMap<String, String> idPortMap = new HashMap<String, String>(0);

    // Basic Queue
    static Queue<NMessage> msgRecvQ = new LinkedList<NMessage>();
    static Queue<NMessage> msgSendQ = new LinkedList<NMessage>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();
    static ArrayList<String> queryAllReturnPort = new ArrayList<String>(0);

    static Lock queryLock = new ReentrantLock();
    static boolean needWaiting;

    // Stored info for failed node
    static ArrayList<NMessage> notifyPredMsgL = new ArrayList<NMessage>(0);
    static ArrayList<NMessage> notifySuccMsgL = new ArrayList<NMessage>(0);

    static Queue<NMessage> msgUpdateSendQ = new LinkedList<NMessage>();
    static Queue<NMessage> msgUpdateRecvQ = new LinkedList<NMessage>();

    // Signal To Send
    static Queue<NMessage> signalSendQ = new LinkedList<NMessage>();
    // Msg that wait signal to confirm
    static Queue<NMessage> waitMsgQueue = new LinkedList<NMessage>();
    static Set<String> waitMsgIdSet = new HashSet<String>();
    static HashMap<String, Integer> waitTimeMap = new HashMap<String, Integer>();

    static String lostPort = null;


    // TODO HANDLE FAILUE: CHANGE TO FALSE
    static boolean deleteTable = true;


}

