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
        // A restored task must not replay the intent that originally opened it. A genuinely
        // different notification intent still takes precedence over the restored page.
        val launchIntentState = ViewModelProvider(this)[LaunchIntentState::class.java]
        if (!launchIntentState.handled) {
            if (savedInstanceState?.getString(LAST_LAUNCH_ROUTE) != routeIntentKey(intent)) {
                if (intent.getBooleanExtra(OPEN_SAVED, false)) openSavedRequest.intValue++
                if (intent.getBooleanExtra(OPEN_NOTIFICATIONS, false)) openNotificationsRequest.intValue++
                openNotificationId.value = intent.getStringExtra(OPEN_NOTIFICATION_ID)
                if (openNotificationId.value != null) openNotificationRequest.intValue++
            }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(LAST_LAUNCH_ROUTE, routeIntentKey(intent))
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(OPEN_SAVED, false)) openSavedRequest.intValue++
        if (intent.getBooleanExtra(OPEN_NOTIFICATIONS, false)) openNotificationsRequest.intValue++
        openNotificationId.value = intent.getStringExtra(OPEN_NOTIFICATION_ID)
        if (openNotificationId.value != null) openNotificationRequest.intValue++
    }

    private fun routeIntentKey(intent: Intent): String = listOf(
        intent.action.orEmpty(), intent.dataString.orEmpty(),
        intent.getBooleanExtra(OPEN_SAVED, false).toString(),
        intent.getBooleanExtra(OPEN_NOTIFICATIONS, false).toString(),
        intent.getStringExtra(OPEN_NOTIFICATION_ID).orEmpty()
    ).joinToString("|")

    companion object {
        private const val LAST_LAUNCH_ROUTE = "last_launch_route"
        const val OPEN_SAVED = "open_saved"
        const val OPEN_NOTIFICATIONS = "open_notifications"
        const val OPEN_NOTIFICATION_ID = "open_notification_id"
    }
}
