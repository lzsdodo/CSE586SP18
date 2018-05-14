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

    // TODO
    private void handleTcpServerMsg(NMessage msgRecv) {
        this.detectRecvLostMsg(msgRecv.getSndPort());

        switch (msgRecv.getMsgType()) {
            case SIGNAL:
                this.tcpHandleRecvSignal(msgRecv.getMsgKey());
                break;

            case RESTART:
            case IS_ALIVE:
            case RECOVERY:
            case UPDATE_INSERT:
            case UPDATE_DELETE:
            case UPDATE_COMPLETED:
                GV.msgUpdateRecvQ.offer(msgRecv);
                break;

            case QUERY:
            case INSERT:
            case DELETE:
                GV.signalSendQ.offer(msgRecv);
                GV.msgRecvQ.offer(msgRecv);
                this.detectSkipMsg(msgRecv);
                break;

            case RESULT_ONE:
            case RESULT_ALL:
            case RESULT_ALL_FLAG:
            case RESULT_ALL_COMLETED:
                GV.msgRecvQ.offer(msgRecv);
                break;

            default:
                Log.e(TAG, "handleMsg -> SWITCH DEFAULT CASE ERROR: " + msgRecv.toString());
                break;
        }

        this.refreshUI(msgRecv.toString());
    }

    private void detectRecvLostMsg(String sndPort) {
        if (GV.lostPort!=null) {
            if (sndPort.equals(GV.lostPort)) {
                // Send A SIGNAL TO LOST PORT
                GV.msgUpdateRecvQ.offer(new NMessage(NMessage.TYPE.IS_ALIVE,
                        GV.MY_PORT, GV.lostPort, "___"));
            }
        }
    }

    private void detectSkipMsg(NMessage msg) {
        // skip [0]/[1], pred post
        switch (msg.getMsgType()) {
            case INSERT:
                if (Dynamo.detectSkipMsg(msg.getMsgKey(), msg.getSndPort(), msg.getTgtPort())) {
                    msg.setMsgType(NMessage.TYPE.UPDATE_INSERT);
                    msg.setSndPort(GV.MY_PORT);
                    msg.setTgtPort(GV.PRED_PORT);
                    GV.notifyPredMsgL.add(msg);
                    GV.lostPort = GV.PRED_PORT;
                    Log.e("DETECT SKIP MSG", "LOST PRED PORT " + GV.PRED_PORT);
                }
                break;
            case DELETE:
                if (Dynamo.detectSkipMsg(msg.getMsgKey(), msg.getSndPort(), msg.getTgtPort())) {
                    msg.setMsgType(NMessage.TYPE.UPDATE_DELETE);
                    msg.setSndPort(GV.MY_PORT);
                    msg.setTgtPort(GV.PRED_PORT);
                    GV.notifyPredMsgL.add(msg);
                    GV.lostPort = GV.PRED_PORT;
                    Log.e("DETECT SKIP MSG", "LOST PRED PORT " + GV.PRED_PORT);
                }
                break;
            default: break;
        }

    }

    private void tcpHandleRecvSignal(String msgId) {
        if (GV.waitMsgIdSet.contains(msgId)) {
            int delta = (int) System.currentTimeMillis() - GV.waitTimeMap.get(msgId);
            Log.d("HANDLE TCP SIGNAL", msgId + ": " + delta + " (delta)");
            GV.waitMsgIdSet.remove(msgId);
            GV.waitTimeMap.remove(msgId);
        }
    }

    // DONE
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
                    this.socket.setReceiveBufferSize(8192);

                    this.in = this.socket.getInputStream();
                    this.out = this.socket.getOutputStream();
                    this.br = new BufferedReader(new InputStreamReader(in));

                    String msgRecvStr = this.br.readLine();
                    Log.v("TCP RECV MSG", msgRecvStr);
                    this.handleTcpServerMsg(NMessage.parseMsg(msgRecvStr));

                    this.br.close();
                    this.in.close();
                    this.out.close();
                    this.socket.close();
                    // Log.v(TAG, "SERVER SOCKET IO CLOSED.");
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
            }
        }
    }

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
