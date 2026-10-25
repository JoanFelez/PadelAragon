package com.padelaragon.app.data.favorites

import android.content.Context
import android.content.SharedPreferences
import com.padelaragon.app.data.model.League
import com.padelaragon.app.data.repository.datasource.FavoritesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesManager : FavoritesDataSource {
    private const val PREFS_NAME = "favorites"
    private const val KEY_FAVORITE_GROUP_IDS = "favorite_group_ids"
    private const val MAX_FAVORITES = 3

    private lateinit var sharedPreferences: SharedPreferences

    private val _favorites = MutableStateFlow<Set<Int>>(emptySet())
    override val favorites: StateFlow<Set<Int>> = _favorites.asStateFlow()

    fun init(context: Context) {
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _favorites.value = readFavoritesFromPrefs()
    }

    override fun toggleFavorite(groupId: Int): Boolean {
        val currentFavorites = _favorites.value

        if (currentFavorites.contains(groupId)) {
            val updated = currentFavorites.toMutableSet().apply { remove(groupId) }
            persistFavorites(updated)
            _favorites.value = updated
            return false
        }

        if (currentFavorites.size >= MAX_FAVORITES) {
            return false
        }

        val updated = currentFavorites.toMutableSet().apply { add(groupId) }
        persistFavorites(updated)
        _favorites.value = updated
        return true
    }

    override fun isFavorite(groupId: Int): Boolean {
        return _favorites.value.contains(groupId)
    }

    private fun readFavoritesFromPrefs(): Set<Int> {
        val storedSet = prefs().getStringSet(KEY_FAVORITE_GROUP_IDS, emptySet()) ?: emptySet()
        val copiedSet = HashSet(storedSet)
        return copiedSet.mapNotNull { it.toIntOrNull() }.toSet()
    }

    private fun persistFavorites(favorites: Set<Int>) {
        val asStringSet = HashSet(favorites.map { it.toString() })
        prefs().edit()
            .putStringSet(KEY_FAVORITE_GROUP_IDS, asStringSet)
            .apply()
    }

    private fun prefs(): SharedPreferences {
        check(::sharedPreferences.isInitialized) {
            "FavoritesManager.init(context) must be called before using FavoritesManager"
        }
        return sharedPreferences
    }

    fun forLeague(league: League): FavoritesDataSource = LeagueFavoritesDataSource(prefs(), league)
}

private class LeagueFavoritesDataSource(
    private val preferences: SharedPreferences,
    league: League
) : FavoritesDataSource {
    private val key = "favorite_group_ids_${league.id}"
    private val _favorites = MutableStateFlow(read())
    override val favorites: StateFlow<Set<Int>> = _favorites.asStateFlow()

    override fun toggleFavorite(groupId: Int): Boolean {
        val current = _favorites.value
        val updated = if (groupId in current) current - groupId
        else {
            if (current.size >= 3) return false
            current + groupId
        }
        preferences.edit().putStringSet(key, HashSet(updated.map(Int::toString))).apply()
        _favorites.value = updated
        return groupId in updated
    }

    override fun isFavorite(groupId: Int): Boolean = groupId in _favorites.value

    private fun read(): Set<Int> =
        HashSet(preferences.getStringSet(key, emptySet()) ?: emptySet())
            .mapNotNull(String::toIntOrNull)
            .toSet()
}
