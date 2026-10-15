package com.iamvkryt.yomusic.helpers;

import android.content.Context;

import com.iamvkryt.yomusic.Song;
import com.iamvkryt.yomusic.models.VideoItem;
import com.iamvkryt.yomusic.storage.HistoryStorage;

import java.util.ArrayList;
import java.util.List;

public class QueueManager {

    private final HistoryStorage historyStorage;
    private final List<Song> playlist = new ArrayList<>();
    private int currentIndex = -1;

    public QueueManager(Context context) {
        this.historyStorage = new HistoryStorage(context);
    }

    public Song setQueue(List<VideoItem> items, int startIndex) {
        if (items == null || items.isEmpty()) return null;

        playlist.clear();
        for (VideoItem item : items) {
            String thumb = "https://i.ytimg.com/vi/" + item.getVideoId() + "/hqdefault.jpg";
            playlist.add(new Song(item.getVideoId(), item.getTitle(), item.getChannel(), thumb));
        }

        int targetIndex = (startIndex >= 0 && startIndex < playlist.size()) ? startIndex : 0;
        currentIndex = targetIndex;
        return playlist.get(currentIndex);
    }

    public void enqueueSong(Song song) {
        if (song == null) return;

        if (!playlist.contains(song)) {
            playlist.add(song);
        }
        currentIndex = playlist.indexOf(song);

        String videoId = song.getVideoId();
        if (historyStorage != null && videoId != null && !videoId.isEmpty()) {
            historyStorage.addToHistory(new VideoItem(videoId, song.getTitle(), song.getArtist(), 0));
        }
    }

    public Song getNextSong() {
        if (playlist.isEmpty()) return null;
        currentIndex = (currentIndex + 1) % playlist.size();
        return playlist.get(currentIndex);
    }

    public Song getPreviousSong() {
        if (playlist.isEmpty()) return null;
        currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        return playlist.get(currentIndex);
    }

    public Song getCurrentSong() {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }

    public List<Song> getPlaylist() {
        return playlist;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
}
