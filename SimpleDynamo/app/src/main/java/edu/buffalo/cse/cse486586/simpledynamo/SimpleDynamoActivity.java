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

        Log.e("LIFE CYCLE", "onCreate");

        this.mTextView = (TextView) findViewById(R.id.textView1);
        this.mTextView.setMovementMethod(new ScrollingMovementMethod());
        mTextView.append("INIT UI\n");

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        GV.MY_PORT = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        mTextView.append("INIT MY PORT: " + GV.MY_PORT + "\n");

        // Init Dynamo Instance
        this.dynamo = Dynamo.getInstance();
        mTextView.append("INIT DYNAMO\n");

        GV.dbUri = new Uri.Builder().scheme("content").authority(URI).build();
        this.mCR = getContentResolver();
        mTextView.append("INIT DATABASE\n");

        // UI
        uiCounter = 0;
        uiHandler = new UiHandler();
        mTextView.append("INIT UI HANDLER\n");

        // Task Thread
        new TcpServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new TcpClientTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new ServiceTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getContentResolver());
        mTextView.append("INIT SERVER CLIENT SERVICE TASK\n");

        // PORT INFO
        GV.MY_PORT_INFO = "PRED: " + dynamo.getPredPort() + "; SELF: " + GV.MY_PORT + "; SUCC: " + dynamo.getSuccPort();
        mTextView.append(GV.MY_PORT_INFO + "\n");

        // Update Lost Data
        this.updateLostData();

        //this.test();
	}

	private void updateLostData() {
        // Send msg to neighbour
        GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.RECOVERY,
                this.dynamo.getPort(), this.dynamo.getSuccPort(), "$", "$"));
        GV.msgSendQueue.offer(new NMessage(NMessage.TYPE.RECOVERY,
                this.dynamo.getPort(), this.dynamo.getPredPort(), "$", "$"));
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

    public class UiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UI:
                    uiCounter++;
                    if (uiCounter > 30) {
                        mTextView.setText("CLEAR UI...\n");
                    }
                    mTextView.append(msg.obj + "\n");
                    break;
                default: break;
            }
        }
    }

}
