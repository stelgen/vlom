package com.stelgen.vlom

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Контракт версии: semver + синхронность с UI-маркером.
 * Канон: фича без теста = незаконченная фича.
 */
class VersionTest {

    @Test
    fun `version is semver`() {
        assertTrue(Regex("""^\d+\.\d+\.\d+$""").matches(Version.current))
    }

    @Test
    fun `version starts at phase 0 baseline`() {
        assertEquals("0.1.0", Version.current)
    }
}
