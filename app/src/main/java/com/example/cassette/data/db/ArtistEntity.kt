package com.example.cassette.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.cassette.data.types.Artist
import java.io.ByteArrayOutputStream

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey
    val name: String,
    @ColumnInfo(name = "album_count")
    val albumCount: Int,
    @ColumnInfo(name = "track_count")
    val trackCount: Int,
    @ColumnInfo(name = "thumbnail")
    val thumbnail: ByteArray?
)

fun ArtistEntity.toArtist(): Artist {
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    val thumbnailBitmap = thumbnail?.let { bytes ->
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    return Artist(
        name = name,
        albumCount = albumCount,
        trackCount = trackCount,
        thumbnail = thumbnailBitmap
    )
}
