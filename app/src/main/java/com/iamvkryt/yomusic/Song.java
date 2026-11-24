package com.iamvkryt.yomusic;

public class Song {
    private final String videoId;
    private final String title;
    private final String artist;
    private final String thumbnailUrl;

    public Song(String videoId, String title, String artist, String thumbnailUrl) {
        this.videoId = videoId;
        this.title = title;
        this.artist = artist;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return videoId != null ? videoId.equals(song.videoId) : song.videoId == null;
    }

    @Override
    public int hashCode() {
        return videoId != null ? videoId.hashCode() : 0;
    }
}
