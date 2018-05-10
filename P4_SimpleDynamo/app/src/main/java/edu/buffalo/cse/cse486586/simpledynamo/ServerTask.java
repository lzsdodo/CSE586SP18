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


public class ServerTask extends AsyncTask<Void, String, Void> {

    static final String TAG = "SERVER";

    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private OutputStream out = null;
    private InputStream in = null;
    private BufferedReader br = null;

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            this.serverSocket = new ServerSocket();
            this.serverSocket.setReuseAddress(true);
            this.serverSocket.bind(new InetSocketAddress(GV.SERVER_PORT));
            this.socket = new Socket();
            Log.e(TAG, "Create a ServerSocket listening on: " + serverSocket.getLocalSocketAddress());
        } catch (IOException e1) {
            Log.e(TAG, "Can't create a ServerSocket");
            e1.printStackTrace();
        }

        try {
            while (true) {
                // Receive message
                this.socket = this.serverSocket.accept();

                if (this.socket != null) {
                    Log.d(TAG, "Accepted connection from " + this.socket.getRemoteSocketAddress().toString());
                    this.socket.setReceiveBufferSize(8192); // Receive Buffer Default 8192
                    this.socket.setSoTimeout(300); // Response Timeout

                    this.out = this.socket.getOutputStream();
                    this.in = this.socket.getInputStream();
                    this.br = new BufferedReader(new InputStreamReader(in));

                    publishProgress(this.br.readLine());

                    this.br.close();
                    this.in.close();
                    this.out.close();
                    this.socket.close();
                    Log.d(TAG, "ServerSocket and IO Closed.");
                }
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

        return null;
    }

    @Override
    protected void onProgressUpdate(String... strings) {
        String recvStr = strings[0].trim();
        Log.v(TAG, "RECV MSG: " + recvStr);

        // Put it to the msg receive queue
        NMessage msg = NMessage.parseMsg(recvStr);
        GV.msgRecvQueue.offer(msg);

        // Print it to UI
        Message uiMsg = new Message();
        uiMsg.what = SimpleDynamoActivity.UI;
        uiMsg.obj = msg.toString();
        SimpleDynamoActivity.uiHandler.sendMessage(uiMsg);
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.e("LOST SERVER", "SERVER TASK SHOULD NOT BREAK.");
    }

}