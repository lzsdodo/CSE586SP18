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
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class TcpClientTask extends AsyncTask<Void, Void, Void> {

    static final String TAG = "CLIENT";
    static final String REMOTE_ADDR = "10.0.2.2";

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private boolean connFlag;


    @Override
    protected Void doInBackground (Void... voids) {

        Log.v(TAG, "Start TcpClientTask.");

        this.init();

        while (true) {

            while (!GV.msgSendQueue.isEmpty()) {

                NMessage msg = GV.msgSendQueue.poll();
                String msgToSend = msg.toString();
                String tgtPort = msg.getTgtPort();
                Integer remotePort = Integer.parseInt(tgtPort) * 2;
                Log.e("HANDLE SEND MSG", "" + msgToSend);

                try {
                    this.socket.connect(new InetSocketAddress(REMOTE_ADDR, remotePort));


                    this.connFlag = false;
                    if (this.socket.isConnected()) {
                        this.socket.setSendBufferSize(8192);    // Send Buff Default 8192
                        this.socket.setSoTimeout(100);          // Response Timeout
                        Log.v(TAG, "CONN SERVER: " + this.socket.getRemoteSocketAddress());

                        this.in = socket.getInputStream();
                        this.out = socket.getOutputStream();

                        this.out.write(msgToSend.getBytes());
                        this.out.flush();
                        Log.v(TAG, "SENT MSG: " + msgToSend);

                        this.closeIO();
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
                        this.handleDisconn(tgtPort);
                    }
                }
            }

        }
    }

    private void init() {
        this.socket = new Socket();
        this.connFlag = false;
        // Log.v(TAG, "CLIENT SOCKET IO CLOSED.");
    }

    private void closeIO() {
        try {
            this.out.close();
            this.in.close();
            while (!socket.isClosed()) {
                this.socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleDisconn(String tgtPort) {
        Log.e(TAG, "DISCONN DEVICE: " + tgtPort);
        String str = "DISCONN DEVICE: " + tgtPort;
        this.refreshUI(str);
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
