package edu.buffalo.cse.cse486586.simpledht;

import android.os.AsyncTask;
import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/*
 * Reference:
 * - Android Dev Docs:
 *      Java.IO Overview: https://developer.android.com/reference/java/io/package-summary.html
 *      InputStream: https://developer.android.com/reference/java/io/InputStream.html
 *      OutputStream: https://developer.android.com/reference/java/io/OutputStream.html
 *      BufferedReader: https://developer.android.com/reference/java/io/BufferedReader.html
 *      Socket: https://developer.android.com/reference/java/net/Socket.html
 *      ServerSocket: https://developer.android.com/reference/java/net/ServerSocket.html
 *      Java Sockets Tutorials: https://docs.oracle.com/javase/tutorial/networking/sockets/index.html
 *      SocketException: https://developer.android.com/reference/java/net/SocketException.html
 *      ConnectTimeoutException: https://developer.android.com/reference/org/apache/http/conn/ConnectTimeoutException.html
 *      SocketTimeoutException: https://developer.android.com/reference/java/net/SocketTimeoutException.html
 * - Oracle Doce:
 *      Socket: https://docs.oracle.com/javase/9/docs/api/java/net/Socket.html
 */


public class ClientTask extends AsyncTask<String, Integer, Void> {
    // Usage: new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, remotePort);

    static final String TAG = "CLIENT";

    @Override
    protected Void doInBackground(String... params) {
        String msgToSend = params[0];
        String remotePort = params[1];

        boolean connFlag = false;

        try {
            SocketAddress socketAddr = new InetSocketAddress(
                    GV.REMOTE_ADDR, Integer.parseInt(remotePort));
            Socket socket = new Socket();
            socket.connect(socketAddr); // Connect Timeout
            // socket.setOOBInline(true); // For sendUrgentData

            if (socket.isConnected()) {
                Log.d(TAG, "CONNECTED SERVER: " + socket.getRemoteSocketAddress());
                socket.setSendBufferSize(8192); // Send Buff Default 8192
                socket.setSoTimeout(500); // Response Timeout

                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                Log.d(TAG, "MSG: " + msgToSend + " SENT.");
                out.write(msgToSend.getBytes());
                out.flush();

                out.close();
                in.close();
                while (!socket.isClosed()) {
                    socket.close();
                }
                connFlag = true;
                Log.d(TAG, "CLIENTSOCKET CLOSED.");
            }
        }
        catch (SocketTimeoutException e) {
            // Server response timeout
            Log.e(TAG, "ClientTask SocketTimeoutException");
        } catch (SocketException e) {
            // 1. IO on socket after closed by yourself.
            // 2. Connect reset by peer / Connection reset
            Log.e(TAG, "ClientTask SocketException");
        } catch (EOFException e) {
            // 当输入过程中意外到达文件或流的末尾时，抛出此异常。
            Log.e(TAG, "ClientTask EOFException");
        } catch (StreamCorruptedException e) {
            Log.e(TAG, "ClientTask StreamCorruptedException");
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
        } catch (IOException e) {
            Log.e(TAG, "ClientTask IOException");
        } catch (Exception e) {
            Log.e(TAG, "ClientTask Exception");
        } finally {
            if (!connFlag)
                Log.e(TAG, "Disconnected device: " + remotePort);
        }

        return null;
    }
}
