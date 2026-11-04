package com.nikhil.lumen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nikhil.lumen.data.WatchNextRepository
import com.nikhil.lumen.domain.CrashGuard
import com.nikhil.lumen.ui.HomeScreen
import com.nikhil.lumen.ui.LumenTheme
import com.nikhil.lumen.ui.RecoveryScreen
import com.nikhil.lumen.ui.SettingsScreen
import com.nikhil.lumen.ui.SetupScreen

private enum class Screen { HOME, SETTINGS, SETUP }

class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()

    private var screen by mutableStateOf(Screen.HOME)
    private var resetSignal by mutableIntStateOf(0)
    private var recovery by mutableStateOf(false)

    private val watchNextPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { !it }) {
                Toast.makeText(this, "Not allowed. You can allow it later in App info › Permissions.", Toast.LENGTH_LONG).show()
            }
            vm.refresh()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = LumenApp.from(this).settings
        recovery = CrashGuard.shouldRecover(store.crashTimes(), System.currentTimeMillis())

        setContent {
            val state by vm.ui.collectAsStateWithLifecycle()
            LumenTheme(state.settings.accent) {
                if (recovery) {
                    RecoveryScreen(
                        onRetry = {
                            store.clearCrashes()
                            recovery = false
                        },
                        onResetAndRetry = {
                            vm.resetLayout()
                            recovery = false
                        },
                    )
                } else {
                    // A launcher has nowhere to go back to: Back on the home screen does nothing.
                    BackHandler(enabled = true) { screen = Screen.HOME }

                    when (screen) {
                        Screen.HOME -> HomeScreen(
                            state = state,
                            vm = vm,
                            resetSignal = resetSignal,
                            onOpenSettings = { screen = Screen.SETTINGS },
                            onOpenSetup = { screen = Screen.SETUP },
                            onRequestWatchNext = ::requestWatchNext,
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            state = state,
                            vm = vm,
                            onOpenSetup = { screen = Screen.SETUP },
                            onRequestWatchNext = ::requestWatchNext,
                        )
                        Screen.SETUP -> SetupScreen(state)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing Home while Lumen is already in front: go back to the top of the home screen.
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || intent.hasCategory(Intent.CATEGORY_HOME)) {
            screen = Screen.HOME
            resetSignal++
        }
    }

    private fun requestWatchNext() = watchNextPermission.launch(WatchNextRepository.PERMISSIONS)
}

object BuildConfigInfo {
    fun versionName(ctx: Context): String = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }
}
