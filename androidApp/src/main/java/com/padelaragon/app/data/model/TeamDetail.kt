package com.padelaragon.app.data.model

data class Player(
    val name: String,
    val isCaptain: Boolean = false,
    val points: String? = null,
    val birthYear: String? = null
)

data class TeamDetail(
    val category: String? = null,
    val captainName: String? = null,
    val players: List<Player> = emptyList()
) {
    val captain: Player?
        get() = players.firstOrNull { it.isCaptain }
            ?: captainName?.let { name ->
                players.firstOrNull {
                    name.contains(it.name, ignoreCase = true) || it.name.contains(name, ignoreCase = true)
                }
            }
}
