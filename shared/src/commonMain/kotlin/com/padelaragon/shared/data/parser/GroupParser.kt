package com.padelaragon.shared.data.parser

import com.padelaragon.shared.util.Logger

import com.padelaragon.shared.data.model.Gender
import com.padelaragon.shared.data.model.LeagueGroup
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.io.IOException

class GroupParser {
    fun parse(html: String): List<LeagueGroup> {
        if (html.isBlank()) throw IOException("Empty HTML response received")

        val document = Ksoup.parse(html)
        val groupSelect = document.selectFirst("select[name=grupo]")

        if (groupSelect == null) {
            // Log a snippet of what we got for debugging
            val snippet = html.take(500)
            Logger.e("GroupParser", "No <select name=grupo> found. HTML snippet: $snippet")
            throw IOException("Could not find group selector in page. The website structure may have changed.")
        }

        val parsedGroups = mutableListOf<LeagueGroup>()

        // The website uses empty <optgroup> tags as separators (not as wrappers).
        // Options are siblings of optgroups, not children.
        // So we get ALL options directly and infer gender from the option text.
        val allOptions = groupSelect.select("option")

        // Try to build a gender map from optgroup positions
        // Walk through child nodes: when we see an optgroup, update current gender
        var currentGender: Gender = Gender.MASCULINA
        val optionGenderMap = mutableMapOf<String, Gender>()

        for (child in groupSelect.children()) {
            if (child.tagName() == "optgroup") {
                currentGender = parseGender(child.attr("label"))
            } else if (child.tagName() == "option") {
                val value = child.attr("value").trim()
                if (value.isNotEmpty()) {
                    optionGenderMap[value] = currentGender
                }
            }
        }

        allOptions.forEach { option ->
            val value = option.attr("value").trim()
            val text = option.text().trim()
            // Skip placeholder options
            if (value.isEmpty() || value == "--" || text.isEmpty()) return@forEach

            // Keep this local inference for debugging visibility in parse() logs.
            val gender = optionGenderMap[value] ?: parseGender(text)
            parseOption(option, gender)?.let { group ->
                Logger.d(
                    "GroupParser",
                    "Group: '${group.name}' → Gender: ${group.gender} (text contained: ${if (text.uppercase().contains("FEMEN")) "FEMENINA" else "MASCULINA"})"
                )
                parsedGroups.add(group)
            }
        }

        if (parsedGroups.isEmpty()) {
            Logger.w("GroupParser", "Parsed 0 groups from HTML")
        } else {
            Logger.d("GroupParser", "Parsed ${parsedGroups.size} groups")
        }

        return parsedGroups
    }

    fun parseJornadas(html: String): List<Int> {
        val document = Ksoup.parse(html)
        val jornadaSelect = document.selectFirst("select[name=jornada]")
        if (jornadaSelect == null) {
            Logger.w("GroupParser", "No <select name=jornada> found")
            return emptyList()
        }
        val jornadas = linkedSetOf<Int>()

        jornadaSelect.select("option").forEach { option ->
            val value = option.attr("value").trim()
            val text = option.text().trim()
            val parsed = value.toIntOrNull() ?: text.extractFirstInt()
            if (parsed != null && parsed > 0) {
                jornadas.add(parsed)
            }
        }

        return jornadas.toList()
    }

    private fun parseOption(option: Element, groupGender: Gender?): LeagueGroup? {
        val id = option.attr("value").trim().toIntOrNull() ?: return null
        val name = option.text().trim()
        if (name.isEmpty()) return null

        val category = CATEGORY_REGEX.find(name)?.groupValues?.get(1)?.trim() ?: name
        val groupLetter = GROUP_REGEX.find(name)?.groupValues?.get(1)?.trim()?.uppercase()
        // Always infer from text first since option text always contains MASCULINA/FEMENINA
        val gender = parseGender(name)

        return LeagueGroup(
            id = id,
            name = name,
            gender = gender,
            category = category,
            groupLetter = groupLetter
        )
    }

    private fun parseGender(text: String): Gender {
        val normalized = text.uppercase()
        return if (normalized.contains("FEMEN")) Gender.FEMENINA else Gender.MASCULINA
    }

    private fun String.extractFirstInt(): Int? =
        INT_REGEX.find(this)?.value?.toIntOrNull()

    companion object {
        private val CATEGORY_REGEX = Regex("(\\d+ª\\s+CATEGOR[ÍI]A)", RegexOption.IGNORE_CASE)
        private val GROUP_REGEX = Regex("GRUPO\\s+([A-Z0-9Ñ]+)", RegexOption.IGNORE_CASE)
        private val INT_REGEX = Regex("\\d+")
    }
}
