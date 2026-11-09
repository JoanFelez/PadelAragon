package com.padelaragon.scraper

import com.padelaragon.shared.appVersion
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.remote.dto.LeagueBundleDto
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val parsed = try {
        parseArgs(args.toList(), System.getenv())
    } catch (e: UsageException) {
        System.err.println("error: ${e.message}\n$USAGE")
        exitProcess(2)
    }
    exitProcess(runScraper(parsed, FetcherPageSource()))
}

/** Runs one full scrape; returns the process exit code (0 ok, 1 total failure or upload failure). */
fun runScraper(args: ScraperArgs, pages: PageSource, now: String = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()): Int {
    println("PadelAragon scraper v$appVersion: leagues=${args.leagues.joinToString { it.id.toString() }} upload=${args.upload}")
    val backend = args.previousFrom?.let { BackendClient(it, appKey = args.appKey) }
    val previous = loadPrevious(backend, args.leagues)
    val scraper = LeagueScraper(pages)

    val scraped = LinkedHashMap<Int, LeagueBundleDto>()
    for (league in args.leagues) {
        val started = System.nanoTime()
        try {
            val result = runBlocking { scraper.scrape(league, previous.bundle(league.id), now) }
            scraped[league.id] = result.bundle
            val s = result.stats
            println(
                "league ${league.id}: groups=${s.groups} matches=${s.matches} detailsFetched=${s.detailsFetched} " +
                    "detailsReused=${s.detailsReused} warnings=${s.warnings} seconds=${(System.nanoTime() - started) / 1_000_000_000}"
            )
        } catch (e: Exception) {
            val kept = if (previous.bundleBytes.containsKey(league.id)) "keeping previous bundle" else "no previous bundle"
            System.err.println("WARN: league ${league.id} failed (${e::class.simpleName}); $kept")
        }
    }
    if (scraped.isEmpty()) {
        System.err.println("error: no league could be scraped")
        return 1
    }

    val artifacts = assemble(args.leagues, scraped, League.entries.toList(), previous, now)
    val out = File(args.outDir).apply { mkdirs() }
    artifacts.forEach {
        File(out, it.fileName).writeBytes(it.bytes)
        println("wrote ${it.fileName} bytes=${it.bytes.size} sha256=${it.hash.take(12)} changed=${it.changed}")
    }

    if (!args.upload) return 0
    val uploader = BackendClient(args.workerUrl!!, scraperToken = args.scraperToken)
    var failed = false
    uploadsFor(artifacts).forEach {
        try {
            uploader.put(it.route, it.bytes, it.hash)
            println("uploaded ${it.fileName}")
        } catch (e: Exception) {
            failed = true
            System.err.println("error: upload of ${it.fileName} failed: ${e.message}")
        }
    }
    if (uploadsFor(artifacts).isEmpty()) println("nothing to upload")
    return if (failed) 1 else 0
}

private fun loadPrevious(backend: BackendClient?, leagues: List<League>): PreviousState {
    if (backend == null) return PreviousState()
    return try {
        val manifest = backend.get("/api/manifest")
        val leaguesJson = backend.get("/api/leagues")
        val bundles = leagues.mapNotNull { l -> backend.get("/api/leagues/${l.id}/bundle")?.let { l.id to it } }.toMap()
        println("previous state: manifest=${manifest != null} bundles=${bundles.size}")
        PreviousState(manifest, leaguesJson, bundles)
    } catch (e: Exception) {
        System.err.println("WARN: could not load previous state (${e::class.simpleName}); running a full scrape")
        PreviousState()
    }
}
