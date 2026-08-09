package com.iamvkryt.yomusic.models;

public class VideoItem {
    private final String videoId;
    private final String title;
    private final String channel;
    private final int duration;

    public VideoItem(String videoId, String title, String channel, int duration) {
        this.videoId = videoId;
        this.title = title;
        this.channel = channel;
        this.duration = duration;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getChannel() {
        return channel;
    }

    public int getDuration() {
        return duration;
    }
}
