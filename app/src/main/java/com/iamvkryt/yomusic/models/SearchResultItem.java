package com.iamvkryt.yomusic.models;

public class SearchResultItem {

    public enum Type {
        VIDEO,
        PLAYLIST
    }

    private final Type type;
    private final VideoItem video;
    private final Playlist playlist;

    public SearchResultItem(VideoItem video) {
        this.type = Type.VIDEO;
        this.video = video;
        this.playlist = null;
    }

    public SearchResultItem(Playlist playlist) {
        this.type = Type.PLAYLIST;
        this.video = null;
        this.playlist = playlist;
    }

    public Type getType() { return type; }

    public VideoItem getVideo() { return video; }

    public Playlist getPlaylist() { return playlist; }
}
