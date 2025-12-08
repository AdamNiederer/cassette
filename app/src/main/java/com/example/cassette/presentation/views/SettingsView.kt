package com.example.cassette.presentation.views

import android.os.Parcelable
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.foundation.requestFocusOnHierarchyActive
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.SwipeToDismissKeys
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.example.cassette.R
import com.example.cassette.presentation.viewmodels.SettingsViewModel
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.rotaryinput.RotaryInputConfigDefaults
import com.google.android.horologist.compose.rotaryinput.accumulatedBehavior
import kotlinx.parcelize.Parcelize
import android.util.Log

sealed class SettingsScreen : Parcelable {
    @Parcelize
    data object Main : SettingsScreen()
    @Parcelize
    data object LyricsOffset : SettingsScreen()
    @Parcelize
    data object LyricsBlankLine : SettingsScreen()
    @Parcelize
    data object ReplayGain : SettingsScreen()
    @Parcelize
    data object PreampGain : SettingsScreen()
}

fun formatLrcOffset(offset: Long): String {
    return if(offset == 0L) {
        "None"
    } else {
        "${Math.abs(offset)}ms " + if(offset < 0) { "Early" } else { "Late" }
    }
}

fun formatReplayGain(preamp: Float, mode: String): String {
    return if (mode == "None" || preamp == 0f) {
        mode
    } else {
        "$mode; ${formatReplayGainPreamp(preamp)}"
    }
}

fun formatReplayGainPreamp(preamp: Float): String {
    val sign = when {
        preamp > 0 -> "+"
        else -> "" // negative handled automatically
    }
    return "${sign}${preamp.toInt()} dB"

}

@Composable
fun SettingsView(viewModel: SettingsViewModel) {
    val navigationStack = remember { mutableStateListOf<SettingsScreen>(SettingsScreen.Main) }
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val saveableStateHolder = rememberSaveableStateHolder()

    BackHandler(enabled = navigationStack.size > 1) {
        navigationStack.removeLast()
    }

    val currentScreen = navigationStack.last()
    val previousScreen = navigationStack.getOrNull(navigationStack.size - 2)

    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        hasBackground = previousScreen != null,
        backgroundKey = previousScreen ?: SwipeToDismissKeys.Background,
        contentKey = currentScreen,
        onDismissed = {
            if (navigationStack.size > 1) {
                navigationStack.removeLast()
            }
        }
    ) { isBackground ->
        val screen = if (isBackground) previousScreen ?: SettingsScreen.Main else currentScreen

        saveableStateHolder.SaveableStateProvider(key = screen) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (screen) {
                    SettingsScreen.Main -> SettingsList(
                        viewModel = viewModel,
                        onNavigate = { navigationStack.add(it) }
                    )
                    SettingsScreen.LyricsOffset -> LyricsOffsetSetting(viewModel = viewModel)
                    SettingsScreen.LyricsBlankLine -> LyricsBlankLineSetting(viewModel = viewModel)
                    SettingsScreen.ReplayGain -> ReplayGainSetting(viewModel = viewModel, onNavigate = { navigationStack.add(it) })
                    SettingsScreen.PreampGain -> PreampGainSetting(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
private fun SettingsList(
    viewModel: SettingsViewModel,
    onNavigate: (SettingsScreen) -> Unit
) {
    val columnState = rememberResponsiveColumnState()
    val lyricsOffset by viewModel.lyricsOffset.collectAsState()
    val replayGainMode by viewModel.replayGainMode.collectAsState()
    val replayGainPreamp by viewModel.replayGainPreamp.collectAsState()
    val lyricsBlankLineInterval by viewModel.lyricsBlankLineInterval.collectAsState()

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            columnState = columnState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = "Settings",
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Chip(
                    onClick = { onNavigate(SettingsScreen.LyricsOffset) },
                    label = { Text("LRC Offset") },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_synced_lyrics),
                            contentDescription = "Lyrics Offset",
                        )
                    },
                    secondaryLabel = { Text(formatLrcOffset(lyricsOffset)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            item {
                Chip(
                    onClick = { onNavigate(SettingsScreen.LyricsBlankLine) },
                    label = { Text("LRC Duration") },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_music),
                            contentDescription = "LRC Duration",
                        )
                    },
                    secondaryLabel = { Text("${lyricsBlankLineInterval / 1000}s") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            item {
                Chip(
                    onClick = { onNavigate(SettingsScreen.ReplayGain) },
                    label = { Text("Replay Gain") },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_volume_up),
                            contentDescription = "Replay Gain",
                        )
                    },
                    secondaryLabel = { Text(formatReplayGain(replayGainPreamp, replayGainMode)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}

@Composable
private fun LyricsOffsetSetting(viewModel: SettingsViewModel) {
    val lyricsOffset by viewModel.lyricsOffset.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .focusRequester(focusRequester)
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = accumulatedBehavior(
                    rateLimitCoolDownMs = 2 * RotaryInputConfigDefaults.DEFAULT_RATE_LIMIT_COOL_DOWN_MS,
                    onValueChange = { delta ->
                        val newOffset = if (delta > 0) {
                            (lyricsOffset + 100).coerceAtMost(1000)
                        } else {
                            (lyricsOffset - 100).coerceAtLeast(-1000)
                        }
                        if (newOffset != lyricsOffset) {
                            viewModel.setLyricsOffset(newOffset)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                ),
                focusRequester = focusRequester
            )
    ) {
        Stepper(
            value = lyricsOffset.toFloat(),
            onValueChange = { viewModel.setLyricsOffset(it.toLong()) },
            valueRange = -1000f..1000f,
            steps = 19,
            decreaseIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "Decrease"
                )
            },
            increaseIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Increase"
                )
            }
        ) {
            AnimatedContent(
                targetState = lyricsOffset,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { height -> -height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> height } + fadeOut())
                    } else {
                        (slideInVertically { height -> height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    }
                },
                label = "LyricsOffsetAnimation",
                // modifier = Modifier.align(Alignment.Center)
            ) { offset ->
                Text(
                    text = formatLrcOffset(offset),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 3.0.em,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        
        PositionIndicator(
            value = { lyricsOffset.toFloat() },
            range = -1000f..1000f,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun LyricsBlankLineSetting(viewModel: SettingsViewModel) {
    val interval by viewModel.lyricsBlankLineInterval.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .focusRequester(focusRequester)
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = accumulatedBehavior(
                    rateLimitCoolDownMs = 2 * RotaryInputConfigDefaults.DEFAULT_RATE_LIMIT_COOL_DOWN_MS,
                    onValueChange = { delta ->
                        val newInterval = if (delta > 0) {
                            (interval + 1000).coerceAtMost(20000)
                        } else {
                            (interval - 1000).coerceAtLeast(8000)
                        }
                        if (newInterval != interval) {
                            viewModel.setLyricsBlankLineInterval(newInterval)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                ),
                focusRequester = focusRequester
            )
    ) {
        Stepper(
            value = (interval / 1000).toFloat(),
            onValueChange = { viewModel.setLyricsBlankLineInterval(it.toLong() * 1000) },
            valueRange = 8f..20f,
            steps = 11,
            decreaseIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "Decrease"
                )
            },
            increaseIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Increase"
                )
            }
        ) {
            AnimatedContent(
                targetState = interval,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { height -> -height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> height } + fadeOut())
                    } else {
                        (slideInVertically { height -> height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    }
                },
                label = "LyricsBlankLineAnimation",
            ) { value ->
                Text(
                    text = "${value / 1000}s",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 3.0.em,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        PositionIndicator(
            value = { (interval / 1000).toFloat() },
            range = 8f..20f,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
private fun ReplayGainSetting(
    viewModel: SettingsViewModel,
    onNavigate: (SettingsScreen) -> Unit
) {
    val columnState = rememberResponsiveColumnState()
    val replayGainMode by viewModel.replayGainMode.collectAsState()
    val replayGainPreamp by viewModel.replayGainPreamp.collectAsState()
    val modes = listOf("None", "Track", "Album")

    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            columnState = columnState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = "Replay Gain",
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Chip(
                    onClick = { onNavigate(SettingsScreen.PreampGain) },
                    label = { Text("Preamp Gain") },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_volume_up),
                            contentDescription = "Preamp Gain",
                        )
                    },
                    secondaryLabel = { Text(formatReplayGainPreamp(replayGainPreamp)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }

            item {
                Text(
                    text = "Mode",
                    modifier = Modifier.padding(vertical = 8.dp),
                    style = MaterialTheme.typography.title3,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(modes.size) { index ->
                val mode = modes[index]
                ToggleChip(
                    checked = replayGainMode == mode,
                    onCheckedChange = { if (it) viewModel.setReplayGainMode(mode) },
                    label = { Text(mode) },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.radioIcon(checked = replayGainMode == mode),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ToggleChipDefaults.toggleChipColors()
                )
            }
        }
    }
}

@Composable
private fun PreampGainSetting(viewModel: SettingsViewModel) {
    val preampGain by viewModel.replayGainPreamp.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val view = LocalView.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusable()
            .focusRequester(focusRequester)
            .requestFocusOnHierarchyActive()
            .rotaryScrollable(
                behavior = accumulatedBehavior(
                    rateLimitCoolDownMs = 2 * RotaryInputConfigDefaults.DEFAULT_RATE_LIMIT_COOL_DOWN_MS,
                    onValueChange = { delta ->
                        val newGain = if (delta > 0) {
                            (preampGain + 1.0f).coerceAtMost(10.0f)
                        } else {
                            (preampGain - 1.0f).coerceAtLeast(-10.0f)
                        }
                        if (newGain != preampGain) {
                            viewModel.setReplayGainPreamp(newGain)
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                    }
                ),
                focusRequester = focusRequester
            )
    ) {
        Stepper(
            value = preampGain,
            onValueChange = { viewModel.setReplayGainPreamp(it) },
            valueRange = -10f..10f,
            steps = 19,
            decreaseIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = "Decrease"
                )
            },
            increaseIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "Increase"
                )
            }
        ) {
            AnimatedContent(
                targetState = preampGain,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInVertically { height -> -height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> height } + fadeOut())
                    } else {
                        (slideInVertically { height -> height } + fadeIn())
                            .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                    }
                },
                label = "PreampGainAnimation",
            ) { gain ->
                val sign = if (gain > 0) "+" else ""
                Text(
                    text = "${sign}${gain.toInt()} dB",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 3.0.em,
                    fontWeight = FontWeight.Black,
                )
            }
        }

        PositionIndicator(
            value = { preampGain },
            range = -10f..10f,
            modifier = Modifier.padding(16.dp),
        )
    }
}
