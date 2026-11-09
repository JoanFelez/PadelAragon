package com.padelaragon.shared.data.repository

import com.padelaragon.shared.data.local.entity.StandingRowEntity
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.StandingRow
import com.padelaragon.shared.data.parser.StandingsParser
import com.padelaragon.shared.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.shared.data.repository.datasource.StandingsDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StandingsRepository(
    private val league: League,
    private val scraping: ScrapingService,
    private val standingsParser: StandingsParser = StandingsParser()
) : StandingsDataSource {

    private fun cacheKey(groupId: Int) = "standings_${league.id}_$groupId"

    override fun observeStandings(groupId: Int): Flow<List<StandingRow>> =
        scraping.db.standingRowDao().observeByGroupId(league.id, groupId).map { rows -> rows.map { it.toModel() } }

    override suspend fun getStandings(groupId: Int): List<StandingRow> {
        val cached = readCached(groupId)
        if (cached.isEmpty()) return fetch(groupId)
        val key = cacheKey(groupId)
        if (!scraping.isCacheValid(key, TTL_STANDINGS)) {
            scraping.revalidateInBackground(key) { fetch(groupId) }
        }
        return cached
    }

    override suspend fun refreshStandings(groupId: Int): List<StandingRow> = fetch(groupId)

    private suspend fun readCached(groupId: Int): List<StandingRow> =
        scraping.db.standingRowDao().getByGroupId(league.id, groupId).map { it.toModel() }

    private suspend fun fetch(groupId: Int): List<StandingRow> {
        val html = scraping.fetcher.post(
            "${BASE_URL}Ligas_Clasificacion.asp",
            mapOf("Liga" to league.id.toString(), "grupo" to groupId.toString())
        )
        val standings = standingsParser.parse(html)
        if (standings.isEmpty()) return readCached(groupId)
        scraping.writeIfChanged(cacheKey(groupId), standings) {
            scraping.db.standingRowDao().replaceForGroup(
                league.id, groupId, standings.map { StandingRowEntity.fromModel(league.id, groupId, it) }
            )
        }
        return standings
    }

    companion object {
        internal const val TTL_STANDINGS = 30 * 60 * 1000L
    }
}
