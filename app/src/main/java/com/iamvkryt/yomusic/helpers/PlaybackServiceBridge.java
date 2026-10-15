package com.iamvkryt.yomusic.helpers;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.content.ContextCompat;

import com.iamvkryt.yomusic.MediaButtonReceiver;
import com.iamvkryt.yomusic.PlaybackService;

public class PlaybackServiceBridge {

    public interface PlaybackCallback {
        void onPlay();
        void onPause();
        void onNext();
        void onPrevious();
    }

    private final Activity activity;
    private final PlaybackCallback callback;
    private PlaybackService playbackService;
    private boolean isServiceBound = false;

    // Cached state to sync when service connects
    private String lastTitle = "";
    private String lastArtist = "";
    private String lastThumbnailUrl = "";
    private boolean lastIsPlaying = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            isServiceBound = true;
            playbackService.setCallback(new PlaybackService.Callback() {
                @Override
                public void onPlay() {
                    if (callback != null) callback.onPlay();
                }

                @Override
                public void onPause() {
                    if (callback != null) callback.onPause();
                }

                @Override
                public void onNext() {
                    if (callback != null) callback.onNext();
                }

                @Override
                public void onPrevious() {
                    if (callback != null) callback.onPrevious();
                }
            });
            syncPlaybackState();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            playbackService = null;
            isServiceBound = false;
        }
    };

    public PlaybackServiceBridge(Activity activity, PlaybackCallback callback) {
        this.activity = activity;
        this.callback = callback;
    }

    public void bind() {
        checkNotificationPermission();
        Intent intent = new Intent(activity, PlaybackService.class);
        activity.startService(intent);
        activity.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void unbind() {
        if (isServiceBound) {
            try {
                activity.unbindService(serviceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            }
            isServiceBound = false;
            playbackService = null;
        }
    }

    public void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    public void updatePlaybackState(String title, String artist, String thumbnailUrl, boolean isPlaying) {
        this.lastTitle = title;
        this.lastArtist = artist;
        this.lastThumbnailUrl = thumbnailUrl;
        this.lastIsPlaying = isPlaying;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
        }

        if (isServiceBound && playbackService != null) {
            playbackService.updatePlaybackState(title, artist, thumbnailUrl, isPlaying);
        } else {
            Intent serviceIntent = new Intent(activity, PlaybackService.class);
            if (isPlaying) {
                ContextCompat.startForegroundService(activity, serviceIntent);
            }
            activity.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void syncPlaybackState() {
        if (isServiceBound && playbackService != null && lastTitle != null && !lastTitle.isEmpty()) {
            playbackService.updatePlaybackState(lastTitle, lastArtist, lastThumbnailUrl, lastIsPlaying);
        }
    }

    public void handleNotificationAction(String action, boolean isPlaying) {
        if (playbackService != null) {
            playbackService.handleAction(action);
        } else {
            if (action == null || callback == null) return;
            switch (action) {
                case MediaButtonReceiver.ACTION_PREVIOUS:
                    callback.onPrevious();
                    break;
                case MediaButtonReceiver.ACTION_PLAY_PAUSE:
                    if (isPlaying) {
                        callback.onPause();
                    } else {
                        callback.onPlay();
                    }
                    break;
                case MediaButtonReceiver.ACTION_NEXT:
                    callback.onNext();
                    break;
            }
        }
    }
}
