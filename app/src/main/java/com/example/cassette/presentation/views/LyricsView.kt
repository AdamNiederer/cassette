package com.example.cassette.presentation.views

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.cassette.R
import com.example.cassette.data.types.PlayerState
import com.example.cassette.presentation.viewmodels.PlayerViewModel
import com.example.cassette.utils.LyricLine
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import kotlinx.coroutines.launch

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LyricsView(
    viewModel: PlayerViewModel,
    backgroundColor: Color,
    onDismiss: () -> Unit
) {
    val lyricsList by viewModel.lyricsList.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle(PlayerState(false, false, false, 0, 0))
    val currentPosition = playbackState.currentPosition
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    
    val columnState = rememberResponsiveColumnState(
        contentPadding = { PaddingValues(top = 20.dp, bottom = 40.dp, start = 16.dp, end = 16.dp) }
    )

    LaunchedEffect(Unit) {
        val initialOffset = with(density) { 64.dp.roundToPx() }
        columnState.state.scrollToItem(1, initialOffset)
    }

    val displayLyrics = remember(lyricsList, currentTrack) {
        val rawLyrics = lyricsList ?: currentTrack?.lyrics?.lines()?.map { LyricLine(0, it) } ?: emptyList()
        
        if (rawLyrics.isEmpty()) return@remember emptyList<LyricLine>()

        val result = mutableListOf<LyricLine>()
        var previousWasBlank = false

        for (line in rawLyrics) {
            val isBlank = line.text.trim().isEmpty()
            if (isBlank) {
                if (!previousWasBlank) {
                    result.add(line)
                    previousWasBlank = true
                }
            } else {
                result.add(line)
                previousWasBlank = false
            }
        }

        // If the last line is blank, remove it
        if (result.isNotEmpty() && result.last().text.trim().isEmpty()) {
            result.removeAt(result.lastIndex)
        }
        
        result
    }
    val isSynced = lyricsList != null

    val dismissThresholdPx = with(density) { 48.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(dismissThresholdPx) {
                var cumulativeX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { cumulativeX = 0f },
                    onHorizontalDrag = { _, amount -> cumulativeX += amount },
                    onDragEnd = { if (cumulativeX > dismissThresholdPx) onDismiss() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .graphicsLayer(alpha = 0.6f)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.radialGradient(
                            0.0f to backgroundColor,
                            0.2f to backgroundColor,
                            1.0f to Color.Transparent,
                            center = center,
                            radius = size.minDimension / 2,
                        ),
                        blendMode = BlendMode.DstAtop
                    )
                }
                .clip(CircleShape)
        )

        ScalingLazyColumn(
            columnState = columnState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { onDismiss() },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                        modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Dismiss",
                            tint = Color.White
                        )
                    }
                }
            }

            item {
                Text(
                    text = currentTrack?.title ?: "",
                    style = MaterialTheme.typography.caption1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = Color.White
                )
            }

            items(displayLyrics.size) { index ->
                val line = displayLyrics[index]
                val isCurrent = if (isSynced) {
                    val nextTimestamp = displayLyrics.getOrNull(index + 1)?.timestampMs ?: Long.MAX_VALUE
                    currentPosition in line.timestampMs until nextTimestamp
                } else false

                val targetColor = when {
                    !isSynced -> Color.White
                    isCurrent -> Color.White
                    line.timestampMs < currentPosition -> Color.Gray
                    else -> lerp(Color.White, Color.Gray, 0.5f)
                }
                
                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 300),
                    label = "LyricsColorAnimation"
                )

                if(line.text == "") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.Gray)
                        )
                        Icon(
                            painter = painterResource(id = R.drawable.ic_music),
                            contentDescription = "Instrumental section",
                            tint = animatedColor,
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(12.dp),
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .background(Color.Gray)
                        )
                    }
                } else {
                    Text(
                        text = line.text,
                        style = TextStyle(
                            color = animatedColor,
                            fontWeight = FontWeight.Normal,
                            fontSize = 1.75.em,
                            textAlign = TextAlign.Left,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 1.dp)
                    )
                }
            }

            item {
                Button(
                    onClick = { 
                        scope.launch { 
                            columnState.state.animateScrollToItem(0) 
                        } 
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_line_up),
                        contentDescription = "Return to top",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
