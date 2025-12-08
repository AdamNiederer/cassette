package com.example.cassette.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

import com.example.cassette.data.repositories.MusicRepository
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Album
import com.example.cassette.data.types.Artist
import com.example.cassette.data.types.DiscoveryPreview
import kotlinx.coroutines.delay

sealed class TrackListState {
    object Idle : TrackListState()
    object Loading : TrackListState()
    data class Error(val message: String) : TrackListState()
}

@HiltViewModel
class TrackListViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {
    val state = MutableStateFlow<TrackListState>(TrackListState.Idle)
    val trackPager = musicRepository.getTracksPaged().cachedIn(viewModelScope)
    val artists = musicRepository.getArtists()
    val artistsPaged = musicRepository.getArtistsSummaryPaged().cachedIn(viewModelScope)
    val albums = musicRepository.getAlbums()
    val albumsPaged = musicRepository.getAlbumsPaged().cachedIn(viewModelScope)
    val progress = musicRepository.getProgress()
    val currentPreview = MutableStateFlow<DiscoveryPreview?>(null)
    
    private val previews = ArrayDeque<DiscoveryPreview>(4)

    init {
        viewModelScope.launch {
            musicRepository.getDiscoveryPreviews().collect {
                if(currentPreview.value == null) {
                    currentPreview.value = it
                }
                if(previews.size < 4) {
                    previews.addLast(it)
                }
            }
        }
        
        viewModelScope.launch {
             musicRepository.getProgress().collect { p ->
                 if (p !is com.example.cassette.data.sources.DiscoveryState.Idle) {
                     state.value = TrackListState.Loading
                 } else {
                     if (state.value is TrackListState.Loading) {
                         state.value = TrackListState.Idle
                     }
                 }
             }
        }

        viewModelScope.launch {
            while (true) {
                if (previews.isNotEmpty()) {
                    val preview = previews.removeFirst()
                    currentPreview.value = preview
                }
                delay(5000)
            }
        }
    }

    fun refreshTracks() {
        previews.clear()
        currentPreview.value = null
        viewModelScope.launch {
            state.value = TrackListState.Loading 
            val result = musicRepository.refreshTracks()
            
            if (result.error != null) {
                state.value = TrackListState.Error(result.error)
            } else {
                state.value = TrackListState.Idle
            }

            previews.clear();
        }
    }

    fun rescan() {
        previews.clear()
        currentPreview.value = null
        state.value = TrackListState.Loading
        musicRepository.scanMusic()
    }

    fun getAlbumById(id: String): Flow<Album?> {
        return musicRepository.getAlbum(id)
    }

    fun getAlbumsByArtist(artist: String): Flow<List<Album>> {
        return musicRepository.getAlbumsByArtist(artist)
    }

    fun getAlbumsByArtistPaged(artist: String): Flow<PagingData<Album>> {
        return musicRepository.getAlbumsByArtistPaged(artist).cachedIn(viewModelScope)
    }

    fun getTracksByAlbum(albumId: String): Flow<List<Track>> {
        return musicRepository.getTracksByAlbum(albumId)
    }

    fun getTracksByAlbumPaged(albumId: String): Flow<PagingData<Track>> {
        return musicRepository.getTracksByAlbumPaged(albumId).cachedIn(viewModelScope)
    }
}
