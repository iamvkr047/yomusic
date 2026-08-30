package com.iamvkryt.yomusic.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a recently played song/video in Room DB history table.
 */
@Entity(tableName = "history")
public class HistoryEntity {

    @PrimaryKey
    @NonNull
    private String videoId;

    private String title;
    private String channel;
    private int duration;
    private long playedAt;

    public HistoryEntity(@NonNull String videoId, String title, String channel, int duration, long playedAt) {
        this.videoId = videoId;
        this.title = title;
        this.channel = channel;
        this.duration = duration;
        this.playedAt = playedAt;
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

    public long getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(long playedAt) {
        this.playedAt = playedAt;
    }
}
