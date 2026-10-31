package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "iptv_accounts")
data class IptvAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileName: String,
    val serverUrl: String,
    val username: String = "",
    val password: String = "",
    val isM3u: Boolean = false,
    val m3uUrl: String = "",
    val isActive: Boolean = true,
    val accountStatus: String = "نشط",
    val expiryDate: String = "",
    val maxConnections: String = "1",
    val activeConnections: String = "0",
    val streamFormat: String = "m3u8", // m3u8 or ts
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getRemainingDays(): Long? {
        if (expiryDate.isBlank() || expiryDate == "غير محدد" || expiryDate == "غير محدود") return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(expiryDate) ?: return null
            val diff = date.time - System.currentTimeMillis()
            diff / (1000L * 60 * 60 * 24)
        } catch (_: Exception) {
            null
        }
    }

    fun isExpiringSoon(): Boolean {
        val days = getRemainingDays() ?: return false
        return days in 0..7
    }

    fun isExpired(): Boolean {
        val days = getRemainingDays() ?: return false
        return days < 0
    }

    fun getExpiryWarningText(): String? {
        val days = getRemainingDays() ?: return null
        return when {
            days < 0 -> "انتهت صلاحية هذا الاشتراك! يرجى التجديد مع مزود الخدمة."
            days == 0L -> "ينتهي اشتراكك اليوم! يرجى التجديد لتجنب انقطاع البث."
            days <= 7L -> "تنبيه: متبقي $days أيام فقط على انتهاء اشتراكك."
            else -> null
        }
    }
}

@Entity(tableName = "favorite_channels")
data class FavoriteChannelEntity(
    @PrimaryKey
    val id: String, // "${accountId}_${streamId}"
    val accountId: Long,
    val streamId: String,
    val name: String,
    val iconUrl: String? = null,
    val categoryId: String = "all",
    val categoryName: String = "عام",
    val streamUrl: String,
    val streamType: String = "LIVE",
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_channels")
data class RecentChannelEntity(
    @PrimaryKey
    val id: String, // "${accountId}_${streamId}"
    val accountId: Long,
    val streamId: String,
    val name: String,
    val iconUrl: String? = null,
    val categoryId: String = "all",
    val categoryName: String = "عام",
    val streamUrl: String,
    val streamType: String = "LIVE",
    val watchedAt: Long = System.currentTimeMillis()
)
