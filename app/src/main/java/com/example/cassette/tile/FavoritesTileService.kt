package com.example.cassette.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import androidx.wear.protolayout.layout.androidImageResource
import androidx.wear.protolayout.layout.imageResource
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.iconEdgeButton
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.example.cassette.R
import com.example.cassette.data.repositories.PlayerRepository
import com.example.cassette.data.types.Album
import com.example.cassette.presentation.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private val ACTION_KEY = AppDataKey<DynamicString>("action")
private val ALBUM_ARTIST_KEY = AppDataKey<DynamicString>("album_artist")
private val ALBUM_NAME_KEY = AppDataKey<DynamicString>("album_name")

class FavoritesTileService : Material3TileService(allowDynamicTheme = false) {

    override suspend fun MaterialScope.tileResponse(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val repositories = EntryPointAccessors.fromApplication(context.applicationContext, TileEntryPoint::class.java)
        val playerRepository = repositories.playerRepository()
        val musicRepository = repositories.musicRepository()
        val state = requestParams.currentState
        val action = state.keyToValueMapping[ACTION_KEY]?.getStringValue()

        when (action) {
            "play_album" -> {
                val albumArtist = state.keyToValueMapping[ALBUM_ARTIST_KEY]?.getStringValue()
                val albumName = state.keyToValueMapping[ALBUM_NAME_KEY]?.getStringValue()
                if (albumArtist != null && albumName != null) {
                    val tracks = musicRepository.getTracksByAlbum(albumArtist, albumName).first()
                    if (tracks.isNotEmpty()) {
                        playerRepository.play(tracks[0], PlayerRepository.QueueSource.ALBUM)
                    }
                }
            }
        }

        val albums = musicRepository.getAlbums().first().take(2)

        val singleTileTimeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(quickPlayLayout(albums))
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setTileTimeline(singleTileTimeline)
            .setFreshnessIntervalMillis(0L)
            .build()
    }
}

private fun MaterialScope.quickPlayLayout(albums: List<Album>): LayoutElementBuilders.LayoutElement {
    val darkColor = LayoutColor(staticArgb = 0xFF1C308F.toInt())
    val lightColor = LayoutColor(staticArgb = 0xFFEDFB6A.toInt())

    val launchAppAction = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setClassName(MainActivity::class.java.name)
                .setPackageName(context.packageName)
                .build()
        )
        .build()

    fun playAlbumAction(artist: String, albumName: String) = ActionBuilders.LoadAction.Builder()
        .setRequestState(
            StateBuilders.State.Builder()
                .addKeyToValueMapping(ACTION_KEY, DynamicDataValue.fromString("play_album"))
                .addKeyToValueMapping(ALBUM_ARTIST_KEY, DynamicDataValue.fromString(artist))
                .addKeyToValueMapping(ALBUM_NAME_KEY, DynamicDataValue.fromString(albumName))
                .build()
        )
        .build()

    fun albumColor(album: Album) = album.palette?.darkVibrant ?: album.palette?.darkMuted ?: album.palette?.muted ?: colorScheme.surfaceContainer.staticArgb
    val edgeButtonForeground = albums.firstOrNull()?.palette?.lightVibrant ?: albums.firstOrNull()?.palette?.lightMuted ?: colorScheme.onSurface.staticArgb

    fun radius(a: Float, b: Float) = ModifiersBuilders.CornerRadius.Builder(dp(a), dp(b)).build()

    fun albumRow(index: Int, album: Album) = LayoutElementBuilders.Box.Builder()
        .setWidth(expand())
        .setHeight(weight(1f))
        .setModifiers(
            ModifiersBuilders.Modifiers.Builder()
                .setBackground(
                    ModifiersBuilders.Background.Builder()
                        .setColor(argb(albumColor(album)))
                        .setCorner(
                            ModifiersBuilders.Corner.Builder()
                                .setTopLeftRadius(if (index == 0) { radius(64f, 40f) } else { radius(16f, 16f) })
                                .setTopRightRadius(radius(16f, 16f))
                                .setBottomLeftRadius(radius(16f, 16f))
                                .setBottomRightRadius(if (index == 0) { radius(16f, 16f) } else { radius(64f, 40f) })
                                .build()
                        )
                        .build()
                )
                .setClickable(
                    ModifiersBuilders.Clickable.Builder()
                        .setId("fav_${index + 1}")
                        .setOnClick(playAlbumAction(album.artist, album.name))
                        .build()
                )
                .build()
        )
        .addContent(
            LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setModifiers(
                    ModifiersBuilders.Modifiers.Builder()
                        .setPadding(
                            ModifiersBuilders.Padding.Builder()
                                .setStart(dp(if (index == 0) { 12f } else { 6f }))
                                .setEnd(dp(if (index == 0) { 6f } else { 12f }))
                                .build()
                        )
                        .build()
                )
                .addContent(
                    text(
                        text = album.name.layoutString,
                        typography = Typography.LABEL_MEDIUM,
                        color = colorScheme.onSurface,
                        overflow = LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE,
                        maxLines = 2,
                    )
                )
                .build()
        )
        .build()

    return primaryLayout(
        mainSlot = {
            LayoutElementBuilders.Box.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setBackground(
                                    ModifiersBuilders.Background.Builder()
                                        .setColor(argb(colorScheme.surfaceContainerLow.staticArgb))
                                        .setCorner(
                                            ModifiersBuilders.Corner.Builder()
                                                .setTopLeftRadius(dp(80f), dp(60f))
                                                .setTopRightRadius(dp(24f), dp(24f))
                                                .setBottomLeftRadius(dp(24f), dp(24f))
                                                .setBottomRightRadius(dp(80f), dp(60f))
                                                .build()
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .addContent(
                    LayoutElementBuilders.Column.Builder()
                        .setWidth(expand())
                        .setHeight(expand())
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setPadding(
                                    ModifiersBuilders.Padding.Builder()
                                        .setStart(dp(12f))
                                        .setEnd(dp(12f))
                                        .setTop(dp(12f))
                                        .setBottom(dp(12f))
                                        .build()
                                )
                                .build()
                        )
                        .apply {
                            if (albums.isEmpty()) {
                                addContent(
                                    text(
                                        text = "No albums yet".layoutString,
                                        typography = Typography.LABEL_MEDIUM,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                )
                            } else {
                                albums.forEachIndexed { index, album ->
                                    if (index > 0) {
                                        addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(6f)).build())
                                    }
                                    addContent(albumRow(index, album))
                                }
                            }
                        }
                        .build()
                )
                .build()
        },
        bottomSlot = {
            iconEdgeButton(
                onClick = ModifiersBuilders.Clickable.Builder()
                    .setId("launch_app")
                    .setOnClick(launchAppAction)
                    .build(),
                colors = ButtonColors(
                    containerColor = LayoutColor(staticArgb = colorScheme.surfaceContainerLow.staticArgb),
                    iconColor = LayoutColor(staticArgb = edgeButtonForeground),
                ),
                iconContent = {
                    icon(imageResource(androidImage = androidImageResource(R.drawable.ic_library_music)))
                },
            )
        }
    )
}
