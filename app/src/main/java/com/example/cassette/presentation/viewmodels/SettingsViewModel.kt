package com.example.cassette.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cassette.data.repositories.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {
    
    private val _lyricsOffset = MutableStateFlow(500L)
    val lyricsOffset = _lyricsOffset.asStateFlow()
    
    private val _replayGainMode = MutableStateFlow("None")
    val replayGainMode = _replayGainMode.asStateFlow()

    private val _replayGainPreamp = MutableStateFlow(0.0f)
    val replayGainPreamp = _replayGainPreamp.asStateFlow()

    private val _lyricsBlankLineInterval = MutableStateFlow(12000L)
    val lyricsBlankLineInterval = _lyricsBlankLineInterval.asStateFlow()

    init {
        viewModelScope.launch {
            _lyricsOffset.value = configRepository.getLyricsOffset()
            _replayGainMode.value = configRepository.getReplayGainMode()
            _replayGainPreamp.value = configRepository.getReplayGainPreamp()
            _lyricsBlankLineInterval.value = configRepository.getLyricsBlankLineInterval()
        }
    }

    fun setLyricsOffset(offset: Long) {
        viewModelScope.launch {
            configRepository.setLyricsOffset(offset)
            _lyricsOffset.value = offset
        }
    }

    fun setReplayGainMode(mode: String) {
        viewModelScope.launch {
            configRepository.setReplayGainMode(mode)
            _replayGainMode.value = mode
        }
    }

    fun setReplayGainPreamp(gain: Float) {
        viewModelScope.launch {
            configRepository.setReplayGainPreamp(gain)
            _replayGainPreamp.value = gain
        }
    }

    fun setLyricsBlankLineInterval(interval: Long) {
        viewModelScope.launch {
            configRepository.setLyricsBlankLineInterval(interval)
            _lyricsBlankLineInterval.value = interval
        }
    }
}
