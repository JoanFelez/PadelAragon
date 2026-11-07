package com.padelaragon.scraper

import kotlin.test.Test
import kotlin.test.assertEquals

class RunScraperTest {
    private val failing = object : PageSource {
        override suspend fun get(url: String): String = error("offline")
        override suspend fun post(url: String, params: Map<String, String>): String = error("offline")
        override suspend fun getOrNull(url: String): String? = null
    }

    @Test
    fun totalFailureExitsNonZeroAndWritesNothing() {
        val out = kotlin.io.path.createTempDirectory("scraper-test").toFile()
        try {
            val args = parseArgs(listOf("--out", out.path, "--leagues", "27951"), emptyMap())
            assertEquals(1, runScraper(args, failing))
            assertEquals(0, out.listFiles()!!.size)
        } finally {
            out.deleteRecursively()
        }
    }
}
