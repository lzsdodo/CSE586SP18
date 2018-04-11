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

    private static Chord node;

    private String nID;
    private String nPort;
    private String predNID;
    private String predPort;
    private String succNID;
    private String succPort;
    public FingerTable fingerTable;

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

    public String getNPort() {return this.nPort;}
    public String getSuccPort() {return this.succPort;}

    public void sendJoin(String cmdPort, String targetPort) {
        Message joinMsg = new Message();
        joinMsg.setCommandPort(cmdPort);
        joinMsg.setTargetPort(targetPort);
        joinMsg.setMsgType(Message.TYPE.JOIN);
        GV.msgSendQueue.offer(joinMsg);
    }

    public void join(String nID, String nPort, boolean isNotify) {
        // bool:
        //      true: this node receive join request;
        //      false: this node receive join info;

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
                if (Utils.inInterval(nID, this.nID, this.succNID)) {
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
                if (Utils.inInterval(nID, this.predNID, this.succNID)) {
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
                    this.sendJoinToSucc();
                }
            }

            // Tell node that I receive join request
            // And notify the related node
            if (!isNotify) {
                this.sendNotify(nPort, this.nPort);
                if (tmpPort!= null)
                    this.sendNotify(tmpPort, nPort);
            }


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

    private void sendNotify(String cmdPort, String targetNPort) {

    }

    private void getNotify(String newNPort) {
        // TODO
        // n' thinks it might be our predecessor
        // if (pred is nil or n' ∈ (pred, n))
        //      pred = n'

        if (!GV.knownNodes.contains(newNPort)) {
            GV.knownNodes.add(newNPort);
            String newNID = Utils.genHash(newNPort);
            this.fingerTable.updateFingerTable(newNID, newNPort);

        }

    }

    private void sendJoinToSucc() {
        // TODO
    }

    private void stabilize() {
        // TODO: when get new succ node
        Log.e(TAG, "STABLIZING. <TBD>");
        // 1. Query local all

    }

    public String lookup(String kid) {
        // Single node: succNID = predNID = null;
        if (this.succNID == null)
            return this.nPort;

        // Local
        if (Utils.inInterval(kid, this.predNID, this.nID))
            return this.nPort;

        // Two nodes: succNID=predNID;
        if (this.succNID.equals(this.predNID)) {
            return this.succPort;

        } else {
            // Three or more: succNID!=predNID;
            String targetPort = this.fingerTable.lookupFingerTable(kid);
            if (targetPort != null) return targetPort;
        }
        return this.succPort;
    }

    public void leave() {
        // TODO
        // 1. notify neighbors (pred and succ)
        // 2. query local
        // 3. send insert to succ
    }

    private void logInfo() {
        Log.d(TAG, "PRED: " + this.predPort + "-" + this.predNID + "\n" +
                "NODE: " + this.nPort + "-" + this.nID + "\n" +
                "SUCC: " + this.succPort + "-" + this.succNID);
    }

}
