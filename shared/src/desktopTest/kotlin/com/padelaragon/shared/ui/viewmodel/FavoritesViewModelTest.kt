package com.padelaragon.shared.ui.viewmodel

import com.padelaragon.shared.data.model.Gender
import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.model.LeagueGroup
import com.padelaragon.shared.data.repository.datasource.FavoritesDataSource
import com.padelaragon.shared.data.repository.datasource.GroupDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private class FakeGroups(groups: List<LeagueGroup>) : GroupDataSource {
        val state = MutableStateFlow(groups)
        var getGroupsCalls = 0
        override fun observeGroups(): Flow<List<LeagueGroup>> = state
        override suspend fun getGroups(): List<LeagueGroup> = state.value.also { getGroupsCalls++ }
        override suspend fun refreshGroups(): List<LeagueGroup> = state.value
        override suspend fun prefetchAllGroups() = Unit
        override suspend fun prefetchGroups(groupIds: List<Int>) = Unit
    }

    private class FakeFavorites(ids: Set<Int>) : FavoritesDataSource {
        private val state = MutableStateFlow(ids)
        override val favorites: StateFlow<Set<Int>> = state
        override fun toggleFavorite(groupId: Int): Boolean {
            val added = groupId !in state.value
            state.value = if (added) state.value + groupId else state.value - groupId
            return added
        }
        override fun isFavorite(groupId: Int) = groupId in state.value
    }

    private fun group(id: Int, name: String, gender: Gender = Gender.MASCULINA) =
        LeagueGroup(id = id, name = name, gender = gender, category = "1ª", groupLetter = null)

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `lists favourites of every league in league order`() = runTest {
        val absolutaFavorites = FakeFavorites(setOf(2))
        val veteranosFavorites = FakeFavorites(setOf(20))
        val vm = FavoritesViewModel(
            listOf(
                FavoritesViewModel.LeagueSources(
                    League.ABSOLUTA, FakeGroups(listOf(group(1, "A1"), group(2, "A2"))), absolutaFavorites
                ),
                FavoritesViewModel.LeagueSources(
                    League.VETERANOS, FakeGroups(listOf(group(20, "V1"), group(21, "V2"))), veteranosFavorites
                )
            )
        )
        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf(League.ABSOLUTA to 2, League.VETERANOS to 20), state.entries.map { it.league to it.group.id })
        collector.cancel()
    }

    @Test
    fun `entry disappears when the favourite is removed`() = runTest {
        val favorites = FakeFavorites(setOf(1))
        val vm = FavoritesViewModel(
            listOf(FavoritesViewModel.LeagueSources(League.ABSOLUTA, FakeGroups(listOf(group(1, "A1"))), favorites))
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect { } }
        assertEquals(1, vm.uiState.value.entries.size)

        favorites.toggleFavorite(1)

        assertTrue(vm.uiState.value.entries.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `loads groups of leagues that have favourites but nothing cached`() = runTest {
        val withFavorites = FakeGroups(emptyList())
        val without = FakeGroups(emptyList())
        FavoritesViewModel(
            listOf(
                FavoritesViewModel.LeagueSources(League.ABSOLUTA, withFavorites, FakeFavorites(setOf(1))),
                FavoritesViewModel.LeagueSources(League.VETERANOS, without, FakeFavorites(emptySet()))
            )
        )

        assertEquals(1, withFavorites.getGroupsCalls)
        assertEquals(0, without.getGroupsCalls)
    }
}
