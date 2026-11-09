package com.padelaragon.shared.data.network

import com.padelaragon.shared.util.Logger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

actual fun createPlatformHttpClient(config: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(OkHttp) {
        config()
        engine {
            config {
                connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, 2, TimeUnit.MINUTES))
                // Validation stays on: we only widen the set of trusted roots (see below).
                buildAugmentedSslSocketFactory()?.let { (factory, trustManager) ->
                    sslSocketFactory(factory, trustManager)
                }
            }
        }
    }

private const val MAX_IDLE_CONNECTIONS = 4
private const val TAG = "HttpClient"
private const val BUNDLED_CERTS_RESOURCE_DIR = "/certs/"
private val BUNDLED_CERT_FILES = listOf("itx-root-ca.pem", "itx-ssl-proxy-intermediate.pem")

/**
 * Some networks (corporate proxies doing TLS inspection) re-sign padelfederacion.es with a private
 * root CA trusted by the OS but not by the JVM's cacerts. The trust manager built here accepts the
 * JVM default roots, extra roots bundled as resources, and on Windows the OS root store.
 */
private fun buildAugmentedSslSocketFactory(): Pair<SSLSocketFactory, X509TrustManager>? = runCatching {
    fun trustManagersFor(keyStore: KeyStore?): List<X509TrustManager> {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(keyStore)
        return factory.trustManagers.filterIsInstance<X509TrustManager>()
    }

    val bundledCerts = loadBundledCertificates()
    val bundledManagers = if (bundledCerts.isNotEmpty()) {
        val store = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            bundledCerts.forEachIndexed { index, cert -> setCertificateEntry("bundled-$index", cert) }
        }
        trustManagersFor(store)
    } else {
        emptyList()
    }

    val isWindows = System.getProperty("os.name")?.contains("windows", ignoreCase = true) == true
    val windowsManagers = if (isWindows) {
        runCatching {
            trustManagersFor(KeyStore.getInstance("Windows-ROOT").apply { load(null, null) })
        }.getOrElse { emptyList() }
    } else {
        emptyList()
    }

    val combined = CompositeX509TrustManager(trustManagersFor(null) + bundledManagers + windowsManagers)
    val sslContext = SSLContext.getInstance("TLS").apply { init(null, arrayOf(combined), null) }
    sslContext.socketFactory to combined
}.onFailure { e ->
    Logger.e(TAG, "Failed to build augmented trust manager, using JVM default", e)
}.getOrNull()

private fun loadBundledCertificates(): List<X509Certificate> {
    val certFactory = CertificateFactory.getInstance("X.509")
    return BUNDLED_CERT_FILES.mapNotNull { fileName ->
        runCatching {
            object {}.javaClass.getResourceAsStream("$BUNDLED_CERTS_RESOURCE_DIR$fileName")
                ?.use { certFactory.generateCertificate(it) as X509Certificate }
        }.onFailure { e ->
            Logger.e(TAG, "Failed to load bundled certificate $fileName", e)
        }.getOrNull()
    }
}

/** Trusts a chain if ANY delegate trusts it. */
private class CompositeX509TrustManager(
    private val delegates: List<X509TrustManager>
) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
        checkAny("client") { it.checkClientTrusted(chain, authType) }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) =
        checkAny("server") { it.checkServerTrusted(chain, authType) }

    override fun getAcceptedIssuers(): Array<X509Certificate> =
        delegates.flatMap { it.acceptedIssuers.toList() }.toTypedArray()

    private inline fun checkAny(kind: String, check: (X509TrustManager) -> Unit) {
        var last: Exception? = null
        for (delegate in delegates) {
            try {
                check(delegate)
                return
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: SecurityException("No trust manager trusted this $kind certificate")
    }
}
