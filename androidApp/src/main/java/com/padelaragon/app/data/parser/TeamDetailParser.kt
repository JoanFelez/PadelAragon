package com.padelaragon.app.data.parser

import android.util.Log
import com.padelaragon.app.data.model.Player
import com.padelaragon.app.data.model.TeamDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class TeamDetailParser {

    fun parse(html: String): TeamDetail {
        if (html.isBlank()) {
            Log.w(TAG, "Empty HTML received")
            return TeamDetail()
        }

        return runCatching {
            val document = Jsoup.parse(html)

            val (category, captainName) = extractMetadata(document)
            val players = extractPlayers(document).toMutableList()
            applyCaptainFromMetadata(players, captainName)

            TeamDetail(category = category, captainName = captainName, players = players).also {
                Log.d(TAG, "Result: ${players.size} players, captain=${it.captain?.name}, category=$category")
            }
        }.getOrElse { e ->
            Log.e(TAG, "Error parsing team detail", e)
            TeamDetail()
        }
    }

    // ── Metadata ──────────────────────────────────────────────

    private fun extractMetadata(doc: Document): Pair<String?, String?> {
        val rankingDiv = doc.selectFirst("div.ranking")
            ?: return extractCategoryFallback(doc) to null

        var category: String? = null
        var captainName: String? = null

        // Primary: <em> label → next <strong> value
        for (em in rankingDiv.select("em")) {
            val label = em.text().trim().lowercase()
            val value = findNextStrong(em)?.text()?.trim()?.takeIf { it.isNotBlank() } ?: continue

            when {
                (label.contains("categor") || label.contains("grupo")) && category == null -> category = value
                label.contains("capit") -> captainName = value
            }
        }

        // Fallback: regex on inner HTML
        val tdHtml = rankingDiv.selectFirst("td")?.html()
        if (tdHtml != null) {
            if (captainName == null)
                captainName = CAPTAIN_REGEX.find(tdHtml)?.groupValues?.get(1)?.trim()
            if (category == null)
                category = CATEGORY_REGEX.find(tdHtml)?.groupValues?.get(1)?.trim()
        }

        return category to captainName
    }

    private fun findNextStrong(em: Element): Element? {
        var sibling = em.nextElementSibling()
        while (sibling != null && sibling.tagName() != "strong") sibling = sibling.nextElementSibling()
        return sibling
    }

    private fun extractCategoryFallback(doc: Document): String? {
        val bodyHtml = doc.body()?.html() ?: return null
        val match = CATEGORY_FALLBACK_REGEX.find(bodyHtml) ?: return null
        return match.value.take(150).trim()
    }

    // ── Players ───────────────────────────────────────────────

    private fun extractPlayers(doc: Document): List<Player> {
        // Fast path: federation-specific rows
        val lineasRnkRows = doc.select("tr.LineasRnk")
        if (lineasRnkRows.isNotEmpty()) {
            val table = lineasRnkRows.first()!!.parents().firstOrNull { it.tagName() == "table" }
            if (table != null) {
                val header = findHeaderRow(table.select("tr"))
                if (header != null) {
                    val mapping = mapColumns(header)
                    if (mapping.hasNameColumn) {
                        val players = parseRows(lineasRnkRows, mapping)
                        if (players.isNotEmpty()) return players
                    }
                }
            }
        }

        // Generic fallback: scan all tables
        val tableWithPlayers = doc.select("table").firstNotNullOfOrNull { table ->
            val rows = table.select("tr")
            if (rows.size < 2) return@firstNotNullOfOrNull null

            val header = findHeaderRow(rows) ?: return@firstNotNullOfOrNull null
            val mapping = mapColumns(header)
            if (!mapping.hasNameColumn) return@firstNotNullOfOrNull null

            val headerIdx = rows.indexOf(header)
            val dataRows = rows.subList(headerIdx + 1, rows.size)

            val resolvedMapping = if (mapping.captainIdx == -1)
                mapping.copy(captainIdx = autoDetectCaptainColumn(dataRows, mapping.columnCount))
            else mapping

            val players = parseRows(dataRows, resolvedMapping)
            players.takeIf { it.isNotEmpty() }
        }

        return tableWithPlayers ?: emptyList()
    }

    private fun findHeaderRow(rows: List<Element>): Element? =
        rows.firstOrNull { row ->
            row.select("th, td").any { cell ->
                val t = cell.text().trim().lowercase()
                t.contains("nombre") || t.contains("jugador") || t.contains("apellido")
            }
        }

    // ── Column Mapping ────────────────────────────────────────

    private data class ColumnMapping(
        val nombreIdx: Int = -1,
        val apellido1Idx: Int = -1,
        val apellido2Idx: Int = -1,
        val fullNameIdx: Int = -1,
        val captainIdx: Int = -1,
        val pointsIdx: Int = -1,
        val birthYearIdx: Int = -1,
        val columnCount: Int = 0,
    ) {
        val hasNameColumn get() = nombreIdx >= 0 || fullNameIdx >= 0
    }

    private fun mapColumns(headerRow: Element): ColumnMapping {
        val headers = headerRow.select("th, td").map { it.text().trim().lowercase() }
        var m = ColumnMapping(columnCount = headers.size)

        headers.forEachIndexed { i, h ->
            m = when {
                h.contains("jugador") || h == "nombre completo" || h == "nombre y apellidos" ->
                    m.copy(fullNameIdx = i)
                h == "nombre" || h in listOf("nom", "nom.") ->
                    m.copy(nombreIdx = i)
                (h.contains("1") && h.contains("apellido")) || h == "apellido 1" || h == "primer apellido" || h == "1er apellido" ->
                    m.copy(apellido1Idx = i)
                (h.contains("2") && h.contains("apellido")) || h == "apellido 2" || h == "segundo apellido" || h in listOf("2o apellido", "2º apellido") ->
                    m.copy(apellido2Idx = i)
                h.contains("apellido") && m.apellido1Idx == -1 ->
                    m.copy(apellido1Idx = i)
                h.contains("capit") || h in listOf("c", "c.", "cap", "cap.") ->
                    m.copy(captainIdx = i)
                h.contains("punto") || h.contains("ranking") || h.contains("rnk") || h.contains("pts") ->
                    m.copy(pointsIdx = i)
                h.contains("año") || h.contains("nacimiento") || h.contains("nac") || h.contains("f.nac") || h.contains("fnac") ->
                    m.copy(birthYearIdx = i)
                else -> m
            }
        }
        return m
    }

    // ── Row Parsing ───────────────────────────────────────────

    private fun parseRows(rows: Iterable<Element>, mapping: ColumnMapping): List<Player> =
        rows.mapNotNull { row -> parseRow(row, mapping) }

    private fun parseRow(row: Element, m: ColumnMapping): Player? {
        val cells = row.select("td")
        if (cells.isEmpty()) return null

        val texts = cells.map { it.text().trim() }
        if (texts.all { it.isEmpty() }) return null

        val name = if (m.fullNameIdx in texts.indices) {
            texts[m.fullNameIdx]
        } else {
            listOfNotNull(
                texts.getOrNull(m.nombreIdx)?.takeIf { it.isNotBlank() },
                texts.getOrNull(m.apellido1Idx)?.takeIf { it.isNotBlank() },
                texts.getOrNull(m.apellido2Idx)?.takeIf { it.isNotBlank() },
            ).joinToString(" ")
        }
        if (name.isBlank()) return null

        val captainCell = cells.getOrNull(m.captainIdx)
        val isCaptain = isCaptainCell(captainCell)

        return Player(
            name = name,
            isCaptain = isCaptain,
            points = texts.getOrNull(m.pointsIdx)?.takeIf { it.isNotBlank() },
            birthYear = texts.getOrNull(m.birthYearIdx)?.takeIf { it.isNotBlank() },
        )
    }

    // ── Captain Detection ─────────────────────────────────────

    private fun isCaptainCell(cell: Element?): Boolean {
        if (cell == null) return false
        val text = cell.text().trim()
        return text.lowercase() in CAPTAIN_VALUES || cell.select("img").isNotEmpty()
    }

    private fun autoDetectCaptainColumn(dataRows: List<Element>, numCols: Int): Int {
        for (colIdx in 0 until numCols) {
            val values = dataRows.mapNotNull { it.select("td").getOrNull(colIdx)?.text()?.trim() }
            if (values.size < 2) continue

            val captainCount = values.count { it.lowercase() in CAPTAIN_VALUES }
            val emptyOrNo = values.count { it.isEmpty() || it.lowercase() in listOf("no", "n", "-") }

            if (captainCount == 1 && captainCount + emptyOrNo == values.size) return colIdx
        }
        // Fallback: single-image column
        for (colIdx in 0 until numCols) {
            val imgCount = dataRows.count { it.select("td").getOrNull(colIdx)?.select("img")?.isNotEmpty() == true }
            if (imgCount == 1 && dataRows.size > 1) return colIdx
        }
        return -1
    }

    private fun applyCaptainFromMetadata(players: MutableList<Player>, captainName: String?) {
        if (captainName == null || players.any { it.isCaptain }) return
        val idx = players.indexOfFirst {
            captainName.contains(it.name, ignoreCase = true) || it.name.contains(captainName, ignoreCase = true)
        }
        if (idx >= 0) players[idx] = players[idx].copy(isCaptain = true)
    }

    companion object {
        private const val TAG = "TeamDetailParser"
        private val CAPTAIN_VALUES = setOf("sí", "si", "s", "c", "✓", "x")
        private val CAPTAIN_REGEX = Regex("""(?i)capit[aá]n\s*</em>\s*:?\s*<strong>([^<]+)</strong>""")
        private val CATEGORY_REGEX = Regex("""(?i)categor[ií]a/?grupo\s*</em>\s*:?\s*<strong>([^<]+)</strong>""")
        private val CATEGORY_FALLBACK_REGEX = Regex("""(?i)\b((?:categor[ií]a|grupo)\s*[^<]{0,100}(?:masculin|femenin)[^<]{0,50})""")
    }
}
