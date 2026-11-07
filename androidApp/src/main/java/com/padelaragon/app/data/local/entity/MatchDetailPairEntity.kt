package com.padelaragon.app.data.local.entity

import androidx.room.Entity
import com.padelaragon.app.data.model.PairDetail
import com.padelaragon.app.data.model.SetScore

@Entity(tableName = "match_detail_pairs", primaryKeys = ["detailUrl", "pairNumber"])
data class MatchDetailPairEntity(
    val detailUrl: String,
    val pairNumber: Int,
    val localPlayer1: String,
    val localPlayer2: String,
    val visitorPlayer1: String,
    val visitorPlayer2: String,
    val set1Local: Int?,
    val set1Visitor: Int?,
    val set2Local: Int?,
    val set2Visitor: Int?,
    val set3Local: Int? = null,
    val set3Visitor: Int? = null
) {
    fun toModel(): PairDetail = PairDetail(
        pairNumber = pairNumber,
        localPlayer1 = localPlayer1,
        localPlayer2 = localPlayer2,
        visitorPlayer1 = visitorPlayer1,
        visitorPlayer2 = visitorPlayer2,
        sets = buildList {
            if (set1Local != null && set1Visitor != null) add(SetScore(set1Local, set1Visitor))
            if (set2Local != null && set2Visitor != null) add(SetScore(set2Local, set2Visitor))
            if (set3Local != null && set3Visitor != null) add(SetScore(set3Local, set3Visitor))
        }
    )

    companion object {
        fun fromModel(detailUrl: String, model: PairDetail): MatchDetailPairEntity = MatchDetailPairEntity(
            detailUrl = detailUrl,
            pairNumber = model.pairNumber,
            localPlayer1 = model.localPlayer1,
            localPlayer2 = model.localPlayer2,
            visitorPlayer1 = model.visitorPlayer1,
            visitorPlayer2 = model.visitorPlayer2,
            set1Local = model.sets.getOrNull(0)?.localScore,
            set1Visitor = model.sets.getOrNull(0)?.visitorScore,
            set2Local = model.sets.getOrNull(1)?.localScore,
            set2Visitor = model.sets.getOrNull(1)?.visitorScore,
            set3Local = model.sets.getOrNull(2)?.localScore,
            set3Visitor = model.sets.getOrNull(2)?.visitorScore
        )
    }
}
