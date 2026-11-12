package space.o4bit.projectasteria.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import space.o4bit.projectasteria.AsteriaApplication
import space.o4bit.projectasteria.MainActivity
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.repository.LaunchRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class LaunchAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    updateWidget(context, appWidgetManager, id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating launch widgets", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidget(context, appWidgetManager, appWidgetId)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating launch widget on options changed", e)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        for (id in appWidgetIds) {
            editor.remove(KEY_INDEX_PREFIX + id)
            editor.remove(KEY_LAUNCH_IDS_PREFIX + id)
            editor.remove(KEY_DISPLAY_MODE_PREFIX + id)
        }
        editor.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, LaunchAppWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds.isNotEmpty()) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            if (intent.action == ACTION_REFRESH) {
                                val app = context.applicationContext as AsteriaApplication
                                try {
                                    LaunchRepository(app.database.launchDao(), app.sortingPreferences)
                                        .refreshUpcomingLaunches(limit = 15)
                                } catch (_: Exception) {
                                }
                            }
                            for (id in appWidgetIds) {
                                updateWidget(context, appWidgetManager, id)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating launch widgets in onReceive", e)
                        } finally {
                            pendingResult?.finish()
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "LaunchAppWidgetProvider"
        const val ACTION_REFRESH = "space.o4bit.projectasteria.widget.ACTION_REFRESH_LAUNCH"
        const val ACTION_PREV = "space.o4bit.projectasteria.widget.ACTION_PREV_LAUNCH"
        const val ACTION_NEXT = "space.o4bit.projectasteria.widget.ACTION_NEXT_LAUNCH"
        const val EXTRA_WIDGET_ID = "space.o4bit.projectasteria.widget.EXTRA_WIDGET_ID"
        const val EXTRA_LAUNCH_ID = "EXTRA_LAUNCH_ID"
        const val DISPLAY_MODE_MULTI = "MULTI"
        const val DISPLAY_MODE_SINGLE = "SINGLE"
        private const val PREFS_NAME = "launch_widget_preferences"
        private const val KEY_INDEX_PREFIX = "launch_index_"
        private const val KEY_LAUNCH_IDS_PREFIX = "widget_launch_ids_"
        private const val KEY_DISPLAY_MODE_PREFIX = "widget_display_mode_"
        private const val MAX_SWIPE_ITEMS = 8

        fun getWidgetLaunchIds(context: Context, appWidgetId: Int): List<String> {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return emptyList()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_LAUNCH_IDS_PREFIX + appWidgetId, null) ?: return emptyList()
            return raw.split(",").filter { it.isNotBlank() }
        }

        fun setWidgetLaunchIds(context: Context, appWidgetId: Int, launchIds: List<String>) {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val distinct = launchIds.filter { it.isNotBlank() }.distinct()
            val editor = prefs.edit()
            if (distinct.isEmpty()) {
                editor.remove(KEY_LAUNCH_IDS_PREFIX + appWidgetId)
            } else {
                editor.putString(KEY_LAUNCH_IDS_PREFIX + appWidgetId, distinct.joinToString(","))
            }
            editor.putInt(KEY_INDEX_PREFIX + appWidgetId, 0)
            editor.apply()
        }

        fun getWidgetDisplayMode(context: Context, appWidgetId: Int): String {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return DISPLAY_MODE_MULTI
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_DISPLAY_MODE_PREFIX + appWidgetId, DISPLAY_MODE_MULTI) ?: DISPLAY_MODE_MULTI
        }

        fun setWidgetDisplayMode(context: Context, appWidgetId: Int, mode: String) {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_DISPLAY_MODE_PREFIX + appWidgetId, mode)
                .putInt(KEY_INDEX_PREFIX + appWidgetId, 0)
                .apply()
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LaunchAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isNotEmpty()) {
                CoroutineScope(Dispatchers.IO).launch {
                    for (id in appWidgetIds) {
                        try {
                            updateWidget(context, appWidgetManager, id)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed updating launch widget $id", e)
                        }
                    }
                }
            }
        }

        suspend fun updateSingleWidget(context: Context, appWidgetId: Int) {
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val appWidgetManager = AppWidgetManager.getInstance(context)
            updateWidget(context, appWidgetManager, appWidgetId)
        }

        private suspend fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.launch_app_widget_layout)
            val app = context.applicationContext as AsteriaApplication
            val database = app.database
            val launchDao = database.launchDao()
            val backgroundPrefs = app.backgroundPreferences

            views.setViewVisibility(R.id.launch_widget_preview_container, View.GONE)

            val configIntent = Intent(context, LaunchWidgetConfigActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val configPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 9,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.launch_widget_btn_configure, configPendingIntent)

            val refreshIntent = Intent(context, LaunchAppWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.launch_widget_btn_refresh, refreshPendingIntent)

            var cachedLaunches = launchDao.getOldestLaunches().first()
            if (cachedLaunches.isEmpty()) {
                try {
                    LaunchRepository(launchDao, app.sortingPreferences)
                        .refreshUpcomingLaunches(limit = 15)
                    cachedLaunches = launchDao.getOldestLaunches().first()
                } catch (_: Exception) {
                }
            }

            val widgetSpecificIds = getWidgetLaunchIds(context, appWidgetId)
            val globalOrderedPinnedIds = backgroundPrefs.orderedPinnedLaunchIds.first()
            val targetIds = if (widgetSpecificIds.isNotEmpty()) widgetSpecificIds else globalOrderedPinnedIds

            val isPinnedMode: Boolean
            val launches: List<LaunchEntity> = if (targetIds.isNotEmpty()) {
                val dbLaunches = launchDao.getLaunchesByIds(targetIds)
                val idMap = dbLaunches.associateBy { it.id }
                val orderedList = targetIds.mapNotNull { idMap[it] }
                if (orderedList.isNotEmpty()) {
                    isPinnedMode = true
                    orderedList.take(MAX_SWIPE_ITEMS)
                } else {
                    isPinnedMode = false
                    cachedLaunches.take(MAX_SWIPE_ITEMS)
                }
            } else {
                isPinnedMode = false
                cachedLaunches.take(MAX_SWIPE_ITEMS)
            }

            if (launches.isEmpty()) {
                views.setViewVisibility(R.id.launch_widget_content, View.GONE)
                views.setViewVisibility(R.id.launch_widget_empty_state, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.launch_widget_root, configPendingIntent)
            } else {
                views.setViewVisibility(R.id.launch_widget_content, View.VISIBLE)
                views.setViewVisibility(R.id.launch_widget_empty_state, View.GONE)

                val detailTemplateIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val detailTemplatePendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId * 100 + 7,
                    detailTemplateIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.launch_widget_single_list, detailTemplatePendingIntent)
                views.setPendingIntentTemplate(R.id.launch_widget_multi_list, detailTemplatePendingIntent)

                val displayMode = getWidgetDisplayMode(context, appWidgetId)
                val useMultiView = displayMode == DISPLAY_MODE_MULTI && launches.size > 1

                if (isPinnedMode) {
                    views.setTextViewText(R.id.launch_widget_badge, "${launches.size} Selected")
                } else {
                    views.setTextViewText(R.id.launch_widget_badge, "Upcoming")
                }

                if (useMultiView) {
                    views.setViewVisibility(R.id.launch_widget_single_list, View.GONE)
                    views.setViewVisibility(R.id.launch_widget_multi_list, View.VISIBLE)

                    val collectionBuilder = RemoteViews.RemoteCollectionItems.Builder()
                        .setHasStableIds(true)
                        .setViewTypeCount(1)

                    launches.forEachIndexed { index, launch ->
                        val itemViews = buildMultiItemRemoteViews(context, launch)
                        collectionBuilder.addItem(index.toLong(), itemViews)
                    }

                    views.setRemoteAdapter(R.id.launch_widget_multi_list, collectionBuilder.build())
                } else {
                    views.setViewVisibility(R.id.launch_widget_single_list, View.VISIBLE)
                    views.setViewVisibility(R.id.launch_widget_multi_list, View.GONE)

                    val collectionBuilder = RemoteViews.RemoteCollectionItems.Builder()
                        .setHasStableIds(true)
                        .setViewTypeCount(1)

                    launches.forEachIndexed { index, launch ->
                        val itemViews = buildSingleItemRemoteViews(context, launch)
                        collectionBuilder.addItem(index.toLong(), itemViews)
                    }

                    views.setRemoteAdapter(R.id.launch_widget_single_list, collectionBuilder.build())
                }
            }

            withContext(Dispatchers.Main) {
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private suspend fun buildSingleItemRemoteViews(
            context: Context,
            launch: LaunchEntity
        ): RemoteViews {
            val itemViews = RemoteViews(context.packageName, R.layout.launch_widget_item_single)
            itemViews.setTextViewText(R.id.launch_single_name, launch.name)

            val providerRocketText = buildString {
                if (!launch.providerName.isNullOrEmpty()) {
                    append(launch.providerName)
                }
                val locationOrPad = launch.locationName ?: launch.padName
                if (!locationOrPad.isNullOrEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(locationOrPad)
                }
            }
            itemViews.setTextViewText(R.id.launch_single_provider, providerRocketText)
            itemViews.setTextViewText(R.id.launch_single_countdown, formatCountdown(launch.netMillis))
            itemViews.setTextViewText(R.id.launch_single_status, launch.statusName)
            itemViews.setTextViewText(R.id.launch_single_time, formatLaunchDate(launch.net))

            val bitmap = loadRoundedLaunchBitmap(
                context = context,
                imageUrl = launch.image,
                sizePx = 160,
                cornerRadiusPx = 36f
            )
            if (bitmap != null) {
                itemViews.setImageViewBitmap(R.id.launch_single_image, bitmap)
            } else {
                itemViews.setImageViewResource(R.id.launch_single_image, R.drawable.launch_widget_photo_placeholder)
            }

            val fillInIntent = Intent().apply {
                putExtra(EXTRA_LAUNCH_ID, launch.id)
            }
            itemViews.setOnClickFillInIntent(R.id.launch_item_root, fillInIntent)
            return itemViews
        }

        private suspend fun buildMultiItemRemoteViews(
            context: Context,
            launch: LaunchEntity
        ): RemoteViews {
            val itemViews = RemoteViews(context.packageName, R.layout.launch_widget_item_multi)
            itemViews.setTextViewText(R.id.launch_multi_name, launch.name)

            val shortDate = formatShortLaunchDate(launch.net)
            val subText = buildString {
                if (!launch.providerName.isNullOrEmpty()) {
                    append(launch.providerName)
                }
                if (shortDate.isNotEmpty()) {
                    if (isNotEmpty()) append(" • ")
                    append(shortDate)
                }
            }
            itemViews.setTextViewText(R.id.launch_multi_sub, subText)
            itemViews.setTextViewText(R.id.launch_multi_countdown, formatCompactCountdown(launch.netMillis))

            val bitmap = loadRoundedLaunchBitmap(
                context = context,
                imageUrl = launch.image,
                sizePx = 96,
                cornerRadiusPx = 24f
            )
            if (bitmap != null) {
                itemViews.setImageViewBitmap(R.id.launch_multi_image, bitmap)
            } else {
                itemViews.setImageViewResource(R.id.launch_multi_image, R.drawable.launch_widget_photo_placeholder)
            }

            val fillInIntent = Intent().apply {
                putExtra(EXTRA_LAUNCH_ID, launch.id)
            }
            itemViews.setOnClickFillInIntent(R.id.launch_item_root, fillInIntent)
            return itemViews
        }

        private suspend fun loadRoundedLaunchBitmap(
            context: Context,
            imageUrl: String?,
            sizePx: Int,
            cornerRadiusPx: Float
        ): Bitmap? {
            if (imageUrl.isNullOrBlank()) return null
            return try {
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(sizePx, sizePx)
                    .allowHardware(false)
                    .build()
                val result = Coil.imageLoader(context).execute(request)
                val rawBitmap = (result as? SuccessResult)?.let {
                    (it.drawable as? BitmapDrawable)?.bitmap
                } ?: return null
                createCenterCroppedRoundedBitmap(rawBitmap, sizePx, cornerRadiusPx)
            } catch (_: Exception) {
                null
            }
        }

        private fun createCenterCroppedRoundedBitmap(
            source: Bitmap,
            targetSize: Int,
            cornerRadius: Float
        ): Bitmap {
            val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val matrix = Matrix()
            val scale: Float
            var dx = 0f
            var dy = 0f
            if (source.width * targetSize > targetSize * source.height) {
                scale = targetSize.toFloat() / source.height.toFloat()
                dx = (targetSize - source.width * scale) * 0.5f
            } else {
                scale = targetSize.toFloat() / source.width.toFloat()
                dy = (targetSize - source.height * scale) * 0.5f
            }
            matrix.setScale(scale, scale)
            matrix.postTranslate(dx, dy)
            shader.setLocalMatrix(matrix)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                this.shader = shader
            }
            val rect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            return output
        }

        private fun formatCountdown(netMillis: Long): String {
            if (netMillis <= 0L) return "TBD"
            val now = System.currentTimeMillis()
            val diff = netMillis - now
            if (diff <= 0L) {
                val pastMinutes = TimeUnit.MILLISECONDS.toMinutes(-diff)
                return if (pastMinutes < 120) {
                    "T+ ${pastMinutes}m (IN FLIGHT)"
                } else {
                    "LAUNCHED"
                }
            }
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            return when {
                days > 0 -> String.format(Locale.US, "T- %dd %02dh %02dm", days, hours, minutes)
                hours > 0 -> String.format(Locale.US, "T- %02dh %02dm", hours, minutes)
                else -> String.format(Locale.US, "T- %02dm", minutes)
            }
        }

        private fun formatCompactCountdown(netMillis: Long): String {
            if (netMillis <= 0L) return "TBD"
            val now = System.currentTimeMillis()
            val diff = netMillis - now
            if (diff <= 0L) {
                val pastMinutes = TimeUnit.MILLISECONDS.toMinutes(-diff)
                return if (pastMinutes < 120) "T+ ${pastMinutes}m" else "LAUNCHED"
            }
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
            val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
            return when {
                days > 0 -> String.format(Locale.US, "T- %dd %02dh", days, hours)
                hours > 0 -> String.format(Locale.US, "T- %02dh %02dm", hours, minutes)
                else -> String.format(Locale.US, "T- %02dm", minutes)
            }
        }

        private fun formatLaunchDate(netString: String): String {
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(netString) ?: Date()
                val formatter = SimpleDateFormat("MMM d, yyyy • HH:mm z", Locale.getDefault())
                formatter.format(date)
            } catch (e: Exception) {
                netString
            }
        }

        private fun formatShortLaunchDate(netString: String): String {
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = parser.parse(netString) ?: Date()
                val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                formatter.format(date)
            } catch (e: Exception) {
                ""
            }
        }
    }
}
