package edu.buffalo.cse.cse486586.simpledynamo;

import android.os.AsyncTask;
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

    private void init() {
        try {
            this.serverSocket = new ServerSocket();
            this.serverSocket.setReuseAddress(true);
            this.serverSocket.bind(new InetSocketAddress(SERVER_PORT));
            Log.v(TAG, "Create a ServerSocket listening on: " + serverSocket.getLocalSocketAddress());
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground (Void... voids) {
        Log.v(TAG, "Start TcpServerTask.");
        this.init();

        while (true) {
            try {
                this.socket = this.serverSocket.accept();

                if (this.socket != null) {
                    //Log.v(TAG, "ACCEPTED CONN: " + this.socket.getRemoteSocketAddress().toString());
                    this.socket.setTrafficClass(0x04 | 0x10);
                    this.socket.setReceiveBufferSize(16*1024);

                    this.in = this.socket.getInputStream();
                    this.out = this.socket.getOutputStream();
                    this.br = new BufferedReader(new InputStreamReader(in));

                    String msgRecvStr = this.br.readLine();
                    Log.v("TCP RECV MSG", msgRecvStr);
                    GV.msgRecvQ.offer(MSG.parseMsg(msgRecvStr));

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

}
