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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

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
        mTextView.append("PRED: " + GV.PRED_PORT + "; SELF: " + GV.MY_PORT + "; SUCC: " + GV.SUCC_PORT +
                "\nREPLICAS PORTS: " + GV.REPLICA_PORTS.toString());

        ArrayList<String> ports = new ArrayList<String>();
        ports.addAll(Dynamo.PORTS);
        ports.remove(GV.MY_PORT);
        for (String port: ports) {
            GV.backupMsgQMap.put(port, new LinkedBlockingQueue<MSG>());
            GV.msgSendQ.offer(new MSG(MSG.TYPE.RESTART, GV.MY_PORT, port));
        }

        //this.test();
	}

    private void test() {
        Log.e(TAG, "Testing");
        this.testDynamo();
        this.testTCP();
    }

    private void testDynamo() {
	    String TAG = "TEST DYNAMO";
        String key = "xo1R4fhe37p0ee81msccP3tRxB2LrNKJ"; // [5554, 5558, 5560]
        //String key = "5eJV8lT1wRoVMkHolpudrFlMunYGrWod"; // [5556, 5554, 5558]
		//String key = "XytP6LeFFQLUmZxLw6xoYaaoe6nuqcIK"; // [5558, 5560, 5562]
        //String key = "hDjuKlGct3lJt9PaR0EaUlEVSevjrYYG"; // [5560, 5562, 5556]
        //String key = "NywLQ4F6h0DDhxy8hc0vHWtJfCZUNEGg"; // [5562, 5556, 5554]
        String kid = Dynamo.genHash(key);
        Log.e(TAG, "Key<>Kid: " + key + " <> " + kid);
        ArrayList<String> perferIdList = Dynamo.getPerferIdList(kid);
        ArrayList<String> perferPortList = Dynamo.getPerferPortList(perferIdList);
        Log.e(TAG, "Perfer Id List: " + perferIdList.toString());
        Log.e(TAG, "Perfer Node List: " + perferPortList.toString());
        Log.e(TAG, "My Port: " + GV.MY_PORT);
        Log.e(TAG, "First Port: " + Dynamo.getFirstPort(kid));
        Log.e(TAG, Dynamo.isFirstNode(kid) + "");
        Log.e(TAG, Dynamo.getSuccPortOfPort(Dynamo.getFirstPort(kid)) +
                "=" + Dynamo.getPredPortOfPort(Dynamo.getLastPort(kid)));
        Log.e(TAG, "Last Port: " + Dynamo.getLastPort(kid));
        Log.e(TAG, Dynamo.isLastNode(kid) + "");
    }

    private void testTCP() {
        String TAG = "TEST TCP";
        long lastTime = System.currentTimeMillis();
        Log.e(TAG, "test: " + lastTime );
        while (true) {
            if (System.currentTimeMillis() > lastTime + 3000) {
                Dynamo dynamo = Dynamo.getInstance();
                GV.msgSendQ.offer(new MSG(MSG.TYPE.INSERT, GV.MY_PORT, GV.MY_PORT,
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
                    if (uiCounter > 15) {
                        uiCounter = 0;
                        mTextView.setText("CLEAR UI...\n");
                    }
                    mTextView.append(msg.obj + "\n");
                    break;
                default: break;
            }
        }
    }

}
