package net.nicochristmann.revivetendo.admin.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.nicochristmann.revivetendo.admin.cert.ClientCertStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

class ApiException(message: String, val httpCode: Int? = null) : Exception(message)

/**
 * Talks to relay-admin's /admin/api/v1/* JSON API over mTLS, presenting the
 * cert held in [ClientCertStore]. Real request path in production: this app
 * --mTLS--> the Apache2 box at BASE_HOST (which forwards the raw client cert
 * PEM via a header) --> nginx --> relay-admin, which does the actual crypto
 * verification of the forwarded cert. The client cert is only used for TLS
 * client auth here - relay-admin never sees this app's request body
 * differently because of it.
 */
object ApiClient {
    const val BASE_HOST = "https://revivetendo-dashboard.nicochristmann.net"

    // The Apache2 box (BASE_HOST) forwards everything under /inkay/ to nginx,
    // which strips that prefix before proxying to relay-admin - so relay-admin's
    // own /admin/... routes are reached externally as /inkay/admin/...
    // (see /etc/nginx/sites-available/revivetendo-dashboard.nicochristmann.net).
    const val ADMIN_BASE = "$BASE_HOST/inkay/admin"
    private const val API_BASE = "$ADMIN_BASE/api/v1"

    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private var cachedClient: OkHttpClient? = null

    /** Call after importing/replacing the cert so the next request picks it up. */
    fun invalidateClient() {
        cachedClient = null
    }

    private fun httpClient(): OkHttpClient {
        cachedClient?.let { return it }
        val (sslContext, trustManager) = ClientCertStore.buildSslContext()
        val built = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
        cachedClient = built
        return built
    }

    fun get(path: String): String =
        execute(Request.Builder().url("$API_BASE$path").get().build())

    fun post(path: String, bodyJson: String = "{}"): String {
        val body = bodyJson.toRequestBody(jsonMedia)
        val request = Request.Builder().url("$API_BASE$path").post(body).build()
        return execute(request)
    }

    /** For non-JSON endpoints like /admin/client-cert.p12 (binary download). */
    fun getRawResponse(url: String): Response {
        val request = Request.Builder().url(url).get().build()
        return httpClient().newCall(request).execute()
    }

    private fun execute(request: Request): String {
        httpClient().newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(extractError(bodyStr) ?: "HTTP ${response.code}", response.code)
            }
            return bodyStr
        }
    }

    private fun extractError(body: String): String? =
        try {
            json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            null
        }
}
