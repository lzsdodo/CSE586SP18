package edu.buffalo.cse.cse486586.simpledynamo;


import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Queue;

public class TcpClientTask extends AsyncTask<Void, Void, Void> {

    static final String TAG = "CLIENT";
    static final String REMOTE_ADDR = "10.0.2.2";

    private Socket socket;
    private InputStream in;
    private OutputStream out;

    private boolean isConnected;
    private boolean skipThisMsg = false;
    private Queue<NMessage> reSendMsgQueue = new LinkedList<NMessage>();

    @Override
    protected Void doInBackground (Void... voids) {

        Log.v(TAG, "Start TcpClientTask.");

        this.isConnected = false;

        while (true) {

            while (!GV.updateSendQueue.isEmpty()) {
                NMessage updateMsg = GV.updateSendQueue.poll();
                Log.e("SEND UPDATE MSG",  updateMsg.toString());
                this.sendMsg(updateMsg);
            }

            while (!GV.msgSendQueue.isEmpty()) {
                NMessage msg = GV.msgSendQueue.poll();
                Log.e("SEND NORMAL MSG",  msg.toString());
                if (GV.lostPort!=null) {
                    this.skipLostPort(msg);
                }
                if (!this.skipThisMsg) {
                    this.sendMsg(msg);
                }
                this.skipThisMsg = false;

            }

        }
    }

    private void sendMsg(NMessage msg) {
        GV.waitMsgId = msg.getMsgID();
        String msgToSend = msg.toString();
        String tgtPort = msg.getTgtPort();
        Integer remotePort = Integer.parseInt(tgtPort) * 2;
        Log.e("HANDLE SEND MSG", "" + msgToSend);

        if (GV.lostPort!=null) {
            if (GV.lostPort.equals(tgtPort)) {
                this.skipLostPort(msg);
            }
        }

        try {
            this.socket = new Socket();
            this.isConnected = false;
            this.socket.connect(new InetSocketAddress(REMOTE_ADDR, remotePort), 100);


            if (this.socket.isConnected()) {
                Log.v("CONN SERVER", this.socket.getRemoteSocketAddress() + "");
                this.socket.setTrafficClass(0x04 | 0x10);
                this.socket.setSendBufferSize(8192);    // Send Buff Default 8192
                this.socket.setSoTimeout(200);          // Response Timeout

                this.in = this.socket.getInputStream();
                this.out = this.socket.getOutputStream();
                this.out.write(msgToSend.getBytes());
                this.out.flush();
                Log.d("SEND MSG", "OUT: " + msgToSend + "; IN: " + this.in.read());

                this.isConnected = true;
            }

        } catch (ConnectTimeoutException e) {
            Log.e(TAG, "ClientTask ConnectTimeoutException");
        } catch (SocketTimeoutException e) {
            // Server response timeout
            Log.e(TAG, "ClientTask SocketTimeoutException");
        } catch (SocketException e) {
            // 1. IO on socket after closed by yourself;
            // 2. Connect reset by peer / Connection reset.
            Log.e(TAG, "ClientTask SocketException");
        } catch (EOFException e) {
            // 当输入过程中意外到达文件或流的末尾时，抛出此异常。
            Log.e(TAG, "ClientTask EOFException");
        } catch (StreamCorruptedException e) {
            Log.e(TAG, "ClientTask StreamCorruptedException");
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e(TAG, "CONNECTED IOException");

        } finally {

            try {
                this.in.close();
                this.out.close();
                while (!this.socket.isClosed()) {
                    this.socket.close();
                }
                Log.v(TAG, "CLIENTSOCKET CLOSED.");
            } catch (IOException e) {
                Log.e(TAG, "CLOSE IOException");
            }

            if (!this.isConnected) {
                Log.e(TAG, "DISCONN DEVICE: " + tgtPort);
                this.refreshUI("DISCONN DEVICE: " + tgtPort);
                this.handleDisconn(msg);
            }
        }
    }


    private void handleDisconn(NMessage msg) {
        // TODO
        Log.e(TAG, "HANDLE DISCONN: " + msg.getTgtPort());

        Dynamo dynamo = Dynamo.getInstance();
        GV.lostPort = msg.getTgtPort();
        String tgtId = dynamo.genHash(GV.lostPort);

        switch (msg.getMsgType()) {
            case INSERT:
                break;
            case DELETE:
                break;
            case QUERY:
                break;

            default:
                Log.e("DISCONN ERROR", msg.toString());
                break;
        }
    }

    private void skipLostPort(NMessage msg) {
        Dynamo dynamo = Dynamo.getInstance();
        String lostId = dynamo.genHash(GV.lostPort);

        if (msg.getTgtPort().equals(GV.lostPort)) {
            Log.e("SKIP LOST PORT", msg.getMsgType().name() + " TO " + GV.lostPort +
                    "\nIn " + dynamo.portsOfPerferIdList(dynamo.getPerferIdList(dynamo.genHash(msg.getMsgKey()))));

            switch (msg.getMsgType()) {
                case INSERT:
                    if (dynamo.isLastNode(lostId, "INSERT")) {
                        msg.setMsgType(NMessage.TYPE.UPDATE_INSERT);
                        GV.notifySuccNode.add(msg);
                        this.skipThisMsg = true;
                    } else {
                        msg.setTgtPort(dynamo.getSuccPortOfPort(GV.lostPort));
                        msg.setSndPort(GV.MY_PORT);
                    }
                    break;

                case DELETE:
                    if (dynamo.isLastNode(lostId, "DELETE")) {
                        msg.setMsgType(NMessage.TYPE.UPDATE_DELETE);
                        GV.notifySuccNode.add(msg);
                        this.skipThisMsg = true;
                    } else {
                        msg.setTgtPort(dynamo.getSuccPortOfPort(GV.lostPort));
                        msg.setSndPort(GV.MY_PORT);
                    }
                    break;

                case QUERY:
                    if (dynamo.isLastNode(lostId, "QUERY")) {
                        Log.e("SKIP NODE", "LAST NODE AND SEND QUERY TO SELF");
                        msg.setTgtPort(GV.MY_PORT);
                    } else {
                        Log.d("QUERY WITH LOST", msg.toString() + "\nMY PORTS: " + GV.MY_PORT_INFO);
                        msg.setTgtPort(dynamo.getPredPortOfPort(GV.lostPort));
                    }
                    msg.setSndPort(GV.MY_PORT);
                    break;

                default:
                    break;
            }
        }

    }

    private void refreshUI(String str) {
        Message uiMsg = new Message();
        uiMsg.what = SimpleDynamoActivity.UI;
        uiMsg.obj = str;
        SimpleDynamoActivity.uiHandler.sendMessage(uiMsg);
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.e("LOST CLIENT", "CLIENT TASK SHOULD NOT BREAK.");
    }

}
