package space.o4bit.projectasteria.utils

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import space.o4bit.projectasteria.R

class TestCrashActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val layout = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(32, 48, 32, 48)
		}

		val crashBtn = Button(this).apply {
			text = getString(R.string.test_crash_button_label)
			setOnClickListener {
				try {
					CrashReporter.simulateCrash()
				} catch (t: Throwable) {
					CrashReporter.recordException(t)
					Toast.makeText(this@TestCrashActivity, getString(R.string.test_crash_simulated_recorded), Toast.LENGTH_SHORT).show()
				}
			}
		}

		val logBtn = Button(this).apply {
			text = getString(R.string.test_crash_log_breadcrumb)
			setOnClickListener {
				CrashReporter.log("Manual breadcrumb from TestCrashActivity")
				Toast.makeText(this@TestCrashActivity, getString(R.string.test_crash_breadcrumb_logged), Toast.LENGTH_SHORT).show()
			}
		}

		layout.addView(crashBtn)
		layout.addView(logBtn)
		setContentView(layout)
	}
}
