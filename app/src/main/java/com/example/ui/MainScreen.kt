package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.player.VideoPlayerScreen
import com.example.ui.screens.AddSubscriptionDialog
import com.example.ui.screens.ChannelsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.viewmodel.IptvViewModel

@Composable
fun MainScreen(viewModel: IptvViewModel) {
    val context = LocalContext.current
    val activeTab by viewModel.activeTab.collectAsState()
    val activeAccount by viewModel.activeAccount.collectAsState()
    val allAccounts by viewModel.allAccounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val channels by viewModel.channels.collectAsState()
    val filteredChannels by viewModel.filteredChannels.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val recents by viewModel.recents.collectAsState()
    val currentPlayingChannel by viewModel.currentPlayingChannel.collectAsState()
    val currentEpg by viewModel.currentEpg.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddSubscriptionDialog by remember { mutableStateOf(false) }

    // Toast error notifications
    LaunchedEffect(errorMessage) {
        errorMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
        Scaffold(
            bottomBar = {
                if (currentPlayingChannel == null) {
                    NavigationBar(
                        containerColor = DarkSurface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = activeTab == 0,
                            onClick = { viewModel.setActiveTab(0) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = "الرئيسية"
                                )
                            },
                            label = { Text("الرئيسية", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = CyanPrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_home")
                        )

                        NavigationBarItem(
                            selected = activeTab == 1,
                            onClick = { viewModel.setActiveTab(1) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = "البث المباشر"
                                )
                            },
                            label = { Text("القنوات", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = CyanPrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_channels")
                        )

                        NavigationBarItem(
                            selected = activeTab == 2,
                            onClick = { viewModel.setActiveTab(2) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "المفضلة"
                                )
                            },
                            label = { Text("المفضلة", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = CyanPrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_favorites")
                        )

                        NavigationBarItem(
                            selected = activeTab == 3,
                            onClick = { viewModel.setActiveTab(3) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "الإعدادات"
                                )
                            },
                            label = { Text("الإعدادات", fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = CyanPrimary,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = CyanPrimary
                            ),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (activeTab) {
                    0 -> HomeScreen(
                        activeAccount = activeAccount,
                        allAccounts = allAccounts,
                        channels = channels,
                        favorites = favorites,
                        recents = recents,
                        onOpenLiveTv = { viewModel.setActiveTab(1) },
                        onOpenFavorites = { viewModel.setActiveTab(2) },
                        onPlayChannel = { viewModel.playChannel(context, it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onOpenAddSubscription = { showAddSubscriptionDialog = true },
                        onSwitchAccount = { viewModel.switchAccount(it) }
                    )
                    1 -> ChannelsScreen(
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onSelectCategory = { viewModel.selectCategory(it) },
                        selectedQuality = selectedQuality,
                        onSelectQuality = { viewModel.setSelectedQuality(it) },
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        channels = filteredChannels,
                        isLoading = isLoading,
                        onPlayChannel = { viewModel.playChannel(context, it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onRefresh = { viewModel.refreshChannels() }
                    )
                    2 -> FavoritesScreen(
                        favorites = favorites,
                        onPlayChannel = { viewModel.playChannel(context, it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onNavigateToChannels = { viewModel.setActiveTab(1) }
                    )
                    3 -> SettingsScreen(
                        viewModel = viewModel,
                        onOpenAddSubscription = { showAddSubscriptionDialog = true }
                    )
                }
            }
        }

        // Full Screen Video Player Overlay
        if (currentPlayingChannel != null) {
            VideoPlayerScreen(
                channel = currentPlayingChannel!!,
                channelList = filteredChannels.ifEmpty { channels },
                epgPair = currentEpg,
                onClose = { viewModel.stopPlayer() },
                onNextChannel = { viewModel.playNextChannel() },
                onPreviousChannel = { viewModel.playPreviousChannel() },
                onSelectChannel = { viewModel.playChannel(context, it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) }
            )
        }

        // Add Subscription Dialog
        AddSubscriptionDialog(
            isOpen = showAddSubscriptionDialog,
            onDismiss = { showAddSubscriptionDialog = false },
            isLoading = isConnecting,
            onConnectXtream = { profile, host, user, pass ->
                viewModel.saveXtreamSubscription(
                    profileName = profile,
                    serverUrl = host,
                    username = user,
                    password = pass,
                    onSuccess = {
                        showAddSubscriptionDialog = false
                        Toast.makeText(context, "تم حفظ وتفعيل الاشتراك بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onConnectM3u = { profile, url ->
                viewModel.saveM3uSubscription(
                    profileName = profile,
                    m3uUrl = url,
                    onSuccess = {
                        showAddSubscriptionDialog = false
                        Toast.makeText(context, "تم تحميل القنوات بنجاح!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            },
            onUseDemo = {
                showAddSubscriptionDialog = false
                viewModel.refreshChannels()
                Toast.makeText(context, "تم تحميل باقة البث المباشر التجريبية", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
