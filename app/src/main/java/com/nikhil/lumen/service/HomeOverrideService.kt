package com.nikhil.lumen.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.nikhil.lumen.LumenApp
import com.nikhil.lumen.domain.HomePress
import com.nikhil.lumen.system.HomeSetup

/**
 * Makes the Home button open Lumen on Google TV, where the system hides the default-home dialog.
 *
 * - Short Home press: opens Lumen.
 * - Long Home press (600 ms): opens the stock Google TV home, as an escape hatch. Lumen then stops
 *   covering the stock home until the next short press.
 * - If the stock home window appears some other way (an app finishing, a system shortcut), Lumen
 *   brings itself back in front, when "cover stock home" is on.
 *
 * Only key events and window-change package names are read. canRetrieveWindowContent is false.
 */
class HomeOverrideService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var homeDownAt = 0L
    private var longPressFired = false
    /** Set after a long press: the user asked for the stock home, so do not cover it. */
    private var stockAllowed = false

    private val longPress = Runnable {
        longPressFired = true
        stockAllowed = true
        if (!HomeSetup.openStockHome(this)) {
            // No other home found; fall back to Lumen rather than doing nothing.
            stockAllowed = false
            HomeSetup.openLumen(this)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME) return false
        // When the system already routes Home to Lumen (plain Android TV, or strong mode), stay out of the way.
        if (HomeSetup.isDefaultHome(this)) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                homeDownAt = event.eventTime
                longPressFired = false
                handler.postDelayed(longPress, HomePress.LONG_PRESS_MS)
            }
            KeyEvent.ACTION_UP -> {
                handler.removeCallbacks(longPress)
                if (!longPressFired &&
                    HomePress.classify(homeDownAt, event.eventTime) == HomePress.Kind.SHORT
                ) {
                    stockAllowed = false
                    HomeSetup.openLumen(this)
                }
            }
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) {
            stockAllowed = false
            return
        }
        if (pkg !in HomeSetup.STOCK_HOMES || stockAllowed) return
        if (!LumenApp.from(this).settings.state.value.coverStockHome) return
        // Debounce: Google TV can post several window changes while its home draws.
        val now = SystemClock.uptimeMillis()
        if (now - lastCoverAt < 1_000) return
        lastCoverAt = now
        HomeSetup.openLumen(this)
    }

    private var lastCoverAt = 0L

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        private var instance: HomeOverrideService? = null

        /** Used by the sleep timer. Returns false when the service is not running. */
        fun sleepNow(): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }
    }
}
