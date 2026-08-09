package com.iamvkr.yomusic.models;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String playlistId;
    private String title;
    private String thumbnail;
    private String channel;
    private boolean isCustomPlaylist;
    private List<VideoItem> videoItems;
    private String nextPage;
    private int total;

    public Playlist(String playlistId, String title, String thumbnail, String channel, boolean isCustomPlaylist, List<VideoItem> videoItems, String nextPage, int total) {
        this.playlistId = playlistId;
        this.title = title;
        this.thumbnail = thumbnail;
        this.channel = channel;
        this.isCustomPlaylist = isCustomPlaylist;
        this.videoItems = videoItems != null ? videoItems : new ArrayList<>();
        this.nextPage = nextPage != null ? nextPage : "";
        this.total = total;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public void setPlaylistId(String playlistId) {
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

    public boolean isCustomPlaylist() {
        return isCustomPlaylist;
    }

    public void setCustomPlaylist(boolean customPlaylist) {
        isCustomPlaylist = customPlaylist;
    }

    public List<VideoItem> getVideoItems() {
        return videoItems;
    }

    public void setVideoItems(List<VideoItem> videoItems) {
        this.videoItems = videoItems;
    }

    public String getNextPage() {
        return nextPage;
    }

    public void setNextPage(String nextPage) {
        this.nextPage = nextPage;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
