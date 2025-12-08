package com.example.cassette.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

@Composable
fun PartialCircularProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = CircularProgressIndicatorDefaults.smallStrokeWidth,
    trackSweep: Float = 180f,
    startAngle: Float = 180f
) {
    val stroke = with(LocalDensity.current) { Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round) }
    Canvas(modifier) {
        drawArc(
            color = color.copy(alpha = 0.2f),
            startAngle = startAngle,
            sweepAngle = trackSweep,
            useCenter = false,
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = startAngle,
            sweepAngle = trackSweep * progress.coerceIn(0f, 1f),
            useCenter = false,
            style = stroke
        )

        // val r = size.minDimension / 2
        // val angleDeg = startAngle + trackSweep * progress.coerceIn(0f, 1f)
        // val angleRad = angleDeg * (PI / 180f)
        // val tipX = center.x + r * cos(angleRad).toFloat()
        // val tipY = center.y + r * sin(angleRad).toFloat()

        // drawCircle(
        //     color = Color.White,
        //     radius = stroke.width / 4,
        //     center = Offset(tipX, tipY)
        // )
    }
}
