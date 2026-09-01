package com.iamvkryt.yomusic.storage;

import android.content.Context;
import android.util.Log;

import com.iamvkryt.yomusic.db.AppDatabase;
import com.iamvkryt.yomusic.db.dao.HistoryDao;
import com.iamvkryt.yomusic.db.entity.HistoryEntity;
import com.iamvkryt.yomusic.models.VideoItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage facade for Song History, powered by Room Database.
 */
public class HistoryStorage {
    private static final String TAG = "HistoryStorage";
    public static final int MAX_HISTORY_SIZE = 20;
    private final HistoryDao historyDao;

    public HistoryStorage(Context context) {
        Context appContext = context.getApplicationContext();
        this.historyDao = AppDatabase.getInstance(appContext).historyDao();
    }

    public synchronized List<VideoItem> loadHistory() {
        List<VideoItem> result = new ArrayList<>();
        try {
            List<HistoryEntity> entities = historyDao.getAllHistory();
            for (HistoryEntity entity : entities) {
                result.add(new VideoItem(
                        entity.getVideoId(),
                        entity.getTitle(),
                        entity.getChannel(),
                        entity.getDuration()
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading history from Room DB: " + e.getMessage());
        }
        return result;
    }

    public synchronized void addToHistory(VideoItem video) {
        if (video == null || video.getVideoId() == null || video.getVideoId().isEmpty()) return;
        addToHistory(video.getVideoId(), video.getTitle(), video.getChannel(), video.getDuration());
    }

    public synchronized void addToHistory(String videoId, String title, String channel, int duration) {
        if (videoId == null || videoId.isEmpty()) return;
        try {
            HistoryEntity entity = new HistoryEntity(
                    videoId,
                    title != null ? title : "",
                    channel != null ? channel : "",
                    duration,
                    System.currentTimeMillis()
            );
            historyDao.insert(entity);

            // Keep only max limit of recent history items (default 20)
            List<HistoryEntity> entities = historyDao.getAllHistory();
            if (entities.size() > MAX_HISTORY_SIZE) {
                for (int i = MAX_HISTORY_SIZE; i < entities.size(); i++) {
                    historyDao.deleteById(entities.get(i).getVideoId());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding to history: " + e.getMessage());
        }
    }

    public synchronized void removeFromHistory(String videoId) {
        if (videoId == null || videoId.isEmpty()) return;
        try {
            historyDao.deleteById(videoId);
        } catch (Exception e) {
            Log.e(TAG, "Error removing from history: " + e.getMessage());
        }
    }

    public synchronized void clearHistory() {
        try {
            historyDao.deleteAll();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing history: " + e.getMessage());
        }
    }
}
