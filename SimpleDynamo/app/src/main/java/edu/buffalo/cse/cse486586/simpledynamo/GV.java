package edu.buffalo.cse.cse486586.simpledynamo;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class GV {

    static Uri dbUri = null;
    static String MY_PORT = null;

    static final Object lockOne = new Object();
    static final Object lockAll = new Object();

    static Queue<NMessage> msgRecvQueue = new LinkedList<NMessage>();
    static Queue<NMessage> msgSendQueue = new LinkedList<NMessage>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();

    // Stored info for failed node
    static ArrayList<NMessage> notifyPredNode = new ArrayList<NMessage>(0);
    static ArrayList<NMessage> notifySuccNode = new ArrayList<NMessage>(0);


    // TODO HANDLE FAILUE: CHANGE TO FALSE
    static boolean deleteTable = true;

}

