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

	static final int UI = 0x00;
	static UiHandler uiHandler;
	static int uiCounter;

	public TextView mTextView;
	public ContentResolver mCR;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dynamo);
    
		TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        Log.d(TAG, "INIT");

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        GV.MY_PORT = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        Log.d(TAG, "PORT = " + GV.MY_PORT);

        GV.dbUri = new Uri.Builder().scheme("content").authority(GV.URI).build();
        this.mCR = getContentResolver();
        Log.d(TAG, "DATABASE");

        // UI
        uiCounter = 0;
        uiHandler = new UiHandler();
        Log.d(TAG, "UI HANDLER");

        // Init Dynamo Instance
        Dynamo dynamo = Dynamo.getInstance();
        Log.d(TAG, "DYNAMO");

        // Task Thread
        new TcpServer().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new TcpClient().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        // new QueueTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getContentResolver());
        Log.d(TAG, "SERVER ADN CLIENT THREAD");

        this.test();
	}

    private void test() {
        Log.e(TAG, "Testing");
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
                    mTextView.append(msg.obj + "\n");
                    break;
                default: break;
            }
        }
    }

}
