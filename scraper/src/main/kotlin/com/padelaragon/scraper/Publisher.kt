package com.padelaragon.scraper

import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.remote.dto.BundleJson
import com.padelaragon.shared.data.remote.dto.LeagueBundleDto
import com.padelaragon.shared.data.remote.dto.LeagueDto
import com.padelaragon.shared.data.remote.dto.ManifestDto
import com.padelaragon.shared.data.remote.dto.ManifestLeagueDto
import com.padelaragon.shared.data.remote.dto.toDto
import kotlinx.serialization.builtins.ListSerializer
import java.security.MessageDigest

/** What the Worker currently serves (exact bytes, so hashes match its ETags). */
class PreviousState(
    val manifestBytes: ByteArray? = null,
    val leaguesBytes: ByteArray? = null,
    val bundleBytes: Map<Int, ByteArray> = emptyMap()
) {
    val manifest: ManifestDto? = manifestBytes?.let { decodeOrNull { BundleJson.decodeFromString(ManifestDto.serializer(), it.decodeToString()) } }

    fun bundle(id: Int): LeagueBundleDto? = bundleBytes[id]?.let {
        decodeOrNull { BundleJson.decodeFromString(LeagueBundleDto.serializer(), it.decodeToString()) }
    }

    private fun <T> decodeOrNull(block: () -> T): T? = try {
        block()
    } catch (_: Exception) {
        null
    }
}

/** A document to write (and maybe upload). [route] is the Worker path of the `PUT /internal/...` endpoint. */
class Artifact(val fileName: String, val route: String, val bytes: ByteArray, val hash: String, val changed: Boolean)

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

private fun LeagueBundleDto.encode(): ByteArray =
    BundleJson.encodeToString(LeagueBundleDto.serializer(), this).encodeToByteArray()

/**
 * Turns scraped bundles into the documents to publish. `generatedAt`/`updatedAt` only move when content
 * changes, so an unchanged league keeps the exact same bytes (and hash) run after run.
 * Returns bundles first, then leagues.json, then manifest.json (publish order).
 */
fun assemble(
    selected: List<League>,
    scraped: Map<Int, LeagueBundleDto>,
    allLeagues: List<League>,
    previous: PreviousState,
    now: String
): List<Artifact> {
    val artifacts = mutableListOf<Artifact>()
    val previousEntries = previous.manifest?.leagues.orEmpty().associateBy { it.id }
    val entries = previousEntries.toMutableMap()

    for (league in selected) {
        val oldEntry = previousEntries[league.id]
        val bytes: ByteArray = scraped[league.id]?.let { bundle ->
            val oldBundle = previous.bundle(league.id)
            val stable = bundle.copy(generatedAt = oldBundle?.generatedAt ?: now).encode()
            if (oldEntry != null && sha256Hex(stable) == oldEntry.hash) stable else bundle.copy(generatedAt = now).encode()
        } ?: previous.bundleBytes[league.id] ?: continue

        val hash = sha256Hex(bytes)
        val changed = oldEntry?.hash != hash
        artifacts += Artifact("${league.id}.json", "/internal/leagues/${league.id}/bundle", bytes, hash, changed)
        entries[league.id] = ManifestLeagueDto(
            id = league.id,
            name = league.displayName,
            hash = hash,
            updatedAt = if (oldEntry != null && !changed) oldEntry.updatedAt else now
        )
    }

    val leaguesBytes = BundleJson.encodeToString(
        ListSerializer(LeagueDto.serializer()), allLeagues.map { it.toDto() }
    ).encodeToByteArray()
    val leaguesHash = sha256Hex(leaguesBytes)
    artifacts += Artifact(
        "leagues.json", "/internal/leagues", leaguesBytes, leaguesHash,
        previous.leaguesBytes?.let { sha256Hex(it) } != leaguesHash
    )

    val sortedEntries = entries.values.sortedBy { it.id }
    val oldManifest = previous.manifest
    val manifest = ManifestDto(
        generatedAt = if (oldManifest != null && oldManifest.leagues == sortedEntries) oldManifest.generatedAt else now,
        leagues = sortedEntries
    )
    val manifestBytes = BundleJson.encodeToString(ManifestDto.serializer(), manifest).encodeToByteArray()
    val manifestHash = sha256Hex(manifestBytes)
    artifacts += Artifact(
        "manifest.json", "/internal/manifest", manifestBytes, manifestHash,
        previous.manifestBytes?.let { sha256Hex(it) } != manifestHash
    )
    return artifacts
}

fun uploadsFor(artifacts: List<Artifact>): List<Artifact> = artifacts.filter { it.changed }
