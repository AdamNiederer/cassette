package com.example.cassette.data.repositories

import com.example.cassette.data.sources.CacheDataSource
import com.example.cassette.data.sources.LocalMusicSource
import com.example.cassette.data.sources.DiscoveryState
import com.example.cassette.data.types.MusicDiscoveryResult
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Album
import com.example.cassette.data.types.toTrack
import com.example.cassette.data.db.toAlbum
import com.example.cassette.data.types.Artist
import com.example.cassette.data.db.toArtist

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject
import androidx.paging.PagingSource
import androidx.paging.PagingData
import androidx.paging.PagingConfig
import androidx.paging.Pager
import androidx.paging.map
import android.util.Log

import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import com.example.cassette.data.workers.MusicScanWorker

class MusicRepository @Inject constructor(
    private val localMusicSource: LocalMusicSource,
    private val cacheDataSource: CacheDataSource,
    private val workManager: WorkManager
) {
    val updateScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    fun scanMusic() {
        val request = OneTimeWorkRequestBuilder<MusicScanWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        workManager.enqueue(request)
    }

    suspend fun performFullScan(): MusicDiscoveryResult {
        localMusicSource.scanMusicFolder()
        return refreshTracks(force = true)
    }

    fun getTracks(): Flow<List<Track>> {
        return cacheDataSource.getTracks()
    }

    fun getArtists(): Flow<List<String>> {
        return cacheDataSource.getArtists()
    }

    fun getArtistsPaged(): Flow<PagingData<String>> {
        return Pager(config = PagingConfig(pageSize = 20, prefetchDistance = 40, maxSize = 100, enablePlaceholders = true)) {
            cacheDataSource.getArtistsPaged()
        }.flow
    }

    fun getArtistsSummaryPaged(): Flow<PagingData<Artist>> {
        return Pager(config = PagingConfig(pageSize = 20, prefetchDistance = 40, maxSize = 100, enablePlaceholders = true)) {
            cacheDataSource.getArtistsSummaryPaged()
        }.flow.map { pagingData ->
            pagingData.map { it.toArtist() }
        }
    }

    fun getTracksPaged(): Flow<PagingData<Track>> {
        return Pager(config = PagingConfig(pageSize = 20, prefetchDistance = 40, maxSize = 100, enablePlaceholders = true)) {
            cacheDataSource.getTracksPaged()
        }.flow.map { pagingData ->
            pagingData.map { it.toTrack() } 
        }
    }
    
    fun getTrack(id: Long): Flow<Track?> {
        return cacheDataSource.getTrack(id)
    }

    fun getProgress(): StateFlow<DiscoveryState> {
        return localMusicSource.progress
    }
    
    fun getDiscoveryPreviews() = localMusicSource.discoveryPreviews

    suspend fun refreshTracks(force: Boolean = false): MusicDiscoveryResult {
        val lastGeneration = cacheDataSource.getLastGeneration()
        val currentGeneration = localMusicSource.getCurrentGeneration()

        if (!force && lastGeneration != null && currentGeneration != null && lastGeneration == currentGeneration) {
            Log.i("MusicRepository", "Skipping discovery, MediaStore generation unchanged: $currentGeneration")
            return MusicDiscoveryResult(tracks = emptyList(), albums = emptyList(), skipped = true)
        }

        val result = localMusicSource.discoverMusic()

        if (result.error == null) {
            updateScope.launch { 
                cacheDataSource.updateTracks(result.tracks)
                cacheDataSource.updateAlbums(result.albums)
                if (currentGeneration != null) {
                    cacheDataSource.setLastGeneration(currentGeneration)
                }
            }
        }

        return result
    }

    fun getAlbums(): Flow<List<Album>> {
        return cacheDataSource.getAlbums()
    }

    fun getAlbumsPaged(): Flow<PagingData<Album>> {
        return Pager(config = PagingConfig(pageSize = 20, prefetchDistance = 40, enablePlaceholders = true)) {
            cacheDataSource.getAlbumsPaged()
        }.flow.map { pagingData ->
            pagingData.map { it.toAlbum() }
        }
    }

    fun getAlbum(id: String): Flow<Album?> {
        return cacheDataSource.getAlbum(id)
    }

    fun getAlbumsByArtist(artist: String): Flow<List<Album>> {
        return cacheDataSource.getAlbumsByArtist(artist)
    }

    fun getAlbumsByArtistPaged(artist: String): Flow<PagingData<Album>> {
        return Pager(config = PagingConfig(pageSize = 20, prefetchDistance = 40, enablePlaceholders = true)) {
            cacheDataSource.getAlbumsByArtistPaged(artist)
        }.flow.map { pagingData ->
            pagingData.map { it.toAlbum() }
        }
    }

    fun getTracksByAlbum(albumId: String): Flow<List<Track>> {
        return cacheDataSource.getTracksByAlbum(albumId)
    }

    fun getTracksByAlbumPaged(albumId: String): Flow<PagingData<Track>> {
        return Pager(config = PagingConfig(pageSize = 20, prefetchDistance = 40, enablePlaceholders = true)) {
            cacheDataSource.getTracksByAlbumPaged(albumId)
        }.flow.map { pagingData ->
            pagingData.map { it.toTrack() }
        }
    }

    suspend fun toggleFavorite(track: Track) {
        val updatedTrack = track.copy(isFavorite = !track.isFavorite)
        cacheDataSource.updateTracks(listOf(updatedTrack))
    }
}
