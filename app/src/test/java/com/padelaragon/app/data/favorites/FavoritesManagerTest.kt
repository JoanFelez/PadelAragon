package com.padelaragon.app.data.favorites

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [FavoritesManager] covering initialization, toggle behavior,
 * the max-favorites cap (3), persistence/reload via SharedPreferences, and
 * the StateFlow contract.
 *
 * Uses reflection to inject an [InMemorySharedPreferences] into the singleton,
 * avoiding the need for Robolectric or an Android device.
 */
class FavoritesManagerTest {

    private lateinit var prefs: InMemorySharedPreferences

    @Before
    fun setUp() {
        prefs = InMemorySharedPreferences()
        injectPrefs(prefs)
        resetFavoritesFlow()
    }

    // ── Initialization ──────────────────────────────────────────

    @Test
    fun `initial state is empty after fresh setup`() {
        assertTrue(FavoritesManager.favorites.value.isEmpty())
    }

    @Test
    fun `init reloads persisted favorites from SharedPreferences`() {
        // Pre-populate preferences as if a previous session saved favorites
        prefs.edit().putStringSet("favorite_group_ids", mutableSetOf("10", "20")).apply()

        // Simulate re-initialization
        reInitFromPrefs()

        assertEquals(setOf(10, 20), FavoritesManager.favorites.value)
    }

    @Test
    fun `init ignores non-integer values in stored set`() {
        prefs.edit().putStringSet("favorite_group_ids", mutableSetOf("5", "abc", "")).apply()

        reInitFromPrefs()

        assertEquals(setOf(5), FavoritesManager.favorites.value)
    }

    // ── Toggle: add ─────────────────────────────────────────────

    @Test
    fun `toggleFavorite adds a new favorite and returns true`() {
        val result = FavoritesManager.toggleFavorite(100)

        assertTrue(result)
        assertTrue(FavoritesManager.isFavorite(100))
        assertEquals(setOf(100), FavoritesManager.favorites.value)
    }

    @Test
    fun `toggleFavorite persists addition to SharedPreferences`() {
        FavoritesManager.toggleFavorite(42)

        val stored = prefs.getStringSet("favorite_group_ids", mutableSetOf())
        assertTrue(stored!!.contains("42"))
    }

    // ── Toggle: remove ──────────────────────────────────────────

    @Test
    fun `toggleFavorite removes existing favorite and returns false`() {
        FavoritesManager.toggleFavorite(10) // add
        val result = FavoritesManager.toggleFavorite(10) // remove

        assertFalse(result)
        assertFalse(FavoritesManager.isFavorite(10))
        assertTrue(FavoritesManager.favorites.value.isEmpty())
    }

    @Test
    fun `toggleFavorite removal persists to SharedPreferences`() {
        FavoritesManager.toggleFavorite(10)
        FavoritesManager.toggleFavorite(10)

        val stored = prefs.getStringSet("favorite_group_ids", mutableSetOf())
        assertTrue(stored!!.isEmpty())
    }

    // ── Max-favorites cap ───────────────────────────────────────

    @Test
    fun `max favorites cap is 3 - fourth add returns false`() {
        FavoritesManager.toggleFavorite(1)
        FavoritesManager.toggleFavorite(2)
        FavoritesManager.toggleFavorite(3)

        val result = FavoritesManager.toggleFavorite(4)

        assertFalse(result)
        assertFalse(FavoritesManager.isFavorite(4))
        assertEquals(3, FavoritesManager.favorites.value.size)
    }

    @Test
    fun `removing one favorite allows adding another up to cap`() {
        FavoritesManager.toggleFavorite(1)
        FavoritesManager.toggleFavorite(2)
        FavoritesManager.toggleFavorite(3)
        FavoritesManager.toggleFavorite(2) // remove 2

        val result = FavoritesManager.toggleFavorite(4)

        assertTrue(result)
        assertEquals(setOf(1, 3, 4), FavoritesManager.favorites.value)
    }

    @Test
    fun `cap check is based on current count, not lifetime additions`() {
        // Add and remove several times to build up "lifetime" count
        FavoritesManager.toggleFavorite(1)
        FavoritesManager.toggleFavorite(1)
        FavoritesManager.toggleFavorite(2)
        FavoritesManager.toggleFavorite(2)

        // Current set is empty, so we should be able to add 3
        FavoritesManager.toggleFavorite(10)
        FavoritesManager.toggleFavorite(20)
        FavoritesManager.toggleFavorite(30)

        assertEquals(3, FavoritesManager.favorites.value.size)
    }

    // ── isFavorite ──────────────────────────────────────────────

    @Test
    fun `isFavorite returns false for unknown id`() {
        assertFalse(FavoritesManager.isFavorite(999))
    }

    @Test
    fun `isFavorite returns true for added id`() {
        FavoritesManager.toggleFavorite(42)
        assertTrue(FavoritesManager.isFavorite(42))
    }

    // ── Persistence reload ──────────────────────────────────────

    @Test
    fun `favorites survive re-initialization from same SharedPreferences`() {
        FavoritesManager.toggleFavorite(100)
        FavoritesManager.toggleFavorite(200)

        // Simulate app restart by re-reading from the same backing prefs
        reInitFromPrefs()

        assertEquals(setOf(100, 200), FavoritesManager.favorites.value)
        assertTrue(FavoritesManager.isFavorite(100))
        assertTrue(FavoritesManager.isFavorite(200))
    }

    @Test
    fun `removal persists across re-initialization`() {
        FavoritesManager.toggleFavorite(10)
        FavoritesManager.toggleFavorite(20)
        FavoritesManager.toggleFavorite(10) // remove

        reInitFromPrefs()

        assertEquals(setOf(20), FavoritesManager.favorites.value)
        assertFalse(FavoritesManager.isFavorite(10))
    }

    // ── Helpers ─────────────────────────────────────────────────

    /**
     * Inject our [InMemorySharedPreferences] into the singleton via reflection.
     */
    private fun injectPrefs(prefs: InMemorySharedPreferences) {
        val field = FavoritesManager::class.java.getDeclaredField("sharedPreferences")
        field.isAccessible = true
        field.set(FavoritesManager, prefs)
    }

    /**
     * Reset the internal [MutableStateFlow] to empty so tests start clean.
     */
    private fun resetFavoritesFlow() {
        val field = FavoritesManager::class.java.getDeclaredField("_favorites")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(FavoritesManager) as MutableStateFlow<Set<Int>>
        flow.value = emptySet()
    }

    /**
     * Simulate [FavoritesManager.init] re-reading from the already-injected
     * SharedPreferences. Uses reflection to call the private [readFavoritesFromPrefs].
     */
    private fun reInitFromPrefs() {
        val method = FavoritesManager::class.java.getDeclaredMethod("readFavoritesFromPrefs")
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val loaded = method.invoke(FavoritesManager) as Set<Int>

        val field = FavoritesManager::class.java.getDeclaredField("_favorites")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(FavoritesManager) as MutableStateFlow<Set<Int>>
        flow.value = loaded
    }
}
