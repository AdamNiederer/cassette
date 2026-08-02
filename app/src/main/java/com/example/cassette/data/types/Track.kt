package com.example.cassette.data.types

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import android.util.Log
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

import com.example.cassette.data.db.TrackEntity

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val uri: String,
    val discNumber: Int = 0,
    val trackNumber: Int = 0,
    val lyrics: String? = null,
    val trackGain: Float? = null,
    val trackPeak: Float? = null,
)

public fun Track.toEntity(): TrackEntity {
    return TrackEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = uri,
        discNumber = discNumber,
        trackNumber = trackNumber,
        lyrics = lyrics,
        trackGain = trackGain,
        trackPeak = trackPeak,
    )
}

public fun TrackEntity.toTrack(): Track {
    return Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = uri,
        discNumber = discNumber,
        trackNumber = trackNumber,
        lyrics = lyrics,
        trackGain = trackGain,
        trackPeak = trackPeak,
    )
}
