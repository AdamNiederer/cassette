package com.example.cassette.data.types

data class PlayerState(
    val isPlaying: Boolean,
    val playWhenReady: Boolean,
    val isPrepared: Boolean,
    val duration: Long,
    val currentPosition: Long
)
