package space.o4bit.projectasteria.utils

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import space.o4bit.projectasteria.ui.theme.ProjectAsteriaTheme

/**
 * Activity used for testing crash reporting functionality.
 * This activity provides buttons to simulate different types of crashes
 * and errors to verify that Crashlytics is working correctly.
 */
class TestCrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ProjectAsteriaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrashTestScreen()
                }
            }
        }
    }
}

@Composable
fun CrashTestScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crash Testing",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(
            onClick = {
                // Log a non-fatal exception
                try {
                    val testMap = mapOf<String, String>()
                    val value = testMap["nonexistent"]!!
                } catch (e: Exception) {
                    CrashReporter.logException(e, "Test non-fatal exception")
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Test Non-Fatal Exception")
        }
        
        Button(
            onClick = {
                // Log custom event before crash
                CrashReporter.logBreadcrumb("About to cause a test crash")
                CrashReporter.setCustomKey("test_crash", true)
                
                // This will cause a crash
                throw RuntimeException("Test crash from TestCrashActivity")
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Test Fatal Crash")
        }
        
        Button(
            onClick = {
                // Trigger an out of memory error simulation
                CrashReporter.logBreadcrumb("Testing memory error simulation")
                try {
                    val array = IntArray(Int.MAX_VALUE)
                } catch (e: OutOfMemoryError) {
                    CrashReporter.logException(e, "OOM test")
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Test Memory Error")
        }
        
        Button(
            onClick = {
                // Trigger ANR-like behavior
                CrashReporter.logBreadcrumb("Testing ANR-like behavior")
                try {
                    // Simulate work on main thread
                    Thread.sleep(10000)
                } catch (e: Exception) {
                    CrashReporter.logException(e, "ANR test interrupted")
                }
            },
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text("Simulate ANR")
        }
    }
}
