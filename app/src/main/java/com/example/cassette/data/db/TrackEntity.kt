package com.example.cassette.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tracks",
    indices = [Index(value = ["artist", "album"]), Index(value = ["title"])]
)
data class TrackEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "artist")
    val artist: String,
    @ColumnInfo(name = "album")
    val album: String,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "disc_number", defaultValue = "0")
    val discNumber: Int,
    @ColumnInfo(name = "track_number", defaultValue = "0")
    val trackNumber: Int,
    @ColumnInfo(name = "lyrics")
    val lyrics: String? = null,
    @ColumnInfo(name = "track_gain")
    val trackGain: Float? = null,
    @ColumnInfo(name = "track_peak")
    val trackPeak: Float? = null,
)
