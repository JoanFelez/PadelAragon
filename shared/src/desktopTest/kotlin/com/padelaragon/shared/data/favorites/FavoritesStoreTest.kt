package com.padelaragon.shared.data.favorites

import com.padelaragon.shared.data.model.League
import com.russhwolf.settings.MapSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoritesStoreTest {

    @Test
    fun `favorites are isolated per league`() {
        val settings = MapSettings()
        val absoluta = FavoritesStore(League.ABSOLUTA, settings)
        val veteranos = FavoritesStore(League.VETERANOS, settings)

        absoluta.toggleFavorite(100)
        veteranos.toggleFavorite(200)

        assertTrue(absoluta.isFavorite(100))
        assertFalse(absoluta.isFavorite(200))
        assertTrue(veteranos.isFavorite(200))
        assertFalse(veteranos.isFavorite(100))
    }

    @Test
    fun `each league is capped at 3 favorites independently`() {
        val settings = MapSettings()
        val absoluta = FavoritesStore(League.ABSOLUTA, settings)
        val menores = FavoritesStore(League.MENORES, settings)

        listOf(1, 2, 3).forEach { absoluta.toggleFavorite(it) }
        assertFalse(absoluta.toggleFavorite(4))
        assertEquals(3, absoluta.favorites.value.size)

        listOf(11, 12, 13).forEach { menores.toggleFavorite(it) }
        assertEquals(3, menores.favorites.value.size)
    }

    @Test
    fun `favorites persist across store instances using the per-league key`() {
        val settings = MapSettings()
        FavoritesStore(League.ABSOLUTA, settings).toggleFavorite(7)

        assertEquals("7", settings.getString("favorite_group_ids_${League.ABSOLUTA.id}", ""))
        assertEquals(setOf(7), FavoritesStore(League.ABSOLUTA, settings).favorites.value)
    }

    @Test
    fun `desktop comma-joined format is read as is`() {
        val settings = MapSettings("favorite_group_ids_${League.ABSOLUTA.id}" to "5, 9")
        assertEquals(setOf(5, 9), FavoritesStore(League.ABSOLUTA, settings).favorites.value)
    }

    @Test
    fun `legacy android favorites migrate to ABSOLUTA once`() {
        val settings = MapSettings()

        assertTrue(FavoritesStore.migrateLegacy(settings, setOf(30, 10, 20, 40)))
        assertEquals(setOf(10, 20, 30), FavoritesStore(League.ABSOLUTA, settings).favorites.value)
        assertTrue(FavoritesStore(League.VETERANOS, settings).favorites.value.isEmpty())

        FavoritesStore(League.ABSOLUTA, settings).toggleFavorite(10)
        assertFalse(FavoritesStore.migrateLegacy(settings, setOf(99)))
        assertEquals(setOf(20, 30), FavoritesStore(League.ABSOLUTA, settings).favorites.value)
    }
}
