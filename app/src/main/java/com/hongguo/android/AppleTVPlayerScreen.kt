package com.hongguo.android

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusGroup
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.hongguo.account.AccountSessionStatus
import com.hongguo.account.AccountState

/** Short transition while the selected work and first stream are being prepared. */
@Composable
internal fun AppleTVPreparingScreen(state: HongguoUiState, model: HongguoViewModel) {
    val retryFocus = remember { FocusRequester() }
    androidx.compose.runtime.LaunchedEffect(state.detailError) {
        if (state.detailError != null) runCatching { retryFocus.requestFocus() }
    }
    Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) {
            model.closeDetail()
            true
        } else false
    }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(appleTVDp(16f))) {
            if (state.detailError == null) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.width(appleTVDp(44f)).height(appleTVDp(44f)))
                Text(state.preparationStage.ifBlank { if (state.loadingDetail) "正在载入剧集…" else "正在准备播放…" },
                    color = Color.White, fontSize = appleTVSp(18f))
            } else {
                Text(state.selectedCard?.title.orEmpty(), color = Color.White, fontSize = appleTVSp(24f), fontWeight = FontWeight.Bold)
                Text(state.detailError.orEmpty(), color = Color.White.copy(alpha = .72f), fontSize = appleTVSp(16f))
                AppleTVControl("重试", modifier = Modifier.focusRequester(retryFocus), onClick = model::retryDetails)
            }
        }
    }
}

/** TV player presentation: full-bleed video, transport controls and the four Apple TV info tabs. */
@Composable
internal fun AppleTVPlayerScreen(
    state: HongguoUiState,
    model: HongguoViewModel,
    accountModel: com.hongguo.account.AccountViewModel,
    accountState: AccountState,
    active: Boolean = true,
) {
    val detail = state.detail ?: return
    val currentCard = state.selectedCard ?: DramaCard(detail.id, detail.title, detail.cover, detail.episodes.size, detail.summary)
    val favorite = if (accountState.sessionStatus == AccountSessionStatus.LOGGED_IN)
        accountState.favorites.any { it.card.id == detail.id }
    else state.favorites.any { it.card.id == detail.id }
    val activity = LocalActivity.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val playback = rememberVideoPlayback(
        state.video,
        state.resumePositionMs,
        model,
        enabled = active,
        playbackSpeed = state.playbackSpeed,
    )
    val playFocus = remember { FocusRequester() }
    val infoFocus = remember { FocusRequester() }
    val infoTabFocus = remember { FocusRequester() }
    var initialPlayerFocusSet by remember { mutableStateOf(false) }
    var showInfo by rememberSaveable(detail.id) { mutableStateOf(false) }
    var infoTab by rememberSaveable(detail.id) { mutableStateOf("选集") }
    var targetEpisode by rememberSaveable(detail.id) { mutableStateOf(state.episodePosition.coerceAtLeast(0)) }
    var seekValue by remember(state.video?.requestId, playback.durationMs) {
        mutableFloatStateOf(if (playback.durationMs > 0) playback.positionMs.toFloat() / playback.durationMs else 0f)
    }
    androidx.compose.runtime.LaunchedEffect(playback.player != null) {
        if (!initialPlayerFocusSet) runCatching {
            if (playback.player != null) {
                playFocus.requestFocus()
                initialPlayerFocusSet = true
            } else infoFocus.requestFocus()
        }
    }
    androidx.compose.runtime.LaunchedEffect(showInfo) {
        runCatching { if (showInfo) infoTabFocus.requestFocus() else infoFocus.requestFocus() }
    }
    DisposableEffect(active, activity, view, lifecycle, model) {
        if (!active) return@DisposableEffect onDispose { }
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        val oldLightStatus = controller?.isAppearanceLightStatusBars
        val oldLightNavigation = controller?.isAppearanceLightNavigationBars
        val oldBehavior = controller?.systemBarsBehavior
        val wereBarsVisible = ViewCompat.getRootWindowInsets(view)?.isVisible(WindowInsetsCompat.Type.systemBars()) ?: false
        fun hideBars() {
            controller?.isAppearanceLightStatusBars = false
            controller?.isAppearanceLightNavigationBars = false
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        hideBars()
        model.onPlayerForeground(true)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> model.onPlayerForeground(true)
                Lifecycle.Event.ON_RESUME -> hideBars()
                Lifecycle.Event.ON_STOP -> model.onPlayerForeground(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            model.onPlayerForeground(false)
            if (wereBarsVisible) controller?.show(WindowInsetsCompat.Type.systemBars())
            else controller?.hide(WindowInsetsCompat.Type.systemBars())
            if (oldLightStatus != null) controller?.isAppearanceLightStatusBars = oldLightStatus
            if (oldLightNavigation != null) controller?.isAppearanceLightNavigationBars = oldLightNavigation
            if (oldBehavior != null) controller?.systemBarsBehavior = oldBehavior
        }
    }
    BackHandler(enabled = showInfo) { showInfo = false }
    androidx.compose.runtime.LaunchedEffect(showInfo, infoTab, state.episodePosition) {
        if (showInfo && infoTab == "选集") targetEpisode = state.episodePosition.coerceAtLeast(0)
    }

    Box(Modifier.fillMaxSize().background(Color.Black).onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && event.key == Key.Menu) {
            if (showInfo) showInfo = false else model.closePlayerToLibrary()
            true
        } else false
    }) {
        VideoSurface(playback, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Black.copy(alpha = .38f),
            .45f to Color.Transparent,
            1f to Color.Black.copy(alpha = .88f),
        )))
        Column(Modifier.fillMaxSize().padding(horizontal = appleTVDp(64f), vertical = appleTVDp(34f))) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(detail.title, color = Color.White, fontSize = appleTVSp(24f), fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "第 ${(state.episodePosition + 1).coerceAtLeast(1)} 集 / 共 ${detail.episodes.size} 集",
                        color = Color.White.copy(alpha = .72f), fontSize = appleTVSp(15f),
                    )
                }
                if (playback.isBuffering || state.preparing) CircularProgressIndicator(color = Color.White, modifier = Modifier.width(appleTVDp(24f)).height(appleTVDp(24f)))
            }
            Spacer(Modifier.weight(1f))
            if (state.playbackError != null) {
                Text(state.playbackError, color = Color.White, fontSize = appleTVSp(16f),
                    modifier = Modifier.padding(bottom = appleTVDp(12f)))
            } else if (state.preparing && playback.player == null) {
                Text(state.preparationStage.ifBlank { "正在准备播放…" }, color = Color.White, fontSize = appleTVSp(16f),
                    modifier = Modifier.padding(bottom = appleTVDp(12f)))
            }
            var dragging by remember { mutableStateOf(false) }
            Slider(
                value = if (dragging) seekValue else if (playback.durationMs > 0)
                    (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f) else 0f,
                onValueChange = {
                    dragging = true
                    seekValue = it
                },
                onValueChangeFinished = {
                    if (playback.durationMs > 0) playback.player?.seekTo((seekValue * playback.durationMs).toLong())
                    dragging = false
                },
                valueRange = 0f..1f,
                enabled = playback.player != null && playback.durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = .28f),
                    disabledThumbColor = Color.White.copy(alpha = .3f),
                    disabledActiveTrackColor = Color.White.copy(alpha = .25f),
                    disabledInactiveTrackColor = Color.White.copy(alpha = .12f),
                ),
            )
            Row(Modifier.focusGroup(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(appleTVDp(14f))) {
                Text(formatTVTime(if (dragging) (seekValue * playback.durationMs).toLong() else playback.positionMs),
                    color = Color.White, fontSize = appleTVSp(13f))
                Spacer(Modifier.weight(1f))
                Text(formatTVTime(playback.durationMs), color = Color.White.copy(alpha = .7f), fontSize = appleTVSp(13f))
            }
            Spacer(Modifier.height(appleTVDp(12f)))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(appleTVDp(14f))) {
                AppleTVControl(if (playback.isPlaying) "暂停" else "播放", enabled = playback.player != null,
                    modifier = Modifier.focusRequester(playFocus)) {
                    playback.player?.let { player -> if (player.isPlaying) player.pause() else player.play() }
                }
                AppleTVControl("上一集", enabled = state.episodePosition > 0) {
                    if (state.episodePosition > 0) model.playEpisode(state.episodePosition - 1)
                }
                AppleTVControl("下一集", enabled = state.episodePosition + 1 < detail.episodes.size) {
                    if (state.episodePosition + 1 < detail.episodes.size) model.playEpisode(state.episodePosition + 1)
                }
                AppleTVControl(if (favorite) "取消收藏" else "收藏", selected = favorite) {
                    if (accountState.sessionStatus == AccountSessionStatus.LOGGED_IN) {
                        accountModel.setFavorite(currentCard, !favorite)
                    } else model.toggleFavorite(currentCard)
                }
                AppleTVControl("信息与选项", selected = showInfo, modifier = Modifier.focusRequester(infoFocus)) { showInfo = !showInfo }
            }
        }
        if (showInfo) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(appleTVDp(300f))
                    .padding(horizontal = appleTVDp(64f), vertical = appleTVDp(24f))
                    .background(Color(0xF0181818), RoundedCornerShape(appleTVDp(18f)))
                    .padding(if (infoTab == "相关作品") appleTVDp(20f) else appleTVDp(24f)),
            ) {
                Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(appleTVDp(12f))) {
                    listOf("选集", "相关作品", "倍速", "清晰度").forEach { tab ->
                        AppleTVControl(tab, selected = infoTab == tab,
                            modifier = if (tab == "选集") Modifier.focusRequester(infoTabFocus) else Modifier) { infoTab = tab }
                    }
                }
                Spacer(Modifier.height(appleTVDp(18f)))
                when (infoTab) {
                    "选集" -> {
                        if (detail.episodes.isEmpty()) {
                            Text("暂无剧集", color = Color.White.copy(alpha = .7f), fontSize = appleTVSp(16f))
                        } else {
                            val selectedEpisode = targetEpisode.coerceIn(0, detail.episodes.lastIndex)
                            Text("第 ${selectedEpisode + 1} 集 / 共 ${detail.episodes.size} 集",
                                color = Color.White, fontSize = appleTVSp(22f), fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(appleTVDp(14f)))
                            Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(appleTVDp(16f)), verticalAlignment = Alignment.CenterVertically) {
                                AppleTVStepButton(androidx.compose.material.icons.Icons.Outlined.FastRewind) {
                                    targetEpisode = (selectedEpisode - 10).coerceAtLeast(0)
                                }
                                AppleTVStepButton(androidx.compose.material.icons.Icons.Outlined.SkipPrevious) {
                                    targetEpisode = (selectedEpisode - 1).coerceAtLeast(0)
                                }
                                Text("${selectedEpisode + 1}", color = Color.White,
                                    fontSize = appleTVSp(40f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                                    modifier = Modifier.widthIn(min = appleTVDp(100f)))
                                AppleTVStepButton(androidx.compose.material.icons.Icons.Outlined.SkipNext) {
                                    targetEpisode = (selectedEpisode + 1).coerceAtMost(detail.episodes.lastIndex)
                                }
                                AppleTVStepButton(androidx.compose.material.icons.Icons.Outlined.FastForward) {
                                    targetEpisode = (selectedEpisode + 10).coerceAtMost(detail.episodes.lastIndex)
                                }
                                if (selectedEpisode == state.episodePosition) {
                                    Text("正在播放", color = AppleTVColors.secondary, fontSize = appleTVSp(16f), fontWeight = FontWeight.SemiBold)
                                } else {
                                    AppleTVControl("播放该集", icon = androidx.compose.material.icons.Icons.Outlined.PlayArrow,
                                        paddingH = 16.dp, paddingV = 8.dp) {
                                        model.playEpisode(selectedEpisode)
                                        showInfo = false
                                    }
                                }
                            }
                        }
                    }
                    "相关作品" -> {
                        if (state.related.isEmpty()) Text("暂无相关作品", color = Color.White.copy(alpha = .7f))
                        else LazyRow(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(appleTVDp(16f))) {
                            items(state.related, key = { it.series.id }) { work ->
                                val card = DramaCard(work.series.id, work.series.title, work.series.cover,
                                    work.episodeCount, work.series.description, work.series.badge)
                                var focused by remember(work.series.id) { mutableStateOf(false) }
                                Column(
                                    Modifier.width(appleTVDp(120f))
                                        .onFocusChanged { focused = it.isFocused }
                                        .graphicsLayer { scaleX = if (focused) 1.05f else 1f; scaleY = if (focused) 1.05f else 1f }
                                        .shadow(if (focused) appleTVDp(12f) else 0.dp, RoundedCornerShape(appleTVDp(10f)),
                                            ambientColor = AppleTVColors.focusShadow, spotColor = AppleTVColors.focusShadow)
                                        .clip(RoundedCornerShape(appleTVDp(10f)))
                                        .background(if (focused) AppleTVColors.focusedFill else Color.Transparent)
                                        .then(if (focused) Modifier.border(appleTVDp(2f), AppleTVColors.accent,
                                            RoundedCornerShape(appleTVDp(10f))) else Modifier)
                                        .alpha(if (!work.upcoming || accountState.sessionStatus == AccountSessionStatus.LOGGED_IN) 1f else .4f)
                                        .clickable(enabled = !work.upcoming || accountState.sessionStatus == AccountSessionStatus.LOGGED_IN) {
                                            if (work.upcoming) accountModel.reserve(work)
                                            else model.playDrama(card)
                                            showInfo = false
                                        }
                                        .focusable(enabled = !work.upcoming || accountState.sessionStatus == AccountSessionStatus.LOGGED_IN)
                                        .padding(appleTVDp(4f)),
                                    verticalArrangement = Arrangement.spacedBy(appleTVDp(6f)),
                                ) {
                                    Cover(work.series.cover, work.series.title,
                                        Modifier.width(appleTVDp(120f)).height(appleTVDp(180f)).clip(RoundedCornerShape(appleTVDp(10f))))
                                    Text(work.series.title, color = Color.White, fontSize = appleTVSp(12f), maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, modifier = Modifier.width(appleTVDp(120f)))
                                }
                            }
                        }
                    }
                    "倍速" -> Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(appleTVDp(12f))) {
                        PlaybackSpeeds.forEach { speed ->
                            AppleTVControl(playbackSpeedLabel(speed), selected = state.playbackSpeed == speed, paddingH = 20.dp, paddingV = 10.dp) {
                                model.setPlaybackSpeed(speed)
                            }
                        }
                    }
                    "清晰度" -> Row(Modifier.focusGroup(), horizontalArrangement = Arrangement.spacedBy(appleTVDp(12f))) {
                        PlaybackQualities.forEach { quality ->
                            AppleTVControl(
                                playbackQualityLabel(quality),
                                selected = state.playbackQuality == quality,
                                paddingH = 20.dp,
                                paddingV = 10.dp,
                            ) {
                                model.setPlaybackQuality(quality, playback.positionMs)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTVTime(ms: Long): String {
    val total = (ms.coerceAtLeast(0) / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
private fun AppleTVStepButton(icon: ImageVector, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(appleTVDp(6f))
    Box(
        Modifier.width(appleTVDp(32f)).height(appleTVDp(32f))
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer { val scale = if (focused) 1.05f else 1f; scaleX = scale; scaleY = scale }
            .shadow(if (focused) appleTVDp(12f) else 0.dp, shape,
                ambientColor = AppleTVColors.focusShadow, spotColor = AppleTVColors.focusShadow)
            .clip(shape)
            .background(AppleTVColors.normalFill)
            .then(if (focused) Modifier.border(appleTVDp(2f), AppleTVColors.accent, shape)
                else Modifier.border(appleTVDp(1f), Color.White.copy(alpha = .35f), shape))
            .clickable(onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.width(appleTVDp(22f)).height(appleTVDp(22f)),
            tint = Color.White)
    }
}
