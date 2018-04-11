package edu.buffalo.cse.cse486586.simpledht;

import android.content.ContentResolver;
import android.net.Uri;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

class GV {

    static final String URI = "edu.buffalo.cse.cse486586.simpledht.provider";

    static TextView uiTV = null;
    static Uri dbUri = null;

    static ContentResolver dbCR = null;
    static boolean dbIsBusy = false;
    static boolean dbIsWaiting = false;
    static boolean dbIsOtherQuery = false;

    static final int SERVER_PORT = 10000;
    static final String REMOTE_ADDR = "10.0.2.2";
    static final ArrayList<String> PORTS = new ArrayList<String>(
             Arrays.asList("5554", "5556", "5558", "5560", "5562"));

    static String MY_PORT = null; // My Port
    static String MY_NID = null;
    static ArrayList<String> knownNodes = new ArrayList<String>();

    static Queue<Message> msgRecvQueue = new LinkedList<Message>();
    static Queue<Message> msgSendQueue = new LinkedList<Message>();

    static HashMap<String, String> resultOneMap = new HashMap<String, String>();
    static HashMap<String, String> resultAllMap = new HashMap<String, String>();
}

/*

Order: 4 -> 1 -> 0 -> 2 -> 3 (5562 -> 5556 -> 5554 -> 5558 -> 5560)

"5554", "33d6357cfaaf0f72991b0ecd8c56da066613c089";
"5556", "208f7f72b198dadd244e61801abe1ec3a4857bc9";
"5558", "abf0fd8db03e5ecb199a9b82929e9db79b909643";
"5560", "c25ddd596aa7c81fa12378fa725f706d54325d12";
"5562", "177ccecaec32c54b82d5aaafc18a2dadb753e3b1";

NODE: 5554-33d6357cfaaf0f72991b0ecd8c56da066613c089
PRED: 5556-208f7f72b198dadd244e61801abe1ec3a4857bc9
SUCC: 5558-abf0fd8db03e5ecb199a9b82929e9db79b909643
FingerTable:
	0: 33d6357cfaaf0f72991b0ecd8c56da066613c089, null, null
	...
	13: 53d6357cfaaf0f72991b0ecd8c56da066613c089, null, null
	14: 73d6357cfaaf0f72991b0ecd8c56da066613c089, abf0fd8db03e5ecb199a9b82929e9db79b909643, 5558
	15: b3d6357cfaaf0f72991b0ecd8c56da066613c089, c25ddd596aa7c81fa12378fa725f706d54325d12, 5560
	(~)

---

PRED: 5562-177ccecaec32c54b82d5aaafc18a2dadb753e3b1
NODE: 5556-208f7f72b198dadd244e61801abe1ec3a4857bc9
SUCC: 5554-33d6357cfaaf0f72991b0ecd8c56da066613c089
FingerTable:
	0: 208f7f72b198dadd244e61801abe1ec3a4857bc9, null, null
	...
	11: 288f7f72b198dadd244e61801abe1ec3a4857bc9, null, null
	12: 308f7f72b198dadd244e61801abe1ec3a4857bc9, 33d6357cfaaf0f72991b0ecd8c56da066613c089, 5554
	13: 408f7f72b198dadd244e61801abe1ec3a4857bc9, null, null
	14: 608f7f72b198dadd244e61801abe1ec3a4857bc9, null, null
	15: a08f7f72b198dadd244e61801abe1ec3a4857bc9, abf0fd8db03e5ecb199a9b82929e9db79b909643, 5558
    (~)

---

PRED: 5554-33d6357cfaaf0f72991b0ecd8c56da066613c089
NODE: 5558-abf0fd8db03e5ecb199a9b82929e9db79b909643
SUCC: 5560-c25ddd596aa7c81fa12378fa725f706d54325d12
FingerTable:
	0: abf0fd8db03e5ecb199a9b82929e9db79b909643, null, null
	...
	12: bbf0fd8db03e5ecb199a9b82929e9db79b909643, c25ddd596aa7c81fa12378fa725f706d54325d12, 5560
	13: cbf0fd8db03e5ecb199a9b82929e9db79b909643, null, null
	14: ebf0fd8db03e5ecb199a9b82929e9db79b909643, 177ccecaec32c54b82d5aaafc18a2dadb753e3b1, 5562
	(~)
	15: 2bf1fd8db03e5ecb199a9b82929e9db79b909643, 33d6357cfaaf0f72991b0ecd8c56da066613c089, 5554

---

PRED: 5558-abf0fd8db03e5ecb199a9b82929e9db79b909643
NODE: 5560-c25ddd596aa7c81fa12378fa725f706d54325d12
SUCC: 5562-177ccecaec32c54b82d5aaafc18a2dadb753e3b1
FingerTable:
	0: c25ddd596aa7c81fa12378fa725f706d54325d12, null, null
	13: e25ddd596aa7c81fa12378fa725f706d54325d12, null, null
	(~)
	14: 025edd596aa7c81fa12378fa725f706d54325d12, 177ccecaec32c54b82d5aaafc18a2dadb753e3b1, 5562
	15: 425edd596aa7c81fa12378fa725f706d54325d12, abf0fd8db03e5ecb199a9b82929e9db79b909643, 5558

---

PRED: 5560-c25ddd596aa7c81fa12378fa725f706d54325d12
NODE: 5562-177ccecaec32c54b82d5aaafc18a2dadb753e3b1
SUCC: 5556-208f7f72b198dadd244e61801abe1ec3a4857bc9
FingerTable:
	0: 177ccecaec32c54b82d5aaafc18a2dadb753e3b1, null, null
	...
	11: 1f7ccecaec32c54b82d5aaafc18a2dadb753e3b1, 208f7f72b198dadd244e61801abe1ec3a4857bc9, 5556
	12: 277ccecaec32c54b82d5aaafc18a2dadb753e3b1, 33d6357cfaaf0f72991b0ecd8c56da066613c089, 5554
	13: 377ccecaec32c54b82d5aaafc18a2dadb753e3b1, null, null
	14: 577ccecaec32c54b82d5aaafc18a2dadb753e3b1, null, null
	15: 977ccecaec32c54b82d5aaafc18a2dadb753e3b1, abf0fd8db03e5ecb199a9b82929e9db79b909643, 5558

 */