package com.example.cassette.tile

import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.wrap
import androidx.wear.protolayout.DimensionBuilders.weight
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.StateBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.expression.DynamicBuilders
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.expression.DynamicDataBuilders.DynamicDataValue
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.iconEdgeButton
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.ButtonDefaults
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.ColorScheme
import androidx.wear.protolayout.types.layoutString
import androidx.wear.protolayout.types.asLayoutString
import androidx.wear.protolayout.TypeBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.cassette.R
import com.example.cassette.data.repositories.MusicRepository
import com.example.cassette.data.repositories.PlayerRepository
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Album
import com.example.cassette.presentation.MainActivity
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.BlurMaskFilter
import android.graphics.RenderNode
import android.graphics.RenderEffect
import android.graphics.Shader
import android.util.Log
import java.io.ByteArrayOutputStream
import java.nio.Buffer
import java.nio.ByteBuffer
import javax.inject.Inject

private const val RESOURCES_VERSION = "WWARIP"
private val ACTION_KEY = AppDataKey<DynamicString>("action")
private val TRACK_TITLE_KEY = AppDataKey<DynamicString>("track_title")
private val TRACK_ARTIST_KEY = AppDataKey<DynamicString>("track_artist")
private val TRACK_PROGRESS_KEY = AppDataKey<DynamicBuilders.DynamicFloat>("track_progress")
private val TRACK_PROGRESS_MIN_KEY = AppDataKey<DynamicBuilders.DynamicFloat>("track_progress_min")
private val TRACK_PROGRESS_MAX_KEY = AppDataKey<DynamicBuilders.DynamicFloat>("track_progress_max")
private val TRACK_VIBRANT_COLOR_KEY = AppDataKey<DynamicBuilders.DynamicColor>("track_vibrant_color")
private val TRACK_DARK_VIBRANT_COLOR_KEY = AppDataKey<DynamicBuilders.DynamicColor>("track_dark_vibrant_color")
private val TRACK_LIGHT_VIBRANT_COLOR_KEY = AppDataKey<DynamicBuilders.DynamicColor>("track_light_vibrant_color")
private val TRACK_MUTED_COLOR_KEY = AppDataKey<DynamicBuilders.DynamicColor>("track_muted_color")
private val TRACK_DARK_MUTED_COLOR_KEY = AppDataKey<DynamicBuilders.DynamicColor>("track_dark_muted_color")
private val TRACK_LIGHT_MUTED_COLOR_KEY = AppDataKey<DynamicBuilders.DynamicColor>("track_light_muted_color")
private val TRACK_DOMINANT_COLOR_KEY = AppDataKey<DynamicBuilders.DynamicColor>("track_dominant_color")

@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class SingleFavoriteTileService : SuspendingTileService() {

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
        val action = state.keyToValueMapping[ACTION_KEY as AppDataKey<*>]?.getStringValue()

        when (action) {
            "play_fav" -> {
                val albumId = "Megadeth|Rust in Peace"
                val tracks = musicRepository.getTracksByAlbum(albumId).first()
                if (tracks.isNotEmpty()) {
                    playerRepository.play(tracks[0], PlayerRepository.QueueSource.ALBUM)
                }
            }
        }

        val playbackState = playerRepository.playbackState.first()
        val isPlaying = playbackState.isPlaying
        val stateBuilder = StateBuilders.State.Builder()
        val singleTileTimeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(quickPlayLayout(this, requestParams.deviceConfiguration, isPlaying))
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(singleTileTimeline)
            .setState(stateBuilder.build())
            .setFreshnessIntervalMillis(0L)
            .build()
    }
}

private suspend fun tileResources(
    requestParams: RequestBuilders.ResourcesRequest,
): ResourceBuilders.Resources {
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
        .addIdToImageMapping(
            "ic_radiation",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.ic_radiation)
                        .build()
                )
                .build()
        )
    return resourcesBuilder.build()
}

private fun quickPlayLayout(
    context: Context,
    deviceConfiguration: DeviceParametersBuilders.DeviceParameters,
    isPlaying: Boolean,
): LayoutElementBuilders.LayoutElement = materialScope(context, deviceConfiguration, false) {
    val darkColor = ColorBuilders.ColorProp.Builder(0xFF1C308F.toInt()).build()
    val lightColor = ColorBuilders.ColorProp.Builder(0xFFEDFB6A.toInt()).build()

    val playFavAction = ActionBuilders.LoadAction.Builder()
        .setRequestState(
            StateBuilders.State.Builder()
                .addKeyToValueMapping(ACTION_KEY, DynamicDataValue.fromString("play_fav"))
                .build()
        )
        .build()
    
    val launchAppAction = ActionBuilders.LaunchAction.Builder()
        .setAndroidActivity(
            ActionBuilders.AndroidActivity.Builder()
                .setClassName(MainActivity::class.java.name)
                .setPackageName(context.packageName)
                .build()
        )
        .build()

    primaryLayout(
        mainSlot = {
            LayoutElementBuilders.Row.Builder()
                .setWidth(expand())
                .setHeight(expand())
                .addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(weight(1f))
                        .setHeight(expand())
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setBackground(
                                    ModifiersBuilders.Background.Builder()
                                        .setColor(darkColor)
                                        .setCorner(
                                            ModifiersBuilders.Corner.Builder()
                                                .setBottomLeftRadius(
                                                    ModifiersBuilders.CornerRadius.Builder(
                                                        dp(64f),
                                                        dp(48f),
                                                    ).build())
                                                .setBottomRightRadius(
                                                    ModifiersBuilders.CornerRadius.Builder(
                                                        dp(64f),
                                                        dp(48f),
                                                    ).build())
                                                .setTopLeftRadius(
                                                    ModifiersBuilders.CornerRadius.Builder(
                                                        dp((deviceConfiguration.screenWidthDp / 2).toFloat()),
                                                        dp((deviceConfiguration.screenHeightDp / 2).toFloat()),
                                                    ).build())
                                                .setTopRightRadius(
                                                    ModifiersBuilders.CornerRadius.Builder(
                                                        dp((deviceConfiguration.screenWidthDp / 2).toFloat()),
                                                        dp((deviceConfiguration.screenHeightDp / 2).toFloat()),
                                                    ).build())
                                                .build()
                                        ).build()
                                )
                                .setClickable(
                                    ModifiersBuilders.Clickable.Builder()
                                        .setId("fav_1")
                                        .setOnClick(playFavAction)
                                        .build()
                                ).build()
                        )
                        .addContent(
                            LayoutElementBuilders.Column.Builder()
                                .addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(4f)).build())
                                .addContent(
                                    LayoutElementBuilders.Image.Builder()
                                        .setResourceId("ic_radiation")
                                        .setWidth(dp(64f))
                                        .setHeight(dp(64f))
                                        .setColorFilter(
                                            LayoutElementBuilders.ColorFilter.Builder()
                                                .setTint(lightColor)
                                                .build()
                                        ).build()
                                ).build()
                        ).build()
                ).build()
        },
        bottomSlot = {
            iconEdgeButton(
                onClick = ModifiersBuilders.Clickable.Builder()
                    .setId("launch_app")
                    .setOnClick(launchAppAction)
                    .build(),
                iconContent = { icon("ic_library_music") },
            )
        }
    )
}

