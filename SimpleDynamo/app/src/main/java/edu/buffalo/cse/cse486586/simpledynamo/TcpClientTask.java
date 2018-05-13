package edu.buffalo.cse.cse486586.simpledynamo;


import android.os.AsyncTask;
import android.os.Message;
import android.util.Log;

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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/*
 * Reference:
 * - Android Dev Docs:
 *      Java.IO Overview: https://developer.android.com/reference/java/io/package-summary.html
 *      InputStream: https://developer.android.com/reference/java/io/InputStream.html
 *      OutputStream: https://developer.android.com/reference/java/io/OutputStream.html
 *      BufferedReader: https://developer.android.com/reference/java/io/BufferedReader.html
 *      Socket: https://developer.android.com/reference/java/net/Socket.html
 *      ServerSocket: https://developer.android.com/reference/java/net/ServerSocket.html
 *      Java Sockets Tutorials: https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
 *      SocketException: https://developer.android.com/reference/java/net/SocketException.html
 *      ConnectTimeoutException: https://developer.android.com/reference/org/apache/http/conn/ConnectTimeoutException.html
 *      SocketTimeoutException: https://developer.android.com/reference/java/net/SocketTimeoutException.html
 * - Oracle Doce:
 *      Socket: https://docs.oracle.com/javase/9/docs/api/java/net/Socket.html
 */

public class TcpClientTask extends AsyncTask<Void, Void, Void> {

    static final String TAG = "CLIENT";
    static final String REMOTE_ADDR = "10.0.2.2";

    private OutputStream out;
    private InputStream in;

    private boolean connFlag = false;
    private boolean bocastLost = false;
    private boolean skipMsg = false;
    private Queue<NMessage> reSendMsgQueue = new LinkedList<NMessage>();;

    @Override
    protected Void doInBackground (Void... voids) {

        Log.v(TAG, "Start TcpClientTask.");

        this.connFlag = false;

        while (true) {

            if (this.bocastLost) {
                Log.e("BOCAST", "LOST: " + GV.lostPort);
                while (!this.reSendMsgQueue.isEmpty()) {
                    NMessage resendMsg = this.reSendMsgQueue.poll();
                    this.sendMsg(resendMsg);
                }
                this.bocastLost();
                this.bocastLost = false;
            }

            while (!GV.updateSendQueue.isEmpty()) {
                NMessage updateMsg = GV.updateSendQueue.poll();
                this.sendMsg(updateMsg);
            }

            if (!GV.msgSendQueue.isEmpty() && GV.updateCompleted) {
                NMessage msg = GV.msgSendQueue.poll();
                this.skipLostPort(msg);
                if (!this.skipMsg) {
                    this.sendMsg(msg);
                    this.skipMsg = false;
                }
            }
        }
    }

    private void sendMsg(NMessage msg) {
        String msgToSend = msg.toString();
        String tgtPort = msg.getTgtPort();
        Integer remotePort = Integer.parseInt(tgtPort) * 2;
        Log.e("HANDLE SEND MSG", "" + msgToSend);

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(REMOTE_ADDR, remotePort));

            this.connFlag = false;
            if (socket.isConnected()) {
                socket.setSendBufferSize(8192);    // Send Buff Default 8192
                socket.setSoTimeout(100);          // Response Timeout
                Log.v(TAG, "CONN SERVER: " + socket.getRemoteSocketAddress());

                this.in = socket.getInputStream();
                this.out = socket.getOutputStream();

                this.out.write(msgToSend.getBytes());
                this.out.flush();
                Log.v(TAG, "SENT MSG: " + msgToSend);

                this.out.close();
                this.in.close();
                while (!socket.isClosed()) { socket.close();}
                // Log.v(TAG, "CLIENTSOCKET CLOSED.");
                this.connFlag = true;
            }

        } catch (SocketTimeoutException e) {
            // Server response timeout
            Log.e(TAG, "ClientTask SocketTimeoutException");
        } catch (SocketException e) {
            // 1. IO on socket after closed by yourself; 2. Connect reset by peer / Connection reset.
            Log.e(TAG, "ClientTask SocketException");
        } catch (EOFException e) {
            // 当输入过程中意外到达文件或流的末尾时，抛出此异常。
            Log.e(TAG, "ClientTask EOFException");
        } catch (StreamCorruptedException e) {
            Log.e(TAG, "ClientTask StreamCorruptedException");
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e(TAG, "ClientTask IOException");
        } catch (Exception e) {
            Log.e(TAG, "ClientTask Exception");
        } finally {
            if (!this.connFlag) {
                Log.e(TAG, "DISCONN DEVICE: " + tgtPort);
                this.refreshUI("DISCONN DEVICE: " + tgtPort);
                this.handleDisconn(msg);
            }
        }
    }

    private void handleDisconn(NMessage msg) {
        Log.e(TAG, "HANDLE DISCONN: " + msg.getTgtPort());

        if (GV.updateCompleted) {
            Dynamo dynamo = Dynamo.getInstance();
            GV.lostPort = msg.getTgtPort();
            this.bocastLost = true;

            String tgtId = dynamo.genHash(GV.lostPort);

            switch (msg.getMsgType()) {
                case INSERT:
                    if (dynamo.isLastNode(tgtId, "INSERT")) {
                        Log.e("HANDLE DISCONN", "MISS LAST INSERT");
                        msg.setMsgType(NMessage.TYPE.UPDATE_INSERT);
                        GV.notifySuccNode.offer(msg);
                    } else {
                        Log.e("HANDLE DISCONN", "MISS FIRST/SECOND INSERT");
                        msg.setTgtPort(dynamo.getSuccPortOfPort(GV.lostPort));
                        msg.setSndPort(GV.MY_PORT);
                        this.reSendMsgQueue.offer(msg);
                    }
                    break;

                case DELETE:
                    if (dynamo.isLastNode(tgtId, "DELETE")) {
                        Log.e("HANDLE DISCONN", "MISS LAST DELETE");
                        msg.setMsgType(NMessage.TYPE.UPDATE_DELETE);
                        GV.notifySuccNode.offer(msg);
                    } else {
                        Log.e("HANDLE DISCONN", "MISS FIRST/SECOND INSERT");
                        msg.setTgtPort(dynamo.getSuccPortOfPort(GV.lostPort));
                        msg.setSndPort(GV.MY_PORT);
                        this.reSendMsgQueue.offer(msg);
                    }
                    break;

                case QUERY:
                    if (dynamo.isLastNode(tgtId, "QUERY")) {
                        Log.e("HANDLE DISCONN", "MISS LAST QUERY");
                        msg.setTgtPort(GV.MY_PORT);
                        this.reSendMsgQueue.offer(msg);
                    } else {
                        Log.e("HANDLE DISCONN", "MISS FIRST/SECOND QUERY");
                        msg.setTgtPort(dynamo.getPredPort());
                        msg.setSndPort(GV.MY_PORT);
                        this.reSendMsgQueue.offer(msg);
                    }
                    break;

                case RECOVERY:
                    GV.updateSendQueue.offer(msg);
                    break;

                default:
                    Log.e("DISCONN ERROR", msg.toString());
                    break;
            }

        } else {

            switch (msg.getMsgType()) {
                case RECOVERY:
                    Log.e("HANDLE DISCONN", "INIT RECOVERY");
                    GV.updateSendQueue.offer(msg);
                    break;

                default:
                    Log.e("DISCONN ERROR", "IN UPDATE STATUS: " + msg.toString());
                    break;
            }
        }
    }

    private void bocastLost() {
        ArrayList<String> ports = Dynamo.PORTS;
        ports.remove(GV.MY_PORT);
        ports.remove(GV.lostPort);
        Log.e("BOCAST LOST", "LOCAL PORT: " + GV.MY_PORT +
                "; LOST PORT: " + GV.lostPort + "; PORTS: " + ports.toString());
        for (String port: ports) {
            NMessage msg = new NMessage(NMessage.TYPE.LOST,
                    GV.lostPort, port, "$$$", "$$$");
            this.sendMsg(msg);
        }

    }

    private void skipLostPort(NMessage msg) {
        if (GV.lostPort!=null) {

            Dynamo dynamo = Dynamo.getInstance();
            String lostId = dynamo.genHash(GV.lostPort);

            if (msg.getTgtPort().equals(GV.lostPort)) {
                switch (msg.getMsgType()) {
                    case INSERT:
                        if (dynamo.isLastNode(lostId, "INSERT")) {
                            msg.setMsgType(NMessage.TYPE.UPDATE_INSERT);
                            GV.notifySuccNode.offer(msg);
                            this.skipMsg = true;
                        } else {
                            msg.setTgtPort(dynamo.getSuccPortOfPort(GV.lostPort));
                            msg.setSndPort(GV.MY_PORT);
                        }
                        break;

                    case DELETE:
                        if (dynamo.isLastNode(lostId, "DELETE")) {
                            msg.setMsgType(NMessage.TYPE.UPDATE_DELETE);
                            GV.notifySuccNode.offer(msg);
                            this.skipMsg = true;
                        } else {
                            msg.setTgtPort(dynamo.getSuccPortOfPort(GV.lostPort));
                            msg.setSndPort(GV.MY_PORT);
                        }
                        break;

                    case QUERY:
                        if (dynamo.isLastNode(lostId, "QUERY")) {
                            msg.setTgtPort(GV.MY_PORT);
                        } else {
                            msg.setTgtPort(dynamo.getPredPort());
                        }
                        msg.setSndPort(GV.MY_PORT);
                        break;

                    default:
                        break;
                }
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
