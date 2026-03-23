package com.molysystems.pinvault

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PinVaultApp : Application() {

    companion object {
        const val CHANNEL_OVERLAY = "pinvault_overlay"
        const val CHANNEL_DETECTOR = "pinvault_detector"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_OVERLAY,
                    "Credential Overlay",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows while credential overlay is active"
                    setShowBadge(false)
                }
            )

            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_DETECTOR,
                    "App Monitor",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    description = "Background app detection for overlay trigger"
                    setShowBadge(false)
                }
            )
        }
    }
}
