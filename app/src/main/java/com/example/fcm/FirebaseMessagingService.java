package com.example.fcm;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.RemoteMessage;


public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private Context mContext;
    private static PowerManager.WakeLock sCpuWakeLock;
    String title, body;
    private String TAG = "imentrance";


    public FirebaseMessagingService() {
        mContext = (Context) getBaseContext();
    }

    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");

            Log.i(TAG,"title: " + title +"/body: "+body);
            wakePower();

            Intent intent = new Intent("MainActivity");
            intent.putExtra("title", title);
            intent.putExtra("body", body);

            LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(intent);

        }
    }




    @SuppressLint("InvalidWakeLockTag")
    private void wakePower() {
        if (sCpuWakeLock != null) {
            return;
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        sCpuWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,"imentrance");
        sCpuWakeLock.acquire(10000);

        if (sCpuWakeLock != null) {
            sCpuWakeLock.release();
            sCpuWakeLock = null;
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
    }
}