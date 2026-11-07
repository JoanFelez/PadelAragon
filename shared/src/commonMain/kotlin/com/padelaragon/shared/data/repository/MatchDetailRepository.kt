package com.padelaragon.shared.data.repository

import com.padelaragon.shared.data.local.entity.MatchDetailPairEntity
import com.padelaragon.shared.data.model.MatchDetail
import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.parser.MatchDetailParser
import com.padelaragon.shared.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.shared.data.repository.datasource.MatchDetailDataSource
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Played match sheets never change, so a stored detail is served forever. */
class MatchDetailRepository(
    private val scraping: ScrapingService,
    private val matchDetailParser: MatchDetailParser = MatchDetailParser()
) : MatchDetailDataSource {

    override suspend fun getMatchDetail(detailUrl: String): MatchDetail? {
        readCached(detailUrl)?.let { return it }

        val html = scraping.fetcher.get(resolveUrl(BASE_URL, detailUrl))
        val detail = matchDetailParser.parse(html)
        if (detail.pairs.isEmpty()) return null
        scraping.db.matchDetailDao().replaceForUrl(
            detailUrl, detail.pairs.map { MatchDetailPairEntity.fromModel(detailUrl, it) }
        )
        return detail
    }

    override suspend fun prefetchMatchDetails(results: List<MatchResult>) {
        val urls = results.mapNotNull { it.detailUrl }.filter { it.isNotBlank() }.distinct()
            .filter { readCached(it) == null }
        if (urls.isEmpty()) return

        coroutineScope {
            urls.map { url -> async { runCatching { getMatchDetail(url) } } }.awaitAll()
        }
    }

    private suspend fun readCached(detailUrl: String): MatchDetail? {
        val entities = scraping.db.matchDetailDao().getByDetailUrl(detailUrl)
        return if (entities.isEmpty()) null else MatchDetail(entities.map { it.toModel() })
    }
}

internal fun resolveUrl(base: String, relative: String): String {
    val url = relative.trim()
    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) return url
    return URLBuilder(base).takeFrom(url).buildString()
}
