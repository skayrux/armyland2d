package skayrux.shootout2d

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun home_menu(
    playerName: String,
    serebroCoins: Double,
    zolotoCoins: Double,
    userAvatar: String?,
    onProfileClick: () -> Unit,
    onPlayClick: () -> Unit,
    onAddFriendClick: () -> Unit,
    onLeadersClick: () -> Unit,
    onNewsClick: () -> Unit,
    onNavigateTarget: (String) -> Unit = {}
) {
    val context = LocalContext.current
    // фоновая синхронизация данных и баннеров
    LaunchedEffect(Unit) {
        launch { sys_menu_socket_repository.syncFullAccountData() }
        banners_cache_manager.startBackgroundSync(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-18).dp, y = (-5).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onProfileClick() }
                            )
                        }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pForAvatar = sys_menu_socket_repository.getProfile(playerName)
                    val resolvedId = pForAvatar?.id ?: if (playerName == sys_menu_socket_repository.currentPlayerName) sys_menu_socket_repository.currentPlayerId else ""
                    val isBanned = (pForAvatar?.banStatus == 1) || (playerName == sys_menu_socket_repository.currentPlayerName && sys_menu_socket_repository.currentPlayerBanStatus == 1)
                    UserAvatar(
                        userId = resolvedId,
                        userName = playerName,
                        sizeDp = 48,
                        fontSizeSp = 26,
                        shape = RoundedCornerShape(4.dp),
                        isBanned = isBanned
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        val p = sys_menu_socket_repository.getProfile(playerName)
                        val displayId = if (!sys_menu_socket_repository.isServerOnline) {
                            "N/A"
                        } else if (playerName == sys_menu_socket_repository.currentPlayerName && sys_menu_socket_repository.currentPlayerId.isNotBlank()) {
                            sys_menu_socket_repository.currentPlayerId
                        } else {
                            p?.id ?: "N/A"
                        }
                        Text(
                            text = "ID: $displayId",
                            color = AppColors.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                        PlayerNameWithBan(
                            name = playerName,
                            isBanned = isBanned,
                            baseColor = AppColors.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val isOnline = p.status == "Online" || p.status.startsWith("Online")
                        Text(
                            text = p.status,
                            color = if (isOnline) AppColors.OnlineGreen else AppColors.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                CoinsBalanceContainer(modifier = Modifier.align(Alignment.TopEnd))
            }
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .width(135.dp)
                    .layout { measurable, constraints ->
                        val topExtension = 10.dp.roundToPx()
                        val bottomExtension = 3.dp.roundToPx()
                        val placeable = measurable.measure(constraints.copy(
                            minHeight = constraints.maxHeight + topExtension + bottomExtension,
                            maxHeight = constraints.maxHeight + topExtension + bottomExtension
                        ))
                        layout(placeable.width, placeable.height) {
                            placeable.placeRelative(0, -topExtension)
                        }
                    }
                    .offset(x = (-10).dp)
                    .background(AppColors.SurfaceLight, RoundedCornerShape(6.dp))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
            ) {
                val friendsDetailsRaw = sys_menu_socket_repository.friendsDetails
                val totalFriendsCount = friendsDetailsRaw.size
                val onlineFriendsCount = friendsDetailsRaw.count { friend ->
                    val statusText = sys_menu_socket_repository.getUnifiedOnlineStatus(friend.lastSeen)
                    statusText == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba) || statusText == "Online"
                }
                val onlineColor = if (onlineFriendsCount > 0) AppColors.OnlineGreen else AppColors.TextMuted

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            drawLine(AppColors.Border, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth)
                        }
                        .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.total_friends, totalFriendsCount),
                        color = AppColors.TextMuted,
                        fontSize = 8.sp,
                        maxLines = 1
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(onlineColor, CircleShape)
                        )
                        Text(
                            text = "$onlineFriendsCount",
                            color = onlineColor,
                            fontSize = 8.sp,
                            maxLines = 1
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 2.dp)
                ) {
                    val friendsData = friendsDetailsRaw.map { friend ->
                        val statusText = sys_menu_socket_repository.getUnifiedOnlineStatus(friend.lastSeen)
                        val isOnline = statusText == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba) || statusText == "Online"
                        val statusColor = if (isOnline) AppColors.OnlineGreen else AppColors.TextMuted
                        val sortWeight = if (isOnline) 0 else if (statusText.contains(":")) 1 else 2

                        object {
                            val id = friend.id
                            val name = friend.name
                            val status = statusText
                            val color = statusColor
                            val sort = sortWeight
                            val banStatus = friend.banStatus
                        }
                    }.sortedBy { it.sort }

                    if (friendsData.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_druzey_9509), color = AppColors.TextDarkGray, fontSize = 8.5.sp)
                        }
                    } else {
                        friendsData.forEach { friend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // аватарка друга в списке
                                UserAvatar(
                                    userId = friend.id,
                                    userName = friend.name,
                                    sizeDp = 28,
                                    fontSizeSp = 13,
                                    shape = RoundedCornerShape(4.dp),
                                    isBanned = friend.banStatus == 1
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = "ID: ${friend.id}",
                                        color = AppColors.TextDarkGray,
                                        fontSize = 7.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    PlayerNameWithBan(
                                        name = friend.name,
                                        isBanned = friend.banStatus == 1,
                                        baseColor = AppColors.GrayE0,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = friend.status,
                                        color = friend.color,
                                        fontSize = 7.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            val strokeWidth = 1.dp.toPx()
                            drawLine(AppColors.Border, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth)
                        }
                        .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                        .clickable { onAddFriendClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = AppColors.TextDarkGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.game_menu_add_friend), color = AppColors.TextDarkGray, fontSize = 8.5.sp)
                }
            }
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-45).dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HomeBannerCarousel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1280f / 500f),
                        onNavigateTarget = onNavigateTarget
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.SurfaceLight)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
                                .clickable { onNewsClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Newspaper,
                                    contentDescription = null,
                                    tint = Color(0xFFC0C0C0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.news),
                                    color = Color(0xFFC0C0C0),
                                    fontSize = 8.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AppColors.SurfaceLight)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
                                .clickable { onLeadersClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Leaderboard,
                                    contentDescription = null,
                                    tint = Color(0xFFC0C0C0),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.leaders),
                                    color = Color(0xFFC0C0C0),
                                    fontSize = 8.5.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.PlayBg)
                        .border(1.dp, AppColors.PlayBorder, RoundedCornerShape(4.dp))
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igrat_caps),
                        color = AppColors.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        UnifiedToast(
            message = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_akkaunt_byl_zablokirovan_b094),
            visible = sys_menu_socket_repository.currentPlayerBanStatus == 1,
            textColor = AppColors.Error
        )
    }
    }
}

@Composable
fun CoinsBalanceContainer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(30.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        CoinsBalanceBlock()
    }
}

@Composable
fun CoinsBalanceBlock(modifier: Modifier = Modifier) {
    val isOnlineServer = sys_menu_socket_repository.isServerOnline
    val serebroCoins = sys_menu_socket_repository.currentPlayerSerebro
    val zolotoCoins = sys_menu_socket_repository.currentPlayerZoloto
    val serebroStr = if (isOnlineServer) {
        serebroCoins.toInt().toString()
    } else {
        "N/A"
    }
    val zolotoStr = if (isOnlineServer) {
        "%.2f".format(java.util.Locale.US, zolotoCoins).replace('.', ',')
    } else {
        "N/A"
    }

    val flexTextStyle = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )

    Row(
        modifier = modifier.requiredHeight(28.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .requiredHeight(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF252931))
                .border(1.dp, Color(0xFF353942), RoundedCornerShape(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF393D45))
                    .requiredHeight(28.dp)
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "C",
                    color = SilverColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    style = flexTextStyle
                )
                Text(
                    text = serebroStr,
                    color = SilverColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    style = flexTextStyle
                )
            }
            Box(
                modifier = Modifier
                    .requiredHeight(28.dp)
                    .width(1.dp)
                    .background(Color(0xFF353942))
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFF252931))
                    .requiredHeight(28.dp)
                    .width(20.dp)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = Color(0xFF9EA3AE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    style = flexTextStyle
                )
            }
        }

        Row(
            modifier = Modifier
                .requiredHeight(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF3C2800))
                .border(1.dp, Color(0xFF4A3100), RoundedCornerShape(4.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .background(Color(0xFF664500))
                    .requiredHeight(28.dp)
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "G",
                    color = Color(0xFFE1B34B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    style = flexTextStyle
                )
                Text(
                    text = zolotoStr,
                    color = Color(0xFFE1B34B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    style = flexTextStyle
                )
            }
            Box(
                modifier = Modifier
                    .requiredHeight(28.dp)
                    .width(1.dp)
                    .background(Color(0xFF4A3100))
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFF3C2800))
                    .requiredHeight(28.dp)
                    .width(20.dp)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    color = Color(0xFFE1B34B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    style = flexTextStyle
                )
            }
        }
    }
}

@Composable
fun HomeBannerCarousel(
    modifier: Modifier = Modifier,
    onNavigateTarget: (String) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // локальная загрузка баннеров с диска
    banners_cache_manager.loadFromDiskInitial(context)

    // запуск фонового обновления баннеров
    LaunchedEffect(Unit) {
        banners_cache_manager.startBackgroundSync(context)
    }

    val banners = banners_cache_manager.bannersList

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.SurfaceLight)
            .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (banners.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                BanIcon(
                    modifier = Modifier.size(24.dp),
                    color = AppColors.DarkGray4A
                )
            }
        } else {
            val isMulti = banners.size > 1
            val virtualCount = if (isMulti) banners.size * 10000 else 1
            val initialPage = if (isMulti) (virtualCount / 2) - ((virtualCount / 2) % banners.size) else 0
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { virtualCount }
            )

            var showIndicators by remember { mutableStateOf(false) }
            val indicatorsAlpha by animateFloatAsState(
                targetValue = if (showIndicators) 1f else 0f,
                animationSpec = tween(durationMillis = 500),
                label = "indicatorsAlpha"
            )

            LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
                if (isMulti) {
                    showIndicators = true
                    if (!pagerState.isScrollInProgress) {
                        delay(1200L)
                        showIndicators = false
                    }
                }
            }

            LaunchedEffect(pagerState, banners) {
                if (isMulti) {
                    while (true) {
                        delay(10000L)
                        if (!pagerState.isScrollInProgress) {
                            try {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val actualIndex = if (isMulti) page % banners.size else 0
                val banner = banners.getOrNull(actualIndex)
                if (banner != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                val target = banner.perehodnikMenu.trim()
                                if (target.isNotEmpty()) {
                                    onNavigateTarget(target)
                                } else if (banner.ssylka.isNotBlank()) {
                                    try {
                                        uriHandler.openUri(banner.ssylka)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(banner.ssylka)).apply {
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (ex: Exception) {}
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val file = banner.localFile
                        if (file != null && file.exists() && file.length() > 0L) {
                            if (banner.isVideo) {
                                AndroidView(
                                    factory = { ctx ->
                                        android.widget.VideoView(ctx).apply {
                                            setVideoPath(file.absolutePath)
                                            setOnPreparedListener { mp ->
                                                mp.setVolume(0f, 0f)
                                                mp.isLooping = true
                                                start()
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val bmp = remember(file.absolutePath) {
                                    banners_cache_manager.bitmapCache.getOrPut(file.absolutePath) {
                                        try {
                                            android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                        } catch (e: Exception) {
                                            null
                                        } ?: return@getOrPut null as android.graphics.Bitmap
                                    }
                                }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    BanIcon(
                                        modifier = Modifier.size(24.dp),
                                        color = AppColors.DarkGray4A
                                    )
                                }
                            }
                        } else {
                            BanIcon(
                                modifier = Modifier.size(24.dp),
                                color = AppColors.DarkGray4A
                            )
                        }
                    }
                }
            }

            if (isMulti && indicatorsAlpha > 0.01f) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .graphicsLayer { alpha = indicatorsAlpha },
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeIndex = pagerState.currentPage % banners.size
                    repeat(banners.size) { index ->
                        val isSelected = activeIndex == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 3.5.dp else 2.5.dp)
                                .background(
                                    if (isSelected) AppColors.White else AppColors.White.copy(alpha = 0.35f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

