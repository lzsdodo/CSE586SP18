package edu.buffalo.cse.cse486586.groupmessenger2;

import java.util.LinkedList;
import java.util.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/*
 * Reference:
 *  Enum: https://docs.oracle.com/javase/9/docs/api/java/lang/Enum.html
 *  Queue: https://docs.oracle.com/javase/9/docs/api/java/util/Queue.html
 *  LinkedList: https://docs.oracle.com/javase/9/docs/api/java/util/LinkedList.html
 *  HashMap: https://docs.oracle.com/javase/9/docs/api/java/util/HashMap.html
 *  Map: https://docs.oracle.com/javase/9/docs/api/java/util/Map.html
 */

// Global Variable
public class GV {

    static final String URI = "edu.buffalo.cse.cse486586.groupmessenger2.provider";

    static final String REMOTE_ADDR = "10.0.2.2";
    static final int SERVER_PORT = 10000;
    static final ArrayList<String> REMOTE_PORTS = new ArrayList<String>(
            Arrays.asList("11108", "11112", "11116", "11120", "11124"));
    static String MY_PORT = ""; // My Port

    static final int GROUP_PID = 999; // My Process ID
    static int MY_PID = -1; // My Process ID

    static final int RETRY_TIME = 5;
    static int devConnNum = 5;
    static ArrayList<Boolean> devStatus = new ArrayList<Boolean>(
            Arrays.asList(true, true, true, true, true));
    static ArrayList<Integer> devDisconnTimes = new ArrayList<Integer>(
            Arrays.asList(0, 0, 0, 0, 0));
    static ArrayList<Integer> devNotAliveTimes = new ArrayList<Integer>(
            Arrays.asList(0, 0, 0, 0, 0));

    // Msg Info
    static int counter = 0; // Message sent by this device
    static int maxPropPrior = 0;
    static int agreePrior = 0;

    // Msg Queue
    // Insert: q.offer(e); Head: q.element(); Remove: q.poll();
    // Store all msg that receive from socket and execute by QueueTask
    static Queue<Message> msgRecvQueue = new LinkedList<Message>();
    static Queue<Message> msgSendQueue = new LinkedList<Message>();

    static Boolean isEmptyMRQ = true; // Message Recv Queue
    static Boolean isEmptyMSQ = true; // Message Send Queue

    // Store new msgID, which also hint the origin receive order
    static ArrayList msgRecvOrderList = new ArrayList();
    static ArrayList<Integer> msgWaitList = new ArrayList<Integer>();
    static HashMap<Integer, ArrayList<ArrayList<Integer>>> msgWaitProirMap =
            new HashMap<Integer, ArrayList<ArrayList<Integer>>>();

    // Structure: <msgID, MessageInfo>
    // MessageInfo Structure: <msg, msgID, senderPID, agrProir, propPID, dilverable>
    static HashMap<Integer, MessageInfo> msgInfoMap = new HashMap<Integer, MessageInfo>();
    static ArrayList<MessageInfo> msgTOList = new ArrayList<MessageInfo>();
    static int msgNum = 0;
    static int msgTONum = 0;
    static int TONum = 0;
}