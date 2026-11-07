package com.padelaragon.shared.domain.notification

import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.model.StandingRow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

sealed interface NotificationEvent {
    val leagueId: Int
    val groupId: Int
    val groupName: String
    val title: String
    val message: String

    data class NewResult(
        override val leagueId: Int,
        override val groupId: Int,
        override val groupName: String,
        val match: MatchResult
    ) : NotificationEvent {
        override val title get() = "Nuevo resultado · $groupName"
        override val message
            get() = "${match.localTeam} ${match.localScore}-${match.visitorScore} ${match.visitorTeam} (jornada ${match.jornada})"
    }

    data class PositionChange(
        override val leagueId: Int,
        override val groupId: Int,
        override val groupName: String,
        val teamName: String,
        val from: Int,
        val to: Int
    ) : NotificationEvent {
        override val title get() = "Clasificación · $groupName"
        override val message
            get() = "$teamName ${if (to < from) "sube" else "baja"} del puesto $from al $to"
    }
}

/** What was last notified for one group; persisted so each change is announced once. */
@Serializable
data class GroupSnapshot(
    /** `jornada:localId:visitorId` -> `localScore-visitorScore`, played matches only. */
    val results: Map<String, String> = emptyMap(),
    /** teamId -> position. */
    val positions: Map<Int, Int> = emptyMap(),
    val teamNames: Map<Int, String> = emptyMap()
)

/** Persistence of the last-notified [GroupSnapshot] per group. */
interface NotificationStateStore {
    fun load(leagueId: Int, groupId: Int): GroupSnapshot?
    fun save(leagueId: Int, groupId: Int, snapshot: GroupSnapshot)
}

/**
 * Compares the data now cached for a favourite group with the snapshot stored after the previous
 * check. The first check of a group only records a baseline (no events), so enabling notifications
 * never floods the user with old results.
 */
class ChangeDetector(private val store: NotificationStateStore) {

    class GroupData(val name: String, val results: Collection<MatchResult>, val standings: List<StandingRow>)

    /** Checks only the groups in [favoriteIds]; every other group in [data] is ignored. */
    fun detectFavorites(leagueId: Int, favoriteIds: Set<Int>, data: Map<Int, GroupData>): List<NotificationEvent> =
        favoriteIds.sorted().flatMap { id ->
            data[id]?.let { detect(leagueId, id, it.name, it.results, it.standings) } ?: emptyList()
        }

    fun detect(
        leagueId: Int,
        groupId: Int,
        groupName: String,
        results: Collection<MatchResult>,
        standings: List<StandingRow>
    ): List<NotificationEvent> {
        val current = snapshotOf(results, standings)
        val previous = store.load(leagueId, groupId)
        if (previous == current) return emptyList()
        store.save(leagueId, groupId, current)
        if (previous == null) return emptyList()

        val events = mutableListOf<NotificationEvent>()
        results.filter { isPlayed(it) }
            .sortedBy { it.jornada }
            .forEach { match ->
                if (previous.results[key(match)] != score(match)) {
                    events += NotificationEvent.NewResult(leagueId, groupId, groupName, match)
                }
            }
        standings.sortedBy { it.position }.forEach { row ->
            val before = previous.positions[row.teamId]
            if (before != null && before != row.position) {
                events += NotificationEvent.PositionChange(leagueId, groupId, groupName, row.teamName, before, row.position)
            }
        }
        return events
    }

    private fun snapshotOf(results: Collection<MatchResult>, standings: List<StandingRow>) = GroupSnapshot(
        results = results.filter { isPlayed(it) }.associate { key(it) to score(it) },
        positions = standings.associate { it.teamId to it.position },
        teamNames = standings.associate { it.teamId to it.teamName }
    )

    private fun isPlayed(m: MatchResult) = m.localScore != UNPLAYED && m.visitorScore != UNPLAYED &&
        m.localScore.isNotBlank() && m.visitorScore.isNotBlank()

    private fun key(m: MatchResult) = "${m.jornada}:${m.localTeamId}:${m.visitorTeamId}"
    private fun score(m: MatchResult) = "${m.localScore}-${m.visitorScore}"

    private companion object {
        const val UNPLAYED = "--"
    }
}

class SettingsNotificationStateStore(private val settings: com.russhwolf.settings.Settings) : NotificationStateStore {
    override fun load(leagueId: Int, groupId: Int): GroupSnapshot? =
        settings.getStringOrNull(key(leagueId, groupId))?.let {
            runCatching { Json.decodeFromString<GroupSnapshot>(it) }.getOrNull()
        }

    override fun save(leagueId: Int, groupId: Int, snapshot: GroupSnapshot) =
        settings.putString(key(leagueId, groupId), Json.encodeToString(GroupSnapshot.serializer(), snapshot))

    private fun key(leagueId: Int, groupId: Int) = "notif_state_${leagueId}_$groupId"
}
