package com.example.data.model

import android.util.Base64
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class XtreamEpgResponse(
    @Json(name = "epg_listings") val epgListings: List<XtreamEpgListing>? = null
)

@JsonClass(generateAdapter = true)
data class XtreamEpgListing(
    @Json(name = "id") val id: String? = null,
    @Json(name = "epg_id") val epgId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "lang") val lang: String? = null,
    @Json(name = "start") val start: String? = null,
    @Json(name = "end") val end: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "channel_id") val channelId: String? = null,
    @Json(name = "start_timestamp") val startTimestamp: Long? = null,
    @Json(name = "stop_timestamp") val stopTimestamp: Long? = null
) {
    fun getDecodedTitle(): String {
        val raw = title ?: return "برنامج مباشر"
        return try {
            val decoded = String(Base64.decode(raw, Base64.DEFAULT))
            if (decoded.isNotBlank()) decoded else raw
        } catch (_: Exception) {
            raw
        }
    }

    fun getDecodedDescription(): String {
        val raw = description ?: return ""
        return try {
            val decoded = String(Base64.decode(raw, Base64.DEFAULT))
            if (decoded.isNotBlank()) decoded else raw
        } catch (_: Exception) {
            raw
        }
    }
}

data class EpgProgram(
    val title: String,
    val description: String,
    val startTime: String,
    val endTime: String,
    val startTimestamp: Long,
    val endTimestamp: Long
) {
    val progress: Float
        get() {
            val now = System.currentTimeMillis() / 1000L
            if (now < startTimestamp) return 0f
            if (now >= endTimestamp) return 1f
            val total = (endTimestamp - startTimestamp).toFloat()
            if (total <= 0) return 0f
            return ((now - startTimestamp) / total).coerceIn(0f, 1f)
        }

    val timeSpan: String
        get() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val s = if (startTimestamp > 0) sdf.format(Date(startTimestamp * 1000L)) else startTime
            val e = if (endTimestamp > 0) sdf.format(Date(endTimestamp * 1000L)) else endTime
            return "$s - $e"
        }
}
