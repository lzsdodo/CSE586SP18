package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class SimpleDynamoActivity extends Activity {

	static String TAG = "MAIN";
    static final String URI = "edu.buffalo.cse.cse486586.simpledynamo.provider";
	static final int UI = 0x00;
	static Handler uiHandler;
	static int uiCounter;

	public TextView mTextView;
	public ContentResolver mCR;
	public Dynamo dynamo;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dynamo);

        this.mTextView = (TextView) findViewById(R.id.textView1);
        this.mTextView.setMovementMethod(new ScrollingMovementMethod());
        this.refreshUI("INIT UI");

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        GV.MY_PORT = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        this.refreshUI("INIT MY PORT: " + GV.MY_PORT);

        // Init Dynamo Instance
        this.dynamo = Dynamo.getInstance();
        this.refreshUI("INIT DYNAMO");

        GV.dbUri = new Uri.Builder().scheme("content").authority(URI).build();
        this.mCR = getContentResolver();
        this.refreshUI("INIT DATABASE");

        // UI
        uiCounter = 0;
        uiHandler = new UiHandler();
        this.refreshUI("INIT UI HANDLER");

        // Task Thread
        new ServiceTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getContentResolver());
        new TcpServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new TcpClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        this.refreshUI("INIT TASK");

        // Update Lost Data
        this.updateLostData();

        //this.test();
	}

	private void updateLostData() {
        // Send msg to neighbour
        this.refreshUI("RECOVER: " + "PRED=" + this.dynamo.getPredPort() +
                "; SELF=" + GV.MY_PORT + "\n" + "; SUCC=" + this.dynamo.getSuccPort() + "\n");

        GV.updateSendQueue.offer(new NMessage(NMessage.TYPE.RECOVERY,
                GV.MY_PORT, this.dynamo.getSuccPort(),"$$$", "$$$"));

        GV.updateSendQueue.offer(new NMessage(NMessage.TYPE.RECOVERY,
                GV.MY_PORT, this.dynamo.getPredPort(), "$$$", "$$$"));
    }

    private void test() {
        Log.e(TAG, "Testing");
        this.testTCP();
    }

    private void testTCP() {
        long lastTime = System.currentTimeMillis();
        Log.e(TAG, "test: " + lastTime );
        while (true) {
            if (System.currentTimeMillis() > lastTime + 3000) {
                Dynamo dynamo = Dynamo.getInstance();
                GV.msgSendQueue.offer(new NMessage(
                        NMessage.TYPE.INSERT,
                        dynamo.getPort(), dynamo.getPort(),
                        "xo1R4fhe37p0ee81msccP3tRxB2LrNKJ", "TEST_VALUE"));
                break;
            }
        }
        Log.e(TAG, "test finish: " + System.currentTimeMillis());
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_dynamo, menu);
		return true;
	}
	
	public void onStop() {
        super.onStop();
	    Log.v("Test", "onStop()");
	}

    public void refreshUI(String str) {
        this.mTextView.append(str + "\n");
    }

	public class UiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UI:
                    uiCounter++;
                    if (uiCounter > 50) {
                        mTextView.setText("CLEAR UI...\n");
                    }
                    refreshUI(msg.obj.toString() + "\n");
                    break;
                default: break;
            }
        }
    }

}
