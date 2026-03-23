package com.padelaragon.app.domain.usecase

import com.padelaragon.app.data.model.MatchResult

class FindDefaultJornadaUseCase {
    operator fun invoke(
        sortedJornadas: List<Int>,
        allResults: Map<Int, List<MatchResult>>
    ): Int? {
        val lastWithResults = sortedJornadas.lastOrNull { jornada ->
            allResults[jornada]?.any { it.localScore != "--" && it.visitorScore != "--" } == true
        }
        return lastWithResults ?: sortedJornadas.firstOrNull()
    }
}
