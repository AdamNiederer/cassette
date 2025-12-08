package com.example.cassette.data.db

import android.graphics.Bitmap
import android.graphics.BitmapFactory

data class ArtistSummary(
    val name: String,
    val albumCount: Long,
    val trackCount: Long,
    val thumbnail1: ByteArray?,
    val thumbnail2: ByteArray?,
    val thumbnail3: ByteArray?
)

fun ArtistSummary.toArtist(): com.example.cassette.data.types.Artist {
    val options = BitmapFactory.Options().apply {
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    val thumbnails = listOfNotNull(thumbnail1, thumbnail2, thumbnail3).map { bytes ->
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    return com.example.cassette.data.types.Artist(
        name = name,
        albumCount = albumCount.toInt(),
        trackCount = trackCount.toInt(),
        albumThumbnails = thumbnails
    )
}
