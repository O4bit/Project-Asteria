package space.o4bit.projectasteria.utils

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.widget.AsteriaAppWidget

/**
 * Utility class for handling widget pinning functionality
 */
object WidgetPinningUtils {
    
    fun pinWidgetToHomeScreen(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val myProvider = ComponentName(context, AsteriaAppWidget::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val successIntent = Intent(context, AsteriaAppWidget::class.java).apply {
                    action = AsteriaAppWidget.ACTION_REFRESH
                }
                
                val successCallback = android.app.PendingIntent.getBroadcast(
                    context, 0, successIntent, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                
                appWidgetManager.requestPinAppWidget(myProvider, null, successCallback)
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
            "To add the widget: Long press on your home screen → Widgets → Project Asteria",
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
