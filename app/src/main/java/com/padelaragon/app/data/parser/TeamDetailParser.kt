package com.padelaragon.app.data.parser

import android.util.Log
import com.padelaragon.app.data.model.Player
import com.padelaragon.app.data.model.TeamDetail
import org.jsoup.Jsoup

class TeamDetailParser {

    fun parse(html: String): TeamDetail {
        if (html.isBlank()) {
            Log.w(TAG, "Empty HTML received")
            return TeamDetail()
        }

        return runCatching {
            val document = Jsoup.parse(html)
            var category: String? = null
            var captainName: String? = null
            val players = mutableListOf<Player>()

            // Extract info from div.ranking section
            val rankingDiv = document.selectFirst("div.ranking")
            if (rankingDiv != null) {
                Log.d(TAG, "Found div.ranking")
                // Look for <em> labels followed by <strong> values
                val emElements = rankingDiv.select("em")
                for (em in emElements) {
                    val label = em.text().trim().lowercase()
                    // Find the next <strong> sibling after this <em>
                    var sibling = em.nextElementSibling()
                    // Skip intermediate elements (spans, br, etc.) to find the <strong>
                    while (sibling != null && sibling.tagName() != "strong") {
                        sibling = sibling.nextElementSibling()
                    }
                    val value = sibling?.text()?.trim()

                    if (value.isNullOrBlank()) {
                        // Try parent's direct search - the <em> and <strong> may not be direct siblings
                        // but within the same <td>
                        continue
                    }

                    when {
                        label.contains("categor") || label.contains("grupo") -> {
                            if (category == null) {
                                category = value
                                Log.d(TAG, "Found category from div.ranking: $category")
                            }
                        }

                        label.contains("capit") -> {
                            captainName = value
                            Log.d(TAG, "Found captain from div.ranking: $captainName")
                        }
                    }
                }

                // Fallback: parse the td text content to find labels and values
                if (category == null || captainName == null) {
                    val td = rankingDiv.selectFirst("td")
                    if (td != null) {
                        val html = td.html()
                        // Extract captain if not found yet
                        if (captainName == null) {
                            val captainRegex = Regex("""(?i)capit[aá]n\s*</em>\s*:?\s*<strong>([^<]+)</strong>""")
                            captainRegex.find(html)?.groupValues?.getOrNull(1)?.trim()?.let {
                                captainName = it
                                Log.d(TAG, "Found captain via regex: $captainName")
                            }
                        }
                        // Extract category if not found yet
                        if (category == null) {
                            val catRegex = Regex("""(?i)categor[ií]a/?grupo\s*</em>\s*:?\s*<strong>([^<]+)</strong>""")
                            catRegex.find(html)?.groupValues?.getOrNull(1)?.trim()?.let {
                                category = it
                                Log.d(TAG, "Found category via regex: $category")
                            }
                        }
                    }
                }
            } else {
                Log.d(TAG, "No div.ranking found, falling back to generic search")
                // Fallback: search for category in any element
                document.select("td, th, span, div, p, font, b, strong").forEach { el ->
                    val text = el.ownText().trim()
                    if (text.isNotEmpty() && text.length < 150 && category == null) {
                        if (text.contains("CATEGOR", ignoreCase = true) ||
                            (text.contains("GRUPO", ignoreCase = true) &&
                                (text.contains("MASCULIN", ignoreCase = true) || text.contains("FEMENIN", ignoreCase = true)))
                        ) {
                            category = text
                            Log.d(TAG, "Found category (fallback): $category")
                        }
                    }
                }
            }

            // Fast path: try to find players using tr.LineasRnk class (used by federation site)
            val lineasRnkRows = document.select("tr.LineasRnk")
            if (lineasRnkRows.isNotEmpty()) {
                Log.d(TAG, "Found ${lineasRnkRows.size} tr.LineasRnk rows - using fast path")

                // Find the parent table to get the header row
                val parentTable = lineasRnkRows.first()!!.parent()?.parent() // tr -> tbody/table -> table
                    ?: lineasRnkRows.first()!!.parent() // tr -> table (no tbody)

                if (parentTable != null) {
                    // Get the header row (first tr that isn't LineasRnk, or has th elements)
                    val allTrs = parentTable.select("tr")
                    val headerRow = allTrs.firstOrNull { tr ->
                        !tr.hasClass("LineasRnk") && tr.select("td, th").any { cell ->
                            val t = cell.text().trim().lowercase()
                            t.contains("nombre") || t.contains("jugador") || t.contains("apellido")
                        }
                    }

                    if (headerRow != null) {
                        val headerCells = headerRow.select("th, td").map { it.text().trim().lowercase() }
                        Log.d(TAG, "Fast path header: $headerCells")

                        // Map column indices (same logic as generic path)
                        var nombreIdx = -1
                        var apellido1Idx = -1
                        var apellido2Idx = -1
                        var fullNameIdx = -1
                        var captainIdx = -1
                        var pointsIdx = -1
                        var birthYearIdx = -1

                        headerCells.forEachIndexed { i, h ->
                            when {
                                h.contains("jugador") || h == "nombre completo" || h == "nombre y apellidos" ->
                                    fullNameIdx = i
                                h == "nombre" || h == "nom" || h == "nom." ->
                                    nombreIdx = i
                                (h.contains("1") && h.contains("apellido")) || h == "apellido 1" || h == "primer apellido" || h == "1er apellido" ->
                                    apellido1Idx = i
                                (h.contains("2") && h.contains("apellido")) || h == "apellido 2" || h == "segundo apellido" || h == "2o apellido" || h == "2º apellido" ->
                                    apellido2Idx = i
                                h.contains("apellido") && apellido1Idx == -1 ->
                                    apellido1Idx = i
                                h.contains("capit") || h == "c" || h == "c." || h == "cap" || h == "cap." ->
                                    captainIdx = i
                                h.contains("punto") || h.contains("ranking") || h.contains("rnk") || h.contains("pts") ->
                                    pointsIdx = i
                                h.contains("año") || h.contains("nacimiento") || h.contains("nac") || h.contains("f.nac") || h.contains("fnac") ->
                                    birthYearIdx = i
                            }
                        }

                        Log.d(
                            TAG,
                            "Fast path mapping: nombre=$nombreIdx, ap1=$apellido1Idx, ap2=$apellido2Idx, " +
                                "fullName=$fullNameIdx, captain=$captainIdx, points=$pointsIdx, birthYear=$birthYearIdx"
                        )

                        if (nombreIdx >= 0 || fullNameIdx >= 0) {
                            // Parse data rows
                            for (row in lineasRnkRows) {
                                val cells = row.select("td")
                                if (cells.isEmpty()) continue

                                val cellTexts = cells.map { it.text().trim() }
                                if (cellTexts.all { it.isEmpty() }) continue

                                val name = if (fullNameIdx >= 0 && fullNameIdx < cellTexts.size) {
                                    cellTexts[fullNameIdx]
                                } else {
                                    listOfNotNull(
                                        cellTexts.getOrNull(nombreIdx)?.takeIf { it.isNotBlank() },
                                        cellTexts.getOrNull(apellido1Idx)?.takeIf { it.isNotBlank() },
                                        cellTexts.getOrNull(apellido2Idx)?.takeIf { it.isNotBlank() }
                                    ).joinToString(" ")
                                }
                                if (name.isBlank()) continue

                                val captainCell = if (captainIdx >= 0 && captainIdx < cells.size) cells[captainIdx] else null
                                val captainText = captainCell?.text()?.trim() ?: ""
                                val captainHasImg = captainCell?.select("img")?.isNotEmpty() == true
                                val isCaptain = captainText.equals("Sí", ignoreCase = true) ||
                                    captainText.equals("Si", ignoreCase = true) ||
                                    captainText.equals("S", ignoreCase = true) ||
                                    captainText.equals("C", ignoreCase = true) ||
                                    captainText.equals("✓") ||
                                    captainText.equals("X", ignoreCase = true) ||
                                    captainHasImg

                                val points = if (pointsIdx >= 0 && pointsIdx < cellTexts.size) {
                                    cellTexts[pointsIdx].takeIf { it.isNotBlank() }
                                } else {
                                    null
                                }

                                val birthYear = if (birthYearIdx >= 0 && birthYearIdx < cellTexts.size) {
                                    cellTexts[birthYearIdx].takeIf { it.isNotBlank() }
                                } else {
                                    null
                                }

                                players.add(Player(name = name, isCaptain = isCaptain, points = points, birthYear = birthYear))
                                Log.d(TAG, "Player (fast): '$name' captain=$isCaptain points=$points year=$birthYear")
                            }

                            if (players.isNotEmpty()) {
                                Log.d(TAG, "Fast path found ${players.size} players")
                            }
                        }
                    }
                }
            }

            // Generic fallback: scan all tables if fast path didn't find players
            if (players.isEmpty()) {
                val tables = document.select("table")
                Log.d(TAG, "Found ${tables.size} tables in page (${html.length} chars)")

                for ((tableIdx, table) in tables.withIndex()) {
                    val rows = table.select("tr")
                    if (rows.size < 2) continue // Need at least header + 1 data row

                    // Try to find a header row
                    val headerRow = rows.firstOrNull { row ->
                        val cells = row.select("th, td")
                        cells.any { cell ->
                            val t = cell.text().trim().lowercase()
                            t.contains("nombre") || t.contains("jugador") || t.contains("apellido")
                        }
                    } ?: continue

                    val headerCells = headerRow.select("th, td").map { it.text().trim().lowercase() }
                    Log.d(TAG, "Table $tableIdx header: $headerCells")

                    // Map column indices
                    var nombreIdx = -1
                    var apellido1Idx = -1
                    var apellido2Idx = -1
                    var fullNameIdx = -1
                    var captainIdx = -1
                    var pointsIdx = -1
                    var birthYearIdx = -1

                    headerCells.forEachIndexed { i, h ->
                        when {
                            // Full name column
                            h.contains("jugador") || h == "nombre completo" || h == "nombre y apellidos" ->
                                fullNameIdx = i
                            // Individual name parts
                            h == "nombre" || h == "nom" || h == "nom." ->
                                nombreIdx = i
                            h.contains("1") && h.contains("apellido") || h == "apellido 1" || h == "primer apellido" || h == "1er apellido" ->
                                apellido1Idx = i
                            h.contains("2") && h.contains("apellido") || h == "apellido 2" || h == "segundo apellido" || h == "2o apellido" || h == "2º apellido" ->
                                apellido2Idx = i
                            h.contains("apellido") && apellido1Idx == -1 ->
                                apellido1Idx = i
                            // Captain
                            h.contains("capit") || h == "c" || h == "c." || h == "cap" || h == "cap." ->
                                captainIdx = i
                            // Points
                            h.contains("punto") || h.contains("ranking") || h.contains("rnk") || h.contains("pts") ->
                                pointsIdx = i
                            // Birth year
                            h.contains("año") || h.contains("nacimiento") || h.contains("nac") || h.contains("f.nac") || h.contains("fnac") ->
                                birthYearIdx = i
                        }
                    }

                    val headerIndex = rows.indexOf(headerRow)

                    Log.d(
                        TAG,
                        "Column mapping: nombre=$nombreIdx, ap1=$apellido1Idx, ap2=$apellido2Idx, " +
                            "fullName=$fullNameIdx, captain=$captainIdx, points=$pointsIdx, birthYear=$birthYearIdx"
                    )

                    // If captain column not found by header, try detecting from data
                    if (captainIdx == -1) {
                        val dataRows = rows.subList(headerIndex + 1, rows.size)
                        val numCols = headerCells.size
                        for (colIdx in 0 until numCols) {
                            val colValues = dataRows.mapNotNull { r ->
                                r.select("td").getOrNull(colIdx)?.text()?.trim()
                            }

                            val captainLikeValues = colValues.count { v ->
                                v.equals("Sí", ignoreCase = true) ||
                                    v.equals("Si", ignoreCase = true) ||
                                    v.equals("S", ignoreCase = true) ||
                                    v.equals("C", ignoreCase = true) ||
                                    v.equals("✓") ||
                                    v.equals("X", ignoreCase = true)
                            }
                            val emptyOrNo = colValues.count { v ->
                                v.isEmpty() || v.equals("No", ignoreCase = true) || v.equals("N", ignoreCase = true) || v == "-"
                            }
                            // Captain column: exactly 1 captain-like value, rest are empty/No
                            if (captainLikeValues == 1 && (captainLikeValues + emptyOrNo) == colValues.size && colValues.size >= 2) {
                                captainIdx = colIdx
                                Log.d(TAG, "Auto-detected captain column at index $colIdx from data values")
                                break
                            }
                        }
                    }

                    // If still no captain column, check for columns with images in exactly one row
                    if (captainIdx == -1) {
                        val dataRows = rows.subList(headerIndex + 1, rows.size)
                        val numCols = headerCells.size
                        for (colIdx in 0 until numCols) {
                            val imgCount = dataRows.count { r ->
                                val cell = r.select("td").getOrNull(colIdx)
                                cell?.select("img")?.isNotEmpty() == true
                            }
                            if (imgCount == 1 && dataRows.size > 1) {
                                captainIdx = colIdx
                                Log.d(TAG, "Auto-detected captain column at index $colIdx from single image")
                                break
                            }
                        }
                    }

                    // If we didn't find name columns, skip this table
                    if (nombreIdx == -1 && fullNameIdx == -1) {
                        Log.d(TAG, "No name column found in table $tableIdx, skipping")
                        continue
                    }

                    // Parse data rows (everything after the header)
                    for (rowIdx in (headerIndex + 1) until rows.size) {
                        val row = rows[rowIdx]
                        val cells = row.select("td")
                        if (cells.isEmpty()) continue

                        val cellTexts = cells.map { it.text().trim() }
                        if (cellTexts.all { it.isEmpty() }) continue

                        // Build full name
                        val name = if (fullNameIdx >= 0 && fullNameIdx < cellTexts.size) {
                            cellTexts[fullNameIdx]
                        } else {
                            listOfNotNull(
                                cellTexts.getOrNull(nombreIdx)?.takeIf { it.isNotBlank() },
                                cellTexts.getOrNull(apellido1Idx)?.takeIf { it.isNotBlank() },
                                cellTexts.getOrNull(apellido2Idx)?.takeIf { it.isNotBlank() }
                            ).joinToString(" ")
                        }

                        if (name.isBlank()) continue

                        // Captain detection
                        val captainCell = if (captainIdx >= 0 && captainIdx < cells.size) cells[captainIdx] else null
                        val captainText = captainCell?.text()?.trim() ?: ""
                        val captainHasImg = captainCell?.select("img")?.isNotEmpty() == true
                        val isCaptain = captainText.equals("Sí", ignoreCase = true) ||
                            captainText.equals("Si", ignoreCase = true) ||
                            captainText.equals("S", ignoreCase = true) ||
                            captainText.equals("C", ignoreCase = true) ||
                            captainText.equals("✓") ||
                            captainText.equals("X", ignoreCase = true) ||
                            captainHasImg ||
                            row.html().contains("capit", ignoreCase = true)

                        // Points
                        val points = if (pointsIdx >= 0 && pointsIdx < cellTexts.size) {
                            cellTexts[pointsIdx].takeIf { it.isNotBlank() }
                        } else {
                            null
                        }

                        // Birth year
                        val birthYear = if (birthYearIdx >= 0 && birthYearIdx < cellTexts.size) {
                            cellTexts[birthYearIdx].takeIf { it.isNotBlank() }
                        } else {
                            null
                        }

                        val player = Player(
                            name = name,
                            isCaptain = isCaptain,
                            points = points,
                            birthYear = birthYear
                        )
                        players.add(player)
                        Log.d(TAG, "Player: '${player.name}' captain=${player.isCaptain} points=${player.points} year=${player.birthYear}")
                    }

                    if (players.isNotEmpty()) {
                        Log.d(TAG, "Found ${players.size} players in table $tableIdx")
                        break // Found the roster table, stop looking
                    }
                }
            }

            // Dump HTML if nothing found
            if (players.isEmpty()) {
                Log.w(TAG, "=== NO PLAYERS FOUND === Dumping first 3000 chars of HTML:")
                document.body().html().take(3000).chunked(1000).forEachIndexed { i, chunk ->
                    Log.w(TAG, "HTML chunk $i: $chunk")
                }
            }

            // If captainName was found, mark the matching player as captain
            if (captainName != null && players.none { it.isCaptain }) {
                val captainLabel = captainName!!
                val captainPlayer = players.indexOfFirst {
                    captainLabel.contains(it.name, ignoreCase = true) || it.name.contains(captainLabel, ignoreCase = true)
                }
                if (captainPlayer >= 0) {
                    players[captainPlayer] = players[captainPlayer].copy(isCaptain = true)
                    Log.d(TAG, "Marked player '${players[captainPlayer].name}' as captain based on captainName match")
                }
            }

            val result = TeamDetail(category = category, captainName = captainName, players = players)
            Log.d(TAG, "Result: ${players.size} players, captain=${result.captain?.name}, category=$category")
            result
        }.getOrElse { e ->
            Log.e(TAG, "Error parsing team detail", e)
            TeamDetail()
        }
    }

    companion object {
        private const val TAG = "TeamDetailParser"
    }
}
