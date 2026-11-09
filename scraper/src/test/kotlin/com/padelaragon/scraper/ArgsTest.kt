package com.padelaragon.scraper

import com.padelaragon.shared.data.model.League
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArgsTest {
    @Test
    fun defaultsToAllLeagues() {
        val a = parseArgs(listOf("--out", "build/out"), emptyMap())
        assertEquals(League.entries.toList(), a.leagues)
        assertEquals("build/out", a.outDir)
        assertNull(a.previousFrom)
        assertTrue(!a.upload)
    }

    @Test
    fun parsesLeaguesAndEnvBackedOptions() {
        val env = mapOf("APP_KEY" to "k", "WORKER_URL" to "https://w.example/", "SCRAPER_TOKEN" to "t")
        val a = parseArgs(
            listOf("--out", "o", "--leagues", "27951, 28060", "--previous-from", "https://w.example/", "--upload"), env
        )
        assertEquals(listOf(League.ABSOLUTA, League.MENORES), a.leagues)
        assertEquals("https://w.example", a.previousFrom)
        assertEquals("https://w.example", a.workerUrl)
        assertTrue(a.upload)
    }

    @Test
    fun rejectsInvalidUsage() {
        assertFailsWith<UsageException> { parseArgs(emptyList(), emptyMap()) }
        assertFailsWith<UsageException> { parseArgs(listOf("--out", "o", "--leagues", "1"), emptyMap()) }
        assertFailsWith<UsageException> { parseArgs(listOf("--out", "o", "--upload"), emptyMap()) }
        assertFailsWith<UsageException> { parseArgs(listOf("--out", "o", "--previous-from", "u"), emptyMap()) }
        assertFailsWith<UsageException> { parseArgs(listOf("--out", "o", "--bogus"), emptyMap()) }
        assertFailsWith<UsageException> { parseArgs(listOf("--out"), emptyMap()) }
    }
}
