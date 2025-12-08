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

private const val RESOURCES_VERSION = "0"
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
class MainTileService : SuspendingTileService() {

    @Inject
    lateinit var playerRepository: PlayerRepository

    @Inject
    lateinit var musicRepository: MusicRepository

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = tileResources(requestParams, this, musicRepository)

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
            "play_pause" -> {
                val playbackState = playerRepository.playbackState.first()
                if (playbackState.isPlaying) playerRepository.pause() else playerRepository.play()
            }
            "next" -> playerRepository.seekToNext()
            "prev" -> playerRepository.seekToPrevious()
        }

        val playbackState = playerRepository.playbackState.first()
        val isPlaying = playbackState.isPlaying
        val track = playerRepository.currentTrack.value

        val stateBuilder = StateBuilders.State.Builder()
        if (track != null) {
            stateBuilder.addKeyToValueMapping(TRACK_TITLE_KEY, DynamicDataValue.fromString(track.title))
            stateBuilder.addKeyToValueMapping(TRACK_ARTIST_KEY, DynamicDataValue.fromString(track.artist))
            val progress = if (playbackState.duration > 0) {
                playbackState.currentPosition.toFloat() / playbackState.duration.toFloat()
            } else 0f
            stateBuilder.addKeyToValueMapping(TRACK_PROGRESS_KEY, DynamicDataValue.fromFloat(progress))
            stateBuilder.addKeyToValueMapping(TRACK_PROGRESS_MAX_KEY, DynamicDataValue.fromFloat((progress + 0.15f).coerceAtMost(1f)))
            stateBuilder.addKeyToValueMapping(TRACK_PROGRESS_MIN_KEY, DynamicDataValue.fromFloat((progress - 0.15f).coerceAtLeast(0f)))

            val album = musicRepository.getAlbum(track.albumId).first()
            album?.palette?.let { palette ->
                palette.vibrant?.let { stateBuilder.addKeyToValueMapping(TRACK_VIBRANT_COLOR_KEY, DynamicDataValue.fromColor(it)) }
                palette.darkVibrant?.let { stateBuilder.addKeyToValueMapping(TRACK_DARK_VIBRANT_COLOR_KEY, DynamicDataValue.fromColor(it)) }
                palette.lightVibrant?.let { stateBuilder.addKeyToValueMapping(TRACK_LIGHT_VIBRANT_COLOR_KEY, DynamicDataValue.fromColor(it)) }
                palette.muted?.let { stateBuilder.addKeyToValueMapping(TRACK_MUTED_COLOR_KEY, DynamicDataValue.fromColor(it)) }
                palette.darkMuted?.let { stateBuilder.addKeyToValueMapping(TRACK_DARK_MUTED_COLOR_KEY, DynamicDataValue.fromColor(it)) }
                palette.lightMuted?.let { stateBuilder.addKeyToValueMapping(TRACK_LIGHT_MUTED_COLOR_KEY, DynamicDataValue.fromColor(it)) }
                palette.dominant?.let { stateBuilder.addKeyToValueMapping(TRACK_DOMINANT_COLOR_KEY, DynamicDataValue.fromColor(it)) }
            }
        }

        val singleTileTimeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(
                                if (isPlaying) playerLayout(this, playerRepository, requestParams.deviceConfiguration) 
                                else quickPlayLayout(this, requestParams.deviceConfiguration)
                            )
                            .build()
                    )
                    .build()
            )
            .build()

        Log.i("MainTileService", "setResourcesVersion ${track?.id?.toString()}")

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(track?.id?.toString() ?: RESOURCES_VERSION)
            .setTileTimeline(singleTileTimeline)
            .setState(stateBuilder.build())
            .setFreshnessIntervalMillis(if (isPlaying) 1000L else 0L)
            .build()
    }
}

private suspend fun tileResources(
    requestParams: RequestBuilders.ResourcesRequest,
    context: Context,
    musicRepository: MusicRepository
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
        .addIdToImageMapping(
            "ic_play",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.ic_play) 
                        .build()
                )
                .build()
        )
        .addIdToImageMapping(
            "ic_pause",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.ic_pause) 
                        .build()
                )
                .build()
        )
        .addIdToImageMapping(
            "ic_next",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.ic_next) 
                        .build()
                )
                .build()
        )
        .addIdToImageMapping(
            "ic_prev",
            ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.ic_prev) 
                        .build()
                )
                .build()
        )

    Log.i("MainTileService", "requestResources ${requestParams.version}")

    // if (requestParams.version != RESOURCES_VERSION) {
    //     Log.i("MainTileService", "foo")
    //     try {
    //         val track = musicRepository.getTrack(requestParams.version.toLong()).first()
    //         if (track != null) {
    //             val album = musicRepository.getAlbum(track.albumId).first()
    //             album?.thumbnail?.let { bitmap ->
    //                 val blurred = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
                    
    //                 val canvas = Canvas(blurred)
    //                 // val renderNode = RenderNode("BlurNode").apply {
    //                 //     setPosition(0, 0, bitmap.width, bitmap.height)
    //                 //     setRenderEffect(RenderEffect.createBlurEffect(0f, 0f, Shader.TileMode.REPEAT))
    //                 // }
                    
    //                 // val recordingCanvas = renderNode.beginRecording()
    //                 // recordingCanvas.drawBitmap(bitmap, 0f, 0f, null)
    //                 // renderNode.endRecording()
                    
    //                 // canvas.drawRenderNode(renderNode)

    //                 // val paint = Paint().apply {
    //                 //     isAntiAlias = true
    //                 //     isFilterBitmap = true
    //                 //     maskFilter = BlurMaskFilter(64f, BlurMaskFilter.Blur.INNER)
    //                 // }
    //                 // canvas.drawBitmap(bitmap, 0f, 0f, paint)
    //                 // canvas.drawBitmap(bitmap, 1f, 1f, paint)
    //                 // canvas.drawBitmap(bitmap, 1f, 0f, paint)
    //                 // canvas.drawBitmap(bitmap, 0f, 1f, paint)
    //                 // canvas.drawBitmap(bitmap, 2f, 0f, paint)
    //                 // canvas.drawBitmap(bitmap, 0f, 2f, paint)


    //                 // val byteBuffer = ByteBuffer.allocate(blurred.byteCount)
    //                 // blurred.copyPixelsToBuffer(byteBuffer)
    //                 // val bytes = byteBuffer.array()

    //                 Log.i("MainTileService", "blurred to ${bytes.size / 1024}kb")
    //                 if (bytes.size < 1024 * 1024) {
    //                     Log.i("MainTileService", "sending ${bytes.size / 1024}kb")
    //                     resourcesBuilder.addIdToImageMapping(
    //                         "album_art",
    //                         ResourceBuilders.ImageResource.Builder()
    //                             .setInlineResource(
    //                                 ResourceBuilders.InlineImageResource.Builder()
    //                                     .setData(bytes)
    //                                     .setWidthPx(bitmap.width)
    //                                     .setHeightPx(bitmap.height)
    //                                     .setFormat(ResourceBuilders.IMAGE_FORMAT_RGB_565)
    //                                     .build()
    //                             )
    //                             .build()
    //                     )
    //                 }
    //             }
    //         }
    //     } catch (e: Exception) {
    //         Log.e("MainTileService", "Error loading album art", e)
    //     }
    // }

    return resourcesBuilder.build()
}


private suspend fun playerLayout(
    context: Context,
    playerRepository: PlayerRepository,
    deviceConfiguration: DeviceParametersBuilders.DeviceParameters
): LayoutElementBuilders.LayoutElement {
    val track = playerRepository.currentTrack.value ?: return quickPlayLayout(context, deviceConfiguration)
    val playbackState = playerRepository.playbackState.first()
    val isPlaying = playbackState.isPlaying

    return materialScope(context, deviceConfiguration, false) {
        val titleProp = DynamicString.from(TRACK_TITLE_KEY).asLayoutString(
            "Track Title",
            TypeBuilders.StringLayoutConstraint.Builder("Reasonably Long Title").build()
        )

        val artistProp = DynamicString.from(TRACK_ARTIST_KEY).asLayoutString(
            "Track Artist",
            TypeBuilders.StringLayoutConstraint.Builder("Longish Artist").build()
        )

        val playPauseAction = ActionBuilders.LoadAction.Builder()
            .setRequestState(
                StateBuilders.State.Builder()
                    .addKeyToValueMapping(ACTION_KEY, DynamicDataValue.fromString("play_pause"))
                    .build()
            ).build()

        val nextAction = ActionBuilders.LoadAction.Builder()
            .setRequestState(
                StateBuilders.State.Builder()
                    .addKeyToValueMapping(ACTION_KEY, DynamicDataValue.fromString("next"))
                    .build()
            ).build()

        val prevAction = ActionBuilders.LoadAction.Builder()
            .setRequestState(
                StateBuilders.State.Builder()
                    .addKeyToValueMapping(ACTION_KEY, DynamicDataValue.fromString("prev"))
                    .build()
            ).build()

        val lightVibrantColorProp = ColorBuilders.ColorProp.Builder(colorScheme.primary.staticArgb)
            .setDynamicValue(DynamicBuilders.DynamicColor.from(TRACK_LIGHT_VIBRANT_COLOR_KEY))
            .build()

        val darkMutedColorProp = ColorBuilders.ColorProp.Builder(colorScheme.surfaceContainer.staticArgb)
            .setDynamicValue(DynamicBuilders.DynamicColor.from(TRACK_DARK_MUTED_COLOR_KEY))
            .build()

        val lightMutedColorProp = ColorBuilders.ColorProp.Builder(colorScheme.surfaceContainer.staticArgb)
            .setDynamicValue(DynamicBuilders.DynamicColor.from(TRACK_LIGHT_MUTED_COLOR_KEY))
            .build()

        val brush = ColorBuilders.SweepGradient.Builder(
            ColorBuilders.ColorStop.Builder(lightVibrantColorProp, TypeBuilders.FloatProp.Builder(0f).build()).build(),
            ColorBuilders.ColorStop.Builder(
                lightVibrantColorProp,
                TypeBuilders.FloatProp.Builder(0f)
                    .setDynamicValue(DynamicBuilders.DynamicFloat.from(TRACK_PROGRESS_MIN_KEY).div(3f))
                    .build()
            ).build(),
            ColorBuilders.ColorStop.Builder(
                darkMutedColorProp,
                TypeBuilders.FloatProp.Builder(0f)
                    .setDynamicValue(DynamicBuilders.DynamicFloat.from(TRACK_PROGRESS_MAX_KEY).div(3f))
                    .build()
            ).build(),
            ColorBuilders.ColorStop.Builder(darkMutedColorProp, TypeBuilders.FloatProp.Builder(1f).build()).build(),
        ).build()

        LayoutElementBuilders.Box.Builder()
            .setWidth(dp(context.resources.configuration.screenWidthDp.toFloat()))
            .setHeight(dp(context.resources.configuration.screenHeightDp.toFloat()))
            .addContent(
                LayoutElementBuilders.Arc.Builder()
                    .addContent(
                        LayoutElementBuilders.ArcLine.Builder()
                            .setLength(DimensionBuilders.DegreesProp.Builder(120f).build())
                            .setBrush(brush)
                            .setThickness(dp(8f))
                            .build()
                    )
                    .build()
            )
            .addContent(
                LayoutElementBuilders.Column.Builder()
                    .addContent(
                        text(
                            text = titleProp,
                            typography = Typography.TITLE_SMALL,
                            color = colorScheme.onSurface,
                            overflow = LayoutElementBuilders.TEXT_OVERFLOW_MARQUEE,
                            maxLines = 1,
                        )
                    )
                    .addContent(
                        text(
                            text = artistProp,
                            typography = Typography.LABEL_SMALL,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                        )
                    )
                    .addContent(
                        LayoutElementBuilders.Row.Builder()
                            .addContent(
                                LayoutElementBuilders.Box.Builder()
                                    .setModifiers(ModifiersBuilders.Modifiers.Builder()
                                                      .setClickable(ModifiersBuilders.Clickable.Builder()
                                                                        .setId("prev")
                                                                        .setOnClick(prevAction)
                                                                        .build())
                                                      .build())
                                    .addContent(LayoutElementBuilders.Image.Builder()
                                                    .setResourceId("ic_prev")
                                                    .setWidth(dp(24f))
                                                    .setHeight(dp(24f))
                                                    .setColorFilter(
                                                        LayoutElementBuilders.ColorFilter.Builder()
                                                            .setTint(lightMutedColorProp)
                                                            .build()
                                                    )
                                                    .build())
                                    .build()
                            )
                            .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(12f)).build())
                            .addContent(
                                LayoutElementBuilders.Box.Builder()
                                    .setModifiers(ModifiersBuilders.Modifiers.Builder()
                                                      .setClickable(ModifiersBuilders.Clickable.Builder()
                                                                        .setId("play_pause")
                                                                        .setOnClick(playPauseAction)
                                                                        .build())
                                                      .build())
                                    .addContent(LayoutElementBuilders.Image.Builder()
                                                    .setResourceId(if (isPlaying) "ic_pause" else "ic_play")
                                                    .setWidth(dp(32f))
                                                    .setHeight(dp(32f))
                                                    .setColorFilter(
                                                        LayoutElementBuilders.ColorFilter.Builder()
                                                            .setTint(lightMutedColorProp)
                                                            .build()
                                                    )
                                                    .build())
                                    .build()
                            )
                            .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(12f)).build())
                            .addContent(
                                LayoutElementBuilders.Box.Builder()
                                    .setModifiers(ModifiersBuilders.Modifiers.Builder()
                                                      .setClickable(ModifiersBuilders.Clickable.Builder()
                                                                        .setId("next")
                                                                        .setOnClick(nextAction)
                                                                        .build())
                                                      .build())
                                    .addContent(LayoutElementBuilders.Image.Builder()
                                                    .setResourceId("ic_next")
                                                    .setWidth(dp(24f))
                                                    .setHeight(dp(24f))
                                                    .setColorFilter(
                                                        LayoutElementBuilders.ColorFilter.Builder()
                                                            .setTint(lightMutedColorProp)
                                                            .build()
                                                    )
                                                    .build())
                                    .build()
                            ).build()
                    ).build()
            ).build()
    }
}

private fun quickPlayLayout(
    context: Context,
    deviceConfiguration: DeviceParametersBuilders.DeviceParameters
): LayoutElementBuilders.LayoutElement = materialScope(context, deviceConfiguration, false) {
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
                                        .setColor(ColorBuilders.ColorProp.Builder(0xFF1C308F.toInt()).build())
                                        .setCorner(
                                            ModifiersBuilders.Corner.Builder()
                                                .setRadius(dp(16f))
                                                .setTopLeftRadius(
                                                    ModifiersBuilders.CornerRadius.Builder(
                                                        dp(100f),
                                                        dp(100f),
                                                    ).build())
                                                .build()
                                        )
                                        .build()
                                )
                                .setClickable(
                                    ModifiersBuilders.Clickable.Builder()
                                        .setId("fav_1")
                                        .setOnClick(playFavAction)
                                        .build()
                                )
                                .build()
                        )
                        .addContent(
                            LayoutElementBuilders.Image.Builder()
                                .setResourceId("ic_radiation")
                                .setWidth(dp(32f))
                                .setHeight(dp(32f))
                                .setColorFilter(
                                    LayoutElementBuilders.ColorFilter.Builder()
                                        .setTint(ColorBuilders.ColorProp.Builder(0xFFEDFB6A.toInt()).build())
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .addContent(LayoutElementBuilders.Spacer.Builder().setWidth(dp(4f)).build())
                .addContent(
                    LayoutElementBuilders.Box.Builder()
                        .setWidth(weight(1f))
                        .setHeight(expand())
                        .setModifiers(
                            ModifiersBuilders.Modifiers.Builder()
                                .setBackground(
                                    ModifiersBuilders.Background.Builder()
                                        .setColor(argb(colorScheme.surfaceContainer.staticArgb))
                                        .setCorner(
                                            ModifiersBuilders.Corner.Builder()
                                                .setRadius(dp(16f))
                                                .setTopRightRadius(
                                                    ModifiersBuilders.CornerRadius.Builder(
                                                        dp(100f),
                                                        dp(100f),
                                                    ).build())
                                                .build()
                                        )
                                        .build()
                                )
                                .setClickable(
                                    ModifiersBuilders.Clickable.Builder()
                                        .setId("fav_2")
                                        .setOnClick(playFavAction)
                                        .build()
                                )
                                .build()
                        )
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
                iconContent = { icon("ic_library_music") },
            )
        }
    )
}

