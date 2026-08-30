package com.iamvkryt.yomusic.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.iamvkryt.yomusic.db.entity.PlaylistEntity;
import com.iamvkryt.yomusic.db.entity.PlaylistSongEntity;

import java.util.List;

/**
 * Data Access Object (DAO) for managing playlists and their songs in Room DB.
 */
@Dao
public interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    List<PlaylistEntity> getAllPlaylists();

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    List<PlaylistSongEntity> getSongsForPlaylist(String playlistId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylist(PlaylistEntity playlist);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylistSongs(List<PlaylistSongEntity> songs);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void deleteSongsForPlaylist(String playlistId);

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    void deletePlaylistById(String playlistId);

    @Query("DELETE FROM playlists")
    void deleteAllPlaylists();

    @Transaction
    default void insertPlaylistWithSongs(PlaylistEntity playlist, List<PlaylistSongEntity> songs) {
        // Clear existing songs for this playlist to avoid duplicates or orphaned entries
        deleteSongsForPlaylist(playlist.getPlaylistId());
        
        // Insert playlist metadata
        insertPlaylist(playlist);
        
        // Insert new songs list
        if (songs != null && !songs.isEmpty()) {
            insertPlaylistSongs(songs);
        }
    }
}
