package com.example.cassette.data.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.example.cassette.data.repositories.MusicRepository
import androidx.work.ForegroundInfo
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.cassette.R
import android.content.pm.ServiceInfo

@HiltWorker
class MusicScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val musicRepository: MusicRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        setForeground(createForegroundInfo())
        val result = musicRepository.performFullScan()
        return if (result.error == null) Result.success() else Result.failure()
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val id = "music_scan_channel"
        val title = "Scanning Music"
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(id, title, NotificationManager.IMPORTANCE_MIN)
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, id)
            .setContentTitle(title)
            .setTicker(title)
            .setContentText("Updating library...")
            .setSmallIcon(R.drawable.ic_refresh)
            .setOngoing(true)
            .build()

        return ForegroundInfo(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
}
