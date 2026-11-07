package com.padelaragon.shared.ui.navigation

import kotlinx.serialization.Serializable

/** Typed navigation routes. Top-level tab roots first, then pushed detail destinations. */
@Serializable
data object LeaguesRoute

@Serializable
data object FavoritesRoute

@Serializable
data object ChatRoute

@Serializable
data object SettingsRoute

/** Categories of [leagueId]; [groupId] pre-selects a group and [fromFavorites] keeps the Favoritos tab highlighted. */
@Serializable
data class GroupsRoute(val leagueId: Int, val groupId: Int? = null, val fromFavorites: Boolean = false)

@Serializable
data class TeamRoute(val leagueId: Int, val teamId: Int, val teamName: String, val groupId: Int)

/** The heavy match data of a player screen is handed over through [PlayerArgsStore] under [argsKey]. */
@Serializable
data class PlayerRoute(val leagueId: Int, val teamId: Int, val playerName: String, val argsKey: String)
