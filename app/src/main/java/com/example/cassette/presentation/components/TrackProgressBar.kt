package com.example.cassette.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.concurrent.TimeUnit
import androidx.wear.compose.material.Text

import com.example.cassette.presentation.components.PartialCircularProgressIndicator

@Composable
fun TrackProgressBar(
    progress: Float,
    color: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        PartialCircularProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxSize(),
            startAngle = 210f,
            trackSweep = 120f,
            color = color,
            strokeWidth = 8.dp
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
