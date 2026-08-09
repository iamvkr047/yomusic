package com.iamvkryt.yomusic.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.iamvkryt.yomusic.models.Playlist;
import com.iamvkryt.yomusic.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PlaylistStorage {
    private static final String TAG = "PlaylistStorage";
    private static final String PREFS_NAME = "YoMusicPrefs";
    private static final String KEY_SAVED_PLAYLISTS = "saved_playlists";

    private final SharedPreferences prefs;

    public PlaylistStorage(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<Playlist> loadPlaylists() {
        List<Playlist> playlists = new ArrayList<>();
        String jsonString = prefs.getString(KEY_SAVED_PLAYLISTS, null);
        if (jsonString == null || jsonString.isEmpty()) {
            return playlists;
        }

        try {
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String playlistId = obj.optString("playlistId", "");
                String title = obj.optString("title", "");
                String thumbnail = obj.optString("thumbnail", "");
                String channel = obj.optString("channel", "");
                boolean isCustom = obj.optBoolean("isCustomPlaylist", false);
                int total = obj.optInt("total", 0);
                String nextPage = obj.optString("nextPage", "");

                List<VideoItem> videoItems = new ArrayList<>();
                JSONArray videosArray = obj.optJSONArray("videoItems");
                if (videosArray != null) {
                    for (int j = 0; j < videosArray.length(); j++) {
                        JSONObject vObj = videosArray.getJSONObject(j);
                        videoItems.add(new VideoItem(
                                vObj.optString("videoId", ""),
                                vObj.optString("title", ""),
                                vObj.optString("channel", ""),
                                vObj.optInt("duration", 0)
                        ));
                    }
                }

                playlists.add(new Playlist(playlistId, title, thumbnail, channel, isCustom, videoItems, nextPage, total));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading playlists: " + e.getMessage());
        }

        return playlists;
    }

    public void savePlaylists(List<Playlist> playlists) {
        try {
            JSONArray array = new JSONArray();
            for (Playlist p : playlists) {
                JSONObject obj = new JSONObject();
                obj.put("playlistId", p.getPlaylistId());
                obj.put("title", p.getTitle());
                obj.put("thumbnail", p.getThumbnail());
                obj.put("channel", p.getChannel());
                obj.put("isCustomPlaylist", p.isCustomPlaylist());
                obj.put("total", p.getTotal());
                obj.put("nextPage", p.getNextPage());

                JSONArray videosArray = new JSONArray();
                // Store video items for custom playlists (or YT saved playlists if present)
                if (p.getVideoItems() != null) {
                    for (VideoItem v : p.getVideoItems()) {
                        JSONObject vObj = new JSONObject();
                        vObj.put("videoId", v.getVideoId());
                        vObj.put("title", v.getTitle());
                        vObj.put("channel", v.getChannel());
                        vObj.put("duration", v.getDuration());
                        videosArray.put(vObj);
                    }
                }
                obj.put("videoItems", videosArray);
                array.put(obj);
            }

            prefs.edit().putString(KEY_SAVED_PLAYLISTS, array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving playlists: " + e.getMessage());
        }
    }

    public void addPlaylist(Playlist playlist) {
        List<Playlist> list = loadPlaylists();
        // Insert at beginning
        list.add(0, playlist);
        savePlaylists(list);
    }

    public void removePlaylist(String playlistId) {
        List<Playlist> list = loadPlaylists();
        List<Playlist> updated = new ArrayList<>();
        for (Playlist p : list) {
            if (!p.getPlaylistId().equals(playlistId)) {
                updated.add(p);
            }
        }
        savePlaylists(updated);
    }
}
