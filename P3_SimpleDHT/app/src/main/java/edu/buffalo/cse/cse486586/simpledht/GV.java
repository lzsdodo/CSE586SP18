package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

class GV {

    static final String URI = "edu.buffalo.cse.cse486586.simpledht.provider";

    static Uri dbUri = null;
    static ContentResolver dbCR = null;
    static boolean dbIsWaiting = false;

    static final int SERVER_PORT = 10000;
    static final String REMOTE_ADDR = "10.0.2.2";
    static final ArrayList<String> PORTS = new ArrayList<String>(
             Arrays.asList("5554", "5556", "5558", "5560", "5562"));

    static String MY_PORT = null; // My Port

    static Queue<Message> msgRecvQueue = new LinkedList<Message>();
    static Queue<Message> msgSendQueue = new LinkedList<Message>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();
}

/*

Order: 5562 -> 5556 -> 5554 -> 5558 -> 5560

"5554", "33d6357cfaaf0f72991b0ecd8c56da066613c089";
"5556", "208f7f72b198dadd244e61801abe1ec3a4857bc9";
"5558", "abf0fd8db03e5ecb199a9b82929e9db79b909643";
"5560", "c25ddd596aa7c81fa12378fa725f706d54325d12";
"5562", "177ccecaec32c54b82d5aaafc18a2dadb753e3b1";


PRED: 5556, NODE: 5554, SUCC: 5558
FingerTable:
	14: 73d6357cfaaf0f72991b0ecd8c56da066613c089, abf0fd8db03e5ecb199a9b82929e9db79b909643, 5558
	15: b3d6357cfaaf0f72991b0ecd8c56da066613c089, c25ddd596aa7c81fa12378fa725f706d54325d12, 5560

---

PRED: 5562, NODE: 5556, SUCC: 5554
FingerTable:
	12: 308f7f72b198dadd244e61801abe1ec3a4857bc9, 33d6357cfaaf0f72991b0ecd8c56da066613c089, 5554
	15: a08f7f72b198dadd244e61801abe1ec3a4857bc9, abf0fd8db03e5ecb199a9b82929e9db79b909643, 5558

---

PRED: 5554, NODE: 5558, SUCC: 5560
FingerTable:
	12: bbf0fd8db03e5ecb199a9b82929e9db79b909643, c25ddd596aa7c81fa12378fa725f706d54325d12, 5560
	14: ebf0fd8db03e5ecb199a9b82929e9db79b909643, 177ccecaec32c54b82d5aaafc18a2dadb753e3b1, 5562
	15: 2bf1fd8db03e5ecb199a9b82929e9db79b909643, 33d6357cfaaf0f72991b0ecd8c56da066613c089, 5554

---

PRED: 5558, NODE: 5560, SUCC: 5562
FingerTable:
	14: 025edd596aa7c81fa12378fa725f706d54325d12, 177ccecaec32c54b82d5aaafc18a2dadb753e3b1, 5562
	15: 425edd596aa7c81fa12378fa725f706d54325d12, abf0fd8db03e5ecb199a9b82929e9db79b909643, 5558

---

PRED: 5560, NODE: 5562, SUCC: 5556
FingerTable:
	11: 1f7ccecaec32c54b82d5aaafc18a2dadb753e3b1, 208f7f72b198dadd244e61801abe1ec3a4857bc9, 5556
	12: 277ccecaec32c54b82d5aaafc18a2dadb753e3b1, 33d6357cfaaf0f72991b0ecd8c56da066613c089, 5554
	15: 977ccecaec32c54b82d5aaafc18a2dadb753e3b1, abf0fd8db03e5ecb199a9b82929e9db79b909643, 5558

 */