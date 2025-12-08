package com.example.cassette.data.types

import android.graphics.Bitmap

data class Artist(
    val name: String,
    val albumCount: Int,
    val trackCount: Int,
    val albumThumbnails: List<Bitmap>
)
