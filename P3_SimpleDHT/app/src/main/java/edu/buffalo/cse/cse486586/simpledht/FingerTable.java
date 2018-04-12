package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.util.ArrayList;

/*
 * Reference:
 * - Article:
 *      - Paper: Chord: A Scalable Peer-to-peer Lookup Service for Internet Applications
 *      - Class Slides
 */

public class FingerTable {

    static final String TAG = "FINGER-TABLE";

    private ArrayList<Finger> fingers = new ArrayList<Finger>();
    private String nID;
    private String nPort;
    private int ftSize;

    public FingerTable(String nID, String nPort, int ftSize) {
        this.nID = nID;
        this.nPort = nPort;
        this.ftSize = ftSize;
        this.fingers = new ArrayList<Finger>(ftSize);
        this.initFingerTable(this.nID, this.nPort, this.ftSize);
    }

    public String lookupFingerTable(String kID) {
        if (this.inFingerTable(kID)) {
            for (int i=ftSize-1; i>0; i--) {
                if (this.inInterval(kID,
                        this.fingers.get(i-1).getStartID(),
                        this.fingers.get(i).getStartID())) {
                    // if there is null port in this interval, find the closest succ port
                    String targetPort = this.fingers.get(i-1).getSuccPort();
                    while ((targetPort==null) && (i<=this.ftSize-1)) {
                        targetPort = this.fingers.get(i).getSuccPort();
                        i++;
                    }
                    return targetPort;
                }
            }
        }
        return this.fingers.get(this.ftSize-1).getSuccPort();
    }

    public void updateFingerTable(String nID, String nPort) {
        // bool can add to the parameter: true as join, false as leave;
        boolean bool = true;

        if (bool) {
            // Join only for now
            if (this.inFingerTable(nID)) {
                // [fingers[0].startID ~ nID ~ finger[15].startID]
                for (int i=ftSize-1; i>0; i--) {
                    if (this.inInterval(nID,
                            this.fingers.get(i-1).getStartID(),
                            this.fingers.get(i).getStartID())) {
                        this.fingers.get(i-1).updateFinger(nID, nPort);
                    }
                }

            } else {
                // [fingers[0].startID ~ finger[15].startID ~ nID]
                this.fingers.get(this.ftSize-1).updateFinger(nID, nPort);
            }

        } else {
            // Leave
            Log.e(TAG, "FINGER TABLE UPDATE ERROR" );
        }
        this.logInfo();
    }

    public boolean inFingerTable(String id) {
        if (this.inInterval(id, this.nID, this.fingers.get(this.ftSize-1).startID))
            return true;
        return false;
    }

    private void initFingerTable(String nID, String nPort, int ftSize) {
        int partition = ftSize/4;
        String prefNID = nID.substring(0, partition);
        String suffNID = nID.substring(partition);

        this.fingers.add(new Finger(this.nID));
        for (int i=1; i<ftSize; i++) {
            String startID = genPrefStartID(prefNID, i) + suffNID;
            this.fingers.add(new Finger(startID));
        }
        this.logInfo();
    }

    private String genPrefStartID(String prefNID, int index) {
        long id = (long) (hexStrToInt(prefNID) + Math.pow(2, index));
        if (id > 0xffff) id -= 0xffff;
        String hexStr = Long.toHexString(id);
        for (int i=0; i<4-hexStr.length(); i++)
            hexStr = "0" + hexStr;
        return hexStr;
    }

    private int hexStrToInt(String hexStr) {
        // 0xffff 16bit
        char[] charsID = hexStr.toCharArray();
        int len = charsID.length;
        int intVal = 0;
        for (int i=0; i<len; i++) {
            intVal += (int) (this.hexCharToByte(charsID[i]) * Math.pow(16, len-i-1));
        }
        return intVal;
    }

    private byte hexCharToByte(char c) {
        // Make sure the char is lowercase
        c = Character.toLowerCase(c);
        byte b = (byte) "0123456789abcdef".indexOf(c);
        return b;
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
        int i = 0;
        for (Finger finger : fingers) {
            Log.d("FT", i + ": " + finger.toString());
            i++;
        }
    }

    private class Finger {
        // 1. startID; 2. interval; 3. succID; 4. succPort;
        // interval = (startID, succID]
        private String startID;
        private String succID;
        private String succPort;

        public Finger (String startID) {
            this.startID = startID;
            this.succID = null;
            this.succPort = null;
        }

        public String getStartID() {return this.startID;}
        // public String getSuccID() {return this.succID;}
        public String getSuccPort() {return this.succPort;}

        public void setSuccID(String succID) {this.succID = succID;}
        public void setSuccPort(String succPort) {this.succPort = succPort;}

        public void updateFinger(String succID, String succPort) {
            if (this.succID != null) {
                if (this.startID.compareTo(this.succID) < 0) {
                    if ((succID.compareTo(this.startID) > 0)
                            && (succID.compareTo(this.succID) < 0)) {
                        this.setSuccID(succID);
                        this.setSuccPort(succPort);
                    }
                } else {
                    if ((succID.compareTo(this.startID) > 0)
                            || (succID.compareTo(this.succID) < 0)) {
                        this.setSuccID(succID);
                        this.setSuccPort(succPort);
                    }
                }

            } else {
                this.succID = succID;
                this.succPort = succPort;
            }
        }

        public String toString() {
            return this.startID + ", " + this.succID + ", " + this.succPort;
        }

    }

}
