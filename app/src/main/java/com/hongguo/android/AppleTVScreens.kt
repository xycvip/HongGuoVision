package com.hongguo.android

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusGroup
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hongguo.account.AccountSessionStatus
import com.hongguo.account.AccountState
import kotlinx.coroutines.flow.collectLatest

/**
 * Apple TV library shell. The composition follows the reference's fixed top navigation,
 * filter strip, one horizontal poster shelf and bottom status line.
 * Android business state remains owned by HongguoViewModel.
 */
@Composable
internal fun AppleTVLibraryScreen(
    state: HongguoUiState,
    model: HongguoViewModel,
    accountName: String? = null,
    accountStatus: String = "登录后查看账号片单",
    accountState: AccountState,
    accountModel: com.hongguo.account.AccountViewModel,
    updateState: UpdateUiState,
    updateModel: UpdateViewModel,
    onAccount: () -> Unit = { model.selectTab(AppTab.ACCOUNT) },
) {
    val homeFocus = androidx.compose.runtime.remember { FocusRequester() }
    val findFocus = androidx.compose.runtime.remember { FocusRequester() }
    val rankFocus = androidx.compose.runtime.remember { FocusRequester() }
    val mineFocus = androidx.compose.runtime.remember { FocusRequester() }
    val searchFocus = androidx.compose.runtime.remember { FocusRequester() }
    Column(Modifier.fillMaxSize().background(AppleTVColors.background).onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyUp && event.key == Key.Menu && state.tab != AppTab.HOME) {
            model.selectTab(AppTab.HOME)
            true
        } else false
    }) {
        Row(
            Modifier.fillMaxWidth().height(AppleTVLayout.height(AppleTVLayout.NAV_VH, androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp))
                .padding(horizontal = appleTVDp(64f), vertical = appleTVDp(20f))
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appleTVDp(48f)),
        ) {
            AppleTVTabButton("首页", Icons.Outlined.SmartDisplay, state.tab == AppTab.HOME,
                modifier = Modifier.focusRequester(homeFocus)) { model.selectTab(AppTab.HOME) }
            AppleTVTabButton("找剧", Icons.Outlined.GridView, state.tab == AppTab.CATEGORY,
                modifier = Modifier.focusRequester(findFocus)) { model.selectTab(AppTab.CATEGORY) }
            AppleTVTabButton("榜单", Icons.Outlined.Leaderboard, state.tab == AppTab.RANK,
                modifier = Modifier.focusRequester(rankFocus)) { model.openRank() }
            AppleTVTabButton("我的", Icons.Outlined.AccountCircle,
                state.tab in setOf(AppTab.MINE, AppTab.FAVORITES, AppTab.HISTORY, AppTab.ACCOUNT,
                    AppTab.SETTINGS, AppTab.THEME_STYLE, AppTab.PLAY_HISTORY, AppTab.LOCAL_FAVORITES),
                modifier = Modifier.focusRequester(mineFocus)) { model.selectTab(AppTab.MINE) }
            BasicTextField(
                value = state.query,
                onValueChange = model::updateQuery,
                modifier = Modifier.width(appleTVDp(480f)).height(appleTVDp(40f)).focusRequester(searchFocus),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = appleTVSp(16f)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { model.search() }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (state.query.isEmpty()) Text("搜索剧名", color = AppleTVColors.secondary, fontSize = appleTVSp(16f))
                        inner()
                    }
                },
            )
            Spacer(Modifier.weight(1f))
        }
        androidx.compose.material3.HorizontalDivider(color = Color.White.copy(alpha = .16f), thickness = appleTVDp(1f))
        when (state.tab) {
            AppTab.HOME -> AppleTVHome(state, model)
            AppTab.CATEGORY -> AppleTVDiscover(state, model)
            AppTab.RANK -> AppleTVRank(state, model)
            AppTab.SEARCH -> AppleTVSearch(state, model)
            AppTab.MINE, AppTab.FAVORITES, AppTab.HISTORY -> AppleTVMine(state, model, accountName, accountStatus, accountState, accountModel, onAccount)
            AppTab.ACCOUNT -> AccountScreen(accountModel, onBack = model::closeAccount, onOpenLibrary = model::openLibrary)
            AppTab.SETTINGS -> SystemSettingsScreen(state, model, updateState, updateModel,
                onTheme = { model.selectTab(AppTab.THEME_STYLE) }, onBack = model::closeSettings)
            AppTab.THEME_STYLE -> ThemeStyleScreen(state, model, model::closeThemeStyle)
            AppTab.PLAY_HISTORY -> PlayHistoryScreen(state, model, accountModel)
            AppTab.LOCAL_FAVORITES -> LocalFavoritesScreen(state, model)
            else -> AppleTVHome(state, model)
        }
    }
    LaunchedEffect(state.tab) {
        runCatching {
            when (state.tab) {
                AppTab.HOME -> homeFocus.requestFocus()
                AppTab.CATEGORY -> findFocus.requestFocus()
                AppTab.RANK -> rankFocus.requestFocus()
                AppTab.SEARCH -> searchFocus.requestFocus()
                AppTab.MINE, AppTab.FAVORITES, AppTab.HISTORY,
                AppTab.ACCOUNT, AppTab.SETTINGS, AppTab.THEME_STYLE,
                AppTab.PLAY_HISTORY, AppTab.LOCAL_FAVORITES -> mineFocus.requestFocus()
            }
        }
    }
}

@Composable
private fun AppleTVHome(state: HongguoUiState, model: HongguoViewModel) {
    val feeds = listOf(
        HomeFeed.GUESS to "推荐",
        HomeFeed.RECOMMEND to "真人剧",
        HomeFeed.COMIC to "漫剧",
    )
    AppleTVSection(
        canvasHeight = appleTVLibraryCanvasHeight(),
        status = state.home.error ?: if (state.home.loading) "正在加载…" else "共 ${state.home.items.size} 部",
        emptyText = "看看下一部好故事",
        emptyIcon = Icons.Outlined.PlayCircleOutline,
        filterSpacing = 16.dp,
        filters = {
            feeds.forEach { (feed, label) ->
                AppleTVControl(label, selected = state.homeFeed == feed) {
                    if (state.homeFeed != feed) model.selectHomeFeed(feed)
                }
            }
            AppleTVControl("换一批", icon = Icons.Outlined.Refresh, paddingH = 16.dp, paddingV = 8.dp,
                enabled = !state.home.loading && !state.home.loadingMore) { model.refreshHome() }
            AppleTVControl("开始刷剧", icon = Icons.Outlined.PlayArrow, paddingH = 16.dp, paddingV = 8.dp,
                enabled = !state.home.loading && !state.home.loadingMore) { model.startHomeTrial() }
        },
        cards = state.home.items,
        loading = state.home.loading && state.home.items.isEmpty(),
        onOpen = model::playDrama,
        more = if (state.home.hasMore) ({ model.loadMoreHome() }) else null,
    )
}

@Composable
private fun AppleTVDiscover(state: HongguoUiState, model: HongguoViewModel) {
    val genres = listOf("comic_series" to "漫剧", "short_play" to "真人剧", "ai_series" to "AI 剧")
    AppleTVSection(
        canvasHeight = appleTVLibraryCanvasHeight(),
        status = state.category.error ?: if (state.category.loading) "正在加载…" else "共 ${state.category.items.size} 部",
        emptyText = "暂无内容",
        emptyIcon = Icons.Outlined.VideoLibrary,
        filters = {
            Text("类型", color = AppleTVColors.secondary, fontSize = appleTVSp(14f))
            genres.forEach { (genre, label) ->
                AppleTVControl(label, selected = state.browseDraft.genre == genre) {
                    if (state.browseDraft.genre != genre) model.changeGenre(genre)
                }
            }
            state.filterGroups.firstOrNull { it.key == "theme" }?.let { topics ->
                if (topics.options.isNotEmpty()) {
                    Text("题材", color = AppleTVColors.secondary, fontSize = appleTVSp(14f))
                    AppleTVControl("全部", selected = state.browseDraft.options[topics.key].isNullOrBlank()) {
                        if (!state.browseDraft.options[topics.key].isNullOrBlank()) {
                            model.selectFilter(topics.key, "")
                            model.applyBrowse()
                        }
                    }
                    topics.options.forEach { option ->
                        AppleTVControl(option.label, selected = state.browseDraft.options[topics.key] == option.id) {
                            if (state.browseDraft.options[topics.key] != option.id) {
                                model.selectFilter(topics.key, option.id)
                                model.applyBrowse()
                            }
                        }
                    }
                }
            }
        },
        cards = state.category.items,
        loading = state.category.loading && state.category.items.isEmpty(),
        onOpen = model::playDrama,
        more = if (state.category.hasMore) ({ model.loadMoreBrowse() }) else null,
    )
}

@Composable
private fun AppleTVRank(state: HongguoUiState, model: HongguoViewModel) {
    AppleTVSection(
        canvasHeight = appleTVLibraryCanvasHeight(),
        status = state.rankError ?: if (state.rankLoading) "正在加载榜单…" else "共 ${state.rankEntries.size} 部",
        emptyText = "暂无榜单内容",
        emptyIcon = Icons.Outlined.Leaderboard,
        filters = {
            Text("热播榜", color = AppleTVColors.secondary, fontSize = appleTVSp(14f))
            listOf(
                com.hongguo.account.protocol.RankBoard.ALL,
                com.hongguo.account.protocol.RankBoard.COMIC,
                com.hongguo.account.protocol.RankBoard.REAL,
                com.hongguo.account.protocol.RankBoard.AI,
            ).forEach { board ->
                AppleTVControl(board.label, selected = state.rankBoard == board) {
                    if (state.rankBoard != board) model.selectRankBoard(board)
                }
            }
        },
        cards = state.rankEntries.map { it.toCard() },
        rankLabels = state.rankEntries.associate { entry ->
            entry.series.id to listOf(if (entry.rank > 0) "第 ${entry.rank} 名" else "", entry.heat)
                .filter { it.isNotBlank() }.joinToString(" · ")
        },
        loading = state.rankLoading && state.rankEntries.isEmpty(),
        onOpen = model::playDrama,
        more = null,
    )
}

@Composable
private fun AppleTVSearch(state: HongguoUiState, model: HongguoViewModel) {
    AppleTVSection(
        canvasHeight = appleTVLibraryCanvasHeight(),
        status = state.searchError ?: if (state.searching) "正在搜索…" else "搜索结果 ${state.results.size} 部",
        emptyText = if (state.searchedQuery.isBlank()) "点击顶部搜索按钮输入剧名" else "暂无搜索结果",
        emptyIcon = Icons.Outlined.Search,
        showFilters = false,
        filters = {},
        cards = state.results,
        loading = state.searching && state.results.isEmpty(),
        onOpen = model::playDrama,
        more = if (state.hasMore && !state.loadingMore) ({ model.loadMore() }) else null,
    )
}

@Composable
private fun AppleTVMine(
    state: HongguoUiState,
    model: HongguoViewModel,
    accountName: String?,
    accountStatus: String,
    accountState: AccountState,
    accountModel: com.hongguo.account.AccountViewModel,
    onAccount: () -> Unit,
) {
    var category by rememberSaveable { mutableStateOf("收藏") }
    val cloud = accountState.sessionStatus == AccountSessionStatus.LOGGED_IN
    val items = when (category) {
        "历史" -> if (cloud) accountState.history else state.history
        "预约" -> if (cloud) accountState.reservations else emptyList()
        else -> if (cloud) accountState.favorites else state.favorites
    }
    val qrFocus = androidx.compose.runtime.remember { FocusRequester() }
    val loginFocus = androidx.compose.runtime.remember { FocusRequester() }
    val accountFocus = androidx.compose.runtime.remember { FocusRequester() }
    val qrBitmap = remember(accountState.qrBytes) {
        accountState.qrBytes?.let { bytes -> runCatching { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }.getOrNull() }
    }
    BackHandler(enabled = qrBitmap != null) { accountModel.cancelLogin() }
    LaunchedEffect(qrBitmap, cloud) {
        if (qrBitmap != null) runCatching { qrFocus.requestFocus() }
        else if (!cloud) runCatching { loginFocus.requestFocus() }
        else runCatching { accountFocus.requestFocus() }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().then(
            if (qrBitmap != null) Modifier.focusProperties { canFocus = false } else Modifier
        )) {
        AppleTVSection(
            canvasHeight = appleTVLibraryCanvasHeight(),
            status = accountState.message?.takeIf { it.isNotBlank() } ?: accountStatus,
            emptyText = when (category) { "历史" -> "暂无观看记录"; "预约" -> "暂无已上线预约"; else -> "暂无收藏" },
            filters = {
                AppleTVControl("收藏", selected = category == "收藏") { category = "收藏" }
                AppleTVControl("历史", selected = category == "历史") { category = "历史" }
                AppleTVControl("预约", selected = category == "预约") { category = "预约" }
                if (cloud) {
                    AppleTVControl(accountName ?: "红果用户", modifier = Modifier.focusRequester(accountFocus), onClick = onAccount)
                    AppleTVControl("登出", enabled = !accountState.busy, onClick = accountModel::logout)
                } else {
                    AppleTVControl("扫码登录", enabled = !accountState.busy, modifier = Modifier.focusRequester(loginFocus)) {
                        accountModel.startQrLogin()
                    }
                }
            },
            cards = items.map { it.card },
            loading = false,
            onOpen = { card ->
                val entry = items.firstOrNull { it.card.id == card.id }
                if (entry != null) model.openLibrary(entry) else model.playDrama(card)
            },
            more = null,
        )
        }
        if (qrBitmap != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .97f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(appleTVDp(18f))) {
                    Text("扫码登录", color = Color.White, fontSize = appleTVSp(30f), fontWeight = FontWeight.Bold)
                    Image(qrBitmap, contentDescription = "红果账号登录二维码", modifier = Modifier.width(appleTVDp(480f)).height(appleTVDp(480f)))
                    Text(accountState.message?.takeIf { it.isNotBlank() } ?: "请使用手机红果扫描二维码", color = Color.White.copy(alpha = .8f), fontSize = appleTVSp(16f))
                    if (accountState.qrRemainingSeconds > 0) {
                        Text("二维码剩余 ${accountState.qrRemainingSeconds} 秒", color = AppleTVColors.secondary, fontSize = appleTVSp(13f))
                    }
                    AppleTVControl("取消登录", modifier = Modifier.focusRequester(qrFocus)) { accountModel.cancelLogin() }
                }
            }
        }
    }
}

@Composable
private fun AppleTVSection(
    canvasHeight: androidx.compose.ui.unit.Dp,
    status: String,
    emptyText: String = "暂无内容",
    emptyIcon: ImageVector = Icons.Outlined.VideoLibrary,
    filterSpacing: androidx.compose.ui.unit.Dp = 12.dp,
    filters: @Composable RowScope.() -> Unit,
    cards: List<DramaCard>,
    loading: Boolean,
    rankLabels: Map<String, String> = emptyMap(),
    showFilters: Boolean = true,
    onOpen: (DramaCard) -> Unit,
    more: (() -> Unit)?,
) {
    val filterHeight = AppleTVLayout.height(AppleTVLayout.FILTER_VH, canvasHeight)
    val cardHeight = AppleTVLayout.height(AppleTVLayout.CARD_VH, canvasHeight)
    val listState = rememberLazyListState()
    LaunchedEffect(listState, cards.size, more != null) {
        if (more == null || cards.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collectLatest { lastVisible ->
                if (lastVisible >= cards.size - 2) more()
            }
    }
    Column(
        Modifier.fillMaxSize().padding(top = AppleTVLayout.height(AppleTVLayout.GAP_VH, canvasHeight)),
        verticalArrangement = Arrangement.spacedBy(AppleTVLayout.height(AppleTVLayout.GAP_VH, canvasHeight)),
    ) {
        if (showFilters) Row(
            Modifier.fillMaxWidth().height(filterHeight).padding(horizontal = appleTVDp(64f))
                .horizontalScroll(rememberScrollState()).focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(appleTVDp(filterSpacing.value)),
            content = filters,
        )
        Box(Modifier.fillMaxWidth().height(cardHeight), contentAlignment = Alignment.CenterStart) {
            when {
                loading -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(appleTVDp(16f)),
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(status, color = AppleTVColors.secondary, fontSize = appleTVSp(16f))
                }
                cards.isEmpty() -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(appleTVDp(16f)),
                ) {
                    Icon(emptyIcon, contentDescription = null, modifier = Modifier.width(appleTVDp(52f)).height(appleTVDp(52f)),
                        tint = AppleTVColors.accent)
                    Text(emptyText, color = Color.White, fontSize = appleTVSp(22f), fontWeight = FontWeight.Bold)
                }
                else -> LazyRow(
                    state = listState,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = appleTVDp(64f), vertical = appleTVDp(32f)),
                    horizontalArrangement = Arrangement.spacedBy(appleTVDp(24f)),
                    verticalAlignment = Alignment.Top,
                ) {
                    items(cards, key = { it.id }) { card ->
                        AppleTVPosterCard(card, subtitle = if (card.episodeCount > 0) "全 ${card.episodeCount} 集" else "",
                            rankText = rankLabels[card.id].orEmpty(), onClick = { onOpen(card) })
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(AppleTVLayout.height(AppleTVLayout.STATUS_VH, canvasHeight)),
            contentAlignment = Alignment.CenterStart) {
            Text(status, Modifier.fillMaxWidth().padding(horizontal = appleTVDp(64f)).height(appleTVDp(20f)),
                color = AppleTVColors.secondary, fontSize = appleTVSp(12f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
