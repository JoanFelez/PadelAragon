package com.padelaragon.shared.data.remote

import com.padelaragon.shared.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.shared.data.repository.resolveUrl

/** padelfederacion.es URLs used by the scraper, mirroring what the on-device repositories request. */
object FederationUrls {
    const val STANDINGS = "${BASE_URL}Ligas_Clasificacion.asp"

    fun leagueCalendar(leagueId: Int) = "${BASE_URL}Ligas_Calendario.asp?Liga=$leagueId"

    fun groupCalendar(leagueId: Int, groupId: Int) = "${leagueCalendar(leagueId)}&grupo=$groupId"

    fun jornada(leagueId: Int, groupId: Int, jornada: Int) = "${groupCalendar(leagueId, groupId)}&jornada=$jornada"

    fun matchDetail(detailUrl: String) = resolveUrl(BASE_URL, detailUrl)

    /** The team's own link when the standings provide one; otherwise the known page variants, in order. */
    fun teamDetailCandidates(leagueId: Int, teamId: Int, teamHref: String): List<String> {
        val href = teamHref.trim()
        if (href.isNotEmpty()) return listOf(teamHrefToUrl(href))
        return listOf(
            "${BASE_URL}Ligas_FichaEquipo.asp?Liga=$leagueId&IdEquipo=$teamId",
            "${BASE_URL}Ligas_Equipo.asp?Liga=$leagueId&IdEquipo=$teamId",
            "${BASE_URL}Ligas_Clasificacion.asp?Liga=$leagueId&IdEquipo=$teamId"
        )
    }

    private fun teamHrefToUrl(href: String): String {
        if (href.startsWith("http://", ignoreCase = true) || href.startsWith("https://", ignoreCase = true)) return href
        if (href.startsWith("//")) return "https:$href"
        if (href.contains("padelfederacion.es", ignoreCase = true)) {
            return if (href.startsWith("/")) "https://padelfederacion.es$href" else "https://$href"
        }
        return resolveUrl(BASE_URL, href)
    }
}
