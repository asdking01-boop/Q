package com.example.data.network

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

enum class DnsMode(val displayName: String, val ips: List<String>) {
    SYSTEM("افتراضي النظام (ISP / Emulator)", emptyList()),
    CLOUDFLARE("Cloudflare DNS (1.1.1.1)", listOf("1.1.1.1", "1.0.0.1")),
    GOOGLE("Google Public DNS (8.8.8.8)", listOf("8.8.8.8", "8.8.4.4")),
    QUAD9("Quad9 DNS (9.9.9.9 - حماية وتخطي الحجب)", listOf("9.9.9.9", "149.112.112.112")),
    ADGUARD("AdGuard DNS (حظر إعلانات وتخطي قيود)", listOf("94.140.14.14", "94.140.15.15"))
}

class DnsManager(context: Context) : Dns {

    private val prefs: SharedPreferences = context.getSharedPreferences("asd_dns_prefs", Context.MODE_PRIVATE)

    private val _currentMode = MutableStateFlow(
        try {
            DnsMode.valueOf(prefs.getString("dns_mode", DnsMode.SYSTEM.name) ?: DnsMode.SYSTEM.name)
        } catch (_: Exception) {
            DnsMode.SYSTEM
        }
    )
    val currentMode: StateFlow<DnsMode> = _currentMode.asStateFlow()

    private val _customDnsIp = MutableStateFlow(prefs.getString("custom_dns_ip", "") ?: "")
    val customDnsIp: StateFlow<String> = _customDnsIp.asStateFlow()

    fun setDnsMode(mode: DnsMode) {
        prefs.edit().putString("dns_mode", mode.name).apply()
        _currentMode.value = mode
    }

    fun setCustomDnsIp(ip: String) {
        prefs.edit().putString("custom_dns_ip", ip).apply()
        _customDnsIp.value = ip
    }

    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            val addresses = Dns.SYSTEM.lookup(hostname)
            if (addresses.isNotEmpty()) {
                addresses
            } else {
                InetAddress.getAllByName(hostname).toList()
            }
        } catch (e: UnknownHostException) {
            try {
                InetAddress.getAllByName(hostname).toList()
            } catch (_: Exception) {
                throw e
            }
        } catch (e: Exception) {
            throw UnknownHostException("تعذر العثور على عنوان المضيف: $hostname (${e.message})")
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: DnsManager? = null

        fun getInstance(context: Context): DnsManager {
            return INSTANCE ?: synchronized(this) {
                val instance = DnsManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
