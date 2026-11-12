package com.hongguo.android

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hongguo.account.AccountSessionStatus
import com.hongguo.account.AccountViewModel

private val TvBackground = Color(0xFF090B10)
private val TvPanel = Color(0xFF151922)
private val TvPanelFocused = Color(0xFF252D3A)
private val TvText = Color(0xFFF5F7FA)
private val TvMuted = Color(0xFF9AA4B2)
private val TvAccent = Color(0xFFFF6A36)

/** A wide tablet stays on the phone UI; only Android TV / Leanback devices use this activity. */
internal fun Context.isTelevisionDevice(): Boolean {
    val mode = getSystemService(UiModeManager::class.java)?.currentModeType
    return mode == Configuration.UI_MODE_TYPE_TELEVISION ||
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
}

internal class TvActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            val model: HongguoViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            val accountModel: AccountViewModel = viewModel()
            val accountState by accountModel.state.collectAsStateWithLifecycle()
            val updateModel: UpdateViewModel = viewModel()
            val updateState by updateModel.state.collectAsStateWithLifecycle()
            val signedIn = accountState.sessionStatus == AccountSessionStatus.LOGGED_IN
            LaunchedEffect(signedIn) { model.onAccountSession(signedIn) }
            HongguoTheme(AppThemeStyle.IMMERSIVE, dark = true) {
                if (state.playerOpen && state.detail != null) {
                    TvPlayerScreen(state, model)
                } else {
                    TvMainScreen(state, model, accountModel, updateState, updateModel,
                        accountName = if (accountState.sessionStatus == AccountSessionStatus.WEBSITE_ONLY)
                            accountState.websiteProfile?.nickname ?: "官网登录已保存" else accountState.profile?.nickname,
                        accountAvatar = if (accountState.sessionStatus == AccountSessionStatus.WEBSITE_ONLY)
                            accountState.websiteProfile?.avatarUrl else accountState.profile?.avatarUrl,
                        accountStatus = when (accountState.sessionStatus) {
                            AccountSessionStatus.WEBSITE_ONLY -> "官网已登录，手机云账号未接通"
                            AccountSessionStatus.LOGGED_IN -> "查看账号收藏、历史与预约"
                            AccountSessionStatus.NEEDS_VERIFICATION -> "已保存账号，等待重新验证"
                            else -> "登录后查看手机片单"
                        })
                }
                UpdateDialogs(updateState, updateModel, this@TvActivity)
            }
        }
    }
}


@Composable
private fun TvPlayerScreen(state: HongguoUiState, model: HongguoViewModel) {
    val detail = state.detail ?: return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val selected = state.episodePosition.coerceIn(detail.episodes.indices)
    val episode = detail.episodes[selected]
    val playback = rememberVideoPlayback(state.video, state.resumePositionMs, model,
        enabled = true, playbackSpeed = state.playbackSpeed)
    var showEpisodes by rememberSaveable(detail.id) { mutableStateOf(false) }

    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> model.onPlayerForeground(true)
                Lifecycle.Event.ON_STOP -> model.onPlayerForeground(false)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) model.onPlayerForeground(true)
        onDispose {
            lifecycle.removeObserver(observer)
            model.onPlayerForeground(false)
        }
    }
    BackHandler { model.closePlayer() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        VideoSurface(playback, Modifier.fillMaxSize())
        if (playback.player == null || playback.isBuffering || !state.firstFrameRendered) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = TvAccent)
                Text(state.playbackError ?: state.playbackStatus.ifBlank { state.preparationStage.ifBlank { "正在准备播放" } },
                    color = Color.White, fontSize = 15.sp)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .94f))))
            .padding(horizontal = 34.dp, vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(detail.title + "  ·  第 " + episode.index + " 集", color = Color.White,
                fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Slider(
                value = if (playback.durationMs > 0) (playback.positionMs.toFloat() / playback.durationMs).coerceIn(0f, 1f) else 0f,
                onValueChange = { fraction ->
                    if (playback.durationMs > 0) playback.player?.seekTo((playback.durationMs * fraction).toLong())
                },
                enabled = playback.durationMs > 0,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TvAction("返回详情", onClick = model::closePlayer, secondary = true)
                TvAction("上一集", onClick = { model.playEpisode(selected - 1) }, enabled = selected > 0, secondary = true)
                TvAction(if (playback.isPlaying) "暂停" else "继续播放", onClick = {
                    playback.player?.let { player ->
                        if (player.isPlaying) player.pause() else player.play()
                    }
                })
                TvAction("下一集", onClick = { model.playEpisode(selected + 1) },
                    enabled = selected < detail.episodes.lastIndex, secondary = true)
                TvAction(if (showEpisodes) "收起选集" else "选集", onClick = { showEpisodes = !showEpisodes }, secondary = true)
                Spacer(Modifier.weight(1f))
                Text(if (playback.durationMs > 0) playerTime(playback.positionMs) + " / " + playerTime(playback.durationMs)
                    else state.playbackStatus, color = Color.White.copy(alpha = .75f), fontSize = 13.sp)
            }
            if (showEpisodes) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    itemsIndexed(detail.episodes) { index, item ->
                        TvAction("第 " + item.index + " 集", onClick = {
                            showEpisodes = false
                            model.playEpisode(index)
                        }, secondary = index == selected)
                    }
                }
            }
        }
    }
}

private fun playerTime(positionMs: Long): String {
    val seconds = positionMs.coerceAtLeast(0) / 1000
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun TvMainScreen(
    state: HongguoUiState,
    model: HongguoViewModel,
    accountModel: AccountViewModel,
    updateState: UpdateUiState,
    updateModel: UpdateViewModel,
    accountName: String?,
    accountAvatar: String?,
    accountStatus: String,
) {
    BackHandler(enabled = state.selectedCard != null || state.tab != AppTab.HOME) {
        when {
            state.selectedCard != null -> model.closeDetail()
            state.tab == AppTab.SEARCH -> model.closeSearch()
            state.tab == AppTab.RANK -> model.closeRank()
            state.tab in setOf(AppTab.HISTORY, AppTab.FAVORITES) -> model.closeLibrary()
            state.tab in setOf(AppTab.PLAY_HISTORY, AppTab.LOCAL_FAVORITES) -> model.closeMyLibrary()
            state.tab == AppTab.ACCOUNT -> model.closeAccount()
            state.tab == AppTab.SETTINGS -> model.closeSettings()
            state.tab == AppTab.THEME_STYLE -> model.closeThemeStyle()
            else -> model.selectTab(AppTab.HOME)
        }
    }
    Row(Modifier.fillMaxSize().background(TvBackground).padding(28.dp)) {
        TvSidebar(state.tab, onSelect = model::selectTab)
        Spacer(Modifier.width(30.dp))
        Box(Modifier.weight(1f).fillMaxHeight()) {
            when {
                state.selectedCard != null -> TvDetailContent(state, model)
                state.tab == AppTab.HOME -> TvHomeContent(state, model)
                state.tab == AppTab.CATEGORY -> TvCategoryContent(state, model)
                state.tab == AppTab.RANK -> TvRankContent(state, model)
                state.tab == AppTab.FAVORITES || state.tab == AppTab.HISTORY -> TvLibraryContent(state, model)
                state.tab == AppTab.SEARCH -> SearchScreen(state, model, trending = state.home.items.take(10))
                state.tab == AppTab.MINE -> MyScreen(state, model, updateState, updateModel,
                    accountName = accountName, accountAvatar = accountAvatar, accountStatus = accountStatus,
                    style = AppThemeStyle.IMMERSIVE)
                state.tab == AppTab.ACCOUNT -> AccountScreen(accountModel, onBack = model::closeAccount,
                    onOpenLibrary = model::openLibrary)
                state.tab == AppTab.SETTINGS -> SystemSettingsScreen(state, model, updateState, updateModel,
                    onTheme = { model.selectTab(AppTab.THEME_STYLE) }, onBack = model::closeSettings)
                state.tab == AppTab.THEME_STYLE -> ThemeStyleScreen(state, model, model::closeThemeStyle)
                state.tab == AppTab.PLAY_HISTORY -> PlayHistoryScreen(state, model, accountModel)
                state.tab == AppTab.LOCAL_FAVORITES -> LocalFavoritesScreen(state, model)
            }
        }
    }
}

@Composable
private fun TvSidebar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    val active = when (selected) {
        AppTab.ACCOUNT, AppTab.SETTINGS, AppTab.THEME_STYLE -> AppTab.MINE
        else -> selected
    }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { firstFocus.requestFocus() } }
    Column(Modifier.width(190.dp).fillMaxHeight().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("HONGGUO", color = TvAccent, fontSize = 15.sp, fontWeight = FontWeight.Black,
            letterSpacing = 2.sp, modifier = Modifier.padding(start = 18.dp, bottom = 20.dp))
        Text("红果剧场", color = TvText, fontSize = 25.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(start = 18.dp, bottom = 24.dp))
        listOf(
            AppTab.HOME to "首页",
            AppTab.CATEGORY to "剧场",
            AppTab.RANK to "排行榜",
            AppTab.FAVORITES to "追剧收藏",
            AppTab.HISTORY to "观看历史",
            AppTab.SEARCH to "搜索",
            AppTab.MINE to "我的",
        ).forEachIndexed { index, item ->
            val tab = item.first
            val label = item.second
            val selectedItem = active == tab
            var focused by remember { mutableStateOf(false) }
            Surface(onClick = { onSelect(tab) },
                color = when { focused -> TvPanelFocused; selectedItem -> TvPanel; else -> Color.Transparent },
                shape = RoundedCornerShape(13.dp),
                border = if (focused) BorderStroke(2.dp, TvAccent) else null,
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .then(if (index == 0) Modifier.focusRequester(firstFocus) else Modifier)
                    .onFocusChanged { focused = it.isFocused }) {
                Box(Modifier.padding(horizontal = 18.dp).fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    Text(label, color = if (selectedItem || focused) TvText else TvMuted,
                        fontSize = 17.sp, fontWeight = if (selectedItem || focused) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text("方向键选择  ·  确认进入  ·  返回上一级", color = TvMuted, fontSize = 11.sp,
            modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun TvHomeContent(state: HongguoUiState, model: HongguoViewModel) {
    val cards = state.home.items
    LazyColumn(Modifier.fillMaxSize().testTag("tv_home"),
        contentPadding = PaddingValues(bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("为你精选", color = TvText, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("发现好故事，坐下来慢慢看", color = TvMuted, fontSize = 15.sp)
                }
                TvAction("刷新", onClick = model::refreshHome)
            }
        }
        if (cards.isEmpty()) item {
            Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                if (state.home.loading || !state.home.loaded) CircularProgressIndicator(color = TvAccent)
                else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(state.home.error ?: "暂时没有推荐内容", color = TvText, fontSize = 17.sp)
                    TvAction("重新加载", onClick = model::refreshHome)
                }
            }
        }
        if (cards.isNotEmpty()) item {
            val featured = cards.first()
            Row(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(22.dp)).background(TvPanel).padding(22.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                PosterImage(featured.cover, Modifier.width(145.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("今日推荐", color = TvAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(featured.title, color = TvText, fontSize = 26.sp, fontWeight = FontWeight.Black,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (featured.episodeCount > 0) "全 " + featured.episodeCount + " 集" else "精彩短剧",
                        color = TvMuted, fontSize = 14.sp)
                    Text(featured.summary, color = TvMuted, fontSize = 14.sp, maxLines = 2,
                        overflow = TextOverflow.Ellipsis, lineHeight = 21.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TvAction("立即播放", onClick = { model.playDrama(featured) })
                        TvAction("查看详情", onClick = { model.openDrama(featured) }, secondary = true)
                    }
                }
            }
        }
        if (cards.isNotEmpty()) item { TvPosterRail("热门推荐", cards, onClick = model::openDrama) }
        if (state.history.isNotEmpty()) item {
            TvPosterRail("继续观看", state.history.map { it.card }, onClick = { card ->
                state.history.firstOrNull { it.card.id == card.id }?.let(model::openLibrary)
            }, subtitle = { card ->
                state.history.firstOrNull { it.card.id == card.id }?.let { "看到第 " + it.episodeIndex.coerceAtLeast(1) + " 集" } ?: ""
            })
        }
        if (state.favorites.isNotEmpty()) item {
            TvPosterRail("我的收藏", state.favorites.map { it.card }, onClick = { card ->
                state.favorites.firstOrNull { it.card.id == card.id }?.let(model::openLibrary)
            })
        }
        if (state.home.hasMore) item {
            TvAction(if (state.home.loadingMore) "正在加载…" else "加载更多", onClick = model::loadMoreHome)
        }
    }
}

@Composable
private fun TvPosterRail(
    title: String,
    cards: List<DramaCard>,
    onClick: (DramaCard) -> Unit,
    subtitle: (DramaCard) -> String = { if (it.episodeCount > 0) "全 " + it.episodeCount + " 集" else "短剧" },
) {
    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text(title, color = TvText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)) {
            items(cards, key = { it.id }) { card -> TvPosterCard(card, subtitle(card)) { onClick(card) } }
        }
    }
}

@Composable
private fun TvPosterCard(card: DramaCard, subtitle: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.055f else 1f, label = "tv_poster_scale")
    Column(Modifier.width(142.dp).graphicsLayer(scaleX = scale, scaleY = scale)
        .onFocusChanged { focused = it.isFocused }.clip(RoundedCornerShape(13.dp))
        .background(if (focused) TvPanelFocused else Color.Transparent)
        .clickable(onClick = onClick).border(BorderStroke(2.dp, if (focused) TvAccent else Color.Transparent), RoundedCornerShape(13.dp)).padding(5.dp).testTag("tv_card_" + card.id),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        PosterImage(card.cover, Modifier.fillMaxWidth().aspectRatio(.72f).clip(RoundedCornerShape(10.dp)))
        Text(card.title, color = if (focused) TvText else TvMuted, fontSize = 14.sp,
            fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 3.dp))
        Text(subtitle, color = TvMuted, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 3.dp))
    }
}

@Composable
private fun TvCategoryContent(state: HongguoUiState, model: HongguoViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text("剧场", color = TvText, fontSize = 30.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 18.dp))
        if (state.category.items.isEmpty() && (state.category.loading || !state.category.loaded)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TvAccent) }
        } else if (state.category.items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.category.error ?: "暂时没有剧集", color = TvMuted, fontSize = 17.sp)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                itemsIndexed(state.category.items, key = { _, card -> card.id }) { _, card ->
                    TvPosterCard(card, if (card.episodeCount > 0) "全 " + card.episodeCount + " 集" else "短剧") {
                        model.openDrama(card)
                    }
                }
                if (state.category.hasMore) item { TvAction("加载更多", onClick = model::loadMoreBrowse) }
            }
        }
    }
}

@Composable
private fun TvRankContent(state: HongguoUiState, model: HongguoViewModel) {
    LaunchedEffect(Unit) { model.openRank() }
    Column(Modifier.fillMaxSize()) {
        Text("排行榜", color = TvText, fontSize = 30.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 18.dp))
        if (state.rankEntries.isEmpty() && state.rankLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TvAccent) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                items(state.rankEntries, key = { it.series.id }) { entry ->
                    val card = entry.toCard()
                    var focused by remember { mutableStateOf(false) }
                    Row(Modifier.fillMaxWidth().height(116.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (focused) TvPanelFocused else TvPanel).clickable { model.openDrama(card) }
                        .onFocusChanged { focused = it.isFocused }.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.rank.toString(), color = TvAccent, fontSize = 23.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.width(38.dp))
                        PosterImage(card.cover, Modifier.width(70.dp).fillMaxHeight().clip(RoundedCornerShape(8.dp)))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(card.title, color = TvText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(entry.heat, color = TvAccent, fontSize = 13.sp)
                            Text(card.summary, color = TvMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (state.rankError != null) item { TvAction("重试", onClick = model::retryRank) }
                else if (state.rankPage < state.rankTotalPages) item { TvAction("下一页", onClick = model::loadMoreRank) }
            }
        }
    }
}

@Composable
private fun TvLibraryContent(state: HongguoUiState, model: HongguoViewModel) {
    val history = state.tab == AppTab.HISTORY
    val entries = if (history) state.history else state.favorites
    Column(Modifier.fillMaxSize()) {
        Text(if (history) "观看历史" else "追剧收藏", color = TvText, fontSize = 30.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 18.dp))
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (history) "播放过的短剧会出现在这里" else "收藏的短剧会出现在这里", color = TvMuted, fontSize = 17.sp)
            }
        } else {
            LazyVerticalGrid(columns = GridCells.Adaptive(150.dp), modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)) {
                items(entries.size) { index ->
                    val entry = entries[index]
                    TvPosterCard(entry.card, if (history) "看到第 " + entry.episodeIndex.coerceAtLeast(1) + " 集" else "已收藏") {
                        model.openLibrary(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun TvDetailContent(state: HongguoUiState, model: HongguoViewModel) {
    val card = state.selectedCard ?: return
    val detail = state.detail
    val playRequester = remember(card.id) { FocusRequester() }
    LaunchedEffect(card.id, state.loadingDetail) {
        if (!state.loadingDetail) runCatching { playRequester.requestFocus() }
    }
    Column(Modifier.fillMaxSize().padding(end = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            PosterImage(detail?.cover ?: card.cover, Modifier.width(170.dp).height(230.dp).clip(RoundedCornerShape(16.dp)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(detail?.title ?: card.title, color = TvText, fontSize = 30.sp, fontWeight = FontWeight.Black,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(detail?.let { "共 " + it.episodes.size + " 集" } ?: if (state.loadingDetail) "正在读取剧集…" else "剧集信息暂不可用",
                    color = TvMuted, fontSize = 15.sp)
                Text(detail?.summary ?: card.summary, color = TvMuted, fontSize = 14.sp, maxLines = 4,
                    overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
                if (state.detailError != null) Text(state.detailError, color = TvAccent, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TvAction(if (state.loadingDetail) "读取中…" else "立即播放",
                        onClick = model::openPlayer, modifier = Modifier.focusRequester(playRequester),
                        enabled = detail?.episodes?.isNotEmpty() == true)
                    TvAction("返回", onClick = model::closeDetail, secondary = true)
                }
            }
        }
        if (detail != null) {
            Text("选集", color = TvText, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 22.dp, bottom = 12.dp))
            LazyVerticalGrid(columns = GridCells.Adaptive(76.dp), modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(detail.episodes) { index, episode ->
                    TvAction("第 " + episode.index + " 集", onClick = { model.playEpisode(index) },
                        secondary = state.episodePosition == index)
                }
            }
        }
    }
}

@Composable
private fun TvAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(12.dp),
        color = when { focused -> TvPanelFocused; secondary -> TvPanel; else -> TvAccent },
        border = if (focused) BorderStroke(2.dp, Color.White) else null,
        modifier = modifier.heightIn(min = 48.dp).widthIn(min = 100.dp)
            .onFocusChanged { focused = it.isFocused }.focusable()) {
        Box(Modifier.padding(horizontal = 18.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (secondary || focused) TvText else Color.White, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
