package com.padelaragon.scraper

import com.padelaragon.shared.data.remote.dto.GroupBundleDto
import com.padelaragon.shared.data.remote.dto.MatchDetailDto
import com.padelaragon.shared.data.remote.dto.MatchResultDto

private const val UNPLAYED = "--"

fun MatchResultDto.isPlayed(): Boolean = localScore != UNPLAYED || visitorScore != UNPLAYED

/**
 * Detail URLs worth fetching: played matches that have no stored detail yet, or whose score
 * changed since the previous bundle (a correction). Everything else is reused.
 */
fun detailsToFetch(results: List<MatchResultDto>, previous: GroupBundleDto?): List<String> {
    val previousByUrl = previous?.results?.filter { !it.detailUrl.isNullOrBlank() }?.associateBy { it.detailUrl!! }.orEmpty()
    return results.filter { it.isPlayed() && !it.detailUrl.isNullOrBlank() }
        .distinctBy { it.detailUrl }
        .filter { current ->
            val old = previousByUrl[current.detailUrl]
            val stored = previous?.matchDetails?.containsKey(current.detailUrl) == true
            !stored || old == null || old.localScore != current.localScore || old.visitorScore != current.visitorScore
        }
        .map { it.detailUrl!! }
}

/** Newly fetched details win; otherwise previous ones are kept; entries no longer referenced are dropped. Keys sorted. */
fun mergeDetails(
    results: List<MatchResultDto>,
    previous: GroupBundleDto?,
    fetched: Map<String, MatchDetailDto>
): Map<String, MatchDetailDto> {
    val urls = results.mapNotNull { it.detailUrl }.filter { it.isNotBlank() }.toSortedSet()
    val merged = LinkedHashMap<String, MatchDetailDto>()
    for (url in urls) {
        val detail = fetched[url] ?: previous?.matchDetails?.get(url)
        if (detail != null) merged[url] = detail
    }
    return merged
}
