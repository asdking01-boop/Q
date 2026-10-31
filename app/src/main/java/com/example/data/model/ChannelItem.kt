package com.example.data.model

data class ChannelItem(
    val id: String,
    val streamId: String,
    val name: String,
    val iconUrl: String? = null,
    val categoryId: String = "all",
    val categoryName: String = "عام",
    val streamUrl: String,
    val streamType: String = "LIVE",
    val isFavorite: Boolean = false,
    val isRecent: Boolean = false
) {
    val qualityBadge: String
        get() {
            val upper = name.uppercase()
            return when {
                upper.contains("4K") || upper.contains("UHD") -> "4K"
                upper.contains("FHD") || upper.contains("1080") -> "FHD"
                upper.contains("HD") || upper.contains("720") -> "HD"
                upper.contains("SD") -> "SD"
                upper.contains("HEVC") || upper.contains("H.265") -> "HEVC"
                else -> "HD"
            }
        }
}
