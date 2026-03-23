package com.padelaragon.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cache_timestamps")
data class CacheTimestamp(
    @PrimaryKey val cacheKey: String,
    val timestamp: Long
)
