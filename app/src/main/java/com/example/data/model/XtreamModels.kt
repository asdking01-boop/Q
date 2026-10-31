package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @Json(name = "user_info") val userInfo: XtreamUserInfo? = null,
    @Json(name = "server_info") val serverInfo: XtreamServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @Json(name = "username") val username: String? = null,
    @Json(name = "password") val password: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "auth") val auth: Int? = 0,
    @Json(name = "status") val status: String? = null,
    @Json(name = "exp_date") val expDate: String? = null,
    @Json(name = "is_trial") val isTrial: String? = null,
    @Json(name = "active_cons") val activeCons: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "max_connections") val maxConnections: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @Json(name = "url") val url: String? = null,
    @Json(name = "port") val port: String? = null,
    @Json(name = "https_port") val httpsPort: String? = null,
    @Json(name = "server_protocol") val serverProtocol: String? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "timestamp_now") val timestampNow: Long? = null,
    @Json(name = "time_now") val timeNow: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id") val categoryId: String = "",
    @Json(name = "category_name") val categoryName: String = "",
    @Json(name = "parent_id") val parentId: Int? = 0
)

@JsonClass(generateAdapter = true)
data class XtreamLiveStream(
    @Json(name = "num") val num: Int? = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "stream_type") val streamType: String? = "live",
    @Json(name = "stream_id") val streamId: Any? = null, // Can be Int or String in some servers
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "epg_channel_id") val epgChannelId: String? = null,
    @Json(name = "added") val added: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "custom_sid") val customSid: String? = null,
    @Json(name = "tv_archive") val tvArchive: Int? = 0,
    @Json(name = "direct_source") val directSource: String? = null
) {
    fun getSafeStreamId(): String {
        return when (streamId) {
            is Number -> streamId.toInt().toString()
            is String -> streamId
            else -> ""
        }
    }
}
