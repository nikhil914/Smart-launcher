package com.nikhil.lumen.data

import android.content.Context
import android.util.Log
import com.nikhil.lumen.domain.CrashGuard
import com.nikhil.lumen.domain.LauncherState
import com.nikhil.lumen.domain.StateCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single source of truth for [LauncherState]. Shared by the activity and the Home override
 * service (same process), persisted as one JSON string in SharedPreferences.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("lumen", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<LauncherState> = _state.asStateFlow()

    fun update(transform: (LauncherState) -> LauncherState) {
        _state.update(transform)
        prefs.edit().putString(KEY_STATE, StateCodec.encode(_state.value)).apply()
    }

    fun replace(newState: LauncherState) = update { newState }

    private fun load(): LauncherState {
        val json = prefs.getString(KEY_STATE, null) ?: return LauncherState()
        return try {
            StateCodec.decode(json)
        } catch (e: Exception) {
            Log.w(TAG, "Stored state unreadable, starting fresh", e)
            LauncherState()
        }
    }

    // --- Crash bookkeeping for recovery mode. Written with commit() because the process is dying. ---

    fun recordCrash(now: Long) {
        val updated = CrashGuard.record(crashTimes(), now)
        prefs.edit().putString(KEY_CRASHES, updated.joinToString(",")).commit()
    }

    fun crashTimes(): List<Long> =
        prefs.getString(KEY_CRASHES, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }

    fun clearCrashes() {
        prefs.edit().remove(KEY_CRASHES).apply()
    }

    private companion object {
        const val TAG = "Lumen"
        const val KEY_STATE = "state"
        const val KEY_CRASHES = "crashes"
    }
}
