package com.example.cassette.data.types

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AlbumPalette(
    val vibrant: Int? = null,
    val darkVibrant: Int? = null,
    val lightVibrant: Int? = null,
    val muted: Int? = null,
    val darkMuted: Int? = null,
    val lightMuted: Int? = null,
    val dominant: Int? = null
) : Parcelable
