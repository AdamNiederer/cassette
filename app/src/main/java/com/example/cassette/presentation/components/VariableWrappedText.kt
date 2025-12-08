package com.example.cassette.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import android.util.Log
import com.example.cassette.presentation.services.TextMeasurementService

@Composable
fun VariableWrappedText(
    text: String,
    lineWidths: List<Dp>,
    style: TextStyle,
    modifier: Modifier = Modifier,
    horizontalAlignment: TextAlign = TextAlign.Center,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val layoutResult = remember(text, lineWidths, style, density) {
        TextMeasurementService.layoutText(
            text = text,
            lineWidths = lineWidths,
            style = style,
            density = density,
            textMeasurer = textMeasurer,
            horizontalAlignment = horizontalAlignment
        )
    }

    Canvas(modifier = Modifier.width(layoutResult.totalWidth).height(layoutResult.totalHeight)) {
        for (line in layoutResult.lines) {
            drawText(
                textMeasurer = textMeasurer,
                text = line.text,
                style = style,
                topLeft = line.offset,
                size = line.size,
                overflow = if(line.ellipsize) { TextOverflow.Ellipsis } else { TextOverflow.Visible },
                maxLines = 1,
            )
        }
    }
}
