package skayrux.shootout2d

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.style.TextAlign
import android.content.Context
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun my_profile_menu(
    playerName: String,
    isRegistered: Boolean,
    registeredLogin: String,
    accountId: String,
    description: String,
    userAvatar: String? = null,
    isSettingsTabActive: Boolean = false,
    onNameChange: (String) -> Unit,
    onRegisterChange: (Boolean) -> Unit,
    onRegisteredLoginChange: (String) -> Unit,
    onIdChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onBack: () -> Unit,
    onBackToProfile: () -> Unit = {}
) {
    val isOnline = sys_menu_socket_repository.isServerOnline
    val targetKills = if (isOnline) sys_menu_socket_repository.currentPlayerEnemk else 0f
    val targetDeaths = if (isOnline) sys_menu_socket_repository.currentPlayerDeath else 0f
    val targetPoints = if (isOnline) sys_menu_socket_repository.currentPlayerScore else 0
    val targetZombies = if (isOnline) sys_menu_socket_repository.currentPlayerZomk else 0
    val targetJuggernauts = if (isOnline) sys_menu_socket_repository.currentPlayerJugk else 0
    val targetBugs = if (isOnline) sys_menu_socket_repository.currentPlayerBugk else 0
    val targetGhosts = if (isOnline) sys_menu_socket_repository.currentPlayerGhostk else 0

    val totalKills = targetKills + targetZombies + targetJuggernauts + targetGhosts
    val targetKd = if (targetDeaths > 0f) totalKills / targetDeaths else totalKills

    val currentAccountKey = "${isRegistered}_${accountId}_${sys_menu_socket_repository.currentPlayerName}"
    var lastAccountKey by remember { mutableStateOf(currentAccountKey) }
    var animTrigger by remember { mutableIntStateOf(0) }
    var shouldAnimate by remember { mutableStateOf(sys_menu_socket_repository.shouldAnimateStats) }

    LaunchedEffect(Unit) {
        if (sys_menu_socket_repository.shouldAnimateStats) {
            sys_menu_socket_repository.shouldAnimateStats = false
            shouldAnimate = true
            animTrigger++
        }
    }

    LaunchedEffect(currentAccountKey) {
        if (lastAccountKey != currentAccountKey) {
            lastAccountKey = currentAccountKey
            shouldAnimate = true
            animTrigger++
        }
    }

    val animSpec: androidx.compose.animation.core.AnimationSpec<Float> = if (shouldAnimate) {
        tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    } else {
        androidx.compose.animation.core.snap()
    }

    val animatedKills by androidx.compose.animation.core.animateFloatAsState(targetKills, animSpec, label = "kills")
    val animatedDeaths by androidx.compose.animation.core.animateFloatAsState(targetDeaths, animSpec, label = "deaths")
    val animatedKd by androidx.compose.animation.core.animateFloatAsState(targetKd, animSpec, label = "kd")
    val animatedPoints by androidx.compose.animation.core.animateFloatAsState(targetPoints.toFloat(), animSpec, label = "points")
    val animatedZombies by androidx.compose.animation.core.animateFloatAsState(targetZombies.toFloat(), animSpec, label = "zombies")
    val animatedJuggernauts by androidx.compose.animation.core.animateFloatAsState(targetJuggernauts.toFloat(), animSpec, label = "juggernauts")
    val animatedBugs by androidx.compose.animation.core.animateFloatAsState(targetBugs.toFloat(), animSpec, label = "bugs")
    val animatedGhosts by androidx.compose.animation.core.animateFloatAsState(targetGhosts.toFloat(), animSpec, label = "ghosts")

    fun formatProfileStat(value: Number): String {
        return java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(value.toLong())
    }

    val killsText = formatProfileStat(animatedKills)
    val deathsText = formatProfileStat(animatedDeaths)
    val kdValText = String.format(java.util.Locale.US, "%.2f", animatedKd)
    val pointsText = formatProfileStat(animatedPoints)
    val zombiesText = formatProfileStat(animatedZombies)
    val juggernautsText = formatProfileStat(animatedJuggernauts)
    val bugsText = formatProfileStat(animatedBugs)
    val ghostsText = formatProfileStat(animatedGhosts)

    var toastMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(sys_menu_socket_repository.kickedReason) {
        val reason = sys_menu_socket_repository.kickedReason
        if (reason != null) {
            if (reason.contains("BAN", ignoreCase = true) || reason.contains(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zablokirovan_c65d), ignoreCase = true) || reason.contains("banned", ignoreCase = true)) {
                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_akkaunt_zablokirovan_f0eb)
            }
            sys_menu_socket_repository.kickedReason = null
        }
    }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sys_menu_socket_repository.refreshProfileConfig()
        sys_menu_socket_repository.syncFullAccountData()
        if (accountId.isNotEmpty()) {
            sys_menu_socket_repository.getProfileById(accountId)
        }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var showTokenAuthDialog by remember { mutableStateOf(false) }
    var isProfileSettingsOpen by remember { mutableStateOf(false) }
    var activeDialog by remember { mutableStateOf<String?>(null) }
    var selectedAvatarUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(
        showEditNameDialog, showEditInfoDialog, showAvatarDialog
    ) {
        val openList = mutableListOf<String>()
        if (showEditNameDialog) openList.add("EditName")
        if (showEditInfoDialog) openList.add("EditInfo")
        if (showAvatarDialog) openList.add("Avatar")

        if (openList.size > 1) {
            val currentActive = activeDialog
            if (currentActive != null && openList.contains(currentActive)) {
                if (currentActive != "EditName" && showEditNameDialog) showEditNameDialog = false
                if (currentActive != "EditInfo" && showEditInfoDialog) showEditInfoDialog = false
                if (currentActive != "Avatar" && showAvatarDialog) showAvatarDialog = false
            } else {
                val first = openList.first()
                activeDialog = first
                if (first != "EditName" && showEditNameDialog) showEditNameDialog = false
                if (first != "EditInfo" && showEditInfoDialog) showEditInfoDialog = false
                if (first != "Avatar" && showAvatarDialog) showAvatarDialog = false
            }
        } else if (openList.size == 1) {
            activeDialog = openList.first()
        } else {
            activeDialog = null
        }
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val objectsBitmap = remember { ResourceCache.getObjects(context) }
    // полный список оружия для просмотра
    val top10Weapons = remember {
        listOf(
            ProfileWeaponItem("ob_ak47", "AK47", 0.25f, 18.2f),
            ProfileWeaponItem("ob_m16", "M16", 0.18f, 15.6f),
            ProfileWeaponItem("ob_aug", "AUG", 0.10f, 14.8f),
            ProfileWeaponItem("ob_famas", "FAMAS", 0.08f, 15.1f),
            ProfileWeaponItem("ob_deagle", "DEAGLE", 0.08f, 16.8f),
            ProfileWeaponItem("ob_p90", "P90", 0.07f, 12.4f),
            ProfileWeaponItem("ob_usi", "USI", 0.05f, 13.0f),
            ProfileWeaponItem("ob_p1", "P1", 0.04f, 14.0f),
            ProfileWeaponItem("ob_p2", "P2", 0.03f, 13.5f),
            ProfileWeaponItem("ob_p3", "P3", 0.02f, 12.0f),
            ProfileWeaponItem("ob_rifle", "RIFLE", 0.02f, 24.5f),
            ProfileWeaponItem("ob_svd", "SVD", 0.02f, 22.1f),
            ProfileWeaponItem("ob_vintorez", "VINTOREZ", 0.015f, 19.4f),
            ProfileWeaponItem("ob_osv96", "OSV96", 0.015f, 26.0f),
            ProfileWeaponItem("ob_dr1", "DR1", 0.01f, 10.0f),
            ProfileWeaponItem("ob_dr2", "DR2", 0.01f, 11.0f),
            ProfileWeaponItem("ob_dr3", "DR3", 0.01f, 12.0f),
            ProfileWeaponItem("ob_minig", "MINIGUN", 0.01f, 14.0f),
            ProfileWeaponItem("ob_grl", "GL", 0.005f, 16.0f),
            ProfileWeaponItem("ob_rpg", "RPG", 0.005f, 18.0f),
            ProfileWeaponItem("ob_ognemet", "OGNEMET", 0.005f, 15.0f),
            ProfileWeaponItem("ob_arbalet", "ARBALET", 0.005f, 20.0f),
            ProfileWeaponItem("ob_knife", "KNIFE", 0.005f, 50.0f),
            ProfileWeaponItem("ob_axe", "AXE", 0.005f, 45.0f),
            ProfileWeaponItem("ob_gr", "FRAG GRENADE", 0.005f, 10.0f),
            ProfileWeaponItem("ob_oskgr", "FRAG GRENADE", 0.005f, 10.0f),
            ProfileWeaponItem("ob_grslp", "FLASHBANG", 0.005f, 5.0f),
            ProfileWeaponItem("ob_grdim", "SMOKE GRENADE", 0.005f, 5.0f),
            ProfileWeaponItem("ob_molotov", "MOLOTOV", 0.005f, 8.0f)
        )
    }
    var isAvatarPickerLaunching by remember { mutableStateOf(false) }
    var lastAvatarPickerLaunchTime by remember { mutableStateOf(0L) }
    val avatarPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        isAvatarPickerLaunching = false
        if (uri != null) {
            selectedAvatarUri = uri
        }
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            toastMessage = null
        }
    }

    if (isSettingsTabActive || isProfileSettingsOpen) {
        menu_my_profile_settings(
            onBack = {
                isProfileSettingsOpen = false
                onBackToProfile()
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // левая карточка игрока
                Column(
                    modifier = Modifier
                        .widthIn(min = 230.dp, max = 265.dp)
                        .weight(0.36f)
                        .fillMaxHeight()
                        .background(AppColors.PanelDark, RoundedCornerShape(0.dp))
                        .border(1.dp, AppColors.Border, RoundedCornerShape(0.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // шапка игрока
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var isAskingUserAvatar by remember { mutableStateOf(false) }
                        var avatarTickNowMs by remember { mutableStateOf(System.currentTimeMillis()) }
                        val userAvatarCooldownSeconds = sys_menu_socket_repository.getUserAvatarCooldownRemainingSec(avatarTickNowMs)
                        LaunchedEffect(sys_menu_socket_repository.userAvatarCooldownUntilMs) {
                            while (sys_menu_socket_repository.getUserAvatarCooldownRemainingSec(System.currentTimeMillis()) > 0) {
                                avatarTickNowMs = System.currentTimeMillis()
                                delay(500)
                            }
                            avatarTickNowMs = System.currentTimeMillis()
                        }

                        UserAvatar(
                            userId = accountId,
                            userName = sys_menu_socket_repository.currentPlayerName,
                            sizeDp = 54,
                            fontSizeSp = 22,
                            shape = RoundedCornerShape(0.dp),
                            isBanned = sys_menu_socket_repository.currentPlayerBanStatus == 1,
                            onClick = if (userAvatarCooldownSeconds > 0 || isAskingUserAvatar) null else {
                                {
                                    scope.launch {
                                        if (!sys_menu_socket_repository.isServerOnline) {
                                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_podklyucheniya_k_serveru_02ed)
                                            return@launch
                                        }
                                        val currentAv = sys_menu_socket_repository.userAvatarsMap[accountId] ?: ""
                                        if (currentAv.isNotEmpty() && currentAv != "none" && currentAv != "LOADING") {
                                            showAvatarDialog = true
                                            return@launch
                                        }
                                        isAskingUserAvatar = true
                                        try {
                                            val askResp = sys_menu_socket_repository.askChangeUserAvatar()
                                            if (askResp == "ASK_CHANGE_USER_AVATAR_OK") {
                                                showAvatarDialog = true
                                            } else if (askResp.startsWith("ASK_CHANGE_USER_AVATAR_ERROR:RATE_LIMIT")) {
                                                val sec = askResp.substringAfter("|", "60").toIntOrNull()?.coerceIn(1, 60) ?: 60
                                                sys_menu_socket_repository.setUserAvatarCooldown(sec)
                                                avatarTickNowMs = System.currentTimeMillis()
                                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_limit_dostignut_povtorite_pozzhe)
                                            } else if (askResp == "ASK_CHANGE_USER_AVATAR_ERROR:SCORE_TOO_LOW") {
                                                toastMessage = skayrux.shootout2d.Loc.tr(
                                                    skayrux.shootout2d.R.string.str_nuzhno_minimum_50_000_ochkov_dlya_8382,
                                                    sys_menu_socket_repository.formattedMinScoreAvatar
                                                )
                                            } else {
                                                toastMessage = sys_menu_socket_repository.translateError(askResp)
                                            }
                                        } finally {
                                            isAskingUserAvatar = false
                                        }
                                    }
                                }
                            }
                        )

                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val idValue = if (!sys_menu_socket_repository.isServerOnline) "N/A" else sys_menu_socket_repository.currentPlayerId.ifBlank { "N/A" }
                            Text(
                                text = "ID: $idValue",
                                color = AppColors.TextDarkGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = sys_menu_socket_repository.currentPlayerName,
                                    color = if (sys_menu_socket_repository.currentPlayerBanStatus == 1) Color(0xFFFF5252) else AppColors.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                IconButton(
                                    onClick = { showEditNameDialog = true },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit Nick",
                                        tint = AppColors.TextDarkGray,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                            val isVkAccStatus = sys_menu_socket_repository.isVkAccount ||
                                sys_menu_socket_repository.currentPlayerVkId.isNotBlank() ||
                                AppSettings.getString(context, "auth_type", "").lowercase() == "vk"
                            val accountStatusText = if (!sys_menu_socket_repository.isServerOnline) "N/A" else if (isVkAccStatus) "register" else "guest"
                            Text(
                                text = "Аккаунт: $accountStatusText",
                                color = AppColors.TextGray,
                                fontSize = 9.5.sp
                            )
                        }
                    }

                    // дата создания
                    val regDateValue = if (!isOnline) "—" else sys_menu_socket_repository.currentPlayerCreated.ifBlank { "—" }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.PanelDark, RoundedCornerShape(0.dp))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(0.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Создан:", color = AppColors.TextDarkGray, fontSize = 9.5.sp)
                        Text(text = regDateValue, color = AppColors.TextGray, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                    }

                    // блок описания
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.PanelDark, RoundedCornerShape(0.dp))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(0.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ОПИСАНИЕ",
                                color = AppColors.TextDarkGray,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            IconButton(
                                onClick = { showEditInfoDialog = true },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Bio",
                                    tint = AppColors.TextDarkGray,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Text(
                            text = sys_menu_socket_repository.currentPlayerDescription.ifBlank { "Описание не указано" },
                            color = AppColors.TextGray,
                            fontSize = 10.5.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // статус блокировки если есть
                    if (sys_menu_socket_repository.currentPlayerBanStatus == 1) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x33FF5252), RoundedCornerShape(0.dp))
                                .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f), RoundedCornerShape(0.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "АККАУНТ ЗАБЛОКИРОВАН",
                                color = Color(0xFFFF5252),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // правая панель статистики и режимов
                Column(
                    modifier = Modifier
                        .weight(0.64f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // вкладка общей статистики
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .background(AppColors.TabActive, RoundedCornerShape(0.dp))
                            .border(1.dp, AppColors.BorderActive, RoundedCornerShape(0.dp))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Общая статистика",
                            color = AppColors.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    // разделитель как в инвентаре
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(AppColors.Overlay10)
                    )

                    // контент общей статистики
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(AppColors.PanelDark, RoundedCornerShape(0.dp))
                            .border(1.dp, AppColors.Border, RoundedCornerShape(0.dp))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // верхняя строка: кд слева
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, top = 8.dp, end = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // график кд и счетчики убийств/смертей
                                Column(
                                    modifier = Modifier.width(120.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    KdDonutChart(
                                        kills = animatedKills,
                                        deaths = animatedDeaths,
                                        zombies = animatedZombies,
                                        juggernauts = animatedJuggernauts,
                                        ghosts = animatedGhosts,
                                        kdValText = kdValText,
                                        animTrigger = animTrigger,
                                        shouldAnimate = shouldAnimate,
                                        chartSizeDp = 64,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(68.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Убийства",
                                                fontSize = 8.sp,
                                                color = AppColors.TextDarkGray,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = killsText,
                                                fontSize = 13.sp,
                                                color = AppColors.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Смерти",
                                                fontSize = 8.sp,
                                                color = AppColors.TextDarkGray,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = deathsText,
                                                fontSize = 13.sp,
                                                color = AppColors.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                                    // горизонтальный разделитель
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(AppColors.Border)
                                    )

                                    // показатели игрока из ресурсов
                                    val labelScore = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.score1)
                                    val labelZombies = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.zombies_killed).trimEnd(':').trim()
                                    val labelJuggernauts = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.juggernauts_killed).trimEnd(':').trim()
                                    val labelBugs = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bugs_killed).trimEnd(':').trim()
                                    val labelGhosts = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ghosts_killed).trimEnd(':').trim()

                                    // нижняя часть: слева все 5 показателей, справа оружие
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(start = 10.dp, end = 6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // колонка статистики слева
                                        Column(
                                            modifier = Modifier
                                                .width(165.dp)
                                                .padding(bottom = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            MinimalistStatRow(labelScore, pointsText)
                                            MinimalistStatRow(labelZombies, zombiesText)
                                            MinimalistStatRow(labelJuggernauts, juggernautsText)
                                            MinimalistStatRow(labelBugs, bugsText)
                                            MinimalistStatRow(labelGhosts, ghostsText)
                                        }

                                        // вертикальный разделитель
                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 6.dp)
                                                .width(1.dp)
                                                .fillMaxHeight()
                                                .background(AppColors.Border)
                                        )

                                        // список оружия с прокруткой вплотную до нижней рамки
                                        androidx.compose.foundation.lazy.LazyColumn(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 6.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            items(count = top10Weapons.size) { i ->
                                                val weaponStat = top10Weapons[i]
                                                val weaponData = ObjectDatabase.objects.find { it.name == weaponStat.objectName }
                                                val weaponKills = (targetKills * weaponStat.killsFraction).toInt()

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp, horizontal = 2.dp)
                                                ) {
                                                    // строка названия, убийств и текстуры
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 2.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(
                                                                text = "${i + 1}. ${weaponData?.displayName ?: weaponStat.displayName}",
                                                                color = AppColors.White,
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1
                                                            )
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = "Убийств:",
                                                                    color = AppColors.TextDarkGray,
                                                                    fontSize = 8.5.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Text(
                                                                    text = "$weaponKills",
                                                                    color = AppColors.White,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                        WeaponThumbnail(
                                                            item = weaponData,
                                                            objectsBitmap = objectsBitmap,
                                                            modifier = Modifier
                                                                .width(68.dp)
                                                                .height(22.dp)
                                                        )
                                                    }

                                                    // разделитель элементов списка
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(top = 4.dp, bottom = 2.dp)
                                                            .fillMaxWidth()
                                                            .height(1.dp)
                                                            .background(AppColors.Overlay10)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

        // окно изменения ника
        if (showEditNameDialog) {
            val currentNick = sys_menu_socket_repository.currentPlayerName.ifBlank { playerName }.ifBlank { "Player" }
            var newName by remember(showEditNameDialog) { mutableStateOf(currentNick) }
            var isLoading by remember { mutableStateOf(false) }

            LaunchedEffect(showEditNameDialog) {
                val freshNick = sys_menu_socket_repository.currentPlayerName.ifBlank { playerName }.ifBlank { "Player" }
                newName = freshNick
            }
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.change_nickname),
                content = {
                    // ввод нового ника
                    UnifiedTextField(
                        value = newName,
                        onValueChange = { newName = AllowedChars.sanitize(it, 16) },
                        placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_novyy_nik_f734),
                        regex = AllowedChars.REGEX,
                        inputFilter = { AllowedChars.sanitize(it, 16) },
                        maxLength = 16,
                        enabled = !isLoading
                    )
                },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sohranit_74ea),
                confirmEnabled = !isLoading && newName.isNotBlank(),
                isLoading = isLoading,
                dismissOnConfirm = false,
                onConfirm = {
                    val finalName = if (newName.isBlank()) "Player" else newName.trim()
                    if (!sys_menu_socket_repository.isServerOnline) {
                        onNameChange(finalName)
                        toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nik_izmenen_fb67)
                        showEditNameDialog = false
                    } else {
                        scope.launch {
                            isLoading = true
                            try {
                                val res = sys_menu_socket_repository.changeNicknameGuestOnline(sys_menu_socket_repository.currentPlayerId, finalName)

                                if (res == "SUCCESS") {
                                    onNameChange(finalName)
                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nik_uspeshno_izmenen_5971)
                                    showEditNameDialog = false
                                } else if (res == "OFFLINE") {
                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_podklyucheniya_k_serveru_02ed)
                                } else if (res.startsWith("NICK_ERROR:BAD_NICK")) {
                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nedopustimyy_nik_1_20_simv_5d51)
                                } else if (res.startsWith("NICK_ERROR:WRONG_PASS") || res.startsWith("LOGIN_ERROR:WRONG_PASS")) {
                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.wrong_password)
                                } else {
                                    toastMessage = sys_menu_socket_repository.translateError(res)
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                onDismiss = { showEditNameDialog = false }
            )
        }

        // окно изменения описания
        if (showEditInfoDialog) {
            var info by remember { mutableStateOf(if (description == "—" || description == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_informatsiya_ne_ukazana_40d9) || description == "Информация не указана." || description == "Information not specified." || description == "Information not provided.") "" else description) }
            var isLoading by remember { mutableStateOf(false) }
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_izmenit_opisanie_98db),
                content = {
                    // ввод описания
                    UnifiedTextField(
                        value = info,
                        onValueChange = { info = AllowedChars.sanitize(it, 25) },
                        placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_opisanie_38ca),
                        regex = AllowedChars.REGEX,
                        inputFilter = { AllowedChars.sanitize(it, 25) },
                        maxLength = 25,
                        enabled = !isLoading
                    )
                },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sohranit_74ea),
                confirmEnabled = !isLoading,
                isLoading = isLoading,
                dismissOnConfirm = false,
                onConfirm = {
                    scope.launch {
                        isLoading = true
                        try {
                            if (!sys_menu_socket_repository.isServerOnline) {
                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_podklyucheniya_k_serveru_02ed)
                                return@launch
                            }
                            val newDesc = info.ifBlank { "—" }
                            val targetLogin = sys_menu_socket_repository.currentPlayerId
                            val pass = sys_menu_socket_repository.authCredential
                            val res = sys_menu_socket_repository.updateDescriptionOnline(targetLogin, pass, newDesc)
                            if (res == "SUCCESS") {
                                onDescriptionChange(newDesc)
                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_uspeshno_izmenili_opisanie_a067)
                                showEditInfoDialog = false
                            } else if (res == "OFFLINE") {
                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_podklyucheniya_k_serveru_02ed)
                            } else if (res == "DESC_ERROR:TOO_LONG") {
                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_opisanie_dolzhno_byt_do_30_sim_54a1)
                            } else if (res.startsWith("DESC_ERROR:WRONG_PASS") || res.startsWith("LOGIN_ERROR:WRONG_PASS")) {
                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.wrong_password)
                            } else {
                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_oshibka_pri_izmenenii_opisaniya_997d)
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                onDismiss = { showEditInfoDialog = false }
            )
        }

        if (showTokenAuthDialog) {
            var isTokenRevealed by remember { mutableStateOf(false) }
            var isCopied by remember { mutableStateOf(false) }
            var isLoading by remember { mutableStateOf(false) }
            val isVkAcc = sys_menu_socket_repository.isVkAccount || sys_menu_socket_repository.currentPlayerVkId.isNotBlank() || AppSettings.getString(context, "auth_type", "").startsWith("vk")
            val currentToken = sys_menu_socket_repository.currentSessionToken.ifEmpty {
                AppSettings.getString(context, "session_token", "").ifEmpty {
                    AppSettings.getString(context, "vk_auth_code", "N/A")
                }
            }

            val isTokenRenewAllowed = !isVkAcc && sys_menu_socket_repository.mozhnoLiObnovlyatToken
            UnifiedDialog(
                title = Loc.tr(R.string.str_vash_token_avtorizatsii),
                cancelText = Loc.tr(R.string.back),
                confirmText = Loc.tr(R.string.refresh),
                confirmEnabled = isTokenRenewAllowed,
                isLoading = isLoading,
                dismissOnConfirm = false,
                onConfirm = {
                    if (isVkAcc) {
                        toastMessage = "Сессионный токен ВК нельзя обновить"
                        return@UnifiedDialog
                    }
                    if (!sys_menu_socket_repository.mozhnoLiObnovlyatToken) {
                        toastMessage = Loc.tr(R.string.str_obnovlenie_tokena_otklyucheno)
                        return@UnifiedDialog
                    }
                    val lastRenew = AppSettings.getLong(context, "last_token_renew_timestamp", 0L)
                    val now = System.currentTimeMillis()
                    if (now - lastRenew < 86_400_000L) {
                        toastMessage = Loc.tr(R.string.str_obnovlyat_token_mozhno_1_raz)
                    } else {
                        scope.launch {
                            isLoading = true
                            try {
                                val res = sys_menu_socket_repository.renewTokenOnline(context)
                                if (res == "SUCCESS") {
                                    AppSettings.setLong(context, "last_token_renew_timestamp", now)
                                    isCopied = false
                                    toastMessage = Loc.tr(R.string.str_token_uspeshno_obnovlen)
                                } else {
                                    toastMessage = sys_menu_socket_repository.translateError(res)
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                onDismiss = { showTokenAuthDialog = false },
                content = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // токен
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTokenRevealed = !isTokenRevealed }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentToken,
                                color = Color.White,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                fontWeight = if (isTokenRevealed) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = if (isTokenRevealed) Modifier else Modifier.blur(22.dp)
                            )
                        }

                        // кнопка копирования
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(33.5.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (!isCopied) AppColors.DialogButtonActive else AppColors.DialogButtonInactive)
                                .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                                .clickable(enabled = !isCopied && currentToken.isNotBlank() && currentToken != "N/A") {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Auth Token", currentToken)
                                        clipboard?.setPrimaryClip(clip)
                                        isCopied = true
                                    } catch (e: Exception) {
                                        toastMessage = Loc.tr(R.string.str_oshibka_kopirovaniya)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCopied) Loc.tr(R.string.str_skopirovano) else Loc.tr(R.string.str_skopirovat),
                                color = if (!isCopied) Color.White else Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                            )
                        }
                    }
                }
            )
        }

        if (showAvatarDialog) {
            val uId = sys_menu_socket_repository.currentPlayerId
            val currentAvatar = sys_menu_socket_repository.userAvatarsMap[uId] ?: ""
            val hasAvatar = currentAvatar.isNotEmpty() && currentAvatar != "none" && currentAvatar != "LOADING"
            
            UnifiedDialog(
                title = if (hasAvatar) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_udalit_avatarku_dde4) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzit_avatarku_a3f8),
                content = {
                    if (!hasAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .drawBehind {
                                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                    drawRoundRect(
                                        color = AppColors.SemiWhite66,
                                        style = stroke,
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(AppColors.InputContainer)
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    if (!isAvatarPickerLaunching && now - lastAvatarPickerLaunchTime > 1000L) {
                                        isAvatarPickerLaunching = true
                                        lastAvatarPickerLaunchTime = now
                                        avatarPickerLauncher.launch("image/*")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedAvatarUri == null) {
                                  Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vybrat_kartinku_jpg_png_6c38), color = AppColors.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            } else {
                                  Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_kartinka_vybrana_8bc9), color = AppColors.TextGreenSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmText = if (hasAvatar) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_primenit_2cd8),
                confirmEnabled = hasAvatar || selectedAvatarUri != null,
                onConfirm = {
                    if (hasAvatar) {
                        scope.launch {
                            val resp = sys_menu_socket_repository.updateUserAvatar("")
                            if (resp.startsWith("UPDATE_USER_AVATAR_OK")) {
                                AvatarkiCacheManager.saveAvatar(context, uId, "none")
                                sys_menu_socket_repository.userAvatarsMap = sys_menu_socket_repository.userAvatarsMap + (uId to "none")
                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_avatarka_uspeshno_udalena_1dc2)
                            } else {
                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_oshibka_udaleniya_avatarki, resp)
                            }
                        }
                    } else {
                        selectedAvatarUri?.let { uri ->
                            val base64 = uriToBase64(context, uri)
                            if (base64 != null) {
                                if (base64.length > 7_000_000) {
                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_avatarka_ne_dolzhna_prevyshat_5_81d9)
                                } else {
                                    scope.launch {
                                        val uploadResp = sys_menu_socket_repository.updateUserAvatar(base64)
                                        if (uploadResp.startsWith("UPDATE_USER_AVATAR_OK")) {
                                            val cd = uploadResp.substringAfter("|", "0").toIntOrNull() ?: 0
                                            if (cd > 0) {
                                                sys_menu_socket_repository.setUserAvatarCooldown(cd)
                                            }
                                            AvatarkiCacheManager.saveAvatar(context, uId, base64)
                                            sys_menu_socket_repository.userAvatarsMap = sys_menu_socket_repository.userAvatarsMap + (uId to base64)
                                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_avatarka_uspeshno_obnovlena_d7c6)
                                        } else if (uploadResp.startsWith("UPDATE_USER_AVATAR_ERROR:RATE_LIMIT")) {
                                            val sec = uploadResp.substringAfter("|", "60").toIntOrNull()?.coerceIn(1, 60) ?: 60
                                            sys_menu_socket_repository.setUserAvatarCooldown(sec)
                                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_limit_dostignut_povtorite_pozzhe)
                                        } else if (uploadResp == "UPDATE_USER_AVATAR_ERROR:COOLDOWN") {
                                            sys_menu_socket_repository.setUserAvatarCooldown(60)
                                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzhat_avatarku_mozhno_raz_v_7a30)
                                        } else {
                                            toastMessage = sys_menu_socket_repository.translateError(uploadResp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    selectedAvatarUri = null
                    showAvatarDialog = false
                },
                onDismiss = {
                    selectedAvatarUri = null
                    showAvatarDialog = false
                }
            )
        }

        UnifiedToast(
            message = toastMessage ?: "",
            visible = toastMessage != null,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        UnifiedToast(
            message = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_akkaunt_byl_zablokirovan_b094),
            visible = sys_menu_socket_repository.currentPlayerBanStatus == 1,
            textColor = AppColors.Error,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
}

@Composable
fun InfoLine(
    label: String, 
    value: String, 
    isBanned: Boolean = false, 
    banReason: String? = null, 
    onEditClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label ", 
            color = AppColors.TextGray, 
            fontSize = 13.sp,
            maxLines = 1,
            softWrap = false
        )
        val isBannedNow = isBanned && label == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nik_3e1c)
        Box(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            PlayerNameWithBan(
                name = value,
                isBanned = isBannedNow,
                baseColor = AppColors.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onEditClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = AppColors.TextDarkGray,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileButton(
    text: String,
    modifier: Modifier = Modifier,
    bgColor: Color = AppColors.Panel,
    borderColor: Color = AppColors.Border,
    textColor: Color = AppColors.White,
    isLoading: Boolean = false,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(5.dp))
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = AppColors.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, color = textColor, fontSize = 11.sp)
        }
    }
}

@Composable
fun KdStatLine(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = AppColors.GrayBB,
            fontSize = 11.sp,
            modifier = Modifier.width(66.dp)
        )
        Text(text = value, color = AppColors.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
fun GridStatLine(label: String, value: String = "0") {
    val displayLabel = if (label.trimEnd().endsWith(":")) label.trimEnd() else "${label.trimEnd()}:"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Overlay05, RoundedCornerShape(3.dp))
            .drawBehindLeftBorder()
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

fun Modifier.drawBehindBottomBorder(): Modifier = this.drawBehind {
    drawLine(
        color = AppColors.OverlayVeryLight,
        start = androidx.compose.ui.geometry.Offset(0f, size.height),
        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx()
    )
}

fun Modifier.drawBehindRightBorder(): Modifier = this.drawBehind {
    drawLine(
        color = AppColors.OverlayVeryLight,
        start = androidx.compose.ui.geometry.Offset(size.width, 0f),
        end = androidx.compose.ui.geometry.Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx()
    )
}

fun Modifier.drawBehindLeftBorder(): Modifier = this.drawBehind {
    drawLine(
        color = AppColors.Border,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(0f, size.height),
        strokeWidth = 2.dp.toPx()
    )
}

// строка статистики
@Composable
fun MinimalistStatRow(label: String, value: String, valueColor: Color = AppColors.White) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = AppColors.TextGray, fontSize = 11.sp)
        Text(text = value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// карточка показателя
@Composable
fun MinimalistMetricCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    subtitle: String,
    valueColor: Color = AppColors.White
) {
    Column(
        modifier = modifier
            .background(AppColors.PanelDark, RoundedCornerShape(0.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            color = AppColors.TextDarkGray,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            color = AppColors.TextGray,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// данные оружия для профиля
data class ProfileWeaponItem(
    val objectName: String,
    val displayName: String,
    val killsFraction: Float,
    val baseAccuracy: Float
)

// миниатюра оружия
@Composable
fun WeaponThumbnail(
    item: MapObjectData?,
    objectsBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier = Modifier
) {
    if (item != null && objectsBitmap != null) {
        androidx.compose.foundation.Canvas(modifier = modifier) {
            val w = item.atlasW.toFloat()
            val h = item.atlasH.toFloat()
            if (w > 0f && h > 0f) {
                val targetFraction = when (item.name) {
                    "ob_ak47", "ob_m16", "ob_rifle", "ob_svd", "ob_vintorez", "ob_osv96" -> 0.95f
                    "ob_aug", "ob_famas" -> 0.92f
                    "ob_p90", "ob_dr1", "ob_dr2", "ob_dr3" -> 0.85f
                    "ob_minig", "ob_grl", "ob_rpg", "ob_arbalet", "ob_ognemet" -> 0.92f
                    "ob_deagle" -> 0.60f
                    "ob_usi" -> 0.54f
                    "ob_p1", "ob_p2", "ob_p3" -> 0.55f
                    "ob_knife", "ob_axe" -> 0.58f
                    "ob_gr", "ob_oskgr", "ob_grslp", "ob_grdim", "ob_molotov" -> 0.36f
                    else -> 0.85f
                }
                val scale = (size.width / w) * targetFraction
                val dstW = (w * scale).toInt()
                val dstH = (h * scale).toInt()
                val dx = (size.width - dstW) / 2f
                val yOffset = when (item.name) {
                    "ob_famas" -> -size.height * 0.04f
                    "ob_ak47" -> size.height * 0.02f
                    "ob_ognemet" -> -size.height * 0.04f
                    "ob_usi" -> -size.height * 0.04f
                    else -> 0f
                }
                val dy = (size.height - dstH) / 2f + yOffset
                drawImage(
                    image = objectsBitmap,
                    srcOffset = androidx.compose.ui.unit.IntOffset(item.atlasX, item.atlasY),
                    srcSize = androidx.compose.ui.unit.IntSize(item.atlasW, item.atlasH),
                    dstOffset = androidx.compose.ui.unit.IntOffset(dx.toInt(), dy.toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(dstW, dstH)
                )
            }
        }
    }
}
