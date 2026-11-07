package com.padelaragon.shared.data.repository

import com.padelaragon.shared.data.local.AppDatabase
import com.padelaragon.shared.data.local.entity.CacheTimestamp
import com.padelaragon.shared.data.network.HtmlFetcher
import com.padelaragon.shared.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

/**
 * Shared infrastructure used by all repositories: HTML fetching, cache freshness (TTL),
 * change detection by content hash and background revalidation (stale-while-revalidate).
 */
class ScrapingService(
    val db: AppDatabase,
    val fetcher: HtmlFetcher = HtmlFetcher(),
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    /** Lets the sync engine veto background scraping while a healthy backend feeds the cache. */
    private val backgroundScrapingAllowed: () -> Boolean = { true },
    private val nowMs: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    private val activeRefreshes = MutableStateFlow<Set<String>>(emptySet())

    suspend fun isCacheValid(key: String, ttl: Long): Boolean {
        val timestamp = db.cacheTimestampDao().getTimestamp(key) ?: return false
        return (nowMs() - timestamp) < ttl
    }

    /**
     * Runs [refresh] on [scope] unless a refresh for the same [key] is already running.
     * Failures are swallowed: the caller already has cached data.
     */
    fun revalidateInBackground(key: String, refresh: suspend () -> Unit) {
        if (!backgroundScrapingAllowed()) return
        if (activeRefreshes.getAndUpdate { it + key }.contains(key)) return
        scope.launch {
            try {
                refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w("ScrapingService", "Background refresh failed for $key: ${e.message}")
            } finally {
                activeRefreshes.update { it - key }
            }
        }
    }

    /**
     * Records a successful fetch for [key] and runs [write] only when [content] differs from the
     * previously stored version. Returns true when the database was written.
     */
    suspend fun writeIfChanged(key: String, content: Any, write: suspend () -> Unit): Boolean {
        val hash = contentHash(content)
        val previous = db.cacheTimestampDao().getEntry(key)
        val changed = previous?.contentHash != hash
        if (changed) write()
        db.cacheTimestampDao().set(CacheTimestamp(key, nowMs(), hash))
        return changed
    }

    companion object {
        const val BASE_URL = "https://padelfederacion.es/pAGINAS/ARAPADEL/"

        internal fun contentHash(content: Any): String {
            // FNV-1a 64-bit over the deterministic data-class rendering.
            var hash = -3750763034362895579L
            for (ch in content.toString()) {
                hash = (hash xor ch.code.toLong()) * 1099511628211L
            }
            return hash.toULong().toString(16)
        }
    }
}
