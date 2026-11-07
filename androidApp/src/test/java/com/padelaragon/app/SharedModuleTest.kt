package com.padelaragon.app

import com.padelaragon.shared.appVersion
import com.padelaragon.shared.platformName
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedModuleTest {
    @Test
    fun sharedModuleIsWiredIntoAndroidApp() {
        assertEquals("Android", platformName)
        assertEquals("3.0.0", appVersion)
    }
}
