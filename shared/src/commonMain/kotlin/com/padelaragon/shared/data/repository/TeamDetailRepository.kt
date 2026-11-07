package com.padelaragon.shared.data.repository

import com.padelaragon.shared.util.Logger

import com.padelaragon.shared.data.local.entity.PlayerEntity
import com.padelaragon.shared.data.local.entity.TeamDetailEntity
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.TeamDetail
import com.padelaragon.shared.data.model.TeamInfo
import com.padelaragon.shared.data.parser.TeamDetailParser
import com.padelaragon.shared.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.shared.data.repository.datasource.GroupDataSource
import com.padelaragon.shared.data.repository.datasource.MatchResultDataSource
import com.padelaragon.shared.data.repository.datasource.StandingsDataSource
import com.padelaragon.shared.data.repository.datasource.TeamDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class TeamDetailRepository(
    private val league: League,
    private val scraping: ScrapingService,
    private val teamDetailParser: TeamDetailParser = TeamDetailParser(),
    private val groupDataSource: GroupDataSource,
    private val standingsDataSource: StandingsDataSource,
    private val matchResultDataSource: MatchResultDataSource
) : TeamDataSource {

    private fun cacheKey(teamId: Int) = "team_${league.id}_$teamId"

    override suspend fun getTeamDetail(teamId: Int, teamHref: String): TeamDetail? {
        val cached = readCached(teamId)
        if (cached == null) return fetchTeamDetail(teamId, teamHref)
        val key = cacheKey(teamId)
        if (!scraping.isCacheValid(key, TTL_TEAM)) {
            scraping.revalidateInBackground(key) { fetchTeamDetail(teamId, teamHref) }
        }
        return cached
    }

    private suspend fun readCached(teamId: Int): TeamDetail? {
        val entity = scraping.db.teamDetailDao().getByTeamId(league.id, teamId) ?: return null
        val players = scraping.db.teamDetailDao().getPlayersByTeamId(league.id, teamId).map { it.toModel() }
        return TeamDetail(category = entity.category, captainName = entity.captainName, players = players)
    }

    private suspend fun fetchTeamDetail(teamId: Int, teamHref: String): TeamDetail? {
        Logger.d("TeamDetailRepo", "getTeamDetail(teamId=$teamId) received teamHref='$teamHref'")

        val urlsToTry = if (teamHref.isNotBlank()) {
            listOf(buildTeamDetailUrl(teamHref))
        } else {
            listOf(
                "${BASE_URL}Ligas_FichaEquipo.asp?Liga=${league.id}&IdEquipo=$teamId",
                "${BASE_URL}Ligas_Equipo.asp?Liga=${league.id}&IdEquipo=$teamId",
                "${BASE_URL}Ligas_Clasificacion.asp?Liga=${league.id}&IdEquipo=$teamId"
            )
        }

        if (urlsToTry.size > 1) {
            val details = coroutineScope {
                urlsToTry.map { url ->
                    async {
                        runCatching {
                            val response = scraping.fetcher.getWithStatus(url)
                            if (response.statusCode == 200 && response.body.isNotBlank()) {
                                teamDetailParser.parse(response.body)
                            } else null
                        }.onFailure { e ->
                            Logger.w("TeamDetailRepo", "Failed: $url - ${e.message}", e)
                        }.getOrNull()
                    }
                }.awaitAll()
            }

            val detail = details.firstOrNull { it != null && (it.players.isNotEmpty() || it.captain != null) }
            if (detail != null) {
                persistTeamDetail(teamId, detail)
                return detail
            }
        } else {
            val url = urlsToTry.first()
            val detail = runCatching {
                val response = scraping.fetcher.getWithStatus(url)
                teamDetailParser.parse(response.body)
            }.onFailure { e ->
                Logger.w("TeamDetailRepo", "Failed: $url - ${e.message}", e)
            }.getOrNull()

            if (detail != null && (detail.players.isNotEmpty() || detail.captain != null)) {
                persistTeamDetail(teamId, detail)
                return detail
            }
        }

        return null
    }

    override suspend fun getTeamInfoForGroup(teamId: Int, teamName: String, groupId: Int): TeamInfo? {
        val groups = groupDataSource.getGroups()
        val groupName = groups.find { it.id == groupId }?.name ?: "Grupo"

        val standings = standingsDataSource.getStandings(groupId)
        val teamStanding = standings.find { it.teamId == teamId }

        if (teamStanding == null) {
            return getTeamInfo(teamId, teamName)
        }

        var allResults = emptyMap<Int, List<com.padelaragon.shared.data.model.MatchResult>>()
        var teamDetail: TeamDetail? = null

        coroutineScope {
            val resultsDeferred = async { matchResultDataSource.getAllMatchResults(groupId) }
            val detailDeferred = async { runCatching { getTeamDetail(teamId, teamStanding.teamHref) }.getOrNull() }
            allResults = resultsDeferred.await()
            teamDetail = detailDeferred.await()
        }

        val teamMatches = allResults.values
            .flatten()
            .filter { it.localTeamId == teamId || it.visitorTeamId == teamId }
            .sortedBy { it.jornada }

        return TeamInfo(
            teamId = teamId, teamName = teamName, groupName = groupName,
            groupId = groupId, standing = teamStanding, matches = teamMatches, teamDetail = teamDetail
        )
    }

    override suspend fun getTeamInfo(teamId: Int, teamName: String): TeamInfo? {
        val groups = groupDataSource.getGroups()

        var teamStanding: com.padelaragon.shared.data.model.StandingRow? = null
        var teamGroupId: Int? = null
        var teamGroupName: String? = null

        coroutineScope {
            groups.map { group ->
                async {
                    val standings = standingsDataSource.getStandings(group.id)
                    val found = standings.find { it.teamId == teamId }
                    if (found != null) Triple(found, group.id, group.name) else null
                }
            }.awaitAll().filterNotNull().firstOrNull()?.let { (standing, gId, gName) ->
                teamStanding = standing
                teamGroupId = gId
                teamGroupName = gName
            }
        }

        val groupId = teamGroupId ?: return null
        val groupName = teamGroupName ?: return null

        var allResults = emptyMap<Int, List<com.padelaragon.shared.data.model.MatchResult>>()
        var teamDetail: TeamDetail? = null

        coroutineScope {
            val resultsDeferred = async { matchResultDataSource.getAllMatchResults(groupId) }
            val detailDeferred = async { runCatching { getTeamDetail(teamId, teamStanding?.teamHref.orEmpty()) }.getOrNull() }
            allResults = resultsDeferred.await()
            teamDetail = detailDeferred.await()
        }

        val teamMatches = allResults.values
            .flatten()
            .filter { it.localTeamId == teamId || it.visitorTeamId == teamId }
            .sortedBy { it.jornada }

        return TeamInfo(
            teamId = teamId, teamName = teamName, groupName = groupName,
            groupId = groupId, standing = teamStanding, matches = teamMatches, teamDetail = teamDetail
        )
    }

    private suspend fun persistTeamDetail(teamId: Int, detail: TeamDetail) {
        scraping.writeIfChanged(cacheKey(teamId), detail) {
            scraping.db.teamDetailDao().insertTeamWithPlayers(
                TeamDetailEntity(leagueId = league.id, teamId = teamId, category = detail.category, captainName = detail.captainName),
                detail.players.map { PlayerEntity.fromModel(league.id, teamId, it) }
            )
        }
    }

    private fun buildTeamDetailUrl(teamHref: String): String {
        val href = teamHref.trim()
        if (href.startsWith("http://", ignoreCase = true) || href.startsWith("https://", ignoreCase = true)) return href
        if (href.startsWith("//")) return "https:$href"
        if (href.contains("padelfederacion.es", ignoreCase = true)) {
            return if (href.startsWith("/")) "https://padelfederacion.es$href" else "https://$href"
        }
        return resolveUrl(BASE_URL, href)
    }

    companion object {
        internal const val TTL_TEAM = 24 * 60 * 60 * 1000L
    }
}
