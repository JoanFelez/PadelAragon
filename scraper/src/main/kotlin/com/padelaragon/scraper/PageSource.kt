package com.padelaragon.scraper

import com.padelaragon.shared.data.network.HtmlFetcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/** HTML access used by the scraper; abstracted so tests never touch the network. */
interface PageSource {
    suspend fun get(url: String): String
    suspend fun post(url: String, params: Map<String, String>): String

    /** Returns the body, or null when the page does not answer 200. */
    suspend fun getOrNull(url: String): String?
}

/** Polite wrapper: at most [concurrency] requests in flight, each followed by a short pause. */
class FetcherPageSource(
    private val fetcher: HtmlFetcher = HtmlFetcher(),
    concurrency: Int = 3,
    private val delayMs: Long = 150
) : PageSource {
    private val permits = Semaphore(concurrency)

    override suspend fun get(url: String): String = paced { fetcher.get(url) }

    override suspend fun post(url: String, params: Map<String, String>): String = paced { fetcher.post(url, params) }

    override suspend fun getOrNull(url: String): String? = paced {
        val response = fetcher.getWithStatus(url)
        if (response.statusCode == 200 && response.body.isNotBlank()) response.body else null
    }

    private suspend fun <T> paced(block: suspend () -> T): T = permits.withPermit {
        try {
            block()
        } finally {
            delay(delayMs)
        }
    }
}
