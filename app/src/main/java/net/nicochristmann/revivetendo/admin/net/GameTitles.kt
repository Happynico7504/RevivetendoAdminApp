package net.nicochristmann.revivetendo.admin.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * In-memory cache of relay-admin's gameServerTitles map, fetched from
 * /admin/api/v1/game-titles instead of being duplicated as a hardcoded
 * table client-side, so a game added server-side shows up here without an
 * app update. Session-lived only - no need to persist it.
 */
object GameTitles {
    @Volatile
    var cache: Map<String, String> = emptyMap()
        private set

    suspend fun ensureLoaded() {
        if (cache.isNotEmpty()) return
        refresh()
    }

    suspend fun refresh() {
        cache = withContext(Dispatchers.IO) { fetch() }
    }

    private fun fetch(): Map<String, String> {
        val obj = lenientJson.parseToJsonElement(ApiClient.get("/game-titles")).jsonObject
        if (obj["ok"]?.jsonPrimitive?.boolean != true) {
            throw ApiException(obj["error"]?.jsonPrimitive?.content ?: "request failed")
        }
        val data = obj["data"]
        if (data == null || data is JsonNull) return emptyMap()
        return data.jsonObject.mapValues { it.value.jsonPrimitive.content }
    }
}
