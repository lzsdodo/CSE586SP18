package edu.buffalo.cse.cse486586.simpledht;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

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

public class SimpleDhtActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        GV.MY_PORT = String.valueOf((Integer.parseInt(portStr) * 2));

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(tv, getContentResolver()));

        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick (View v) {
                // LDump - @
            }
        });

        findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick (View v) {
                // GDump - *
            }
        });

        new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new QueueTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getContentResolver());

        Chord node = Chord.getNode();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<Void, String, Void> {

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
            String recvString = strings[0].trim();

            Log.d(TAG, "RECV MSG: " + recvString);

            /*
            // Print it to UI
            String printStr = msg.getMsgID() + "-" + msg.getMsgType() + ": "
                    + msg.getMsgContent() + "\n";
            mTextView.append(printStr);
            Log.d("UI", printStr);

            // Put it to the msg receive queue
            GV.msgRecvQueue.offer(msg);
            */
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.e("LOST SERVER", "SERVER TASK SHOULD NOT BREAK.");
        }

    }
}
