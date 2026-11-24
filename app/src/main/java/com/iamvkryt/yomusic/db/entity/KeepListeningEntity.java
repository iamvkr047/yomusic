package com.iamvkryt.yomusic.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a qualified song in the Keep Listening / recommendation system.
 * A song is added only after continuous playback exceeds the qualification threshold.
 */
@Entity(tableName = "keep_listening")
public class KeepListeningEntity {

    @PrimaryKey
    @NonNull
    private String videoId;

    private String title;
    private String channel;
    private int duration;
    private int playCount;
    private long firstQualifiedAt;
    private long lastPlayedAt;

    public KeepListeningEntity(@NonNull String videoId, String title, String channel, int duration, int playCount, long firstQualifiedAt, long lastPlayedAt) {
        this.videoId = videoId;
        this.title = title;
        this.channel = channel;
        this.duration = duration;
        this.playCount = playCount;
        this.firstQualifiedAt = firstQualifiedAt;
        this.lastPlayedAt = lastPlayedAt;
    }

    @NonNull
    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(@NonNull String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public long getFirstQualifiedAt() {
        return firstQualifiedAt;
    }

    public void setFirstQualifiedAt(long firstQualifiedAt) {
        this.firstQualifiedAt = firstQualifiedAt;
    }

    public long getLastPlayedAt() {
        return lastPlayedAt;
    }

    public void setLastPlayedAt(long lastPlayedAt) {
        this.lastPlayedAt = lastPlayedAt;
    }
}

