package com.padelaragon.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.padelaragon.shared.data.local.entity.CacheTimestamp

@Dao
interface CacheTimestampDao {
    @Query("SELECT timestamp FROM cache_timestamps WHERE cacheKey = :key")
    suspend fun getTimestamp(key: String): Long?

    @Query("SELECT * FROM cache_timestamps WHERE cacheKey = :key")
    suspend fun getEntry(key: String): CacheTimestamp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(entry: CacheTimestamp)

    @Query("DELETE FROM cache_timestamps WHERE cacheKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM cache_timestamps")
    suspend fun deleteAll()
}
