package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;
import android.net.Uri;

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

    // UI
    public TextView mTextView;
    public EditText mEditText;

    private final int SERVER_PORT = 10000;

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
        findViewById(R.id.button1).setOnClickListener(new OnPTestClickListener(mTextView, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        mEditText = (EditText) findViewById(R.id.editText1);
        findViewById(R.id.button4).setOnClickListener(new OnSendClickListener(mEditText));

        try {
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

        // Database
        private Uri sUri = new Uri.Builder().scheme("content").authority("edu.buffalo.cse.cse486586.groupmessenger1.provider").build();asd
        private ContentResolver sCR = getContentResolver();
        private ContentValues sCV = new ContentValues();
        private int msgReceivedNum = 0;

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
            sCV.put("key", Integer.toString(msgReceivedNum));
            sCV.put("value", strReceived);
            sCR.insert(sUri, sCV);
            msgReceivedNum = msgReceivedNum + 1;
            sCV.clear();
            Log.d(TAG, "Saved MSG " + strReceived + " to DB.");
        }

        @Override
        protected void onPostExecute(Void result) {
            Log.d(TAG, "ServerTask should not break the loop.");
        }
    }
}
