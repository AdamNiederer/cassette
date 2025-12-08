package com.example.cassette.data.repositories

import android.net.Uri
import android.util.Log
import android.content.Intent
import android.content.ComponentName
import android.graphics.Bitmap
import android.os.Bundle
import android.graphics.Point
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.Listener
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaMetadata
import com.example.cassette.data.sources.CacheDataSource
import com.example.cassette.data.repositories.ConfigRepository
import com.example.cassette.playback.ReplayGainEffect
import com.example.cassette.data.types.PlayerState
import com.example.cassette.data.types.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.example.cassette.complication.MainComplicationService

import com.example.cassette.utils.LyricLine
import com.example.cassette.utils.parseLrc

data class PlayerState(
    val isPlaying: Boolean,
    val playWhenReady: Boolean,
    val isPrepared: Boolean,
    val duration: Long,
    val currentPosition: Long
)

class PlayerRepository @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val cacheDataSource: CacheDataSource,
    private val configRepository: ConfigRepository,
    private val exoPlayer: ExoPlayer
) {
    
    val currentTrack = MutableStateFlow<Track?>(null)
    val currentLyric = MutableStateFlow<String?>(null)
    val parsedLyrics = MutableStateFlow<List<LyricLine>?>(null)
    val playbackState = MutableStateFlow(getCurrentState())
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val replayGainEffect = ReplayGainEffect()
    
    init {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    mediaItem?.mediaId?.toLongOrNull()?.let { id ->
                        serviceScope.launch {
                            cacheDataSource.getTrack(id).first()?.let { currentTrack.value = it }
                        }
                    }
                }

                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    serviceScope.launch {
                        applyCurrentReplayGain(audioSessionId)
                    }
                }

                override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                    Log.i("onPositionDiscontinuity", "${oldPosition} -> ${newPosition}")
                    updateCurrentLyric()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playbackState.value = getCurrentState()
                }

                override fun onPlaybackStateChanged(state: Int) {
                    playbackState.value = getCurrentState()
                }
            })

        serviceScope.launch {
            while (true) {
                playbackState.value = getCurrentState()
                delay(2000)
            }
        }

        serviceScope.launch {
            currentTrack.collect { track ->
                updateLyrics(track)
                if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    applyCurrentReplayGain(exoPlayer.audioSessionId)
                }

                ComplicationDataSourceUpdateRequester.create(
                    context,
                    ComponentName(context, MainComplicationService::class.java)
                ).requestUpdateAll()
            }
        }

        serviceScope.launch {
            while (true) {
                if (exoPlayer.isPlaying) {
                    updateCurrentLyric()
                    val pos = exoPlayer.currentPosition
                    val next = parsedLyrics.value?.firstOrNull { it.timestampMs > pos }
                    delay(next?.timestampMs?.let { (it - pos).coerceAtMost(5000) } ?: 5000)
                } else {
                    delay(1000)
                }
            }
        }

        serviceScope.launch {
            configRepository.settingsChanged.collect {
                updateLyrics(currentTrack.value)
                if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                    applyCurrentReplayGain(exoPlayer.audioSessionId)
                }
            }
        }
    }

    private suspend fun updateLyrics(track: Track?) {
        val offset = configRepository.getLyricsOffset()
        val blankLineInterval = configRepository.getLyricsBlankLineInterval()
        parsedLyrics.value = parseLrc(track?.lyrics, track?.durationMs ?: 0L, offset, blankLineInterval)
        updateCurrentLyric()
    }

    private suspend fun applyCurrentReplayGain(sessionId: Int) {
        val track = currentTrack.value ?: return
        val mode = configRepository.getReplayGainMode()
        
        if (mode == "None") {
            replayGainEffect.release()
            return
        }

        var gain: Float?
        var peak: Float?

        if (mode == "Album") {
            val album = cacheDataSource.getAlbum(track.albumId).first()
            gain = album?.albumGain ?: track.trackGain
            peak = album?.albumPeak ?: track.trackPeak
        } else { 
            gain = track.trackGain
            peak = track.trackPeak
        }

        if (gain != null) {
            val preampGain = configRepository.getReplayGainPreamp()
            replayGainEffect.updateGain(sessionId, preampGain, gain, peak ?: 1.0f)
        } else {
            replayGainEffect.release()
        }
    }

    private fun updateCurrentLyric() {
        val pos = exoPlayer.currentPosition
        val line = parsedLyrics.value?.lastOrNull { it.timestampMs <= pos }?.text
        if (currentLyric.value != line) {
            currentLyric.value = line
        }
    }

    private fun startService() {
        val intent = Intent(context, com.example.cassette.playback.PlaybackService::class.java)
        context.startForegroundService(intent)
    }

    enum class QueueSource {
        ALBUM,
        ALL_TRACKS
    }
    
    fun play(track: Track, source: QueueSource = QueueSource.ALL_TRACKS) {
        if (currentTrack.value?.id == track.id && exoPlayer.playbackState != Player.STATE_IDLE) {
            return play()
        }

        serviceScope.launch {
            startService()
            currentTrack.value = track
            playbackState.value = PlayerState(
                isPlaying = false,
                playWhenReady = true,
                isPrepared = true,
                duration = 1,
                currentPosition = 0,
            )

            // val artworkData = withContext(Dispatchers.IO) {
            //     val albumId = "${track.artist}|${track.album}"
            //     val album = cacheDataSource.getAlbum(albumId).first()
            //     album?.thumbnail?.let { bitmap ->
            //         val stream = ByteArrayOutputStream()
            //         bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, stream)
            //         stream.toByteArray()
            //     }
            // }

            val tracksToLoad = when (source) {
                QueueSource.ALBUM -> {
                    val albumTracks = cacheDataSource.getTracksByAlbumQueue(track.albumId)
                    // Ensure the start track is included/found
                    if (albumTracks.any { it.id == track.id }) albumTracks else listOf(track.toQueueItem()) + albumTracks
                }
                QueueSource.ALL_TRACKS -> {
                    val nextTracks = cacheDataSource.getTracksAfter(track.title, track.id)
                    listOf(track.toQueueItem()) + nextTracks
                }
            }

            val mediaItems = tracksToLoad.map { item ->
                createMediaItem(item.id, item.title, item.artist, item.album, item.uri, null)
            }

            val startIndex = tracksToLoad.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

            exoPlayer.setMediaItems(mediaItems, startIndex, 0)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    private fun createMediaItem(id: Long, title: String, artist: String, album: String, uri: String, artworkData: ByteArray?): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)

        if (artworkData != null) {
            metadataBuilder.setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        return MediaItem.Builder()
            .setUri(Uri.parse(uri))
            .setMediaId(id.toString())
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }
    
    private fun Track.toQueueItem() = com.example.cassette.data.types.TrackQueueItem(id, title, artist, album, uri)

    fun play() {
        if (!exoPlayer.isPlaying) {
            startService()
            exoPlayer.play()
        }
        playbackState.value = getCurrentState()
    }
    
    fun pause() {
        exoPlayer.pause()
        playbackState.value = getCurrentState()
    }
    
    fun stop() {
        exoPlayer.stop()
        val intent = Intent(context, com.example.cassette.playback.PlaybackService::class.java)
        context.stopService(intent)
        playbackState.value = getCurrentState()
    }
    
    fun seekTo(position: Long) {
        exoPlayer.seekTo(position)
        playbackState.value = getCurrentState()
    }

    fun seekToNext() {
        exoPlayer.seekToNext()
        play()
        playbackState.value = getCurrentState()
    }

    fun seekToPrevious() {
        exoPlayer.seekToPrevious()
        play()
        playbackState.value = getCurrentState()
    }
    
    private fun getCurrentState(): PlayerState {
        return PlayerState(
            isPlaying = exoPlayer.isPlaying,
            playWhenReady = exoPlayer.playWhenReady,
            isPrepared = exoPlayer.playbackState != Player.STATE_IDLE,
            duration = exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: 0,
            currentPosition = exoPlayer.currentPosition
        )
    }
}
