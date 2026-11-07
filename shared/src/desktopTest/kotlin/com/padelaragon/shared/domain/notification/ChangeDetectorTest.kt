package com.padelaragon.shared.domain.notification

import com.padelaragon.shared.data.model.MatchResult
import com.padelaragon.shared.data.model.StandingRow
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChangeDetectorTest {
    private val settings = MapSettings()
    private val detector = ChangeDetector(SettingsNotificationStateStore(settings))

    private fun match(j: Int, l: Int, v: Int, ls: String = "--", vs: String = "--") =
        MatchResult("Equipo $l", l, "Equipo $v", v, ls, vs, null, null, j)

    private fun row(team: Int, pos: Int) = StandingRow(pos, "Equipo $team", team, "", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

    @Test
    fun firstRunOnlyRecordsBaseline() {
        val events = detector.detect(1, 10, "Grupo A", listOf(match(1, 1, 2, "3", "1")), listOf(row(1, 1), row(2, 2)))
        assertTrue(events.isEmpty())
    }

    @Test
    fun newResultAndPositionChangeAreReportedOnce() {
        val before = listOf(match(1, 1, 2), match(2, 3, 4))
        detector.detect(1, 10, "Grupo A", before, listOf(row(1, 1), row(2, 2)))

        val after = listOf(match(1, 1, 2, "1", "3"), match(2, 3, 4))
        val standings = listOf(row(2, 1), row(1, 2))
        val events = detector.detect(1, 10, "Grupo A", after, standings)

        assertEquals(3, events.size)
        assertIs<NotificationEvent.NewResult>(events[0])
        assertEquals("Equipo 1 1-3 Equipo 2 (jornada 1)", events[0].message)
        assertEquals(2, events.filterIsInstance<NotificationEvent.PositionChange>().size)

        assertTrue(detector.detect(1, 10, "Grupo A", after, standings).isEmpty())
    }

    @Test
    fun stateIsKeptPerGroupAndLeague() {
        detector.detect(1, 10, "Grupo A", emptyList(), emptyList())
        val events = detector.detect(2, 10, "Grupo A", listOf(match(1, 1, 2, "2", "2")), listOf(row(1, 1)))
        assertTrue(events.isEmpty()) // other league: baseline, not a change
    }

    @Test
    fun unplayedMatchesProduceNoEvents() {
        detector.detect(1, 10, "Grupo A", listOf(match(1, 1, 2)), emptyList())
        assertTrue(detector.detect(1, 10, "Grupo A", listOf(match(1, 1, 2), match(2, 1, 3)), emptyList()).isEmpty())
    }

    @Test
    fun nonFavouriteGroupsAreIgnored() {
        fun data(m: MatchResult?) = mapOf(
            10 to ChangeDetector.GroupData("A", listOfNotNull(m), emptyList()),
            20 to ChangeDetector.GroupData("B", listOfNotNull(m), emptyList())
        )
        detector.detectFavorites(1, setOf(10), data(null))
        val events = detector.detectFavorites(1, setOf(10), data(match(1, 1, 2, "3", "1")))
        assertEquals(listOf(10), events.map { it.groupId })
        // group 20 was never tracked, so it still has no baseline
        assertTrue(detector.detectFavorites(1, setOf(20), data(match(1, 1, 2, "3", "1"))).isEmpty())
    }
}
