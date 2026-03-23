package com.padelaragon.app.data.repository

import com.padelaragon.app.data.local.entity.MatchDetailPairEntity
import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.parser.MatchDetailParser
import com.padelaragon.app.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.app.data.repository.datasource.MatchDetailDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

class MatchDetailRepository(
    private val scraping: ScrapingService,
    private val matchDetailParser: MatchDetailParser = MatchDetailParser()
) : MatchDetailDataSource {

    private val cachedMatchDetails = ConcurrentHashMap<String, MatchDetail>()

    override suspend fun getMatchDetail(detailUrl: String): MatchDetail? {
        cachedMatchDetails[detailUrl]?.let { return it }

        val entities = scraping.db.matchDetailDao().getByDetailUrl(detailUrl)
        if (entities.isNotEmpty()) {
            val detail = MatchDetail(entities.map { it.toModel() })
            cachedMatchDetails[detailUrl] = detail
            return detail
        }

        val fullUrl = buildDetailUrl(detailUrl)
        val html = scraping.withSemaphore { scraping.fetcher.get(fullUrl) }
        val detail = matchDetailParser.parse(html)
        if (detail.pairs.isNotEmpty()) {
            cachedMatchDetails[detailUrl] = detail
            scraping.db.matchDetailDao().insertAll(
                detail.pairs.map { MatchDetailPairEntity.fromModel(detailUrl, it) }
            )
        }
        return if (detail.pairs.isNotEmpty()) detail else null
    }

    override suspend fun prefetchMatchDetails(results: List<MatchResult>) {
        val urlsToPrefetch = results
            .mapNotNull { it.detailUrl }
            .filter { it.isNotBlank() && !cachedMatchDetails.containsKey(it) }
        if (urlsToPrefetch.isEmpty()) return

        coroutineScope {
            urlsToPrefetch.map { url ->
                async { runCatching { getMatchDetail(url) } }
            }.awaitAll()
        }
    }

    private fun buildDetailUrl(relativeUrl: String): String {
        val url = relativeUrl.trim()
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) return url
        return URI(BASE_URL).resolve(url).toString()
    }
}
