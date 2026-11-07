package com.padelaragon.shared.domain.usecase

import com.padelaragon.shared.data.model.League
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupPreloaderTest {

    @Test
    fun `all leagues succeed marks progress done with no failures`() = runTest {
        val preloader = StartupPreloader(leagues = League.entries) { /* succeeds */ }

        preloader.preloadAll()

        val progress = preloader.progress.value
        assertTrue(progress.isDone)
        assertEquals(League.entries.toSet(), progress.completedLeagues)
        assertTrue(progress.failedLeagues.isEmpty())
        assertEquals(1f, progress.fraction, 0.0001f)
    }

    @Test
    fun `a failing league is tracked separately without blocking the others`() = runTest {
        val preloader = StartupPreloader(leagues = League.entries) { league ->
            if (league == League.MENORES) error("network error")
        }

        preloader.preloadAll()

        val progress = preloader.progress.value
        assertTrue(progress.isDone)
        assertEquals(setOf(League.MENORES), progress.failedLeagues)
        assertEquals(League.entries.toSet() - League.MENORES, progress.completedLeagues)
    }

    @Test
    fun `fraction reflects partial completion before all leagues finish`() {
        val progress = StartupPreloader.Progress(
            completedLeagues = setOf(League.ABSOLUTA),
            totalLeagues = 3
        )

        assertEquals(1f / 3f, progress.fraction, 0.0001f)
        assertTrue(!progress.isDone)
    }
}
