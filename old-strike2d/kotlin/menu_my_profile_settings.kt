package skayrux.shootout2d

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vk.id.AccessToken
import com.vk.id.VKID
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// экран настроек профиля
@Composable
fun menu_my_profile_settings(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
    ) {
        // верхняя вкладка настроек профиля (без кнопки назад)
        Row(
            modifier = Modifier
                .padding(bottom = 10.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // вкладка общее
            ProfileSettingsTabButton(
                text = Loc.tr(R.string.str_obshchee),
                isSelected = true,
                onClick = { /* активная вкладка */ }
            )
        }

        // разделительная полоса
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Overlay10)
        )

        Spacer(Modifier.height(10.dp))

        // содержимое вкладки общее
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            TabGeneral()
        }
    }
}

// кнопка верхней вкладки
@Composable
private fun ProfileSettingsTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) AppColors.TabActive else AppColors.PanelDark
    val borderColor = if (isSelected) AppColors.BorderActive else AppColors.Border

    Box(
        modifier = Modifier
            .height(30.dp)
            .width(110.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

// заголовок группы настроек
@Composable
private fun ProfileCategoryTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = AppColors.TextDarkGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier.padding(top = 2.dp, bottom = 7.dp)
    )
}


// кнопка действия в стиле настроек
@Composable
private fun SettingsActionButton(
    text: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 34.dp,
    bgColor: Color = AppColors.PanelDark,
    borderColor: Color = AppColors.Border,
    textColor: Color = AppColors.White,
    fontWeight: FontWeight = FontWeight.Normal,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.5.sp,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = minHeight)
            .height(minHeight)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) { onClick() }
            .then(if (leadingIcon != null) Modifier.padding(start = 7.dp, end = 10.dp) else Modifier.padding(horizontal = 10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (leadingIcon != null) {
            Box(modifier = Modifier.align(Alignment.CenterStart)) {
                leadingIcon()
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = textColor,
                fontSize = fontSize,
                fontWeight = fontWeight,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}



// общий раздел (приватность слева, авторизация справа)
@Composable
private fun TabGeneral() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var noGroupInvites by remember {
        mutableStateOf(sys_menu_socket_repository.noGroupInvites)
    }

    LaunchedEffect(sys_menu_socket_repository.noGroupInvites) {
        noGroupInvites = sys_menu_socket_repository.noGroupInvites
    }

    var isTokenRevealed by remember { mutableStateOf(false) }
    var isCopied by remember { mutableStateOf(false) }
    var isRenewLoading by remember { mutableStateOf(false) }
    var isClaimLoading by remember { mutableStateOf(false) }
    var hasAutoClaimAttempted by remember { mutableStateOf(false) }
    var lastClaimClickMs by remember { mutableLongStateOf(0L) }
    var showSwitchAuthDialog by remember { mutableStateOf(false) }
    var showLinkVkDialog by remember { mutableStateOf(false) }
    var isLinkingVk by remember { mutableStateOf(false) }
    var showDataDeletionDialog by remember { mutableStateOf(false) }
    var deleteTimerSeconds by remember { mutableIntStateOf(5) }
    var lastTokenRenewTimestamp by remember {
        mutableLongStateOf(AppSettings.getLong(context, "last_token_renew_timestamp", 0L))
    }

    // таймер перед подтверждением удаления данных
    LaunchedEffect(showDataDeletionDialog) {
        if (showDataDeletionDialog) {
            deleteTimerSeconds = 5
            while (deleteTimerSeconds > 0) {
                delay(1000)
                deleteTimerSeconds--
            }
        }
    }

    val isVkAcc = sys_menu_socket_repository.isVkAccount ||
        sys_menu_socket_repository.currentPlayerVkId.isNotBlank() ||
        AppSettings.getString(context, "auth_type", "").lowercase() == "vk"
    val currentToken = sys_menu_socket_repository.currentSessionToken.ifEmpty {
        AppSettings.getString(context, "session_token", "").ifEmpty {
            AppSettings.getString(context, "vk_auth_code", "N/A")
        }
    }
    val hasThirtyDayToken = sys_menu_socket_repository.isThirtyDayToken(currentToken)
    val hasEnoughScoreForToken = isVkAcc || sys_menu_socket_repository.currentPlayerScore >= sys_menu_socket_repository.minScoreForToken
    val isTokenRenewAllowed = !isVkAcc && sys_menu_socket_repository.mozhnoLiObnovlyatToken
    val now = System.currentTimeMillis()
    val isCooldownActive = (now - lastTokenRenewTimestamp) in 0 until 86_400_000L

    // автополучение токена для вк аккаунта
    LaunchedEffect(isVkAcc, hasThirtyDayToken) {
        if (isVkAcc && !hasThirtyDayToken && !isClaimLoading && !hasAutoClaimAttempted) {
            hasAutoClaimAttempted = true
            isClaimLoading = true
            try {
                sys_menu_socket_repository.claimThirtyDayTokenOnline(context)
            } finally {
                isClaimLoading = false
            }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // левая колонка: приватность
            Column(modifier = Modifier.weight(1f)) {
                // заголовок приватности
                ProfileCategoryTitle(Loc.tr(R.string.str_privatnost))

                // пункт запрета приглашений в группы
                SettingRow(Loc.tr(R.string.no_group)) {
                    CustomSwitch(noGroupInvites) { newVal ->
                        noGroupInvites = newVal
                        AppSettings.setBoolean(context, "no_group_invites", newVal)
                        coroutineScope.launch {
                            sys_menu_socket_repository.updateGroupInviteSettingOnline(newVal)
                        }
                    }
                }

                // пункт запрета заявок в друзья
                SettingRow(
                    title = Loc.tr(R.string.str_zapretit_zayavki_v_druzya),
                    drawBottomBorder = false,
                    isLocked = true
                ) {
                    CustomSwitch(checked = false, enabled = false)
                }
            }

            // правая колонка: авторизация (токен аккаунта и смена авторизации)
            Column(modifier = Modifier.weight(1f)) {
                // заголовок токена аккаунта
                ProfileCategoryTitle(Loc.tr(R.string.str_token_akkaunta))

                // карточка отображения или получения токена
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(AppColors.PanelDark)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // окно отображения токена
                    val displayToken = if (hasThirtyDayToken) {
                        currentToken.ifEmpty { "N/A" }
                    } else {
                        "????????????????-????????????????-????????????????-????????????????-????????????????-????????????????-????????????????-????????????????"
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { isTokenRevealed = !isTokenRevealed }
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayToken,
                            color = AppColors.White,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontWeight = if (isTokenRevealed) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = if (isTokenRevealed) Modifier else Modifier.blur(22.dp)
                        )
                    }

                    if (hasThirtyDayToken) {
                        // кнопки управления токеном
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // кнопка копирования токена
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .menuActionButtonStyle(enabled = !isCopied, shape = RoundedCornerShape(4.dp))
                                    .clickable(enabled = currentToken.isNotBlank() && currentToken != "N/A") {
                                        try {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val clip = ClipData.newPlainText("Auth Token", currentToken)
                                            clipboard?.setPrimaryClip(clip)
                                            isCopied = true
                                            GlobalToastManager.show(Loc.tr(R.string.str_skopirovano))
                                        } catch (e: Exception) {
                                            GlobalToastManager.show(Loc.tr(R.string.str_oshibka_kopirovaniya))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = null,
                                        tint = if (!isCopied) AppColors.White else AppColors.TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isCopied) Loc.tr(R.string.str_skopirovano) else Loc.tr(R.string.str_skopirovat),
                                        color = if (!isCopied) AppColors.White else AppColors.TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // кнопка обновления токена
                            val canRenew = isTokenRenewAllowed && !isCooldownActive && !isRenewLoading
                            val renewBtnBg = if (canRenew) AppColors.ActionButtonBg else AppColors.ActionButtonDisabledBg
                            val renewBtnBorder = if (canRenew) AppColors.ActionButtonBorder else AppColors.ActionButtonDisabledBorder
                            val renewBtnText = if (canRenew) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(renewBtnBg)
                                    .border(1.dp, renewBtnBorder, RoundedCornerShape(4.dp))
                                    .clickable(enabled = canRenew) {
                                        if (isVkAcc) {
                                            GlobalToastManager.show("Сессионный токен ВК нельзя обновить")
                                            return@clickable
                                        }
                                        if (!sys_menu_socket_repository.mozhnoLiObnovlyatToken) {
                                            GlobalToastManager.show(Loc.tr(R.string.str_obnovlenie_tokena_otklyucheno))
                                            return@clickable
                                        }
                                        val currentNow = System.currentTimeMillis()
                                        if (currentNow - lastTokenRenewTimestamp in 0 until 86_400_000L) {
                                            GlobalToastManager.show(Loc.tr(R.string.str_obnovlyat_token_mozhno_1_raz))
                                            return@clickable
                                        }
                                        coroutineScope.launch {
                                            isRenewLoading = true
                                            try {
                                                val res = sys_menu_socket_repository.renewTokenOnline(context)
                                                val updatedNow = System.currentTimeMillis()
                                                if (res == "SUCCESS") {
                                                    lastTokenRenewTimestamp = updatedNow
                                                    AppSettings.setLong(context, "last_token_renew_timestamp", updatedNow)
                                                    isCopied = false
                                                    GlobalToastManager.show(Loc.tr(R.string.str_token_uspeshno_obnovlen))
                                                } else if (res == "RENEW_TOKEN_ERROR:COOLDOWN" || res.contains("COOLDOWN")) {
                                                    lastTokenRenewTimestamp = updatedNow
                                                    AppSettings.setLong(context, "last_token_renew_timestamp", updatedNow)
                                                    GlobalToastManager.show(Loc.tr(R.string.str_obnovlyat_token_mozhno_1_raz))
                                                } else {
                                                    GlobalToastManager.show(sys_menu_socket_repository.translateError(res))
                                                }
                                            } finally {
                                                isRenewLoading = false
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (canRenew) {
                                        Icon(
                                            imageVector = Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            tint = renewBtnText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                    } else {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_lock),
                                            contentDescription = "Locked",
                                            tint = renewBtnText,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                    }
                                    Text(
                                        text = Loc.tr(R.string.refresh),
                                        color = renewBtnText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        // кнопка создания 30-дневного токена
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.TabInactive)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                .clickable(enabled = !isClaimLoading) {
                                    val clickNow = System.currentTimeMillis()
                                    if (isClaimLoading || (clickNow - lastClaimClickMs) in 0 until 1500L) {
                                        return@clickable
                                    }
                                    lastClaimClickMs = clickNow
                                    if (!hasEnoughScoreForToken) {
                                        GlobalToastManager.show(
                                            Loc.tr(
                                                R.string.str_nuzhno_minimum_ochkov_dlya_tokena,
                                                sys_menu_socket_repository.formattedMinScoreForToken
                                            )
                                        )
                                        return@clickable
                                    }
                                    val cdSec = sys_menu_socket_repository.getTokenClaimCooldownRemainingSec()
                                    if (cdSec > 0) {
                                        return@clickable
                                    }
                                    isClaimLoading = true
                                    coroutineScope.launch {
                                        try {
                                            val res = sys_menu_socket_repository.claimThirtyDayTokenOnline(context)
                                            if (res == "SUCCESS") {
                                                isCopied = false
                                                isTokenRevealed = false
                                                GlobalToastManager.show(Loc.tr(R.string.str_token_uspeshno_poluchen))
                                            } else {
                                                GlobalToastManager.show(sys_menu_socket_repository.translateError(res))
                                            }
                                        } finally {
                                            isClaimLoading = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = Loc.tr(R.string.str_poluchit_token),
                                    color = AppColors.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.VpnKey,
                                    contentDescription = "Token",
                                    tint = AppColors.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // заголовок блока авторизации
                ProfileCategoryTitle(Loc.tr(R.string.str_avtorizatsiya))

                // кнопка смены авторизации
                SettingsActionButton(
                    text = Loc.tr(R.string.str_smenit_avtorizatsiyu),
                    modifier = Modifier.fillMaxWidth(),
                    minHeight = 34.dp,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Login,
                            contentDescription = null,
                            tint = AppColors.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    showSwitchAuthDialog = true
                }

                Spacer(Modifier.height(8.dp))

                // нижний ряд кнопок: привязка вк и удаление данных
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // кнопка привязки вк
                    val canLinkVk = sys_menu_socket_repository.isVkAuthEnabled && !isVkAcc && !isLinkingVk
                    SettingsActionButton(
                        text = Loc.tr(R.string.str_privyazat_vk),
                        modifier = Modifier.weight(1f),
                        minHeight = 34.dp,
                        enabled = canLinkVk,
                        bgColor = if (canLinkVk) Color(0x590077FF) else Color(0x2E0077FF),
                        borderColor = if (canLinkVk) AppColors.Border else AppColors.Border.copy(alpha = 0.05f),
                        textColor = if (canLinkVk) AppColors.White else Color.White.copy(alpha = 0.4f),
                        trailingIcon = if (!sys_menu_socket_repository.isVkAuthEnabled || isVkAcc) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lock),
                                    contentDescription = "Locked",
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        } else null
                    ) {
                        if (canLinkVk) {
                            showLinkVkDialog = true
                        }
                    }

                    // кнопка удаления данных
                    SettingsActionButton(
                        text = Loc.tr(R.string.str_udalenie_dannyh),
                        modifier = Modifier.weight(1f),
                        minHeight = 34.dp,
                        bgColor = AppColors.ProfileDeleteBg,
                        borderColor = AppColors.ProfileDeleteBorder
                    ) {
                        showDataDeletionDialog = true
                    }
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }

    // диалог подтверждения смены авторизации
    if (showSwitchAuthDialog) {
        UnifiedDialog(
            title = Loc.tr(R.string.str_smenit_avtorizatsiyu),
            cancelText = Loc.tr(R.string.back),
            confirmText = Loc.tr(R.string.str_prodolzhit),
            content = {
                Text(
                    text = Loc.tr(R.string.str_vy_deystvitelno_hotite_smenit_avtorizatsiyu),
                    color = AppColors.TextMuted,
                    fontSize = 14.sp
                )
            },
            onConfirm = {
                showSwitchAuthDialog = false
                // сохранение токена перед выходом на экран авторизации
                val authType = AppSettings.getString(context, "auth_type", "").lowercase()
                val isCurrentGuest = !sys_menu_socket_repository.currentPlayerIsRegistered &&
                    !sys_menu_socket_repository.isVkAccount &&
                    !authType.startsWith("vk")
                val curTok = sys_menu_socket_repository.currentSessionToken.ifBlank {
                    AppSettings.getString(context, "session_token", "")
                }
                if (!sys_menu_socket_repository.isVkAccount && !authType.startsWith("vk") && curTok.isNotBlank()) {
                    AppSettings.setString(context, "saved_guest_token", curTok)
                }
                if (isCurrentGuest) {
                    val curPid = sys_menu_socket_repository.currentPlayerId.ifBlank {
                        AppSettings.getString(context, "driver_account_id", "")
                    }
                    if (curPid.isNotBlank()) AppSettings.setString(context, "saved_guest_id", curPid)
                } else {
                    AppSettings.setString(context, "saved_guest_id", "")
                    AppSettings.setString(context, "saved_guest_pass", "")
                }
                AppSettings.setString(context, "session_token", "")
                AppSettings.setString(context, "vk_auth_code", "")
                AppSettings.setString(context, "vk_user_id", "")
                AppSettings.setString(context, "auth_type", "")
                AppSettings.setString(context, "driver_account_id", "")
                GlobalReconnectState.isIntentionalDisconnect = true
                GlobalReconnectState.showConnectionErrorDialog = false
                GlobalReconnectState.isReconnecting = false
                sys_menu_socket_repository.resetSession()
                sys_menu_socket_repository.disconnectAndForceOffline()
                sys_menu_socket_repository.currentPlayerId = ""
                sys_menu_socket_repository.currentSessionToken = ""
                sys_menu_socket_repository.currentPlayerVkId = ""
                sys_menu_socket_repository.currentPlayerIsRegistered = false
                sys_menu_socket_repository.currentPlayerRegisteredLogin = ""

                GlobalReconnectState.requestSwitchToSplash = true
                (findActivity(context) as? system)?.scheduleHideSystemBars()
            },
            onDismiss = { showSwitchAuthDialog = false }
        )
    }

    // диалог удаления данных
    if (showDataDeletionDialog) {
        var isDeletingData by remember { mutableStateOf(false) }
        UnifiedDialog(
            title = Loc.tr(R.string.str_dialog_data_deletion_title),
            dialogWidthDp = 440,
            cancelText = Loc.tr(R.string.cancel),
            confirmText = Loc.tr(R.string.delete),
            confirmEnabled = deleteTimerSeconds == 0 && !isDeletingData,
            isLoading = isDeletingData,
            dismissOnConfirm = false,
            confirmContent = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Loc.tr(R.string.delete),
                        color = if (deleteTimerSeconds == 0) Color.White else Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (deleteTimerSeconds > 0) {
                        Text(
                            text = "($deleteTimerSeconds)",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = 38.dp)
                        )
                    }
                }
            },
            content = {
                val isImmediateDeletion = sys_menu_socket_repository.currentPlayerScore < 10000
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (isImmediateDeletion) Loc.tr(R.string.str_dialog_data_deletion_msg_immediate) else Loc.tr(R.string.str_dialog_data_deletion_msg),
                        color = AppColors.TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 19.sp
                    )
                }
            },
            onConfirm = {
                if (isDeletingData) return@UnifiedDialog
                coroutineScope.launch {
                    isDeletingData = true
                    try {
                        val res = sys_menu_socket_repository.udaleniyeDataDannihIgrokaOnline(context)
                        if (res == "SUCCESS") {
                            showDataDeletionDialog = false
                            AppSettings.setString(context, "session_token", "")
                            AppSettings.setString(context, "saved_guest_token", "")
                            AppSettings.setString(context, "saved_guest_id", "")
                            AppSettings.setString(context, "saved_guest_pass", "")
                            AppSettings.setString(context, "vk_auth_code", "")
                            AppSettings.setString(context, "vk_user_id", "")
                            AppSettings.setString(context, "auth_type", "")
                            AppSettings.setString(context, "driver_account_id", "")
                            AppSettings.setString(context, "driver_password", "")
                            AppSettings.setString(context, "driver_registered_login", "")
                            AppSettings.setString(context, "driver_is_registered", "false")
                            AppSettings.setBoolean(context, "terms_accepted", false)
                            GlobalReconnectState.isIntentionalDisconnect = true
                            GlobalReconnectState.showConnectionErrorDialog = false
                            GlobalReconnectState.isReconnecting = false
                            sys_menu_socket_repository.resetSession()
                            sys_menu_socket_repository.disconnectAndForceOffline()
                            sys_menu_socket_repository.currentPlayerId = ""
                            sys_menu_socket_repository.currentSessionToken = ""
                            sys_menu_socket_repository.currentPlayerVkId = ""
                            sys_menu_socket_repository.currentPlayerIsRegistered = false
                            sys_menu_socket_repository.currentPlayerRegisteredLogin = ""
                            GlobalReconnectState.requestSwitchToSplash = true
                            (findActivity(context) as? system)?.scheduleHideSystemBars()
                        } else if (res == "OFFLINE") {
                            GlobalToastManager.show(Loc.tr(R.string.str_net_podklyucheniya_k_serveru_02ed))
                        } else {
                            GlobalToastManager.show(sys_menu_socket_repository.translateError(res))
                        }
                    } finally {
                        isDeletingData = false
                    }
                }
            },
            onDismiss = {
                if (!isDeletingData) {
                    showDataDeletionDialog = false
                }
            }
        )
    }

    // диалог привязки вк
    if (showLinkVkDialog) {
        UnifiedDialog(
            title = Loc.tr(R.string.str_dialog_link_vk_title),
            cancelText = Loc.tr(R.string.cancel),
            confirmText = Loc.tr(R.string.str_prodolzhit),
            content = {
                Text(
                    text = Loc.tr(R.string.str_dialog_link_vk_msg),
                    color = AppColors.TextMuted,
                    fontSize = 14.sp
                )
            },
            onConfirm = {
                showLinkVkDialog = false
                if (!sys_menu_socket_repository.isVkAuthEnabled || isVkAcc || isLinkingVk) {
                    return@UnifiedDialog
                }
                isLinkingVk = true
                try {
                    VKID.instance.authorize(
                        lifecycleOwner = lifecycleOwner,
                        callback = object : VKIDAuthCallback {
                            override fun onAuth(accessToken: AccessToken) {
                                coroutineScope.launch {
                                    try {
                                        val tokenStr = "${accessToken.token}:${accessToken.userID}"
                                        val res = sys_menu_socket_repository.linkVkAccountOnline(
                                            authCodeOrToken = tokenStr,
                                            context = context
                                        )
                                        if (res == "SUCCESS" || res.startsWith("VK_LINK_OK")) {
                                            isCopied = false
                                            isTokenRevealed = false
                                            GlobalToastManager.show(Loc.tr(R.string.str_vk_successfully_linked))
                                        } else {
                                            GlobalToastManager.show(sys_menu_socket_repository.translateError(res))
                                        }
                                    } finally {
                                        isLinkingVk = false
                                    }
                                }
                            }

                            override fun onFail(fail: VKIDAuthFail) {
                                isLinkingVk = false
                                if (fail is VKIDAuthFail.Canceled) {
                                    return
                                }
                                openVkBrowserAuth(context, mode = "link")
                            }
                        }
                    )
                } catch (e: Exception) {
                    isLinkingVk = false
                    openVkBrowserAuth(context, mode = "link")
                }
            },
            onDismiss = {
                showLinkVkDialog = false
            }
        )
    }
}
