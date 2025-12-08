package com.example.cassette.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FadedAlbumArt(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alpha: Float = 1.0f,
    blurRadius: Dp = 0.dp
) {
    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier
            .padding(36.dp)
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.radialGradient(
                        0.0f to Color.Black,
                        0.2f to Color.Black,
                        1.0f to Color.Transparent,
                        center = center,
                        radius = size.minDimension / 2,
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
            .clip(CircleShape)
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
    )
}
