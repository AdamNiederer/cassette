package com.example.cassette.data.repositories

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VolumeRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    val volume = MutableStateFlow(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
    val maxVolume = MutableStateFlow(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.media.VOLUME_CHANGED_ACTION") {
                updateVolume()
            }
        }
    }

    init {
        val filter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        ContextCompat.registerReceiver(context, volumeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun updateVolume() {
        volume.update { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }
        val currentMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (currentMax > 0) {
            maxVolume.value = currentMax
        }
    }

    fun setVolume(index: Int) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val clamped = index.coerceIn(0, max)
        volume.value = clamped
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            clamped,
            AudioManager.FLAG_VIBRATE,
        )
    }
}
