package com.padelaragon.shared.data.favorites

import android.content.Context
import com.padelaragon.shared.data.local.PlatformContext
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private const val PREFS_NAME = "favorites"
private const val LEGACY_KEY = "favorite_group_ids"

actual fun createFavoritesSettings(context: PlatformContext): Settings {
    val prefs = context.context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val settings = SharedPreferencesSettings(prefs)
    val legacy = prefs.getStringSet(LEGACY_KEY, null)
    if (legacy != null) {
        FavoritesStore.migrateLegacy(settings, legacy.mapNotNull { it.toIntOrNull() }.toSet())
        prefs.edit().remove(LEGACY_KEY).apply()
    }
    return settings
}
