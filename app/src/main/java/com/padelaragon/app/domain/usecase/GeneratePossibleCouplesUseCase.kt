package com.padelaragon.app.domain.usecase

import com.padelaragon.app.data.model.Gender

data class AgedPlayer(val name: String, val age: Int)

data class SuggestedPair(
    val player1: AgedPlayer,
    val player2: AgedPlayer,
    val tierIndex: Int,
    val requiredSum: Int
) {
    val ageSum: Int get() = player1.age + player2.age
}

data class CoupleCombination(val pairs: List<SuggestedPair>) {
    val totalAgeSum: Int get() = pairs.sumOf { it.ageSum }
}

/**
 * Android port of the desktop Veteranos pairing rule.  It deliberately has no Android
 * dependencies so the rule can be verified independently of the selection UI.
 */
class GeneratePossibleCouplesUseCase(private val maxSelectedPlayers: Int = 16) {
    companion object {
        const val MIN_REQUIRED_PLAYERS = 6
        val MASCULINA_THRESHOLDS = listOf(95, 100, 105)
        val FEMENINA_THRESHOLDS = listOf(85, 90, 95)
    }

    sealed interface Result {
        data class Success(val combinations: List<CoupleCombination>) : Result
        data object NoCombinationsPossible : Result
        data object NotEnoughPlayersSelected : Result
        data object TooManyPlayersSelected : Result
    }

    operator fun invoke(players: List<AgedPlayer>, gender: Gender): Result {
        if (players.size < MIN_REQUIRED_PLAYERS) return Result.NotEnoughPlayersSelected
        if (players.size > maxSelectedPlayers) return Result.TooManyPlayersSelected

        val thresholds = when (gender) {
            Gender.MASCULINA -> MASCULINA_THRESHOLDS
            Gender.FEMENINA -> FEMENINA_THRESHOLDS
        }
        val combinations = mutableListOf<CoupleCombination>()
        val seen = mutableSetOf<String>()

        triplesOfPairs(players) { pairs ->
            val ordered = pairs.sortedBy { (first, second) -> first.age + second.age }
            if (ordered.indices.all { index ->
                    ordered[index].first.age + ordered[index].second.age >= thresholds[index]
                }
            ) {
                val key = ordered.joinToString("|") { (first, second) ->
                    listOf(first.name, second.name).sorted().joinToString(",")
                }
                if (seen.add(key)) {
                    combinations += CoupleCombination(
                        ordered.mapIndexed { index, (first, second) ->
                            SuggestedPair(first, second, index, thresholds[index])
                        }
                    )
                }
            }
        }

        return combinations
            .sortedBy { it.totalAgeSum }
            .takeIf { it.isNotEmpty() }
            ?.let(Result::Success)
            ?: Result.NoCombinationsPossible
    }

    private fun triplesOfPairs(
        players: List<AgedPlayer>,
        onTriple: (List<Pair<AgedPlayer, AgedPlayer>>) -> Unit
    ) {
        val indexes = players.indices
        for (first in indexes) for (second in first + 1 until players.size) {
            val pairOne = players[first] to players[second]
            for (third in indexes) {
                if (third == first || third == second) continue
                for (fourth in third + 1 until players.size) {
                    if (fourth == first || fourth == second) continue
                    if (third < first || (third == first && fourth <= second)) continue
                    val pairTwo = players[third] to players[fourth]
                    for (fifth in indexes) {
                        if (fifth in setOf(first, second, third, fourth)) continue
                        for (sixth in fifth + 1 until players.size) {
                            if (sixth in setOf(first, second, third, fourth)) continue
                            if (fifth < third || (fifth == third && sixth <= fourth)) continue
                            onTriple(listOf(pairOne, pairTwo, players[fifth] to players[sixth]))
                        }
                    }
                }
            }
        }
    }
}
