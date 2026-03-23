package com.padelaragon.app.data.favorites

import android.content.SharedPreferences

/**
 * Minimal in-memory [SharedPreferences] for testing [FavoritesManager]
 * without Robolectric or an Android device. Supports only the operations
 * FavoritesManager actually uses: [getStringSet] and [Editor.putStringSet].
 */
class InMemorySharedPreferences : SharedPreferences {

    private val store = mutableMapOf<String, Any?>()

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? {
        @Suppress("UNCHECKED_CAST")
        val stored = store[key] as? Set<String> ?: return defValues
        // Return a defensive copy, matching Android SharedPreferences behavior
        return HashSet(stored)
    }

    override fun getString(key: String?, defValue: String?): String? = defValue
    override fun getInt(key: String?, defValue: Int): Int = defValue
    override fun getLong(key: String?, defValue: Long): Long = defValue
    override fun getFloat(key: String?, defValue: Float): Float = defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = defValue
    override fun contains(key: String?): Boolean = store.containsKey(key)
    override fun getAll(): MutableMap<String, *> = store.toMutableMap()

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clear = false

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
            pending[key] = values?.let { HashSet(it) }
            return this
        }

        override fun putString(key: String?, value: String?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String?): SharedPreferences.Editor { key?.let { removals.add(it) }; return this }
        override fun clear(): SharedPreferences.Editor { clear = true; return this }

        override fun commit(): Boolean { applyPending(); return true }
        override fun apply() { applyPending() }

        private fun applyPending() {
            if (clear) store.clear()
            removals.forEach { store.remove(it) }
            store.putAll(pending)
        }
    }

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) = Unit
}
