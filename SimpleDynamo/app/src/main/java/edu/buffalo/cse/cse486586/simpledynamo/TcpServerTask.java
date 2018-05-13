package edu.buffalo.cse.cse486586.simpledynamo;


import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class TcpServerTask extends AsyncTask<Void, Void, Void> {

    static final String TAG = "SERVER";
    static final int SERVER_PORT = 10000;

    private ServerSocket serverSocket;
    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private BufferedReader br;

    @Override
    protected Void doInBackground (Void... voids) {
        Log.v(TAG, "Start TcpServerTask.");
        this.init();

        while (true) {
            try {
                this.socket = this.serverSocket.accept();

                if (this.socket != null) {
                    Log.v(TAG, "ACCEPTED CONN: " + this.socket.getRemoteSocketAddress().toString());
                    this.socket.setTrafficClass(0x04 | 0x10);
                    this.socket.setReceiveBufferSize(8 * 1024);

                    this.out = this.socket.getOutputStream();
                    this.in = this.socket.getInputStream();
                    this.br = new BufferedReader(new InputStreamReader(in));

                    String msgRecvStr = this.br.readLine();
                    Log.v("TCP RECV MSG", msgRecvStr);
                    this.handleMsg(NMessage.parseMsg(msgRecvStr));
                }

            } catch (ConnectTimeoutException e) {
                Log.e(TAG, "ServerTask ConnectTimeoutException");
                e.printStackTrace();
            } catch (SocketTimeoutException e) {
                // Socket's soTimeout method is timeout for read/write, not for the connection
                // Socket's IO is blocking
                // We should close by ourselves when we catch the exception
                Log.e(TAG, "ServerTask SocketTimeoutException");
                e.printStackTrace();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ServerTask UnknownHostException");
                e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "ServerTask IOException");
                e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "ServerTask Exception.");
                e.printStackTrace();
            } finally {

                try {
                    this.br.close();
                    this.in.close();
                    this.out.close();
                    this.socket.close();
                    // Log.v(TAG, "SERVER SOCKET IO CLOSED.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void handleMsg(NMessage msgRecv) {

        if (GV.lostPort!=null) {
            if (GV.lostPort.equals(msgRecv.getSndPort())) {
                GV.lostPort = null;
            }
        }

        switch (msgRecv.getMsgType()) {
            case LOST:
            case RECOVERY:
            case UPDATE_COMPLETED:
            case UPDATE_INSERT:
            case UPDATE_DELETE:
                GV.updateRecvQueue.offer(msgRecv);
                break;

            case QUERY:
            case INSERT:
            case DELETE:
            case RESULT_ONE:
            case RESULT_ALL:
            case RESULT_ALL_COMLETED:
                if (GV.lostPort!=null) {
                    Dynamo dynamo = Dynamo.getInstance();
                    if (dynamo.detectFail(msgRecv.getMsgKey(),
                            msgRecv.getSndPort(), msgRecv.getTgtPort())) {
                        // Skip [0]/[1] node, store in notifyPredNode
                        Log.d(TAG, "DETECT FAIL " + msgRecv.getMsgType().name() +
                                " : \nSEND PORT=" + msgRecv.getSndPort() +
                                "; TGT PORT=" + msgRecv.getTgtPort() +
                                "; PERFER PORT LIST=" + dynamo.getPerferPortList(
                                        dynamo.getPerferIdList(dynamo.genHash(msgRecv.getMsgKey()))));
                        msgRecv.setTgtPort(dynamo.getPredPort());
                        GV.notifyPredNode.add(msgRecv);
                    }
                }
                GV.msgRecvQueue.offer(msgRecv);
                break;

            default:
                Log.e(TAG, "handleMsg -> SWITCH DEFAULT CASE ERROR: " + msgRecv.toString());
                break;
        }

        this.refreshUI(msgRecv.toString());
    }



    // TEST
    @Override
    protected void onPostExecute(Void result) {
        Log.e("LOST SERVER", "SERVER TASK SHOULD NOT BREAK.");
    }

    private void init() {
        try {
            this.serverSocket = new ServerSocket();
            this.serverSocket.setReuseAddress(true);
            this.serverSocket.bind(new InetSocketAddress(SERVER_PORT));
            Log.e(TAG, "Create a ServerSocket listening on: " + serverSocket.getLocalSocketAddress());
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            e.printStackTrace();
        }
    }

    private void refreshUI(String str) {
        Message uiMsg = new Message();
        uiMsg.what = SimpleDynamoActivity.UI;
        uiMsg.obj = str;
        SimpleDynamoActivity.uiHandler.sendMessage(uiMsg);
    }

}
