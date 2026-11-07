package com.padelaragon.shared.domain.notification

import com.russhwolf.settings.Settings

class NotificationSettings(private val settings: Settings) {
    var enabled: Boolean
        get() = settings.getBoolean(KEY_ENABLED, false)
        set(value) = settings.putBoolean(KEY_ENABLED, value)

    var intervalHours: Int
        get() = settings.getInt(KEY_INTERVAL, DEFAULT_INTERVAL_HOURS).takeIf { it in INTERVAL_OPTIONS }
            ?: DEFAULT_INTERVAL_HOURS
        set(value) = settings.putInt(KEY_INTERVAL, value)

    /** Last time favourite groups were scraped directly (no backend), to cap that at [SCRAPE_MIN_INTERVAL_MS]. */
    var lastScrapeAtMs: Long
        get() = settings.getLong(KEY_LAST_SCRAPE, 0L)
        set(value) = settings.putLong(KEY_LAST_SCRAPE, value)

    var lastCheckAtMs: Long
        get() = settings.getLong(KEY_LAST_CHECK, 0L)
        set(value) = settings.putLong(KEY_LAST_CHECK, value)

    companion object {
        val INTERVAL_OPTIONS = listOf(1, 3, 6, 12)
        const val DEFAULT_INTERVAL_HOURS = 1
        const val SCRAPE_MIN_INTERVAL_MS = 3 * 60 * 60 * 1000L
        private const val KEY_ENABLED = "notif_enabled"
        private const val KEY_INTERVAL = "notif_interval_hours"
        private const val KEY_LAST_SCRAPE = "notif_last_scrape"
        private const val KEY_LAST_CHECK = "notif_last_check"
    }
}
