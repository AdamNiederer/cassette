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

    @Query("""
        SELECT 
            a.artist as name,
            (SELECT COUNT(*) FROM albums WHERE albums.artist = a.artist) as albumCount,
            (SELECT COUNT(*) FROM tracks WHERE tracks.artist = a.artist) as trackCount,
            (SELECT thumbnail FROM albums WHERE albums.artist = a.artist ORDER BY name ASC LIMIT 1 OFFSET 0) as thumbnail1,
            (SELECT thumbnail FROM albums WHERE albums.artist = a.artist ORDER BY name ASC LIMIT 1 OFFSET 1) as thumbnail2,
            (SELECT thumbnail FROM albums WHERE albums.artist = a.artist ORDER BY name ASC LIMIT 1 OFFSET 2) as thumbnail3
        FROM albums a
        GROUP BY a.artist
        ORDER BY a.artist ASC
    """)
    fun getArtistSummaries(): Flow<List<ArtistSummary>>

    @Query("""
        SELECT 
            a.artist as name,
            (SELECT COUNT(*) FROM albums WHERE albums.artist = a.artist) as albumCount,
            (SELECT COUNT(*) FROM tracks WHERE tracks.artist = a.artist) as trackCount,
            (SELECT thumbnail FROM albums WHERE albums.artist = a.artist ORDER BY name ASC LIMIT 1 OFFSET 0) as thumbnail1,
            (SELECT thumbnail FROM albums WHERE albums.artist = a.artist ORDER BY name ASC LIMIT 1 OFFSET 1) as thumbnail2,
            (SELECT thumbnail FROM albums WHERE albums.artist = a.artist ORDER BY name ASC LIMIT 1 OFFSET 2) as thumbnail3
        FROM albums a
        GROUP BY a.artist
        ORDER BY a.artist ASC
    """)
    fun getArtistSummariesPaged(): PagingSource<Int, ArtistSummary>

    @Query("SELECT * FROM albums WHERE artist = :artist ORDER BY name ASC")
    fun getAlbumsByArtist(artist: String): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE artist = :artist ORDER BY name ASC")
    fun getAlbumsByArtistPaged(artist: String): PagingSource<Int, AlbumEntity>

    @Query("SELECT * FROM albums WHERE id = :id")
    fun getAlbum(id: String): Flow<AlbumEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAlbums(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums WHERE id IN (:ids)")
    suspend fun deleteAlbums(ids: List<String>)

    @Query("DELETE FROM albums")
    suspend fun deleteAlbums()
}
