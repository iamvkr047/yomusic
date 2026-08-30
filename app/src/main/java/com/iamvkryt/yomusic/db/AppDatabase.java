package com.iamvkryt.yomusic.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.iamvkryt.yomusic.db.dao.FavoriteDao;
import com.iamvkryt.yomusic.db.dao.PlaylistDao;
import com.iamvkryt.yomusic.db.entity.FavoriteEntity;
import com.iamvkryt.yomusic.db.entity.PlaylistEntity;
import com.iamvkryt.yomusic.db.entity.PlaylistSongEntity;

/**
 * Main Room Database for YoMusic.
 * Manages connections and DAOs.
 */
@Database(entities = {FavoriteEntity.class, PlaylistEntity.class, PlaylistSongEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "yomusic_database.db";
    private static volatile AppDatabase instance;

    public abstract FavoriteDao favoriteDao();
    public abstract PlaylistDao playlistDao();

    // private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    //     @Override
    //     public void migrate(@NonNull SupportSQLiteDatabase database) {
    //         // Create playlists table
    //         database.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (" +
    //                 "`playlistId` TEXT NOT NULL, " +
    //                 "`title` TEXT, " +
    //                 "`thumbnail` TEXT, " +
    //                 "`channel` TEXT, " +
    //                 "`isCustom` INTEGER NOT NULL, " +
    //                 "`total` INTEGER NOT NULL, " +
    //                 "`nextPage` TEXT, " +
    //                 "`createdAt` INTEGER NOT NULL, " +
    //                 "PRIMARY KEY(`playlistId`)" +
    //                 ")");

    //         // Create playlist_songs table with foreign key referencing playlists
    //         database.execSQL("CREATE TABLE IF NOT EXISTS `playlist_songs` (" +
    //                 "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
    //                 "`playlistId` TEXT NOT NULL, " +
    //                 "`videoId` TEXT, " +
    //                 "`title` TEXT, " +
    //                 "`channel` TEXT, " +
    //                 "`duration` INTEGER NOT NULL, " +
    //                 "`position` INTEGER NOT NULL, " +
    //                 "FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`playlistId`) ON UPDATE NO ACTION ON DELETE CASCADE " +
    //                 ")");

    //         // Create index in playlist_songs
    //         database.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)");
    //     }
    // };

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
