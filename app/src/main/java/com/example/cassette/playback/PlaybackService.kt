package com.example.cassette.playback

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.google.common.collect.ImmutableList
import androidx.media3.session.CommandButton
import android.os.Bundle
import android.util.Log
import androidx.wear.tiles.TileService

import com.example.cassette.R
import com.example.cassette.presentation.PlayerActivity

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private var mediaSession: MediaSession? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateOngoingActivity()
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            updateOngoingActivity()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            updateOngoingActivity()
        }
    }

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "playback_channel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSession.Builder(this, exoPlayer)
            .setSessionActivity(createPendingIntent())
            .build()

        setMediaNotificationProvider(object : MediaNotification.Provider {
            override fun createNotification(
                mediaSession: MediaSession,
                customLayout: ImmutableList<CommandButton>,
                actionFactory: MediaNotification.ActionFactory,
                onNotificationChangedCallback: MediaNotification.Provider.Callback
            ): MediaNotification {
                return createMediaNotification(mediaSession)
            }

            override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo {
                return MediaNotification.Provider.NotificationChannelInfo(
                    CHANNEL_ID,
                    getString(R.string.playback_channel_name)
                )
            }

            override fun handleCustomCommand(session: MediaSession, action: String, extras: android.os.Bundle): Boolean = false
        })

        exoPlayer.addListener(playerListener)
    }

    private fun updateOngoingActivity() {
        Log.i("PlaybackService", "updateOngoingActivity")
        mediaSession?.let { session ->
            if (exoPlayer.isPlaying) {
                val mediaNotification = createMediaNotification(session)
                startForeground(
                    mediaNotification.notificationId,
                    mediaNotification.notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!exoPlayer.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.playback_channel_name)
        val descriptionText = getString(R.string.playback_channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createPendingIntent(): PendingIntent {
        val intent = Intent(this, PlayerActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun createMediaNotification(session: MediaSession): MediaNotification {
        val mediaMetadata = session.player.mediaMetadata
        val title = mediaMetadata.title ?: "Cassette"
        val artist = mediaMetadata.artist ?: "Unknown Artist"
        val isPlaying = session.player.isPlaying

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_music)
            .setOngoing(isPlaying)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(androidx.media3.session.MediaStyleNotificationHelper.MediaStyle(session))

        mediaMetadata.artworkData?.let { data ->
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            notificationBuilder.setLargeIcon(bitmap)
        }

        val statusText = if (isPlaying) "Playing $title" else "Paused $title"
        val ongoingActivityStatus = Status.Builder()
            .addTemplate(statusText)
            .build()

        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notificationBuilder)
            .setStaticIcon(R.drawable.ic_music)
            .setTouchIntent(createPendingIntent())
            .setStatus(ongoingActivityStatus)
            .build()
            .apply(applicationContext)

        return MediaNotification(NOTIFICATION_ID, notificationBuilder.build())
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (exoPlayer.isPlaying) {
            updateOngoingActivity()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        exoPlayer.removeListener(playerListener)
        mediaSession?.run {
            release()
            mediaSession = null
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
