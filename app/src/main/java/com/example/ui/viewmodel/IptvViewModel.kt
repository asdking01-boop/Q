package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.IptvAccountEntity
import com.example.data.local.IptvDatabase
import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgram
import com.example.data.network.DnsManager
import com.example.data.network.DnsMode
import com.example.data.repository.IptvRepository
import com.example.data.security.ParentalControlManager
import com.example.util.ExternalPlayerHelper
import com.example.util.PlayerChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: IptvRepository
    val dnsManager = DnsManager.getInstance(application)
    val parentalManager = ParentalControlManager.getInstance(application)

    val allAccounts: StateFlow<List<IptvAccountEntity>>
    val activeAccount: StateFlow<IptvAccountEntity?>

    private val _categories = MutableStateFlow<List<String>>(listOf("الكل"))
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedQuality = MutableStateFlow("الكل")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _channels = MutableStateFlow<List<ChannelItem>>(emptyList())
    val channels: StateFlow<List<ChannelItem>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentPlayingChannel = MutableStateFlow<ChannelItem?>(null)
    val currentPlayingChannel: StateFlow<ChannelItem?> = _currentPlayingChannel.asStateFlow()

    private val _currentEpg = MutableStateFlow<Pair<EpgProgram?, EpgProgram?>?>(null)
    val currentEpg: StateFlow<Pair<EpgProgram?, EpgProgram?>?> = _currentEpg.asStateFlow()

    private val _favorites = MutableStateFlow<List<ChannelItem>>(emptyList())
    val favorites: StateFlow<List<ChannelItem>> = _favorites.asStateFlow()

    private val _recents = MutableStateFlow<List<ChannelItem>>(emptyList())
    val recents: StateFlow<List<ChannelItem>> = _recents.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    // Player Selection Prefs
    private val prefs = application.getSharedPreferences("asd_player_prefs", Context.MODE_PRIVATE)
    private val _playerChoice = MutableStateFlow(
        try {
            PlayerChoice.valueOf(prefs.getString("player_choice", PlayerChoice.INTERNAL.name) ?: PlayerChoice.INTERNAL.name)
        } catch (_: Exception) {
            PlayerChoice.INTERNAL
        }
    )
    val playerChoice: StateFlow<PlayerChoice> = _playerChoice.asStateFlow()

    // Filtered Channels with Category, Quality, Search, and Parental Control
    val filteredChannels: StateFlow<List<ChannelItem>> = combine(
        _channels,
        _selectedCategory,
        _selectedQuality,
        _searchQuery,
        _favorites,
        parentalManager.isEnabled,
        parentalManager.isSessionUnlocked
    ) { args ->
        val list = args[0] as List<ChannelItem>
        val cat = args[1] as String
        val quality = args[2] as String
        val query = args[3] as String
        val favs = args[4] as List<ChannelItem>
        val parentalEnabled = args[5] as Boolean
        val sessionUnlocked = args[6] as Boolean

        val favIds = favs.map { it.streamId }.toSet()
        val mappedWithFav = list.map { it.copy(isFavorite = favIds.contains(it.streamId)) }

        mappedWithFav.filter { item ->
            // Parental Lock check
            if (parentalEnabled && !sessionUnlocked && parentalManager.isCategoryLocked(item.categoryName)) {
                return@filter false
            }

            val matchesCat = (cat == "الكل") || item.categoryName == cat || item.categoryId == cat
            val matchesQuality = (quality == "الكل") || item.qualityBadge.equals(quality, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    item.name.contains(query, ignoreCase = true) ||
                    item.categoryName.contains(query, ignoreCase = true)

            matchesCat && matchesQuality && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        val database = IptvDatabase.getDatabase(application)
        repository = IptvRepository(database.iptvDao(), dnsManager)
        allAccounts = repository.allAccounts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        activeAccount = repository.activeAccountFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        viewModelScope.launch {
            activeAccount.collect { account ->
                loadAccountData(account)
            }
        }
    }

    fun setActiveTab(tab: Int) {
        _activeTab.value = tab
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedQuality(quality: String) {
        _selectedQuality.value = quality
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setDnsMode(mode: DnsMode) {
        dnsManager.setDnsMode(mode)
        repository.updateDns()
        refreshChannels()
    }

    fun setPlayerChoice(choice: PlayerChoice) {
        prefs.edit().putString("player_choice", choice.name).apply()
        _playerChoice.value = choice
    }

    fun loadAccountData(account: IptvAccountEntity?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val (cats, items) = repository.loadChannels(account)
                _categories.value = cats
                _channels.value = items

                val currentAccId = account?.id ?: 0L
                repository.getFavorites(currentAccId).collect { favEntities ->
                    _favorites.value = favEntities.map { entity ->
                        ChannelItem(
                            id = entity.id,
                            streamId = entity.streamId,
                            name = entity.name,
                            iconUrl = entity.iconUrl,
                            categoryId = entity.categoryId,
                            categoryName = entity.categoryName,
                            streamUrl = entity.streamUrl,
                            streamType = entity.streamType,
                            isFavorite = true
                        )
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "حدث خطأ أثناء تحميل القنوات"
            } finally {
                _isLoading.value = false
            }
        }

        val accId = account?.id ?: 0L
        viewModelScope.launch {
            repository.getRecents(accId).collect { recEntities ->
                _recents.value = recEntities.map { entity ->
                    ChannelItem(
                        id = entity.id,
                        streamId = entity.streamId,
                        name = entity.name,
                        iconUrl = entity.iconUrl,
                        categoryId = entity.categoryId,
                        categoryName = entity.categoryName,
                        streamUrl = entity.streamUrl,
                        streamType = entity.streamType,
                        isRecent = true
                    )
                }
            }
        }
    }

    fun refreshChannels() {
        loadAccountData(activeAccount.value)
    }

    fun playChannel(context: Context, channel: ChannelItem) {
        val choice = _playerChoice.value
        if (choice == PlayerChoice.INTERNAL) {
            _currentPlayingChannel.value = channel
            loadEpg(channel)
        } else {
            ExternalPlayerHelper.launchPlayer(
                context = context,
                streamUrl = channel.streamUrl,
                channelName = channel.name,
                playerChoice = choice,
                onFallbackToInternal = {
                    _currentPlayingChannel.value = channel
                    loadEpg(channel)
                }
            )
        }

        viewModelScope.launch {
            val accId = activeAccount.value?.id ?: 0L
            repository.recordRecent(accId, channel)
        }
    }

    private fun loadEpg(channel: ChannelItem) {
        viewModelScope.launch {
            val epg = repository.getChannelEpg(activeAccount.value, channel)
            _currentEpg.value = epg
        }
    }

    fun stopPlayer() {
        _currentPlayingChannel.value = null
        _currentEpg.value = null
    }

    fun playNextChannel() {
        val current = _currentPlayingChannel.value ?: return
        val list = filteredChannels.value.ifEmpty { _channels.value }
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.streamId == current.streamId }
        val nextIndex = if (currentIndex in 0 until list.size - 1) currentIndex + 1 else 0
        val next = list[nextIndex]
        _currentPlayingChannel.value = next
        loadEpg(next)
    }

    fun playPreviousChannel() {
        val current = _currentPlayingChannel.value ?: return
        val list = filteredChannels.value.ifEmpty { _channels.value }
        if (list.isEmpty()) return
        val currentIndex = list.indexOfFirst { it.streamId == current.streamId }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
        val prev = list[prevIndex]
        _currentPlayingChannel.value = prev
        loadEpg(prev)
    }

    fun toggleFavorite(channel: ChannelItem) {
        viewModelScope.launch {
            val accId = activeAccount.value?.id ?: 0L
            repository.toggleFavorite(accId, channel)
        }
    }

    fun saveXtreamSubscription(
        profileName: String,
        serverUrl: String,
        username: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isConnecting.value = true
            _errorMessage.value = null
            try {
                val res = repository.saveXtreamAccount(profileName, serverUrl, username, password)
                if (res.isSuccess) {
                    onSuccess()
                } else {
                    val msg = res.exceptionOrNull()?.localizedMessage ?: "فشل تسجيل الدخول للاشتراك"
                    _errorMessage.value = msg
                    onError(msg)
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "حدث خطأ غير متوقع"
                _errorMessage.value = msg
                onError(msg)
            } finally {
                _isConnecting.value = false
            }
        }
    }

    fun saveM3uSubscription(
        profileName: String,
        m3uUrl: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isConnecting.value = true
            _errorMessage.value = null
            try {
                val res = repository.saveM3uAccount(profileName, m3uUrl)
                if (res.isSuccess) {
                    onSuccess()
                } else {
                    val msg = res.exceptionOrNull()?.localizedMessage ?: "فشل تحميل قائمة M3U"
                    _errorMessage.value = msg
                    onError(msg)
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "حدث خطأ غير متوقع"
                _errorMessage.value = msg
                onError(msg)
            } finally {
                _isConnecting.value = false
            }
        }
    }

    fun switchAccount(accountId: Long) {
        viewModelScope.launch {
            repository.switchAccount(accountId)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            val accId = activeAccount.value?.id ?: 0L
            repository.clearHistory(accId)
        }
    }
}
