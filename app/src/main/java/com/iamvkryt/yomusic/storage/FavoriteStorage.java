package com.iamvkryt.yomusic.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.iamvkryt.yomusic.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class FavoriteStorage {
    private static final String TAG = "FavoriteStorage";
    private static final String PREFS_NAME = "YoMusicPrefs";
    private static final String KEY_SAVED_FAVORITES = "saved_favorites";

    private final SharedPreferences prefs;

    public FavoriteStorage(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public synchronized List<VideoItem> loadFavorites() {
        List<VideoItem> favorites = new ArrayList<>();
        String jsonString = prefs.getString(KEY_SAVED_FAVORITES, null);
        if (jsonString == null || jsonString.isEmpty()) {
            return favorites;
        }

        try {
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String videoId = obj.optString("videoId", "");
                String title = obj.optString("title", "");
                String channel = obj.optString("channel", "");
                int duration = obj.optInt("duration", 0);

                if (!videoId.isEmpty()) {
                    favorites.add(new VideoItem(videoId, title, channel, duration));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorites: " + e.getMessage());
        }

        return favorites;
    }

    public synchronized void saveFavorites(List<VideoItem> favorites) {
        try {
            JSONArray array = new JSONArray();
            for (VideoItem v : favorites) {
                JSONObject obj = new JSONObject();
                obj.put("videoId", v.getVideoId());
                obj.put("title", v.getTitle());
                obj.put("channel", v.getChannel());
                obj.put("duration", v.getDuration());
                array.put(obj);
            }
            prefs.edit().putString(KEY_SAVED_FAVORITES, array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving favorites: " + e.getMessage());
        }
    }

    public synchronized boolean isFavorite(String videoId) {
        if (videoId == null || videoId.isEmpty()) return false;
        List<VideoItem> list = loadFavorites();
        for (VideoItem item : list) {
            if (item.getVideoId().equals(videoId)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addFavorite(VideoItem video) {
        if (video == null || video.getVideoId().isEmpty()) return;
        List<VideoItem> list = loadFavorites();
        
        // Check if already exists
        boolean found = false;
        for (VideoItem item : list) {
            if (item.getVideoId().equals(video.getVideoId())) {
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(0, video);
            saveFavorites(list);
        }
    }

    public synchronized void removeFavorite(String videoId) {
        if (videoId == null || videoId.isEmpty()) return;
        List<VideoItem> list = loadFavorites();
        VideoItem toRemove = null;
        for (VideoItem item : list) {
            if (item.getVideoId().equals(videoId)) {
                toRemove = item;
                break;
            }
        }
        if (toRemove != null) {
            list.remove(toRemove);
            saveFavorites(list);
        }
    }

    public synchronized boolean toggleFavorite(VideoItem video) {
        if (video == null || video.getVideoId().isEmpty()) return false;
        if (isFavorite(video.getVideoId())) {
            removeFavorite(video.getVideoId());
            return false;
        } else {
            addFavorite(video);
            return true;
        }
    }
}
