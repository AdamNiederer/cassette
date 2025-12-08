package com.example.cassette.data.types

data class TrackQueueItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val uri: String
)
