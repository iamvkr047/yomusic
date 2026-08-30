package com.iamvkryt.yomusic.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity representing a playlist in Room DB.
 */
@Entity(tableName = "playlists")
public class PlaylistEntity {

    @PrimaryKey
    @NonNull
    private String playlistId;

    private String title;
    private String thumbnail;
    private String channel;
    private boolean isCustom;
    private int total;
    private String nextPage;
    private long createdAt;

    public PlaylistEntity(@NonNull String playlistId, String title, String thumbnail, String channel, boolean isCustom, int total, String nextPage, long createdAt) {
        this.playlistId = playlistId;
        this.title = title;
        this.thumbnail = thumbnail;
        this.channel = channel;
        this.isCustom = isCustom;
        this.total = total;
        this.nextPage = nextPage;
        this.createdAt = createdAt;
    }

    @NonNull
    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(@NonNull String playlistId) {
        this.playlistId = playlistId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getNextPage() {
        return nextPage;
    }

    public void setNextPage(String nextPage) {
        this.nextPage = nextPage;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
