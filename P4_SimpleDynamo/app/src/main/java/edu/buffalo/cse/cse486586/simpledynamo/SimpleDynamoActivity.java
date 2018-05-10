package edu.buffalo.cse.cse486586.simpledynamo;

import android.annotation.SuppressLint;
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

/*
  * Reference:
 * - Android Dev Docs:
 *      - Build Your First App: https://developer.android.com/training/basics/firstapp/index.html
 *      - App Fundamentals: https://developer.android.com/guide/components/fundamentals.html
 *      - Log: https://developer.android.com/reference/android/util/Log.html
 *      - UI Overview: https://developer.android.com/guide/topics/ui/overview.html
 *      - View: https://developer.android.com/reference/android/view/View.html
 *      - EditText: https://developer.android.com/reference/android/widget/EditText.html
 *      - TextView: https://developer.android.com/reference/android/widget/TextView.html
 *      - The Activity Lifecycle: https://developer.android.com/guide/components/activities/activity-lifecycle.html
 *      - AsyncTask: https://developer.android.com/reference/android/os/AsyncTask.html
 *      - Handler: https://developer.android.com/reference/android/os/Handler.html
 *      - Message: https://developer.android.com/reference/android/os/Message.html
 *      - Button: https://developer.android.com/reference/android/widget/Button.html
 *      - View.OnClickListener: https://developer.android.com/reference/android/view/View.OnClickListener.html
 * - Previous Project:
 *      - GroupMessenger 2B and SimpleDHT
 */

public class SimpleDynamoActivity extends Activity {

	static final int UI = 0x00;
	static Handler uiHandler;
    static int uiCounter = 0;

    public TextView mTextView;
    public ContentResolver mCR;

	@SuppressLint("HandlerLeak")
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_simple_dynamo);
    
		mTextView = (TextView) findViewById(R.id.textView1);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        GV.MY_PORT = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        Log.e("MAIN", GV.MY_PORT + "" );


        GV.dbUri = new Uri.Builder().scheme("content").authority(GV.URI).build();
        this.mCR = getContentResolver();

        this.uiHandler = new Handler() {
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
        };

        // Task Thread
        new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        // new QueueTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getContentResolver());
        this.test();
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
	    // The test will kill process directly, so we can not handle stop on this.
	}

	public void test() {
        Log.d("TEST", "Test...");
        Dynamo dynamo = Dynamo.getInstance();
    }

}
