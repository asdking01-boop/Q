package com.example.data.repository

import com.example.data.local.FavoriteChannelEntity
import com.example.data.local.IptvAccountEntity
import com.example.data.local.IptvDao
import com.example.data.local.RecentChannelEntity
import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgram
import com.example.data.model.M3uParser
import com.example.data.network.DemoChannels
import com.example.data.network.DnsManager
import com.example.data.network.XtreamApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class IptvRepository(
    private val iptvDao: IptvDao,
    private val dnsManager: DnsManager? = null
) {

    private var apiClient: XtreamApiClient = XtreamApiClient(dnsManager ?: okhttp3.Dns.SYSTEM)

    val allAccounts: Flow<List<IptvAccountEntity>> = iptvDao.getAllAccounts()
    val activeAccountFlow: Flow<IptvAccountEntity?> = iptvDao.observeActiveAccount()

    fun updateDns() {
        apiClient = XtreamApiClient(dnsManager ?: okhttp3.Dns.SYSTEM)
    }

    suspend fun getActiveAccount(): IptvAccountEntity? = iptvDao.getActiveAccount()

    fun getFavorites(accountId: Long): Flow<List<FavoriteChannelEntity>> =
        iptvDao.getFavorites(accountId)

    fun getRecents(accountId: Long): Flow<List<RecentChannelEntity>> =
        iptvDao.getRecentChannels(accountId)

    suspend fun saveXtreamAccount(
        profileName: String,
        serverUrl: String,
        username: String,
        pass: String
    ): Result<IptvAccountEntity> {
        val authResult = apiClient.authenticate(serverUrl, username, pass)
        return if (authResult.isSuccess) {
            val auth = authResult.getOrNull()
            val uInfo = auth?.userInfo
            val expTimestamp = uInfo?.expDate?.toLongOrNull()
            val readableExp = if (expTimestamp != null && expTimestamp > 0) {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(expTimestamp * 1000L))
            } else {
                uInfo?.expDate ?: "غير محدد"
            }

            iptvDao.deactivateAllAccounts()

            val account = IptvAccountEntity(
                profileName = profileName.ifEmpty { "اشتراك ASD" },
                serverUrl = XtreamApiClient.sanitizeUrl(serverUrl),
                username = username.trim(),
                password = pass.trim(),
                isActive = true,
                accountStatus = uInfo?.status ?: "نشط",
                expiryDate = readableExp,
                maxConnections = uInfo?.maxConnections ?: "1",
                activeConnections = uInfo?.activeCons ?: "0",
                streamFormat = "m3u8"
            )
            val newId = iptvDao.insertAccount(account)
            Result.success(account.copy(id = newId))
        } else {
            Result.failure(authResult.exceptionOrNull() ?: Exception("فشل التحقق من الحساب"))
        }
    }

    suspend fun saveM3uAccount(
        profileName: String,
        m3uUrl: String
    ): Result<IptvAccountEntity> {
        val contentRes = apiClient.fetchM3uContent(m3uUrl)
        return if (contentRes.isSuccess) {
            iptvDao.deactivateAllAccounts()
            val account = IptvAccountEntity(
                profileName = profileName.ifEmpty { "قائمة M3U" },
                serverUrl = m3uUrl,
                isM3u = true,
                m3uUrl = m3uUrl,
                isActive = true,
                accountStatus = "نشط",
                expiryDate = "غير محدود"
            )
            val newId = iptvDao.insertAccount(account)
            Result.success(account.copy(id = newId))
        } else {
            Result.failure(contentRes.exceptionOrNull() ?: Exception("فشل تحميل قائمة M3U"))
        }
    }

    suspend fun switchAccount(accountId: Long) {
        iptvDao.deactivateAllAccounts()
        iptvDao.setActiveAccount(accountId)
    }

    suspend fun deleteAccount(accountId: Long) {
        iptvDao.deleteAccount(accountId)
        val remaining = iptvDao.getAllAccounts().firstOrNull()
        if (!remaining.isNullOrEmpty()) {
            iptvDao.setActiveAccount(remaining.first().id)
        }
    }

    suspend fun loadChannels(account: IptvAccountEntity?): Pair<List<String>, List<ChannelItem>> {
        if (account == null) {
            return Pair(DemoChannels.SAMPLE_CATEGORIES, DemoChannels.CHANNELS)
        }

        if (account.isM3u) {
            val contentRes = apiClient.fetchM3uContent(account.m3uUrl)
            return if (contentRes.isSuccess) {
                val parsed = M3uParser.parse(contentRes.getOrNull() ?: "")
                val cats = mutableListOf("الكل")
                cats.addAll(parsed.map { it.categoryName }.distinct().filter { it.isNotBlank() })
                Pair(cats, parsed)
            } else {
                Pair(DemoChannels.SAMPLE_CATEGORIES, DemoChannels.CHANNELS)
            }
        }

        // Xtream Codes
        val catsRes = apiClient.getLiveCategories(account.serverUrl, account.username, account.password)
        val streamsRes = apiClient.getLiveStreams(account.serverUrl, account.username, account.password)

        val categoriesMap = catsRes.getOrNull()?.associate { it.categoryId to it.categoryName } ?: emptyMap()
        val streams = streamsRes.getOrNull() ?: emptyList()

        if (streams.isEmpty()) {
            return Pair(DemoChannels.SAMPLE_CATEGORIES, DemoChannels.CHANNELS)
        }

        val channelItems = streams.map { stream ->
            val safeStreamId = stream.getSafeStreamId()
            val catName = categoriesMap[stream.categoryId] ?: "أخرى"
            val streamUrl = XtreamApiClient.buildLiveStreamUrl(
                serverUrl = account.serverUrl,
                username = account.username,
                pass = account.password,
                streamId = safeStreamId,
                format = account.streamFormat
            )

            ChannelItem(
                id = "${account.id}_$safeStreamId",
                streamId = safeStreamId,
                name = stream.name,
                iconUrl = stream.streamIcon,
                categoryId = stream.categoryId ?: "all",
                categoryName = catName,
                streamUrl = streamUrl,
                streamType = "LIVE"
            )
        }

        val categoryList = mutableListOf("الكل")
        categoryList.addAll(categoriesMap.values.filter { it.isNotBlank() })

        return Pair(categoryList, channelItems)
    }

    suspend fun getChannelEpg(account: IptvAccountEntity?, channel: ChannelItem): Pair<EpgProgram?, EpgProgram?> {
        if (account == null || account.isM3u) {
            // Provide simulated active EPG for demo / m3u channel
            val now = System.currentTimeMillis() / 1000L
            val current = EpgProgram(
                title = "بث مباشر - ${channel.name}",
                description = "متابعة حية ومباشرة بجودة فائقة بدون إعلانات.",
                startTime = "الآن",
                endTime = "القادم",
                startTimestamp = now - 1800,
                endTimestamp = now + 1800
            )
            val next = EpgProgram(
                title = "النشرة الإخبارية والبرامج المسائية",
                description = "تغطية شاملة لأهم الأخبار والتحليلات.",
                startTime = "القادم",
                endTime = "بعد ساعتين",
                startTimestamp = now + 1800,
                endTimestamp = now + 5400
            )
            return Pair(current, next)
        }

        val res = apiClient.getShortEpg(account.serverUrl, account.username, account.password, channel.streamId)
        val listings = res.getOrNull()?.epgListings
        if (listings.isNullOrEmpty()) {
            val now = System.currentTimeMillis() / 1000L
            val current = EpgProgram(
                title = "بث مباشر: ${channel.name}",
                description = "متابعة حية متواصلة.",
                startTime = "الآن",
                endTime = "القادم",
                startTimestamp = now - 1200,
                endTimestamp = now + 2400
            )
            return Pair(current, null)
        }

        val now = System.currentTimeMillis() / 1000L
        val currentListing = listings.firstOrNull { listing ->
            val start = listing.startTimestamp ?: 0L
            val stop = listing.stopTimestamp ?: Long.MAX_VALUE
            now in start..stop
        } ?: listings.firstOrNull()

        val currentIndex = if (currentListing != null) listings.indexOf(currentListing) else -1
        val nextListing = if (currentIndex != -1 && currentIndex < listings.size - 1) {
            listings[currentIndex + 1]
        } else null

        val currentProg = currentListing?.let {
            EpgProgram(
                title = it.getDecodedTitle(),
                description = it.getDecodedDescription(),
                startTime = it.start ?: "",
                endTime = it.end ?: "",
                startTimestamp = it.startTimestamp ?: (now - 600),
                endTimestamp = it.stopTimestamp ?: (now + 1800)
            )
        }

        val nextProg = nextListing?.let {
            EpgProgram(
                title = it.getDecodedTitle(),
                description = it.getDecodedDescription(),
                startTime = it.start ?: "",
                endTime = it.end ?: "",
                startTimestamp = it.startTimestamp ?: (now + 1800),
                endTimestamp = it.stopTimestamp ?: (now + 3600)
            )
        }

        return Pair(currentProg, nextProg)
    }

    suspend fun toggleFavorite(accountId: Long, channel: ChannelItem): Boolean {
        val favId = "${accountId}_${channel.streamId}"
        val exists = iptvDao.isFavorite(favId)
        if (exists) {
            iptvDao.deleteFavorite(favId)
            return false
        } else {
            iptvDao.insertFavorite(
                FavoriteChannelEntity(
                    id = favId,
                    accountId = accountId,
                    streamId = channel.streamId,
                    name = channel.name,
                    iconUrl = channel.iconUrl,
                    categoryId = channel.categoryId,
                    categoryName = channel.categoryName,
                    streamUrl = channel.streamUrl,
                    streamType = channel.streamType
                )
            )
            return true
        }
    }

    suspend fun recordRecent(accountId: Long, channel: ChannelItem) {
        val recentId = "${accountId}_${channel.streamId}"
        iptvDao.insertRecent(
            RecentChannelEntity(
                id = recentId,
                accountId = accountId,
                streamId = channel.streamId,
                name = channel.name,
                iconUrl = channel.iconUrl,
                categoryId = channel.categoryId,
                categoryName = channel.categoryName,
                streamUrl = channel.streamUrl,
                streamType = channel.streamType
            )
        )
    }

    suspend fun clearHistory(accountId: Long) {
        iptvDao.clearRecents(accountId)
    }
}
