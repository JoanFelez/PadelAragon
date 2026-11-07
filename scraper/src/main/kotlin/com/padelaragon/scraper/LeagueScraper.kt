package com.padelaragon.scraper

import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.LeagueGroup
import com.padelaragon.shared.data.model.TeamDetail
import com.padelaragon.shared.data.parser.GroupParser
import com.padelaragon.shared.data.parser.MatchDetailParser
import com.padelaragon.shared.data.parser.MatchResultParser
import com.padelaragon.shared.data.parser.StandingsParser
import com.padelaragon.shared.data.parser.TeamDetailParser
import com.padelaragon.shared.data.remote.FederationUrls
import com.padelaragon.shared.data.remote.dto.GroupBundleDto
import com.padelaragon.shared.data.remote.dto.LeagueBundleDto
import com.padelaragon.shared.data.remote.dto.MatchDetailDto
import com.padelaragon.shared.data.remote.dto.StandingRowDto
import com.padelaragon.shared.data.remote.dto.TeamBundleDto
import com.padelaragon.shared.data.remote.dto.toBundleDto
import com.padelaragon.shared.data.remote.dto.toDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicInteger

data class ScrapeStats(
    val groups: Int,
    val matches: Int,
    val detailsFetched: Int,
    val detailsReused: Int,
    val warnings: Int
)

data class ScrapeResult(val bundle: LeagueBundleDto, val stats: ScrapeStats)

class LeagueScraper(
    private val pages: PageSource,
    private val log: (String) -> Unit = { System.err.println(it) },
    private val groupParser: GroupParser = GroupParser(),
    private val standingsParser: StandingsParser = StandingsParser(),
    private val matchResultParser: MatchResultParser = MatchResultParser(),
    private val matchDetailParser: MatchDetailParser = MatchDetailParser(),
    private val teamDetailParser: TeamDetailParser = TeamDetailParser()
) {
    /**
     * Scrapes one league. A failure of the group list, a standings/calendar page or a jornada page fails
     * the whole league (the caller then keeps the previous bundle); missing team or match details only warn.
     */
    suspend fun scrape(league: League, previous: LeagueBundleDto?, generatedAt: String): ScrapeResult {
        val warnings = AtomicInteger()
        val fetched = AtomicInteger()
        val reused = AtomicInteger()

        val groups = groupParser.parse(pages.get(FederationUrls.leagueCalendar(league.id)))
        if (groups.isEmpty()) throw IllegalStateException("No groups found for league ${league.id}")
        val previousGroups = previous?.groups?.associateBy { it.group.id }.orEmpty()

        val groupBundles = coroutineScope {
            groups.map { group ->
                async {
                    scrapeGroup(league, group, previousGroups[group.id], warnings, fetched, reused)
                }
            }.awaitAll()
        }.sortedBy { it.group.id }

        val bundle = LeagueBundleDto(league = league.toDto(), generatedAt = generatedAt, groups = groupBundles)
        return ScrapeResult(
            bundle,
            ScrapeStats(
                groups = groupBundles.size,
                matches = groupBundles.sumOf { it.results.size },
                detailsFetched = fetched.get(),
                detailsReused = reused.get(),
                warnings = warnings.get()
            )
        )
    }

    private suspend fun scrapeGroup(
        league: League,
        group: LeagueGroup,
        previous: GroupBundleDto?,
        warnings: AtomicInteger,
        fetched: AtomicInteger,
        reused: AtomicInteger
    ): GroupBundleDto = coroutineScope {
        val standingsJob = async {
            standingsParser.parse(
                pages.post(
                    FederationUrls.STANDINGS,
                    mapOf("Liga" to league.id.toString(), "grupo" to group.id.toString())
                )
            ).map { it.toDto() }
        }
        val jornadasJob = async {
            groupParser.parseJornadas(pages.get(FederationUrls.groupCalendar(league.id, group.id)))
        }
        val standings = standingsJob.await().ifEmpty { previous?.standings.orEmpty() }
        val jornadas = jornadasJob.await().ifEmpty { previous?.jornadas.orEmpty() }.distinct().sorted()

        val results = jornadas.map { jornada ->
            async {
                val html = pages.get(FederationUrls.jornada(league.id, group.id, jornada))
                matchResultParser.parse(html, jornada).map { it.toDto() }
            }
        }.awaitAll().flatten()

        val teamsJob = async { scrapeTeams(league, standings, previous, warnings) }
        val newDetails = detailsToFetch(results, previous)
        val detailsJob = async { fetchDetails(newDetails, warnings) }
        val fetchedDetails = detailsJob.await()
        fetched.addAndGet(fetchedDetails.size)

        val merged = mergeDetails(results, previous, fetchedDetails)
        reused.addAndGet(merged.keys.count { it !in fetchedDetails })

        GroupBundleDto(
            group = group.toDto(),
            standings = standings,
            jornadas = jornadas,
            results = results,
            teams = teamsJob.await(),
            matchDetails = merged
        )
    }

    private suspend fun fetchDetails(urls: List<String>, warnings: AtomicInteger): Map<String, MatchDetailDto> =
        coroutineScope {
            urls.map { url ->
                async {
                    try {
                        val detail = matchDetailParser.parse(pages.get(FederationUrls.matchDetail(url)))
                        if (detail.pairs.isEmpty()) null else url to detail.toDto()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        warnings.incrementAndGet()
                        log("WARN: match detail failed (${e::class.simpleName})")
                        null
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }

    private suspend fun scrapeTeams(
        league: League,
        standings: List<StandingRowDto>,
        previous: GroupBundleDto?,
        warnings: AtomicInteger
    ): List<TeamBundleDto> = coroutineScope {
        val previousTeams = previous?.teams?.associateBy { it.teamId }.orEmpty()
        standings.distinctBy { it.teamId }.map { row ->
            async {
                val detail = fetchTeam(league, row)
                when {
                    detail != null -> detail.toBundleDto(row.teamId, row.teamName)
                    else -> {
                        warnings.incrementAndGet()
                        log("WARN: team detail unavailable for team ${row.teamId}")
                        previousTeams[row.teamId]
                            ?: TeamBundleDto(teamId = row.teamId, teamName = row.teamName)
                    }
                }
            }
        }.awaitAll().sortedBy { it.teamId }
    }

    private suspend fun fetchTeam(league: League, row: StandingRowDto): TeamDetail? {
        for (url in FederationUrls.teamDetailCandidates(league.id, row.teamId, row.teamHref)) {
            val detail = try {
                pages.getOrNull(url)?.let { teamDetailParser.parse(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (detail != null && (detail.players.isNotEmpty() || detail.captain != null)) return detail
        }
        return null
    }
}
