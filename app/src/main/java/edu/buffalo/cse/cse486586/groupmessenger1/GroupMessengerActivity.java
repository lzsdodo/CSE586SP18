package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();

    // Related to UI
    public TextView mTextView;
    public EditText mEditText;
    public Button pTestBtn;
    public Button sendBtn;


    // Related to TCP
    static final int SERVER_PORT = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        mTextView = (TextView) findViewById(R.id.textView1);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        pTestBtn = (Button) findViewById(R.id.button1);
        pTestBtn.setOnClickListener(new OnPTestClickListener(mTextView, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        mEditText = (EditText) findViewById(R.id.editText1);
        sendBtn = (Button) findViewById(R.id.button4);
        sendBtn.setOnClickListener(new OnSendClickListener(mEditText));

        try {
            TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            Log.d(TAG, "Create a ServerSocket listening on: " + serverSocket.getLocalSocketAddress());
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Can't create a ServerSocket");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            ServerSocket serverSocket = sockets[0];
            Socket socket = null;

            try {
                while (true) {
                    socket = serverSocket.accept();
                    if (socket != null) {
                        Log.d(TAG, "Accepted connection from " + socket.getRemoteSocketAddress().toString());

                        OutputStream out = socket.getOutputStream();
                        InputStream in = socket.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        String msg = br.readLine();

                        publishProgress(msg);

                        br.close();
                        in.close();
                        out.close();
                        socket.close();
                        Log.d(TAG, "ServerSocket and IO Closed.");
                    }
                }
            } catch (UnknownHostException e) {
                Log.e(TAG, "ServerTask UnknownHostException");
            } catch (IOException e) {
                Log.e(TAG, "ServerTask IOException");
            } catch (Exception e) {
                Log.d(TAG, "ServerTask Exception.");
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... strings) {
            String strReceived = strings[0].trim();
            Log.d(TAG, "Received MSG: " + strReceived);
            mTextView.append(strReceived + "\n");

            // Saved msg to Database
            // ...
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "ServerTask should not break the loop.");
        }
    }
}
