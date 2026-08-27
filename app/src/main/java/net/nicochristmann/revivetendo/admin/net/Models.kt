package net.nicochristmann.revivetendo.admin.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Redirect(
    val id: Int,
    val type: String,
    val address: String? = null,
    val from_host: String,
    val to_host: String,
    val game_server_id: String? = null,
    val port: Int? = null,
    val access_mode: String,
    val enabled: Boolean,
    val created_at: String,
)

@Serializable
data class UserAccessEntry(
    val pid: Long,
    val pnid: String? = null,
    val game_server_id: String,
    val note: String? = null,
    val created_at: String,
)

@Serializable
data class BannedUser(
    val pid: Long,
    val pnid: String? = null,
    val reason: String? = null,
    val created_at: String,
)

@Serializable
data class AccessLevelEntry(
    val pid: Long,
    val pnid: String? = null,
    val access_level: Int,
    val note: String? = null,
    val updated_at: String,
)

@Serializable
data class SystemMessage(
    val id: Int,
    val subject: String,
    val body: String,
    val title_id: String,
    val high_priority: Boolean,
    val active: Boolean,
    val region: String? = null,
    val created_at: String,
)

@Serializable
data class SwapdoodleNote(
    val data_id: Long,
    val owner_pid: Long,
    val owner_pnid: String? = null,
    val recipient_pid: Long? = null,
    val recipient_pnid: String? = null,
    val size: Long,
    val upload_completed: Boolean,
    val read: Boolean,
    val created_at: String,
)

@Serializable
data class ReviewEntry(
    val pid: Long,
    val pnid: String? = null,
    @SerialName("game_server_id") val gameServerId: String,
    val first_seen: String,
    val last_seen: String,
    val attempts: Int,
)
