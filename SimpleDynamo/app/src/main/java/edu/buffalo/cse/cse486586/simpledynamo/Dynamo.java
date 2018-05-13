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

    static ArrayList<String> nodeIdList = new ArrayList<String>(0);
    static HashMap<String, String> idPortMap = new HashMap<String, String>(0);

    private Dynamo() {
        this.port = GV.MY_PORT;
        this.id = genHash(this.port);
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


    // TODO HANDLE FAIL SEND
    public boolean detectFail(String key, String sndPort, String tgtPort) {
        // INSERT AND DELETE
        String kid = genHash(key);
        ArrayList<String> perferIdList = this.getPerferIdList(kid);
        int sndIndex = perferIdList.indexOf(genHash(sndPort));
        int tgtIndex = perferIdList.indexOf(genHash(tgtPort));

        if (sndIndex<0 && tgtIndex==1) {
            // skip [0] node
            GV.lostPort = idPortMap.get(perferIdList.get(0));
            return true;

        } else if (sndIndex==0 && tgtIndex==2) {
            // Skip [1] node, store in notifyPredNode
            GV.lostPort = idPortMap.get(perferIdList.get(tgtIndex-1));
            return true;

        } else {
            Log.e(TAG, "DETECT FAIL ERROR: \nSEND PORT=" + sndPort + "; TGT PORT=" +
                    tgtPort + "; PERFER PORT LIST=" + this.portsOfPerferIdList(perferIdList));
        }

        return false;
    }

    // TODO MAYBE HAVE BUG
    synchronized public String getFirstPort(String kid, String op) {
        String firstPort = null;
        ArrayList<String> perferIdList = getPerferIdList(kid);

        if (op.equals("INSERT") || op.equals("DELETE")) {
            firstPort = idPortMap.get(perferIdList.get(0));
        } else if (op.equals("QUERY")) {
            firstPort = idPortMap.get(perferIdList.get(N-1));
        } else {
            Log.e("IS FIRST NODE", "ERROR OPERATION");
        }
        Log.d(TAG, "My port " + this.port + " is equal to first? " + firstPort + "\nin" +
                portsOfPerferIdList(perferIdList).toString() + " ~" + kid);
        return firstPort;
    }

    synchronized public boolean isLastNode(String kid, String op) {
        String lastId = "";
        ArrayList<String> perferIdList = getPerferIdList(kid);

        if (op.equals("INSERT") || op.equals("DELETE")) {
            lastId =  perferIdList.get(N-1);

        } else if (op.equals("QUERY")) {
            lastId =  perferIdList.get(0);

        } else {
            Log.e("IS LAST NODE", "ERROR OPERATION");
        }
        Log.d(TAG, "My id " + this.id + " is equal to last? " + lastId + "\nin" +
                perferIdList.toString());
        return this.id.equals(lastId);
    }

    static String getSuccPortOfPort(String port) {
        String pid = genHash(port);
        int pidIndex = nodeIdList.indexOf(pid);
        String succId = nodeIdList.get(pidIndex+1);
        String succPort = idPortMap.get(succId);
        Log.e("DYNAMO INFO", port + "\'s SUCC PORT: " + succPort);
        return succPort;
    }

    static String getPredPortOfPort(String port) {
        String pid = genHash(port);
        int pidIndex = nodeIdList.indexOf(pid);
        if (pidIndex==0) {
            pidIndex = nodeIdList.lastIndexOf(pid);
        }
        String predId = nodeIdList.get(pidIndex-1);
        String predPort = idPortMap.get(predId);
        Log.e("DYNAMO INFO", port + "\'s PRED PORT: " + predPort);
        return predPort;
    }

    // AFTER TEST
    static ArrayList<String> getPerferIdList(String kid) {
        for (int i=1; i<6; i++) {
            if (inInterval(kid, nodeIdList.get(i-1), nodeIdList.get(i))) {
                ArrayList<String> perferIdList = new ArrayList<String>(0);
                perferIdList.addAll(nodeIdList.subList(i, i+N));
                Log.v(TAG, kid + ": ID LIST => " + perferIdList.toString());
                return perferIdList;
            }
        }
        return null;
    }

    static ArrayList<String> portsOfPerferIdList(ArrayList<String> perferIdList) {
        ArrayList<String> ports = new ArrayList<String>();
        for(String id: perferIdList) {
            ports.add(idPortMap.get(id));
        }
        Log.v("DYNAMO INFO", ports.toString() + " <= PORTS LIST = ID LIST =>: " + perferIdList.toString());
        return ports;
    }

    private void initNodeIDList(ArrayList<String> ports) {
        for(String port: ports) {
            this.nodeIdList.add(genHash(port));
        }
        Collections.sort(this.nodeIdList);
        this.nodeIdList.addAll(this.nodeIdList.subList(0, N));
        Log.v("INIT DYNAMO", "NODE-ID-LIST: " + this.nodeIdList.toString());
    }

    private void initIdPortMap(ArrayList<String> ports) {
        for(String port: ports) {
            String nodeId = genHash(port);
            this.idPortMap.put(nodeId, port);
        }
        Log.v("INIT DYNAMO", "ID-PORT-MAP: " + this.idPortMap.toString());
    }

    private void initNeighbourInfo() {
        int index = this.nodeIdList.indexOf(this.id);
        Log.v("INIT DYNAMO", "INDEX OF NODE_IDS: " + index);
        if (index == 0) {
            index = this.nodeIdList.lastIndexOf(this.id);
        }
        this.predID = this.nodeIdList.get(index-1);
        this.succID = this.nodeIdList.get(index+1);
        this.predPort = this.idPortMap.get(this.predID);
        this.succPort = this.idPortMap.get(this.succID);
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
        return formatter.toString();
    }

    static boolean inInterval(String id, String fromID, String toID) {
        if (toID.compareTo(fromID) > 0) {
            return ((id.compareTo(fromID) >= 0) && (id.compareTo(toID) < 0));
        } else {
            return ((id.compareTo(toID) < 0) || (id.compareTo(fromID) >= 0));

        }
    }

}
