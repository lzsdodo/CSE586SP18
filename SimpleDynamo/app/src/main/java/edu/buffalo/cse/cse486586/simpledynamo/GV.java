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
    static int msgCounter = 0;

    static Queue<NMessage> msgRecvQueue = new LinkedList<NMessage>();
    static Queue<NMessage> msgSendQueue = new LinkedList<NMessage>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();

    // Stored info for failed node
    static ArrayList<NMessage> notifyPredNode = new ArrayList<NMessage>(0);
    static ArrayList<NMessage> notifySuccNode = new ArrayList<NMessage>(0);
    static Queue<NMessage> updateSendQueue = new LinkedList<NMessage>();
    static Queue<NMessage> updateRecvQueue = new LinkedList<NMessage>();

    static Lock queryLock = new ReentrantLock();
    static boolean needWaiting;

    // TODO HANDLE FAILUE: CHANGE TO FALSE
    static int dbRows = 0;
    static boolean deleteTable = false;

    static String lostPort = null;
//    static boolean updatePred = false;
//    static boolean updateSucc = false;
//    static boolean updateCompleted = false;

    static String waitMsgId = null;
    static int serverStatus = 0;
}

