package net.nicochristmann.revivetendo.admin.net

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * Thin typed wrapper around relay-admin's /admin/api/v1/... JSON API
 * (net.nicochristmann.revivetendo.admin.net.ApiClient does the actual HTTP
 * work over mTLS). One function per endpoint, matching the Go handlers in
 * relay-admin/main.go's "JSON admin API" section field-for-field.
 */
object AdminApi {
    private fun <T> decodeList(body: String, serializer: KSerializer<T>): List<T> {
        val obj = lenientJson.parseToJsonElement(body).jsonObject
        if (obj["ok"]?.jsonPrimitive?.boolean != true) {
            throw ApiException(obj["error"]?.jsonPrimitive?.content ?: "request failed")
        }
        val data = obj["data"]
        // Go's encoding/json marshals a nil slice as JSON null, not [] - an
        // empty list (no bans yet, empty review queue, etc.) hits this even
        // though the server-side handler is now fixed to always emit [].
        // Keep this client-side guard too so a future endpoint can't
        // reintroduce the same crash.
        if (data == null || data is JsonNull) return emptyList()
        return lenientJson.decodeFromJsonElement(ListSerializer(serializer), data)
    }

    private fun postForResult(path: String, body: JsonObject) {
        val obj = lenientJson.parseToJsonElement(ApiClient.post(path, body.toString())).jsonObject
        if (obj["ok"]?.jsonPrimitive?.boolean != true) {
            throw ApiException(obj["error"]?.jsonPrimitive?.content ?: "request failed")
        }
    }

    // --- Redirects ---
    fun redirects(): List<Redirect> = decodeList(ApiClient.get("/redirects"), Redirect.serializer())

    fun addRedirect(type: String, address: String?, gameServerId: String?, port: Int?, fromHost: String, toHost: String): List<Redirect> {
        val body = buildJsonObject {
            put("type", type)
            put("address", address.orEmpty())
            put("game_server_id", gameServerId.orEmpty())
            put("port", port ?: 0)
            put("from_host", fromHost)
            put("to_host", toHost)
        }
        return decodeList(ApiClient.post("/redirects/add", body.toString()), Redirect.serializer())
    }

    fun deleteRedirect(id: Int) = postForResult("/redirects/delete", buildJsonObject { put("id", id) })
    fun toggleRedirect(id: Int) = postForResult("/redirects/toggle", buildJsonObject { put("id", id) })

    // --- Per-game user whitelist ---
    fun users(game: String): List<UserAccessEntry> = decodeList(ApiClient.get("/users?game=$game"), UserAccessEntry.serializer())

    fun addUser(game: String, pid: Long, note: String?): List<UserAccessEntry> {
        val body = buildJsonObject { put("game", game); put("pid", pid); put("note", note.orEmpty()) }
        return decodeList(ApiClient.post("/users/add", body.toString()), UserAccessEntry.serializer())
    }

    fun deleteUser(game: String, pid: Long) =
        postForResult("/users/delete", buildJsonObject { put("game", game); put("pid", pid) })

    // --- Bans ---
    fun bans(): List<BannedUser> = decodeList(ApiClient.get("/bans"), BannedUser.serializer())

    fun addBan(pid: Long, reason: String?): List<BannedUser> {
        val body = buildJsonObject { put("pid", pid); put("reason", reason.orEmpty()) }
        return decodeList(ApiClient.post("/bans/add", body.toString()), BannedUser.serializer())
    }

    fun removeBan(pid: Long) = postForResult("/bans/remove", buildJsonObject { put("pid", pid) })

    // --- Access levels ---
    fun access(): List<AccessLevelEntry> = decodeList(ApiClient.get("/access"), AccessLevelEntry.serializer())

    fun setAccess(pid: Long, level: Int, note: String?): List<AccessLevelEntry> {
        val body = buildJsonObject { put("pid", pid); put("level", level); put("note", note.orEmpty()) }
        return decodeList(ApiClient.post("/access/set", body.toString()), AccessLevelEntry.serializer())
    }

    fun removeAccess(pid: Long) = postForResult("/access/remove", buildJsonObject { put("pid", pid) })

    // --- SpotPass system messages ---
    fun spotpassWiiU(): List<SystemMessage> = decodeList(ApiClient.get("/spotpass-wiiu"), SystemMessage.serializer())

    fun addSpotpassWiiU(subject: String, body: String, region: String?): List<SystemMessage> {
        val json = buildJsonObject { put("subject", subject); put("body", body); put("region", region.orEmpty()) }
        return decodeList(ApiClient.post("/spotpass-wiiu/add", json.toString()), SystemMessage.serializer())
    }

    fun toggleSpotpassWiiU(id: Int) = postForResult("/spotpass-wiiu/toggle", buildJsonObject { put("id", id) })
    fun removeSpotpassWiiU(id: Int) = postForResult("/spotpass-wiiu/remove", buildJsonObject { put("id", id) })

    fun spotpass3DSSysMsg(): List<SystemMessage> = decodeList(ApiClient.get("/spotpass-3ds-sysmsg"), SystemMessage.serializer())

    fun addSpotpass3DSSysMsg(subject: String, body: String, region: String?): List<SystemMessage> {
        val json = buildJsonObject { put("subject", subject); put("body", body); put("region", region.orEmpty()) }
        return decodeList(ApiClient.post("/spotpass-3ds-sysmsg/add", json.toString()), SystemMessage.serializer())
    }

    fun toggleSpotpass3DSSysMsg(id: Int) = postForResult("/spotpass-3ds-sysmsg/toggle", buildJsonObject { put("id", id) })
    fun removeSpotpass3DSSysMsg(id: Int) = postForResult("/spotpass-3ds-sysmsg/remove", buildJsonObject { put("id", id) })

    // --- SpotPass 3DS notes (read-only, matches adminSwapdoodle) ---
    fun spotpass3DSNotes(): List<SwapdoodleNote> = decodeList(ApiClient.get("/spotpass-3ds"), SwapdoodleNote.serializer())

    // --- Review queue ---
    fun review(): List<ReviewEntry> = decodeList(ApiClient.get("/review"), ReviewEntry.serializer())

    fun approveReview(pid: Long, game: String, note: String?): List<ReviewEntry> {
        val json = buildJsonObject { put("pid", pid); put("game", game); put("note", note.orEmpty()) }
        return decodeList(ApiClient.post("/review/approve", json.toString()), ReviewEntry.serializer())
    }

    fun dismissReview(pid: Long, game: String) =
        postForResult("/review/dismiss", buildJsonObject { put("pid", pid); put("game", game) })
}
