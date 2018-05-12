package edu.buffalo.cse.cse486586.simpledynamo;

import android.content.ContentResolver;
import android.os.AsyncTask;
import android.util.Log;


public class ServiceTask extends AsyncTask<ContentResolver, Void, Void> {

    static String TAG = "SERVICE";

    private ContentResolver qCR;

    @Override
    protected Void doInBackground (ContentResolver... cr) {
        Log.v(TAG, "Start ServiceTask.");

        this.qCR = cr[0];

        while (true) {

        }
    }
}
