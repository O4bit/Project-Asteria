package space.o4bit.projectasteria.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.Log
import space.o4bit.projectasteria.utils.WidgetPinningUtils

/**
 * Widget test utilities to help debug widget registration
 */
object WidgetTestUtils {
    
    fun logWidgetInfo(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val installedProviders = appWidgetManager.installedProviders
        
        Log.d("WidgetTest", "Total installed providers: ${installedProviders.size}")
        Log.d("WidgetTest", "Widget pinning supported: ${WidgetPinningUtils.isWidgetPinningSupported(context)}")
        
        installedProviders.forEach { provider ->
            Log.d("WidgetTest", "Provider: ${provider.provider.className}")
            if (provider.provider.packageName == context.packageName) {
                Log.d("WidgetTest", "Found our widget: ${provider.provider.className}")
                Log.d("WidgetTest", "Label: ${provider.loadLabel(context.packageManager)}")
            }
        }
    }
    
    fun testWidgetUpdate(context: Context) {
        val widget = AsteriaAppWidget()
        val intent = Intent().apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        widget.onReceive(context, intent)
        Log.d("WidgetTest", "Widget update test completed")
    }
    
    fun testWidgetPinning(context: Context) {
        Log.d("WidgetTest", "Testing widget pinning functionality")
        WidgetPinningUtils.pinWidgetToHomeScreen(context)
    }
}
