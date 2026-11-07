package com.padelaragon.shared.data.favorites

import com.padelaragon.shared.data.model.League
import com.padelaragon.shared.data.repository.datasource.FavoritesDataSource
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Favourite group ids of one [League], capped at [MAX_FAVORITES]. Stored comma-joined under
 * `favorite_group_ids_<leagueId>` so every league keeps its own independent set.
 */
class FavoritesStore(private val league: League, private val settings: Settings) : FavoritesDataSource {

    private val _favorites = MutableStateFlow(read())
    override val favorites: StateFlow<Set<Int>> = _favorites.asStateFlow()

    override fun toggleFavorite(groupId: Int): Boolean {
        val current = _favorites.value
        if (groupId in current) {
            update(current - groupId)
            return false
        }
        if (current.size >= MAX_FAVORITES) return false
        update(current + groupId)
        return true
    }

    override fun isFavorite(groupId: Int): Boolean = groupId in _favorites.value

    private fun update(updated: Set<Int>) {
        settings.putString(keyFor(league), updated.joinToString(","))
        _favorites.value = updated
    }

    private fun read(): Set<Int> =
        settings.getString(keyFor(league), "").split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

    companion object {
        const val MAX_FAVORITES = 3

        fun keyFor(league: League) = "favorite_group_ids_${league.id}"

        /**
         * One-time import of the pre-3.0 Android favourites (a single, league-less set) into
         * [League.ABSOLUTA], the only league that app version knew. Returns true when imported.
         */
        fun migrateLegacy(settings: Settings, legacyGroupIds: Set<Int>): Boolean {
            val key = keyFor(League.ABSOLUTA)
            if (legacyGroupIds.isEmpty() || settings.hasKey(key)) return false
            settings.putString(key, legacyGroupIds.sorted().take(MAX_FAVORITES).joinToString(","))
            return true
        }
    }
}
