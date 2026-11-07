package com.padelaragon.shared.data.sync

import com.padelaragon.shared.data.local.AppDatabase
import com.padelaragon.shared.data.local.dao.LeagueImport
import com.padelaragon.shared.data.local.entity.CacheTimestamp
import com.padelaragon.shared.data.local.entity.JornadaEntity
import com.padelaragon.shared.data.local.entity.LeagueGroupEntity
import com.padelaragon.shared.data.local.entity.MatchDetailPairEntity
import com.padelaragon.shared.data.local.entity.MatchResultEntity
import com.padelaragon.shared.data.local.entity.PlayerEntity
import com.padelaragon.shared.data.local.entity.StandingRowEntity
import com.padelaragon.shared.data.local.entity.TeamDetailEntity
import com.padelaragon.shared.data.remote.dto.LeagueBundleDto
import com.padelaragon.shared.data.remote.dto.toDetail
import com.padelaragon.shared.data.remote.dto.toModel

/** Writes a backend [LeagueBundleDto] into Room in one transaction. */
class BundleImporter(private val db: AppDatabase) {

    suspend fun import(bundle: LeagueBundleDto, nowMs: Long) {
        db.bundleImportDao().replaceLeague(toImport(bundle, nowMs))
    }

    internal fun toImport(bundle: LeagueBundleDto, nowMs: Long): LeagueImport {
        val leagueId = bundle.league.id
        val groups = bundle.groups
        // Same keys the scraping repositories use, so their TTL logic sees the imported data as fresh.
        val timestamps = buildList {
            add(CacheTimestamp("groups_$leagueId", nowMs))
            groups.forEach { g ->
                val groupId = g.group.id
                add(CacheTimestamp("standings_${leagueId}_$groupId", nowMs))
                add(CacheTimestamp("jornadas_${leagueId}_$groupId", nowMs))
                g.jornadas.forEach { add(CacheTimestamp("results_${leagueId}_${groupId}_$it", nowMs)) }
                g.teams.forEach { add(CacheTimestamp("team_${leagueId}_${it.teamId}", nowMs)) }
            }
        }
        return LeagueImport(
            leagueId = leagueId,
            groups = groups.map { LeagueGroupEntity.fromModel(leagueId, it.group.toModel()) },
            standings = groups.flatMap { g ->
                g.standings.map { StandingRowEntity.fromModel(leagueId, g.group.id, it.toModel()) }
            },
            jornadas = groups.flatMap { g -> g.jornadas.map { JornadaEntity(leagueId, g.group.id, it) } },
            results = groups.flatMap { g ->
                g.results.map { MatchResultEntity.fromModel(leagueId, g.group.id, it.toModel()) }
            },
            teams = groups.flatMap { g ->
                g.teams.map { TeamDetailEntity(leagueId, it.teamId, it.category, it.captainName) }
            },
            players = groups.flatMap { g ->
                g.teams.flatMap { t -> t.toDetail().players.map { PlayerEntity.fromModel(leagueId, t.teamId, it) } }
            },
            matchPairs = groups.flatMap { g ->
                g.matchDetails.flatMap { (url, detail) ->
                    detail.toModel().pairs.map { MatchDetailPairEntity.fromModel(url, it) }
                }
            },
            timestamps = timestamps
        )
    }
}
