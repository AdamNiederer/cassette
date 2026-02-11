package com.example.cassette.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingSource

@Dao
interface ArtistDao {
    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getArtists(): Flow<List<ArtistEntity>>

    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun getArtistsPaged(): PagingSource<Int, ArtistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateArtists(artists: List<ArtistEntity>)

    @Query("DELETE FROM artists")
    suspend fun deleteArtists()
}
