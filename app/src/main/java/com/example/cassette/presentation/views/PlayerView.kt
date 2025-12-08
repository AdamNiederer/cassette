package com.example.cassette.presentation.views

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.cassette.R
import com.example.cassette.data.types.PlayerState
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Album
import com.example.cassette.presentation.components.PlaybackControls
import com.example.cassette.presentation.components.TrackProgressBar
import com.example.cassette.presentation.components.VolumeBar
import com.example.cassette.presentation.components.VariableWrappedText
import com.example.cassette.presentation.components.FadedAlbumArt
import com.example.cassette.presentation.viewmodels.PlayerViewModel
import com.example.cassette.utils.LyricLine
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.rotaryinput.RotaryInputConfigDefaults
import com.google.android.horologist.compose.rotaryinput.accumulatedBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.core.graphics.ColorUtils
import android.content.Context
import android.util.TypedValue

@Composable
fun PlayerView(
    viewModel: PlayerViewModel,
    isAmbient: Boolean,
    ambientOffset: Pair<Int, Int> = 0 to 0
) {
    val context = LocalContext.current
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val album by remember(currentTrack) {
        if (currentTrack != null) {
            viewModel.getAlbum(currentTrack!!.artist, currentTrack!!.album)
        } else {
            flowOf(null)
        }
    }.collectAsState(initial = null)

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle(PlayerState(false, false, false, 0, 0))
    val progressState by remember {
        derivedStateOf {
            if (playbackState.duration > 0) {
                playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()
            } else 0f
        }
    }

    val currentLyric by viewModel.currentLyric.collectAsStateWithLifecycle()
    val volume by viewModel.volume.collectAsStateWithLifecycle()
    val maxVolume by viewModel.maxVolume.collectAsStateWithLifecycle()

    var highResBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentTrack) {
        highResBitmap = withContext(Dispatchers.IO) {
            currentTrack?.uri?.let { uriString ->
                runCatching {
                    context.contentResolver.loadThumbnail(Uri.parse(uriString), Size(384, 384), null)
                }.getOrNull()
            }
        }
    }

    val displayBitmap = (highResBitmap ?: album?.thumbnail)?.asImageBitmap()

    val paletteColor = remember(album?.palette) {
        album?.palette?.let { p ->
            val rgb = p.lightVibrant ?: p.dominant ?: p.muted ?: android.graphics.Color.WHITE
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(rgb, hsl)
            Color(ColorUtils.HSLToColor(hsl.apply { this[2] = this[2].coerceAtLeast(0.5f) }))
        } ?: Color.White
    }

    val darkMutedColor = remember(album?.palette) {
        album?.palette?.let { p ->
            val rgb = p.darkMuted ?: p.darkVibrant ?: android.graphics.Color.BLACK
            Color(rgb)
        } ?: Color.Black
    }

    var showingLyrics by remember { mutableStateOf(false) }

    // Ambient mode logic: Close lyrics and reset to main player view when wake up.
    LaunchedEffect(isAmbient) {
        if (isAmbient) {
            showingLyrics = false
        }
    }

    if (isAmbient) {
        PlayerAmbientView(
            currentTrack = currentTrack,
            displayBitmap = displayBitmap,
            paletteColor = paletteColor,
            offset = ambientOffset
        )
    } else {
        AnimatedContent(
            targetState = showingLyrics,
            transitionSpec = {
                if (targetState) {
                    // Slide up when opening lyrics
                    (slideInVertically(initialOffsetY = { it }) + fadeIn()) togetherWith
                            (slideOutVertically(targetOffsetY = { -it }) + fadeOut())
                } else {
                    // Slide down when closing lyrics
                    (slideInVertically(initialOffsetY = { -it }) + fadeIn()) togetherWith
                            (slideOutVertically(targetOffsetY = { it }) + fadeOut())
                }
            },
            label = "PlayerLyricsTransition"
        ) { isLyricsVisible ->
            if (isLyricsVisible) {
                BackHandler { 
                    showingLyrics = false 
                }
                LyricsView(
                    viewModel = viewModel,
                    backgroundColor = darkMutedColor,
                    onDismiss = { 
                        showingLyrics = false
                    }
                )
            } else {
                PlayerNonambientView(
                    currentTrack = currentTrack,
                    playbackState = playbackState,
                    progressState = progressState,
                    currentLyric = currentLyric,
                    volume = volume,
                    maxVolume = maxVolume,
                    displayBitmap = displayBitmap,
                    paletteColor = paletteColor,
                    onPlay = { viewModel.play() },
                    onPause = { viewModel.pause() },
                    onSeekToNext = { viewModel.seekToNext() },
                    onSeekToPrevious = { viewModel.seekToPrevious() },
                    onVolumeChange = { newVolume -> viewModel.setVolume(newVolume) },
                    onShowLyrics = { showingLyrics = true }
                )
            }
        }
    }
}

@Composable
fun PlayerNonambientView(
    currentTrack: Track?,
    playbackState: PlayerState,
    progressState: Float,
    currentLyric: String?,
    volume: Int,
    maxVolume: Int,
    displayBitmap: ImageBitmap?,
    paletteColor: Color,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeekToNext: () -> Unit,
    onSeekToPrevious: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onShowLyrics: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = accumulatedBehavior(
                    rateLimitCoolDownMs = 2 * RotaryInputConfigDefaults.DEFAULT_RATE_LIMIT_COOL_DOWN_MS,
                    onValueChange = { delta ->
                        val newVolume = (volume + if (delta > 0) 1 else -1).coerceIn(0, maxVolume)
                        if (newVolume != volume) {
                            onVolumeChange(newVolume)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                ),
                focusRequester = focusRequester
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20) { // Swipe up
                        onShowLyrics()
                    }
                }
            }
    ) {
        displayBitmap?.let { bitmap ->
            FadedAlbumArt(
                bitmap = bitmap,
                contentDescription = "Album art for ${currentTrack?.title}",
                alpha = 0.7f
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            TrackProgressBar(
                progress = progressState,
                color = paletteColor
            )

            VolumeBar(
                currentVolume = volume,
                maxVolume = maxVolume,
                color = paletteColor
            )

            PlaybackControls(
                isPlaying = playbackState.isPlaying,
                playWhenReady = playbackState.playWhenReady,
                songTitle = currentTrack?.title ?: "Unknown Track",
                artistName = currentTrack?.artist ?: "Unknown Artist",
                albumName = currentTrack?.album ?: "Unknown Album",
                onPlay = onPlay,
                onPause = onPause,
                onSeekToNext = onSeekToNext,
                onSeekToPrevious = onSeekToPrevious,
            )

            Box(
                modifier = Modifier
                    .width(116.dp)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            ) {
                AnimatedContent(
                    targetState = currentLyric,
                    transitionSpec = {
                        (slideInVertically { height -> height / 2 } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
                    },
                    label = "LyricAnimation",
                    modifier = Modifier.fillMaxWidth(),
                ) { lyric ->
                    val style = TextStyle(
                        fontSize = 1.5.em,
                        color = Color.White,
                        lineHeight = 1.0.em,
                        textAlign = TextAlign.Center,
                    )
                    if (lyric != null) {
                        VariableWrappedText(
                            text = lyric,
                            style = style,
                            lineWidths = listOf(116.dp, 96.dp, 56.dp),
                        )
                    } else {
                        Spacer(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount > 50) { 
                        onDismiss()
                    }
                }
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

@Composable
fun PlayerAmbientView(
    currentTrack: Track?,
    displayBitmap: ImageBitmap?,
    paletteColor: Color,
    offset: Pair<Int, Int> = 0 to 0
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000 * (60 - LocalTime.now().second.toLong()))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = 1.0f
                translationX = offset.first.toFloat()
                translationY = offset.second.toFloat()
            }
    ) {
        if (displayBitmap != null) {
            FadedAlbumArt(
                bitmap = displayBitmap,
                contentDescription = null,
                alpha = 0.5f
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = currentTrack?.title ?: "Unknown Track",
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
                fontSize = 2.0.em,
                fontWeight = FontWeight.Bold,
                color = paletteColor
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = currentTime.format(formatter),
                textAlign = TextAlign.Center,
                fontSize = 8.0.em,
                fontWeight = FontWeight.W100,
                color = paletteColor,
            )
        }
    }
}
