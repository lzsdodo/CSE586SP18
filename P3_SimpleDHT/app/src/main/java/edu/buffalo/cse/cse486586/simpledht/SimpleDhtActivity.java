package edu.buffalo.cse.cse486586.simpledht;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;


public class SimpleDhtActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_dht_main);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        GV.MY_PORT = String.valueOf((Integer.parseInt(portStr) * 2));

        GV.uiTV = (TextView) findViewById(R.id.textView1);
        GV.uiTV.setMovementMethod(new ScrollingMovementMethod());

        GV.dbUri = new Uri.Builder().scheme("content").authority(GV.URI).build();
        GV.dbCR = getContentResolver();

        findViewById(R.id.button3).setOnClickListener(
                new OnTestClickListener(GV.uiTV, getContentResolver()));

        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick (View v) {
                // LDump - @
                initInsert();
                if (testQuery("@"))
                    GV.uiTV.append("Query local success\n");
                else
                    GV.uiTV.append("Query local fail\n");
                if (testDelete("@")) {
                    GV.uiTV.append("Delete local success\n");
                } else {
                    GV.uiTV.append("Delete local fail\n");
                }
            }
        });

        findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick (View v) {
                // GDump - *
                initInsert();
                if (testQuery("*"))
                    GV.uiTV.append("Query all success\n");
                else
                    GV.uiTV.append("Query all fail\n");
                if (testDelete("*")) {
                    GV.uiTV.append("Delete all success\n");
                } else {
                    GV.uiTV.append("Delete all fail\n");
                }
            }
        });

        new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        new QueueTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        Chord node = Chord.getNode();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }

    private void initInsert() {
        ContentValues cv = new ContentValues();
        for (int i=0; i<30; i++) {
            cv.put("key", "key" + i);
            cv.put("value", "val" + i);
            GV.dbCR.insert(GV.dbUri, cv);
            cv.clear();
        }
    }

    private boolean testQuery(String key) {
        try {
            Cursor c = GV.dbCR.query(GV.dbUri, null, key, null, null);

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
            GV.dbCR.delete(GV.dbUri, key, null);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
