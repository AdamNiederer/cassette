package com.example.cassette.data.types

data class Playlist(
    val id: Long,
    val name: String,
    val trackIds: List<Long>
)