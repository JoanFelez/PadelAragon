package com.padelaragon.app.data.parser

import com.padelaragon.app.data.model.MatchResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MatchResultParser {
    fun parse(html: String, jornada: Int): List<MatchResult> {
        return runCatching {
            val document = Jsoup.parse(html)
            val results = mutableListOf<MatchResult>()

            document.select("tr.LineasRnk").forEach { row ->
                val cells = row.select("td")
                if (cells.size < 4) return@forEach

                val localTeam = parseTeam(cells[0]) ?: return@forEach
                val visitorTeam = parseTeam(cells[3]) ?: return@forEach

                val localScore = normalizeScore(cells.getOrNull(1)?.text())
                val visitorScore = normalizeScore(cells.getOrNull(2)?.text())
                val (date, venue) = parseDateAndVenue(cells.getOrNull(4)?.text().orEmpty())

                results.add(
                    MatchResult(
                        localTeam = localTeam.name,
                        localTeamId = localTeam.id,
                        visitorTeam = visitorTeam.name,
                        visitorTeamId = visitorTeam.id,
                        localScore = localScore,
                        visitorScore = visitorScore,
                        date = date,
                        venue = venue,
                        jornada = jornada
                    )
                )
            }

            results
        }.getOrDefault(emptyList())
    }

    private fun parseTeam(cell: Element): TeamInfo? {
        val text = cell.text().trim()
        if (text.isEmpty()) return null

        if (text.contains("descansa", ignoreCase = true)) {
            return TeamInfo(name = text, id = BYE_TEAM_ID)
        }

        val link = cell.selectFirst("a[href]")
        if (link != null) {
            val name = link.text().trim().ifEmpty { text }
            val id = extractIdEquipo(link.attr("href")) ?: UNKNOWN_TEAM_ID
            return TeamInfo(name = name, id = id)
        }

        return TeamInfo(name = text, id = UNKNOWN_TEAM_ID)
    }

    private fun parseDateAndVenue(raw: String): Pair<String?, String?> {
        val clean = raw.trim()
        if (clean.isEmpty()) return null to null

        val parts = clean.split(" - ", limit = 2)
        val date = parts.getOrNull(0)?.trim().orEmpty().ifEmpty { null }
        val venue = parts.getOrNull(1)?.trim().orEmpty().ifEmpty { null }
        return date to venue
    }

    private fun normalizeScore(value: String?): String {
        val clean = value?.trim().orEmpty()
        return if (clean.isEmpty()) "--" else clean
    }

    private fun extractIdEquipo(href: String): Int? =
        ID_EQUIPO_REGEX.find(href)?.groupValues?.get(1)?.toIntOrNull()

    private data class TeamInfo(
        val name: String,
        val id: Int
    )

    companion object {
        private const val UNKNOWN_TEAM_ID = -1
        private const val BYE_TEAM_ID = -2
        private val ID_EQUIPO_REGEX = Regex("[?&]IdEquipo=(\\d+)", RegexOption.IGNORE_CASE)
    }
}
