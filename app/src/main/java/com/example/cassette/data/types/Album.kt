package com.example.cassette.data.types

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Album(
    val id: String, // Composite key: "$artist|$album"
    val name: String,
    val artist: String,
    val thumbnail: Bitmap?,
    val palette: AlbumPalette? = null,
    val albumGain: Float? = null,
    val albumPeak: Float? = null,
) : Parcelable
