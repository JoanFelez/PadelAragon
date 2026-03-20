package com.padelaragon.app.data.parser

import com.padelaragon.app.data.model.StandingRow
import org.jsoup.Jsoup

class StandingsParser {
    fun parse(html: String): List<StandingRow> {
        return runCatching {
            val document = Jsoup.parse(html)
            val rows = mutableListOf<StandingRow>()

            document.select("tr.LineasRnk").forEach { row ->
                val cells = row.select("td")
                if (cells.size < 12) return@forEach

                val position = cells[0].text().toIntSafe() ?: return@forEach
                val teamCell = cells[1]
                val teamName = teamCell.text().trim()
                val teamHref = teamCell.selectFirst("a[href]")?.attr("href").orEmpty()
                android.util.Log.d("StandingsParser", "Team href for $teamName: $teamHref")
                val teamId = extractIdEquipo(teamHref) ?: return@forEach
                val matchesPlayed = cells[2].text().toIntSafe() ?: return@forEach
                val points = cells[3].text().toIntSafe() ?: return@forEach
                val encountersWon = cells[4].text().toIntSafe() ?: return@forEach
                val encountersLost = cells[5].text().toIntSafe() ?: return@forEach
                val matchesWon = cells[6].text().toIntSafe() ?: return@forEach
                val matchesLost = cells[7].text().toIntSafe() ?: return@forEach
                val setsWon = cells[8].text().toIntSafe() ?: return@forEach
                val setsLost = cells[9].text().toIntSafe() ?: return@forEach
                val gamesWon = cells[10].text().toIntSafe() ?: return@forEach
                val gamesLost = cells[11].text().toIntSafe() ?: return@forEach

                rows.add(
                    StandingRow(
                        position = position,
                        teamName = teamName,
                        teamId = teamId,
                        teamHref = teamHref,
                        points = points,
                        matchesPlayed = matchesPlayed,
                        encountersWon = encountersWon,
                        encountersLost = encountersLost,
                        matchesWon = matchesWon,
                        matchesLost = matchesLost,
                        setsWon = setsWon,
                        setsLost = setsLost,
                        gamesWon = gamesWon,
                        gamesLost = gamesLost
                    )
                )
            }

            rows
        }.getOrDefault(emptyList())
    }

    private fun extractIdEquipo(href: String): Int? =
        ID_EQUIPO_REGEX.find(href)?.groupValues?.get(1)?.toIntOrNull()

    private fun String.toIntSafe(): Int? =
        trim().replace(".", "").toIntOrNull()

    companion object {
        private val ID_EQUIPO_REGEX = Regex("[?&]IdEquipo=(\\d+)", RegexOption.IGNORE_CASE)
    }
}
