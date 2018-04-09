package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.util.ArrayList;

public class FingerTable {

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

    public String lookupFingerTable(String kID, String succPort) {
        for (int i=ftSize-1; i>=0; i--) {
            if (kID.compareTo(this.fingers.get(i).getStartID()) >= 0) {
                return this.fingers.get(i).getSuccPort();
            }
        }
        return succPort;
    }



    public void updateFingerTable(String nID, String nPort, boolean bool) {
        // Refresh finger table entries
        // i = random index > 1 into finger[];
        // finger[i].node = find_succ(finger[i].start);

        // bool = true as join, false as leave
        // TODO: When node join and leave
        if (bool) {
            // Join
            if (this.inFingerTable(nID)) {
                for (int i=0; i<this.ftSize-1; i++) {
                    if (this.fingers.get(i) != null) {
                        if (this.inInterval(nID, this.fingers.get(i).getStartID(), this.fingers.get(i+1).getStartID())) {

                        }
                    } else {
                        this.fingers.get(i).setSuccID(nID);
                        this.fingers.get(i).setSuccPort(nPort);
                    }

                }
            }

            if (this.fingers.get(this.ftSize-1).getSuccID() != null){
                if (nID.compareTo(this.fingers.get(this.ftSize-1).getSuccID()) < 0) {
                    this.fingers.get(this.ftSize-1).setSuccID(nID);
                    this.fingers.get(this.ftSize-1).setSuccPort(nPort);
                }
            } else {
                this.fingers.get(this.ftSize-1).setSuccID(nID);
                this.fingers.get(this.ftSize-1).setSuccPort(nPort);
            }

        } else {
            // Leave
        }
        this.logInfo();
    }

    public boolean inFingerTable(String nid) {
        if (this.inInterval(nid, this.nID, this.fingers.get(this.ftSize-1).startID))
            return true;
        return false;
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

    private void initFingerTable(String nID, String nPort, int ftSize) {
        int partition = ftSize/4;
        String prefNID = nID.substring(0, partition);
        String suffNID = nID.substring(partition);

        for (int i=0; i<ftSize; i++) {
            String startID = genPrefStartID(prefNID, i) + suffNID;
            this.fingers.add(new Finger(startID));
        }
        this.logInfo();

        this.updateFingerTable(nID, nPort, true);
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

    private void logInfo() {
        for (Finger finger : fingers) {
            Log.v("FT", finger.toString());
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
        public String getSuccID() {return this.succID;}
        public String getSuccPort() {return this.succPort;}

        public void setSuccID(String succID) {this.succID = succID;}
        public void setSuccPort(String succPort) {this.succPort = succPort;}

        public String toString() {
            return this.startID + ", " + this.succID + ", " + this.succPort;
        }

    }

}
