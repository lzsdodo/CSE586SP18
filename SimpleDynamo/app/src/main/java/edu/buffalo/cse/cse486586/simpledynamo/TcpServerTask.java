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
                    Log.d(TAG, "ACCEPTED CONN: " + this.socket.getRemoteSocketAddress().toString());
                    this.out = this.socket.getOutputStream();
                    this.in = this.socket.getInputStream();
                    this.br = new BufferedReader(new InputStreamReader(in));
                    this.handleMsg(this.br.readLine());
                    this.closeIO();
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

    private void init() {
        try {
            this.serverSocket = new ServerSocket();
            this.serverSocket.setReuseAddress(true);
            this.serverSocket.bind(new InetSocketAddress(SERVER_PORT));
            this.serverSocket.setReceiveBufferSize(8192);   // Receive Buffer Default 8192
            Log.e(TAG, "Create a ServerSocket listening on: " + serverSocket.getLocalSocketAddress());

            this.socket = new Socket();
        } catch (IOException e1) {
            Log.e(TAG, "Can't create a ServerSocket");
            e1.printStackTrace();
        }
    }

    private void closeIO() {
        try {
            this.br.close();
            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (IOException e) {
            Log.d(TAG, "SERVER IO CLOSED ERROR.");
            e.printStackTrace();
        }
        // Log.v(TAG, "SERVER SOCKET IO CLOSED.");
    }

    private void handleMsg(String strings) {
        String strRecv = strings.trim();
        Log.v(TAG, "RECV MSG: " + strRecv);
        NMessage msgRecv = NMessage.parseMsg(strRecv);
        GV.msgRecvQueue.offer(msgRecv);
        this.refreshUI(msgRecv.toString());
    }

    private void refreshUI(String str) {
        Message uiMsg = new Message();
        uiMsg.what = SimpleDynamoActivity.UI;
        uiMsg.obj = str;
        SimpleDynamoActivity.uiHandler.sendMessage(uiMsg);
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.e("LOST SERVER", "SERVER TASK SHOULD NOT BREAK.");
    }
}
