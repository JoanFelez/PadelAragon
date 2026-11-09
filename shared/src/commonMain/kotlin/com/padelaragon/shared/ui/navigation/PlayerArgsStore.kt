package com.padelaragon.shared.ui.navigation

import androidx.lifecycle.ViewModel
import com.padelaragon.shared.data.model.MatchDetail
import com.padelaragon.shared.data.model.MatchResult

/**
 * Keeps the already-loaded match data of the team screen so the player screen needs no extra
 * requests. Lives in a ViewModel so it survives configuration changes.
 */
class PlayerArgsStore : ViewModel() {
    class Args(val matchDetails: Map<String, MatchDetail>, val playedMatches: List<MatchResult>)

    private val entries = mutableMapOf<String, Args>()

    fun put(key: String, args: Args) {
        entries[key] = args
    }

    fun get(key: String): Args? = entries[key]
}
