package com.example.cassette.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface AlbumDao {
    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums ORDER BY name ASC")
    fun getAlbumsPaged(): PagingSource<Int, AlbumEntity>

    @Query("SELECT * FROM albums WHERE artist = :artist ORDER BY name ASC")
    fun getAlbumsByArtist(artist: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE artist = :artist ORDER BY name ASC")
    fun getAlbumsByArtistPaged(artist: String): PagingSource<Int, AlbumEntity>

    @Query("SELECT * FROM albums WHERE artist = :artist AND name = :name")
    fun getAlbum(artist: String, name: String): Flow<AlbumEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun deleteAlbums()
}
