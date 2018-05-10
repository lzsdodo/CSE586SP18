package edu.buffalo.cse.cse486586.simpledynamo;

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;

public class Dynamo {

    static final String TAG = "DYNAMO";
    static final int N = 3;

    private static Dynamo instance;

    private String port;
    private String id;
    private String succPort;
    private String succID;
    private String predPort;
    private String predID;

    private ArrayList<String> nodeIdList = new ArrayList<String>(0);
    private HashMap<String, String> idPortMap = new HashMap<String, String>(0);

    private Dynamo() {
        this.port = GV.MY_PORT;
        this.id = this.genHash(this.port);
        initIdPortMap(GV.PORTS);
        initNodeIDList(GV.PORTS);
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


    public ArrayList<String> getPerferIdList(String key) {
        String kid = genHash(key);
        for (int i=1; i<6; i++) {
            if (inInterval(kid, this.nodeIdList.get(i-1), this.nodeIdList.get(i))) {
                ArrayList<String> perferIdList = new ArrayList<String>(0);
                perferIdList.addAll(this.nodeIdList.subList(i, i+N));
                Log.d("TAG", kid + " -> " + perferIdList.toString());
                return perferIdList;
            }
        }
        return null;
    }

    public ArrayList<String> getPerferPortList(ArrayList<String> perferIdList) {
        ArrayList<String> perferPortList = new ArrayList<String>(0);
        for(int i=0; i<perferIdList.size(); i++) {
            perferPortList.add(this.idPortMap.get(perferIdList.get(i)));
        }
        return perferPortList;
    }







    private void initNodeIDList(ArrayList<String> ports) {
        for(String port: ports) {
            this.nodeIdList.add(genHash(port));
        }
        Collections.sort(this.nodeIdList);
        this.nodeIdList.addAll(this.nodeIdList.subList(0, N));
        Log.e(TAG, "INIT NODE-ID-LIST: " + this.nodeIdList.toString());
    }

    private void initIdPortMap(ArrayList<String> ports) {
        for(String port: ports) {
            String nodeId = this.genHash(port);
            this.idPortMap.put(nodeId, port);
        }
        Log.e(TAG, "INIT ID-PORT-MAP: " + this.idPortMap.toString());
    }

    private void initNeighbourInfo() {
        int index = this.nodeIdList.indexOf(this.id);
        Log.e(TAG, "" + index);
        if (index == 0) {
            index = this.nodeIdList.lastIndexOf(this.id);
        }
        this.predID = this.nodeIdList.get(index-1);
        this.succID = this.nodeIdList.get(index+1);
        this.predPort = this.idPortMap.get(this.predID);
        this.succPort = this.idPortMap.get(this.succID);
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
            return ((id.compareTo(fromID) >= 0) && (id.compareTo(toID) < 0));
        } else {
            // fromID.compareTo(toID) > 0
            return ((id.compareTo(toID) < 0) || (id.compareTo(fromID) >= 0));

        }
    }

}