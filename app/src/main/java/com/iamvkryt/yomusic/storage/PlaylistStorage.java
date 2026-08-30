package com.iamvkryt.yomusic.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.iamvkryt.yomusic.db.AppDatabase;
import com.iamvkryt.yomusic.db.dao.PlaylistDao;
import com.iamvkryt.yomusic.db.entity.PlaylistEntity;
import com.iamvkryt.yomusic.db.entity.PlaylistSongEntity;
import com.iamvkryt.yomusic.models.Playlist;
import com.iamvkryt.yomusic.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage facade for Playlists, powered by Room Database.
 * Maintains full backward compatibility with existing callers.
 */
public class PlaylistStorage {
    private static final String TAG = "PlaylistStorage";
    private static final String PREFS_NAME = "YoMusicPrefs";
    private static final String KEY_SAVED_PLAYLISTS = "saved_playlists";

    private final AppDatabase database;
    private final PlaylistDao playlistDao;
    private final SharedPreferences legacyPrefs;

    public PlaylistStorage(Context context) {
        Context appContext = context.getApplicationContext();
        this.database = AppDatabase.getInstance(appContext);
        this.playlistDao = database.playlistDao();
        this.legacyPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Perform 1-time migration from SharedPreferences to Room DB if legacy data exists
        migrateLegacySharedPreferencesIfNeeded();
    }

    private synchronized void migrateLegacySharedPreferencesIfNeeded() {
        String jsonString = legacyPrefs.getString(KEY_SAVED_PLAYLISTS, null);
        if (jsonString == null || jsonString.isEmpty()) {
            return;
        }

        try {
            Log.d(TAG, "Migrating legacy SharedPreferences playlists to Room DB...");
            JSONArray array = new JSONArray(jsonString);
            long now = System.currentTimeMillis();

            database.runInTransaction(() -> {
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject obj = array.getJSONObject(i);
                        String playlistId = obj.optString("playlistId", "");
                        String title = obj.optString("title", "");
                        String thumbnail = obj.optString("thumbnail", "");
                        String channel = obj.optString("channel", "");
                        boolean isCustom = obj.optBoolean("isCustomPlaylist", false);
                        int total = obj.optInt("total", 0);
                        String nextPage = obj.optString("nextPage", "");

                        if (playlistId.isEmpty()) continue;

                        // Create playlist entity (decrement now to preserve order)
                        PlaylistEntity pe = new PlaylistEntity(
                                playlistId, title, thumbnail, channel, isCustom, total, nextPage, now - (i * 1000L)
                        );

                        // Parse nested video items
                        List<PlaylistSongEntity> songs = new ArrayList<>();
                        JSONArray videosArray = obj.optJSONArray("videoItems");
                        if (videosArray != null) {
                            for (int j = 0; j < videosArray.length(); j++) {
                                JSONObject vObj = videosArray.getJSONObject(j);
                                songs.add(new PlaylistSongEntity(
                                        playlistId,
                                        vObj.optString("videoId", ""),
                                        vObj.optString("title", ""),
                                        vObj.optString("channel", ""),
                                        vObj.optInt("duration", 0),
                                        j
                                ));
                            }
                        }

                        playlistDao.insertPlaylistWithSongs(pe, songs);
                    } catch (Exception e) {
                        Log.e(TAG, "Error migrating individual playlist: " + e.getMessage());
                    }
                }
            });

            Log.d(TAG, "Successfully completed legacy playlists migration.");
            // Remove legacy key after successful migration
            legacyPrefs.edit().remove(KEY_SAVED_PLAYLISTS).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error migrating legacy playlists: " + e.getMessage());
        }
    }

    public synchronized List<Playlist> loadPlaylists() {
        List<Playlist> result = new ArrayList<>();
        try {
            List<PlaylistEntity> playlistEntities = playlistDao.getAllPlaylists();
            for (PlaylistEntity pe : playlistEntities) {
                // Fetch associated songs
                List<PlaylistSongEntity> songEntities = playlistDao.getSongsForPlaylist(pe.getPlaylistId());
                List<VideoItem> videoItems = new ArrayList<>();
                for (PlaylistSongEntity se : songEntities) {
                    videoItems.add(new VideoItem(
                            se.getVideoId(),
                            se.getTitle(),
                            se.getChannel(),
                            se.getDuration()
                    ));
                }

                result.add(new Playlist(
                        pe.getPlaylistId(),
                        pe.getTitle(),
                        pe.getThumbnail(),
                        pe.getChannel(),
                        pe.isCustom(),
                        videoItems,
                        pe.getNextPage(),
                        pe.getTotal()
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading playlists from Room DB: " + e.getMessage());
        }
        return result;
    }

    public synchronized void savePlaylists(List<Playlist> playlists) {
        try {
            database.runInTransaction(() -> {
                playlistDao.deleteAllPlaylists();
                if (playlists != null) {
                    long now = System.currentTimeMillis();
                    for (int i = 0; i < playlists.size(); i++) {
                        Playlist p = playlists.get(i);
                        if (p == null || p.getPlaylistId() == null || p.getPlaylistId().isEmpty()) {
                            continue;
                        }

                        PlaylistEntity pe = new PlaylistEntity(
                                p.getPlaylistId(),
                                p.getTitle(),
                                p.getThumbnail(),
                                p.getChannel(),
                                p.isCustomPlaylist(),
                                p.getTotal(),
                                p.getNextPage(),
                                now - (i * 1000L) // preserve order via creation timestamp
                        );

                        List<PlaylistSongEntity> songs = new ArrayList<>();
                        if (p.getVideoItems() != null) {
                            for (int j = 0; j < p.getVideoItems().size(); j++) {
                                VideoItem v = p.getVideoItems().get(j);
                                if (v != null) {
                                    songs.add(new PlaylistSongEntity(
                                            p.getPlaylistId(),
                                            v.getVideoId(),
                                            v.getTitle(),
                                            v.getChannel(),
                                            v.getDuration(),
                                            j
                                    ));
                                }
                            }
                        }

                        playlistDao.insertPlaylistWithSongs(pe, songs);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error saving playlists to Room DB: " + e.getMessage());
        }
    }

    public synchronized void addPlaylist(Playlist playlist) {
        if (playlist == null || playlist.getPlaylistId() == null || playlist.getPlaylistId().isEmpty()) return;
        try {
            PlaylistEntity pe = new PlaylistEntity(
                    playlist.getPlaylistId(),
                    playlist.getTitle(),
                    playlist.getThumbnail(),
                    playlist.getChannel(),
                    playlist.isCustomPlaylist(),
                    playlist.getTotal(),
                    playlist.getNextPage(),
                    System.currentTimeMillis() // new playlist goes to top
            );

            List<PlaylistSongEntity> songs = new ArrayList<>();
            if (playlist.getVideoItems() != null) {
                for (int j = 0; j < playlist.getVideoItems().size(); j++) {
                    VideoItem v = playlist.getVideoItems().get(j);
                    if (v != null) {
                        songs.add(new PlaylistSongEntity(
                                playlist.getPlaylistId(),
                                v.getVideoId(),
                                v.getTitle(),
                                v.getChannel(),
                                v.getDuration(),
                                j
                        ));
                    }
                }
            }

            playlistDao.insertPlaylistWithSongs(pe, songs);
        } catch (Exception e) {
            Log.e(TAG, "Error adding playlist: " + e.getMessage());
        }
    }

    public synchronized void removePlaylist(String playlistId) {
        if (playlistId == null || playlistId.isEmpty()) return;
        try {
            // Note: Cascade Delete foreign key on playlist_songs automatically handles removing songs.
            playlistDao.deletePlaylistById(playlistId);
        } catch (Exception e) {
            Log.e(TAG, "Error removing playlist: " + e.getMessage());
        }
    }
}
