package com.example.cassette.data.repositories

import com.example.cassette.data.sources.CacheDataSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigRepository @Inject constructor(
    private val cacheDataSource: CacheDataSource
) {
    private val _settingsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val settingsChanged = _settingsChanged.asSharedFlow()

    suspend fun isOnboardingCompleted(): Boolean {
        return cacheDataSource.isOnboardingCompleted()
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        cacheDataSource.setOnboardingCompleted(completed)
        _settingsChanged.emit(Unit)
    }

    suspend fun getLyricsOffset(): Long {
        return cacheDataSource.getLyricsOffset()
    }

    suspend fun setLyricsOffset(offset: Long) {
        cacheDataSource.setLyricsOffset(offset)
        _settingsChanged.emit(Unit)
    }

    suspend fun getReplayGainMode(): String {
        return cacheDataSource.getReplayGainMode()
    }

    suspend fun setReplayGainMode(mode: String) {
        cacheDataSource.setReplayGainMode(mode)
        _settingsChanged.emit(Unit)
    }

    suspend fun getReplayGainPreamp(): Float {
        return cacheDataSource.getReplayGainPreamp()
    }

    suspend fun setReplayGainPreamp(gain: Float) {
        cacheDataSource.setReplayGainPreamp(gain)
        _settingsChanged.emit(Unit)
    }

    suspend fun getLyricsBlankLineInterval(): Long {
        return cacheDataSource.getLyricsBlankLineInterval()
    }

    suspend fun setLyricsBlankLineInterval(interval: Long) {
        cacheDataSource.setLyricsBlankLineInterval(interval)
        _settingsChanged.emit(Unit)
    }
}