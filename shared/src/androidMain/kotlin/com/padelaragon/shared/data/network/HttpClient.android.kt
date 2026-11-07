package com.padelaragon.shared.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool
import java.util.concurrent.TimeUnit

actual fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        config()
        engine {
            config {
                connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, 2, TimeUnit.MINUTES))
            }
        }
    }

private const val MAX_IDLE_CONNECTIONS = 4
