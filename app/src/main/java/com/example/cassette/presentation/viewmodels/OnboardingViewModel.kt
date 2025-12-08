package com.example.cassette.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cassette.data.repositories.ConfigRepository
import com.example.cassette.data.repositories.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {
    private val _isOnboardingCompleted = MutableStateFlow<Boolean?>(null)
    val isOnboardingCompleted = _isOnboardingCompleted.asStateFlow()

    init {
        viewModelScope.launch {
            _isOnboardingCompleted.value = configRepository.isOnboardingCompleted()
        }
    }

    fun rescan() {
        musicRepository.scanMusic()
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            configRepository.setOnboardingCompleted(completed)
            _isOnboardingCompleted.value = completed
        }
    }
}
