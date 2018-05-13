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

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();

    // Stored info for failed node
    static Queue<NMessage> notifyPredNode = new LinkedList<NMessage>();
    static Queue<NMessage> notifySuccNode = new LinkedList<NMessage>();
    static Queue<NMessage> updateSendQueue = new LinkedList<NMessage>();
    static Queue<NMessage> updateRecvQueue = new LinkedList<NMessage>();

    static Lock queryLock = new ReentrantLock();
    static boolean needWaiting;

    // TODO HANDLE FAILUE: CHANGE TO FALSE
    static int dbRows = 0;
    static boolean deleteTable = true;

    static String lostPort = null;
    static int updateTimes = 0;
}

