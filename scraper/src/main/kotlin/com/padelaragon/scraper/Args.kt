package com.padelaragon.scraper

import com.padelaragon.shared.data.model.League

data class ScraperArgs(
    val outDir: String,
    val previousFrom: String?,
    val upload: Boolean,
    val leagues: List<League>,
    val appKey: String?,
    val workerUrl: String?,
    val scraperToken: String?
)

class UsageException(message: String) : IllegalArgumentException(message)

const val USAGE = """Usage: scraper --out <dir> [--previous-from <workerUrl>] [--upload] [--leagues <id,id,...>]
  --out <dir>              output directory (manifest.json, leagues.json, <leagueId>.json)
  --previous-from <url>    fetch previous bundles from the Worker (env APP_KEY) to scrape incrementally
  --upload                 PUT changed documents to the Worker (env WORKER_URL, SCRAPER_TOKEN)
  --leagues <ids>          comma separated league ids (default: all known leagues)"""

fun parseArgs(args: List<String>, env: Map<String, String>): ScraperArgs {
    var out: String? = null
    var previousFrom: String? = null
    var upload = false
    var leagues: List<League> = League.entries.toList()

    val iterator = args.iterator()
    fun value(flag: String): String =
        if (iterator.hasNext()) iterator.next() else throw UsageException("Missing value for $flag")

    while (iterator.hasNext()) {
        when (val flag = iterator.next()) {
            "--out" -> out = value(flag)
            "--previous-from" -> previousFrom = value(flag)
            "--upload" -> upload = true
            "--leagues" -> leagues = value(flag).split(',').map { raw ->
                val id = raw.trim().toIntOrNull() ?: throw UsageException("Invalid league id: '$raw'")
                League.fromId(id) ?: throw UsageException("Unknown league id: $id")
            }.distinct()
            else -> throw UsageException("Unknown argument: $flag")
        }
    }

    val outDir = out ?: throw UsageException("--out is required")
    val appKey = env["APP_KEY"]?.takeIf { it.isNotBlank() }
    val workerUrl = env["WORKER_URL"]?.trim()?.takeIf { it.isNotEmpty() }
    val scraperToken = env["SCRAPER_TOKEN"]?.takeIf { it.isNotBlank() }
    if (previousFrom != null && appKey == null) throw UsageException("--previous-from requires env APP_KEY")
    if (upload && (workerUrl == null || scraperToken == null)) {
        throw UsageException("--upload requires env WORKER_URL and SCRAPER_TOKEN")
    }
    return ScraperArgs(outDir, previousFrom?.trimEnd('/'), upload, leagues, appKey, workerUrl?.trimEnd('/'), scraperToken)
}
