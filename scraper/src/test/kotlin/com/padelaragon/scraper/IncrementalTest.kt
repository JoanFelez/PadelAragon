package com.padelaragon.scraper

import com.padelaragon.shared.data.remote.dto.MatchDetailDto
import com.padelaragon.shared.data.remote.dto.MatchResultDto
import com.padelaragon.shared.data.remote.dto.GroupBundleDto
import kotlin.test.Test
import kotlin.test.assertEquals

class IncrementalTest {
    private fun result(url: String?, home: String = "2", away: String = "1") = MatchResultDto(
        localTeam = "A", localTeamId = 1, visitorTeam = "B", visitorTeamId = 2,
        localScore = home, visitorScore = away, jornada = 1, detailUrl = url
    )

    private val group = Fixtures.sampleBundle().groups.first()
    private val detail = group.matchDetails.values.first()

    private fun previous(results: List<MatchResultDto>, details: Map<String, MatchDetailDto>): GroupBundleDto =
        group.copy(results = results, matchDetails = details)

    @Test
    fun fetchesOnlyNewPlayedMatches() {
        val prev = previous(listOf(result("u1"), result("u2", "--", "--")), mapOf("u1" to detail))
        val now = listOf(result("u1"), result("u2"), result("u3"), result("u4", "--", "--"), result(null))
        assertEquals(listOf("u2", "u3"), detailsToFetch(now, prev))
    }

    @Test
    fun refetchesWhenScoreWasCorrected() {
        val prev = previous(listOf(result("u1", "2", "1")), mapOf("u1" to detail))
        assertEquals(listOf("u1"), detailsToFetch(listOf(result("u1", "1", "2")), prev))
    }

    @Test
    fun fetchesEverythingWithoutPreviousBundle() {
        assertEquals(listOf("u1", "u2"), detailsToFetch(listOf(result("u1"), result("u2")), null))
    }

    @Test
    fun mergeReusesPreviousDropsStaleAndSortsKeys() {
        val prev = previous(listOf(result("b"), result("gone")), mapOf("b" to detail, "gone" to detail))
        val fresh = detail.copy(pairs = emptyList())
        val merged = mergeDetails(listOf(result("c"), result("b"), result("a")), prev, mapOf("a" to fresh))
        assertEquals(listOf("a", "b"), merged.keys.toList())
        assertEquals(fresh, merged["a"])
        assertEquals(detail, merged["b"])
    }
}
