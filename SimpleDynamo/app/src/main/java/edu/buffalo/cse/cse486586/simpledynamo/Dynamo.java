package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;

public class Dynamo {

    static final String TAG = "DYNAMO";
    static final int N = 3;
    static final ArrayList<String> PORTS = new ArrayList<String>(
            Arrays.asList("5554", "5556", "5558", "5560", "5562"));

    private static Dynamo instance;

    private String port;
    private String id;
    private String succPort;
    private String succID;
    private String predPort;
    private String predID;

    private Dynamo() {
        this.port = GV.MY_PORT;
        this.id = genHash(this.port);
        GV.MY_ID = this.id;
        initIdPortMap(PORTS);
        initNodeIDList(PORTS);
        initNeighbourInfo();
        Log.e(TAG, "Dynamo Info: \n" + "PRED: " + this.getPredPort() + "::" + this.getPredID() + "\n" +
                    "NODE: " + this.getPort() + "::" + this.getId() + "\n" +
                    "SUCC: " + this.getSuccPort() + "::" + this.getSuccID());
    }

    static public Dynamo getInstance() {
        if (instance == null)
            instance = new Dynamo();
        return instance;
    }

    public String getPort() {return this.port;}
    public String getId() {return this.id;}
    public String getSuccPort() {return this.succPort;}
    public String getSuccID() {return this.succID;}
    public String getPredPort() {return this.predPort;}
    public String getPredID() {return this.predID;}

    static synchronized boolean detectSkipMsg(String key, String sndPort, String tgtPort) {
        // ONLY FOR INSERT AND DELETE
        String TAG = "DYNAMO DETECT SKIP";
        String kid = Dynamo.genHash(key);
        ArrayList<String> perferIdList = Dynamo.getPerferIdList(kid);
        int sndIndex = perferIdList.indexOf(Dynamo.genHash(sndPort));
        int tgtIndex = perferIdList.indexOf(Dynamo.genHash(tgtPort));
        Log.d(TAG, "SEND PORT=" + sndPort + "; TGT PORT=" + tgtPort + "; " +
                "PERFER PORT LIST=" + Dynamo.getPerferPortList(perferIdList));

        if (tgtIndex < 0) {
            Log.e(TAG,"ERROR!!!!!!");
            return false;
        }

        if (sndIndex>=0 && sndIndex==tgtIndex) {
            Log.d(TAG,"Send to self...");
            return false;
        }

        if (tgtIndex==0) {
            Log.d(TAG,"First node...");
            return false;
        }

        if (sndIndex==0 && tgtIndex==1) {
            Log.d(TAG,"First to second...");
            return false;
        }

        if (sndIndex==1 && tgtIndex==2) {
            Log.d(TAG,"Second to third...");
            return false;
        }

        if ((sndIndex!=0 && tgtIndex == 1) || (sndIndex==0 && tgtIndex==2)) {
            // skip [0]/[1] node;
            Log.e(TAG,"Lost Port which index is: " + (tgtIndex-1) + " => " + GV.PRED_PORT);
            return true;
        }

        Log.e(TAG, "DETECT FAIL ERROR!!!!!!!!!!!\nSHOULD NOT EXIST!!!");
        return false;
    }

    // ALREADY TEST
    static String getFirstPort(String kid) {
        ArrayList<String> perferIdList = getPerferIdList(kid);
        String firstPort = GV.idPortMap.get(perferIdList.get(0));
        Log.d(TAG, "My port " + GV.MY_PORT + " is equal to first? " + firstPort + "\nin" +
                getPerferPortList(perferIdList).toString() + " ~" + kid);
        return firstPort;
    }

    static String getLastPort(String kid) {
        ArrayList<String> perferIdList = getPerferIdList(kid);
        String lastPort = GV.idPortMap.get(perferIdList.get(N-1));
        Log.d(TAG, "My port " + GV.MY_PORT + " is equal to last? " + lastPort + "\nin" +
                getPerferPortList(perferIdList).toString() + " ~" + kid);
        return lastPort;
    }

    static boolean isFirstNode(String kid) {
        ArrayList<String> perferIdList = getPerferIdList(kid);
        String firstId = perferIdList.get(0);
        Log.d(TAG, "My id " + GV.MY_ID  + " is equal to first? " + firstId + "\nin" +
                perferIdList.toString());
        return GV.MY_ID.equals(firstId);
    }

    static boolean isLastNode(String kid) {
        ArrayList<String> perferIdList = getPerferIdList(kid);
        String lastId = perferIdList.get(N-1);
        Log.d(TAG, "My id " + GV.MY_ID + " is equal to last? " + lastId + "\nin" +
                perferIdList.toString());
        return GV.MY_ID.equals(lastId);
    }

    static String getSuccPortOfPort(String port) {
        String pid = genHash(port);
        int pidIndex = GV.nodeIdList.indexOf(pid);
        String succId = GV.nodeIdList.get(pidIndex+1);
        String succPort = GV.idPortMap.get(succId);
        Log.e("DYNAMO INFO", port + "\'s SUCC PORT: " + succPort);
        return succPort;
    }

    static String getPredPortOfPort(String port) {
        String pid = genHash(port);
        int pidIndex = GV.nodeIdList.indexOf(pid);
        if (pidIndex==0) {
            pidIndex = GV.nodeIdList.lastIndexOf(pid);
        }
        String predId = GV.nodeIdList.get(pidIndex-1);
        String predPort = GV.idPortMap.get(predId);
        Log.e("DYNAMO INFO", port + "\'s PRED PORT: " + predPort);
        return predPort;
    }

    synchronized static ArrayList<String> getPerferIdList(String kid) {
        for (int i=1; i<6; i++) {
            if (inInterval(kid, GV.nodeIdList.get(i-1), GV.nodeIdList.get(i))) {
                ArrayList<String> perferIdList = new ArrayList<String>(0);
                perferIdList.addAll(GV.nodeIdList.subList(i, i+N));
                Log.d(TAG, kid + " -> " + perferIdList.toString());
                return perferIdList;
            }
        }
        return null;
    }

    synchronized static ArrayList<String> getPerferPortList(String kid) {
        return getPerferPortList(getPerferIdList(kid));
    }

    synchronized static ArrayList<String> getPerferPortList(ArrayList<String> perferIdList) {
        ArrayList<String> ports = new ArrayList<String>();
        for(String id: perferIdList) {
            ports.add(GV.idPortMap.get(id));
        }
        return ports;
    }

    private void initNodeIDList(ArrayList<String> ports) {
        for(String port: ports) {
            GV.nodeIdList.add(genHash(port));
        }
        Collections.sort(GV.nodeIdList);
        GV.nodeIdList.addAll(GV.nodeIdList.subList(0, N));
        Log.e(TAG, "INIT NODE-ID-LIST: " + GV.nodeIdList.toString());
    }

    private void initIdPortMap(ArrayList<String> ports) {
        for(String port: ports) {
            String nodeId = genHash(port);
            GV.idPortMap.put(nodeId, port);
        }
        Log.e(TAG, "INIT ID-PORT-MAP: " + GV.idPortMap.toString());
    }

    private void initNeighbourInfo() {
        int index = GV.nodeIdList.indexOf(this.id);
        Log.e(TAG, "" + index);
        if (index == 0) {
            index = GV.nodeIdList.lastIndexOf(this.id);
        }

        this.predID = GV.nodeIdList.get(index-1);
        this.succID = GV.nodeIdList.get(index+1);
        GV.PRED_ID = this.predID;
        GV.SUCC_ID = this.succID;

        this.predPort = GV.idPortMap.get(this.predID);
        this.succPort = GV.idPortMap.get(this.succID);
        GV.PRED_PORT = this.predPort;
        GV.SUCC_PORT = this.succPort;
    }

    static String genHash(String input) {
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
        String hashStr = formatter.toString();
        formatter.close();
        return hashStr;
    }

    static boolean inInterval(String id, String fromID, String toID) {
        if (toID.compareTo(fromID) > 0) {
            return ((id.compareTo(fromID) >= 0) && (id.compareTo(toID) < 0));
        } else {
            return ((id.compareTo(toID) < 0) || (id.compareTo(fromID) >= 0));
        }
    }

}
