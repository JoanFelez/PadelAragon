package com.padelaragon.shared.data.model

/**
 * Wins/losses and pareja (1/2/3) counts scoped to either home or away matches only.
 */
data class PlayerStatsSplit(
    val wins: Int = 0,
    val losses: Int = 0,
    val pair1Count: Int = 0,
    val pair2Count: Int = 0,
    val pair3Count: Int = 0
)

data class PlayerStats(
    val name: String,
    val home: PlayerStatsSplit,
    val away: PlayerStatsSplit
) {
    val wins: Int get() = home.wins + away.wins
    val losses: Int get() = home.losses + away.losses
    val pair1Count: Int get() = home.pair1Count + away.pair1Count
    val pair2Count: Int get() = home.pair2Count + away.pair2Count
    val pair3Count: Int get() = home.pair3Count + away.pair3Count
}
