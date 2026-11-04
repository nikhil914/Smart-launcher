package com.nikhil.lumen.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import com.nikhil.lumen.service.HomeOverrideService

/**
 * Sleep timer. When it fires, Lumen asks its accessibility service to lock the screen, which puts
 * most Android TV devices to sleep. Needs the Home override to be on. Untested on many models.
 */
object SleepTimer {
    val OPTIONS_MINUTES = listOf(0, 15, 30, 60, 90, 120)

    private var endsAtElapsed: Long = 0

    fun remainingMinutes(): Int {
        val left = endsAtElapsed - SystemClock.elapsedRealtime()
        return if (left <= 0) 0 else ((left + 59_999) / 60_000).toInt()
    }

    fun set(ctx: Context, minutes: Int) {
        val alarm = ctx.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(ctx)
        alarm.cancel(pi)
        if (minutes <= 0) {
            endsAtElapsed = 0
            return
        }
        endsAtElapsed = SystemClock.elapsedRealtime() + minutes * 60_000L
        alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endsAtElapsed, pi)
    }

    /** Cycles through [OPTIONS_MINUTES] and returns the new setting. */
    fun cycle(ctx: Context): Int {
        val current = remainingMinutes()
        val next = OPTIONS_MINUTES.firstOrNull { it > current } ?: 0
        set(ctx, next)
        return next
    }

    private fun pendingIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        1,
        Intent(ctx, SleepReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    internal fun fired() {
        endsAtElapsed = 0
    }
}

class SleepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SleepTimer.fired()
        if (!HomeOverrideService.sleepNow()) {
            Toast.makeText(context, "Sleep timer needs the Lumen Home button service", Toast.LENGTH_LONG).show()
        }
    }
}
