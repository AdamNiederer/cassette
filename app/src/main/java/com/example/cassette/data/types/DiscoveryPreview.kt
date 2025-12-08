package com.example.cassette.data.types

import android.graphics.Bitmap

data class DiscoveryPreview(
    val name: String,
    val bitmap: Bitmap,
    val palette: AlbumPalette,
)
