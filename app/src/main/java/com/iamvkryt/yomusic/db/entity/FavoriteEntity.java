package com.iamvkryt.yomusic.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a favorite song/video in the SQLite database.
 * Similar to Schema/Model definition.
 */
@Entity(tableName = "favorites")
public class FavoriteEntity {

    @PrimaryKey
    @NonNull
    private String videoId;

    private String title;
    private String channel;
    private int duration;
    private long addedAt;

    public FavoriteEntity(@NonNull String videoId, String title, String channel, int duration, long addedAt) {
        this.videoId = videoId;
        this.title = title;
        this.channel = channel;
        this.duration = duration;
        this.addedAt = addedAt;
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

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }
}
