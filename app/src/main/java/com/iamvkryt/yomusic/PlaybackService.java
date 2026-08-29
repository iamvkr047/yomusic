package com.iamvkryt.yomusic;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class PlaybackService extends Service {

    public static final String ACTION_PREVIOUS = "com.iamvkryt.yomusic.ACTION_PREVIOUS";
    public static final String ACTION_PLAY_PAUSE = "com.iamvkryt.yomusic.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.iamvkryt.yomusic.ACTION_NEXT";
    public static final String ACTION_STOP = "com.iamvkryt.yomusic.ACTION_STOP";

    private static final String CHANNEL_ID = "yomusic_playback_channel";
    private static final int NOTIFICATION_ID = 1001;

    private final IBinder binder = new LocalBinder();
    private MediaSessionCompat mediaSession;
    private PowerManager.WakeLock wakeLock;

    private Callback callback;

    private String currentTitle = "No Song Selected";
    private String currentArtist = "Yo Music";
    private String currentThumbnailUrl = "";
    private boolean isPlaying = false;

    public interface Callback {
        void onPlay();
        void onPause();
        void onNext();
        void onPrevious();
    }

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setupMediaSession();
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "YoMusic:PlaybackWakeLock");
            }
        }
    }

    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "YoMusicMediaSession");
        mediaSession.setActive(true);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                if (callback != null) callback.onPlay();
            }

            @Override
            public void onPause() {
                if (callback != null) callback.onPause();
            }

            @Override
            public void onSkipToNext() {
                if (callback != null) callback.onNext();
            }

            @Override
            public void onSkipToPrevious() {
                if (callback != null) callback.onPrevious();
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("YoMusic player notification controls");
            channel.setShowBadge(false);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void handleAction(String action) {
        if (action == null) return;
        switch (action) {
            case ACTION_PREVIOUS:
            case MediaButtonReceiver.ACTION_PREVIOUS:
                if (callback != null) callback.onPrevious();
                break;
            case ACTION_PLAY_PAUSE:
            case MediaButtonReceiver.ACTION_PLAY_PAUSE:
                if (isPlaying) {
                    if (callback != null) callback.onPause();
                } else {
                    if (callback != null) callback.onPlay();
                }
                break;
            case ACTION_NEXT:
            case MediaButtonReceiver.ACTION_NEXT:
                if (callback != null) callback.onNext();
                break;
            case ACTION_STOP:
                if (callback != null) callback.onPause();
                stopForegroundService();
                break;
        }
    }

    public void updatePlaybackState(String title, String artist, String thumbnailUrl, boolean playing) {
        this.currentTitle = title;
        this.currentArtist = artist;
        this.currentThumbnailUrl = thumbnailUrl;
        this.isPlaying = playing;

        updateMediaSessionState();

        if (playing) {
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(10 * 60 * 1000L /*10 mins max timeout safety*/);
            }
        } else {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }

        showMediaNotification();
    }

    private void updateMediaSessionState() {
        if (mediaSession == null) return;
        int state = isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED;
        long actions = PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .setActions(actions)
                .build());
    }

    public void showMediaNotification() {
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingContentIntent = PendingIntent.getActivity(
                this, 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, PlaybackService.class).setAction(ACTION_PREVIOUS);
        PendingIntent prevPendingIntent = PendingIntent.getService(
                this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent playPauseIntent = new Intent(this, PlaybackService.class).setAction(ACTION_PLAY_PAUSE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(
                this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, PlaybackService.class).setAction(ACTION_NEXT);
        PendingIntent nextPendingIntent = PendingIntent.getService(
                this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;

        if (currentThumbnailUrl != null && !currentThumbnailUrl.isEmpty()) {
            Glide.with(getApplicationContext())
                    .asBitmap()
                    .load(currentThumbnailUrl)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            postNotification(resource, pendingContentIntent, prevPendingIntent, playPausePendingIntent, nextPendingIntent, playPauseIcon);
                        }

                        @Override
                        public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}

                        @Override
                        public void onLoadFailed(@Nullable android.graphics.drawable.Drawable errorDrawable) {
                            postNotification(null, pendingContentIntent, prevPendingIntent, playPausePendingIntent, nextPendingIntent, playPauseIcon);
                        }
                    });
        } else {
            postNotification(null, pendingContentIntent, prevPendingIntent, playPausePendingIntent, nextPendingIntent, playPauseIcon);
        }
    }

    private void postNotification(Bitmap thumbnail, PendingIntent contentIntent,
                                  PendingIntent prevIntent, PendingIntent playPauseIntent,
                                  PendingIntent nextIntent, int playPauseIcon) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music_note)
                .setLargeIcon(thumbnail)
                .setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setContentIntent(contentIntent)
                .setOngoing(isPlaying)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_skip_previous, "Previous", prevIntent)
                .addAction(playPauseIcon, isPlaying ? "Pause" : "Play", playPauseIntent)
                .addAction(R.drawable.ic_skip_next, "Next", nextIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession != null ? mediaSession.getSessionToken() : null));

        if (isPlaying) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, builder.build());
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_DETACH);
            } else {
                stopForeground(false);
            }
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, builder.build());
            }
        }
    }

    private void stopForegroundService() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (mediaSession != null) {
            mediaSession.release();
        }
        super.onDestroy();
    }
}
