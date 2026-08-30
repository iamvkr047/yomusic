package com.iamvkryt.yomusic.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing a song within a playlist, linked to a PlaylistEntity.
 */
@Entity(
        tableName = "playlist_songs",
        foreignKeys = @ForeignKey(
                entity = PlaylistEntity.class,
                parentColumns = "playlistId",
                childColumns = "playlistId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("playlistId")}
)
public class PlaylistSongEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @NonNull
    private String playlistId;

    private String videoId;
    private String title;
    private String channel;
    private int duration;
    private int position;

    public PlaylistSongEntity(@NonNull String playlistId, String videoId, String title, String channel, int duration, int position) {
        this.playlistId = playlistId;
        this.videoId = videoId;
        this.title = title;
        this.channel = channel;
        this.duration = duration;
        this.position = position;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(@NonNull String playlistId) {
        this.playlistId = playlistId;
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
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

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
