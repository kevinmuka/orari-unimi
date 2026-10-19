package app.orariunimi

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

internal class LaunchIntentState : ViewModel() {
    var handled = false
}

class MainActivity : ComponentActivity() {
    private val openSavedRequest = mutableIntStateOf(0)
    private val openNotificationsRequest = mutableIntStateOf(0)
    private val openNotificationId = mutableStateOf<String?>(null)
    private val openNotificationRequest = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The ViewModel survives rotation, but is new after process death. A rotated Activity
        // must not replay its old intent; a fresh process must still honor the launch intent.
        val launchIntentState = ViewModelProvider(this)[LaunchIntentState::class.java]
        if (!launchIntentState.handled) {
            if (intent.getBooleanExtra(OPEN_SAVED, false)) openSavedRequest.intValue++
            if (intent.getBooleanExtra(OPEN_NOTIFICATIONS, false)) openNotificationsRequest.intValue++
            openNotificationId.value = intent.getStringExtra(OPEN_NOTIFICATION_ID)
            if (openNotificationId.value != null) openNotificationRequest.intValue++
            launchIntentState.handled = true
        }
        enableEdgeToEdge()
        setContent {
            OrariTheme {
                OrariApp(
                    initialTab = 0,
                    openSavedRequest = openSavedRequest.intValue,
                    openNotificationsRequest = openNotificationsRequest.intValue,
                    openNotificationId = openNotificationId.value,
                    openNotificationRequest = openNotificationRequest.intValue
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.enterForeground()
    }

    override fun onResume() {
        super.onResume()
        NotificationScheduler.configure(applicationContext, runNow = true)
    }

    override fun onStop() {
        AppVisibility.enterBackground()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(OPEN_SAVED, false)) openSavedRequest.intValue++
        if (intent.getBooleanExtra(OPEN_NOTIFICATIONS, false)) openNotificationsRequest.intValue++
        openNotificationId.value = intent.getStringExtra(OPEN_NOTIFICATION_ID)
        if (openNotificationId.value != null) openNotificationRequest.intValue++
    }

    companion object {
        const val OPEN_SAVED = "open_saved"
        const val OPEN_NOTIFICATIONS = "open_notifications"
        const val OPEN_NOTIFICATION_ID = "open_notification_id"
    }
}
