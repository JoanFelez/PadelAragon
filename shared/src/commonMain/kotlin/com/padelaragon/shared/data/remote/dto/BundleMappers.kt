package com.padelaragon.shared.data.remote.dto

import com.padelaragon.shared.data.model.Gender
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.LeagueGroup
import com.padelaragon.shared.data.model.MatchDetail
import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.model.PairDetail
import com.padelaragon.shared.data.model.Player
import com.padelaragon.shared.data.model.SetScore
import com.padelaragon.shared.data.model.StandingRow
import com.padelaragon.shared.data.model.TeamDetail
import kotlinx.serialization.json.Json

/** Single serializer configuration so every producer emits identical bytes for identical content. */
val BundleJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = true
    prettyPrint = false
}

fun League.toDto() = LeagueDto(id, displayName)

fun LeagueGroup.toDto() = LeagueGroupDto(id, name, gender.name, category, groupLetter)

/** Unknown gender values fall back to MASCULINA, matching the parser default. */
fun LeagueGroupDto.toModel() = LeagueGroup(
    id = id,
    name = name,
    gender = Gender.entries.firstOrNull { it.name == gender } ?: Gender.MASCULINA,
    category = category,
    groupLetter = groupLetter
)

fun StandingRow.toDto() = StandingRowDto(
    position, teamName, teamId, teamHref, points, matchesPlayed, encountersWon, encountersLost,
    matchesWon, matchesLost, setsWon, setsLost, gamesWon, gamesLost
)

fun StandingRowDto.toModel() = StandingRow(
    position, teamName, teamId, teamHref, points, matchesPlayed, encountersWon, encountersLost,
    matchesWon, matchesLost, setsWon, setsLost, gamesWon, gamesLost
)

fun MatchResult.toDto() = MatchResultDto(
    localTeam, localTeamId, visitorTeam, visitorTeamId, localScore, visitorScore, date, venue, jornada, detailUrl
)

fun MatchResultDto.toModel() = MatchResult(
    localTeam, localTeamId, visitorTeam, visitorTeamId, localScore, visitorScore, date, venue, jornada, detailUrl
)

fun Player.toDto() = PlayerDto(name, isCaptain, points, birthYear)

fun PlayerDto.toModel() = Player(name, isCaptain, points, birthYear)

fun TeamDetail.toBundleDto(teamId: Int, teamName: String) =
    TeamBundleDto(teamId, teamName, category, captainName, players.map { it.toDto() })

fun TeamBundleDto.toDetail() = TeamDetail(category, captainName, players.map { it.toModel() })

fun MatchDetail.toDto() = MatchDetailDto(
    pairs.map { pair ->
        PairDetailDto(
            pair.pairNumber, pair.localPlayer1, pair.localPlayer2, pair.visitorPlayer1, pair.visitorPlayer2,
            pair.sets.map { SetScoreDto(it.localScore, it.visitorScore) }
        )
    }
)

fun MatchDetailDto.toModel() = MatchDetail(
    pairs.map { pair ->
        PairDetail(
            pair.pairNumber, pair.localPlayer1, pair.localPlayer2, pair.visitorPlayer1, pair.visitorPlayer2,
            pair.sets.map { SetScore(it.localScore, it.visitorScore) }
        )
    }
)
