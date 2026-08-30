package com.iamvkryt.yomusic.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.iamvkryt.yomusic.db.entity.FavoriteEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for managing favorite songs in Room DB.
 * Defines SQL queries and database operations.
 */
@Dao
public interface FavoriteDao {

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    List<FavoriteEntity> getAllFavorites();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FavoriteEntity favorite);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FavoriteEntity> favorites);

    @Query("DELETE FROM favorites WHERE videoId = :videoId")
    void deleteById(String videoId);

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE videoId = :videoId)")
    boolean isFavorite(String videoId);

    @Query("DELETE FROM favorites")
    void deleteAll();
}
