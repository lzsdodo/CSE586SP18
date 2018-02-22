package edu.buffalo.cse.cse486586.groupmessenger1;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;


public class OnSendClickListener implements OnClickListener {

    static final String TAG = OnSendClickListener.class.getSimpleName();

    private TextView tv;
    private EditText et;


    public OnSendClickListener(TextView textView, EditText editText) {
        tv = textView;
        et = editText;
    }

    @Override
    public void onClick(View view) {
        try {
            Log.d(TAG, "Click Send Button.");
            // Display what we have send
            String msg = et.getText().toString();
            et.setText(""); // reset input box
            tv.append("Sending msg.\n");

//            private String[] remotePorts = new String[] {"11108", "11112", "11116", "11120", "11124"};
//            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, "11108"); // avd0
//            for (String port:remotePorts) {
//                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, port);

        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception on Send Click.");
        }
    }
}
