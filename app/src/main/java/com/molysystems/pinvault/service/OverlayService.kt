package com.molysystems.pinvault.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.molysystems.pinvault.PinVaultApp
import com.molysystems.pinvault.R
import com.molysystems.pinvault.data.model.CredentialField
import com.molysystems.pinvault.data.model.AppEntry
import com.molysystems.pinvault.data.repository.CredentialRepository
import com.molysystems.pinvault.ui.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Foreground service that manages the floating credentials overlay window.
 * Receives show/hide intents from AppDetectorService.
 */
@AndroidEntryPoint
class OverlayService : Service() {

    @Inject
    lateinit var repository: CredentialRepository

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // For dragging the overlay
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    companion object {
        const val ACTION_SHOW = "com.molysystems.pinvault.action.SHOW_OVERLAY"
        const val ACTION_HIDE = "com.molysystems.pinvault.action.HIDE_OVERLAY"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val NOTIFICATION_ID = 1001

        fun buildShowIntent(context: Context, packageName: String): Intent =
            Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_PACKAGE_NAME, packageName)
            }

        fun buildHideIntent(context: Context): Intent =
            Intent(context, OverlayService::class.java).apply {
                action = ACTION_HIDE
            }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return START_NOT_STICKY
                loadAndShowOverlay(packageName)
            }
            ACTION_HIDE -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        serviceScope.cancel()
    }

    private fun loadAndShowOverlay(packageName: String) {
        serviceScope.launch {
            val app = repository.getAppByPackage(packageName) ?: return@launch
            val fields = repository.getCredentialFieldsOnce(app.id)
            if (fields.isEmpty()) return@launch

            // Update last accessed timestamp
            repository.touchApp(app.id)

            withContext(Dispatchers.Main) {
                showOverlay(app, fields)
            }
        }
    }

    private fun showOverlay(app: AppEntry, fields: List<CredentialField>) {
        hideOverlay()

        val view = LayoutInflater.from(this).inflate(R.layout.overlay_window, null)

        // App name
        view.findViewById<TextView>(R.id.appNameText).text = app.displayName

        // Populate credential rows
        val container = view.findViewById<LinearLayout>(R.id.credentialsContainer)
        fields.forEach { field ->
            val decrypted = runCatching { repository.decryptField(field) }.getOrElse { "???" }
            addCredentialRow(container, field.label, decrypted)
        }

        // Close button
        view.findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            hideOverlay()
        }

        // Window params
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }

        // Make overlay draggable
        view.setOnTouchListener { _, event ->
            val layoutParams = overlayView?.layoutParams as? WindowManager.LayoutParams
                ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (initialTouchX - event.rawX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, layoutParams)
                    true
                }
                else -> false
            }
        }

        overlayView = view
        windowManager?.addView(view, params)
    }

    private fun addCredentialRow(container: LinearLayout, label: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 6, 0, 6)
        }

        val labelView = TextView(this).apply {
            text = "$label: "
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 12f
        }

        val valueView = TextView(this).apply {
            text = value
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = Typeface.MONOSPACE
        }

        row.addView(labelView)
        row.addView(valueView)

        row.setOnClickListener {
            copyToClipboard(label, value)
        }

        container.addView(row)
    }

    private fun hideOverlay() {
        overlayView?.let {
            runCatching { windowManager?.removeView(it) }
            overlayView = null
        }
    }

    private fun copyToClipboard(label: String, value: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        Toast.makeText(this, "Copied: $label", Toast.LENGTH_SHORT).show()
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, PinVaultApp.CHANNEL_OVERLAY)
            .setContentTitle("PinVault overlay active")
            .setContentText("Tap to open PinVault")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
