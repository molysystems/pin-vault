package com.molysystems.pinvault.ui.main

import android.app.AppOpsManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.molysystems.pinvault.service.AppDetectorService
import com.molysystems.pinvault.service.OverlayService
import com.molysystems.pinvault.ui.detail.AppDetailScreen
import com.molysystems.pinvault.ui.setup.AppPickerScreen
import com.molysystems.pinvault.ui.theme.PinVaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private var isAuthenticated by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PinVaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isAuthenticated) {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "main") {
                            composable("main") {
                                MainScreen(
                                    onAppClick = { appId ->
                                        navController.navigate("detail/$appId")
                                    },
                                    onAddClick = {
                                        navController.navigate("picker")
                                    }
                                )
                            }
                            composable("picker") {
                                AppPickerScreen(
                                    onAppSelected = { appId ->
                                        navController.navigate("detail/$appId") {
                                            popUpTo("picker") { inclusive = true }
                                        }
                                    },
                                    onNavigateUp = { navController.popBackStack() }
                                )
                            }
                            composable("detail/{appEntryId}") {
                                AppDetailScreen(
                                    onNavigateUp = { navController.popBackStack() }
                                )
                            }
                        }
                    } else {
                        LockScreen(onUnlockClick = { showBiometricPrompt() })
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthenticated) {
            showBiometricPrompt()
        }
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometric or device credential set up — unlock directly
            isAuthenticated = true
            onUnlocked()
            return
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock PinVault")
            .setSubtitle("Authenticate to access your credentials")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isAuthenticated = true
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled or error — stay on lock screen
                }

                override fun onAuthenticationFailed() {
                    // Wrong biometric — prompt stays open
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }

    private fun onUnlocked() {
        checkAndRequestPermissions()
        startServices()
    }

    private fun startServices() {
        startForegroundService(AppDetectorService.buildStartIntent(this))
        startForegroundService(Intent(this, OverlayService::class.java))
    }

    private fun checkAndRequestPermissions() {
        // Check SYSTEM_ALERT_WINDOW
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        // Check PACKAGE_USAGE_STATS
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
