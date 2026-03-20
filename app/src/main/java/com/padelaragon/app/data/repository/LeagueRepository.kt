package com.padelaragon.app.data.repository

import com.padelaragon.app.data.model.LeagueGroup
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.model.TeamInfo
import com.padelaragon.app.data.network.HtmlFetcher
import com.padelaragon.app.data.parser.GroupParser
import com.padelaragon.app.data.parser.MatchResultParser
import com.padelaragon.app.data.parser.StandingsParser
import com.padelaragon.app.data.parser.TeamDetailParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object LeagueRepository {
    private val fetcher = HtmlFetcher()
    private val groupParser = GroupParser()
    private val standingsParser = StandingsParser()
    private val matchResultParser = MatchResultParser()
    private val teamDetailParser = TeamDetailParser()

    // ── Caches ──────────────────────────────────────────────
    // Groups: stable for the season
    @Volatile
    private var cachedGroups: List<LeagueGroup>? = null

    // Jornadas per group: stable for the season (new jornadas may appear)
    private val cachedJornadas = ConcurrentHashMap<Int, List<Int>>()

    // Standings per group: changes as new matches are played
    private val cachedStandings = ConcurrentHashMap<Int, List<StandingRow>>()

    // Match results per (groupId, jornada): IMMUTABLE once all scores are set
    // Key = groupId * 100_000L + jornada
    private val cachedResults = ConcurrentHashMap<Long, List<MatchResult>>()
    private val cachedTeamDetails = ConcurrentHashMap<Int, com.padelaragon.app.data.model.TeamDetail>()

    // Track which jornadas have ALL results finalized (no "--" scores)
    private val finalizedJornadas = ConcurrentHashMap.newKeySet<Long>()

    // Semaphore to limit concurrent scraping requests (be polite to the server)
    private val scrapeSemaphore = Semaphore(10)

    private fun resultKey(groupId: Int, jornada: Int): Long =
        groupId.toLong() * 100_000 + jornada

    // ── Public API ──────────────────────────────────────────

    suspend fun getGroups(): List<LeagueGroup> {
        cachedGroups?.let { return it }
        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=$LEAGUE_ID"
        android.util.Log.d("LeagueRepo", "Fetching groups from: $url")
        val html = scrapeSemaphore.withPermit { fetcher.get(url) }
        val groups = groupParser.parse(html)
        cachedGroups = groups
        return groups
    }

    suspend fun getStandings(groupId: Int): List<StandingRow> {
        cachedStandings[groupId]?.let { return it }
        val url = "${BASE_URL}Ligas_Clasificacion.asp"
        val html = scrapeSemaphore.withPermit {
            fetcher.post(url, mapOf("Liga" to LEAGUE_ID.toString(), "grupo" to groupId.toString()))
        }
        val standings = standingsParser.parse(html)
        if (standings.isNotEmpty()) cachedStandings[groupId] = standings
        return standings
    }

    suspend fun getMatchResults(groupId: Int, jornada: Int): List<MatchResult> {
        val key = resultKey(groupId, jornada)

        // If this jornada is finalized (all scores set), return cached forever
        if (key in finalizedJornadas) {
            cachedResults[key]?.let { return it }
        }

        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=$LEAGUE_ID&grupo=$groupId&jornada=$jornada"
        val html = scrapeSemaphore.withPermit { fetcher.get(url) }
        val results = matchResultParser.parse(html, jornada)

        if (results.isNotEmpty()) {
            cachedResults[key] = results
            // Mark as finalized if ALL matches have real scores (no "--")
            val allFinalized = results.all { it.localScore != "--" && it.visitorScore != "--" }
            if (allFinalized) {
                finalizedJornadas.add(key)
            }
        }

        return results
    }

    suspend fun getJornadas(groupId: Int): List<Int> {
        cachedJornadas[groupId]?.let { return it }
        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=$LEAGUE_ID&grupo=$groupId"
        val html = scrapeSemaphore.withPermit { fetcher.get(url) }
        val jornadas = groupParser.parseJornadas(html)
        if (jornadas.isNotEmpty()) cachedJornadas[groupId] = jornadas
        return jornadas
    }

    /**
     * Fetch all match results for a group.
     * Smart: only fetches jornadas not already finalized in cache.
     */
    suspend fun getAllMatchResults(groupId: Int): Map<Int, List<MatchResult>> {
        val jornadas = getJornadas(groupId)
        if (jornadas.isEmpty()) return emptyMap()

        val result = mutableMapOf<Int, List<MatchResult>>()
        val toFetch = mutableListOf<Int>()

        for (j in jornadas) {
            val key = resultKey(groupId, j)
            if (key in finalizedJornadas) {
                // Finalized → use cache, never refetch
                cachedResults[key]?.let { result[j] = it }
            } else {
                // Not finalized → must refetch (scores may have been updated)
                toFetch.add(j)
            }
        }

        // Fetch all non-finalized jornadas in parallel
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

    /**
     * Prefetch standings and results for ALL groups in parallel.
     * Call from GroupListViewModel after groups are loaded.
     * Respects the semaphore to avoid overwhelming the server.
     */
    suspend fun prefetchAllGroups() {
        val groups = cachedGroups ?: return
        coroutineScope {
            groups.map { group ->
                launch {
                    runCatching {
                        // Fetch standings and all results for this group in parallel
                        coroutineScope {
                            launch { getStandings(group.id) }
                            launch { getAllMatchResults(group.id) }
                        }
                    }
                }
            }
        }
    }

    suspend fun prefetchGroups(groupIds: List<Int>) {
        val groups = cachedGroups ?: return
        val targetGroups = groups.filter { it.id in groupIds }
        coroutineScope {
            targetGroups.map { group ->
                launch {
                    runCatching {
                        coroutineScope {
                            launch { getStandings(group.id) }
                            launch { getAllMatchResults(group.id) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Refresh standings for a specific group (standings change as matches complete).
     */
    suspend fun refreshStandings(groupId: Int): List<StandingRow> {
        cachedStandings.remove(groupId)
        return getStandings(groupId)
    }

    suspend fun getTeamDetail(teamId: Int, teamHref: String = ""): com.padelaragon.app.data.model.TeamDetail? {
        cachedTeamDetails[teamId]?.let { return it }

        android.util.Log.d("LeagueRepo", "getTeamDetail(teamId=$teamId) received teamHref='$teamHref'")

        // Build list of URLs to try — prefer teamHref, only fall back to guesses if unavailable
        val urlsToTry = if (teamHref.isNotBlank()) {
            val fullHref = buildTeamDetailUrl(teamHref)
            android.util.Log.d("LeagueRepo", "Using teamHref URL: $fullHref")
            listOf(fullHref)
        } else {
            android.util.Log.d("LeagueRepo", "No teamHref, trying fallback URLs")
            listOf(
                "${BASE_URL}Ligas_FichaEquipo.asp?Liga=$LEAGUE_ID&IdEquipo=$teamId",
                "${BASE_URL}Ligas_Equipo.asp?Liga=$LEAGUE_ID&IdEquipo=$teamId",
                "${BASE_URL}Ligas_Clasificacion.asp?Liga=$LEAGUE_ID&IdEquipo=$teamId"
            )
        }

        for (url in urlsToTry) {
            val detail = runCatching {
                android.util.Log.d("LeagueRepo", "Trying team detail URL: $url")
                val response = scrapeSemaphore.withPermit { fetcher.getWithStatus(url) }
                android.util.Log.d(
                    "LeagueRepo",
                    "Team detail HTTP ${response.statusCode} from $url (${response.body.length} chars)"
                )
                android.util.Log.d("LeagueRepo", "Response: ${response.body.length} chars")

                val parsed = teamDetailParser.parse(response.body)
                android.util.Log.d("LeagueRepo", "Parsed: players=${parsed.players.size}, captain=${parsed.captain}, category=${parsed.category}")
                parsed
            }.onFailure { e ->
                android.util.Log.w("LeagueRepo", "Failed team detail URL: $url - ${e.message}", e)
            }.getOrNull()

            if (detail != null && (detail.players.isNotEmpty() || detail.captain != null)) {
                cachedTeamDetails[teamId] = detail
                return detail
            }
        }

        android.util.Log.w("LeagueRepo", "No team detail found for teamId=$teamId after trying ${urlsToTry.size} URLs")
        return null
    }

    private fun buildTeamDetailUrl(teamHref: String): String {
        val href = teamHref.trim()
        if (href.startsWith("http://", ignoreCase = true) || href.startsWith("https://", ignoreCase = true)) {
            return href
        }

        if (href.startsWith("//")) {
            return "https:$href"
        }

        if (href.contains("padelfederacion.es", ignoreCase = true)) {
            return if (href.startsWith("/")) {
                "https://padelfederacion.es$href"
            } else {
                "https://$href"
            }
        }

        return URI(BASE_URL).resolve(href).toString()
    }

    /**
     * Fast team info when the groupId is already known (from navigation).
     * Avoids searching all groups' standings — only fetches the known group.
     * Falls back to full search if team not found in the specified group.
     */
    suspend fun getTeamInfoForGroup(teamId: Int, teamName: String, groupId: Int): TeamInfo? {
        val groups = getGroups()
        val groupName = groups.find { it.id == groupId }?.name ?: "Grupo"

        // Get standings for just this group (1 HTTP call or cache hit)
        val standings = getStandings(groupId)
        val teamStanding = standings.find { it.teamId == teamId }

        // If team not found in this group, fall back to full search
        if (teamStanding == null) {
            android.util.Log.w("LeagueRepo", "Team $teamId not found in group $groupId, falling back to full search")
            return getTeamInfo(teamId, teamName)
        }

        // Get match results for this group
        val allResults = getAllMatchResults(groupId)
        val teamMatches = allResults.values
            .flatten()
            .filter { it.localTeamId == teamId || it.visitorTeamId == teamId }
            .sortedBy { it.jornada }

        val teamDetail = runCatching { getTeamDetail(teamId, teamStanding.teamHref) }.getOrNull()

        return TeamInfo(
            teamId = teamId,
            teamName = teamName,
            groupName = groupName,
            groupId = groupId,
            standing = teamStanding,
            matches = teamMatches,
            teamDetail = teamDetail
        )
    }

    /**
     * Aggregate all info about a specific team from cached data.
     * Searches all groups' standings + results to find this team.
     * If data isn't cached yet, fetches it.
     */
    suspend fun getTeamInfo(teamId: Int, teamName: String): TeamInfo? {
        val groups = getGroups()

        // Find which group this team belongs to by checking cached standings
        // or fetching them
        var teamStanding: StandingRow? = null
        var teamGroupId: Int? = null
        var teamGroupName: String? = null

        // Search all groups in parallel for this team
        coroutineScope {
            groups.map { group ->
                async {
                    val standings = getStandings(group.id)
                    val found = standings.find { it.teamId == teamId }
                    if (found != null) {
                        Triple(found, group.id, group.name)
                    } else null
                }
            }.awaitAll().filterNotNull().firstOrNull()?.let { (standing, gId, gName) ->
                teamStanding = standing
                teamGroupId = gId
                teamGroupName = gName
            }
        }

        val groupId = teamGroupId ?: return null
        val groupName = teamGroupName ?: return null

        // Get all match results for this group
        val allResults = getAllMatchResults(groupId)

        // Filter matches where this team participated
        val teamMatches = allResults.values
            .flatten()
            .filter { it.localTeamId == teamId || it.visitorTeamId == teamId }
            .sortedBy { it.jornada }

        val teamDetail = runCatching { getTeamDetail(teamId, teamStanding?.teamHref.orEmpty()) }.getOrNull()

        return TeamInfo(
            teamId = teamId,
            teamName = teamName,
            groupName = groupName,
            groupId = groupId,
            standing = teamStanding,
            matches = teamMatches,
            teamDetail = teamDetail
        )
    }

    private const val BASE_URL = "https://padelfederacion.es/pAGINAS/ARAPADEL/"
    private const val LEAGUE_ID = 27951
}
