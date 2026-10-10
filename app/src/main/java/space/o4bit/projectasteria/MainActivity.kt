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
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {  }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (resources.configuration.smallestScreenWidthDp < 600) {
            requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
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
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
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