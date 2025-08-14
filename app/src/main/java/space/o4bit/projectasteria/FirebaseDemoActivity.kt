package space.o4bit.projectasteria

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
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Simple activity to demonstrate Firebase Crashlytics integration
 */
class FirebaseDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Crashlytics directly
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(true)
        crashlytics.setUserId("test_user_123")
        crashlytics.log("Firebase Demo Activity created")
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CrashlyticsDemoScreen()
                }
            }
        }
    }
}

@Composable
fun CrashlyticsDemoScreen() {
    val crashlytics = FirebaseCrashlytics.getInstance()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Firebase Crashlytics Integration",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Button(
            onClick = {
                // Log a custom key
                crashlytics.setCustomKey("test_button_clicked", true)
                
                // Log a message
                crashlytics.log("User clicked the test crash button")
                
                // Force a crash
                throw RuntimeException("Test Crash")
            }
        ) {
            Text("Test Crash")
        }
        
        Button(
            onClick = {
                // Log a non-fatal exception
                try {
                    val items = listOf(1, 2, 3)
                    val itemAtIndex10 = items[10] // This will cause an IndexOutOfBoundsException
                } catch (e: Exception) {
                    crashlytics.recordException(e)
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Log Non-Fatal Exception")
        }
        
        Button(
            onClick = {
                // Add a breadcrumb
                crashlytics.log("BREADCRUMB: User clicked Add Breadcrumb button")
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Add Breadcrumb")
        }
    }
}
