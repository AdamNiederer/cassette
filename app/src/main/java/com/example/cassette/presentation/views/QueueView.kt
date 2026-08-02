package com.example.cassette.presentation.views

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.cassette.R
import com.example.cassette.data.types.AlbumPalette
import com.example.cassette.data.types.TrackQueueItem
import com.example.cassette.presentation.viewmodels.PlayerViewModel
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

@Composable
fun QueueItemRow(
    queueItem: TrackQueueItem,
    index: Int,
    animatedColor: Color,
    chipBackgroundColor: Color,
    dimmed: Boolean,
    onRemove: () -> Unit,
    onSkipTo: () -> Unit,
    onGripStart: () -> Unit,
    onGripUp: () -> Unit,
    onGripDown: () -> Unit,
    onGripEnd: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val maxDragPx = with(density) { -48.dp.toPx() }
    val dismissThresholdPx = maxDragPx * 0.75
    val flickThresholdPx = with(density) { 30.dp.toPx() }

    val rowColor by animateColorAsState(
        targetValue = if (dimmed) animatedColor.copy(alpha = 0.8f) else animatedColor,
        animationSpec = tween(durationMillis = 200),
        label = "RowColorDim"
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (dimmed) lerp(chipBackgroundColor, Color.Black, 0.2f) else chipBackgroundColor,
        animationSpec = tween(durationMillis = 200),
        label = "RowBackgroundDim"
    )

    val offsetX = remember { Animatable(0f) }

    var hasHapticed by remember { mutableStateOf(false) }

    val removeIconTint by animateColorAsState(
        targetValue = if (offsetX.value <= dismissThresholdPx) Color.White else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "RemoveIconTint"
    )

    LaunchedEffect(offsetX.value) {
        if (offsetX.value <= dismissThresholdPx && !hasHapticed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            hasHapticed = true
        } else if (offsetX.value > dismissThresholdPx) {
            hasHapticed = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(chipBackgroundColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .padding(end = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_playlist_remove),
                contentDescription = "Remove",
                tint = removeIconTint
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = offsetX.value.coerceAtMost(0f).roundToInt(), y = 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                val newOffset = (offsetX.value + dragAmount).coerceIn(maxDragPx, 0f)
                                offsetX.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value <= dismissThresholdPx) {
                                    onRemove()
                                } else {
                                    offsetX.animateTo(0f)
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f) }
                        }
                    )
                }
                .background(backgroundColor, RoundedCornerShape(24.dp))
                .clickable { onSkipTo() }
                .padding(start = 10.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gripper),
                contentDescription = "Reorder controls",
                modifier = Modifier
                    .size(ChipDefaults.IconSize)
                    .pointerInput(index) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            val pointerId = down.id

                            var totalY = 0f
                            var cancelled = false

                            val heldForLongPress = withTimeoutOrNull(300L) {
                                var stillDown = true
                                while (stillDown) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId } ?: return@withTimeoutOrNull false
                                    if (change.isConsumed || change.changedToUp()) {
                                        return@withTimeoutOrNull false
                                    }
                                    change.consume()
                                }
                                true
                            }

                            if (heldForLongPress == null) {
                                onGripStart()
                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                        if (change.isConsumed) {
                                            cancelled = true
                                            break
                                        }
                                        if (change.changedToUp()) break
                                        totalY += change.positionChange().y
                                        change.consume()
                                    }
                                } catch (_: CancellationException) {
                                    cancelled = true
                                } finally {
                                    if (!cancelled && totalY <= -flickThresholdPx) {
                                        onGripUp()
                                    } else if (!cancelled && totalY >= flickThresholdPx) {
                                        onGripDown()
                                    }
                                    onGripEnd()
                                }
                            }
                        }
                    },
                tint = rowColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = queueItem.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = rowColor,
                        fontWeight = FontWeight.Normal,
                        fontSize = 2.em,
                        lineHeight = 1.25.em,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun QueueView(
    viewModel: PlayerViewModel,
    backgroundColor: Color,
    onDismiss: () -> Unit
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val currentQueueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var hasScrolledToCurrent by remember { mutableStateOf(false) }

    var grippedId by remember { mutableStateOf<Long?>(null) }

    val columnState = rememberResponsiveColumnState(
        contentPadding = { PaddingValues(top = 20.dp, bottom = 40.dp, start = 16.dp, end = 16.dp) }
    )

    LaunchedEffect(queue, currentQueueIndex) {
        if (!hasScrolledToCurrent && currentQueueIndex >= 0 && currentQueueIndex < queue.size) {
            val scrollTarget = currentQueueIndex + 2
            val initialOffset = with(density) { 64.dp.roundToPx() }
            columnState.state.scrollToItem(scrollTarget, initialOffset)
            hasScrolledToCurrent = true
        }
    }

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
                    text = "Play Queue",
                    style = MaterialTheme.typography.caption1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = Color.White
                )
            }

            items(queue.size, key = { queue[it].id }) { index ->
                val queueItem = queue[index]
                val isCurrent = index == currentQueueIndex

                val targetColor = when {
                    isCurrent -> Color.White
                    index < currentQueueIndex -> Color.Gray
                    else -> lerp(Color.White, Color.Gray, 0.5f)
                }

                val animatedColor by animateColorAsState(
                    targetValue = targetColor,
                    animationSpec = tween(durationMillis = 300),
                    label = "QueueColorAnimation"
                )

                var albumPalette by remember(queueItem.artist, queueItem.album) { mutableStateOf<AlbumPalette?>(null) }

                LaunchedEffect(queueItem.artist, queueItem.album) {
                    viewModel.getAlbum(queueItem.artist, queueItem.album)
                        .first()
                        ?.let { albumPalette = it.palette }
                }

                val chipBackgroundColor = remember(albumPalette) {
                    albumPalette?.darkMuted?.let { Color(it) } ?: Color(0xFF333333)
                }

                QueueItemRow(
                    queueItem = queueItem,
                    index = index,
                    animatedColor = animatedColor,
                    chipBackgroundColor = chipBackgroundColor,
                    dimmed = grippedId != null && grippedId != queueItem.id,
                    onRemove = {
                        val currentIndex = queue.indexOfFirst { it.id == queueItem.id }
                        if (currentIndex >= 0) {
                            viewModel.removeFromQueue(currentIndex)
                        }
                    },
                    onSkipTo = {
                        val currentIndex = queue.indexOfFirst { it.id == queueItem.id }
                        if (currentIndex >= 0) {
                            viewModel.skipToQueueItem(currentIndex)
                        }
                    },
                    onGripStart = {
                        grippedId = queueItem.id
                    },
                    onGripUp = {
                        grippedId = null
                        if (index > 0) {
                            viewModel.moveQueueItem(index, index - 1)
                        }
                    },
                    onGripDown = {
                        grippedId = null
                        if (index < queue.size - 1) {
                            viewModel.moveQueueItem(index, index + 1)
                        }
                    },
                    onGripEnd = {
                        grippedId = null
                    }
                )
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