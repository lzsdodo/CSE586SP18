package edu.buffalo.cse.cse486586.groupmessenger1;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;


public class ClientTask extends AsyncTask<String, Void, Void> {
    // Usage: new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend);

    static final String TAG = ClientTask.class.getSimpleName();

    // for (String remotePort:REMOTE_PORTS) {}
    static final String[] REMOTE_PORTS = {"11108", "11112", "11116", "11120", "11124"};

    @Override
    protected Void doInBackground(String... params) {
        String remoteAddr = params[0];
        String remotePort = params[1];
        String msgToSend = params[2];

        try {
            Socket socket = new Socket(remoteAddr, Integer.parseInt(remotePort));

            if (socket.isConnected()) {
                socket.setSendBufferSize(128);
                socket.setKeepAlive(true);
                socket.setSoTimeout(300);
                Log.d(TAG, "Connected Server: " + socket.getRemoteSocketAddress());

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                out.write(msgToSend.getBytes());
                Log.d(TAG, "MSG \"" + msgToSend + "\" Sent.");

                out.close();
                in.close();
                socket.close();
                Log.d(TAG, "ClientSocket Closed.");
            }
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e(TAG, "ClientTask IOException");
        } catch (Exception e) {
            Log.e(TAG, "ClientTask Exception");
        }
        return null;
    }
}
