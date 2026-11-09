package com.padelaragon.shared.data.repository.datasource

import com.padelaragon.shared.data.model.MatchDetail
import com.padelaragon.shared.data.model.MatchResult

interface MatchDetailDataSource {
    suspend fun getMatchDetail(detailUrl: String): MatchDetail?
    suspend fun prefetchMatchDetails(results: List<MatchResult>)
}
