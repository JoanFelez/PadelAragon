package com.padelaragon.shared.domain.usecase

import com.padelaragon.shared.data.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratePossibleCouplesUseCaseTest {

    private val useCase = GeneratePossibleCouplesUseCase()

    @Test
    fun `returns NotEnoughPlayersSelected when fewer than 6 players selected`() {
        val players = listOf(
            AgedPlayer("A", 50), AgedPlayer("B", 50), AgedPlayer("C", 50)
        )
        val result = useCase(players, Gender.MASCULINA)
        assertEquals(GeneratePossibleCouplesUseCase.Result.NotEnoughPlayersSelected, result)
    }

    @Test
    fun `returns NoCombinationsPossible when ages too low for masculina thresholds`() {
        val players = (1..6).map { AgedPlayer("P$it", 40) } // sums always 80, none reach 95
        val result = useCase(players, Gender.MASCULINA)
        assertEquals(GeneratePossibleCouplesUseCase.Result.NoCombinationsPossible, result)
    }

    @Test
    fun `finds a valid masculina combination at exact thresholds`() {
        // Sums: 95, 100, 105 exactly
        val players = listOf(
            AgedPlayer("A", 45), AgedPlayer("B", 50), // 95
            AgedPlayer("C", 48), AgedPlayer("D", 52), // 100
            AgedPlayer("E", 50), AgedPlayer("F", 55)  // 105
        )
        val result = useCase(players, Gender.MASCULINA) as GeneratePossibleCouplesUseCase.Result.Success
        assertTrue(result.combinations.isNotEmpty())
        val combo = result.combinations.first()
        assertEquals(3, combo.pairs.size)
        assertEquals(listOf(95, 100, 105), combo.pairs.map { it.ageSum })
        assertEquals(listOf(95, 100, 105), combo.pairs.map { it.requiredSum })
    }

    @Test
    fun `finds a valid femenina combination`() {
        val players = listOf(
            AgedPlayer("A", 42), AgedPlayer("B", 43), // 85
            AgedPlayer("C", 44), AgedPlayer("D", 46), // 90
            AgedPlayer("E", 47), AgedPlayer("F", 48)  // 95
        )
        val result = useCase(players, Gender.FEMENINA) as GeneratePossibleCouplesUseCase.Result.Success
        assertTrue(result.combinations.isNotEmpty())
        assertEquals(listOf(85, 90, 95), result.combinations.first().pairs.map { it.ageSum })
    }

    @Test
    fun `no duplicate combinations reported`() {
        val players = listOf(
            AgedPlayer("A", 45), AgedPlayer("B", 50),
            AgedPlayer("C", 48), AgedPlayer("D", 52),
            AgedPlayer("E", 50), AgedPlayer("F", 55)
        )
        val result = useCase(players, Gender.MASCULINA) as GeneratePossibleCouplesUseCase.Result.Success
        val keys = result.combinations.map { combo ->
            combo.pairs.map { setOf(it.player1.name, it.player2.name) }.toSet()
        }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `returns TooManyPlayersSelected beyond limit`() {
        val useCaseSmallLimit = GeneratePossibleCouplesUseCase(maxSelectedPlayers = 6)
        val players = (1..7).map { AgedPlayer("P$it", 50) }
        val result = useCaseSmallLimit(players, Gender.MASCULINA)
        assertEquals(GeneratePossibleCouplesUseCase.Result.TooManyPlayersSelected, result)
    }

    @Test
    fun `allows leaving out extra selected players not part of a valid combination`() {
        // 8 players selected (a manager's larger pool); only 6 are needed to form a valid
        // combination — extras must not block finding one, and results must include groupings
        // that leave the too-young players out.
        val players = listOf(
            AgedPlayer("A", 45), AgedPlayer("B", 50), // 95
            AgedPlayer("C", 48), AgedPlayer("D", 52), // 100
            AgedPlayer("E", 50), AgedPlayer("F", 55), // 105
            AgedPlayer("G", 20), AgedPlayer("H", 20)  // too young, should just be excluded
        )
        val result = useCase(players, Gender.MASCULINA) as GeneratePossibleCouplesUseCase.Result.Success
        assertTrue(result.combinations.isNotEmpty())
    }

    @Test
    fun `results are sorted with the tightest combinations first`() {
        val players = listOf(
            AgedPlayer("A", 45), AgedPlayer("B", 50), // 95 (tight)
            AgedPlayer("C", 48), AgedPlayer("D", 52), // 100
            AgedPlayer("E", 50), AgedPlayer("F", 55), // 105
            AgedPlayer("G", 60), AgedPlayer("H", 65)  // 125 (loose, extra pool)
        )
        val result = useCase(players, Gender.MASCULINA) as GeneratePossibleCouplesUseCase.Result.Success
        val sums = result.combinations.map { it.totalAgeSum }
        assertEquals(sums.sorted(), sums)
    }
}
