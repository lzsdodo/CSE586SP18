package edu.buffalo.cse.cse486586.groupmessenger2;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

/*
 * Reference:
 * - Android Dev Docs:
 *      Button: https://developer.android.com/reference/android/widget/Button.html
 *      View.OnClickListener: https://developer.android.com/reference/android/view/View.OnClickListener.html
 */

public class OnSendClickListener implements OnClickListener{

    static final String TAG = OnSendClickListener.class.getSimpleName();

    private EditText editText;

    public OnSendClickListener(EditText editText) {
        this.editText = editText;
    }

    @Override
    public void onClick(View view) {

        try {
            String editContent = this.editText.getText().toString().trim();
            this.editText.setText("");
            Log.d(TAG, "Sending content with heartbeat: " + editContent);

            Message initMsg = new Message(editContent, Message.TYPE.INIT, Message.TARGET_TYPE.GROUP);
            GV.msgWaitList.add(initMsg.getMsgID());

            GV.msgSendQueue.offer(initMsg);
            GV.msgSendQueue.offer(new Message(Message.TYPE.HEART, GV.GROUP_PID));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
