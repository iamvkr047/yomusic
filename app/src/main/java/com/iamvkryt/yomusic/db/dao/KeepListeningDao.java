package com.iamvkryt.yomusic.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.iamvkryt.yomusic.db.entity.KeepListeningEntity;

import java.util.List;

/**
 * Data Access Object for Keep Listening / qualified song recommendations.
 */
@Dao
public interface KeepListeningDao {

    @Query("SELECT * FROM keep_listening")
    List<KeepListeningEntity> getAll();

    @Query("SELECT * FROM keep_listening WHERE videoId = :videoId LIMIT 1")
    KeepListeningEntity getByVideoId(String videoId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(KeepListeningEntity entity);

    @Query("DELETE FROM keep_listening WHERE videoId = :videoId")
    void deleteById(String videoId);

    @Query("DELETE FROM keep_listening")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM keep_listening")
    int getCount();
}
