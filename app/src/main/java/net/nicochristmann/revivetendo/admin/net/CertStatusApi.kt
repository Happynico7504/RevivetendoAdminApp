package net.nicochristmann.revivetendo.admin.net

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@Serializable
data class CertStatusData(
    val issuedAt: String? = null,
    val expiresAt: String? = null,
    val daysUntilRotation: Int? = null,
    val rotationDays: Int? = null,
    val validDays: Int? = null,
)

@Serializable
private data class CertStatusEnvelope(
    val ok: Boolean,
    val data: CertStatusData? = null,
    val error: String? = null,
)

private val lenientJson = Json { ignoreUnknownKeys = true }

object CertStatusApi {
    fun fetch(): CertStatusData {
        val body = ApiClient.get("/cert-status")
        val envelope = lenientJson.decodeFromString<CertStatusEnvelope>(body)
        if (!envelope.ok || envelope.data == null) {
            throw ApiException(envelope.error ?: "Server returned no cert status data")
        }
        return envelope.data
    }

    fun ping(): Boolean {
        val body = ApiClient.get("/ping")
        return lenientJson.decodeFromString<CertStatusEnvelope>(body).ok
    }
}
