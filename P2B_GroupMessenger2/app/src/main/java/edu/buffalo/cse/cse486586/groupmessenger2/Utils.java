package edu.buffalo.cse.cse486586.groupmessenger2;

import android.util.Log;

public class Utils {

    // Sequence Vector


    // Connection
    static void recordHeartbeat(Message msg) {
        int times;
        switch (msg.getMsgTargetType()) {
            case GROUP:
                for(int i=0; i<5; i++) {
                    times = GV.devNotAliveTimes.get(i) + 1;
                    GV.devNotAliveTimes.set(i, times);
                }
                break;
            case PID:
                times = GV.devNotAliveTimes.get(msg.getFromPID()) + 1;
                GV.devNotAliveTimes.set(msg.getFromPID(), times);
                break;
            default:break;
        }
        Log.d("RECORD HEARTBEAT", "NOT ALIVE TIMES: " + GV.devNotAliveTimes.toString());
    }

    static void updateHeartbeat(int pid) {
        int times = GV.devNotAliveTimes.get(pid);
        times -= 1;
        GV.devNotAliveTimes.set(pid, times);
        Log.d("RECORD HEARTBEAT", "NOT ALIVE TIMES: " + GV.devNotAliveTimes.toString());
    }

    static void recordDisconnTime(int pid) {
        int times = GV.devDisconnTimes.get(pid);
        times += 1;
        GV.devDisconnTimes.set(pid, times);
        Log.d("RECORD DISCONN", "DISCONNECTED TIMES: " + GV.devDisconnTimes.toString());
    }

    static public void updateDevStatus() {
        int connDevNum = 0;
        for(int pid = 0; pid < 5; pid++) {
            GV.devStatus.set(pid, true);
            if ((GV.devNotAliveTimes.get(pid) > GV.RETRY_TIME)
                    || (GV.devDisconnTimes.get(pid) > GV.RETRY_TIME)) {
                    GV.devStatus.set(pid, false);
            }
            if (GV.devStatus.get(pid)) connDevNum++;
        }
        GV.devConnNum = Math.min(GV.devConnNum, connDevNum);

        Log.d("CONN STATUS: ", "COMPARE " + GV.devConnNum + "-" + connDevNum + ":\n"
                + "WITH CONN STATUS: " + GV.devStatus.toString() + "; " + "DEVICE REMAIN: " + GV.devConnNum);
    }

}
