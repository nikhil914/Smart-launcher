package com.nikhil.lumen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashGuardTest {
    @Test
    fun threeCrashesInAMinuteTriggerRecovery() {
        var crashes = emptyList<Long>()
        crashes = CrashGuard.record(crashes, 1_000)
        crashes = CrashGuard.record(crashes, 20_000)
        assertFalse(CrashGuard.shouldRecover(crashes, 20_000))
        crashes = CrashGuard.record(crashes, 40_000)
        assertTrue(CrashGuard.shouldRecover(crashes, 40_000))
    }

    @Test
    fun oldCrashesAreForgotten() {
        val crashes = CrashGuard.record(listOf(1_000, 2_000), 120_000)
        assertEquals(listOf(120_000L), crashes)
        assertFalse(CrashGuard.shouldRecover(crashes, 120_000))
    }

    @Test
    fun homePressThreshold() {
        assertEquals(HomePress.Kind.SHORT, HomePress.classify(0, 200))
        assertEquals(HomePress.Kind.LONG, HomePress.classify(0, HomePress.LONG_PRESS_MS))
    }
}
