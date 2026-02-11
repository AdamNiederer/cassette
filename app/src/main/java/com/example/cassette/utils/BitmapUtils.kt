package com.example.cassette.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withClip

fun blurEdges(source: Bitmap, borderPx: Int = 2): Bitmap {
    val output = createBitmap(source.width, source.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val blurred = source.scale((source.width / 8).coerceAtLeast(8), (source.height / 8).coerceAtLeast(8), true)
    val destRect = Rect(0, 0, source.width, source.height)
    canvas.drawBitmap(blurred, null, destRect, paint)
    blurred.recycle()

    val radius = (Math.min(source.width, source.height) / 2f)
    val path = Path().apply {
        addCircle(source.width / 2f, source.height / 2f, radius - borderPx, Path.Direction.CW)
    }

    canvas.withClip(path) {
        drawBitmap(source, 0f, 0f, paint)
    }

    return output
}

fun bakeArtistThumbnail(
    thumbnails: List<Bitmap>,
    iconSize: Int,
    paddingPx: Int,
    borderPx: Int,
    surfaceColor: Int,
    placeholderColor: Int
): Bitmap {
    val width = iconSize + (paddingPx * 2)
    val height = iconSize
    val output = createBitmap(width, height, Bitmap.Config.RGB_565)
    val canvas = Canvas(output)
    canvas.drawColor(surfaceColor)
    
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = placeholderColor
        style = Paint.Style.FILL
    }

    val offsets = listOf(0, paddingPx, paddingPx * 2)
    
    for (i in 2 downTo 0) {
        val thumb = thumbnails.getOrNull(i)
        val x = offsets[i]
        val centerX = (x + iconSize / 2f)
        val centerY = iconSize / 2f
        val radius = iconSize / 2f

        if (thumb != null) {
            val scaledThumb = if (thumb.width != iconSize || thumb.height != iconSize) {
                thumb.scale(iconSize, iconSize)
            } else {
                thumb
            }
            val processedThumb = blurEdges(scaledThumb, borderPx)
            
            val path = Path().apply {
                addCircle(centerX, centerY, radius, Path.Direction.CW)
            }
            canvas.withClip(path) {
                val destRect = Rect(x, 0, x + iconSize, iconSize)
                drawBitmap(processedThumb, null, destRect, paint)
            }
            processedThumb.recycle()
            if (scaledThumb != thumb) scaledThumb.recycle()
        } else {
            canvas.drawCircle(centerX, centerY, radius, placeholderPaint)
        }
    }
    return output
}
