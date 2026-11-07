package com.padelaragon.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import com.padelaragon.shared.data.local.entity.MatchDetailPairEntity

@Dao
interface MatchDetailDao {
    @Query("SELECT * FROM match_detail_pairs WHERE detailUrl = :detailUrl ORDER BY pairNumber")
    suspend fun getByDetailUrl(detailUrl: String): List<MatchDetailPairEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pairs: List<MatchDetailPairEntity>)

    @Query("DELETE FROM match_detail_pairs WHERE detailUrl = :detailUrl")
    suspend fun deleteByDetailUrl(detailUrl: String)

    @Transaction
    suspend fun replaceForUrl(detailUrl: String, pairs: List<MatchDetailPairEntity>) {
        deleteByDetailUrl(detailUrl)
        insertAll(pairs)
    }
}
