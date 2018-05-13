package edu.buffalo.cse.cse486586.simpledynamo;

import android.net.Uri;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GV {

    static Uri dbUri = null;
    static String MY_PORT = null;

    static Queue<NMessage> msgRecvQueue = new LinkedList<NMessage>();
    static Queue<NMessage> msgSendQueue = new LinkedList<NMessage>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>(0);
    static HashMap<String, String> resultAllMap = new HashMap<String, String>(0);

    // Stored info for failed node
    static Queue<NMessage> notifyPredNode = new LinkedList<NMessage>();
    static Queue<NMessage> notifySuccNode = new LinkedList<NMessage>();
    static Queue<NMessage> updateSendQueue = new LinkedList<NMessage>();
    static Queue<NMessage> updateRecvQueue = new LinkedList<NMessage>();

    static Lock queryLock = new ReentrantLock();
    static boolean needWaiting;

    // TODO HANDLE FAILUE: CHANGE TO FALSE
    static boolean deleteTable = false;
    static int dbRows = 0;

    static String lostPort = null;
    static boolean updatePred = false;
    static boolean updateSucc = false;
    static boolean updateCompleted = false;
}

