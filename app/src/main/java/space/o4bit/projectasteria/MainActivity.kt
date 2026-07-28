package space.o4bit.projectasteria

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.data.worker.DailySpaceWorker
import space.o4bit.projectasteria.ui.AsteriaApp
import space.o4bit.projectasteria.ui.theme.ThemedApp
import space.o4bit.projectasteria.widget.WidgetClickReceiver

class MainActivity : ComponentActivity() {

    // Notification permission result — called when the OS dialog resolves.
    // We don't show a Toast here; the Settings UI already reflects the state
    // via the DataStore-backed switch, so no extra feedback is needed.
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result reflected in the DataStore-backed switch in SettingsScreen */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            val imageLoader = ImageLoader.Builder(applicationContext)
                .memoryCache {
                    MemoryCache.Builder(applicationContext)
                        .maxSizePercent(0.25)
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(cacheDir.resolve("image_cache"))
                        .maxSizePercent(0.02)
                        .build()
                }
                .respectCacheHeaders(false)
                .build()
            coil.Coil.setImageLoader(imageLoader)

            val prefs = space.o4bit.projectasteria.data.preferences.NotificationPreferencesRepository(applicationContext)
            if (prefs.dailyNotificationsEnabled.first()) {
                DailySpaceWorker.schedule(applicationContext)
            }
        }

        // Notification permission is now requested contextually from Settings when
        // the user enables daily notifications — not on cold launch. Moved here only
        // if the permission is already granted (no-op) or was previously requested.
        dismissNotificationIfRequested(intent)

        enableEdgeToEdge()
        setContent {
            ThemedApp {
                val openFullscreen = intentOpensFullscreen(intent)
                AsteriaApp(
                    openDirectlyFromNotification = openFullscreen,
                    onRequestNotificationPermission = ::requestNotificationPermission
                )
            }
        }
    }

    /**
     * Handle the case where the app is already in the foreground and a deep-link
     * or notification intent arrives. Without this, `intent` in `setContent` is
     * stale and the new intent is silently dropped.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        dismissNotificationIfRequested(intent)
    }

    override fun onPause() {
        super.onPause()
        if (intent?.getBooleanExtra(WidgetClickReceiver.EXTRA_FROM_WIDGET, false) == true) {
            @Suppress("DEPRECATION")
            overridePendingTransition(
                R.anim.widget_open_enter,
                R.anim.widget_open_exit
            )
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Called from SettingsScreen when the user turns on daily notifications.
     * Requesting at that moment gives the OS rationale dialog the right context
     * ("this app wants to notify you about space events") instead of asking cold
     * before the user has seen a single screen.
     */
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted — nothing to do.
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun dismissNotificationIfRequested(intent: Intent?) {
        if (intent?.getBooleanExtra("DISMISS_NOTIFICATION", false) == true) {
            val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)
            if (notificationId > 0) {
                val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }
        }
    }

    private fun intentOpensFullscreen(intent: Intent?): Boolean =
        intent?.getBooleanExtra("OPEN_FULLSCREEN", false) == true ||
                (intent?.data?.scheme == "asteria" && intent.data?.host == "image")
}
