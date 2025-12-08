package com.example.cassette.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource
import com.example.cassette.data.types.TrackQueueItem

@Dao
interface TrackDao {
    
    @Query("SELECT DISTINCT artist FROM tracks ORDER BY artist ASC")
    fun getArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT artist FROM tracks ORDER BY artist ASC")
    fun getArtistsPaged(): PagingSource<Int, String>

    @Query("SELECT * FROM tracks ORDER BY artist ASC, album ASC, disc_number ASC, track_number ASC")
    fun getTracks(): Flow<List<TrackEntity>>

    @Query("SELECT id, title, artist, album, uri FROM tracks ORDER BY artist ASC, album ASC, disc_number ASC, track_number ASC")
    suspend fun getTracksMetadata(): List<TrackQueueItem>

    @Query("SELECT id, title, artist, album, uri FROM tracks WHERE title > :title OR (title = :title AND id > :id) ORDER BY title ASC, id ASC LIMIT 100")
    suspend fun getTracksAfter(title: String, id: Long): List<TrackQueueItem>

    @Query("SELECT * FROM tracks ORDER BY title ASC")
    fun getTracksPaged(): PagingSource<Int, TrackEntity>

    @Query("SELECT * FROM tracks WHERE album_id = :albumId ORDER BY disc_number ASC, track_number ASC")
    fun getTracksByAlbum(albumId: String): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE album_id = :albumId ORDER BY disc_number ASC, track_number ASC")
    fun getTracksByAlbumPaged(albumId: String): PagingSource<Int, TrackEntity>

    @Query("SELECT id, title, artist, album, uri FROM tracks WHERE album_id = :albumId ORDER BY disc_number ASC, track_number ASC")
    suspend fun getTracksByAlbumQueue(albumId: String): List<TrackQueueItem>
    
    @Query("SELECT * FROM tracks WHERE id = :id")
    fun getTrack(id: Long): Flow<TrackEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateTracks(tracks: List<TrackEntity>)
    
    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteTracks(ids: List<Long>)

    @Query("DELETE FROM tracks")
    suspend fun deleteTracks()
}
