package com.padelaragon.app.data.repository

import com.padelaragon.app.data.local.entity.PlayerEntity
import com.padelaragon.app.data.local.entity.TeamDetailEntity
import com.padelaragon.app.data.model.TeamDetail
import com.padelaragon.app.data.model.TeamInfo
import com.padelaragon.app.data.parser.TeamDetailParser
import com.padelaragon.app.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.app.data.repository.ScrapingService.Companion.LEAGUE_ID
import com.padelaragon.app.data.repository.datasource.GroupDataSource
import com.padelaragon.app.data.repository.datasource.MatchResultDataSource
import com.padelaragon.app.data.repository.datasource.StandingsDataSource
import com.padelaragon.app.data.repository.datasource.TeamDataSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

class TeamDetailRepository(
    private val scraping: ScrapingService,
    private val teamDetailParser: TeamDetailParser = TeamDetailParser(),
    private val groupDataSource: GroupDataSource,
    private val standingsDataSource: StandingsDataSource,
    private val matchResultDataSource: MatchResultDataSource
) : TeamDataSource {

    private val cachedTeamDetails = ConcurrentHashMap<Int, TeamDetail>()

    override suspend fun getTeamDetail(teamId: Int, teamHref: String): TeamDetail? {
        cachedTeamDetails[teamId]?.let { return it }

        val entity = scraping.db.teamDetailDao().getByTeamId(teamId)
        if (entity != null) {
            val players = scraping.db.teamDetailDao().getPlayersByTeamId(teamId).map { it.toModel() }
            val detail = TeamDetail(category = entity.category, captainName = entity.captainName, players = players)
            cachedTeamDetails[teamId] = detail
            return detail
        }

        android.util.Log.d("TeamDetailRepo", "getTeamDetail(teamId=$teamId) received teamHref='$teamHref'")

        val urlsToTry = if (teamHref.isNotBlank()) {
            listOf(buildTeamDetailUrl(teamHref))
        } else {
            listOf(
                "${BASE_URL}Ligas_FichaEquipo.asp?Liga=$LEAGUE_ID&IdEquipo=$teamId",
                "${BASE_URL}Ligas_Equipo.asp?Liga=$LEAGUE_ID&IdEquipo=$teamId",
                "${BASE_URL}Ligas_Clasificacion.asp?Liga=$LEAGUE_ID&IdEquipo=$teamId"
            )
        }

        if (urlsToTry.size > 1) {
            val details = coroutineScope {
                urlsToTry.map { url ->
                    async {
                        runCatching {
                            val response = scraping.withSemaphore { scraping.fetcher.getWithStatus(url) }
                            if (response.statusCode == 200 && response.body.isNotBlank()) {
                                teamDetailParser.parse(response.body)
                            } else null
                        }.onFailure { e ->
                            android.util.Log.w("TeamDetailRepo", "Failed: $url - ${e.message}", e)
                        }.getOrNull()
                    }
                }.awaitAll()
            }

            val detail = details.firstOrNull { it != null && (it.players.isNotEmpty() || it.captain != null) }
            if (detail != null) {
                cachedTeamDetails[teamId] = detail
                persistTeamDetail(teamId, detail)
                return detail
            }
        } else {
            val url = urlsToTry.first()
            val detail = runCatching {
                val response = scraping.withSemaphore { scraping.fetcher.getWithStatus(url) }
                teamDetailParser.parse(response.body)
            }.onFailure { e ->
                android.util.Log.w("TeamDetailRepo", "Failed: $url - ${e.message}", e)
            }.getOrNull()

            if (detail != null && (detail.players.isNotEmpty() || detail.captain != null)) {
                cachedTeamDetails[teamId] = detail
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

        var allResults = emptyMap<Int, List<com.padelaragon.app.data.model.MatchResult>>()
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

        var teamStanding: com.padelaragon.app.data.model.StandingRow? = null
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

        var allResults = emptyMap<Int, List<com.padelaragon.app.data.model.MatchResult>>()
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
        scraping.db.teamDetailDao().insertTeamWithPlayers(
            TeamDetailEntity(teamId = teamId, category = detail.category, captainName = detail.captainName),
            detail.players.map { PlayerEntity.fromModel(teamId, it) }
        )
    }

    private fun buildTeamDetailUrl(teamHref: String): String {
        val href = teamHref.trim()
        if (href.startsWith("http://", ignoreCase = true) || href.startsWith("https://", ignoreCase = true)) return href
        if (href.startsWith("//")) return "https:$href"
        if (href.contains("padelfederacion.es", ignoreCase = true)) {
            return if (href.startsWith("/")) "https://padelfederacion.es$href" else "https://$href"
        }
        return URI(BASE_URL).resolve(href).toString()
    }
}
