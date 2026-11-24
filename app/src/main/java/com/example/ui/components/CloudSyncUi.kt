package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppContainer
import com.example.data.CloudConnection
import com.example.data.CloudStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val OnlineGreen = Color(0xFF22C55E)
private val OfflineAmber = Color(0xFFF59E0B)
private val NeutralGrey = Color(0xFF94A3B8)

/**
 * "● Cloud sync active" pill (same idea as the original WomenFit app), but live:
 * it pings the Supabase server when shown and every 30 seconds while visible.
 *
 *  - green  "Cloud sync active"                 → server reachable, saves go to the cloud
 *  - amber  "Offline mode – saving on phone"    → no connection, data syncs later
 *  - grey   "Connecting to cloud…"              → first check running
 */
@Composable
fun CloudSyncBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val connection by CloudStatus.connection.collectAsState()

    LaunchedEffect(Unit) {
        val sync = AppContainer.getInstance(context).cloudSync
        while (true) {
            runCatching { sync.checkConnection() }
            delay(30_000)
        }
    }

    val (dotColor, label) = when (connection) {
        CloudConnection.ONLINE -> OnlineGreen to "Cloud sync active"
        CloudConnection.OFFLINE -> OfflineAmber to "Offline mode – saving on phone"
        CloudConnection.NOT_CONFIGURED -> NeutralGrey to "Cloud not configured"
        CloudConnection.CHECKING -> NeutralGrey to "Connecting to cloud…"
    }

    // Soft pulse on the dot while online, like a live indicator.
    val pulse = rememberInfiniteTransition(label = "cloudPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = if (connection == CloudConnection.ONLINE) 0.45f else 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "cloudDotAlpha"
    )

    Row(
        modifier = modifier
            .testTag("cloud_sync_badge")
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.85f))
            .border(1.dp, Color(0xFFE9D5E3), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF475569),
            fontWeight = FontWeight.SemiBold
        )
    }
}

private class CloudAlertVisuals(
    override val message: String,
    val isSuccess: Boolean
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = false
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

/**
 * Shows every [CloudStatus.alerts] message as a snackbar ("💧 Water +250 ml — saved to cloud ✓").
 * Place once at the bottom of the app, above the bottom navigation bar.
 */
@Composable
fun CloudAlertHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        CloudStatus.alerts.collect { alert ->
            // Newest alert replaces the one on screen instead of waiting in a queue.
            hostState.currentSnackbarData?.dismiss()
            scope.launch { hostState.showSnackbar(CloudAlertVisuals(alert.message, alert.isSuccess)) }
        }
    }

    SnackbarHost(
        hostState = hostState,
        modifier = modifier.testTag("cloud_alert_host")
    ) { data: SnackbarData ->
        val success = (data.visuals as? CloudAlertVisuals)?.isSuccess ?: true
        Snackbar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            containerColor = if (success) Color(0xFF0F766E) else Color(0xFFB45309),
            contentColor = Color.White
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
