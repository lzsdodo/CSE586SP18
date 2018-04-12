package edu.buffalo.cse.cse486586.simpledht;


import android.content.ContentValues;
import android.database.Cursor;
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

    private String port;
    private String id;
    private String succPort;
    private String succID;
    private String predPort;
    private String predID;
    public FingerTable fingerTable;

    private Chord() {
        this.predPort = null;
        this.port = GV.MY_PORT;
        this.succPort = null;

        this.predID = null;
        this.id = GV.MY_NID;
        this.succID = null;

        this.logInfo();
        this.fingerTable = new FingerTable(this.id, this.port, FINGER_TABLE_SIZE);
    }

    public static Chord getInstance() {
        if (node == null)
            node = new Chord();
        return node;
    }

    public String getPort() {return this.port;}
    public String getSuccPort() {return this.succPort;}

    public String lookup(String key) {
        String kid = Utils.genHash(key);

        // Single node: succNID = predNID = null;
        if (this.succID == null)
            return this.port;

        // Local
        if (Utils.inInterval(kid, this.predID, this.id))
            return this.port;

        // Two nodes: succNID=predNID;
        if (this.succID.equals(this.predID)) {
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
        Log.e(TAG, "NODE LEAVING.");
        // 1. notify neighbors (pred and succ)
        // 2. query local
        // 3. send insert to succ
    }

    public void getJoin(String joinNPort) {
        this.join(joinNPort, false);
    }

    public void getNotify(String joinNPort) {
        this.join(joinNPort, true);
    }

    public void updateFingerTable(String joinPort) {
        if (!GV.knownNodes.contains(joinPort)) {
            GV.knownNodes.add(joinPort);
            this.fingerTable.updateFingerTable(Utils.genHash(joinPort), joinPort);
        }
        // <TBD>: if the size of record is big, remove some
    }

    private void join(String joinPort, boolean isNotify) {
        // bool:
        //      false: this node receive join request;
        //      true: this node receive notification;
        // when you get notify, you just need to update your pred or succ info

        String prevSuccPort = this.succPort;
        String prevPredPort = this.predPort;
        String joinID = Utils.genHash(joinPort);

        // Handle join request
        if (this.succID == null) {
            // Single node, [~, id, ~]
            this.succPort = joinPort;
            this.predPort = joinPort;
            this.succID = joinID;
            this.predID = joinID;

        } else {
            // Two nodes
            if (this.succID.equals(this.predID)) {
                if (Utils.inInterval(joinID, this.id, this.succID)) {
                    // location [id, ~, succID]
                    this.succPort = joinPort;
                    this.succID = joinID;

                } else {
                    // location [predID, ~, id]
                    this.predPort = joinPort;
                    this.predID = joinID;
                }

            } else {
                // Three or more nodes
                if (Utils.inInterval(joinID, this.predID, this.succID)) {
                    // location [predID, ~, id, ~ succID]
                    if (joinID.compareTo(this.id) > 0) {
                        // location [id, joinID, succID]
                        this.succPort = joinPort;
                        this.succID = joinID;

                    } else {
                        // location [predID, ~, id]
                        this.predPort = joinPort;
                        this.predID = joinID;
                    }

                } else {
                    // location [~, predID, id, succID, ~]
                    if (!isNotify) GV.msgSendQueue.offer(new Message(Message.TYPE.JOIN, joinPort, this.getSuccPort(), joinPort));
                }
            }

            // Update finger table
            this.updateFingerTable(joinPort);

            // Tell node that I receive join request and notify the related node
            if (!isNotify)
                this.sendNotify(prevSuccPort, prevPredPort, joinPort);

            // stabilize when have new succ node
            if (!prevSuccPort.equals(this.succPort))
                this.stabilize();
        }

    }

    private void sendNotify(String prevSuccPort, String prevPredPort, String joinPort) {
        // tell joinPort this node received join request
        GV.msgSendQueue.offer(new Message(Message.TYPE.NOTYFY, this.port, joinPort, this.port));

        // succPort changed
        if (!prevSuccPort.equals(this.succPort)) {
            GV.msgSendQueue.offer(new Message(Message.TYPE.NOTYFY, this.port, prevSuccPort, joinPort));
            GV.msgSendQueue.offer(new Message(Message.TYPE.NOTYFY, this.port, joinPort, prevSuccPort));
        }

        // predPort changed
        if (!prevPredPort.equals(this.predPort)) {
            GV.msgSendQueue.offer(new Message(Message.TYPE.NOTYFY, this.port, prevPredPort, joinPort));
            GV.msgSendQueue.offer(new Message(Message.TYPE.NOTYFY, this.port, joinPort, prevPredPort));
        }

    }

    private void stabilize() {
        // TODO: when get new succ node
        Log.e(TAG, "STABLIZING.");
        /*
        // 1. Query local all
        Cursor c = GV.dbCR.query(GV.dbUri, null, "@", null, null);
        // 2. Send specific kv pairs to succ
        String port;
        c.moveToFirst();
        while (!c.isAfterLast()) {
            String k = c.getString(c.getColumnIndex("key"));
            String v = c.getString(c.getColumnIndex("value"));
            port = this.lookup(k);
            if (port.equals(this.succPort)) {
                ContentValues cv = new ContentValues();
                cv.put("key", k);
                cv.put("value", v);
                GV.dbCR.insert(GV.dbUri, cv);
                cv.clear();
            } else {
                Log.e(TAG, "<TBD>: Clean local kv pairs which was inserted to succ node");
            }
        }
        c.close();
        */
    }

    private void logInfo() {
        Log.d(TAG, "PRED: " + this.predPort + "-" + this.predID + "\n" +
                "NODE: " + this.port + "-" + this.id + "\n" +
                "SUCC: " + this.succPort + "-" + this.succID);
    }

}
