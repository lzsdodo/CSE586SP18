package edu.buffalo.cse.cse486586.simpledht;


import android.util.Log;

/*
 * Reference:
 * - Android Dev Docs:
 *
 * - Article:
 *      Simply Singleton: https://www.javaworld.com/article/2073352/core-java/simply-singleton.html
 */
public class Chord {

    static final String TAG = "CHORD";
    static final int FINGER_TABLE_SIZE = 16;
    // static final String CHORD_START = "0000000000000000000000000000000000000000";
    // static final String CHORD_END   = "ffffffffffffffffffffffffffffffffffffffff";

    private static Chord node;

    private String nID;
    private String nPort;
    private String predNID;
    private String predPort;
    private String succNID;
    private String succPort;
    private FingerTable fingerTable;

    private Chord() {
        this.nID = GV.MY_NID;
        this.nPort = GV.MY_PORT;
        this.predNID = null;
        this.predPort = null;
        this.succNID = null;
        this.succPort = null;
        this.logInfo();
        this.fingerTable = new FingerTable(this.nID, this.nPort, FINGER_TABLE_SIZE);
    }

    public static Chord getInstance() {
        if (node == null)
            node = new Chord();
        return node;
    }

    private void join(String nID, String nPort) {
        String oldSuccNID = this.succNID;

        // Handle join request
        if (this.succNID == null) {
            // Single node, [newNID, nID, newNID]
            this.succNID = nID;
            this.succPort = nPort;
            this.predNID = nID;
            this.predPort = nPort;

        } else {
            // Two nodes
            String tmpPort = null;
            if (this.succNID.equals(this.predNID)) {
                tmpPort = this.succPort;
                if (this.inInterval(nID, this.nID, this.succNID)) {
                    // location [nID, newNID, succNID]
                    this.succNID = nID;
                    this.succPort = nPort;
                } else {
                    // location [predNID, newNID, nID]
                    this.predNID = nID;
                    this.predPort = nPort;
                }

            } else {
                // Three or more nodes
                if (this.inInterval(nID, this.predNID, this.succNID)) {
                    if (nID.compareTo(this.nID) > 0) {
                        // location [nID, newNID, succNID]
                        tmpPort = this.succPort;
                        this.succNID = nID;
                        this.succPort = nPort;

                    } else {
                        // location [predNID, newNID, nID]
                        tmpPort = this.predPort;
                        this.predNID = nID;
                        this.predPort = nPort;
                    }

                } else {
                    // location [predNID, nID, succNID, newNID]
                    this.joinSucc();
                }
            }

            // Tell node that I receive join request
            // And notify the related node
            this.notifyNode(nPort, this.nPort);
            if (tmpPort!= null)
                this.notifyNode(tmpPort, nPort);

            // stabilize when have new succ node
            if (!oldSuccNID.equals(this.succNID)) {
                this.stabilize();
            }

            // TODO: Update FingerTable
        }

        // pred = nil
        // succ = n'.find_succ(n)

        // if(n') {
        //     initFingerTable(n');
        //     updateOthers();
        //     // move keys in (pred, n] from succ
        // } else { // n is the only node in the network
        //     for (int i=0; i<FINGER_TABLE_SIZE; i++) {
        //         finger[i].setNID(this.myNID);
        //     }
        //     this.pred = this.mNID;
        // }

    }

    private void notifyNode(String targetPort, String newNodePort) {
        // TODO
        // n' thinks it might be our predecessor
        // if (pred is nil or n' ∈ (pred, n))
        //      pred = n'

    }

    private void joinSucc() {
        // TODO
    }


    private void stabilize() {
        // TODO: when get new succ node
        // verify n's immediate successor, and tell the successor about n
        // x = successor.predecessor
        // if (x ∈ (n, succ))
        //      succ = x
        // succ.notify(n)
    }


    public String lookup(String kid) {
        // Single node: succNID = predNID = null;
        if (this.succNID == null)
            return this.nPort;

        // Local
        if (this.inInterval(kid, this.predNID, this.nID))
            return this.nPort;

        // Two nodes: succNID=predNID; Three or more: succNID!=predNID;
        if (this.succNID.equals(this.predNID))
            return this.succPort;
        else
            return this.fingerTable.lookupFingerTable(kid, this.succPort);
    }

    public void leave() {
        // TODO
        // 1. notify neighbors (pred and succ)
        // 2. query local
        // 3. send insert to succ
    }

    private boolean inInterval(String id, String fromID, String toID) {
        if (toID.compareTo(fromID) > 0) {
            if ((id.compareTo(fromID) > 0) && (id.compareTo(toID) < 0))
                return true;
            else
                return false;
        } else {
            if ((id.compareTo(fromID) < 0) && (id.compareTo(toID) > 0))
                return false;
            else
                return true;
        }
    }

    private void logInfo() {
        Log.v(TAG, "NODE: " + this.nPort + "-" + this.nID + "\n" +
                "PRED: " + this.predPort + "-" + this.predNID + "\n" +
                "SUCC: " + this.succPort + "-" + this.succNID);
    }

}
