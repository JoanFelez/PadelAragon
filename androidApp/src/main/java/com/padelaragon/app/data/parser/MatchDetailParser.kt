package com.padelaragon.app.data.parser

import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.PairDetail
import com.padelaragon.app.data.model.SetScore
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class MatchDetailParser {
    fun parse(html: String): MatchDetail {
        return runCatching {
            val document = Jsoup.parse(html)
            val pairs = mutableListOf<PairDetail>()

            // Find all pair header rows (CabeceraRnk containing "Pareja")
            val allRows = document.select("tr.CabeceraRnk, tr.LineasRnk")
            var pairNumber = 0
            var i = 0

            while (i < allRows.size) {
                val row = allRows[i]
                if (row.hasClass("CabeceraRnk") && row.text().contains("Pareja", ignoreCase = true)) {
                    pairNumber++
                    // Collect subsequent LineasRnk rows for this pair
                    val pairRows = mutableListOf<Element>()
                    var j = i + 1
                    while (j < allRows.size && allRows[j].hasClass("LineasRnk")) {
                        pairRows.add(allRows[j])
                        j++
                    }

                    if (pairRows.isNotEmpty()) {
                        val pairDetail = parsePairDetail(pairNumber, pairRows)
                        if (pairDetail != null) {
                            pairs.add(pairDetail)
                        }
                    }
                    i = j
                } else {
                    i++
                }
            }

            MatchDetail(pairs)
        }.getOrDefault(MatchDetail(emptyList()))
    }

    private fun parsePairDetail(pairNumber: Int, rows: List<Element>): PairDetail? {
        // First row should have td[rowspan] cells with player names
        val firstRow = rows.first()
        val rowspanCells = firstRow.select("td[rowspan]")

        // Extract player names from the two rowspan cells (local and visitor)
        val localPlayers = parsePlayerNames(rowspanCells.getOrNull(0))
        val visitorPlayers = parsePlayerNames(rowspanCells.getOrNull(1))

        if (localPlayers == null || visitorPlayers == null) return null

        // Extract set scores from all rows
        val sets = mutableListOf<SetScore>()
        for (row in rows) {
            // Get all td cells that are NOT rowspan cells (these are set scores)
            val cells = row.select("td").filter { !it.hasAttr("rowspan") }
            // Set scores come in pairs (local, visitor)
            if (cells.size >= 2) {
                val localScore = cells[0].text().trim().toIntOrNull()
                val visitorScore = cells[1].text().trim().toIntOrNull()
                if (localScore != null && visitorScore != null) {
                    sets.add(SetScore(localScore, visitorScore))
                }
            }
        }

        return PairDetail(
            pairNumber = pairNumber,
            localPlayer1 = localPlayers.first,
            localPlayer2 = localPlayers.second,
            visitorPlayer1 = visitorPlayers.first,
            visitorPlayer2 = visitorPlayers.second,
            sets = sets
        )
    }

    private fun parsePlayerNames(cell: Element?): Pair<String, String>? {
        if (cell == null) return null

        // Player names are separated by " - " in the text
        // HTML typically: PLAYER1<br> - <br>PLAYER2
        // text() gives: PLAYER1 - PLAYER2
        val text = cell.text().trim()
        if (text.isEmpty()) return null

        val parts = text.split(" - ", limit = 2)
        if (parts.size < 2) return text to ""

        return parts[0].trim() to parts[1].trim()
    }
}
