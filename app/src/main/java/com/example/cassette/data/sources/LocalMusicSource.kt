package com.example.cassette.data.sources

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle;
import android.provider.MediaStore
import android.util.Log
import android.app.AppOpsManager
import android.media.MediaScannerConnection
import com.example.cassette.data.types.MusicDiscoveryResult
import com.example.cassette.data.types.Track
import com.example.cassette.data.types.Album
import com.google.common.collect.Iterators
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CompletableFuture
import java.util.Optional
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.enums.enumEntries
import kotlin.collections.getOrNull
import kotlin.Result
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point;
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material.ChipDefaults
import android.util.TypedValue
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagField
import org.jaudiotagger.tag.id3.AbstractTagItem
import androidx.palette.graphics.Palette
import com.example.cassette.data.types.AlbumPalette

import com.example.cassette.data.types.DiscoveryPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap

// adb shell appops set com.example.cassette READ_EXTERNAL_STORAGE allow
// adb shell appops set com.example.cassette READ_MEDIA_AUDIO allow
// adb shell pm grant com.example.cassette android.permission.READ_MEDIA_AUDIO
// adb shell pm grant com.example.cassette android.permission.READ_EXTERNAL_STORAGE
// adb shell content call --method scan_volume --uri content://media --arg external_primary

sealed class DiscoveryState {
    data object Idle : DiscoveryState()
    data class Scanning(val current: Int, val total: Int) : DiscoveryState()
    data class Rebuilding(val current: Int, val total: Int) : DiscoveryState()
}

fun Context.dpToPx(dp: Dp): Int {
    val metrics = resources.displayMetrics
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.value.toFloat(), metrics).toInt()
}

private fun Tag.getString(key: FieldKey): String? = getFirst(key).takeIf { it.isNotBlank() }
private fun Tag.getString(key: String): String? = getFirst(key).takeIf { it.isNotBlank() }

private data class MetadataResult(
    val id: Long,
    val title: String,
    val artist: String,
    val albumName: String,
    val duration: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val lyrics: String?,
    val trackGain: Float?,
    val trackPeak: Float?,
    val albumGain: Float?,
    val albumPeak: Float?
)

@OptIn(ExperimentalCoroutinesApi::class)
class LocalMusicSource @Inject constructor(
    private val context: Context
) {
    public val progress = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    public val discoveryPreviews = MutableSharedFlow<DiscoveryPreview>()

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        val widthRatio = ceil(width.toDouble() / reqWidth).toInt()
        val heightRatio = ceil(height.toDouble() / reqHeight).toInt()
        return maxOf(widthRatio, heightRatio).coerceAtLeast(1)
    }

    suspend fun scanMusicFolder() = withContext(Dispatchers.IO) {
        val path = Paths.get("/storage/emulated/0/Music")
        progress.update { DiscoveryState.Scanning(0, 1) }

        val totalCount = Files.walk(path).use { it.filter { p -> p.isRegularFile() }.count().toInt() }
        if (totalCount <= 0) {
            progress.update { DiscoveryState.Idle }
            return@withContext
        }

        progress.update { DiscoveryState.Scanning(0, totalCount) }

        val counter = AtomicInteger(0)
        suspendCancellableCoroutine<Unit> { continuation ->
            Files.walk(path).use {
                val pathIterator = it.filter { p -> p.isRegularFile() }.map { p -> p.pathString }.iterator()
                val batches = Iterators.partition(pathIterator, 100)

                while (batches.hasNext()) {
                    val batch = batches.next()

                    MediaScannerConnection.scanFile(context, batch.toTypedArray(), null) { _, _ ->
                        val current = counter.incrementAndGet()

                        if (current % 50 == 0 || current == totalCount) {
                            progress.update { DiscoveryState.Scanning(current, totalCount) }
                        }

                        if (current == totalCount && continuation.isActive) {
                            continuation.resume(Unit) { _, _, _ -> } 
                        }
                    }
                }
            }

            continuation.invokeOnCancellation {
                Log.i("LocalMusicSource", "Scan cancelled by user")
            }
        }

        progress.update { DiscoveryState.Idle }
    }

    private fun isMediaStoreEmpty(): Boolean {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val queryUri = uri.buildUpon().appendQueryParameter("limit", "1").build()
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        return context.contentResolver.query(queryUri, projection, selection, null, null)?.use { cursor ->
            !cursor.moveToFirst()
        } ?: true
    }

    fun getCurrentGeneration(): Long? {
        return try {
            MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } catch (e: Exception) {
            Log.e("LocalMusicSource", "Error getting MediaStore generation", e)
            null
        }
    }

    private fun queryMediaStore(): List<Pair<Long, String>> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1"
        val rawTracks = mutableListOf<Pair<Long, String>>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            null
        )?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (it.moveToNext()) {
                rawTracks.add(it.getLong(idColumn) to it.getString(dataColumn))
            }
        }
        
        return rawTracks
    }

    private suspend fun processTracksMetadata(rawTracks: List<Pair<Long, String>>): List<MetadataResult> = coroutineScope {
        val counter = AtomicInteger(0)
        val total = rawTracks.size
        val seenAlbums = ConcurrentHashMap.newKeySet<String>()
        val featRegex = Regex(" feat\\..*")

        rawTracks.map { (id, path) ->
            async {
                val file = File(path)
                var title = file.nameWithoutExtension
                var artist = "Unknown Artist"
                var albumName = "Unknown Album"
                var duration = 0L
                var trackNumber = 0
                var discNumber = 0
                var lyrics: String? = null
                var trackGain: Float? = null
                var trackPeak: Float? = null
                var albumGain: Float? = null
                var albumPeak: Float? = null

                try {
                    if (file.exists()) {
                        val audioFile = AudioFileIO.read(file)
                        audioFile.tag?.let { tag ->
                            tag.getString(FieldKey.TITLE)?.let { title = it }
                            tag.getString(FieldKey.ARTIST)?.let { artist = it.replace(featRegex, "") }
                            tag.getString(FieldKey.ALBUM)?.let { albumName = it }
                            tag.getString(FieldKey.TRACK)?.let { 
                                it.split("/")[0].toIntOrNull()?.let { num -> trackNumber = num }
                            }
                            tag.getString(FieldKey.DISC_NO)?.let { 
                                it.split("/")[0].toIntOrNull()?.let { num -> discNumber = num }
                            }
                            lyrics = tag.getString(FieldKey.LYRICS) ?: 
                                arrayOf("LYRICS", "lyrics", "UNSYNCEDLYRICS", "unsyncedlyrics")
                                    .firstNotNullOfOrNull { tag.getString(it) }

                            tag.getString("REPLAYGAIN_TRACK_GAIN")?.replace(" dB", "")?.toFloatOrNull()?.let { trackGain = it }
                            tag.getString("REPLAYGAIN_TRACK_PEAK")?.toFloatOrNull()?.let { trackPeak = it }
                            tag.getString("REPLAYGAIN_ALBUM_GAIN")?.replace(" dB", "")?.toFloatOrNull()?.let { albumGain = it }
                            tag.getString("REPLAYGAIN_ALBUM_PEAK")?.toFloatOrNull()?.let { albumPeak = it }
                        }
                        audioFile.audioHeader?.let {
                            duration = (it.trackLength * 1000).toLong()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LocalMusicSource", "Error reading metadata with jaudiotagger for $path", e)
                }

                val albumId = "${artist}|${albumName}"
                if (seenAlbums.add(albumId)) {
                    // First time seeing this album, try to extract art for preview
                    try {
                        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                        val opts = Bundle().apply {
                            putParcelable("android.content.extra.SIZE", Point(384, 384))
                        }
                        val largeBitmap = context.contentResolver.openTypedAssetFile(contentUri, "image/*", opts, null)?.let {
                            val fd = it.fileDescriptor
                            val meta = BitmapFactory.Options().apply {
                                inJustDecodeBounds = true
                                BitmapFactory.decodeFileDescriptor(fd, null, this)
                            }
                            BitmapFactory.Options().run {
                                inSampleSize = calculateInSampleSize(meta.outWidth, meta.outHeight, 384, 384)
                                inPreferredConfig = Bitmap.Config.RGB_565
                                BitmapFactory.decodeFileDescriptor(fd, null, this) 
                            }
                        }

                        if (largeBitmap != null) {
                            val albumPalette = Palette.from(largeBitmap).maximumColorCount(8).generate().let { p ->
                                AlbumPalette(
                                    vibrant = p.vibrantSwatch?.rgb,
                                    darkVibrant = p.darkVibrantSwatch?.rgb,
                                    lightVibrant = p.lightVibrantSwatch?.rgb,
                                    muted = p.mutedSwatch?.rgb,
                                    darkMuted = p.darkMutedSwatch?.rgb,
                                    lightMuted = p.lightMutedSwatch?.rgb,
                                    dominant = p.dominantSwatch?.rgb
                                )
                            }
                            discoveryPreviews.emit(DiscoveryPreview(albumName, largeBitmap, albumPalette))
                        }
                    } catch (e: Exception) {
                        // Ignore preview errors
                    }
                }

                val current = counter.incrementAndGet()
                if (current % 10 == 0 || current == total) {
                    progress.update { DiscoveryState.Rebuilding(current, total) }
                }

                MetadataResult(id, title, artist, albumName, duration, trackNumber, discNumber, lyrics, trackGain, trackPeak, albumGain, albumPeak)
            }
        }.awaitAll()
    }

    private fun createLibraryFromMetadata(metadataResults: List<MetadataResult>): MusicDiscoveryResult {
        val albumsMap = mutableMapOf<String, Album>()
        val tracks = mutableListOf<Track>()

        for (it in metadataResults) {
            val albumId = "${it.artist}|${it.albumName}"
            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.id)
            
            if (!albumsMap.containsKey(albumId)) {
                val iconSize = context.dpToPx(ChipDefaults.LargeIconSize)
                val opts = Bundle().apply {
                    putParcelable("android.content.extra.SIZE", Point(iconSize, iconSize))
                }
                val thumbnail = try {
                    context.contentResolver.openTypedAssetFile(contentUri, "image/*", opts, null)?.let {
                        val meta = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                            BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, this)
                        }
                        BitmapFactory.Options().run {
                            inSampleSize = calculateInSampleSize(meta.outWidth, meta.outHeight, iconSize, iconSize)
                            inPreferredConfig = Bitmap.Config.RGB_565
                            BitmapFactory.decodeFileDescriptor(it.fileDescriptor, null, this) 
                        }
                    }
                } catch (e: Exception) { null }

                val albumPalette = thumbnail?.let {
                    val p = Palette.from(it).maximumColorCount(8).generate()
                    AlbumPalette(
                        vibrant = p.vibrantSwatch?.rgb,
                        darkVibrant = p.darkVibrantSwatch?.rgb,
                        lightVibrant = p.lightVibrantSwatch?.rgb,
                        muted = p.mutedSwatch?.rgb,
                        darkMuted = p.darkMutedSwatch?.rgb,
                        lightMuted = p.lightMutedSwatch?.rgb,
                        dominant = p.dominantSwatch?.rgb
                    )
                }

                albumsMap[albumId] = Album(
                    id = albumId,
                    name = it.albumName,
                    artist = it.artist,
                    thumbnail = thumbnail,
                    palette = albumPalette,
                    albumGain = it.albumGain,
                    albumPeak = it.albumPeak
                )
            }

            tracks.add(Track(
                id = it.id,
                title = it.title,
                artist = it.artist,
                album = it.albumName,
                albumId = albumId,
                durationMs = it.duration,
                uri = contentUri.toString(),
                discNumber = it.discNumber,
                trackNumber = it.trackNumber,
                lyrics = it.lyrics,
                trackGain = it.trackGain,
                trackPeak = it.trackPeak,
            ))
        }
        return MusicDiscoveryResult(tracks = tracks, albums = albumsMap.values.toList())
    }

    suspend fun discoverMusic(): MusicDiscoveryResult = withContext(Dispatchers.IO) {
        progress.update { DiscoveryState.Rebuilding(0, 1) }

        try {
            val rawTracks = queryMediaStore()

            if (rawTracks.isEmpty()) {
                return@withContext MusicDiscoveryResult(emptyList(), emptyList())
            }

            val metadataResults = processTracksMetadata(rawTracks)
            val result = createLibraryFromMetadata(metadataResults)

            progress.update { DiscoveryState.Idle }
            result
        } catch (e: Exception) {
            Log.e("LocalMusicSource", "Error", e)
            MusicDiscoveryResult(
                tracks = emptyList(),
                albums = emptyList(),
                error = e.message ?: "Failed to discover music"
            )
        }
    }
}
