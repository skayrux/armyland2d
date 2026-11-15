package skayrux.shootout2d
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween


import androidx.compose.material.icons.filled.Block


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private class FriendsInfoBubbleShape : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val arrowWidth = with(density) { 5.dp.toPx() }
        val arrowHeight = with(density) { 7.dp.toPx() }
        val path = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = arrowWidth,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
            )
            moveTo(0f, size.height / 2f)
            lineTo(arrowWidth, size.height / 2f - arrowHeight / 2f)
            lineTo(arrowWidth, size.height / 2f + arrowHeight / 2f)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

private val reasonRuToEn = mapOf(
    "Нечестная игра" to "Unfair Play",
    "Использование читов" to "Use of Cheats",
    "Токсичность" to "Toxicity",
    "Использование багов" to "Bug Exploiting",
    "Мошенничество" to "Fraud",
    "Взлом аккаунта" to "Account Hacking",
    "Нарушение правил сообщества" to "Community Rules Violation",
    "Причина не указана" to "Reason not specified"
)

private val reasonEnToRu = mapOf(
    "Unfair Play" to "Нечестная игра",
    "Use of Cheats" to "Использование читов",
    "Toxicity" to "Токсичность",
    "Bug Exploiting" to "Использование багов",
    "Fraud" to "Мошенничество",
    "Account Hacking" to "Взлом аккаунта",
    "Community Rules Violation" to "Нарушение правил сообщества",
    "Reason not specified" to "Причина не указана"
)

private fun translateBanReason(rawReason: String, isRu: Boolean): String {
    return if (isRu) {
        reasonEnToRu[rawReason] ?: rawReason
    } else {
        reasonRuToEn[rawReason] ?: rawReason
    }
}

fun getPopupBanText(banReasonRaw: String?): String {
    val isRu = AppLanguageManager.currentLanguage == "ru" || java.util.Locale.getDefault().language == "ru"
    if (banReasonRaw == null || banReasonRaw.isBlank()) return skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_ne_ukazana_permanentno_f44f)
    val parts = banReasonRaw.split(":")
    val rawReason = parts.getOrNull(0) ?: skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_ne_ukazana_f75f)
    val expiresStr = parts.getOrNull(1) ?: "-1"
    val expiresMillis = expiresStr.toLongOrNull()
    // плашка запланированного удаления данных
    if (rawReason.equals("Удаление данных", ignoreCase = true) || rawReason.equals("Data deletion", ignoreCase = true)) {
        val shortDate = if (expiresMillis != null && expiresMillis > 0L) {
            try {
                java.text.SimpleDateFormat("dd.MM.yy", java.util.Locale.getDefault()).format(java.util.Date(expiresMillis))
            } catch (e: Exception) {
                ""
            }
        } else ""
        val prefix = if (isRu) "Удаление данных" else "Data deletion"
        return if (shortDate.isNotEmpty()) "$prefix $shortDate" else prefix
    }
    val reason = translateBanReason(rawReason, isRu)
    if (expiresStr == "permanent" || expiresStr == "0" || expiresStr == "-1" || (expiresMillis != null && expiresMillis <= 0L)) {
        return skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ban_permanent, reason)
    }
    if (expiresMillis == null) {
        return skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ban_permanent, reason)
    }
    val simpleDate = try {
        java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(expiresMillis))
    } catch (e: Exception) {
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_permanentno_7115)
    }
    return skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ban_until, reason, simpleDate)
}

fun formatBanExpiration(banReasonRaw: String?): Pair<String, String> {
    val isRu = AppLanguageManager.currentLanguage == "ru" || java.util.Locale.getDefault().language == "ru"
    if (banReasonRaw == null || banReasonRaw.isBlank()) return Pair(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_ne_ukazana_f75f), skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_permanentno_7115))
    val parts = banReasonRaw.split(":")
    val rawReason = parts.getOrNull(0) ?: skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_ne_ukazana_f75f)
    val expiresStr = parts.getOrNull(1) ?: "-1"
    val expiresMillis = expiresStr.toLongOrNull()
    if (rawReason.equals("Удаление данных", ignoreCase = true) || rawReason.equals("Data deletion", ignoreCase = true)) {
        val shortDate = if (expiresMillis != null && expiresMillis > 0L) {
            try {
                java.text.SimpleDateFormat("dd.MM.yy", java.util.Locale.getDefault()).format(java.util.Date(expiresMillis))
            } catch (e: Exception) {
                ""
            }
        } else ""
        return Pair(if (isRu) "Удаление данных" else "Data deletion", shortDate)
    }
    val reason = translateBanReason(rawReason, isRu)
    if (expiresStr == "permanent" || expiresStr == "0" || expiresStr == "-1" || (expiresMillis != null && expiresMillis <= 0L)) {
        return Pair(reason, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_permanentno_7115))
    }
    if (expiresMillis == null) {
        return Pair(reason, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_permanentno_7115))
    }
    val simpleDate = try {
        java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(expiresMillis))
    } catch (e: Exception) {
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_permanentno_7115)
    }
    return Pair(reason, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ban_until_short, simpleDate))
}

@Composable
fun AnimatedReasonButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.DialogButtonActive else AppColors.DialogButtonInactive,
        animationSpec = tween(durationMillis = 200)
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.White.copy(alpha = 0.85f) else AppColors.Border,
        animationSpec = tween(durationMillis = 200)
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.White else AppColors.TextMuted,
        animationSpec = tween(durationMillis = 200)
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .height(26.dp)
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AnimatedPresetDaysButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.DialogButtonActive else AppColors.DialogButtonInactive,
        animationSpec = tween(durationMillis = 200)
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.White.copy(alpha = 0.85f) else AppColors.Border,
        animationSpec = tween(durationMillis = 200)
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) AppColors.White else AppColors.TextMuted,
        animationSpec = tween(durationMillis = 200)
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1.0f,
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .height(26.dp)
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

// глобальный кэш профилей игроков вне жизненного цикла экрана (TTL 5 секунд)
object ProfileMemoryCache {
    private val cache = mutableMapOf<String, Pair<Long, PlayerProfile>>()
    private const val TTL_MS = 5000L

    fun get(idOrName: String): PlayerProfile? {
        if (idOrName.isBlank()) return null
        val entry = cache[idOrName] ?: return null
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - entry.first < TTL_MS) {
            return entry.second
        }
        return null
    }

    fun put(id: String, name: String, profile: PlayerProfile) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (id.isNotBlank()) cache[id] = Pair(now, profile)
        if (name.isNotBlank()) cache[name] = Pair(now, profile)
    }

    fun clear() {
        cache.clear()
    }
}

@Composable
fun FriendInfoDialog(
    playerId: String,
    playerNameFallback: String,
    currentUserName: String,
    initialStatus: String? = null,
    initialBanStatus: Int = 0,
    initialBanReason: String? = null,
    onDismiss: () -> Unit
) {
    var profile by remember(playerId, playerNameFallback) {
        val isOwn = (playerNameFallback.isNotEmpty() && playerNameFallback == sys_menu_socket_repository.currentPlayerName) ||
                (playerId.isNotEmpty() && playerId == sys_menu_socket_repository.currentPlayerId)
        mutableStateOf(
            if (isOwn) {
                sys_menu_socket_repository.getProfile(sys_menu_socket_repository.currentPlayerName)
            } else {
                val fromMem = ProfileMemoryCache.get(playerId) ?: ProfileMemoryCache.get(playerNameFallback)
                if (fromMem != null) {
                    fromMem
                } else {
                    val cached = if (playerId.isNotEmpty()) {
                        sys_menu_socket_repository.getProfileById(playerId)
                    } else null
                    val cachedByName = if (cached == null && playerNameFallback.isNotEmpty() && playerNameFallback != sys_menu_socket_repository.currentPlayerName) {
                        sys_menu_socket_repository.players[playerNameFallback]
                    } else null

                    val base = cached ?: cachedByName
                    if (base != null) {
                        base.copy(
                            status = initialStatus?.takeIf { it.isNotBlank() } ?: base.status,
                            banStatus = if (initialBanStatus != 0) initialBanStatus else base.banStatus,
                            banReason = initialBanReason ?: base.banReason
                        )
                    } else {
                        PlayerProfile(
                            id = playerId.ifBlank { "N/A" },
                            name = playerNameFallback.ifBlank { "" },
                            description = "",
                            status = initialStatus ?: "",
                            banStatus = initialBanStatus,
                            banReason = initialBanReason,
                            created = ""
                        )
                    }
                }
            }
        )
    }

    // разовая фоновая загрузка детального профиля
    LaunchedEffect(playerId, playerNameFallback) {
        val isOwn = (playerNameFallback.isNotEmpty() && playerNameFallback == sys_menu_socket_repository.currentPlayerName) ||
                (playerId.isNotEmpty() && playerId == sys_menu_socket_repository.currentPlayerId)
        if (isOwn) return@LaunchedEffect

        val cachedMem = ProfileMemoryCache.get(playerId) ?: ProfileMemoryCache.get(playerNameFallback)
        if (cachedMem != null && cachedMem.description.isNotBlank()) {
            profile = cachedMem
            return@LaunchedEffect
        }

        if (playerId.isNotEmpty()) {
            val loaded = sys_menu_socket_repository.getProfileByIdOnline(playerId)
            if (loaded != null) {
                profile = loaded
                sys_menu_socket_repository.players[loaded.name] = loaded
                ProfileMemoryCache.put(playerId, loaded.name, loaded)
            }
        } else if (playerNameFallback.isNotEmpty()) {
            val loaded = sys_menu_socket_repository.getProfileByNameOnline(playerNameFallback)
            if (loaded != null) {
                profile = loaded
                sys_menu_socket_repository.players[playerNameFallback] = loaded
                ProfileMemoryCache.put(loaded.id, playerNameFallback, loaded)
            }
        }
    }
    val playerName = profile.name
    val hash = playerName.hashCode().coerceAtLeast(0)
    
    fun formatFriendStat(value: Number): String {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(value.toLong())
    }

    val kills = profile.enemk
    val deaths = profile.death
    val zombies = profile.zomk.toFloat()
    val juggernauts = profile.jugk.toFloat()
    val ghosts = profile.ghostk.toFloat()
    val totalKills = kills + zombies + juggernauts + ghosts
    val kdText = if (deaths > 0f) String.format(java.util.Locale.US, "%.2f", totalKills / deaths) else String.format(java.util.Locale.US, "%.2f", totalKills)
    val killsText = formatFriendStat(kills)
    val deathsText = formatFriendStat(deaths)
    val pointsText = formatFriendStat(profile.score)
    val zombiesText = formatFriendStat(profile.zomk)
    val juggernautsText = formatFriendStat(profile.jugk)
    val bugsText = formatFriendStat(profile.bugk)
    val ghostsText = formatFriendStat(profile.ghostk)

    val isRu = AppLanguageManager.currentLanguage == "ru" || java.util.Locale.getDefault().language == "ru"
    val context = androidx.compose.ui.platform.LocalContext.current
    var isBanMode by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    var showBanPopup by remember { mutableStateOf(false) }
    var selectedReasonIndex by remember { mutableStateOf(0) }
    var selectedDaysPreset by remember { mutableStateOf<Int?>(1) }
    var customDaysInput by remember { mutableStateOf("") }
    var showConfirmBanDialog by remember { mutableStateOf(false) }
    var showConfirmUnbanDialog by remember { mutableStateOf(false) }
    var isActionInProgress by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        sys_menu_socket_repository.refreshProfileConfig()
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            kotlinx.coroutines.delay(200)
            pendingAction?.invoke()
        }
    }

    val animProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = ""
    )

    fun dismissWithAnimation(action: () -> Unit) {
        if (pendingAction != null) return
        pendingAction = action
        isVisible = false
    }

    FullscreenDialogOverlay(
        zOrder = 10,
        onDismissRequest = {
            dismissWithAnimation(onDismiss)
        },
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        blurRadius = 20
    ) {
        androidx.activity.compose.BackHandler(enabled = true) {
            dismissWithAnimation(onDismiss)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .effectZatemneniya(animProgress)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissWithAnimation(onDismiss) }
                ),
            contentAlignment = Alignment.Center
        ) {
            val scale = 0.95f + 0.05f * animProgress
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(0.8f)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        alpha = 1f * animProgress
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(AppColors.FriendCardBg, RoundedCornerShape(8.dp))
                    .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .drawBehindBottomBorderFriend()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1.3f),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                UserAvatar(
                                    userId = profile.id,
                                    userName = playerName,
                                    sizeDp = 68,
                                    fontSizeSp = 28,
                                    shape = RoundedCornerShape(8.dp),
                                    isBanned = profile.banStatus == 1
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nik_a298).trimEnd()} ", 
                                            color = AppColors.TextGray, 
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        val isBanned = profile.banStatus == 1

                                        Box(
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Text(
                                                text = playerName,
                                                color = if (isBanned) BanStyle.NameColor else AppColors.White,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isBanned) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box {
                                                BanIcon(
                                                    color = BanStyle.IconColor,
                                                    modifier = Modifier
                                                        .size(BanStyle.FriendsInfoIconSize)
                                                        .clickable(
                                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                            indication = null,
                                                            onClick = { showBanPopup = true }
                                                        )
                                                )
                                                if (showBanPopup) {
                                                    val density = androidx.compose.ui.platform.LocalDensity.current
                                                    val iconWidthPx = with(density) { BanStyle.FriendsInfoIconSize.toPx() }.toInt()
                                                    val popupHeightPx = with(density) { 26.dp.toPx() }.toInt()
                                                    androidx.compose.ui.window.Popup(
                                                        onDismissRequest = { showBanPopup = false },
                                                        properties = androidx.compose.ui.window.PopupProperties(focusable = true, dismissOnClickOutside = true),
                                                        offset = androidx.compose.ui.unit.IntOffset(
                                                            iconWidthPx + with(density) { 3.dp.toPx() }.toInt(),
                                                            -popupHeightPx / 2 + iconWidthPx / 2
                                                        )
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .wrapContentWidth()
                                                                .height(26.dp)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .matchParentSize()
                                                                    .background(AppColors.Panel2A.copy(alpha = 0.9f), FriendsInfoBubbleShape())
                                                                    .border(1.dp, AppColors.Border, FriendsInfoBubbleShape())
                                                            )
                                                            Box(
                                                                modifier = Modifier
                                                                    .wrapContentWidth()
                                                                    .height(26.dp)
                                                                    .padding(start = 12.dp, end = 10.dp),
                                                                contentAlignment = Alignment.CenterStart
                                                            ) {
                                                                Text(
                                                                    text = getPopupBanText(profile.banReason),
                                                                    color = AppColors.White,
                                                                    fontSize = 11.sp,
                                                                    maxLines = 1,
                                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "ID: ", color = AppColors.TextGray, fontSize = 13.sp)
                                        Text(
                                            text = profile.id,
                                            color = AppColors.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = "${skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zaregistrirovan_eb21).trimEnd()} ", color = AppColors.TextGray, fontSize = 13.sp)
                                        Text(
                                            text = profile.created.takeIf { it.isNotBlank() } ?: skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_neizvestno_8464),
                                            color = AppColors.White,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp, end = 58.dp)
                            ) {
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_opisanie_akkaunta_098e),
                                    color = AppColors.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                val descDisplay = profile.description.trim().let { d ->
                                    if (d.isBlank() || d == "неизвестно" || d == "unknown" || d == "Информация не указана." || d == "Информация не указана" || d == "Information not specified." || d == "Information not provided.") "—" else d
                                }
                                Text(
                                    text = descDisplay,
                                    color = AppColors.TextMuted,
                                    fontSize = 12.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isBanMode) {
                            // --- РЕЖИМ БЛОКИРОВКИ АККАУНТА (ACCOUNT BAN MODE) ---
                            val banReasonsRu = listOf(
                                "Нечестная игра",
                                "Использование читов",
                                "Токсичность",
                                "Использование багов",
                                "Мошенничество",
                                "Взлом аккаунта",
                                "Нарушение правил сообщества"
                            )
                            val banReasonsEn = listOf(
                                "Unfair Play",
                                "Use of Cheats",
                                "Toxicity",
                                "Bug Exploiting",
                                "Fraud",
                                "Account Hacking",
                                "Community Rules Violation"
                            )
                            val currentReasons = if (isRu) banReasonsRu else banReasonsEn
                            val currentReasonText = currentReasons.getOrElse(selectedReasonIndex) { currentReasons[0] }

                            val activeDays = selectedDaysPreset ?: customDaysInput.toIntOrNull() ?: 0
                            val isDaysValid = if (selectedDaysPreset != null) true else activeDays in 1..365
                            val targetIsBanned = profile.banStatus == 1
                            val isAdmin = sys_menu_socket_repository.currentPlayerAdminStatus == 1
                            val isVerifiedAdmin = sys_menu_socket_repository.isCurrentPlayerVerifiedAdmin
                            val canBan = !targetIsBanned && isDaysValid && isAdmin && isVerifiedAdmin && !isActionInProgress
                            val canUnban = targetIsBanned && isAdmin && isVerifiedAdmin && !isActionInProgress

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(top = 10.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // LEFT COLUMN: REASONS SELECTION + PLAYER PROFILE BUTTON
                                    Column(
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Row 1: Нечестная игра (0), Использование читов (1)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf(0, 1).forEach { index ->
                                                    AnimatedReasonButton(
                                                        text = currentReasons[index],
                                                        isSelected = selectedReasonIndex == index,
                                                        onClick = { selectedReasonIndex = index },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            // Row 2: Токсичность (2), Использование багов (3)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf(2, 3).forEach { index ->
                                                    AnimatedReasonButton(
                                                        text = currentReasons[index],
                                                        isSelected = selectedReasonIndex == index,
                                                        onClick = { selectedReasonIndex = index },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            // Row 3: Мошенничество (4), Взлом аккаунта (5)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                listOf(4, 5).forEach { index ->
                                                    AnimatedReasonButton(
                                                        text = currentReasons[index],
                                                        isSelected = selectedReasonIndex == index,
                                                        onClick = { selectedReasonIndex = index },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            // Row 4: Нарушение правил сообщества (6)
                                            AnimatedReasonButton(
                                                text = currentReasons[6],
                                                isSelected = selectedReasonIndex == 6,
                                                onClick = { selectedReasonIndex = 6 },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        // Row 5: Профиль игрока (Button to switch back to stats mode)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(30.dp)
                                                .background(AppColors.DialogButtonActive, RoundedCornerShape(4.dp))
                                                .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                                .clickable { isBanMode = false },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isRu) "Профиль игрока" else "Player Profile",
                                                color = AppColors.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // RIGHT COLUMN: DURATION SELECTION (PRESETS + CUSTOM INPUT + ACTION BUTTONS)
                                    Column(
                                        modifier = Modifier
                                            .weight(1.1f)
                                            .fillMaxHeight(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val row1 = listOf(1, 2, 3, 7)
                                            val row2 = listOf(14, 30, 60, 90)
                                            val row3 = listOf(120, 180, 365, 9999)

                                            // Row 1 presets
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                row1.forEach { days ->
                                                    AnimatedPresetDaysButton(
                                                        text = "$days",
                                                        isSelected = selectedDaysPreset == days,
                                                        onClick = {
                                                            selectedDaysPreset = days
                                                            customDaysInput = ""
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            // Row 2 presets
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                row2.forEach { days ->
                                                    AnimatedPresetDaysButton(
                                                        text = "$days",
                                                        isSelected = selectedDaysPreset == days,
                                                        onClick = {
                                                            selectedDaysPreset = days
                                                            customDaysInput = ""
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            // Row 3 presets
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                row3.forEach { days ->
                                                    val displayText = if (days == 9999) (if (isRu) "Навсегда" else "Forever") else "$days"
                                                    AnimatedPresetDaysButton(
                                                        text = displayText,
                                                        isSelected = selectedDaysPreset == days,
                                                        onClick = {
                                                            selectedDaysPreset = days
                                                            customDaysInput = ""
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }

                                            // Row 4: Custom Days input (full row width, height 26.dp)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isRu) "Своё:" else "Custom:",
                                                    color = AppColors.TextGray,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Normal
                                                )
                                                UnifiedTextField(
                                                    value = customDaysInput,
                                                    onValueChange = { input ->
                                                        val digits = input.filter { it.isDigit() }
                                                        val num = digits.toIntOrNull()
                                                        if (digits.isEmpty() || (num != null && num <= 365)) {
                                                            customDaysInput = digits
                                                            if (digits.isNotEmpty()) {
                                                                selectedDaysPreset = null
                                                            }
                                                        }
                                                    },
                                                    inputFilter = { text -> text.filter { it.isDigit() } },
                                                    placeholder = "1..365",
                                                    maxLength = 3,
                                                    singleLine = true,
                                                    heightDp = 26,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }

                                        // Action Buttons Row: Забанить / Разбанить
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Button: Забанить
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .background(
                                                        if (canBan) AppColors.DialogButtonActive else AppColors.DialogButtonInactive,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        AppColors.Border,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable(enabled = canBan) {
                                                        showConfirmBanDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isRu) "Забанить" else "Ban",
                                                    color = if (canBan) AppColors.White else AppColors.White.copy(alpha = 0.35f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            // Button: Разбанить
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(30.dp)
                                                    .background(
                                                        if (canUnban) AppColors.DialogButtonActive else AppColors.DialogButtonInactive,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        AppColors.Border,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .clickable(enabled = canUnban) {
                                                        showConfirmUnbanDialog = true
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (isRu) "Разбанить" else "Unban",
                                                    color = if (canUnban) AppColors.White else AppColors.White.copy(alpha = 0.35f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // --- РЕЖИМ СТАТИСТИКИ (NORMAL PLAYER STATS MODE) ---
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(top = 10.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(150.dp)
                                                .drawBehindRightBorderFriend()
                                                .padding(end = 20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            KdDonutChart(
                                                kills = kills,
                                                deaths = deaths,
                                                zombies = zombies,
                                                juggernauts = juggernauts,
                                                ghosts = ghosts,
                                                kdValText = kdText,
                                                shouldAnimate = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(80.dp)
                                            )
                                            val maxLen = maxOf(String.format("%.0f", kills).length, String.format("%.0f", deaths).length)
                                            val statLabelFontSize = when {
                                                maxLen <= 5 -> 8.5.sp
                                                maxLen == 6 -> 8.5.sp
                                                maxLen == 7 -> 8.sp
                                                maxLen == 8 -> 7.5.sp
                                                else -> 7.sp
                                            }
                                            val statValueFontSize = when {
                                                maxLen <= 4 -> 16.sp
                                                maxLen == 5 -> 15.sp
                                                maxLen == 6 -> 13.5.sp
                                                maxLen == 7 -> 12.sp
                                                maxLen == 8 -> 11.sp
                                                else -> 10.sp
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(28.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(26.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.kills),
                                                        fontSize = statLabelFontSize,
                                                        color = AppColors.TextGray,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                    Text(
                                                        text = killsText,
                                                        fontSize = statValueFontSize,
                                                        color = AppColors.White,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(26.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.death),
                                                        fontSize = statLabelFontSize,
                                                        color = AppColors.TextGray,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                    Text(
                                                        text = deathsText,
                                                        fontSize = statValueFontSize,
                                                        color = AppColors.White,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 20.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    GridStatLineFriend(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.score), value = pointsText)
                                                    GridStatLineFriend(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.zombies_killed), value = zombiesText)
                                                    GridStatLineFriend(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.juggernauts_killed), value = juggernautsText)
                                                }
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    GridStatLineFriend(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bugs_killed), value = bugsText)
                                                    GridStatLineFriend(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ghosts_killed), value = ghostsText)
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 6.dp, horizontal = 12.dp),
                                                        horizontalArrangement = Arrangement.Start,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.status),
                                                            color = AppColors.TextGray,
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.alignByBaseline()
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        val rawStatus = sys_menu_socket_repository.getUnifiedOnlineStatus(profile.status)
                                                        val currentStatus = rawStatus.takeIf { it.isNotBlank() && it != "-" } ?: skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_neizvestno_8464)
                                                        Text(
                                                            text = currentStatus,
                                                            color = if (currentStatus == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba)) AppColors.Success else AppColors.TextDarkGray,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.alignByBaseline()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(180.dp)
                                                .height(30.dp)
                                                .background(AppColors.DialogButtonActive, RoundedCornerShape(4.dp))
                                                .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                                .clickable { dismissWithAnimation(onDismiss) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zakryt_profil_358e),
                                                color = AppColors.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val isAdmin = sys_menu_socket_repository.currentPlayerAdminStatus == 1
                    val isVerifiedAdmin = sys_menu_socket_repository.isCurrentPlayerVerifiedAdmin
                    val isSelf = profile.id == sys_menu_socket_repository.currentPlayerId
                    if (isAdmin && isVerifiedAdmin && !isSelf) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .width(52.dp)
                                .height(28.dp)
                                .background(if (isBanMode) AppColors.Panel2A else AppColors.DialogButtonActive, RoundedCornerShape(5.dp))
                                .border(1.dp, AppColors.Border, RoundedCornerShape(5.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        isBanMode = !isBanMode
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(skayrux.shootout2d.R.drawable.ic_gavel),
                                contentDescription = if (isBanMode) (if (isRu) "Режим блокировки" else "Ban mode") else (if (isRu) "Управление блокировкой" else "Ban management"),
                                tint = AppColors.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (showConfirmBanDialog) {
                        UnifiedDialog(
                            title = if (isRu) "Заблокировать игрока?" else "Block player?",
                            confirmText = if (isRu) "да" else "yes",
                            cancelText = if (isRu) "нет" else "no",
                            confirmColor = AppColors.DialogButtonActive,
                            isLoading = isActionInProgress,
                            onConfirm = {
                                if (!isActionInProgress) {
                                    isActionInProgress = true
                                    coroutineScope.launch {
                                        val activeDays = selectedDaysPreset ?: customDaysInput.toIntOrNull() ?: 1
                                        val expiresMillis = if (activeDays >= 9999) -1L else (System.currentTimeMillis() + activeDays.toLong() * 24L * 60L * 60L * 1000L)
                                        val banReasonsRu = listOf(
                                            "Нечестная игра",
                                            "Использование читов",
                                            "Токсичность",
                                            "Использование багов",
                                            "Мошенничество",
                                            "Взлом аккаунта",
                                            "Нарушение правил сообщества"
                                        )
                                        val banReasonsEn = listOf(
                                            "Unfair Play",
                                            "Use of Cheats",
                                            "Toxicity",
                                            "Bug Exploiting",
                                            "Fraud",
                                            "Account Hacking",
                                            "Community Rules Violation"
                                        )
                                        val currentReasons = if (isRu) banReasonsRu else banReasonsEn
                                        val currentReasonText = currentReasons.getOrElse(selectedReasonIndex) { currentReasons[0] }
                                        val fullBanPayload = if (expiresMillis <= 0L) "$currentReasonText:-1" else "$currentReasonText:$expiresMillis"
                                        val res = sys_menu_socket_repository.banPlayerOnline(profile.id, fullBanPayload)
                                        if (res == "BAN_OK") {
                                            val updated = profile.copy(banStatus = 1, banReason = fullBanPayload)
                                            profile = updated
                                            sys_menu_socket_repository.players[updated.name] = updated
                                            if (profile.id.isNotEmpty()) {
                                                sys_menu_socket_repository.players[profile.id] = updated
                                            }
                                            AvatarkiCacheManager.deleteAvatar(context, profile.id)
                                            sys_menu_socket_repository.userAvatarsMap = sys_menu_socket_repository.userAvatarsMap + (profile.id to "")
                                        }
                                        isActionInProgress = false
                                        showConfirmBanDialog = false
                                    }
                                }
                            },
                            onDismiss = {
                                if (!isActionInProgress) {
                                    showConfirmBanDialog = false
                                }
                            }
                        )
                    }

                    if (showConfirmUnbanDialog) {
                        UnifiedDialog(
                            title = if (isRu) "Разблокировать игрока?" else "Unblock player?",
                            confirmText = if (isRu) "да" else "yes",
                            cancelText = if (isRu) "нет" else "no",
                            confirmColor = AppColors.DialogButtonActive,
                            isLoading = isActionInProgress,
                            onConfirm = {
                                if (!isActionInProgress) {
                                    isActionInProgress = true
                                    coroutineScope.launch {
                                        val res = sys_menu_socket_repository.unbanPlayerOnline(profile.id)
                                        if (res == "UNBAN_OK") {
                                            val updated = profile.copy(banStatus = 0, banReason = "")
                                            profile = updated
                                            sys_menu_socket_repository.players[updated.name] = updated
                                            if (profile.id.isNotEmpty()) {
                                                sys_menu_socket_repository.players[profile.id] = updated
                                            }
                                        }
                                        isActionInProgress = false
                                        showConfirmUnbanDialog = false
                                    }
                                }
                            },
                            onDismiss = {
                                if (!isActionInProgress) {
                                    showConfirmUnbanDialog = false
                                }
                            }
                        )
                    }
                }
            }
        }
}

@Composable
fun GridStatLineFriend(label: String, value: String) {
    val displayLabel = if (label.trimEnd().endsWith(":")) label.trimEnd() else "${label.trimEnd()}:"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Overlay05, RoundedCornerShape(3.dp))
            .drawBehindLeftBorderFriend()
            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayLabel,
            color = AppColors.TextGray,
            fontSize = 11.sp,
            modifier = Modifier.alignByBaseline()
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            color = AppColors.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.alignByBaseline()
        )
    }
}

fun Modifier.drawBehindBottomBorderFriend(): Modifier = this.drawBehind {
    drawLine(
        color = AppColors.OverlayVeryLight,
        start = androidx.compose.ui.geometry.Offset(0f, size.height),
        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx()
    )
}

fun Modifier.drawBehindRightBorderFriend(): Modifier = this.drawBehind {
    drawLine(
        color = AppColors.OverlayVeryLight,
        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx()
    )
}

fun Modifier.drawBehindLeftBorderFriend(): Modifier = this.drawBehind {
    drawLine(
        color = AppColors.Border,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, size.height),
        strokeWidth = 2.dp.toPx()
    )
}
