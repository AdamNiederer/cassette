package com.example.cassette.data.types

data class MusicDiscoveryResult(
    val tracks: List<Track>,
    val albums: List<Album> = emptyList(),
    val error: String? = null,
    val skipped: Boolean = false
)
