package edu.buffalo.cse.cse486586.groupmessenger2;

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
import java.util.ArrayList;
import java.util.HashMap;

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
    // Usage: new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msgToSend, tPID/GROUP);

    static final String TAG = "CLIENT";

    @Override
    protected Void doInBackground(String... params) {
        String msgToSend = params[0];
        Message.TARGET_TYPE msgTarget = Message.TARGET_TYPE.valueOf(params[1]);
        int targetPID = Integer.parseInt(params[2]);

        ArrayList<String> remotePorts = new ArrayList<String>();
        switch (msgTarget) {
            case GROUP:
                Log.e("SEND GROUP", msgToSend);
                remotePorts = GV.REMOTE_PORTS;
                break;
            case PID:
                Log.e("SEND PID", msgToSend);
                remotePorts.add(GV.REMOTE_PORTS.get(targetPID));
                break;
            default: // NONE
                Log.e("CLIENT MSG TYPE ERROR", msgToSend);
                break;
        }

        for (String remotePort: remotePorts) {
            targetPID = GV.REMOTE_PORTS.indexOf(remotePort);
            Log.d(TAG, "Sending to device: " + targetPID + "::" + remotePort);

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
                // e.printStackTrace();
            } catch (SocketException e) {
                // 1. IO on socket after closed by yourself.
                // 2. Connect reset by peer / Connection reset
                // 心跳包监测 断了就会进入该异常处理
                Log.e(TAG, "ClientTask SocketException");
                // e.printStackTrace();
            } catch (EOFException e) {
                // 当输入过程中意外到达文件或流的末尾时，抛出此异常。
                Log.e(TAG, "ClientTask EOFException");
                // e.printStackTrace();
            } catch (StreamCorruptedException e) {
                Log.e(TAG, "ClientTask StreamCorruptedException");
                // e.printStackTrace();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
                // e.printStackTrace();
            } catch (IOException e) {
                Log.e(TAG, "ClientTask IOException");
                // e.printStackTrace();
            } catch (Exception e) {
                Log.e(TAG, "ClientTask Exception");
                // e.printStackTrace();
            } finally {
                if (!connFlag) {
                    Log.e(TAG, "Disconnected device: " + targetPID + "-" + remotePort);
                    Utils.recordDisconnTime(targetPID);
                    Utils.updateDevStatus();
                }
            }
        }
        return null;
    }
}
