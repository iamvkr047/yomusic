package com.iamvkryt.yomusic.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.iamvkryt.yomusic.AppUpdateManager;
import com.iamvkryt.yomusic.models.Playlist;

import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class YtRepository {
    private static final String TAG = "YtRepository";
    private static final String PREFS_NAME = "YoMusicPrefs";
    private static final String KEY_CACHED_HOME_FEED = "cachedHomeFeed";
    private static final String KEY_EXPIRES_IN = "ExpiresIn";
    private static final String KEY_COUNTRY = "country";

    private static final String MUSIC_CHANNEL_URL = "https://www.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ";
    private static final String PLAYLIST_BASE_URL = "https://www.youtube.com/playlist?list=";
    private static final long CACHE_DURATION_MS = 4 * 60 * 60 * 1000; // 4 hours

    private final Context context;
    private final YtInitDataExtractor extractor;
    private final OkHttpClient httpClient;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public interface OnHomeFeedCallback {
        void onSuccess(MusicDataParser.HomeFeedResult result);
        void onError(String message);
    }

    public interface OnPlaylistCallback {
        void onSuccess(Playlist playlist, JSONObject context, String apiToken);
        void onError(String message);
    }

    public interface OnPlaylistNextCallback {
        void onSuccess(PlaylistDataParser.PlaylistNextResult result);
        void onError(String message);
    }

    public interface OnPlaylistValidateCallback {
        void onSuccess(Playlist playlist);
        void onError(String message);
    }

    public interface OnAutoCompleteCallback {
        void onSuccess(java.util.List<String> suggestions);
        void onError(String message);
    }

    public interface OnSearchCallback {
        void onSuccess(SearchDataParser.SearchResult result, JSONObject context, String apiToken);
        void onError(String message);
    }

    public interface OnSearchNextCallback {
        void onSuccess(SearchDataParser.SearchResult result);
        void onError(String message);
    }

    public YtRepository(Context context) {
        this.context = context.getApplicationContext();
        this.extractor = new YtInitDataExtractor();
        this.httpClient = new OkHttpClient();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void getMusicHomeFeed(OnHomeFeedCallback callback) {
        executor.execute(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String country = prefs.getString(KEY_COUNTRY, "IN");

                long expiresIn = prefs.getLong(KEY_EXPIRES_IN, 0);
                String cachedJson = prefs.getString(KEY_CACHED_HOME_FEED, null);

                if (cachedJson != null && System.currentTimeMillis() < expiresIn) {
                    Log.d(TAG, "Using cached home feed data");
                    JSONObject cachedData = new JSONObject(cachedJson);
                    MusicDataParser.HomeFeedResult result = MusicDataParser.parseMusicData(cachedData);
                    if (result != null) {
                        mainHandler.post(() -> callback.onSuccess(result));
                        return;
                    }
                    Log.w(TAG, "Cached data parse failed, fetching fresh data");
                }

                AppUpdateManager.checkForUpdates(context, true, null);
                Log.d(TAG, "Fetching fresh home feed from YouTube (country=" + country + ")");
                YtInitDataExtractor.YtInitResult initResult = extractor.getYoutubeInitData(MUSIC_CHANNEL_URL, country);
                if (initResult == null || initResult.initData == null) {
                    mainHandler.post(() -> callback.onError("Failed to fetch YouTube data"));
                    return;
                }

                MusicDataParser.HomeFeedResult result = MusicDataParser.parseMusicData(initResult.initData);
                if (result == null) {
                    mainHandler.post(() -> callback.onError("Failed to parse YouTube music data"));
                    return;
                }

                prefs.edit()
                        .putString(KEY_CACHED_HOME_FEED, initResult.initData.toString())
                        .putLong(KEY_EXPIRES_IN, System.currentTimeMillis() + CACHE_DURATION_MS)
                        .apply();

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error in getMusicHomeFeed: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public void getPlaylistVideos(String playlistId, OnPlaylistCallback callback) {
        if (isShortRDPlaylist(playlistId)) {
            getShortRDPlaylistVideos(playlistId, callback);
            return;
        }

        executor.execute(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String country = prefs.getString(KEY_COUNTRY, "IN");
                String url = PLAYLIST_BASE_URL + playlistId;

                YtInitDataExtractor.YtInitResult initResult = extractor.getYoutubeInitData(url, country);
                if (initResult == null || initResult.initData == null) {
                    mainHandler.post(() -> callback.onError("Failed to fetch playlist data"));
                    return;
                }

                Playlist playlist = PlaylistDataParser.parsePlaylistVideos(initResult.initData, playlistId);
                if (playlist == null) {
                    mainHandler.post(() -> callback.onError("Failed to parse playlist data"));
                    return;
                }

                mainHandler.post(() -> callback.onSuccess(playlist, initResult.context, initResult.apiToken));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching playlist videos: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public void getShortRDPlaylistVideos(String playlistId, OnPlaylistCallback callback) {
        if (!isShortRDPlaylist(playlistId)) {
            Log.w(TAG, "Invalid short RD playlist ID: " + playlistId);
            mainHandler.post(() -> callback.onError("Invalid Short RD Playlist ID"));
            return;
        }

        executor.execute(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String country = prefs.getString(KEY_COUNTRY, "IN");
                String url = PLAYLIST_BASE_URL + playlistId + "&playnext=1";

                YtInitDataExtractor.YtInitResult initResult = extractor.getYoutubeInitData(url, country);
                if (initResult == null || initResult.initData == null) {
                    mainHandler.post(() -> callback.onError("Failed to fetch YouTube radio data"));
                    return;
                }

                Playlist playlist = RadioPlaylistDataParser.parseRadioPlaylistVideos(initResult.initData, playlistId);
                if (playlist == null) {
                    mainHandler.post(() -> callback.onError("Failed to parse YouTube radio playlist"));
                    return;
                }

                mainHandler.post(() -> callback.onSuccess(playlist, initResult.context, initResult.apiToken));
            } catch (Exception e) {
                Log.e(TAG, "Error in getShortRDPlaylistVideos: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public void getPlaylistVideosNext(String token, JSONObject clientContext, String apiToken, OnPlaylistNextCallback callback) {
        if (isShortRDToken(token)) {
            getNextShortRD(token, clientContext, apiToken, callback);
            return;
        }

        executor.execute(() -> {
            try {
                String endpoint = "https://www.youtube.com/youtubei/v1/browse?key=" + apiToken;
                JSONObject payload = new JSONObject();
                payload.put("context", clientContext != null ? clientContext : new JSONObject());
                payload.put("continuation", token);

                RequestBody body = RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(endpoint)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        mainHandler.post(() -> callback.onError("Next page request failed: " + response.code()));
                        return;
                    }

                    String jsonStr = response.body().string();
                    JSONObject json = new JSONObject(jsonStr);
                    PlaylistDataParser.PlaylistNextResult result = PlaylistDataParser.parsePlaylistVideosNext(json);
                    if (result == null) {
                        mainHandler.post(() -> callback.onError("Failed to parse continuation items"));
                        return;
                    }

                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in getPlaylistVideosNext: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public void getNextShortRD(String token, JSONObject clientContext, String apiToken, OnPlaylistNextCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject tokenObj = new JSONObject(token);
                String params = tokenObj.optString("params", "");
                String playlistId = tokenObj.optString("playlistId", "");
                int playlistIndex = tokenObj.optInt("playlistIndex", 0);
                String videoId = tokenObj.optString("videoId", "");

                String endpoint = "https://www.youtube.com/youtubei/v1/get_watch?key=" + apiToken;

                JSONObject watchNextRequest = new JSONObject();
                watchNextRequest.put("params", params);
                watchNextRequest.put("playlistId", playlistId);
                watchNextRequest.put("playlistIndex", playlistIndex);
                watchNextRequest.put("racyCheckOk", false);
                watchNextRequest.put("videoId", videoId);

                JSONObject payload = new JSONObject();
                payload.put("context", clientContext != null ? clientContext : new JSONObject());
                payload.put("watchNextRequest", watchNextRequest);

                RequestBody body = RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );

                Request request = new Request.Builder()
                        .url(endpoint)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        mainHandler.post(() -> callback.onError("Next radio page request failed: " + response.code()));
                        return;
                    }

                    String jsonStr = response.body().string();
                    PlaylistDataParser.PlaylistNextResult result = RadioPlaylistDataParser.parseRadioPlaylistNext(jsonStr, token);
                    if (result == null) {
                        mainHandler.post(() -> callback.onError("Failed to parse radio continuation items"));
                        return;
                    }

                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in getNextShortRD: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public static boolean isShortRDPlaylist(String playlistId) {
        return playlistId != null && playlistId.startsWith("RD") && playlistId.length() <= 15;
    }

    public static boolean isShortRDToken(String token) {
        return token != null && token.trim().startsWith("{") && token.contains("\"playlistIndex\"");
    }

    public void validatePlaylistLink(String searchQuery, OnPlaylistValidateCallback callback) {
        // Extract playlist ID from the URL first to check for radio playlists
        Uri uri = Uri.parse(searchQuery);
        String listParam = uri.getQueryParameter("list");

        // Radio playlists (RD*) cannot be validated via oEmbed,
        // so we extract the ID and directly fetch the playlist data
        if (listParam != null && listParam.startsWith("RD")) {
            OnPlaylistCallback wrappedCallback = new OnPlaylistCallback() {
                @Override
                public void onSuccess(Playlist playlist, JSONObject context, String apiToken) {
                    callback.onSuccess(playlist);
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            };

            if (isShortRDPlaylist(listParam)) {
                getShortRDPlaylistVideos(listParam, wrappedCallback);
            } else {
                getPlaylistVideos(listParam, wrappedCallback);
            }
            return;
        }

        // Standard playlists (PL*) — validate via oEmbed
        executor.execute(() -> {
            try {
                String encodedUrl = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8.name());
                String oembedUrl = "https://www.youtube.com/oembed?url=" + encodedUrl + "&format=json";

                Request request = new Request.Builder()
                        .url(oembedUrl)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        mainHandler.post(() -> callback.onError("Invalid Link"));
                        return;
                    }

                    String jsonStr = response.body().string();
                    JSONObject data = new JSONObject(jsonStr);

                    String title = data.optString("title", "");
                    String thumbnail = data.optString("thumbnail_url", "");
                    String channel = data.optString("author_name", "");
                    String html = data.optString("html", "");

                    String playlistId = null;
                    if (html.contains("list=")) {
                        int index = html.indexOf("list=");
                        String substring = html.substring(index + 5);
                        int end = substring.indexOf("&");
                        if (end == -1) end = substring.indexOf("\"");
                        if (end == -1) end = substring.indexOf("'");
                        if (end != -1) {
                            playlistId = substring.substring(0, end);
                        } else {
                            playlistId = substring;
                        }
                    }

                    if (playlistId == null || playlistId.isEmpty()) {
                        // Fallback check URL parameter directly if user passed playlist link directly
                        Uri fallbackUri = Uri.parse(searchQuery);
                        playlistId = fallbackUri.getQueryParameter("list");
                    }

                    if (playlistId == null || playlistId.isEmpty()) {
                        mainHandler.post(() -> callback.onError("Invalid Link"));
                        return;
                    }

                    Playlist playlist = new Playlist(playlistId, title, thumbnail, channel, false, null, "", 0);
                    mainHandler.post(() -> callback.onSuccess(playlist));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error validating playlist link: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Invalid Link"));
            }
        });
    }

    public void clearCache() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(KEY_CACHED_HOME_FEED)
                .remove(KEY_EXPIRES_IN)
                .apply();
    }

    public void getAutoCompleteSearch(String query, OnAutoCompleteCallback callback) {
        executor.execute(() -> {
            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                String url = "https://suggestqueries-clients6.youtube.com/complete/search?client=youtube&q=" + encodedQuery;
                Request request = new Request.Builder().url(url).build();
                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        mainHandler.post(() -> callback.onError("Autocomplete request failed"));
                        return;
                    }
                    String text = response.body().string();
                    java.util.List<String> suggestions = SearchDataParser.parseAutoComplete(text);
                    mainHandler.post(() -> callback.onSuccess(suggestions));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in getAutoCompleteSearch: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public void search(String term, OnSearchCallback callback) {
        executor.execute(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String country = prefs.getString(KEY_COUNTRY, "IN");
                String encodedTerm = URLEncoder.encode(term, StandardCharsets.UTF_8.name());
                String searchUrl = "https://www.youtube.com/results?search_query=" + encodedTerm;

                YtInitDataExtractor.YtInitResult initResult = extractor.getYoutubeInitData(searchUrl, country);
                if (initResult == null || initResult.initData == null) {
                    mainHandler.post(() -> callback.onError("Failed to fetch search results"));
                    return;
                }

                SearchDataParser.SearchResult result = SearchDataParser.parseSearchResults(initResult.initData);
                mainHandler.post(() -> callback.onSuccess(result, initResult.context, initResult.apiToken));
            } catch (Exception e) {
                Log.e(TAG, "Error in search: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }

    public void getSearchNext(String token, JSONObject clientContext, String apiToken, OnSearchNextCallback callback) {
        executor.execute(() -> {
            try {
                String endpoint = "https://www.youtube.com/youtubei/v1/search?key=" + apiToken;
                JSONObject payload = new JSONObject();
                payload.put("context", clientContext != null ? clientContext : new JSONObject());
                payload.put("continuation", token);

                RequestBody body = RequestBody.create(
                        payload.toString(),
                        MediaType.parse("application/json; charset=utf-8")
                );
                Request request = new Request.Builder().url(endpoint).post(body).build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        mainHandler.post(() -> callback.onError("Search next page failed: " + response.code()));
                        return;
                    }
                    JSONObject json = new JSONObject(response.body().string());
                    SearchDataParser.SearchResult result = SearchDataParser.parseSearchResultsNext(json);
                    mainHandler.post(() -> callback.onSuccess(result));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in getSearchNext: " + e.getMessage());
                mainHandler.post(() -> callback.onError("Error: " + e.getMessage()));
            }
        });
    }
}
