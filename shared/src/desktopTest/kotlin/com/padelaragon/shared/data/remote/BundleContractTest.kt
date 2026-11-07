package com.padelaragon.shared.data.remote

import com.padelaragon.shared.data.model.Gender
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.remote.dto.BundleJson
import com.padelaragon.shared.data.remote.dto.LeagueBundleDto
import com.padelaragon.shared.data.remote.dto.LeagueDto
import com.padelaragon.shared.data.remote.dto.ManifestDto
import com.padelaragon.shared.data.remote.dto.toDetail
import com.padelaragon.shared.data.remote.dto.toDto
import com.padelaragon.shared.data.remote.dto.toBundleDto
import com.padelaragon.shared.data.remote.dto.toModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BundleContractTest {
    private fun fixture(name: String): String =
        File(System.getProperty("contracts.dir") ?: "../contracts/fixtures", name).readText()

    private fun JsonElement.withoutNulls(): JsonElement = when (this) {
        is JsonObject -> JsonObject(filterValues { it !is JsonNull }.mapValues { it.value.withoutNulls() })
        is JsonArray -> JsonArray(map { it.withoutNulls() })
        else -> this
    }

    @Test
    fun bundleFixtureDecodesAndRoundTrips() {
        val text = fixture("league-bundle.sample.json")
        val bundle = BundleJson.decodeFromString(LeagueBundleDto.serializer(), text)

        assertEquals(1, bundle.schemaVersion)
        assertEquals(LeagueDto(27951, "Absoluta"), bundle.league)
        assertTrue(bundle.groups.isNotEmpty())

        val encoded = BundleJson.encodeToString(LeagueBundleDto.serializer(), bundle)
        assertEquals(bundle, BundleJson.decodeFromString(LeagueBundleDto.serializer(), encoded))
        assertEquals(
            BundleJson.parseToJsonElement(text).withoutNulls(),
            BundleJson.parseToJsonElement(encoded).withoutNulls()
        )
    }

    @Test
    fun manifestAndLeaguesFixturesRoundTrip() {
        val manifestText = fixture("manifest.sample.json")
        val manifest = BundleJson.decodeFromString(ManifestDto.serializer(), manifestText)
        assertEquals(2, manifest.leagues.size)
        assertEquals(
            BundleJson.parseToJsonElement(manifestText),
            BundleJson.parseToJsonElement(BundleJson.encodeToString(ManifestDto.serializer(), manifest))
        )

        val leaguesText = fixture("leagues.sample.json")
        val leagues = BundleJson.decodeFromString(ListSerializer(LeagueDto.serializer()), leaguesText)
        assertEquals(League.entries.map { it.toDto() }, leagues)
    }

    @Test
    fun mappersAreLossless() {
        val bundle = BundleJson.decodeFromString(
            LeagueBundleDto.serializer(), fixture("league-bundle.sample.json")
        )
        bundle.groups.forEach { g ->
            assertEquals(g.group, g.group.toModel().toDto())
            assertEquals(g.standings, g.standings.map { it.toModel().toDto() })
            assertEquals(g.results, g.results.map { it.toModel().toDto() })
            assertEquals(g.teams, g.teams.map { it.toDetail().toBundleDto(it.teamId, it.teamName) })
            assertEquals(g.matchDetails, g.matchDetails.mapValues { it.value.toModel().toDto() })
        }
        assertTrue(bundle.groups.any { it.group.toModel().gender == Gender.MASCULINA })
    }
}
