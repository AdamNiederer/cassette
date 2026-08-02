package com.example.cassette.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.cassette.data.types.Album
import com.example.cassette.data.types.AlbumPalette
import com.example.cassette.utils.blurEdges
import java.io.ByteArrayOutputStream

@Entity(
    tableName = "albums",
    primaryKeys = ["name", "artist"],
    indices = [Index(value = ["artist"])]
)
data class AlbumEntity(
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "artist")
    val artist: String,
    @ColumnInfo(name = "thumbnail")
    val thumbnail: ByteArray?,
    @ColumnInfo(name = "vibrant") val vibrant: Int? = null,
    @ColumnInfo(name = "dark_vibrant") val darkVibrant: Int? = null,
    @ColumnInfo(name = "light_vibrant") val lightVibrant: Int? = null,
    @ColumnInfo(name = "muted") val muted: Int? = null,
    @ColumnInfo(name = "dark_muted") val darkMuted: Int? = null,
    @ColumnInfo(name = "light_muted") val lightMuted: Int? = null,
    @ColumnInfo(name = "dominant") val dominant: Int? = null,
    @ColumnInfo(name = "album_gain") val albumGain: Float? = null,
    @ColumnInfo(name = "album_peak") val albumPeak: Float? = null,
) 

fun Album.toEntity(): AlbumEntity {
    val thumbnailBytes = thumbnail?.let { bitmap ->
        val blurred = ByteArrayOutputStream().use {
            val blurred = blurEdges(bitmap, 2)
            blurred.compress(Bitmap.CompressFormat.WEBP_LOSSY, 20, it)
            it.toByteArray()
        }
        val unblurred = ByteArrayOutputStream().use {
            thumbnail.compress(Bitmap.CompressFormat.WEBP_LOSSY, 20, it)
            it.toByteArray()
        }
        if(blurred.size < unblurred.size) blurred else unblurred
    }

    return AlbumEntity(
        name = name,
        artist = artist,
        thumbnail = thumbnailBytes,
        vibrant = palette?.vibrant,
        darkVibrant = palette?.darkVibrant,
        lightVibrant = palette?.lightVibrant,
        muted = palette?.muted,
        darkMuted = palette?.darkMuted,
        lightMuted = palette?.lightMuted,
        dominant = palette?.dominant,
        albumGain = albumGain,
        albumPeak = albumPeak,
    )
}

fun AlbumEntity.toAlbum(): Album {
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    val thumbnailBitmap = thumbnail?.let { bytes ->
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }
    
    return Album(
        name = name,
        artist = artist,
        thumbnail = thumbnailBitmap,
        palette = AlbumPalette(
            vibrant = vibrant,
            darkVibrant = darkVibrant,
            lightVibrant = lightVibrant,
            muted = muted,
            darkMuted = darkMuted,
            lightMuted = lightMuted,
            dominant = dominant
        ),
        albumGain = albumGain,
        albumPeak = albumPeak,
    )
}
