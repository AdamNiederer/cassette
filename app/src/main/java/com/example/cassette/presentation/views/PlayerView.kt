package com.example.cassette.presentation.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.text.format.DateFormat
import android.util.Size
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import androidx.wear.compose.material3.Text
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
import androidx.core.content.res.ResourcesCompat
import android.content.Context
import androidx.core.net.toUri
import kotlin.comparisons.minOf

enum class PlayerSubview { MAIN, LYRICS, QUEUE }

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
                    context.contentResolver.loadThumbnail(uriString.toUri(), Size(384, 384), null)
                }.getOrNull()
            }
        }
    }

    val displayBitmap = (highResBitmap ?: album?.thumbnail)?.asImageBitmap()

    val paletteColor = remember(album?.palette) {
        album?.palette?.let { p ->
            val rgb = p.lightVibrant ?: p.vibrant ?: p.muted ?: android.graphics.Color.WHITE
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

    var currentSubview by remember { mutableStateOf(PlayerSubview.MAIN) }

    // Ambient mode logic: Close lyrics/queue and reset to main player view when wake up.
    LaunchedEffect(isAmbient) {
        if (isAmbient) {
            currentSubview = PlayerSubview.MAIN
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
            targetState = currentSubview,
            transitionSpec = {
                when {
                    targetState == PlayerSubview.QUEUE && initialState == PlayerSubview.MAIN -> {
                        (slideInHorizontally { it } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it } + fadeOut())
                    }
                    targetState == PlayerSubview.MAIN && initialState == PlayerSubview.QUEUE -> {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                    }
                    targetState == PlayerSubview.LYRICS && initialState == PlayerSubview.MAIN -> {
                        (slideInVertically { it } + fadeIn()) togetherWith
                                (slideOutVertically { -it } + fadeOut())
                    }
                    targetState == PlayerSubview.MAIN && initialState == PlayerSubview.LYRICS -> {
                        (slideInVertically { -it } + fadeIn()) togetherWith
                                (slideOutVertically { it } + fadeOut())
                    }
                    else -> {
                        fadeIn() togetherWith fadeOut()
                    }
                }
            },
            label = "PlayerViewTransition"
        ) { subview ->
            when (subview) {
                PlayerSubview.LYRICS -> {
                    BackHandler {
                        currentSubview = PlayerSubview.MAIN
                    }
                    LyricsView(
                        viewModel = viewModel,
                        backgroundColor = darkMutedColor,
                        onDismiss = {
                            currentSubview = PlayerSubview.MAIN
                        }
                    )
                }
                PlayerSubview.QUEUE -> {
                    BackHandler {
                        currentSubview = PlayerSubview.MAIN
                    }
                    QueueView(
                        viewModel = viewModel,
                        backgroundColor = darkMutedColor,
                        onDismiss = {
                            currentSubview = PlayerSubview.MAIN
                        }
                    )
                }
                PlayerSubview.MAIN -> {
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
                        onShowLyrics = { currentSubview = PlayerSubview.LYRICS },
                        onShowQueue = { currentSubview = PlayerSubview.QUEUE }
                    )
                }
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
    onShowLyrics: () -> Unit,
    onShowQueue: () -> Unit
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
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -20) { // Swipe right-to-left
                        onShowQueue()
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

@Composable
fun PlayerAmbientView(
    currentTrack: Track?,
    displayBitmap: ImageBitmap?,
    paletteColor: Color,
    offset: Pair<Int, Int> = 0 to 0
) {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(1000 * (60 - LocalTime.now().second.toLong()))
        }
    }

    val use24Hour = DateFormat.is24HourFormat(context)
    val hourFormatter = remember(use24Hour) {
        DateTimeFormatter.ofPattern(if (use24Hour) "HH" else "hh")
    }
    val minuteFormatter = remember { DateTimeFormatter.ofPattern("mm") }
    val maskedTime = remember(currentTime.hour, currentTime.minute, use24Hour, displayBitmap, paletteColor) {
        renderMaskedTimeText(
            context,
            listOf(
                currentTime.format(hourFormatter),
                currentTime.format(minuteFormatter)
            ),
            displayBitmap?.asAndroidBitmap(),
            paletteColor
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .graphicsLayer {
                alpha = 1.0f
                translationX = offset.first.toFloat()
                translationY = offset.second.toFloat()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                bitmap = maskedTime.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            )
            Text(
                text = currentTrack?.title ?: "",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 2.0.em,
                fontWeight = FontWeight.Normal,
                color = paletteColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

private fun renderMaskedTimeText(
    context: Context,
    lines: List<String>,
    art: Bitmap?,
    paletteColor: Color
): Bitmap {
    val width = context.resources.displayMetrics.widthPixels
    val height = context.resources.displayMetrics.heightPixels

    val stroke = 2f
    val availWidth = width - 2f * stroke
    val availHeight = height - 2f * stroke

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 100f
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.chivo_mono)
        color = android.graphics.Color.WHITE
        setFontVariationSettings("'wght' 100")
    }

    val width100 = lines.maxOf { fillPaint.measureText(it) }
    val height100 = (fillPaint.fontMetrics.ascent * -0.85f) * lines.size

    val scale = minOf(availWidth / width100, availHeight / height100).coerceAtLeast(1f)
    fillPaint.textSize = 100f * scale

    val lineHeight = fillPaint.fontMetrics.ascent * -0.85f
    val firstBaseline = lineHeight - stroke

    val strokePaint = Paint(fillPaint).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = paletteColor.toArgb()
    }

    val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(mask).apply {
        drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        lines.forEachIndexed { i, line ->
            drawText(line, width / 2f, firstBaseline + i * lineHeight, fillPaint)
        }
    }

    val artLayer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    if (art != null) {
        val artCanvas = Canvas(artLayer)
        artCanvas.drawBitmap(mask, 0f, 0f, null)
        val artPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        }
        val scaleArt = maxOf(width / art.width.toFloat(), height / art.height.toFloat())
        val w = art.width * scaleArt
        val h = art.height * scaleArt
        val left = (width - w) / 2f
        val top = (height - h) / 2f
        artCanvas.drawBitmap(art, Rect(0, 0, art.width, art.height), RectF(left, top, left + w, top + h), artPaint)
    }

    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    Canvas(result).apply {
        drawColor(android.graphics.Color.BLACK)
        drawBitmap(artLayer, 0f, 0f, null)
        lines.forEachIndexed { i, line ->
            drawText(line, width / 2f, firstBaseline + i * lineHeight, strokePaint)
        }
    }

    return result
}
