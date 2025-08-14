package space.o4bit.projectasteria.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.ActivityOptions
import space.o4bit.projectasteria.MainActivity

class WidgetClickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val launch = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_FROM_WIDGET, true)
        }

        val options = ActivityOptions.makeCustomAnimation(
            context,
            space.o4bit.projectasteria.R.anim.widget_open_enter,
            space.o4bit.projectasteria.R.anim.widget_open_exit
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            context.startActivity(launch, options.toBundle())
        } else {
            context.startActivity(launch)
        }
    }

    companion object {
        const val EXTRA_FROM_WIDGET = "space.o4bit.projectasteria.extra.FROM_WIDGET"
        const val ACTION_OPEN_APP_FROM_WIDGET = "space.o4bit.projectasteria.ACTION_OPEN_APP_FROM_WIDGET"
    }
}
