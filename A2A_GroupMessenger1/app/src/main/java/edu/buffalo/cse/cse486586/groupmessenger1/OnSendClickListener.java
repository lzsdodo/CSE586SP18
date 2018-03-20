package edu.buffalo.cse.cse486586.groupmessenger1;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;


public class OnSendClickListener implements OnClickListener{

    static final String TAG = OnSendClickListener.class.getSimpleName();

    private EditText editText;

    public OnSendClickListener(EditText et) {
        editText = et;
    }

    @Override
    public void onClick(View view) {
        try {
            String msgToSend = editText.getText().toString() + "\n";
            editText.setText("");
            Log.d(TAG, "Sending MSG: " + msgToSend);
            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception on Send Click.");
        }
    }

}
