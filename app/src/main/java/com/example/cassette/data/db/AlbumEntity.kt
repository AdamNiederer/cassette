package com.example.cassette.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.cassette.data.types.Album
import java.io.ByteArrayOutputStream
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey
    val id: String, // "$artist|$album"
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

fun blurEdges(source: Bitmap): Bitmap {
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val blurred = Bitmap.createScaledBitmap(source, (source.width / 8).coerceAtLeast(8), (source.height / 8).coerceAtLeast(8), true)
    val destRect = Rect(0, 0, source.width, source.height)
    canvas.drawBitmap(blurred, null, destRect, paint)
    blurred.recycle()

    val radius = (Math.min(source.width, source.height) / 2f)
    val path = Path().apply {
        addCircle(source.width / 2f, source.height / 2f, radius - 2, Path.Direction.CW)
    }

    canvas.save()
    canvas.clipPath(path)
    canvas.drawBitmap(source, 0f, 0f, paint)
    canvas.restore()

    return output
}

fun Album.toEntity(): AlbumEntity {
    val thumbnailBytes = thumbnail?.let { bitmap ->
        val blurred = ByteArrayOutputStream().use {
            val blurred = blurEdges(bitmap)
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
        id = id,
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
        id = id,
        name = name,
        artist = artist,
        thumbnail = thumbnailBitmap,
        palette = com.example.cassette.data.types.AlbumPalette(
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
