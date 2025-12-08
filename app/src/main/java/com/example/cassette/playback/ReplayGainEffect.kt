package com.example.cassette.playback

import android.media.audiofx.DynamicsProcessing
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import kotlin.math.log10
import kotlin.math.min

@OptIn(UnstableApi::class)
class ReplayGainEffect {
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var audioSessionId: Int? = null

    fun updateGain(sessionId: Int, preampDb: Float, gainDb: Float, peakLinear: Float) {
        val peakDb = 20 * log10(peakLinear)
        val safeGain = min(preampDb + gainDb, -peakDb)

        if(dynamicsProcessing == null) {
            val builder = DynamicsProcessing.Config.Builder(
                DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,        // Channels (Stereo)
                false, 0, // Pre-EQ
                false, 0, // Multi-band
                false, 0, // Post-EQ
                true      // Limiter (Enabled)
            )

            val limiter = DynamicsProcessing.Limiter(
                true,    // inUse
                true,    // enabled
                0,       // linkGroup
                1.0f,    // attackTime
                60.0f,   // releaseTime
                10.0f,   // ratio
                -0.5f,   // threshold dB
                0.0f     // postGain
            )
            
            val config = builder.build()
            for (i in 0 until 2) {
                config.setLimiterByChannelIndex(i, limiter)
            }

            if(dynamicsProcessing != null && audioSessionId == sessionId) {
                dynamicsProcessing?.setInputGainAllChannelsTo(safeGain)
            } else {
                runCatching {
                    audioSessionId = sessionId
                    dynamicsProcessing = DynamicsProcessing(0, sessionId, config).apply {
                        setInputGainAllChannelsTo(safeGain)
                        setEnabled(true)
                    }
                }
            }
        }
    }

    fun release() {
        dynamicsProcessing?.release()
        dynamicsProcessing = null
    }
}
