package skayrux.shootout2d

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch

private fun formatLeaderScore(value: Number): String {
    return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(value.toLong())
}

@Composable
private fun PlayerPlateStyle(
    id: String,
    name: String,
    status: String,
    modifier: Modifier = Modifier
) {
    val isOnline = status == "Online" || status.startsWith("Online") || status == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba)
    val profile = sys_menu_socket_repository.getProfileById(id) ?: sys_menu_socket_repository.getProfile(name).let {
        if (it.id != "N/A" && it.id != id) null else it
    }
    val isBanned = profile?.banStatus == 1

    // отображение аватарки и ника игрока
    val displayNick = AllowedChars.sanitize(name, 16).ifBlank { name }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            userId = id,
            userName = displayNick,
            sizeDp = 34,
            fontSizeSp = 18,
            shape = RoundedCornerShape(4.dp),
            isBanned = isBanned
        )
        Column(
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            PlayerNameWithBan(
                name = displayNick,
                isBanned = isBanned,
                baseColor = AppColors.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (status.isNotEmpty()) {
                Text(
                    text = status,
                    color = if (isOnline) AppColors.OnlineGreen else AppColors.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// глобальный кэш списка лидеров вне жизненного цикла экрана
object LeadersCache {
    private val cache = mutableMapOf<String, Pair<Long, List<LeaderPlayer>>>()
    private const val TTL_MS = 25000L

    fun get(type: String): List<LeaderPlayer>? {
        val entry = cache[type] ?: return null
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - entry.first < TTL_MS && entry.second.isNotEmpty()) {
            return entry.second
        }
        return null
    }

    fun put(type: String, list: List<LeaderPlayer>) {
        cache[type] = Pair(android.os.SystemClock.elapsedRealtime(), list)
    }

    fun clear() {
        cache.clear()
    }
}

object MenuLeadersState {
    var isProfileDialogOpen by mutableStateOf(false)
}

@Composable
fun leaders_menu(
    onBack: () -> Unit
) {
    var activeTab by remember { mutableStateOf("today") }
    var leadersList by remember { mutableStateOf(LeadersCache.get(activeTab) ?: emptyList()) }
    var isLoading by remember { mutableStateOf(leadersList.isEmpty()) }
    val coroutineScope = rememberCoroutineScope()

    var showProfileDialog by remember { mutableStateOf(false) }
    var targetPlayerId by remember { mutableStateOf("") }
    var targetPlayerName by remember { mutableStateOf("") }
    var targetPlayerStatus by remember { mutableStateOf("") }
    var targetPlayerBanStatus by remember { mutableStateOf(0) }
    var targetPlayerBanReason by remember { mutableStateOf<String?>(null) }

    val myRank = remember(leadersList, isLoading) {
        if (!isLoading && sys_menu_socket_repository.currentPlayerId.isNotBlank()) {
            val idx = leadersList.indexOfFirst { it.id == sys_menu_socket_repository.currentPlayerId }
            if (idx != -1) idx + 1 else 0
        } else 0
    }

    LaunchedEffect(showProfileDialog) {
        MenuLeadersState.isProfileDialogOpen = showProfileDialog
    }

    // сброс скролла на первое место
    val lazyListState = remember(activeTab) { androidx.compose.foundation.lazy.LazyListState(0, 0) }

    // переключение вкладок без мерцания старых данных
    fun selectTab(tab: String) {
        if (activeTab == tab) return
        activeTab = tab
        val cached = LeadersCache.get(tab)
        if (cached != null) {
            leadersList = cached
            isLoading = false
        } else {
            leadersList = emptyList()
            isLoading = true
        }
    }

    // загрузка лидеров с кэшем 25 секунд
    LaunchedEffect(activeTab) {
        val cached = LeadersCache.get(activeTab)
        if (cached != null) {
            leadersList = cached
            isLoading = false
        } else {
            leadersList = emptyList()
            isLoading = true
            try {
                val fresh = sys_menu_socket_repository.fetchLeadersOnline(activeTab).take(100)
                leadersList = fresh
                if (fresh.isNotEmpty()) {
                    LeadersCache.put(activeTab, fresh)
                }
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LeadersTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back), false) { onBack() }
                LeadersTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.for_today), activeTab == "today") { selectTab("today") }
                LeadersTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.for_the_week), activeTab == "week") { selectTab("week") }
                LeadersTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.for_all_time), activeTab == "all") { selectTab("all") }
            }

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.PanelDark)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "№",
                                color = AppColors.TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.width(34.dp),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igrok_a1c5),
                                    color = AppColors.TextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                RankStatusTrapezoid(
                                    myRank = myRank,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(end = 12.dp)
                                        .height(24.dp)
                                )
                            }
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_aydi_4349),
                                color = AppColors.TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.7f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.score1),
                                color = AppColors.TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_deystvie_50a7),
                                color = AppColors.TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(0.6f),
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(AppColors.Overlay10)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (isLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.loading),
                                    color = AppColors.TextDarkGray,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (leadersList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_spisok_pust_f930),
                                    color = AppColors.EmptyText,
                                    fontSize = 18.sp
                                )
                            }
                        } else {
                            val top100List = remember(leadersList) { leadersList.take(100) }
                            @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                            CompositionLocalProvider(
                                androidx.compose.foundation.LocalOverscrollConfiguration provides null
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        state = lazyListState,
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        itemsIndexed(top100List, key = { index, player -> "${activeTab}_${player.id}_$index" }) { index, player ->
                                            val itemKey = "${activeTab}_${player.id}_$index"
                                            LeadersFadingItem(lazyListState, itemKey, index) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        val rankColor = when (index) {
                                                            0 -> Color(0xFFFFD700)
                                                            1 -> Color(0xFFC0C0C0)
                                                            2 -> Color(0xFFCD7F32)
                                                            else -> AppColors.Gray
                                                        }
                                                        Text(
                                                            text = "${index + 1}",
                                                            color = rankColor,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.width(34.dp),
                                                            textAlign = TextAlign.Center
                                                        )

                                                        Spacer(Modifier.width(8.dp))

                                                        val status = sys_menu_socket_repository.getUnifiedOnlineStatus(player.lastSeen)

                                                        PlayerPlateStyle(
                                                            id = player.id,
                                                            name = player.name,
                                                            status = status,
                                                            modifier = Modifier.weight(1.5f)
                                                        )

                                                        Text(
                                                            text = player.id,
                                                            color = AppColors.White.copy(alpha = 0.75f),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Normal,
                                                            modifier = Modifier.weight(0.7f),
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        Text(
                                                            text = formatLeaderScore(player.points),
                                                            color = AppColors.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.weight(0.8f),
                                                            textAlign = TextAlign.Center
                                                        )

                                                        Box(
                                                            modifier = Modifier.weight(0.6f),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .clickable(
                                                                        interactionSource = remember { MutableInteractionSource() },
                                                                        indication = null
                                                                    ) {
                                                                        val prof = sys_menu_socket_repository.getProfileById(player.id)
                                                                        targetPlayerId = player.id
                                                                        targetPlayerName = player.name
                                                                        targetPlayerStatus = status
                                                                        targetPlayerBanStatus = prof?.banStatus ?: 0
                                                                        targetPlayerBanReason = prof?.banReason
                                                                        showProfileDialog = true
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Outlined.Info,
                                                                    contentDescription = null,
                                                                    tint = AppColors.White.copy(alpha = 0.75f),
                                                                    modifier = Modifier.fillMaxSize()
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(1.dp)
                                                            .background(AppColors.Overlay10)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Top dark gradient overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.TopCenter)
                                            .height(8.dp)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors = listOf(
                                                        AppColors.PanelDark,
                                                        AppColors.PanelDark.copy(alpha = 0f)
                                                    )
                                                )
                                            )
                                    )

                                    // Bottom dark gradient overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .height(8.dp)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors = listOf(
                                                        AppColors.PanelDark.copy(alpha = 0f),
                                                        AppColors.PanelDark
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        FriendInfoDialog(
            playerId = targetPlayerId,
            playerNameFallback = targetPlayerName,
            currentUserName = sys_menu_socket_repository.currentPlayerName,
            initialStatus = targetPlayerStatus,
            initialBanStatus = targetPlayerBanStatus,
            initialBanReason = targetPlayerBanReason,
            onDismiss = { showProfileDialog = false }
        )
    }
}

@Composable
fun LeadersTabButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) AppColors.TabActive else AppColors.PanelDark
    val borderColor = if (isActive) AppColors.BorderActive else AppColors.Border
    val textColor = AppColors.White

    Box(
        modifier = Modifier
            .width(110.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            maxLines = 1
        )
    }
}

// оптимизированный расчет прозрачности элементов лидеров
@Composable
fun LeadersFadingItem(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    key: Any,
    index: Int,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.graphicsLayer {
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val itemInfo = if (visibleItems.isNotEmpty()) {
                val candidateIdx = index - visibleItems[0].index
                if (candidateIdx in visibleItems.indices && visibleItems[candidateIdx].key == key) {
                    visibleItems[candidateIdx]
                } else {
                    visibleItems.find { it.key == key }
                }
            } else null

            this.alpha = if (itemInfo != null) {
                val itemTop = itemInfo.offset
                val itemBottom = itemInfo.offset + itemInfo.size
                val viewportTop = 0
                val viewportBottom = lazyListState.layoutInfo.viewportEndOffset

                val visibleSize = (minOf(itemBottom, viewportBottom) - maxOf(itemTop, viewportTop)).coerceAtLeast(0)
                val visibilityFactor = if (itemInfo.size > 0) visibleSize.toFloat() / itemInfo.size.toFloat() else 1f

                if (visibilityFactor < 0.95f) {
                    (visibilityFactor / 0.95f).coerceIn(0f, 1f)
                } else {
                    1f
                }
            } else {
                1f
            }
        }
    ) {
        content()
    }
}

@Composable
fun RankStatusTrapezoid(myRank: Int, modifier: Modifier = Modifier) {
    val isRussian = AppLanguageManager.currentLanguage == "ru"
    val isInTop = myRank in 1..100
    val text = if (isInTop) {
        if (isRussian) "Вы на $myRank месте" else "You are in place $myRank"
    } else ""

    Box(
        modifier = modifier
            .width(135.dp)
            .drawBehind {
                val width = size.width
                val height = size.height
                val slant = 14.dp.toPx()

                clipRect(top = 0f, bottom = height, left = 0f, right = width) {
                    val extendY = 4.dp.toPx()
                    val extendX = slant * (extendY / height)

                    // Left slanted border (wide at top y=0, narrow at bottom y=height)
                    drawLine(
                        color = AppColors.Border,
                        start = androidx.compose.ui.geometry.Offset(-extendX, -extendY),
                        end = androidx.compose.ui.geometry.Offset(slant, height),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Right slanted border (wide at top y=0, narrow at bottom y=height)
                    drawLine(
                        color = AppColors.Border,
                        start = androidx.compose.ui.geometry.Offset(width + extendX, -extendY),
                        end = androidx.compose.ui.geometry.Offset(width - slant, height),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Bottom horizontal border (narrow at bottom y=height)
                    drawLine(
                        color = AppColors.Border,
                        start = androidx.compose.ui.geometry.Offset(slant, height),
                        end = androidx.compose.ui.geometry.Offset(width - slant, height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        if (isInTop) {
            Text(
                text = text,
                color = AppColors.TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .width(60.dp)
                    .height(0.7.dp)
                    .background(AppColors.TextGray.copy(alpha = 0.7f), RoundedCornerShape(0.5.dp))
            )
        }
    }
}
