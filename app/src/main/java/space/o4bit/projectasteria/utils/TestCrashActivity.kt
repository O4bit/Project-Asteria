package space.o4bit.projectasteria.utils

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity

/**
 * Simple activity to manually test crash reporting in debug/development.
 * Kept minimal so it doesn't pull in Compose or other app dependencies.
 */
class TestCrashActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		val layout = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(32, 48, 32, 48)
		}

		val crashBtn = Button(this).apply {
			text = "Simulate Crash"
			setOnClickListener {
				try {
					CrashReporter.simulateCrash()
				} catch (t: Throwable) {
					CrashReporter.recordException(t)
					Toast.makeText(this@TestCrashActivity, "Simulated crash recorded", Toast.LENGTH_SHORT).show()
				}
			}
		}

		val logBtn = Button(this).apply {
			text = "Log Breadcrumb"
			setOnClickListener {
				CrashReporter.log("Manual breadcrumb from TestCrashActivity")
				Toast.makeText(this@TestCrashActivity, "Logged breadcrumb", Toast.LENGTH_SHORT).show()
			}
		}

		layout.addView(crashBtn)
		layout.addView(logBtn)
		setContentView(layout)
	}
}
