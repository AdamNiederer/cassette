package com.example.cassette.data.sources


import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.core.graphics.toColorInt
import androidx.paging.PagingSource
import java.io.ByteArrayOutputStream

import com.example.cassette.data.db.TrackDao
import com.example.cassette.data.db.AlbumDao
import com.example.cassette.data.db.ConfigDao
import com.example.cassette.data.db.TrackEntity
import com.example.cassette.data.db.AlbumEntity
import com.example.cassette.data.db.ConfigEntity
import com.example.cassette.data.db.toEntity
import com.example.cassette.data.db.toAlbum
import com.example.cassette.data.types.toEntity
import com.example.cassette.data.types.toTrack
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Album
import com.example.cassette.data.types.TrackQueueItem
import com.example.cassette.data.db.toArtist
import com.example.cassette.data.types.Artist

import com.example.cassette.data.db.ArtistDao
import com.example.cassette.data.db.ArtistEntity
import com.example.cassette.utils.bakeArtistThumbnail
import android.content.Context
import androidx.wear.compose.material.ChipDefaults
import androidx.compose.ui.unit.dp
import android.util.TypedValue
import kotlinx.coroutines.flow.first

class CacheDataSource @Inject constructor(
    private val trackDao: TrackDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val configDao: ConfigDao,
    private val context: Context
) {
    
    fun Context.dpToPx(dp: androidx.compose.ui.unit.Dp): Int {
        val metrics = resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.value, metrics).toInt()
    }

    suspend fun getLastGeneration(): Long? {
        return configDao.getValue("mediastore_generation")?.toLongOrNull()
    }

    suspend fun setLastGeneration(generation: Long) {
        configDao.setValue(ConfigEntity("mediastore_generation", generation.toString()))
    }

    suspend fun isOnboardingCompleted(): Boolean {
        return configDao.getValue("onboarding_completed") == "true"
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        configDao.setValue(ConfigEntity("onboarding_completed", completed.toString()))
    }

    suspend fun getLyricsOffset(): Long {
        return configDao.getValue("lyrics_offset")?.toLongOrNull() ?: 500L
    }

    suspend fun setLyricsOffset(offset: Long) {
        configDao.setValue(ConfigEntity("lyrics_offset", offset.toString()))
    }

    suspend fun getReplayGainMode(): String {
        return configDao.getValue("replay_gain_mode") ?: "None"
    }

    suspend fun setReplayGainMode(mode: String) {
        configDao.setValue(ConfigEntity("replay_gain_mode", mode))
    }

    suspend fun getReplayGainPreamp(): Float {
        return configDao.getValue("replay_gain_preamp")?.toFloatOrNull() ?: 0.0f
    }

    suspend fun setReplayGainPreamp(gain: Float) {
        configDao.setValue(ConfigEntity("replay_gain_preamp", gain.toString()))
    }

    suspend fun getLyricsBlankLineInterval(): Long {
        return configDao.getValue("lyrics_blank_line_interval")?.toLongOrNull() ?: 12000L
    }

    suspend fun setLyricsBlankLineInterval(interval: Long) {
        configDao.setValue(ConfigEntity("lyrics_blank_line_interval", interval.toString()))
    }

    fun getTracks(): Flow<List<Track>> {
        return trackDao.getTracks().map { entities ->
            entities.map { it.toTrack() }
        }
    }

    fun getArtists(): Flow<List<String>> {
        return trackDao.getArtists()
    }

    fun getArtistsPaged(): PagingSource<Int, String> {
        return trackDao.getArtistsPaged()
    }

    fun getArtistsSummary(): Flow<List<Artist>> {
        return artistDao.getArtists().map { entities ->
            entities.map { it.toArtist() }
        }
    }

    fun getArtistsSummaryPaged(): PagingSource<Int, ArtistEntity> {
        return artistDao.getArtistsPaged()
    }

    suspend fun updateArtists() {
        val allAlbums = albumDao.getAlbums().first()
        val allTracks = trackDao.getTracks().first()
        val tracksByArtist = allTracks.groupBy { it.artist }

        val artists = allAlbums.groupBy { it.artist }.map { (artistName, albums) ->
            val albumCount = albums.size
            val trackCount = tracksByArtist[artistName]?.size ?: 0

            val thumbnails = albums.sortedBy { it.name }.take(3).mapNotNull { entity ->
                entity.thumbnail?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }

            val bakedThumbnail = bakeArtistThumbnail(
                thumbnails = thumbnails,
                iconSize = context.dpToPx(ChipDefaults.LargeIconSize),
                paddingPx = context.dpToPx(4.dp),
                borderPx = context.dpToPx(2.dp),
                surfaceColor = "#303133".toColorInt(),
                placeholderColor = Color.DKGRAY,
            )

            val thumbnailBytes = ByteArrayOutputStream().apply {
                bakedThumbnail.compress(Bitmap.CompressFormat.WEBP_LOSSY, 20, this)
            }.toByteArray()

            ArtistEntity(
                name = artistName,
                albumCount = albumCount,
                trackCount = trackCount,
                thumbnail = thumbnailBytes
            )
        }

        artistDao.deleteArtists()
        artistDao.updateArtists(artists)
    }

    fun getTracksPaged(): PagingSource<Int, TrackEntity> {
        return trackDao.getTracksPaged()
    }

    suspend fun getTracksMetadata(): List<TrackQueueItem> {
        return trackDao.getTracksMetadata()
    }

    suspend fun getTracksAfter(title: String, id: Long): List<TrackQueueItem> {
        return trackDao.getTracksAfter(title, id)
    }

    suspend fun getTracksByAlbumQueue(artist: String, albumName: String): List<TrackQueueItem> {
        return trackDao.getTracksByAlbumQueue(artist, albumName)
    }
    
    fun getTrack(id: Long): Flow<Track?> {
        return trackDao.getTrack(id).map { entity ->
            entity?.toTrack()
        }
    }

    fun getAlbums(): Flow<List<Album>> {
        return albumDao.getAlbums().map { entities ->
            entities.map { it.toAlbum() }
        }
    }

    fun getAlbumsPaged(): PagingSource<Int, AlbumEntity> {
        return albumDao.getAlbumsPaged()
    }

    fun getAlbum(artist: String, name: String): Flow<Album?> {
        return albumDao.getAlbum(artist, name).map { it?.toAlbum() }
    }

    fun getAlbumsByArtist(artist: String): Flow<List<Album>> {
        return albumDao.getAlbumsByArtist(artist).map { entities ->
            entities.map { it.toAlbum() }
        }
    }

    fun getAlbumsByArtistPaged(artist: String): PagingSource<Int, AlbumEntity> {
        return albumDao.getAlbumsByArtistPaged(artist)
    }

    fun getTracksByAlbum(artist: String, albumName: String): Flow<List<Track>> {
        return trackDao.getTracksByAlbum(artist, albumName).map { entities ->
            entities.map { it.toTrack() }
        }
    }

    fun getTracksByAlbumPaged(artist: String, albumName: String): PagingSource<Int, TrackEntity> {
        return trackDao.getTracksByAlbumPaged(artist, albumName)
    }
    
    suspend fun updateTracks(tracks: List<Track>) {
        trackDao.deleteTracks()
        trackDao.updateTracks(tracks.map(Track::toEntity))
    }

    suspend fun updateAlbums(albums: List<Album>) {
        albumDao.deleteAlbums()
        albumDao.updateAlbums(albums.map(Album::toEntity))
    }
    
    suspend fun deleteTracks(ids: List<Long>) {
        trackDao.deleteTracks(ids)
    }
}
