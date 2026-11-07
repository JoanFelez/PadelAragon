package com.padelaragon.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelaragon.app.data.local.entity.MatchDetailPairEntity

@Dao
interface MatchDetailDao {
    @Query("SELECT * FROM match_detail_pairs WHERE detailUrl = :detailUrl ORDER BY pairNumber")
    suspend fun getByDetailUrl(detailUrl: String): List<MatchDetailPairEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pairs: List<MatchDetailPairEntity>)

    @Query("DELETE FROM match_detail_pairs WHERE detailUrl = :detailUrl")
    suspend fun deleteByDetailUrl(detailUrl: String)
}
