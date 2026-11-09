package com.padelaragon.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformTest {
    @Test
    fun versionIsSemverFromGradleProperties() {
        assertTrue(Regex("""\d+\.\d+\.\d+""").matches(appVersion))
    }

    @Test
    fun platformNameIsNotBlank() {
        assertEquals(false, platformName.isBlank())
    }
}
