package com.padelaragon.shared.data.repository

import com.padelaragon.shared.data.local.entity.LeagueGroupEntity
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.LeagueGroup
import com.padelaragon.shared.data.parser.GroupParser
import com.padelaragon.shared.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.shared.data.repository.datasource.GroupDataSource
import com.padelaragon.shared.util.Logger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class GroupRepository(
    private val league: League,
    private val scraping: ScrapingService,
    private val groupParser: GroupParser = GroupParser(),
    private val standingsRepo: StandingsRepository,
    private val matchResultRepo: MatchResultRepository
) : GroupDataSource {

    private val cacheKey = "groups_${league.id}"

    override fun observeGroups(): Flow<List<LeagueGroup>> =
        scraping.db.leagueGroupDao().observeByLeagueId(league.id).map { rows -> rows.map { it.toModel() } }

    override suspend fun getGroups(): List<LeagueGroup> {
        val cached = readCached()
        if (cached.isEmpty()) return fetch()
        if (!scraping.isCacheValid(cacheKey, TTL_GROUPS)) {
            scraping.revalidateInBackground(cacheKey) { fetch() }
        }
        return cached
    }

    override suspend fun refreshGroups(): List<LeagueGroup> = fetch()

    override suspend fun prefetchAllGroups() = prefetch(getGroups())

    override suspend fun prefetchGroups(groupIds: List<Int>) =
        prefetch(readCached().filter { it.id in groupIds })

    private suspend fun prefetch(groups: List<LeagueGroup>) {
        coroutineScope {
            groups.forEach { group ->
                launch {
                    runCatching {
                        coroutineScope {
                            launch { standingsRepo.getStandings(group.id) }
                            launch { matchResultRepo.getAllMatchResults(group.id) }
                        }
                    }
                }
            }
        }
    }

    private suspend fun readCached(): List<LeagueGroup> =
        scraping.db.leagueGroupDao().getByLeagueId(league.id).map { it.toModel() }

    private suspend fun fetch(): List<LeagueGroup> {
        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=${league.id}"
        Logger.d("GroupRepo", "Fetching groups from: $url")
        val groups = groupParser.parse(scraping.fetcher.get(url))
        scraping.writeIfChanged(cacheKey, groups) {
            scraping.db.leagueGroupDao().replaceForLeague(
                league.id, groups.map { LeagueGroupEntity.fromModel(league.id, it) }
            )
        }
        return groups
    }

    companion object {
        internal const val TTL_GROUPS = 6 * 60 * 60 * 1000L
    }
}
