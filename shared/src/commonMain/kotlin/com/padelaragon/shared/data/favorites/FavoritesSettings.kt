package com.padelaragon.shared.data.favorites

import com.padelaragon.shared.data.local.PlatformContext
import com.russhwolf.settings.Settings

/**
 * Key-value storage for favourites. Android: SharedPreferences "favorites" (after migrating the
 * legacy set). Desktop: java.util.prefs node `com/padelaragon/desktop/favorites`, unchanged.
 */
expect fun createFavoritesSettings(context: PlatformContext): Settings
