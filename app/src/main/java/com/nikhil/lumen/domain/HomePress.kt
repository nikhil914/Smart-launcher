package com.nikhil.lumen.domain

/** Splits a Home key press into a short press (open Lumen) or a long press (let the stock home through). */
object HomePress {
    const val LONG_PRESS_MS = 600L

    enum class Kind { SHORT, LONG }

    fun classify(downAtMs: Long, upAtMs: Long): Kind =
        if (upAtMs - downAtMs >= LONG_PRESS_MS) Kind.LONG else Kind.SHORT
}
