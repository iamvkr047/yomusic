package com.iamvkryt.yomusic.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.iamvkryt.yomusic.db.entity.HistoryEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for managing song history in Room DB.
 */
@Dao
public interface HistoryDao {

    @Query("SELECT * FROM history ORDER BY playedAt DESC")
    List<HistoryEntity> getAllHistory();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HistoryEntity history);

    @Query("DELETE FROM history WHERE videoId = :videoId")
    void deleteById(String videoId);

    @Query("DELETE FROM history")
    void deleteAll();
}
