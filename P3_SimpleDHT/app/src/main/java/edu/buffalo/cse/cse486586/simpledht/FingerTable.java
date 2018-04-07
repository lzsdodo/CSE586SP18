package edu.buffalo.cse.cse486586.simpledht;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;

public class FingerTable {

    public ArrayList<Finger> fingers = new ArrayList<Finger>();
    private String nID = "";
    private int ftSize = 16;

    public FingerTable(String nID, int ftSize) {
        this.nID = nID;
        this.ftSize = ftSize;
        this.fingers = new ArrayList<Finger>(ftSize);
        this.initFingerTable(nID, ftSize);
    }

    public void updateFingerTable(String nID) {
        // TODO: When node join and leave
    }

    public void logFingerTable() {
        for (Finger finger : fingers) {
            Log.v("FT", finger.toString());
        }
    }

    private void initFingerTable(String nID, int ftSize) {
        int partition = ftSize/4;
        String prefNID = nID.substring(0, partition);
        String suffNID = nID.substring(partition);

        for (int i=0; i<ftSize; i++) {
            String startID = genPrefStartID(prefNID, i) + suffNID;
            this.fingers.add(new Finger(startID));
        }
        Collections.sort(this.fingers);
    }

    private String genPrefStartID(String prefNID, int index) {
        long id = (long) (hexStrToInt(prefNID) + Math.pow(2, index));
        if (id > 0xffff) id = 0xffff;
        return Long.toHexString(id);
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

    private class Finger implements Comparable<Finger> {
        // 1. startID; 2. interval; 3. succID; 4. succPort;
        // interval = (startID, succID]
        private String startID;
        private String succID;
        private String succPort;


        public Finger (String startID) {
            this.startID = startID;
            this.succID = "";
            this.succPort = "";
        }

        public String toString() {
            return this.startID + ", " + this.succID;
        }

        @Override
        public int compareTo (Finger another) {
            return this.startID.compareTo(another.startID);
        }
    }
}
