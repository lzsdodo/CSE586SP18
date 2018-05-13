package edu.buffalo.cse.cse486586.simpledynamo;

import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GV {

    static Uri dbUri = null;
    static String MY_PORT = null;
    static String MY_ID = null;
    static String MY_PORT_INFO = null;
    static int msgCounter = 0;

    static Queue<NMessage> msgRecvQ = new LinkedList<NMessage>();
    static Queue<NMessage> msgSendQ = new LinkedList<NMessage>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();

    // Stored info for failed node
    static ArrayList<NMessage> notifyPredMsgL = new ArrayList<NMessage>(0);
    static ArrayList<NMessage> notifySuccMsgL = new ArrayList<NMessage>(0);
    static Queue<NMessage> msgUpdateSendQ = new LinkedList<NMessage>();
    static Queue<NMessage> msgUpdateRecvQ = new LinkedList<NMessage>();

    static Queue<NMessage> signalSendQueue = new LinkedList<NMessage>();
    static HashMap<String, NMessage> signalMsgMap = new HashMap<String, NMessage>();
    static HashMap<String, Integer> signalTimeMap = new HashMap<String, Integer>();

    static Lock queryLock = new ReentrantLock();
    static boolean needWaiting;

    static ArrayList<String> nodeIdList = new ArrayList<String>(0);
    static HashMap<String, String> idPortMap = new HashMap<String, String>(0);

    // TODO HANDLE FAILUE: CHANGE TO FALSE
    static int dbRows = 0;
    static boolean deleteTable = true;

    static String lostPort = null;

}

