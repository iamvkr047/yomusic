package com.iamvkryt.yomusic.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.iamvkryt.yomusic.db.dao.FavoriteDao;
import com.iamvkryt.yomusic.db.entity.FavoriteEntity;

/**
 * Main Room Database for YoMusic.
 * Manages connections and DAOs.
 */
@Database(entities = {FavoriteEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "yomusic_database.db";
    private static volatile AppDatabase instance;

    public abstract FavoriteDao favoriteDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
