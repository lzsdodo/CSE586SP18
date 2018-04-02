package edu.buffalo.cse.cse486586.simpledht;


/*
 * Reference:
 * - Android Dev Docs:
 *
 * - Article:
 *      Simply Singleton: https://www.javaworld.com/article/2073352/core-java/simply-singleton.html
 */
public class Chord {

    private String myNodeID = "";
    private String predNodeID = "";
    private String succNodeID = "";
    private Boolean isSingleNode = true;

    private static final Chord node = new Chord();

    private Chord() {
        this.createNode();
    }

    private void createNode() {
        this.myNodeID = GV.NODE_ID_MAP.get(GV.MY_PORT);
        this.predNodeID = this.myNodeID;
        this.succNodeID = this.myNodeID;
        this.isSingleNode = true;
    }

    public static Chord getNode() {
        return node;
    }

    private void join() {}

    private void notifyNode() {}

    private void fixFingers() {}

    private void stabilize() {}

    private void lookup() {}

}
