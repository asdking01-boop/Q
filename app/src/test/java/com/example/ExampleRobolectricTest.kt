package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.IptvAccountEntity
import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgram
import com.example.data.model.M3uParser
import com.example.data.network.XtreamApiClient
import com.example.data.security.ParentalControlManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ASD IPTV", appName)
    }

    @Test
    fun `sanitize url helper test`() {
        val raw = "example.com:8080/ "
        val sanitized = XtreamApiClient.sanitizeUrl(raw)
        assertEquals("http://example.com:8080", sanitized)
    }

    @Test
    fun `m3u parser test`() {
        val sampleM3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="ch1" tvg-name="Sports 1 FHD" tvg-logo="http://logo.png" group-title="Sports",Sports 1 FHD
            http://stream.m3u8
        """.trimIndent()

        val parsed = M3uParser.parse(sampleM3u)
        assertEquals(1, parsed.size)
        assertEquals("Sports 1 FHD", parsed[0].name)
        assertEquals("FHD", parsed[0].qualityBadge)
        assertEquals("http://stream.m3u8", parsed[0].streamUrl)
    }

    @Test
    fun `quality badge detection test`() {
        val item4k = ChannelItem(
            id = "1",
            streamId = "1",
            name = "Cinema 4K Ultra",
            streamUrl = "http://stream"
        )
        assertEquals("4K", item4k.qualityBadge)

        val itemFhd = ChannelItem(
            id = "2",
            streamId = "2",
            name = "News FHD 1080p",
            streamUrl = "http://stream"
        )
        assertEquals("FHD", itemFhd.qualityBadge)
    }

    @Test
    fun `epg progress calculation test`() {
        val now = System.currentTimeMillis() / 1000L
        val program = EpgProgram(
            title = "Match Live",
            description = "Final cup match",
            startTime = "14:00",
            endTime = "16:00",
            startTimestamp = now - 1800, // half an hour ago
            endTimestamp = now + 1800   // half an hour later
        )
        // Elapsed is 1800 out of 3600 -> ~0.5f
        assertEquals(0.5f, program.progress, 0.05f)
    }

    @Test
    fun `expiry date calculation test`() {
        val expiredAccount = IptvAccountEntity(
            profileName = "Old Sub",
            serverUrl = "http://iptv.com",
            expiryDate = "2020-01-01"
        )
        assertTrue(expiredAccount.isExpired())
        assertNotNull(expiredAccount.getExpiryWarningText())

        val futureAccount = IptvAccountEntity(
            profileName = "New Sub",
            serverUrl = "http://iptv.com",
            expiryDate = "2030-01-01"
        )
        assertFalse(futureAccount.isExpired())
    }
}
