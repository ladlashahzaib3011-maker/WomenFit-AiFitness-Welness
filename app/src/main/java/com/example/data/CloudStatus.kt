package com.example.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Live connection state to the WomenFit Supabase server (womenfit-api.marketsync.cloud). */
enum class CloudConnection {
    /** First check not finished yet. */
    CHECKING,
    /** Server answered: data is being saved to the cloud. */
    ONLINE,
    /** No internet / server unreachable: data is saved on the phone and synced later. */
    OFFLINE,
    /** SUPABASE_URL / SUPABASE_ANON_KEY missing in the build. */
    NOT_CONFIGURED
}

/** One alert shown to the user after an action (snackbar at the bottom of the screen). */
data class CloudAlert(
    val message: String,
    val isSuccess: Boolean,
    val id: Long = System.nanoTime()
)

/**
 * App-wide cloud sync status + user alerts.
 *
 * - [connection] drives the "Cloud sync active" badge on the auth and profile screens.
 * - [alerts] drives the snackbar shown after every save (water, workout, meal, weight,
 *   goal, profile) and after the sign-in sync.
 *
 * Updated by WomenFitAiCloudClient (every network call), SupabaseAuthRepository (profile)
 * and FitnessRepository (each save).
 */
object CloudStatus {
    private val _connection = MutableStateFlow(CloudConnection.CHECKING)
    val connection: StateFlow<CloudConnection> = _connection.asStateFlow()

    private val _alerts = MutableSharedFlow<CloudAlert>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val alerts: SharedFlow<CloudAlert> = _alerts.asSharedFlow()

    fun setConnection(state: CloudConnection) {
        _connection.value = state
    }

    fun alert(message: String, isSuccess: Boolean) {
        _alerts.tryEmit(CloudAlert(message = message, isSuccess = isSuccess))
    }

    /** Standard alert after saving something: "<what> — saved to cloud ✓" or the offline variant. */
    fun reportSave(what: String, savedToCloud: Boolean) {
        if (savedToCloud) {
            alert("$what — saved to cloud ✓", isSuccess = true)
        } else {
            alert("$what — saved on phone, will sync when online", isSuccess = false)
        }
    }
}
