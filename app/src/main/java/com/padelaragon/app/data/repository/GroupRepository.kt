package com.padelaragon.app.data.repository

import com.padelaragon.app.data.local.entity.LeagueGroupEntity
import com.padelaragon.app.data.model.LeagueGroup
import com.padelaragon.app.data.parser.GroupParser
import com.padelaragon.app.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.app.data.repository.ScrapingService.Companion.LEAGUE_ID
import com.padelaragon.app.data.repository.datasource.GroupDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class GroupRepository(
    private val scraping: ScrapingService,
    private val groupParser: GroupParser = GroupParser(),
    private val standingsRepo: StandingsRepository,
    private val matchResultRepo: MatchResultRepository
) : GroupDataSource {

    @Volatile
    private var cachedGroups: List<LeagueGroup>? = null

    override suspend fun getGroups(): List<LeagueGroup> {
        cachedGroups?.let { return it }

        val roomGroups = scraping.db.leagueGroupDao().getAll().map { it.toModel() }
        if (roomGroups.isNotEmpty()) {
            cachedGroups = roomGroups
            return roomGroups
        }

        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=$LEAGUE_ID"
        android.util.Log.d("GroupRepo", "Fetching groups from: $url")
        val html = scraping.withSemaphore { scraping.fetcher.get(url) }
        val groups = groupParser.parse(html)
        cachedGroups = groups

        scraping.db.leagueGroupDao().deleteAll()
        scraping.db.leagueGroupDao().insertAll(groups.map { LeagueGroupEntity.fromModel(it) })

        return groups
    }

    override suspend fun refreshGroups(): List<LeagueGroup> {
        cachedGroups = null
        scraping.db.leagueGroupDao().deleteAll()
        return getGroups()
    }

    override suspend fun prefetchAllGroups() {
        val groups = cachedGroups ?: return
        coroutineScope {
            groups.map { group ->
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

    override suspend fun prefetchGroups(groupIds: List<Int>) {
        val groups = cachedGroups ?: return
        val targetGroups = groups.filter { it.id in groupIds }
        coroutineScope {
            targetGroups.map { group ->
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

    fun getCachedGroups(): List<LeagueGroup>? = cachedGroups
}
