package com.padelaragon.shared.data.remote.dto

import kotlinx.serialization.Serializable

/** Wire format of `contracts/README.md` (schemaVersion 1). Additive changes only. */
const val SCHEMA_VERSION = 1

@Serializable
data class LeagueDto(val id: Int, val name: String)

@Serializable
data class ManifestDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val generatedAt: String,
    val leagues: List<ManifestLeagueDto>
)

@Serializable
data class ManifestLeagueDto(
    val id: Int,
    val name: String,
    val hash: String,
    val updatedAt: String
)

@Serializable
data class LeagueBundleDto(
    val schemaVersion: Int = SCHEMA_VERSION,
    val league: LeagueDto,
    val generatedAt: String,
    val groups: List<GroupBundleDto>
)

@Serializable
data class GroupBundleDto(
    val group: LeagueGroupDto,
    val standings: List<StandingRowDto> = emptyList(),
    val jornadas: List<Int> = emptyList(),
    val results: List<MatchResultDto> = emptyList(),
    val teams: List<TeamBundleDto> = emptyList(),
    val matchDetails: Map<String, MatchDetailDto> = emptyMap()
)

@Serializable
data class LeagueGroupDto(
    val id: Int,
    val name: String,
    val gender: String,
    val category: String,
    val groupLetter: String? = null
)

@Serializable
data class StandingRowDto(
    val position: Int,
    val teamName: String,
    val teamId: Int,
    val teamHref: String = "",
    val points: Int,
    val matchesPlayed: Int,
    val encountersWon: Int,
    val encountersLost: Int,
    val matchesWon: Int,
    val matchesLost: Int,
    val setsWon: Int,
    val setsLost: Int,
    val gamesWon: Int,
    val gamesLost: Int
)

@Serializable
data class MatchResultDto(
    val localTeam: String,
    val localTeamId: Int,
    val visitorTeam: String,
    val visitorTeamId: Int,
    val localScore: String,
    val visitorScore: String,
    val date: String? = null,
    val venue: String? = null,
    val jornada: Int,
    val detailUrl: String? = null
)

@Serializable
data class TeamBundleDto(
    val teamId: Int,
    val teamName: String,
    val category: String? = null,
    val captainName: String? = null,
    val players: List<PlayerDto> = emptyList()
)

@Serializable
data class PlayerDto(
    val name: String,
    val isCaptain: Boolean = false,
    val points: String? = null,
    val birthYear: String? = null
)

@Serializable
data class MatchDetailDto(val pairs: List<PairDetailDto>)

@Serializable
data class PairDetailDto(
    val pairNumber: Int,
    val localPlayer1: String,
    val localPlayer2: String,
    val visitorPlayer1: String,
    val visitorPlayer2: String,
    val sets: List<SetScoreDto>
)

@Serializable
data class SetScoreDto(val localScore: Int, val visitorScore: Int)
