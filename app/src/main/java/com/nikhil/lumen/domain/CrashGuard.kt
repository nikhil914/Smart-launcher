package com.nikhil.lumen.domain

/**
 * Decides when to start in recovery mode. A launcher that crashes on start can leave the TV with
 * no usable home screen, so after [MAX_CRASHES] crashes inside [WINDOW_MS] Lumen opens a minimal
 * recovery screen instead of the full home.
 */
object CrashGuard {
    const val MAX_CRASHES = 3
    const val WINDOW_MS = 60_000L

    /** Keeps only crash times that still matter and appends [now]. */
    fun record(previous: List<Long>, now: Long): List<Long> =
        (previous.filter { now - it in 0..WINDOW_MS } + now).takeLast(MAX_CRASHES)

    fun shouldRecover(crashes: List<Long>, now: Long): Boolean =
        crashes.count { now - it in 0..WINDOW_MS } >= MAX_CRASHES
}
