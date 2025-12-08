package com.example.cassette.presentation.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.wear.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.IconButtonDefaults
import com.example.cassette.R
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.screens.player.PlayerScreen
import kotlinx.coroutines.delay

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun PlaybackControls(
    isPlaying: Boolean,
    playWhenReady: Boolean,
    songTitle: String,
    artistName: String,
    albumName: String,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekToPrevious: () -> Unit,
    onSeekToNext: () -> Unit,
    iconTint: Color = Color.White,
) {
    var debouncedIsPlaying by remember(isPlaying, playWhenReady) { mutableStateOf(isPlaying || playWhenReady) }

    val prevPainter = painterResource(id = R.drawable.ic_prev)
    val nextPainter = painterResource(id = R.drawable.ic_next)
    val playPausePainter = painterResource(id = if (debouncedIsPlaying) R.drawable.ic_pause else R.drawable.ic_play)

    PlayerScreen(
        mediaDisplay = {
            Text(
                text = songTitle,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                fontSize = 2.0.em,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp),
                    // .basicMarquee(iterations = Int.MAX_VALUE),
                
            )
        },
        controlButtons = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onSeekToPrevious,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        painter = prevPainter,
                        contentDescription = "Previous",
                        tint = iconTint
                    )
                }
                IconButton(
                    onClick = if (debouncedIsPlaying) onPause else onPlay,
                    modifier = Modifier.size(IconButtonDefaults.LargeButtonSize),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        painter = playPausePainter,
                        contentDescription = if (debouncedIsPlaying) "Pause" else "Play",
                        tint = iconTint,
                        modifier = Modifier.size(IconButtonDefaults.iconSizeFor(IconButtonDefaults.LargeButtonSize))
                    )
                }
                IconButton(
                    onClick = onSeekToNext,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        painter = nextPainter,
                        contentDescription = "Next",
                        tint = iconTint
                    )
                }
            }
        },
        buttons = { }
    )
}
