package com.padelaragon.shared.data.favorites

import com.padelaragon.shared.data.local.PlatformContext
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun createFavoritesSettings(context: PlatformContext): Settings =
    PreferencesSettings(Preferences.userRoot().node("com/padelaragon/desktop/favorites"))
