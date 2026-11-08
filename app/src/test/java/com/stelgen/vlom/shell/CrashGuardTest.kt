package com.stelgen.vlom.shell

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты S5.2: CrashGuard — счётчик падений и режим восстановления.
 */
class CrashGuardTest {

    private class MemStore : CrashStateStore {
        val map = mutableMapOf<String, Any>()
        override fun getInt(key: String, def: Int): Int = map[key] as? Int ?: def
        override fun putInt(key: String, value: Int) { map[key] = value }
        override fun getBool(key: String, def: Boolean): Boolean = map[key] as? Boolean ?: def
        override fun putBool(key: String, value: Boolean) { map[key] = value }
    }

    @Test
    fun `two consecutive crashes enter recovery`() {
        val g = CrashGuard(MemStore())
        g.onCrash()
        assertFalse(g.isRecoveryMode())
        g.onCrash()
        assertTrue(g.isRecoveryMode())
    }

    @Test
    fun `healthy home resets the streak`() {
        val g = CrashGuard(MemStore())
        g.onCrash()
        g.onHealthyHome()
        g.onCrash()
        assertFalse(g.isRecoveryMode())
    }

    @Test
    fun `exit recovery clears flag and streak`() {
        val g = CrashGuard(MemStore())
        g.onCrash(); g.onCrash()
        assertTrue(g.isRecoveryMode())
        g.exitRecovery()
        assertFalse(g.isRecoveryMode())
        g.onCrash()
        assertFalse(g.isRecoveryMode())
    }

    @Test
    fun `custom threshold respected`() {
        val g = CrashGuard(MemStore())
        g.onCrash(threshold = 1)
        assertTrue(g.isRecoveryMode())
    }
}
