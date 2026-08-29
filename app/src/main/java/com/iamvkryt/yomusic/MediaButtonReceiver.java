package com.iamvkryt.yomusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MediaButtonReceiver extends BroadcastReceiver {
    public static final String ACTION_PREVIOUS = "ACTION_PREVIOUS";
    public static final String ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;

        Intent serviceIntent = new Intent(context, PlaybackService.class);
        serviceIntent.setAction(intent.getAction());
        try {
            context.startService(serviceIntent);
        } catch (Exception e) {
            if (MainActivity.getInstance() != null) {
                MainActivity.getInstance().handleNotificationAction(intent.getAction());
            }
        }
    }
}
