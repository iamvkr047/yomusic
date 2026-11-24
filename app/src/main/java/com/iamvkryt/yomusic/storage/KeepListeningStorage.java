package com.iamvkryt.yomusic.storage;

import android.content.Context;
import android.util.Log;

import com.iamvkryt.yomusic.Song;
import com.iamvkryt.yomusic.db.AppDatabase;
import com.iamvkryt.yomusic.db.dao.KeepListeningDao;
import com.iamvkryt.yomusic.db.entity.KeepListeningEntity;
import com.iamvkryt.yomusic.models.VideoItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Storage facade for Keep Listening / qualified recommendations.
 * Manages qualified songs, age-decay scoring, and 50-item cap eviction.
 */
public class KeepListeningStorage {

    private static final String TAG = "KeepListeningStorage";
    public static final int MAX_CAP = 50;
    private static final double MS_PER_DAY = 1000.0 * 60 * 60 * 24;

    private final KeepListeningDao keepListeningDao;
    private final ExecutorService dbExecutor;

    public KeepListeningStorage(Context context) {
        Context appContext = context.getApplicationContext();
        this.keepListeningDao = AppDatabase.getInstance(appContext).keepListeningDao();
        this.dbExecutor = Executors.newSingleThreadExecutor();
    }

    /**
     * Records a qualified play (>30s continuous listening).
     * If already exists, increments playCount and refreshes lastPlayedAt.
     * If new, inserts with playCount = 1.
     * Enforces MAX_CAP by evicting items with lowest decay score.
     */
    public void recordQualifiedPlay(String videoId, String title, String channel, int duration) {
        if (videoId == null || videoId.trim().isEmpty()) return;

        dbExecutor.execute(() -> {
            try {
                long now = System.currentTimeMillis();
                KeepListeningEntity existing = keepListeningDao.getByVideoId(videoId);

                if (existing != null) {
                    existing.setPlayCount(existing.getPlayCount() + 1);
                    existing.setLastPlayedAt(now);
                    if (title != null && !title.isEmpty()) existing.setTitle(title);
                    if (channel != null && !channel.isEmpty()) existing.setChannel(channel);
                    if (duration > 0) existing.setDuration(duration);
                    keepListeningDao.insert(existing);
                    Log.d(TAG, "Incremented playCount to " + existing.getPlayCount() + " for: " + videoId);
                } else {
                    KeepListeningEntity newEntity = new KeepListeningEntity(
                            videoId,
                            title != null ? title : "",
                            channel != null ? channel : "",
                            duration,
                            1,
                            now,
                            now
                    );
                    keepListeningDao.insert(newEntity);
                    Log.d(TAG, "Recorded new qualified track: " + videoId);
                }

                enforceCap();
            } catch (Exception e) {
                Log.e(TAG, "Error recording qualified play: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Enforces the MAX_CAP (50 items) by evicting entries with the lowest decay score.
     */
    private void enforceCap() {
        try {
            List<KeepListeningEntity> all = keepListeningDao.getAll();
            if (all.size() <= MAX_CAP) return;

            long now = System.currentTimeMillis();
            // Sort ascending by score so lowest scores appear first
            all.sort(Comparator.comparingDouble(item -> calculateScore(item, now)));

            int removeCount = all.size() - MAX_CAP;
            for (int i = 0; i < removeCount; i++) {
                KeepListeningEntity toRemove = all.get(i);
                keepListeningDao.deleteById(toRemove.getVideoId());
                Log.d(TAG, "Evicted low-score track from Keep Listening: " + toRemove.getVideoId());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error enforcing cap: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the time-decayed score for an entity:
     * Score = playCount / (daysSinceLastPlayed + 1.0)
     */
    public static double calculateScore(KeepListeningEntity entity, long now) {
        if (entity == null) return 0.0;
        double daysSinceLastPlayed = Math.max(0.0, (now - entity.getLastPlayedAt()) / MS_PER_DAY);
        return entity.getPlayCount() / (daysSinceLastPlayed + 1.0);
    }

    /**
     * Loads top recommended songs ranked by decay score descending.
     *
     * @param limit maximum number of songs to return (e.g. 10)
     * @return top scored songs
     */
    public synchronized List<Song> getTopRecommendations(int limit) {
        List<Song> result = new ArrayList<>();
        try {
            List<KeepListeningEntity> all = keepListeningDao.getAll();
            if (all == null || all.isEmpty()) return result;

            long now = System.currentTimeMillis();
            all.sort((a, b) -> Double.compare(calculateScore(b, now), calculateScore(a, now)));

            int targetSize = Math.min(limit, all.size());
            for (int i = 0; i < targetSize; i++) {
                KeepListeningEntity entity = all.get(i);
                String thumbnailUrl = "https://img.youtube.com/vi/" + entity.getVideoId() + "/hqdefault.jpg";
                result.add(new Song(
                        entity.getVideoId(),
                        entity.getTitle(),
                        entity.getChannel(),
                        thumbnailUrl
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading top recommendations: " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * Loads all qualified Keep Listening songs ordered by decay score descending,
     * mapped to VideoItem for list display and playback queueing.
     */
    public synchronized List<VideoItem> loadAllKeepListening() {
        List<VideoItem> result = new ArrayList<>();
        try {
            List<KeepListeningEntity> all = keepListeningDao.getAll();
            if (all == null || all.isEmpty()) return result;

            long now = System.currentTimeMillis();
            all.sort((a, b) -> Double.compare(calculateScore(b, now), calculateScore(a, now)));

            for (KeepListeningEntity entity : all) {
                result.add(new VideoItem(
                        entity.getVideoId(),
                        entity.getTitle(),
                        entity.getChannel(),
                        entity.getDuration()
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading keep listening songs: " + e.getMessage(), e);
        }
        return result;
    }

    public void removeFromKeepListening(String videoId) {
        if (videoId == null || videoId.isEmpty()) return;
        dbExecutor.execute(() -> {
            try {
                keepListeningDao.deleteById(videoId);
            } catch (Exception e) {
                Log.e(TAG, "Error removing from keep listening: " + e.getMessage(), e);
            }
        });
    }

    public void clearKeepListening() {
        dbExecutor.execute(() -> {
            try {
                keepListeningDao.deleteAll();
            } catch (Exception e) {
                Log.e(TAG, "Error clearing keep listening: " + e.getMessage(), e);
            }
        });
    }
}

