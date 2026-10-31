package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.ChannelItem
import com.example.data.model.EpgProgram
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.LiveRed
import kotlinx.coroutines.delay

enum class ResizeMode(val label: String, val mode: Int) {
    FIT("ملاءمة (Fit)", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    FILL("ملء الشاشة (Zoom)", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    FIXED_16_9("أبعاد 16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    FIXED_4_3("أبعاد 4:3", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT)
}

data class TrackInfo(
    val id: String,
    val name: String,
    val language: String?,
    val isSelected: Boolean,
    val groupIndex: Int,
    val trackIndex: Int
)

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    channel: ChannelItem,
    channelList: List<ChannelItem>,
    epgPair: Pair<EpgProgram?, EpgProgram?>? = null,
    onClose: () -> Unit,
    onNextChannel: () -> Unit,
    onPreviousChannel: () -> Unit,
    onSelectChannel: (ChannelItem) -> Unit,
    onToggleFavorite: (ChannelItem) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    var controlsVisible by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    var showChannelsSidebar by remember { mutableStateOf(false) }
    var showTrackDialog by remember { mutableStateOf(false) }
    var showEpgOverlay by remember { mutableStateOf(false) }

    // Gesture indicator state
    var isAdjustingVolume by remember { mutableStateOf(false) }
    var isAdjustingBrightness by remember { mutableStateOf(false) }
    var volumeLevel by remember { mutableFloatStateOf(0.5f) }
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Audio & Subtitle Tracks
    var audioTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }
    var subtitleTracks by remember { mutableStateOf<List<TrackInfo>>(emptyList()) }

    val resizeModes = listOf(
        ResizeMode.FIT,
        ResizeMode.FILL,
        ResizeMode.FIXED_16_9,
        ResizeMode.FIXED_4_3
    )
    val currentResizeMode = resizeModes[resizeModeIndex % resizeModes.size]

    // Keep screen on during playback
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        volumeLevel = curVol.toFloat() / maxVol.coerceAtLeast(1)

        val curBright = activity?.window?.attributes?.screenBrightness ?: 0.5f
        brightnessLevel = if (curBright < 0) 0.5f else curBright

        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("IPTVSmarters/3.1.5 (Linux; Android 14)")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    fun updateTracks(tracks: Tracks) {
        val audios = mutableListOf<TrackInfo>()
        val subs = mutableListOf<TrackInfo>()

        for (groupIndex in 0 until tracks.groups.size) {
            val group = tracks.groups[groupIndex]
            val trackType = group.type
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                val isSelected = group.isTrackSelected(trackIndex)
                val lang = format.language ?: "افتراضي"
                val name = format.label ?: if (trackType == C.TRACK_TYPE_AUDIO) "مسار صوتي ${audios.size + 1} ($lang)" else "ترجمة ${subs.size + 1} ($lang)"

                val info = TrackInfo(
                    id = "${groupIndex}_$trackIndex",
                    name = name,
                    language = lang,
                    isSelected = isSelected,
                    groupIndex = groupIndex,
                    trackIndex = trackIndex
                )

                if (trackType == C.TRACK_TYPE_AUDIO) {
                    audios.add(info)
                } else if (trackType == C.TRACK_TYPE_TEXT) {
                    subs.add(info)
                }
            }
        }
        audioTracks = audios
        subtitleTracks = subs
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    playbackError = null
                    isPlaying = exoPlayer.playWhenReady
                    updateTracks(exoPlayer.currentTracks)
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                updateTracks(tracks)
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                val cause = error.cause
                playbackError = if (cause is java.net.UnknownHostException || error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                    "تعذر الاتصال بسيرفر القناة (لا يوجد اتصال أو تعذر العثور على المضيف). تأكد من اتصال هاتفك بالإنترنت أو اختر قناة أخرى."
                } else if (error.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS) {
                    "رفض السيرفر تشغيل البث (رمز الاستجابة غير صالح أو انتهت جلسة البث). جرب تحديث البث."
                } else {
                    "تعذر تشغيل البث المباشر (${error.errorCodeName}). جرب تحديث البث أو اختيار قناة أخرى."
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Load channel stream whenever channel changes
    LaunchedEffect(channel.streamUrl) {
        isBuffering = true
        playbackError = null
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(channel.streamUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            playbackError = "رابط البث غير صالح: ${e.message}"
            isBuffering = false
        }
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(controlsVisible, isPlaying, showChannelsSidebar, showTrackDialog) {
        if (controlsVisible && isPlaying && !showChannelsSidebar && !showTrackDialog) {
            delay(4500)
            controlsVisible = false
        }
    }

    BackHandler {
        if (showChannelsSidebar) {
            showChannelsSidebar = false
        } else if (showTrackDialog) {
            showTrackDialog = false
        } else {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val isLeftSide = offset.x < (containerSize.width / 2)
                        if (isLeftSide) {
                            isAdjustingBrightness = true
                        } else {
                            isAdjustingVolume = true
                        }
                    },
                    onDragEnd = {
                        isAdjustingBrightness = false
                        isAdjustingVolume = false
                    },
                    onDragCancel = {
                        isAdjustingBrightness = false
                        isAdjustingVolume = false
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount / 350f
                        if (isAdjustingBrightness) {
                            val newB = (brightnessLevel + delta).coerceIn(0.05f, 1.0f)
                            brightnessLevel = newB
                            activity?.let { act ->
                                val lp = act.window.attributes
                                lp.screenBrightness = newB
                                act.window.attributes = lp
                            }
                        } else if (isAdjustingVolume) {
                            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            val newV = (volumeLevel + delta).coerceIn(0f, 1f)
                            volumeLevel = newV
                            val targetVol = (newV * maxVol).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, 0)
                        }
                    }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // Video Render Surface
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    this.resizeMode = currentResizeMode.mode
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = currentResizeMode.mode
            }
        )

        // Volume Gesture HUD
        if (isAdjustingVolume) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 36.dp),
                shape = RoundedCornerShape(14.dp),
                color = DarkBg.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(volumeLevel * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Brightness Gesture HUD
        if (isAdjustingBrightness) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 36.dp),
                shape = RoundedCornerShape(14.dp),
                color = DarkBg.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${(brightnessLevel * 100).toInt()}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مباشر LIVE",
                                color = LiveRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CyanPrimary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = channel.qualityBadge,
                                    color = CyanPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = channel.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = channel.categoryName,
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    // Audio & Subtitle Selector
                    IconButton(
                        onClick = { showTrackDialog = true },
                        modifier = Modifier.testTag("player_audio_tracks_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "الصوت والترجمة",
                            tint = CyanPrimary
                        )
                    }

                    // EPG Guide Toggle
                    IconButton(
                        onClick = { showEpgOverlay = !showEpgOverlay },
                        modifier = Modifier.testTag("player_epg_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "دليل البرامج",
                            tint = if (showEpgOverlay) CyanPrimary else Color.White
                        )
                    }

                    // Aspect Ratio Button
                    IconButton(
                        onClick = { resizeModeIndex++ },
                        modifier = Modifier.testTag("player_aspect_ratio_button")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "أبعاد الشاشة",
                                tint = CyanPrimary
                            )
                            Text(
                                text = currentResizeMode.label.substringBefore(" "),
                                color = Color.White,
                                fontSize = 9.sp
                            )
                        }
                    }

                    // Orientation Switch
                    IconButton(
                        onClick = {
                            activity?.let { act ->
                                val current = act.requestedOrientation
                                act.requestedOrientation = if (current == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                } else {
                                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                }
                            }
                        },
                        modifier = Modifier.testTag("player_orientation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = "تدوير الشاشة",
                            tint = Color.White
                        )
                    }

                    // Open Channels Drawer
                    IconButton(
                        onClick = { showChannelsSidebar = !showChannelsSidebar },
                        modifier = Modifier.testTag("player_channels_list_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "قائمة القنوات",
                            tint = Color.White
                        )
                    }
                }

                // EPG Program Banner (if enabled or available)
                if (showEpgOverlay && epgPair?.first != null) {
                    val currentProg = epgPair.first!!
                    val nextProg = epgPair.second
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 90.dp, start = 20.dp, end = 20.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface.copy(alpha = 0.9f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "البرنامج الحالي: ${currentProg.title}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = currentProg.timeSpan,
                                    color = CyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { currentProg.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = CyanPrimary,
                                trackColor = DarkSurfaceVariant
                            )
                            if (nextProg != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "التالي: ${nextProg.title} (${nextProg.timeSpan})",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Center Play/Pause & Channel Switch Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    IconButton(
                        onClick = onPreviousChannel,
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("player_prev_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "القناة السابقة",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    if (isBuffering) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = CyanPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(CyanPrimary, CircleShape)
                                .testTag("player_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                tint = Color.Black,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onNextChannel,
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("player_next_channel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "القناة التالية",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onToggleFavorite(channel) },
                            modifier = Modifier.testTag("player_fav_button")
                        ) {
                            Icon(
                                imageVector = if (channel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "المفضلة",
                                tint = if (channel.isFavorite) LiveRed else Color.White
                            )
                        }

                        Text(
                            text = if (channel.isFavorite) "في المفضلة" else "إضافة للمفضلة",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            isBuffering = true
                            playbackError = null
                            val item = MediaItem.fromUri(Uri.parse(channel.streamUrl))
                            exoPlayer.setMediaItem(item)
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        modifier = Modifier.testTag("player_reload_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "إعادة تحميل البث",
                                tint = CyanPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تحديث البث",
                                color = CyanPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Error message banner
        if (playbackError != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = LiveRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "تنبيه البث المباشر",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackError ?: "",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CyanPrimary,
                            modifier = Modifier.clickable {
                                playbackError = null
                                isBuffering = true
                                val item = MediaItem.fromUri(Uri.parse(channel.streamUrl))
                                exoPlayer.setMediaItem(item)
                                exoPlayer.prepare()
                                exoPlayer.play()
                            }
                        ) {
                            Text(
                                text = "إعادة المحاولة",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurfaceVariant,
                            modifier = Modifier.clickable { onNextChannel() }
                        ) {
                            Text(
                                text = "القناة التالية",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.DarkGray,
                            modifier = Modifier.clickable { onClose() }
                        ) {
                            Text(
                                text = "الرجوع",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Channels Sidebar Drawer
        AnimatedVisibility(
            visible = showChannelsSidebar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp),
                color = DarkBg.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "قائمة القنوات (${channelList.size})",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { showChannelsSidebar = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.LightGray
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(channelList) { item ->
                            val isCurrent = item.streamId == channel.streamId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isCurrent) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        onSelectChannel(item)
                                        showChannelsSidebar = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(CyanPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = item.name,
                                    color = if (isCurrent) CyanPrimary else Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Audio & Subtitle Track Selector Dialog
        if (showTrackDialog) {
            Dialog(onDismissRequest = { showTrackDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المسارات الصوتية والترجمة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = { showTrackDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "المسارات الصوتية (Audio Tracks):",
                            color = CyanPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        if (audioTracks.isEmpty()) {
                            Text(
                                text = "المسار الصوتي الافتراضي للبث (Stereo)",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            audioTracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val params = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setPreferredAudioLanguage(track.language)
                                                .build()
                                            exoPlayer.trackSelectionParameters = params
                                            showTrackDialog = false
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Audiotrack,
                                        contentDescription = null,
                                        tint = if (track.isSelected) CyanPrimary else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = track.name,
                                        color = if (track.isSelected) CyanPrimary else Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "الترجمة (Subtitles):",
                            color = CyanPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )

                        if (subtitleTracks.isEmpty()) {
                            Text(
                                text = "لا توجد ملفات ترجمة مدمجة في هذا البث",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            subtitleTracks.forEach { track ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val params = exoPlayer.trackSelectionParameters
                                                .buildUpon()
                                                .setPreferredTextLanguage(track.language)
                                                .build()
                                            exoPlayer.trackSelectionParameters = params
                                            showTrackDialog = false
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Subtitles,
                                        contentDescription = null,
                                        tint = if (track.isSelected) CyanPrimary else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = track.name,
                                        color = if (track.isSelected) CyanPrimary else Color.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
