package edu.buffalo.cse.cse486586.groupmessenger1;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;


public class OnSendClickListener implements OnClickListener{

    static final String TAG = OnSendClickListener.class.getSimpleName();

    private EditText et;
    private final String REMOTE_ADDR = "10.0.2.2";
//    private final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};

    public OnSendClickListener(EditText editText) {
        et = editText;
    }

    @Override
    public void onClick(View view) {
        try {
            String msgToSend = et.getText().toString();
            Log.d(TAG, "Sending MSG: " + msgToSend);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
                    REMOTE_ADDR, "11108", msgToSend); // avd0
//            for (String remotePort:REMOTE_PORTS) {
//                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
//                        REMOTE_ADDR, remotePort, msgToSend);
//            }
            et.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception on Send Click.");
        }
    }

}
