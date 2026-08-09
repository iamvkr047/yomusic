package com.iamvkryt.yomusic.storage;

import com.iamvkryt.yomusic.models.Playlist;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

public class PlaylistSessionCache {

    public static class CacheEntry {
        public final Playlist playlist;
        public final JSONObject context;
        public final String apiToken;

        public CacheEntry(Playlist playlist, JSONObject context, String apiToken) {
            this.playlist = playlist;
            this.context = context;
            this.apiToken = apiToken;
        }
    }

    private static final Map<String, CacheEntry> cache = new HashMap<>();

    public static void put(String playlistId, Playlist playlist, JSONObject context, String apiToken) {
        cache.put(playlistId, new CacheEntry(playlist, context, apiToken));
    }

    public static CacheEntry get(String playlistId) {
        return cache.get(playlistId);
    }

    public static void clear() {
        cache.clear();
    }
}
