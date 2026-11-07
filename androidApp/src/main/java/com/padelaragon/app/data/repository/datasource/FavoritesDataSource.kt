package com.padelaragon.app.data.repository.datasource

import kotlinx.coroutines.flow.StateFlow

interface FavoritesDataSource {
    val favorites: StateFlow<Set<Int>>
    fun toggleFavorite(groupId: Int): Boolean
    fun isFavorite(groupId: Int): Boolean
}
