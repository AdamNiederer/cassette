package com.example.cassette.presentation.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.compose.runtime.LaunchedEffect
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material.Icon
import androidx.paging.LoadState
import androidx.paging.compose.itemKey
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.wear.compose.material.SwipeToDismissKeys
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.coroutines.flow.flowOf
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.example.cassette.presentation.SettingsActivity
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height

import com.example.cassette.R
import com.example.cassette.presentation.components.PlaceholderTrackItem
import com.example.cassette.presentation.components.PlaceholderAlbumItem
import com.example.cassette.presentation.components.PlaceholderArtistItem
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Album
import com.example.cassette.data.sources.DiscoveryState
import com.example.cassette.presentation.viewmodels.TrackListState
import com.example.cassette.presentation.viewmodels.TrackListViewModel
import com.example.cassette.presentation.components.TrackItem
import com.example.cassette.presentation.components.AlbumItem
import com.example.cassette.presentation.components.ArtistItem
import com.example.cassette.data.repositories.PlayerRepository.QueueSource
import com.example.cassette.presentation.components.PartialCircularProgressIndicator
import com.example.cassette.data.types.DiscoveryPreview
import androidx.core.graphics.ColorUtils
import androidx.compose.animation.*
import androidx.compose.animation.core.tween

import com.example.cassette.presentation.components.FadedAlbumArt

sealed class LibraryScreen : Parcelable {
    @Parcelize
    data object Home : LibraryScreen()
    @Parcelize
    data object Artists : LibraryScreen()
    @Parcelize
    data class ArtistAlbums(val artist: String) : LibraryScreen()
    @Parcelize
    data object AllAlbums : LibraryScreen()
    @Parcelize
    data class AlbumTracks(val albumId: String, val albumName: String) : LibraryScreen()
    @Parcelize
    data object AllTracks : LibraryScreen()
}

@Composable
fun LibraryView(
    viewModel: TrackListViewModel,
    onTrackSelected: (Track, QueueSource) -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val navigationStack = remember { mutableStateListOf<LibraryScreen>(LibraryScreen.Home) }
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
        val screen = if (isBackground) previousScreen ?: LibraryScreen.Home else currentScreen

        saveableStateHolder.SaveableStateProvider(key = screen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val firstDown = awaitFirstDown(requireUnconsumed = false)
                            var totalDrag = 0f
                            var swiped = false
                            
                            horizontalDrag(firstDown.id) { change ->
                                val dragAmount = change.positionChange().x
                                totalDrag += dragAmount
                                
                                // Right to left swipe to show player
                                if (totalDrag < -50f && !swiped) {
                                    swiped = true
                                    if(state is TrackListState.Idle) { 
                                        change.consume()
                                        onNavigateToPlayer()
                                    }
                                } else if (totalDrag > 0f) {
                                    // Don't consume left-to-right swipe so it can propagate to SwipeToDismissBox
                                } else if (swiped) {
                                    change.consume()
                                }
                            }
                        }
                    }
            ) {
                when (state) {
                    is TrackListState.Loading -> {
                        val preview by viewModel.currentPreview.collectAsState()
                        LoadingView(progress, preview)
                    }
                    is TrackListState.Idle -> {
                        when (screen) {
                            is LibraryScreen.Home -> LibraryHome(viewModel = viewModel, onNavigate = { navigationStack.add(it) })
                            is LibraryScreen.Artists ->
                                ArtistList(
                                    viewModel = viewModel,
                                    onArtistSelected = { navigationStack.add(LibraryScreen.ArtistAlbums(it)) }
                                )
                        is LibraryScreen.ArtistAlbums -> AlbumList(
                            viewModel = viewModel,
                            artist = screen.artist,
                            onAlbumSelected = { navigationStack.add(LibraryScreen.AlbumTracks(it.id, it.name)) }
                        )
                        LibraryScreen.AllAlbums -> AlbumList(
                            viewModel = viewModel,
                            onAlbumSelected = { navigationStack.add(LibraryScreen.AlbumTracks(it.id, it.name)) }
                        )
                        is LibraryScreen.AlbumTracks -> TrackList(
                            viewModel = viewModel,
                            albumId = screen.albumId,
                            albumName = screen.albumName,
                            onTrackSelected = onTrackSelected
                        )
                            is LibraryScreen.AllTracks ->
                                TrackList(
                                    viewModel = viewModel,
                                    onTrackSelected = { track, _ -> onTrackSelected(track, QueueSource.ALL_TRACKS) }
                                )
                        }
                    }
                    is TrackListState.Error -> {
                        ErrorView((state as TrackListState.Error).message) { viewModel.refreshTracks() }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun TrackList(
    viewModel: TrackListViewModel,
    albumId: String? = null,
    albumName: String? = null,
    onTrackSelected: (Track, QueueSource) -> Unit
) {
    val pagedTracks = remember(albumId) {
        if (albumId != null) {
            viewModel.getTracksByAlbumPaged(albumId)
        } else {
            viewModel.trackPager
        }
    }.collectAsLazyPagingItems()

    val album by remember(albumId) {
        if (albumId != null) {
            viewModel.getAlbumById(albumId)
        } else {
            flowOf(null)
        }
    }.collectAsState(initial = null)

    val columnState = rememberResponsiveColumnState()

    Box(modifier = Modifier.fillMaxSize()) {
        album?.thumbnail?.let { bitmap ->
            FadedAlbumArt(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Album art for ${albumName ?: album!!.name}",
                blurRadius = 4.dp
            )
        }
        ScreenScaffold(scrollState = columnState) {
            ScalingLazyColumn(
                columnState = columnState,
                modifier = Modifier.fillMaxSize(),
            ) {
                item { 
                    Text(
                        text = albumName ?: album?.name ?: "All Tracks",
                        modifier = Modifier
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                            .align(Alignment.Center),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    ) 
                }
                
                if (pagedTracks.loadState.refresh is LoadState.Loading) {
                    items(5) { PlaceholderTrackItem(showIcon = album == null && albumName == null) }
                } else if (pagedTracks.loadState.refresh is LoadState.NotLoading && pagedTracks.itemCount == 0) {
                    item { Text("No tracks found.") }
                } else {
                    items(
                        count = pagedTracks.itemCount,
                        key = pagedTracks.itemKey { it.id }
                    ) { index ->
                        val track = pagedTracks[index]
                        if (track != null) {
                            if (albumId != null) {
                                TrackItem(
                                    track = track,
                                    album = album,
                                    showNumber = true,
                                    onClick = { onTrackSelected(track, QueueSource.ALBUM) }
                                )
                            } else {
                                val trackAlbum by remember(track.id) { 
                                    viewModel.getAlbumById(track.albumId)
                                }.collectAsState(initial = null)
                                
                                TrackItem(
                                    track = track,
                                    album = trackAlbum,
                                    onClick = { onTrackSelected(track, QueueSource.ALL_TRACKS) }
                                )
                            }
                        }
                         else {
                            PlaceholderTrackItem(showIcon = album == null && albumName == null)
                        }
                    }
                    
                    if (pagedTracks.loadState.append is LoadState.Loading) {
                        item { PlaceholderTrackItem(showIcon = album == null && albumName == null) }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingView(progress: DiscoveryState, preview: DiscoveryPreview?) {
    val targetColor = remember(preview?.palette) {
        preview?.palette?.let { p ->
            val rgb = p.lightVibrant ?: p.dominant ?: p.muted ?: android.graphics.Color.WHITE
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(rgb, hsl)
            Color(ColorUtils.HSLToColor(hsl.apply { this[2] = this[2].coerceAtLeast(0.5f) }))
        } ?: Color.White
    }
    
    val paletteColor by animateColorAsState(targetColor, animationSpec = tween(1000), label = "ColorAnimation")

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = preview, animationSpec = tween(1000), label = "AlbumArtFade") { currentPreview ->
            if (currentPreview != null) {
                FadedAlbumArt(
                    bitmap = currentPreview.bitmap.asImageBitmap(),
                    contentDescription = "Album art for ${currentPreview.name}",
                    alpha = 0.7f
                )
            }
        }

        val scanningProgress = when (progress) {
            is DiscoveryState.Scanning -> progress.current.toFloat() / progress.total.coerceAtLeast(1)
            is DiscoveryState.Rebuilding -> 1f
            else -> 0f
        }

        PartialCircularProgressIndicator(
            progress = scanningProgress,
            modifier = Modifier.fillMaxSize(),
            startAngle = 210f,
            trackSweep = 120f,
            color = paletteColor,
            strokeWidth = 8.dp
        )

        val rebuildingProgress = when (progress) {
            is DiscoveryState.Rebuilding -> progress.current.toFloat() / progress.total.coerceAtLeast(1)
            else -> 0f
        }

        PartialCircularProgressIndicator(
            progress = rebuildingProgress,
            modifier = Modifier.fillMaxSize(),
            startAngle = 30f,
            trackSweep = 120f,
            color = paletteColor,
            strokeWidth = 8.dp
        )


        AnimatedContent(
            targetState = preview,
            transitionSpec = {
                (slideInVertically { height -> height / 2 } + fadeIn())
                    .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
            },
            label = "LoadingAnimation",
            modifier = Modifier.fillMaxWidth(),
        ) { currentPreview ->
            Text(
                text = if (progress is DiscoveryState.Scanning) {"Scanning..."} else {"Loading ${currentPreview?.name ?: ""}"},
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp), 
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Black,
                fontSize = 2.0.em,
                color = paletteColor,
            )
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Error: $message")
        Chip(
            onClick = onRetry,
            label = { Text("Retry") },
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun LibraryHome(viewModel: TrackListViewModel, onNavigate: (LibraryScreen) -> Unit) {
    val columnState = rememberResponsiveColumnState()
    val context = LocalContext.current
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(
            columnState = columnState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Text(text = "Library", modifier = Modifier.padding(bottom = 8.dp), fontWeight = FontWeight.SemiBold) }
            item {
                Chip(
                    onClick = { onNavigate(LibraryScreen.Artists) },
                    label = { Text("Artists") },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_artist),
                            contentDescription = "Artists",
                            modifier = Modifier.size(ChipDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                Chip(
                    onClick = { onNavigate(LibraryScreen.AllAlbums) },
                    label = { Text("Albums") },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_album),
                            contentDescription = "Albums",
                            modifier = Modifier.size(ChipDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                Chip(
                    onClick = { onNavigate(LibraryScreen.AllTracks) },
                    label = { Text("Tracks") },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_track),
                            contentDescription = "Tracks",
                            modifier = Modifier.size(ChipDefaults.IconSize)
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = ChipDefaults.secondaryChipColors(),
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(ChipDefaults.Height),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { viewModel.rescan() },
                        colors = ButtonDefaults.secondaryButtonColors(),
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        shape = RoundedCornerShape(
                            topStart = 48.dp,
                            bottomStart = 48.dp,
                            topEnd = 8.dp,
                            bottomEnd = 8.dp
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = "Rescan and Refresh Database",
                            modifier = Modifier.size(ButtonDefaults.DefaultIconSize)
                        )
                    }
                    Button(
                        onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        ),
                        modifier = Modifier.weight(2f).fillMaxSize(),
                        shape = RoundedCornerShape(
                            topStart = 8.dp,
                            bottomStart = 8.dp,
                            topEnd = 48.dp,
                            bottomEnd = 48.dp,
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = "Open Settings",
                            modifier = Modifier.size(ButtonDefaults.DefaultIconSize)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun ArtistList(viewModel: TrackListViewModel, onArtistSelected: (String) -> Unit) {
    val artists = viewModel.artistsPaged.collectAsLazyPagingItems()
    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(columnState = columnState) {
            item { Text("Artists", modifier = Modifier.padding(bottom = 8.dp), fontWeight = FontWeight.SemiBold) }
            if (artists.loadState.refresh is LoadState.Loading) {
                items(5) { PlaceholderArtistItem() }
            } else if (artists.loadState.refresh is LoadState.NotLoading && artists.itemCount == 0) {
                 item { Text("No artists found.") }
            } else {
                items(
                    count = artists.itemCount
                ) { index ->
                    val artist = artists[index]
                    if (artist != null) {
                        ArtistItem(artist = artist, onClick = { onArtistSelected(artist.name) })
                    } else {
                        PlaceholderArtistItem()
                    }
                }
                
                if (artists.loadState.append is LoadState.Loading) {
                    item { PlaceholderArtistItem() }
                }
            }
        }
    }
}

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun AlbumList(viewModel: TrackListViewModel, artist: String? = null, onAlbumSelected: (Album) -> Unit) {
    val albums = remember(artist) {
        if (artist != null) {
            viewModel.getAlbumsByArtistPaged(artist)
        } else {
            viewModel.albumsPaged
        }
    }.collectAsLazyPagingItems()
    val columnState = rememberResponsiveColumnState()
    ScreenScaffold(scrollState = columnState) {
        ScalingLazyColumn(columnState = columnState) {
            item { Text(if (artist != null) artist else "All Albums", modifier = Modifier.padding(bottom = 8.dp), fontWeight = FontWeight.SemiBold) }
            if (albums.loadState.refresh is LoadState.Loading) {
                items(5) { PlaceholderAlbumItem(showArtist = artist == null) }
            } else if (albums.loadState.refresh is LoadState.NotLoading && albums.itemCount == 0) {
                 item { Text("No albums found.") }
            } else {
                items(
                    count = albums.itemCount,
                    key = albums.itemKey { it.id }
                ) { index ->
                    val album = albums[index]
                    if (album != null) {
                        AlbumItem(
                            album = album,
                            showArtist = artist == null,
                            onClick = { onAlbumSelected(album) }
                        )
                    } else {
                        PlaceholderAlbumItem(showArtist = artist == null)
                    }
                }
                
                if (albums.loadState.append is LoadState.Loading) {
                    item { PlaceholderAlbumItem(showArtist = artist == null) }
                }
            }
        }
    }
}
