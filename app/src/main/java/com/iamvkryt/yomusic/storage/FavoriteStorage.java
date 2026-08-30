package com.iamvkryt.yomusic.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.iamvkryt.yomusic.db.AppDatabase;
import com.iamvkryt.yomusic.db.dao.FavoriteDao;
import com.iamvkryt.yomusic.db.entity.FavoriteEntity;
import com.iamvkryt.yomusic.models.VideoItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage facade for Favorites, powered by Room Database.
 * Maintains full backward compatibility with existing callers.
 */
public class FavoriteStorage {
    private static final String TAG = "FavoriteStorage";
    private static final String PREFS_NAME = "YoMusicPrefs";
    private static final String KEY_SAVED_FAVORITES = "saved_favorites";

    private final FavoriteDao favoriteDao;
    private final SharedPreferences legacyPrefs;

    public FavoriteStorage(Context context) {
        Context appContext = context.getApplicationContext();
        this.favoriteDao = AppDatabase.getInstance(appContext).favoriteDao();
        this.legacyPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Perform 1-time migration from SharedPreferences to Room DB if legacy data exists
        migrateLegacySharedPreferencesIfNeeded();
    }

    private synchronized void migrateLegacySharedPreferencesIfNeeded() {
        String jsonString = legacyPrefs.getString(KEY_SAVED_FAVORITES, null);
        if (jsonString == null || jsonString.isEmpty()) {
            return;
        }

        try {
            Log.d(TAG, "Migrating legacy SharedPreferences favorites to Room DB...");
            JSONArray array = new JSONArray(jsonString);
            List<FavoriteEntity> migratedEntities = new ArrayList<>();
            long now = System.currentTimeMillis();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String videoId = obj.optString("videoId", "");
                String title = obj.optString("title", "");
                String channel = obj.optString("channel", "");
                int duration = obj.optInt("duration", 0);

                if (!videoId.isEmpty()) {
                    // Decrement addedAt by index to preserve order
                    migratedEntities.add(new FavoriteEntity(videoId, title, channel, duration, now - (i * 1000L)));
                }
            }

            if (!migratedEntities.isEmpty()) {
                favoriteDao.insertAll(migratedEntities);
                Log.d(TAG, "Successfully migrated " + migratedEntities.size() + " favorites to Room DB.");
            }

            // Remove legacy key after successful migration
            legacyPrefs.edit().remove(KEY_SAVED_FAVORITES).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error migrating legacy favorites: " + e.getMessage());
        }
    }

    public synchronized List<VideoItem> loadFavorites() {
        List<VideoItem> result = new ArrayList<>();
        try {
            List<FavoriteEntity> entities = favoriteDao.getAllFavorites();
            for (FavoriteEntity entity : entities) {
                result.add(new VideoItem(
                        entity.getVideoId(),
                        entity.getTitle(),
                        entity.getChannel(),
                        entity.getDuration()
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorites from Room DB: " + e.getMessage());
        }
        return result;
    }

    public synchronized void saveFavorites(List<VideoItem> favorites) {
        try {
            favoriteDao.deleteAll();
            if (favorites != null && !favorites.isEmpty()) {
                List<FavoriteEntity> entities = new ArrayList<>();
                long now = System.currentTimeMillis();
                for (int i = 0; i < favorites.size(); i++) {
                    VideoItem v = favorites.get(i);
                    if (v != null && v.getVideoId() != null && !v.getVideoId().isEmpty()) {
                        entities.add(new FavoriteEntity(
                                v.getVideoId(),
                                v.getTitle(),
                                v.getChannel(),
                                v.getDuration(),
                                now - (i * 1000L)
                        ));
                    }
                }
                favoriteDao.insertAll(entities);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving favorites to Room DB: " + e.getMessage());
        }
    }

    public synchronized boolean isFavorite(String videoId) {
        if (videoId == null || videoId.isEmpty()) return false;
        try {
            return favoriteDao.isFavorite(videoId);
        } catch (Exception e) {
            Log.e(TAG, "Error checking isFavorite for " + videoId + ": " + e.getMessage());
            return false;
        }
    }

    public synchronized void addFavorite(VideoItem video) {
        if (video == null || video.getVideoId() == null || video.getVideoId().isEmpty()) return;
        try {
            FavoriteEntity entity = new FavoriteEntity(
                    video.getVideoId(),
                    video.getTitle(),
                    video.getChannel(),
                    video.getDuration(),
                    System.currentTimeMillis()
            );
            favoriteDao.insert(entity);
        } catch (Exception e) {
            Log.e(TAG, "Error adding favorite: " + e.getMessage());
        }
    }

    public synchronized void removeFavorite(String videoId) {
        if (videoId == null || videoId.isEmpty()) return;
        try {
            favoriteDao.deleteById(videoId);
        } catch (Exception e) {
            Log.e(TAG, "Error removing favorite: " + e.getMessage());
        }
    }

    public synchronized boolean toggleFavorite(VideoItem video) {
        if (video == null || video.getVideoId() == null || video.getVideoId().isEmpty()) return false;
        if (isFavorite(video.getVideoId())) {
            removeFavorite(video.getVideoId());
            return false;
        } else {
            addFavorite(video);
            return true;
        }
    }
}
