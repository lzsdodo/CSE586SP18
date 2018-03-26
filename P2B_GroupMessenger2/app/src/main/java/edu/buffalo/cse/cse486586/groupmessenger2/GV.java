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
    // Internet Info
    static final String REMOTE_ADDR = "10.0.2.2";
    static final int SERVER_PORT = 10000;
    static String MY_PORT = ""; // My Port
    static int MY_PID = -1; // My Process ID
    static int GROUP_PID = 999; // My Process ID
    static final ArrayList<String> REMOTE_PORTS = new ArrayList<String>(
            Arrays.asList("11108", "11112", "11116", "11120", "11124"));
    static final int RETRY_TIME = 5;

    // Time Info
    static ArrayList<Integer> clockVector = new ArrayList<Integer>(
            Arrays.asList(0, 0, 0, 0, 0));
    static ArrayList<Integer> seqVector = new ArrayList<Integer>(
            Arrays.asList(0, 0, 0, 0, 0));

    // Device Info
    static int devConnNum = 5;
    static ArrayList<Boolean> devStatus = new ArrayList<Boolean>(
            Arrays.asList(true, true, true, true, true));
    static ArrayList<Integer> devDisconnTimes = new ArrayList<Integer>(
            Arrays.asList(0, 0, 0, 0, 0));
    static ArrayList<Integer> devNotAliveTimes = new ArrayList<Integer>(
            Arrays.asList(0, 0, 0, 0, 0));

    // Msg Info
    static int counter = 0; // Message first sent by this device

    enum MsgTypeEnum {
        NONE,
        INIT, REPLY, DELIVER,
        HEART, ALIVE
    }

    enum MsgTargetTypeEnum {
        NONE, GROUP, PID
    }

    static int propSeqPriorNum = 0;
    static int agreeSeqPriorNum = 0;
    static int deliverableMsgNum = 0;


    // Msg Queue (Not actually queue)
    //  Store all msg that receive from socket and execute by QueueTask
    static Queue<Message> msgRecvQueue = new LinkedList<Message>();
    static Queue<Message> msgSendQueue = new LinkedList<Message>();
    static Queue<Message> msgDeliverQueue = new LinkedList<Message>();
    // Insert: q.offer(e); Head: q.element(); Remove: q.poll();

    static Boolean isEmptyMsgRecvQueue = true;
    static Boolean isEmptyMsgSendQueue = true;
    static Boolean isEmptyMsgDeliverQueue = false;


    // Store new msgID, which also hint the origin receive order
    static ArrayList<Message> msgReplyQueue = new ArrayList<Message>();

    static HashMap<Integer, ArrayList<ArrayList<Integer>>> msgPropSeqProirListMap = new HashMap<Integer, ArrayList<ArrayList<Integer>>>();

    static HashMap<Integer, Integer> msgIDMap = new HashMap<Integer, Integer>();

    // Structure: <msgID, MessageInfo>
    static HashMap<Integer, MessageInfo> msgInfoMap = new HashMap<Integer, MessageInfo>();
    // MessageInfo Structure: <msg, msgID, senderPID, agrProir, propPID, dilverable>

    static ArrayList<MessageInfo> msgOrderList = new ArrayList<MessageInfo>();

    static HashMap<Integer, Integer> msgRecvTimesMap = new HashMap<Integer, Integer>();
}