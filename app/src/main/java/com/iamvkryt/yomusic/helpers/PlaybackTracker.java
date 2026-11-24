package com.iamvkryt.yomusic.helpers;

import android.util.Log;

import com.iamvkryt.yomusic.Song;
import com.iamvkryt.yomusic.storage.KeepListeningStorage;

/**
 * In-memory playback tracker for qualifying songs for the Keep Listening section.
 * Tracks continuous active playing duration (pausing pauses the counter, song changes reset it).
 * Ignores seek nuances and triggers a single qualification record when active play reaches 30s.
 */
public class PlaybackTracker {

    private static final String TAG = "PlaybackTracker";
    public static final int QUALIFY_THRESHOLD_SECONDS = 30;

    private final KeepListeningStorage keepListeningStorage;

    private Song currentSong;
    private int continuousSeconds = 0;
    private boolean isPlaying = false;
    private boolean hasQualifiedCurrentTrack = false;

    public PlaybackTracker(KeepListeningStorage keepListeningStorage) {
        this.keepListeningStorage = keepListeningStorage;
    }

    /**
     * Called whenever a new song is initiated or loaded.
     */
    public synchronized void onSongChanged(Song song) {
        if (song == null) {
            currentSong = null;
            continuousSeconds = 0;
            hasQualifiedCurrentTrack = false;
            return;
        }

        // If it's a new track, reset tracking
        if (currentSong == null || !song.getVideoId().equals(currentSong.getVideoId())) {
            currentSong = song;
            continuousSeconds = 0;
            hasQualifiedCurrentTrack = false;
            Log.d(TAG, "Song changed to: " + song.getTitle() + " (" + song.getVideoId() + "), resetting tracker.");
        }
    }

    /**
     * Called whenever playback state changes (Playing vs Paused/Buffering/Ended).
     */
    public synchronized void onPlaybackStateChanged(boolean playing) {
        this.isPlaying = playing;
        Log.d(TAG, "Playback state changed: isPlaying=" + playing + " (accumulated=" + continuousSeconds + "s)");
    }

    /**
     * Called on each 1-second progress tick from YouTubePlayerEngine.
     *
     * @param duration total duration of the track in seconds (if available)
     */
    public synchronized void onProgressTick(int duration) {
        if (!isPlaying || hasQualifiedCurrentTrack || currentSong == null) {
            return;
        }

        continuousSeconds++;

        if (continuousSeconds >= QUALIFY_THRESHOLD_SECONDS) {
            hasQualifiedCurrentTrack = true;
            Log.d(TAG, "Track qualified for Keep Listening: " + currentSong.getTitle() + " after " + continuousSeconds + "s");
            if (keepListeningStorage != null) {
                keepListeningStorage.recordQualifiedPlay(
                        currentSong.getVideoId(),
                        currentSong.getTitle(),
                        currentSong.getArtist(),
                        duration
                );
            }
        }
    }

    /**
     * Updates track metadata if YouTube loads better title/author info after playback started.
     */
    public synchronized void onMetadataUpdated(String title, String author, String videoId) {
        if (currentSong != null && currentSong.getVideoId().equals(videoId)) {
            String updatedTitle = (title != null && !title.isEmpty()) ? title : currentSong.getTitle();
            String updatedArtist = (author != null && !author.isEmpty()) ? author : currentSong.getArtist();
            currentSong = new Song(videoId, updatedTitle, updatedArtist, currentSong.getThumbnailUrl());
        }
    }

    public synchronized int getContinuousSeconds() {
        return continuousSeconds;
    }

    public synchronized boolean hasQualified() {
        return hasQualifiedCurrentTrack;
    }
}

