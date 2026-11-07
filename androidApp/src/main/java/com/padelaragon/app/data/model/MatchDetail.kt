package com.padelaragon.app.data.model

data class MatchDetail(
    val pairs: List<PairDetail>
)

data class PairDetail(
    val pairNumber: Int,
    val localPlayer1: String,
    val localPlayer2: String,
    val visitorPlayer1: String,
    val visitorPlayer2: String,
    val sets: List<SetScore>
)

data class SetScore(
    val localScore: Int,
    val visitorScore: Int
)
