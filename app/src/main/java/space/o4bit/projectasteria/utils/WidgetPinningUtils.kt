package space.o4bit.projectasteria.utils

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.Toast
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.widget.AsteriaAppWidget
import space.o4bit.projectasteria.widget.IssAppWidgetProvider
import space.o4bit.projectasteria.widget.LaunchAppWidgetProvider

object WidgetPinningUtils {

    fun registerWidgetPreviews(context: Context) {
        if (Build.VERSION.SDK_INT >= 35) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val category = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN

                appWidgetManager.setWidgetPreview(
                    ComponentName(context, AsteriaAppWidget::class.java),
                    category,
                    RemoteViews(context.packageName, R.layout.asteria_app_widget)
                )
                appWidgetManager.setWidgetPreview(
                    ComponentName(context, IssAppWidgetProvider::class.java),
                    category,
                    RemoteViews(context.packageName, R.layout.iss_app_widget_layout)
                )
                appWidgetManager.setWidgetPreview(
                    ComponentName(context, LaunchAppWidgetProvider::class.java),
                    category,
                    RemoteViews(context.packageName, R.layout.launch_app_widget_layout)
                )
            } catch (_: Exception) {
            }
        }
    }

    fun pinWidgetToHomeScreen(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, AsteriaAppWidget::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val successIntent = Intent(context, AsteriaAppWidget::class.java).apply {
                    action = AsteriaAppWidget.ACTION_REFRESH
                }
                val successCallback = PendingIntent.getBroadcast(
                    context,
                    0,
                    successIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val extras = Bundle().apply {
                    putParcelable(
                        AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
                        RemoteViews(context.packageName, R.layout.asteria_app_widget)
                    )
                }
                appWidgetManager.requestPinAppWidget(myProvider, extras, successCallback)
            } else {
                showWidgetInstructions(context)
            }
        } else {
            showWidgetInstructions(context)
        }
    }

    fun pinLaunchWidgetToHomeScreen(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, LaunchAppWidgetProvider::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val successIntent = Intent(context, LaunchAppWidgetProvider::class.java).apply {
                    action = LaunchAppWidgetProvider.ACTION_REFRESH
                }
                val successCallback = PendingIntent.getBroadcast(
                    context,
                    1,
                    successIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val extras = Bundle().apply {
                    putParcelable(
                        AppWidgetManager.EXTRA_APPWIDGET_PREVIEW,
                        RemoteViews(context.packageName, R.layout.launch_app_widget_layout)
                    )
                }
                appWidgetManager.requestPinAppWidget(myProvider, extras, successCallback)
            } else {
                showWidgetInstructions(context)
            }
        } else {
            showWidgetInstructions(context)
        }
    }

    private fun showWidgetInstructions(context: Context) {
        Toast.makeText(
            context,
            "To add the widget: Long press on your home screen -> Widgets -> Project Asteria",
            Toast.LENGTH_LONG
        ).show()
    }

    fun isWidgetPinningSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager.isRequestPinAppWidgetSupported
        } else {
            false
        }
    }

    fun showAddToHomeScreenMessage(context: Context) {
        if (isWidgetPinningSupported(context)) {
            Toast.makeText(
                context,
                "Adding Project Asteria widget to home screen...",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                context,
                "Widget available in your launcher's widget menu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}