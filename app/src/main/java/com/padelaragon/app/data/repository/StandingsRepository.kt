package com.padelaragon.app.data.repository

import com.padelaragon.app.data.local.entity.StandingRowEntity
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.parser.StandingsParser
import com.padelaragon.app.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.app.data.repository.ScrapingService.Companion.LEAGUE_ID
import com.padelaragon.app.data.repository.datasource.StandingsDataSource
import java.util.concurrent.ConcurrentHashMap

class StandingsRepository(
    private val scraping: ScrapingService,
    private val standingsParser: StandingsParser = StandingsParser()
) : StandingsDataSource {

    private val cachedStandings = ConcurrentHashMap<Int, List<StandingRow>>()

    override suspend fun getStandings(groupId: Int): List<StandingRow> {
        cachedStandings[groupId]?.let { return it }

        val cacheKey = "standings_$groupId"
        if (scraping.isCacheValid(cacheKey, TTL_STANDINGS)) {
            val roomStandings = scraping.db.standingRowDao().getByGroupId(groupId).map { it.toModel() }
            if (roomStandings.isNotEmpty()) {
                cachedStandings[groupId] = roomStandings
                return roomStandings
            }
        }

        val url = "${BASE_URL}Ligas_Clasificacion.asp"
        val html = scraping.withSemaphore {
            scraping.fetcher.post(url, mapOf("Liga" to LEAGUE_ID.toString(), "grupo" to groupId.toString()))
        }
        val standings = standingsParser.parse(html)
        if (standings.isNotEmpty()) {
            cachedStandings[groupId] = standings
            scraping.db.standingRowDao().deleteByGroupId(groupId)
            scraping.db.standingRowDao().insertAll(standings.map { StandingRowEntity.fromModel(groupId, it) })
            scraping.updateCacheTimestamp(cacheKey)
        }
        return standings
    }

    override suspend fun refreshStandings(groupId: Int): List<StandingRow> {
        cachedStandings.remove(groupId)
        scraping.db.cacheTimestampDao().delete("standings_$groupId")
        return getStandings(groupId)
    }

    companion object {
        private const val TTL_STANDINGS = 30 * 60 * 1000L
    }
}
