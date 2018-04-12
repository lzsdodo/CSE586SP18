package edu.buffalo.cse.cse486586.simpledht;


import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class SimpleDhtActivity extends Activity {

    static final int UI = 0x00;
    static final int CLEAN = 0x01;
    static Handler uiHandler;

    public TextView mTextView;
    public ContentResolver mCR;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        GV.MY_PORT = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);

        // UI
        mTextView = (TextView) findViewById(R.id.textView1);
        mTextView.setMovementMethod(new ScrollingMovementMethod());

        // Database
        GV.dbUri = new Uri.Builder().scheme("content").authority(GV.URI).build();
        this.mCR = getContentResolver();

        // Test Insert, Query and Delete One
        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(mTextView, getContentResolver()));
        // LDump - @
        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick (View v) {
                testInsert();
                if (testQuery("@")) mTextView.append("Query local success\n");
                else mTextView.append("Query local fail\n");
                if (testDelete("@"))  mTextView.append("Delete local success\n");
                else mTextView.append("Delete local fail\n");
            }
        });

        // GDump - *
        findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick (View v) {
                testInsert();
                if (testQuery("*")) mTextView.append("Query all success\n");
                else mTextView.append("Query all fail\n");
                if (testDelete("*"))  mTextView.append("Delete all success\n");
                else mTextView.append("Delete all fail\n");
            }
        });

        this.uiHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case UI:
                        mTextView.append(msg.obj + "\n"); break;
                    case CLEAN:
                        mTextView.setText("Update UI\n"); break;
                    default: break;
                }
            }
        };

        // Task Thread
        new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new QueueTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getContentResolver());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

    // Leave chord when app stop
    @Override
    protected void onStop () {
        super.onStop();
        Chord chord = Chord.getInstance();
        chord.leave();
    }

    // Support test functions
    private void testInsert() {
        ContentValues cv = new ContentValues();
        for (int i=0; i<30; i++) {
            cv.put("key", "key" + i);
            cv.put("value", "val" + i);
            this.mCR.insert(GV.dbUri, cv);
            cv.clear();
        }
    }

    private boolean testQuery(String key) {
        try {
            Cursor c = this.mCR.query(GV.dbUri, null, key, null, null);

            c.moveToFirst();
            while (!c.isAfterLast()) {
                String k = c.getString(c.getColumnIndex("key"));
                String v = c.getString(c.getColumnIndex("value"));
                Log.v("QUERY ALL", k + ", " + v);
                c.moveToNext();
            }
            c.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean testDelete(String key) {
        try {
            this.mCR.delete(GV.dbUri, key, null);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
