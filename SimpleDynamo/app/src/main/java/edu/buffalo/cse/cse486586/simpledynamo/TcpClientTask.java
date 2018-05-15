package edu.buffalo.cse.cse486586.simpledynamo;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.conn.ConnectTimeoutException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class TcpClientTask extends AsyncTask<Void, Void, Void> {

    static final String TAG = "CLIENT";
    static final String REMOTE_ADDR = "10.0.2.2";

    private OutputStream out;
    private InputStream in;

    @Override
    protected Void doInBackground (Void... voids) {

        Log.v(TAG, "Start TcpClientTask.");

        while (true) {

            while (!GV.resendQ.isEmpty()) {
                MSG resendMsg = GV.resendQ.poll();
                this.sendMsg(resendMsg);
                Log.e("CLIENT RESEND-MSG", resendMsg.toString());
            }

            if (!GV.msgSendQ.isEmpty()) {
                MSG sendMsg = GV.msgSendQ.poll();

                switch (sendMsg.getMsgType()) {

                    case SIGNAL:
                        String tgtPort = sendMsg.getSndPort();
                        MSG singalMsg = new MSG(MSG.TYPE.SIGNAL, GV.MY_PORT, tgtPort);
                        singalMsg.setMsgKey(sendMsg.getMsgID());
                        this.sendMsg(singalMsg);
                        Log.d("CLIENT SIGNAL-MSG", sendMsg.toString());
                        break;

                    case INSERT:
                    case DELETE:
                    case QUERY:
                        this.sendMsg(sendMsg);
                        this.recordWaitMsg(sendMsg); // Send msg and record according to type
                        Log.d("CLIENT NORMAL-MSG", sendMsg.toString());
                        break;

                    case UPDATE_INSERT:
                    case UPDATE_COMPLETED:
                        Log.d("CLIENT UPDATE-MSG", sendMsg.toString());
                        this.sendMsg(sendMsg);
                        break;

                    default:
                        this.sendMsg(sendMsg);
                        Log.v(TAG, sendMsg.toString());
                        break;
                }
            }
        }
    }

    private void recordWaitMsg(MSG msg) {
        switch (msg.getMsgType()) {
            case QUERY:
            case INSERT:
                if (!msg.getTgtPort().equals(GV.MY_PORT)) {
                    String msgId = msg.getMsgID();
                    GV.waitMsgIdSet.add(msgId);
                    GV.waitMsgIdQueue.offer(msgId);
                    GV.waitMsgMap.put(msgId, msg);
                    GV.waitMsgTimeMap.put(msgId, (int) System.currentTimeMillis());
                    Log.d("RECORD SIGNAL", msg.toString());
                }
                break;

            default:
                Log.v("RECORD SIGNAL", "OTHER TYPE, NO NEED TO RECORD");
                break;
        }
    }

    private void sendMsg(MSG msg) {
        String msgToSend = msg.toString();
        String tgtPort = msg.getTgtPort();
        Integer remotePort = Integer.parseInt(tgtPort) * 2;
        Log.v("HANDLE SEND MSG", "" + msgToSend);

        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(REMOTE_ADDR, remotePort));

            if (socket.isConnected()) {
                Log.v(TAG, "CONN SERVER: " + socket.getRemoteSocketAddress());
                socket.setTrafficClass(0x04 | 0x10);
                socket.setSendBufferSize(8192);

                this.in = socket.getInputStream();
                this.out = socket.getOutputStream();
                this.out.write(msgToSend.getBytes());
                this.out.flush();

                //Log.e("DETECT FAILUE", msg.getTgtPort() + " => " + this.in.read());
                Log.v("TCP SENT MSG", msgToSend);

                this.out.close();
                this.in.close();
                while (!socket.isClosed()) {socket.close();}
                // Log.v(TAG, "CLIENTSOCKET CLOSED.");
            }
        } catch (ConnectTimeoutException e) {
            Log.e(TAG, "ClientTask ConnectTimeoutException");
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            // Server response timeout
            Log.e(TAG, "ClientTask SocketTimeoutException");
            e.printStackTrace();
        } catch (SocketException e) {
            // 1. IO on socket after closed by yourself; 2. Connect reset by peer / Connection reset.
            Log.e(TAG, "ClientTask SocketException");
            e.printStackTrace();
        } catch (EOFException e) {
            // 当输入过程中意外到达文件或流的末尾时，抛出此异常。
            Log.e(TAG, "ClientTask EOFException");
            e.printStackTrace();
        } catch (StreamCorruptedException e) {
            Log.e(TAG, "ClientTask StreamCorruptedException");
            e.printStackTrace();
        } catch (UnknownHostException e) {
            Log.e(TAG, "ClientTask UnknownHostException");
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "ClientTask IOException");
            e.printStackTrace();
        }
    }

    @Override
    protected void onPostExecute(Void result) {
        Log.e("LOST CLIENT", "CLIENT TASK SHOULD NOT BREAK.");
    }

}
