package com.padelaragon.app.domain.usecase

import com.padelaragon.app.data.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratePossibleCouplesUseCaseTest {
    private val useCase = GeneratePossibleCouplesUseCase()

    @Test
    fun `rejects selections outside six through sixteen players`() {
        val players = (1..17).map { AgedPlayer("P$it", 50) }
        assertEquals(GeneratePossibleCouplesUseCase.Result.NotEnoughPlayersSelected, useCase(players.take(5), Gender.MASCULINA))
        assertEquals(GeneratePossibleCouplesUseCase.Result.TooManyPlayersSelected, useCase(players, Gender.MASCULINA))
    }

    @Test
    fun `builds three disjoint ordered pairs using masculine thresholds`() {
        val result = useCase((1..6).map { AgedPlayer("P$it", 55) }, Gender.MASCULINA)
        assertTrue(result is GeneratePossibleCouplesUseCase.Result.Success)
        val combination = (result as GeneratePossibleCouplesUseCase.Result.Success).combinations.first()
        assertEquals(listOf(95, 100, 105), combination.pairs.map { it.requiredSum })
        assertEquals(6, combination.pairs.flatMap { listOf(it.player1.name, it.player2.name) }.toSet().size)
    }

    @Test
    fun `reports no combinations separately from invalid selection`() {
        val result = useCase((1..6).map { AgedPlayer("P$it", 20) }, Gender.FEMENINA)
        assertEquals(GeneratePossibleCouplesUseCase.Result.NoCombinationsPossible, result)
    }

    @Test
    fun `matches desktop ordering and removes duplicate combinations`() {
        val players = listOf(
            AgedPlayer("A", 45), AgedPlayer("B", 50),
            AgedPlayer("C", 48), AgedPlayer("D", 52),
            AgedPlayer("E", 50), AgedPlayer("F", 55),
            AgedPlayer("G", 60), AgedPlayer("H", 65)
        )

        val result = useCase(players, Gender.MASCULINA) as GeneratePossibleCouplesUseCase.Result.Success
        val combinationKeys = result.combinations.map { combination ->
            combination.pairs.map { pair ->
                listOf(pair.player1.name, pair.player2.name).sorted().joinToString("-")
            }.toSet()
        }

        assertEquals(combinationKeys.size, combinationKeys.toSet().size)
        assertEquals(
            result.combinations.map { it.totalAgeSum }.sorted(),
            result.combinations.map { it.totalAgeSum }
        )
        assertTrue(result.combinations.all { combination ->
            combination.pairs.flatMap { listOf(it.player1.name, it.player2.name) }.toSet().size == 6
        })
    }
}
