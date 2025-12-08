package com.example.cassette.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cassette.data.repositories.PlayerRepository
import com.example.cassette.data.repositories.MusicRepository
import com.example.cassette.data.repositories.VolumeRepository
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Album
import com.example.cassette.data.types.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import android.util.Log
import android.content.Context

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val musicRepository: MusicRepository,
    private val volumeRepository: VolumeRepository,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {
    val volume = volumeRepository.volume
    val maxVolume = volumeRepository.maxVolume

    override fun onCleared() {
        super.onCleared()
    }
    
    fun play(track: Track, source: PlayerRepository.QueueSource = PlayerRepository.QueueSource.ALL_TRACKS) {
        playerRepository.play(track, source)
    }

    fun play() {
        playerRepository.play()
    }
    
    fun getTrackById(trackId: Long): Flow<Track?> {
        return musicRepository.getTrack(trackId)
    }
    
    fun pause() {
        playerRepository.pause()
    }
    
    fun stop() {
        playerRepository.stop()
    }
    
    fun seekTo(position: Long) {
        playerRepository.seekTo(position)
    }

    fun seekToNext() {
        playerRepository.seekToNext()
    }

    fun seekToPrevious() {
        playerRepository.seekToPrevious()
    }
    
    val currentTrack = playerRepository.currentTrack
    val currentLyric = playerRepository.currentLyric
    val lyricsList = playerRepository.parsedLyrics
    val playbackState = playerRepository.playbackState

    fun getAlbum(artist: String, album: String): Flow<Album?> {
        return musicRepository.getAlbum("$artist|$album")
    }

    fun setVolume(index: Int) {
        volumeRepository.setVolume(index)
    }
}
