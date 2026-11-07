package com.padelaragon.shared.data.chat

import com.russhwolf.settings.Settings

/** League the assistant answers about by default; null means "no league context". */
class ChatPreferences(private val settings: Settings) {
    var leagueId: Int?
        get() = settings.getIntOrNull(KEY_LEAGUE)
        set(value) = if (value == null) settings.remove(KEY_LEAGUE) else settings.putInt(KEY_LEAGUE, value)

    private companion object {
        const val KEY_LEAGUE = "chat_league_id"
    }
}
