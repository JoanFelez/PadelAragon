package com.padelaragon.app.data.repository

import com.padelaragon.app.data.local.entity.JornadaEntity
import com.padelaragon.app.data.local.entity.MatchResultEntity
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.parser.GroupParser
import com.padelaragon.app.data.parser.MatchResultParser
import com.padelaragon.app.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.app.data.repository.ScrapingService.Companion.LEAGUE_ID
import com.padelaragon.app.data.repository.datasource.MatchResultDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

class MatchResultRepository(
    private val scraping: ScrapingService,
    private val matchResultParser: MatchResultParser = MatchResultParser(),
    private val groupParser: GroupParser = GroupParser()
) : MatchResultDataSource {

    private val cachedJornadas = ConcurrentHashMap<Int, List<Int>>()
    private val cachedResults = ConcurrentHashMap<Long, List<MatchResult>>()
    private val finalizedJornadas = ConcurrentHashMap.newKeySet<Long>()

    private fun resultKey(groupId: Int, jornada: Int): Long =
        groupId.toLong() * 100_000 + jornada

    override suspend fun getJornadas(groupId: Int): List<Int> {
        cachedJornadas[groupId]?.let { return it }

        val roomJornadas = scraping.db.jornadaDao().getByGroupId(groupId)
        if (roomJornadas.isNotEmpty()) {
            cachedJornadas[groupId] = roomJornadas
            return roomJornadas
        }

        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=$LEAGUE_ID&grupo=$groupId"
        val html = scraping.withSemaphore { scraping.fetcher.get(url) }
        val jornadas = groupParser.parseJornadas(html)
        if (jornadas.isNotEmpty()) {
            cachedJornadas[groupId] = jornadas
            scraping.db.jornadaDao().deleteByGroupId(groupId)
            scraping.db.jornadaDao().insertAll(jornadas.map { JornadaEntity(groupId, it) })
        }
        return jornadas
    }

    override suspend fun getMatchResults(groupId: Int, jornada: Int): List<MatchResult> {
        val key = resultKey(groupId, jornada)
        val cacheKey = "results_${groupId}_$jornada"

        if (key in finalizedJornadas) {
            cachedResults[key]?.let { return it }
            val roomResults = scraping.db.matchResultDao().getByGroupAndJornada(groupId, jornada).map { it.toModel() }
            if (roomResults.isNotEmpty()) {
                cachedResults[key] = roomResults
                return roomResults
            }
        }

        val roomResultsColdStart = scraping.db.matchResultDao().getByGroupAndJornada(groupId, jornada).map { it.toModel() }
        if (roomResultsColdStart.isNotEmpty()) {
            val allFinalized = roomResultsColdStart.all { it.localScore != "--" && it.visitorScore != "--" }
            if (allFinalized) {
                finalizedJornadas.add(key)
                cachedResults[key] = roomResultsColdStart
                return roomResultsColdStart
            }
        }

        cachedResults[key]?.let { cached ->
            if (scraping.isCacheValid(cacheKey, TTL_RESULTS)) {
                return cached
            }
        }

        if (scraping.isCacheValid(cacheKey, TTL_RESULTS)) {
            val roomResults = scraping.db.matchResultDao().getByGroupAndJornada(groupId, jornada).map { it.toModel() }
            if (roomResults.isNotEmpty()) {
                cachedResults[key] = roomResults
                return roomResults
            }
        }

        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=$LEAGUE_ID&grupo=$groupId&jornada=$jornada"
        val html = scraping.withSemaphore { scraping.fetcher.get(url) }
        val results = matchResultParser.parse(html, jornada)

        if (results.isNotEmpty()) {
            cachedResults[key] = results
            val allFinalized = results.all { it.localScore != "--" && it.visitorScore != "--" }
            if (allFinalized) {
                finalizedJornadas.add(key)
            }
            scraping.db.matchResultDao().deleteByGroupAndJornada(groupId, jornada)
            scraping.db.matchResultDao().insertAll(results.map { MatchResultEntity.fromModel(groupId, it) })
            scraping.updateCacheTimestamp(cacheKey)
        }

        return results
    }

    override suspend fun getAllMatchResults(groupId: Int): Map<Int, List<MatchResult>> {
        val jornadas = getJornadas(groupId)
        if (jornadas.isEmpty()) return emptyMap()

        val result = mutableMapOf<Int, List<MatchResult>>()
        val toFetch = mutableListOf<Int>()

        for (j in jornadas) {
            val key = resultKey(groupId, j)
            if (key in finalizedJornadas) {
                cachedResults[key]?.let { result[j] = it }
            } else {
                toFetch.add(j)
            }
        }

        if (toFetch.isNotEmpty()) {
            val fetched = coroutineScope {
                toFetch.map { jornada ->
                    async {
                        jornada to runCatching { getMatchResults(groupId, jornada) }
                            .getOrElse { emptyList() }
                    }
                }.awaitAll().toMap()
            }
            result.putAll(fetched)
        }

        return result
    }

    override suspend fun refreshMatchResults(groupId: Int): Map<Int, List<MatchResult>> {
        val jornadas = cachedJornadas[groupId] ?: getJornadas(groupId)
        for (j in jornadas) {
            val key = resultKey(groupId, j)
            if (key !in finalizedJornadas) {
                cachedResults.remove(key)
                scraping.db.cacheTimestampDao().delete("results_${groupId}_$j")
            }
        }
        return getAllMatchResults(groupId)
    }

    companion object {
        private const val TTL_RESULTS = 30 * 60 * 1000L
    }
}
