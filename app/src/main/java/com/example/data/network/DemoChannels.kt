package com.example.data.network

import com.example.data.model.ChannelItem

object DemoChannels {
    val SAMPLE_CATEGORIES = listOf(
        "الكل",
        "بث مباشر تجريبي",
        "أخبار ومعلومات",
        "أفلام وثائقية وفضاء",
        "اختبار 4K & FHD"
    )

    val CHANNELS = listOf(
        ChannelItem(
            id = "demo_bbb",
            streamId = "demo_bbb",
            name = "Cinema Broadcast Test FHD 1080p",
            iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Big_buck_bunny_poster_big.jpg/480px-Big_buck_bunny_poster_big.jpg",
            categoryId = "اختبار 4K & FHD",
            categoryName = "اختبار 4K & FHD",
            streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        ),
        ChannelItem(
            id = "demo_france24",
            streamId = "demo_france24",
            name = "France 24 Arabic فرانس 24 HD",
            iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/86/France_24_logo.svg/512px-France_24_logo.svg.png",
            categoryId = "أخبار ومعلومات",
            categoryName = "أخبار ومعلومات",
            streamUrl = "https://static.france24.com/live/F24_AR_HI_HLS/live_tv.m3u8"
        ),
        ChannelItem(
            id = "demo_dw",
            streamId = "demo_dw",
            name = "DW Arabic دويتشه فيله HD",
            iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Deutsche_Welle_logo.svg/512px-Deutsche_Welle_logo.svg.png",
            categoryId = "أخبار ومعلومات",
            categoryName = "أخبار ومعلومات",
            streamUrl = "https://dwamdstream104.akamaized.net/hls/live/2015530/dwstream104/index.m3u8"
        ),
        ChannelItem(
            id = "demo_apple_hls",
            streamId = "demo_apple_hls",
            name = "HLS Live Adaptive Stream 16:9 HD",
            iconUrl = "https://developer.apple.com/assets/elements/icons/http-live-streaming/http-live-streaming-96x96_2x.png",
            categoryId = "بث مباشر تجريبي",
            categoryName = "بث مباشر تجريبي",
            streamUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
        ),
        ChannelItem(
            id = "demo_tears",
            streamId = "demo_tears",
            name = "Cinema Ultra Demo 4K UHD",
            iconUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/46/Tears_of_Steel_poster.jpg/480px-Tears_of_Steel_poster.jpg",
            categoryId = "اختبار 4K & FHD",
            categoryName = "اختبار 4K & FHD",
            streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        ),
        ChannelItem(
            id = "demo_blazes",
            streamId = "demo_blazes",
            name = "Nature & Action Stream FHD",
            iconUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg",
            categoryId = "أفلام وثائقية وفضاء",
            categoryName = "أفلام وثائقية وفضاء",
            streamUrl = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        )
    )
}
