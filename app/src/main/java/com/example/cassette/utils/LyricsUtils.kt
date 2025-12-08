package com.example.cassette.utils

data class LyricLine(val timestampMs: Long, val text: String)

fun parseLrc(
    lrc: String?,
    durationMs: Long,
    lyricsOffsetMs: Long = 500L,
    blankLineIntervalMs: Long = 12000L
): List<LyricLine>? {
    if (lrc == null) return null
    val timestampRegex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)\\]")
    val initialLines = lrc.lines().mapNotNull { line ->
        val match = timestampRegex.find(line)
        if (match != null) {
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val msPart = match.groupValues[3]
            val ms = when (msPart.length) {
                1 -> msPart.toLong() * 100
                2 -> msPart.toLong() * 10
                else -> msPart.take(3).toLong()
            }
            val timestampMs = ((minutes * 60 + seconds) * 1000 + ms + lyricsOffsetMs).coerceAtLeast(0)
            val text = line.replace(timestampRegex, "").trim()
            LyricLine(timestampMs, text)
        } else null
    }.sortedBy { it.timestampMs }

    if (initialLines.isEmpty()) return null

    val result = mutableListOf<LyricLine>()
    for (i in initialLines.indices) {
        val current = initialLines[i]
        result.add(current)
        
        if (current.text.isNotBlank() && i + 1 < initialLines.size) {
            val blankTime = current.timestampMs + blankLineIntervalMs
            val nextTime = initialLines[i + 1].timestampMs
            val nextText = initialLines[i + 1].text

            val nextTextNotBlank = nextText != ""
            val noNextWithinInterval = nextTime > blankTime
            val isLastPair = i == initialLines.size - 1
            val isNearEnd = current.timestampMs + blankLineIntervalMs >= durationMs
            val notLastPairNearEnd = !(isLastPair && isNearEnd)
            
            if (noNextWithinInterval && notLastPairNearEnd && nextTextNotBlank && blankTime < durationMs) {
                result.add(LyricLine(blankTime, ""))
            }
        }
    }
    return result.sortedBy { it.timestampMs }
}
