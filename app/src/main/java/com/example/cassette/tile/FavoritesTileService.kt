package com.example.cassette.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.iconEdgeButton
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.example.cassette.R
import com.example.cassette.data.repositories.MusicRepository
import com.example.cassette.data.repositories.PlayerRepository
import com.example.cassette.data.types.Album
import com.example.cassette.data.types.AlbumPalette
import com.example.cassette.presentation.MainActivity
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

private const val RESOURCES_VERSION = "0"
private val ACTION_KEY = AppDataKey<DynamicString>("action")
private val ALBUM_ARTIST_KEY = AppDataKey<DynamicString>("album_artist")
private val ALBUM_NAME_KEY = AppDataKey<DynamicString>("album_name")

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class FavoritesTileService : SuspendingTileService() {

    @Inject
    lateinit var playerRepository: PlayerRepository

    @Inject
    lateinit var musicRepository: MusicRepository

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = tileResources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
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
                            .setRoot(quickPlayLayout(this, requestParams.deviceConfiguration, albums))
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(singleTileTimeline)
            .setFreshnessIntervalMillis(0L)
            .build()
    }
}

private fun tileResources(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
    val resourcesBuilder = ResourceBuilders.Resources.Builder()
        .setVersion(requestParams.version)
        .addIdToImageMapping(
            "ic_library_music",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.ic_library_music) 
                        .build()
                )
                .build()
        )
    return resourcesBuilder.build()
}

private fun quickPlayLayout(
    context: Context,
    deviceConfiguration: DeviceParametersBuilders.DeviceParameters,
    albums: List<Album>
): LayoutElementBuilders.LayoutElement = materialScope(context, deviceConfiguration, false) {
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

    primaryLayout(
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
                iconContent = { icon("ic_library_music") },
            )
        }
    )
}

