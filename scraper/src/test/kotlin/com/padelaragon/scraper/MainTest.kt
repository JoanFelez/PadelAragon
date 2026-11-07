package com.padelaragon.scraper

import com.padelaragon.shared.appVersion
import kotlin.test.Test
import kotlin.test.assertTrue

class MainTest {
    @Test
    fun sharedModuleIsReachable() {
        assertTrue(appVersion.isNotBlank())
    }
}
