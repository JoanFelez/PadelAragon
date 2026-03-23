package com.padelaragon.app.data.repository

import com.padelaragon.app.data.local.AppDatabase
import com.padelaragon.app.data.local.entity.CacheTimestamp
import com.padelaragon.app.data.network.HtmlFetcher
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Shared infrastructure used by all domain repositories.
 * Provides HTML fetching (with concurrency limiting), cache TTL checking, and DB access.
 */
class ScrapingService(
    val db: AppDatabase,
    val fetcher: HtmlFetcher = HtmlFetcher()
) {
    val scrapeSemaphore = Semaphore(15)

    suspend fun <T> withSemaphore(block: suspend () -> T): T =
        scrapeSemaphore.withPermit { block() }

    suspend fun isCacheValid(key: String, ttl: Long): Boolean {
        val timestamp = db.cacheTimestampDao().getTimestamp(key) ?: return false
        return (System.currentTimeMillis() - timestamp) < ttl
    }

    suspend fun updateCacheTimestamp(key: String) {
        db.cacheTimestampDao().set(CacheTimestamp(key, System.currentTimeMillis()))
    }

    companion object {
        const val BASE_URL = "https://padelfederacion.es/pAGINAS/ARAPADEL/"
        const val LEAGUE_ID = 27951
    }
}
