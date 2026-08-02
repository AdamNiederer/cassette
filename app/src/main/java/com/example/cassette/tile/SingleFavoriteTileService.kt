package com.example.cassette.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
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
import androidx.wear.protolayout.material3.ButtonColors
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.example.cassette.R
import com.example.cassette.data.repositories.PlayerRepository
import com.example.cassette.presentation.MainActivity
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first

private val ACTION_KEY = AppDataKey<DynamicString>("action")

class SingleFavoriteTileService : Material3TileService(allowDynamicTheme = false) {

    override suspend fun MaterialScope.tileResponse(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val repositories = EntryPointAccessors.fromApplication(context.applicationContext, TileEntryPoint::class.java)
        val playerRepository = repositories.playerRepository()
        val musicRepository = repositories.musicRepository()
        val state = requestParams.currentState
        val action = state.keyToValueMapping[ACTION_KEY]?.getStringValue()

        when (action) {
            "play_fav" -> {
                val tracks = musicRepository.getTracksByAlbum("Megadeth", "Rust in Peace").first()
                if (tracks.isNotEmpty()) {
                    playerRepository.play(tracks[0], PlayerRepository.QueueSource.ALBUM)
                }
            }
        }

        val stateBuilder = StateBuilders.State.Builder()
        val singleTileTimeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder()
                    .setLayout(
                        LayoutElementBuilders.Layout.Builder()
                            .setRoot(quickPlayLayout())
                            .build()
                    )
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setTileTimeline(singleTileTimeline)
            .setState(stateBuilder.build())
            .setFreshnessIntervalMillis(0L)
            .build()
    }
}

private fun MaterialScope.quickPlayLayout(): LayoutElementBuilders.LayoutElement {
    val darkColor = ColorBuilders.ColorProp.Builder(0xFF1C308F.toInt()).build()
    val lightColor = ColorBuilders.ColorProp.Builder(0xFFEDFB6A.toInt()).build()
    val lightLayoutColor = LayoutColor(staticArgb = 0xFFEDFB6A.toInt())
    val blackLayoutColor = LayoutColor(staticArgb = 0xFF000000.toInt())

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

    return primaryLayout(
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
                                    tileImage(
                                        resId = R.drawable.ic_radiation,
                                        protoResourceId = "ic_radiation",
                                        width = dp(64f),
                                        height = dp(64f),
                                        tint = lightColor,
                                    )
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
                colors = ButtonColors(
                    containerColor = blackLayoutColor,
                    iconColor = lightLayoutColor,
                ),
                iconContent = {
                    icon(imageResource(androidImage = androidImageResource(R.drawable.ic_library_music)))
                },
            )
        }
    )
}
