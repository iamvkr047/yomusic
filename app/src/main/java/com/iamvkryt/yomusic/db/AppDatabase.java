package com.iamvkryt.yomusic.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.iamvkryt.yomusic.db.dao.FavoriteDao;
import com.iamvkryt.yomusic.db.dao.HistoryDao;
import com.iamvkryt.yomusic.db.dao.KeepListeningDao;
import com.iamvkryt.yomusic.db.dao.PlaylistDao;
import com.iamvkryt.yomusic.db.entity.FavoriteEntity;
import com.iamvkryt.yomusic.db.entity.HistoryEntity;
import com.iamvkryt.yomusic.db.entity.KeepListeningEntity;
import com.iamvkryt.yomusic.db.entity.PlaylistEntity;
import com.iamvkryt.yomusic.db.entity.PlaylistSongEntity;

/**
 * Main Room Database for YoMusic.
 * Manages connections and DAOs.
 */
@Database(entities = {FavoriteEntity.class, PlaylistEntity.class, PlaylistSongEntity.class, HistoryEntity.class, KeepListeningEntity.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "yomusic_database.db";
    private static volatile AppDatabase instance;

    public abstract FavoriteDao favoriteDao();
    public abstract PlaylistDao playlistDao();
    public abstract HistoryDao historyDao();
    public abstract KeepListeningDao keepListeningDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `keep_listening` (" +
                    "`videoId` TEXT NOT NULL, " +
                    "`title` TEXT, " +
                    "`channel` TEXT, " +
                    "`duration` INTEGER NOT NULL, " +
                    "`playCount` INTEGER NOT NULL, " +
                    "`firstQualifiedAt` INTEGER NOT NULL, " +
                    "`lastPlayedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`videoId`)" +
                    ")");
        }
    };

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_1_2)
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
