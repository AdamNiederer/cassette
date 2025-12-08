package com.example.cassette.presentation.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import android.util.Log
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay

@Composable
fun VolumeBar(
    currentVolume: Int,
    maxVolume: Int,
    color: Color,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        PartialCircularProgressIndicator(
            progress = currentVolume.toFloat() / maxVolume.toFloat(),
            modifier = Modifier.fillMaxSize(),
            startAngle = 30f,
            trackSweep = 120f,
            color = color,
            strokeWidth = 8.dp
        )
    }
}
