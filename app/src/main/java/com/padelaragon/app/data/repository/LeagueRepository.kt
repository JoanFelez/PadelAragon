package com.padelaragon.app.data.repository

import com.padelaragon.app.data.local.AppDatabase
import com.padelaragon.app.data.local.entity.CacheTimestamp
import com.padelaragon.app.data.local.entity.LeagueGroupEntity
import com.padelaragon.app.data.local.entity.MatchResultEntity
import com.padelaragon.app.data.local.entity.PlayerEntity
import com.padelaragon.app.data.local.entity.StandingRowEntity
import com.padelaragon.app.data.local.entity.TeamDetailEntity
import com.padelaragon.app.data.model.LeagueGroup
import com.padelaragon.app.data.model.MatchResult
import com.padelaragon.app.data.model.StandingRow
import com.padelaragon.app.data.model.TeamDetail
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
    private var db: AppDatabase? = null

    fun init(database: AppDatabase) {
        db = database
    }

    // Caches
    // Groups: stable for the season
    @Volatile
    private var cachedGroups: List<LeagueGroup>? = null

    // Jornadas per group: stable for the season (new jornadas may appear)
    private val cachedJornadas = ConcurrentHashMap<Int, List<Int>>()

    // Standings per group: changes as new matches are played
    private val cachedStandings = ConcurrentHashMap<Int, List<StandingRow>>()

    // Match results per (groupId, jornada): immutable once all scores are set
    // Key = groupId * 100_000L + jornada
    private val cachedResults = ConcurrentHashMap<Long, List<MatchResult>>()
    private val cachedTeamDetails = ConcurrentHashMap<Int, TeamDetail>()

    // Track which jornadas have all results finalized (no "--" scores)
    private val finalizedJornadas = ConcurrentHashMap.newKeySet<Long>()

    // Semaphore to limit concurrent scraping requests (be polite to the server)
    private val scrapeSemaphore = Semaphore(10)

    private fun resultKey(groupId: Int, jornada: Int): Long =
        groupId.toLong() * 100_000 + jornada

    private suspend fun isCacheValid(key: String, ttl: Long): Boolean {
        val timestamp = db?.cacheTimestampDao()?.getTimestamp(key) ?: return false
        return (System.currentTimeMillis() - timestamp) < ttl
    }

    private suspend fun updateCacheTimestamp(key: String) {
        db?.cacheTimestampDao()?.set(CacheTimestamp(key, System.currentTimeMillis()))
    }

    suspend fun getGroups(): List<LeagueGroup> {
        // 1. In-memory cache
        cachedGroups?.let { return it }

        // 2. Room cache (cold start)
        val database = db
        if (database != null && isCacheValid(CACHE_KEY_GROUPS, TTL_GROUPS)) {
            val roomGroups = database.leagueGroupDao().getAll().map { it.toModel() }
            if (roomGroups.isNotEmpty()) {
                cachedGroups = roomGroups
                return roomGroups
            }
        }

        // 3. Network fetch
        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=$LEAGUE_ID"
        android.util.Log.d("LeagueRepo", "Fetching groups from: $url")
        val html = scrapeSemaphore.withPermit { fetcher.get(url) }
        val groups = groupParser.parse(html)
        cachedGroups = groups

        // Persist to Room
        database?.let { roomDb ->
            roomDb.leagueGroupDao().deleteAll()
            roomDb.leagueGroupDao().insertAll(groups.map { LeagueGroupEntity.fromModel(it) })
            updateCacheTimestamp(CACHE_KEY_GROUPS)
        }

        return groups
    }

    suspend fun getStandings(groupId: Int): List<StandingRow> {
        // 1. In-memory cache
        cachedStandings[groupId]?.let { return it }

        // 2. Room cache
        val database = db
        val cacheKey = "standings_$groupId"
        if (database != null && isCacheValid(cacheKey, TTL_STANDINGS)) {
            val roomStandings = database.standingRowDao().getByGroupId(groupId).map { it.toModel() }
            if (roomStandings.isNotEmpty()) {
                cachedStandings[groupId] = roomStandings
                return roomStandings
            }
        }

        // 3. Network fetch
        val url = "${BASE_URL}Ligas_Clasificacion.asp"
        val html = scrapeSemaphore.withPermit {
            fetcher.post(url, mapOf("Liga" to LEAGUE_ID.toString(), "grupo" to groupId.toString()))
        }
        val standings = standingsParser.parse(html)
        if (standings.isNotEmpty()) {
            cachedStandings[groupId] = standings

            // Persist to Room
            database?.let { roomDb ->
                roomDb.standingRowDao().deleteByGroupId(groupId)
                roomDb.standingRowDao().insertAll(standings.map { StandingRowEntity.fromModel(groupId, it) })
                updateCacheTimestamp(cacheKey)
            }
        }
        return standings
    }

    suspend fun getMatchResults(groupId: Int, jornada: Int): List<MatchResult> {
        val key = resultKey(groupId, jornada)
        val database = db
        val cacheKey = "results_${groupId}_$jornada"

        // If this jornada is finalized (all scores set), return cached forever.
        if (key in finalizedJornadas) {
            cachedResults[key]?.let { return it }

            // Cold start: finalized jornadas can be restored from Room.
            if (database != null) {
                val roomResults = database.matchResultDao().getByGroupAndJornada(groupId, jornada).map { it.toModel() }
                if (roomResults.isNotEmpty()) {
                    cachedResults[key] = roomResults
                    return roomResults
                }
            }
        }

        // Cold start restoration: if Room already has finalized results, keep forever.
        if (database != null) {
            val roomResults = database.matchResultDao().getByGroupAndJornada(groupId, jornada).map { it.toModel() }
            if (roomResults.isNotEmpty()) {
                val allFinalized = roomResults.all { it.localScore != "--" && it.visitorScore != "--" }
                if (allFinalized) {
                    finalizedJornadas.add(key)
                    cachedResults[key] = roomResults
                    return roomResults
                }
            }
        }

        // Non-finalized: check in-memory + TTL.
        cachedResults[key]?.let { cached ->
            if (isCacheValid(cacheKey, TTL_RESULTS)) {
                return cached
            }
        }

        // Cold start: check Room with TTL for non-finalized jornadas.
        if (database != null && isCacheValid(cacheKey, TTL_RESULTS)) {
            val roomResults = database.matchResultDao().getByGroupAndJornada(groupId, jornada).map { it.toModel() }
            if (roomResults.isNotEmpty()) {
                cachedResults[key] = roomResults
                return roomResults
            }
        }

        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=$LEAGUE_ID&grupo=$groupId&jornada=$jornada"
        val html = scrapeSemaphore.withPermit { fetcher.get(url) }
        val results = matchResultParser.parse(html, jornada)

        if (results.isNotEmpty()) {
            cachedResults[key] = results
            // Mark as finalized if all matches have real scores (no "--")
            val allFinalized = results.all { it.localScore != "--" && it.visitorScore != "--" }
            if (allFinalized) {
                finalizedJornadas.add(key)
            }

            // Persist to Room
            database?.let { roomDb ->
                roomDb.matchResultDao().deleteByGroupAndJornada(groupId, jornada)
                roomDb.matchResultDao().insertAll(results.map { MatchResultEntity.fromModel(groupId, it) })
                updateCacheTimestamp(cacheKey)
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
                // Finalized -> use cache, never refetch
                cachedResults[key]?.let { result[j] = it }
            } else {
                // Not finalized -> may need refresh
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
     * Prefetch standings and results for all groups in parallel.
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
        db?.cacheTimestampDao()?.delete("standings_$groupId")
        return getStandings(groupId)
    }

    suspend fun refreshGroups(): List<LeagueGroup> {
        cachedGroups = null
        db?.cacheTimestampDao()?.delete(CACHE_KEY_GROUPS)
        return getGroups()
    }

    suspend fun refreshMatchResults(groupId: Int): Map<Int, List<MatchResult>> {
        // Clear non-finalized results
        val jornadas = cachedJornadas[groupId] ?: getJornadas(groupId)
        for (j in jornadas) {
            val key = resultKey(groupId, j)
            if (key !in finalizedJornadas) {
                cachedResults.remove(key)
                db?.cacheTimestampDao()?.delete("results_${groupId}_$j")
            }
        }
        return getAllMatchResults(groupId)
    }

    suspend fun getTeamDetail(teamId: Int, teamHref: String = ""): TeamDetail? {
        cachedTeamDetails[teamId]?.let { return it }

        val database = db
        val cacheKey = "team_$teamId"
        if (database != null && isCacheValid(cacheKey, TTL_TEAM_DETAIL)) {
            val entity = database.teamDetailDao().getByTeamId(teamId)
            if (entity != null) {
                val players = database.teamDetailDao().getPlayersByTeamId(teamId).map { it.toModel() }
                val detail = TeamDetail(category = entity.category, captainName = entity.captainName, players = players)
                cachedTeamDetails[teamId] = detail
                return detail
            }
        }

        android.util.Log.d("LeagueRepo", "getTeamDetail(teamId=$teamId) received teamHref='$teamHref'")

        // Build list of URLs to try - prefer teamHref, only fall back to guesses if unavailable
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

                database?.let { roomDb ->
                    roomDb.teamDetailDao().insertTeamWithPlayers(
                        TeamDetailEntity(teamId = teamId, category = detail.category, captainName = detail.captainName),
                        detail.players.map { PlayerEntity.fromModel(teamId, it) }
                    )
                    updateCacheTimestamp(cacheKey)
                }

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
     * Avoids searching all groups' standings - only fetches the known group.
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

        // Get match results for this group and team details concurrently
        var allResults = emptyMap<Int, List<MatchResult>>()
        var teamDetail: TeamDetail? = null

        coroutineScope {
            val resultsDeferred = async { getAllMatchResults(groupId) }
            val detailDeferred = async { runCatching { getTeamDetail(teamId, teamStanding.teamHref) }.getOrNull() }

            allResults = resultsDeferred.await()
            teamDetail = detailDeferred.await()
        }

        val teamMatches = allResults.values
            .flatten()
            .filter { it.localTeamId == teamId || it.visitorTeamId == teamId }
            .sortedBy { it.jornada }

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
     * If data is not cached yet, fetches it.
     */
    suspend fun getTeamInfo(teamId: Int, teamName: String): TeamInfo? {
        val groups = getGroups()

        // Find which group this team belongs to by checking cached standings
        // or fetching them.
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
                    } else {
                        null
                    }
                }
            }.awaitAll().filterNotNull().firstOrNull()?.let { (standing, gId, gName) ->
                teamStanding = standing
                teamGroupId = gId
                teamGroupName = gName
            }
        }

        val groupId = teamGroupId ?: return null
        val groupName = teamGroupName ?: return null

        // Get all match results for this group and team details concurrently
        var allResults = emptyMap<Int, List<MatchResult>>()
        var teamDetail: TeamDetail? = null

        coroutineScope {
            val resultsDeferred = async { getAllMatchResults(groupId) }
            val detailDeferred = async { runCatching { getTeamDetail(teamId, teamStanding?.teamHref.orEmpty()) }.getOrNull() }

            allResults = resultsDeferred.await()
            teamDetail = detailDeferred.await()
        }

        // Filter matches where this team participated
        val teamMatches = allResults.values
            .flatten()
            .filter { it.localTeamId == teamId || it.visitorTeamId == teamId }
            .sortedBy { it.jornada }

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

    private const val TTL_GROUPS = 24 * 60 * 60 * 1000L
    private const val TTL_STANDINGS = 30 * 60 * 1000L
    private const val TTL_RESULTS = 30 * 60 * 1000L
    private const val TTL_TEAM_DETAIL = 6 * 60 * 60 * 1000L

    private const val CACHE_KEY_GROUPS = "groups"
}
