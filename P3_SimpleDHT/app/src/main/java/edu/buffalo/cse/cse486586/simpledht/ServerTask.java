package edu.buffalo.cse.cse486586.simpledht;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;


public class ServerTask extends AsyncTask<Void, String, Void> {

    static final String TAG = "SERVER";

    private ServerSocket serverSocket = null;
    private Socket socket = null;

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
                this.socket = this.serverSocket.accept();

                if (this.socket != null) {
                    Log.d(TAG, "Accepted connection from " + this.socket.getRemoteSocketAddress().toString());
                    socket.setReceiveBufferSize(8192); // Receive Buffer Default 8192
                    socket.setSoTimeout(1000); // Response Timeout
                    // socket.setOOBInline(true); // For sendUrgentData

                    OutputStream out = this.socket.getOutputStream();
                    InputStream in = this.socket.getInputStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    String msg = br.readLine();

                    publishProgress(msg);

                    br.close();
                    in.close();
                    out.close();
                    this.socket.close();
                    Log.d(TAG, "ServerSocket and IO Closed.");
                }
            }

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
        String recvString = strings[0].trim();
        Log.v(TAG, "RECV MSG: " + recvString);

        // Print it to UI
        GV.uiTV.append(recvString+"\n");

        // Put it to the msg receive queue
        Message msg = Message.parseMsg(recvString);
        GV.msgRecvQueue.offer(msg);

        /*
        String printStr = msg.getMsgID() + "-" + msg.getMsgType() + ": "
                + msg.getMsgContent() + "\n";
        mTextView.append(printStr);
        Log.v("UI", printStr);

        */
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.e("LOST SERVER", "SERVER TASK SHOULD NOT BREAK.");
    }


}


