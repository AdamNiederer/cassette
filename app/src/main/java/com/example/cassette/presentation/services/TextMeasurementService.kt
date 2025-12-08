package com.example.cassette.presentation.services

import android.util.Log
import android.util.LruCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

object TextMeasurementService {
    private const val TAG = "TextMeasurementService"
    private const val CACHE_SIZE = 32

    private val wordWidthCache = LruCache<String, Float>(CACHE_SIZE)
    private val fontMetricsCache = ConcurrentHashMap<String, FontMetrics>()

    private val totalPrecomputeTimeNs = AtomicLong(0)
    private val totalFastPathTimeNs = AtomicLong(0)
    private val totalMeasureTimeNs = AtomicLong(0)
    private val cacheHits = AtomicLong(0)
    private val fastPathHits = AtomicLong(0)    
    private val cacheMisses = AtomicLong(0)

    private val CHINESE_UNICODE_RANGES = arrayOf(
        0x4E00..0x9FFF,
        0x3400..0x4DBF,
        0x20000..0x2A6DF,
        0x2A700..0x2B73F,
        0x2B740..0x2B81F,
        0x2B820..0x2CEAF,
        0x2CEB0..0x2EBEF,
        0xF900..0xFAFF,
        0x2F800..0x2FA1F
    )

    private val KANA_RANGE = 0x3040..0x30FF

    data class FontMetrics(
        val asciiWidths: FloatArray,
        val hanziWidth: Float,
        val kanaWidth: Float,
        val kerning: Float
    )

    data class LayoutLine(
        val text: String,
        val offset: Offset,
        val size: Size,
        val ellipsize: Boolean = false
    )

    data class TextLayoutResult(
        val lines: List<LayoutLine>,
        val totalHeight: Dp,
        val totalWidth: Dp
    )

    fun layoutText(
        text: String,
        lineWidths: List<Dp>,
        style: TextStyle,
        density: Density,
        textMeasurer: TextMeasurer,
        horizontalAlignment: TextAlign = TextAlign.Center
    ): TextLayoutResult {
        ensureFontMetrics(style, density, textMeasurer)
        
        val wrapAsCjk = !text.contains(" ") && text.all { isHanzi(it) || isKana(it) }

        val words: MutableList<String> = if (wrapAsCjk) {
            text.toCharArray().map { it.toString() }.toMutableList()
        } else {
            text.split(" ").filter { it.isNotEmpty() }.toMutableList()
        }

        val lines = mutableListOf<LayoutLine>()
        val charHeight = textMeasurer.measure("H", style).size.height.toFloat()
        val spaceWidth = if (wrapAsCjk) 0f else getWordWidth(" ", style, density, textMeasurer)
        
        var currentY = 0f
        val maxLineWidthPx = with(density) { lineWidths.maxOrNull()?.toPx() ?: 0f }
        
        for (requestedWidthDp in lineWidths) {
            if (words.isEmpty()) break

            val requestedWidthPx = with(density) { requestedWidthDp.toPx() }
            var currentLineWidth = 0f
            var wordsInLine = 0
            val lineBuilder = StringBuilder()

            for (word in words) {
                val wordWidth = getWordWidth(word, style, density, textMeasurer)
                val spacing = if (wordsInLine > 0) spaceWidth else 0f
                val newLineWidth = currentLineWidth + spacing + wordWidth
                Log.i(TAG, "Word=$word, Width=$wordWidth, LineWidth=$newLineWidth, RequestedWidth=$requestedWidthPx")

                if (newLineWidth <= requestedWidthPx) {
                    currentLineWidth = newLineWidth
                    if (wordsInLine > 0 && !wrapAsCjk) lineBuilder.append(" ")
                    lineBuilder.append(word)
                    wordsInLine++
                } else {
                    break
                }
            }

            val x = if (horizontalAlignment == TextAlign.Center) (maxLineWidthPx - requestedWidthPx) / 2 else 0f
            val oneWord = wordsInLine == 0 && words.isNotEmpty()
            lines.add(LayoutLine(
                text = if(oneWord) { words.removeAt(0) } else { lineBuilder.toString() },
                offset = Offset(x, currentY),
                size = Size(requestedWidthPx, charHeight),
                ellipsize = oneWord,
            ))
            repeat(wordsInLine) { words.removeAt(0) }

            currentY += charHeight
        }
        
        logPerformance()

        Log.i(TAG, "$lines")

        return TextLayoutResult(
            lines = lines,
            totalHeight = with(density) { (charHeight * lineWidths.size).toDp() },
            totalWidth = with(density) { maxLineWidthPx.toDp() }
        )
    }

    private fun ensureFontMetrics(style: TextStyle, density: Density, textMeasurer: TextMeasurer) {
        val key = "${style.hashCode()}_${density.density}"
        if (fontMetricsCache.containsKey(key)) return

        val start = System.nanoTime()

        val asciiWidths = FloatArray(95)
        for (c in 0x20..0x7E) {
            val s = c.toChar().toString()
            asciiWidths[c - 0x20] = textMeasurer.measure(AnnotatedString(s), style).size.width.toFloat()
        }

        val hanziWidth = textMeasurer.measure(AnnotatedString("我"), style).size.width.toFloat()
        val kanaWidth = textMeasurer.measure(AnnotatedString("あ"), style).size.width.toFloat()

        val widthAB = textMeasurer.measure(AnnotatedString("AB"), style).size.width.toFloat()
        val kerning = widthAB - asciiWidths['A'.code - 0x20] - asciiWidths['B'.code - 0x20]

        fontMetricsCache[key] = FontMetrics(asciiWidths, hanziWidth, kanaWidth, kerning)
        totalPrecomputeTimeNs.addAndGet(System.nanoTime() - start)
        Log.i(TAG, "Stats: Precompute=${totalPrecomputeTimeNs.get() / 1000}us")
    }

    private fun getWordWidth(
        word: String, 
        style: TextStyle, 
        density: Density, 
        textMeasurer: TextMeasurer
    ): Float {
        val key = "${style.hashCode()}_${density.density}"
        val metrics = fontMetricsCache[key] 
            ?: run {
                ensureFontMetrics(style, density, textMeasurer)
                fontMetricsCache[key]!!
            }

        var fastPathWidth = 0f;
        if (canUseFastPath(word)) {
            val start = System.nanoTime()
            fastPathWidth = calculateFastWidth(word, metrics)
            totalFastPathTimeNs.addAndGet(System.nanoTime() - start)
            fastPathHits.incrementAndGet()
            return fastPathWidth
        }

        val cacheKey = "${word}_$key"
        
        synchronized(wordWidthCache) {
            val cached = wordWidthCache.get(cacheKey)
            if (cached != null) {
                cacheHits.incrementAndGet()
                return cached
            }
        }

        val start = System.nanoTime()
        val width = textMeasurer.measure(
            text = AnnotatedString(word),
            style = style
        ).size.width.toFloat()
        
        totalMeasureTimeNs.addAndGet(System.nanoTime() - start)
        cacheMisses.incrementAndGet()

        synchronized(wordWidthCache) {
            wordWidthCache.put(cacheKey, width)
        }

        // if(fastPathWidth - 2f < width && width < fastPathWidth + 2f) {
        //     Log.i(TAG, "Word=${word} Accurate=${width} Fast=${fastPathWidth}")
        // } else {
        //     Log.w(TAG, "Word=${word} Accurate=${width} Fast=${fastPathWidth}")
        // }
        
        return width
    }

    private fun canUseFastPath(word: String): Boolean {
        for (i in 0 until word.length) {
            val c = word[i]
            val code = c.code
            val isAscii = code in 0x20..0x7E
            if (!isAscii && !isHanzi(c) && !isKana(c)) {
                return false
            }
        }
        return true
    }

    private fun calculateFastWidth(word: String, metrics: FontMetrics): Float {
        var width = 0f
        for (i in 0 until word.length) {
            val chr = word[i]
            val charWidth = when {
                chr.code in 0x20..0x7E -> metrics.asciiWidths[chr.code - 0x20]
                isHanzi(chr) -> metrics.hanziWidth
                isKana(chr) -> metrics.kanaWidth
                else -> 0f
            }
            width += charWidth
        }
        if (word.length > 1) {
            width += (word.length - 1) * metrics.kerning
        }
        return width
    }

    private fun isHanzi(chr: Char): Boolean {
        return CHINESE_UNICODE_RANGES.any { chr.code in it }
    }

    private fun isKana(chr: Char): Boolean {
        return chr.code in KANA_RANGE
    }

    private fun logPerformance() {
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        val fast = fastPathHits.get()
        val fastTime = totalFastPathTimeNs.get()
        val avgTime = if (misses > 0) totalMeasureTimeNs.get() / misses else 0
        Log.i(TAG, "Stats: Hits(Fast)=$fast, Time(Fast)=${fastTime / 1000}us, Hits(Cache)=$hits, Misses=$misses, Size=${wordWidthCache.size()}, MissTime=${avgTime/1000}us")
    }
}
