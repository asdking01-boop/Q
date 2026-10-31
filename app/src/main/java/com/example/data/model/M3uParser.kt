package com.example.data.model

object M3uParser {
    fun parse(content: String): List<ChannelItem> {
        val channels = mutableListOf<ChannelItem>()
        val lines = content.lines()
        var currentName = ""
        var currentLogo: String? = null
        var currentGroup = "عام"
        var currentId = ""

        val logoRegex = Regex("""tvg-logo="([^"]+)"""")
        val groupRegex = Regex("""group-title="([^"]+)"""")
        val nameRegex = Regex("""tvg-name="([^"]+)"""")
        val idRegex = Regex("""tvg-id="([^"]+)"""")

        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                logoRegex.find(line)?.let { currentLogo = it.groupValues[1] }
                groupRegex.find(line)?.let { currentGroup = it.groupValues[1] }
                idRegex.find(line)?.let { currentId = it.groupValues[1] }

                val commaIndex = line.lastIndexOf(',')
                currentName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    nameRegex.find(line)?.groupValues?.get(1) ?: "قناة"
                }
            } else if (line.isNotEmpty() && !line.startsWith("#")) {
                val streamUrl = line
                val cleanName = currentName.ifEmpty { "قناة ${channels.size + 1}" }
                val channelId = currentId.ifEmpty { "m3u_${channels.size}_${cleanName.hashCode()}" }
                
                channels.add(
                    ChannelItem(
                        id = channelId,
                        streamId = channelId,
                        name = cleanName,
                        iconUrl = currentLogo,
                        categoryId = currentGroup,
                        categoryName = currentGroup,
                        streamUrl = streamUrl,
                        streamType = "LIVE"
                    )
                )

                // Reset temporary fields
                currentName = ""
                currentLogo = null
                currentGroup = "عام"
                currentId = ""
            }
        }
        return channels
    }
}
