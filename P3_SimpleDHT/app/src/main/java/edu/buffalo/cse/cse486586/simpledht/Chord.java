package edu.buffalo.cse.cse486586.simpledht;


import java.util.ArrayList;

/*
 * Reference:
 * - Android Dev Docs:
 *
 * - Article:
 *      Simply Singleton: https://www.javaworld.com/article/2073352/core-java/simply-singleton.html
 */
public class Chord {

    static final String TAG = "CHORD";
    static final int FINGER_TABLE_SIZE = 40;

    private String myNID = "";
    private String predNID = "";
    private String succNID = "";
    private Boolean isSingleNode = true;

    // Finger Table
    private ArrayList<ArrayList> fingers = new ArrayList<ArrayList>(FINGER_TABLE_SIZE);

    private static final Chord node = new Chord();

    private Chord() {
        this.myNID = Crypto.genHash(GV.MY_PORT);
        this.predNID = this.myNID;
        this.succNID = this.myNID;
        this.isSingleNode = true;
    }

    public static Chord getNode() {
        return node;
    }

    private void initFingerTable() {

    }

    private void updateOthers() {

    }

    private void updateFingerTable() {

    }

    private void join() {
        //
        // pred = nil
        // succ = n'.find_succ(n)
    }

    private void stabilize() {
        // verify n's immediate successor, and tell the successor about n
        // x = successor.predecessor
        // if (x ∈ (n, succ))
        //      succ = x
        // succ.notify(n)
    }

    private void notifyNode() {
        // n' thinks it might be our predecessor
        // if (pred is nil or n' ∈ (pred, n))
        //      pred = n'
    }

    private void fixFingers() {
        // Refresh finger table entries
        // i = random index > 1 into finger[];
        // finger[i].node = find_succ(finger[i].start);
    }



    private void lookup() {}


}
