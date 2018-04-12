package edu.buffalo.cse.cse486586.simpledht;


import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Formatter;

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
    private ArrayList<String> portRecords;

    private Chord() {
        this.port = GV.MY_PORT;
        this.id = this.genHash(GV.MY_PORT);
        this.portRecords = new ArrayList<String>();
        this.portRecords.add(this.port);
        this.logInfo(null, null);
        this.fingerTable = new FingerTable(this.id, this.port, FINGER_TABLE_SIZE);
    }

    public static Chord getInstance() {
        if (node == null)
            node = new Chord();
        return node;
    }

    public String getPort() {return this.port;}
    public String getSuccPort() {return this.succPort;}
    public String getPredPort() {return this.predPort;}

    public String lookup(String key) {
        String kid = this.genHash(key);

        // Single node: succNID = predNID = null;
        if (this.succID == null) {
            return this.port;
        }

        // Local
        if (this.inInterval(kid, this.predID, this.id)) {
            return this.port;
        }

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

    public void getJoin(String joinPort) {
        this.updateFingerTable(joinPort);
        if (!joinPort.equals(this.port)) this.join(joinPort, false);
        GV.msgSendQueue.offer(new NewMessage(NewMessage.TYPE.NOTIFY, this.port, joinPort, this.port));
    }

    public void getNotify(String joinPort ) {
        this.updateFingerTable(joinPort);
        if (!joinPort.equals(this.port)) this.join(joinPort, true);
    }

    private void join(String joinPort, boolean isNotify) {
        // bool:
        //      false: this node receive join request;
        //      true: this node receive notification;
        // when you get notify, you just need to update your pred or succ info

        String prevSuccPort = this.succPort;
        String prevPredPort = this.predPort;
        String joinID = this.genHash(joinPort);

        // Handle join request
        if (prevSuccPort == null) {
            // Single node, [~, id, ~]
            this.succPort = joinPort;
            this.predPort = joinPort;
            this.succID = joinID;
            this.predID = joinID;

        } else {
            // Two nodes, [id, ~, succID/predID, ~, id]
            if (prevSuccPort.equals(prevPredPort)) {

                if (this.inInterval(joinID, this.id, this.succID)) {
                    // [id, ~, succID/predID]
                    this.succPort = joinPort;
                    this.succID = joinID;

                } else {
                    // [succID/predID, ~, id]
                    this.predPort = joinPort;
                    this.predID = joinID;
                }

            } else { // Three or more nodes [?, predID, ~, id, ~ succID, ?]

                if (this.inInterval(joinID, this.id, this.succID)) {
                    // location [predID, id, ~, succID]
                    this.succPort = joinPort;
                    this.succID = joinID;

                } else if(this.inInterval(joinID, this.predID, this.id)) {
                    // location [predID, ~, id]
                    this.predPort = joinPort;
                    this.predID = joinID;

                } else {
                    // location [~, predID, id, succID, ~]
                    if (!isNotify)
                        GV.msgSendQueue.offer(new NewMessage(NewMessage.TYPE.JOIN, joinPort, this.getSuccPort(), joinPort));
                }
            }
        }

        // Tell node that I receive join request and notify the related node
        if (!isNotify) this.sendNotify(prevSuccPort, prevPredPort, joinPort);

        // stabilize when have new succ node
        // if (prevSuccPort == null) this.stabilize();
        // else if (!prevSuccPort.equals(this.succPort)) this.stabilize();

        // Update UI
        this.logInfo(prevSuccPort, prevPredPort);
    }

    private void sendNotify(String prevSuccPort, String prevPredPort, String joinPort) {
        // tell joinPort this node received join request
        if (prevSuccPort != null) {

            if (!prevSuccPort.equals(this.succPort)) {
                // succPort changed
                this.logInfo(prevSuccPort, prevPredPort);
                GV.msgSendQueue.offer(new NewMessage(NewMessage.TYPE.NOTIFY, this.port, prevSuccPort, joinPort));
                GV.msgSendQueue.offer(new NewMessage(NewMessage.TYPE.NOTIFY, this.port, joinPort, prevSuccPort));
            }

            if (!prevPredPort.equals(this.predPort)) {
                // predPort changed
                this.logInfo(prevSuccPort, prevPredPort);
                GV.msgSendQueue.offer(new NewMessage(NewMessage.TYPE.NOTIFY, this.port, prevPredPort, joinPort));
                GV.msgSendQueue.offer(new NewMessage(NewMessage.TYPE.NOTIFY, this.port, joinPort, prevPredPort));
            }

        }
    }

    public void updateFingerTable(String joinPort) {
        if (!this.portRecords.contains(joinPort)) {
            this.portRecords.add(joinPort);
            this.fingerTable.updateFingerTable(this.genHash(joinPort), joinPort);
        }
        // <TBD>: if the size of record is big, remove some
    }

    private String genHash(String input) {
        Formatter formatter = new Formatter();
        byte[] sha1Hash;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1Hash = sha1.digest(input.getBytes());
            for (byte b : sha1Hash) {
                formatter.format("%02x", b);
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "GEN HASH ERR" );
            e.printStackTrace();
        }
        return formatter.toString();
    }

    private boolean inInterval(String id, String fromID, String toID) {
        if (toID.compareTo(fromID) > 0) {
            if ((id.compareTo(fromID) >= 0) && (id.compareTo(toID) < 0))
                return true;
            else
                return false;
        } else {
            // fromID.compareTo(toID) > 0
            if ((id.compareTo(toID) < 0) || (id.compareTo(fromID) >= 0))
                return true;
            else
                return false;
        }
    }

    private void logInfo(String prevSuccPort, String prevPredPort) {
        if (prevSuccPort == null)
            Log.e(TAG,"----- ----- ----- ----- -----\n" +
                    "null=>"+ this.predPort + " ~ " + this.port + " ~ null=>" + this.succPort +
                    "\n----- ----- ----- ----- -----");
        else {
            if (!prevSuccPort.equals(this.succPort))
                Log.e(TAG, "----- ----- ----- ----- -----\n" +
                        this.predPort + " ~ " + this.port + " ~ " + this.succPort + "(" + prevSuccPort +
                        ")\n----- ----- ----- ----- -----");
            if (!prevPredPort.equals(this.predPort))
                Log.e(TAG,"----- ----- ----- ----- -----\n(" +
                        prevPredPort + ")" + this.predPort + " ~  " + this.port + " ~ " + this.succPort +
                        "\n----- ----- ----- ----- -----");
        }
    }

    /*
    private void stabilize() {
        // TODO: when get new succ node
        Log.d(TAG, "STABLIZING.");

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
    }
    */

}
