package com.padelaragon.shared.data.repository

import com.padelaragon.shared.data.local.entity.JornadaEntity
import com.padelaragon.shared.data.local.entity.MatchResultEntity
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.parser.GroupParser
import com.padelaragon.shared.data.parser.MatchResultParser
import com.padelaragon.shared.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.shared.data.repository.datasource.MatchResultDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MatchResultRepository(
    private val league: League,
    private val scraping: ScrapingService,
    private val matchResultParser: MatchResultParser = MatchResultParser(),
    private val groupParser: GroupParser = GroupParser()
) : MatchResultDataSource {

    private fun jornadasKey(groupId: Int) = "jornadas_${league.id}_$groupId"
    private fun resultsKey(groupId: Int, jornada: Int) = "results_${league.id}_${groupId}_$jornada"

    /** A jornada whose matches all have a score can never change again. */
    private fun List<MatchResult>.isFinalized() =
        isNotEmpty() && all { it.localScore != "--" && it.visitorScore != "--" }

    override fun observeMatchResults(groupId: Int): Flow<Map<Int, List<MatchResult>>> =
        scraping.db.matchResultDao().observeByGroupId(league.id, groupId).map { rows ->
            rows.map { it.toModel() }.groupBy { it.jornada }
        }

    override suspend fun getJornadas(groupId: Int): List<Int> {
        val cached = scraping.db.jornadaDao().getByGroupId(league.id, groupId)
        if (cached.isEmpty()) return fetchJornadas(groupId)
        val key = jornadasKey(groupId)
        if (!scraping.isCacheValid(key, GroupRepository.TTL_GROUPS)) {
            scraping.revalidateInBackground(key) { fetchJornadas(groupId) }
        }
        return cached
    }

    override suspend fun getMatchResults(groupId: Int, jornada: Int): List<MatchResult> {
        val cached = readCached(groupId, jornada)
        if (cached.isEmpty()) return fetchResults(groupId, jornada)
        if (cached.isFinalized()) return cached
        val key = resultsKey(groupId, jornada)
        if (!scraping.isCacheValid(key, TTL_RESULTS)) {
            scraping.revalidateInBackground(key) { fetchResults(groupId, jornada) }
        }
        return cached
    }

    override suspend fun getAllMatchResults(groupId: Int): Map<Int, List<MatchResult>> {
        val jornadas = getJornadas(groupId)
        if (jornadas.isEmpty()) return emptyMap()
        return coroutineScope {
            jornadas.map { jornada ->
                async { jornada to runCatching { getMatchResults(groupId, jornada) }.getOrElse { emptyList() } }
            }.awaitAll().toMap()
        }
    }

    override suspend fun refreshMatchResults(groupId: Int): Map<Int, List<MatchResult>> {
        val jornadas = getJornadas(groupId)
        return coroutineScope {
            jornadas.map { jornada ->
                async {
                    val cached = readCached(groupId, jornada)
                    val results = if (cached.isFinalized()) {
                        cached
                    } else {
                        runCatching { fetchResults(groupId, jornada) }.getOrElse { cached }
                    }
                    jornada to results
                }
            }.awaitAll().toMap()
        }
    }

    private suspend fun readCached(groupId: Int, jornada: Int): List<MatchResult> =
        scraping.db.matchResultDao().getByGroupAndJornada(league.id, groupId, jornada).map { it.toModel() }

    private suspend fun fetchJornadas(groupId: Int): List<Int> {
        val html = scraping.fetcher.get("${BASE_URL}Ligas_Calendario.asp?Liga=${league.id}&grupo=$groupId")
        val jornadas = groupParser.parseJornadas(html)
        if (jornadas.isEmpty()) return scraping.db.jornadaDao().getByGroupId(league.id, groupId)
        scraping.writeIfChanged(jornadasKey(groupId), jornadas) {
            scraping.db.jornadaDao().replaceForGroup(
                league.id, groupId, jornadas.map { JornadaEntity(league.id, groupId, it) }
            )
        }
        return jornadas
    }

    private suspend fun fetchResults(groupId: Int, jornada: Int): List<MatchResult> {
        val html = scraping.fetcher.get(
            "${BASE_URL}Ligas_Calendario.asp?Liga=${league.id}&grupo=$groupId&jornada=$jornada"
        )
        val results = matchResultParser.parse(html, jornada)
        if (results.isEmpty()) return readCached(groupId, jornada)
        scraping.writeIfChanged(resultsKey(groupId, jornada), results) {
            scraping.db.matchResultDao().replaceForJornada(
                league.id, groupId, jornada, results.map { MatchResultEntity.fromModel(league.id, groupId, it) }
            )
        }
        return results
    }

    companion object {
        internal const val TTL_RESULTS = 30 * 60 * 1000L
    }
}
