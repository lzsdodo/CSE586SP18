package edu.buffalo.cse.cse486586.simpledynamo;


import android.os.AsyncTask;
import android.os.Message;
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

    private boolean skipMsg = false;

    @Override
    protected Void doInBackground (Void... voids) {

        Log.v(TAG, "Start TcpClientTask.");

        while (true) {

            // 发送心跳信号
            if (!GV.signalSendQ.isEmpty()) {
                NMessage recvMsg = GV.signalSendQ.poll();
                // type = signal, cmdPort = myPort,
                // tgtPort = sndPort, key = mid, val = (*.*)
                String tgtPort = recvMsg.getSndPort();
                NMessage singalMsg = new NMessage(NMessage.TYPE.SIGNAL,
                        GV.MY_PORT, tgtPort, recvMsg.getMsgID(), "(*.*)");
                this.sendMsg(singalMsg);
            }

            // 更新队列
            while (!GV.msgUpdateSendQ.isEmpty()) {
                NMessage updateMsg = GV.msgUpdateSendQ.poll();
                Log.e("SEND UPDATE MSG", updateMsg.toString());
                this.sendMsg(updateMsg);
            }

            // 常规信号
            if (!GV.msgSendQ.isEmpty()) {
                NMessage msg = GV.msgSendQ.poll();
//                if (GV.lostPort!=null) {this.skipLostPort(msg);}
                if (!this.skipMsg) {this.sendMsg(msg);}
                this.skipMsg = false;
            }

        }
    }

    private void recordWaitMsg(NMessage msg) {
        switch (msg.getMsgType()) {
            case QUERY:
            case INSERT:
            case DELETE:
                if (!msg.getTgtPort().equals(GV.MY_PORT)) {
                    String msgId = msg.getMsgID();
                    long now = System.currentTimeMillis();
                    GV.waitMsgQueue.offer(msg);
                    GV.waitMsgIdSet.add(msgId);
                    GV.waitTimeMap.put(msgId, (int) now);
                    Log.d("RECORD SIGNAL", "createSingal: ");
                }
                break;

            default:
                Log.e("RECORD SIGNAL", "OTHER TYPE, NO NEED TO RECORD " + msg.getMsgType().name());
                break;
        }
    }

    synchronized private void sendMsg(NMessage msg) {
        String msgToSend = msg.toString();
        String tgtPort = msg.getTgtPort();
        Integer remotePort = Integer.parseInt(tgtPort) * 2;
        Log.v("HANDLE SEND MSG", "" + msgToSend);

        // Send msg and record according to type
        this.recordWaitMsg(msg);

        Socket socket = new Socket();
        try {
            socket.setTrafficClass(0x04 | 0x10);
            socket.connect(new InetSocketAddress(REMOTE_ADDR, remotePort));

            if (socket.isConnected()) {
                Log.v(TAG, "CONN SERVER: " + socket.getRemoteSocketAddress());
                socket.setSendBufferSize(8192);
                socket.setSoTimeout(200);

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

    private void refreshUI(String str) {
        Message uiMsg = new Message();
        uiMsg.what = SimpleDynamoActivity.UI;
        uiMsg.obj = str;
        SimpleDynamoActivity.uiHandler.sendMessage(uiMsg);
    }

//    private void skipLostPort(NMessage msg) {
//        if (msg.getTgtPort().equals(GV.lostPort)) {
//            Log.e("SKIP LOST PORT", "BEFORE SKIP MSG: " + msg.toString());
//            String lostId = Dynamo.genHash(GV.lostPort);
//            switch (msg.getMsgType()) {
//                case INSERT:
//                    if (Dynamo.isLastNode(lostId)) {
//                        msg.setMsgType(NMessage.TYPE.UPDATE_INSERT);
//                        GV.notifySuccMsgL.add(msg);
//                        this.skipMsg = true;
//                    } else {
//                        msg.setTgtPort(Dynamo.getSuccPortOfPort(GV.lostPort));
//                        msg.setSndPort(GV.MY_PORT);
//                    }
//                    break;
//
//                case DELETE:
//                    if (Dynamo.isLastNode(lostId)) {
//                        msg.setMsgType(NMessage.TYPE.UPDATE_DELETE);
//                        GV.notifySuccMsgL.add(msg);
//                        this.skipMsg = true;
//                    } else {
//                        msg.setTgtPort(Dynamo.getSuccPortOfPort(GV.lostPort));
//                        msg.setSndPort(GV.MY_PORT);
//                    }
//                    break;
//
//                case QUERY:
//                    if (Dynamo.isFirstNode(lostId)) {
//                        msg.setTgtPort(GV.MY_PORT);
//                    } else {
//                        msg.setTgtPort(Dynamo.getPredPortOfPort(GV.lostPort));
//                    }
//                    msg.setSndPort(GV.MY_PORT);
//                    break;
//
//                default:
//                    break;
//            }
//            Log.e("SKIP LOST PORT", "AFTER SKIP MSG: " + msg.toString());
//        }
//
//    }


}
