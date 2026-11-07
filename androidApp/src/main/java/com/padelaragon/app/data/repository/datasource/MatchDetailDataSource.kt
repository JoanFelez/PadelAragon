package com.padelaragon.app.data.repository.datasource

import com.padelaragon.app.data.model.MatchDetail
import com.padelaragon.app.data.model.MatchResult

interface MatchDetailDataSource {
    suspend fun getMatchDetail(detailUrl: String): MatchDetail?
    suspend fun prefetchMatchDetails(results: List<MatchResult>)
}
