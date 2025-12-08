package com.example.cassette.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.material.Icon
import com.example.cassette.R
import com.example.cassette.data.types.Album
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Artist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Brush
import androidx.wear.compose.material.MaterialTheme
import android.util.Log
import androidx.compose.ui.graphics.BlendMode

object PlaceholderColors {
    @Composable
    fun element() = lerp(MaterialTheme.colors.surface, Color.White, 0.25f)
    
    @Composable
    fun surface() = lerp(MaterialTheme.colors.surface, Color.Black, 0.25f)
}

@Composable
fun PlaceholderTrackItem(showIcon: Boolean = true) {
    Chip(
        onClick = { },
        enabled = false,
        icon = if (showIcon) {
            {
                Box(
                    modifier = Modifier
                        .size(ChipDefaults.LargeIconSize)
                        .clip(CircleShape)
                        .background(PlaceholderColors.element())
                )
            }
        } else null,
        label = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PlaceholderColors.element())
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = ChipDefaults.secondaryChipColors(),
        contentPadding = if (showIcon) {
            PaddingValues(
                start = 10.dp, 
                end = 12.dp,
                top = 0.dp,
                bottom = 0.dp,
            )
        } else {
            PaddingValues(
                start = 12.dp, 
                end = 12.dp,
                top = 0.dp,
                bottom = 0.dp,
            )
        }
    )
}

@Composable
fun PlaceholderAlbumItem(showArtist: Boolean = true) {
    Chip(
        onClick = { },
        enabled = false,
        icon = {
            Box(
                modifier = Modifier
                    .size(ChipDefaults.LargeIconSize)
                    .clip(CircleShape)
                    .background(PlaceholderColors.element())
            )
        },
        label = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PlaceholderColors.element())
            )
        },
        secondaryLabel = if (showArtist) {
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .padding(top = 4.dp)
                        .background(PlaceholderColors.element())
                )
            }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = ChipDefaults.secondaryChipColors(),
        contentPadding = PaddingValues(
            start = 10.dp, 
            end = 12.dp,
            top = 0.dp,
            bottom = 0.dp,
        )
    )
}

@Composable
private fun ArtistAvatar(thumbnails: List<android.graphics.Bitmap>? = null) {
    Box(
        modifier = Modifier
            .width(40.dp)
            .height(ChipDefaults.LargeIconSize)
    ) {
        listOf(2, 1, 0).zip(listOf(8.dp, 4.dp, 0.dp)).forEach { (idx, padding) ->
            ThumbnailSlot(
                thumbnail = thumbnails?.getOrNull(idx),
                startPadding = padding,
                backgroundColor = PlaceholderColors.element()
            )
        }
    }
}

@Composable
private fun ThumbnailSlot(
    thumbnail: android.graphics.Bitmap?,
    startPadding: androidx.compose.ui.unit.Dp,
    backgroundColor: Color
) {
    val modifier = Modifier
        .padding(start = startPadding)
        .size(ChipDefaults.LargeIconSize)
        .border(1.dp, MaterialTheme.colors.surface, CircleShape)
        .padding(1.dp)
        .clip(CircleShape)

    if (thumbnail != null) {
        Image(
            bitmap = thumbnail.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(backgroundColor)
        )
    }
}

@Composable
fun PlaceholderArtistItem() {
    Chip(
        onClick = { },
        enabled = false,
        icon = { ArtistAvatar() },
        label = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PlaceholderColors.element())
            )
        },
        secondaryLabel = {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(PlaceholderColors.element())
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = ChipDefaults.secondaryChipColors(),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 12.dp,
            top = 0.dp,
            bottom = 0.dp,
        )
    )
}

@Composable
fun TrackItem(
    track: Track,
    album: Album? = null,
    showNumber: Boolean = false,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        icon = {
            if(showNumber) {
                Column(modifier = Modifier.padding(end = 2.dp)) {
                    val style = TextStyle(
                        fontSize = 2.em,
                        lineHeight = 0.em,
                        fontWeight = FontWeight.Black,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.65f),
                    )
                    if(track.discNumber > 1) {
                        Text(text = "%02d".format(track.discNumber), style = style)
                    }
                    Text(text = "%02d".format(track.trackNumber), style = style)
                }
            } else if(album?.thumbnail != null) {
                Image(
                    bitmap = album.thumbnail.asImageBitmap(),
                    contentDescription = "Cover art for ${track.title}",
                    modifier = Modifier.size(ChipDefaults.LargeIconSize).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(ChipDefaults.LargeIconSize)
                        .clip(CircleShape)
                        .background(PlaceholderColors.element())
                )
            }
        },
        label = {
            Text(
                text = track.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 2.em,
                lineHeight = 1.25.em,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = ChipDefaults.secondaryChipColors(),
        contentPadding = if (!showNumber) {
            PaddingValues(
                start = 10.dp,
                end = 12.dp,
                top = 0.dp,
                bottom = 0.dp,
            )
        } else {
            PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 0.dp,
                bottom = 0.dp,
            )
        }
    )
}

@Composable
fun AlbumItem(
    album: Album,
    showArtist: Boolean = true,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        icon = {
            album.thumbnail?.let { thumbnail ->
                Image(
                    bitmap = thumbnail.asImageBitmap(),
                    contentDescription = "Cover art for ${album.name}",
                    modifier = Modifier
                        .size(ChipDefaults.LargeIconSize)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        },
        label = {
            Text(
                text = album.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = if (showArtist) {
            {
                Text(
                    text = album.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = ChipDefaults.secondaryChipColors(),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 12.dp,
            top = 0.dp,
            bottom = 0.dp,
        )
    )
}

@Composable
fun ArtistItem(
    artist: Artist,
    onClick: () -> Unit
) {
    Chip(
        onClick = onClick,
        icon = { ArtistAvatar(thumbnails = artist.albumThumbnails) },
        label = {
            Text(
                text = artist.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        secondaryLabel = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_album),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = artist.albumCount.toString(),
                    color = MaterialTheme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_track),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colors.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = artist.trackCount.toString(),
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = ChipDefaults.secondaryChipColors(),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 12.dp,
            top = 0.0.dp,
            bottom = 0.0.dp,
        )
    )
}
