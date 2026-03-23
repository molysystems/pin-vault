package com.molysystems.pinvault.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.molysystems.pinvault.PinVaultApp
import com.molysystems.pinvault.R
import com.molysystems.pinvault.data.repository.CredentialRepository
import com.molysystems.pinvault.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that polls UsageStatsManager every 1.5 seconds to detect
 * which app is in the foreground. When a linked app is detected, it triggers
 * OverlayService to show the credentials overlay.
 */
@AndroidEntryPoint
class AppDetectorService : Service() {

    @Inject
    lateinit var repository: CredentialRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var lastForegroundPackage: String? = null

    companion object {
        const val NOTIFICATION_ID = 2001
        private const val POLL_INTERVAL_MS = 1500L

        fun buildStartIntent(context: android.content.Context): Intent =
            Intent(context, AppDetectorService::class.java)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
        // Hide overlay when detector stops
        startService(OverlayService.buildHideIntent(this))
    }

    private fun startPolling() {
        pollingJob = serviceScope.launch {
            val usageStatsManager =
                getSystemService(USAGE_STATS_SERVICE) as? UsageStatsManager ?: return@launch
            val linkedPackages = repository.getAllPackageNames().toMutableSet()

            while (isActive) {
                // Refresh linked package list periodically
                try {
                    linkedPackages.clear()
                    linkedPackages.addAll(repository.getAllPackageNames())

                    val foregroundPackage = getForegroundApp(usageStatsManager)

                    if (foregroundPackage != lastForegroundPackage) {
                        lastForegroundPackage = foregroundPackage

                        if (foregroundPackage != null && foregroundPackage in linkedPackages) {
                            // Linked app is in foreground → show overlay
                            val showIntent = OverlayService.buildShowIntent(this@AppDetectorService, foregroundPackage)
                            startService(showIntent)
                        } else {
                            // Different or no app → hide overlay
                            startService(OverlayService.buildHideIntent(this@AppDetectorService))
                        }
                    }
                } catch (e: Exception) {
                    // Don't crash the polling loop on any error
                }

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun getForegroundApp(usageStatsManager: UsageStatsManager): String? {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 10_000L,
            now
        )
        return stats
            ?.filter { it.lastTimeUsed > 0 }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, PinVaultApp.CHANNEL_DETECTOR)
            .setContentTitle("PinVault")
            .setContentText("Monitoring for linked apps…")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
