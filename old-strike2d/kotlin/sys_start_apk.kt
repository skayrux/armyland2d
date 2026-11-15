package skayrux.shootout2d

import com.vk.id.VKID
import com.vk.id.AccessToken
import com.vk.id.VKIDAuthFail
import com.vk.id.auth.VKIDAuthCallback
import com.vk.id.auth.VKIDAuthParams

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SplashPhase {
    BLACK,
    SKAYRUX,
    LOADING,
    TERMS_AGREEMENT,
    OFFLINE_ERROR,
    VERSION_UPDATE,
    AUTH_CHOICE,
    FADE_OUT
}

// данные одноразового диплинка авторизации вк
data class VkDeeplinkPayload(
    val codeOrToken: String,
    val codeVerifier: String,
    val vkDeviceId: String,
    val redirectUri: String,
    val mode: String
)

// открытие авторизации в браузере с pkce и csrf-state
fun openVkBrowserAuth(context: android.content.Context, mode: String = "login") {
    val clientId = "54814581"
    val redirectUri = "vk54814581://vk.ru/blank.html"
    val sr = java.security.SecureRandom()
    val verifierBytes = ByteArray(48)
    sr.nextBytes(verifierBytes)
    val verifier = verifierBytes.joinToString("") { "%02x".format(it) }
    val stateBytes = ByteArray(32)
    sr.nextBytes(stateBytes)
    val state = stateBytes.joinToString("") { "%02x".format(it) }
    val nowMs = System.currentTimeMillis()

    AppSettings.setString(context, "vk_code_verifier", verifier)
    AppSettings.setString(context, "vk_oauth_state", state)
    AppSettings.setLong(context, "vk_oauth_state_time", nowMs)
    AppSettings.setString(context, "vk_oauth_mode", mode)

    try {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = android.util.Base64.encodeToString(
            digest,
            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        )

        val authUrl = "https://id.vk.ru/auth?client_id=$clientId&redirect_uri=${android.net.Uri.encode(redirectUri)}&response_type=code&code_challenge=$challenge&code_challenge_method=s256&state=${android.net.Uri.encode(state)}&scope=vkid.personal_info&sdk_type=vkid&v=2.7.3"

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(authUrl)).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            addCategory(android.content.Intent.CATEGORY_BROWSABLE)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallbackUrl = "https://oauth.vk.com/authorize?client_id=$clientId&redirect_uri=${android.net.Uri.encode(redirectUri)}&response_type=code&state=${android.net.Uri.encode(state)}&scope=offline,email&v=5.131"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fallbackUrl)).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                addCategory(android.content.Intent.CATEGORY_BROWSABLE)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}

// одноразовое извлечение и валидация диплинка вк (проверка state, ttl 5 минут и белого списка redirect_uri)
fun parseAndConsumeVkDeeplink(intentUri: android.net.Uri?, context: android.content.Context): VkDeeplinkPayload? {
    if (intentUri == null) return null
    val url = intentUri.toString()
    if (!url.startsWith("vk54814581://") && !url.contains("code=") && !url.contains("access_token=")) {
        return null
    }
    val fragment = if (url.contains("#")) url.substringAfter("#") else ""
    val fragParams = if (fragment.isNotEmpty()) {
        fragment.split("&").associate {
            it.substringBefore("=") to it.substringAfter("=", "")
        }
    } else emptyMap()

    val code = intentUri.getQueryParameter("code")
        ?: fragParams["code"]
        ?: if (url.contains("code=")) url.substringAfter("code=").substringBefore("&").substringBefore("#") else null
    val token = intentUri.getQueryParameter("access_token") ?: fragParams["access_token"]
    val vkUserId = (intentUri.getQueryParameter("user_id") ?: fragParams["user_id"]).orEmpty().trim()
    val vkDevId = intentUri.getQueryParameter("device_id")
        ?: fragParams["device_id"]
        ?: Regex("device_id=([^&#]+)").find(url)?.groupValues?.get(1)
        ?: ""
    val returnedState = intentUri.getQueryParameter("state")
        ?: fragParams["state"]
        ?: Regex("state=([^&#]+)").find(url)?.groupValues?.get(1)
        ?: ""

    // чтение и немедленное уничтожение одноразовых pkce/state параметров
    val savedVerifier = AppSettings.getString(context, "vk_code_verifier", "")
    val savedState = AppSettings.getString(context, "vk_oauth_state", "")
    val savedStateTime = AppSettings.getLong(context, "vk_oauth_state_time", 0L)
    val savedMode = AppSettings.getString(context, "vk_oauth_mode", "login").ifBlank { "login" }

    AppSettings.setString(context, "vk_code_verifier", "")
    AppSettings.setString(context, "vk_oauth_state", "")
    AppSettings.setLong(context, "vk_oauth_state_time", 0L)
    AppSettings.setString(context, "vk_oauth_mode", "")

    val codeOrTok = when {
        !code.isNullOrBlank() -> code.trim()
        !token.isNullOrBlank() -> if (vkUserId.isNotBlank() && vkUserId.all { it.isDigit() }) "${token.trim()}:$vkUserId" else token.trim()
        else -> ""
    }
    if (codeOrTok.isEmpty()) return null

    // строгая проверка state и времени жизни (5 минут)
    val now = System.currentTimeMillis()
    if (savedState.isBlank() || returnedState != savedState || (now - savedStateTime) !in 0..300_000L) {
        return null
    }

    val detectedRedirect = if (url.startsWith("vk54814581://vk.com/blank.html") || url.contains("vk.com")) {
        "vk54814581://vk.com/blank.html"
    } else {
        "vk54814581://vk.ru/blank.html"
    }

    return VkDeeplinkPayload(
        codeOrToken = codeOrTok,
        codeVerifier = savedVerifier,
        vkDeviceId = vkDevId,
        redirectUri = detectedRedirect,
        mode = savedMode
    )
}

@Composable
fun start_apk(
    currentIntent: android.content.Intent? = null,
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val skayruxAlpha = remember { Animatable(0f) }
    val loadingAlpha = remember { Animatable(0f) }
    val mainAlpha = remember { Animatable(1f) }
    val authBgAlpha = remember { Animatable(0f) }
    val authUiAlpha = remember { Animatable(0f) }
    var currentPhase by remember { mutableStateOf(SplashPhase.BLACK) }

    var showTokenDialog by remember { mutableStateOf(false) }
    var showVkSwitchWarningDialog by remember { mutableStateOf(false) }
    var vkInputCode by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    var isAuthCooldown by remember { mutableStateOf(false) }
    var isAuthLoading by remember { mutableStateOf(false) }
    var isAcceptingTerms by remember { mutableStateOf(false) }

    // проверка блокировки кнопок входа
    val isAuthBlocked = isAuthCooldown || isAuthLoading

    // плавный переход к окну ошибки подключения
    suspend fun showOfflineErrorWithFade() {
        if (loadingAlpha.value > 0f) {
            loadingAlpha.animateTo(0f, animationSpec = tween(250, easing = LinearEasing))
        }
        authBgAlpha.snapTo(0f)
        currentPhase = SplashPhase.OFFLINE_ERROR
        delay(120)
        authBgAlpha.animateTo(1f, animationSpec = tween(1600, easing = FastOutSlowInEasing))
    }

    // плавный переход к окну авторизации
    suspend fun showAuthChoiceWithFade() {
        if (loadingAlpha.value > 0f) {
            loadingAlpha.animateTo(0f, animationSpec = tween(250, easing = LinearEasing))
        }
        val bgAlreadyVisible = (currentPhase == SplashPhase.OFFLINE_ERROR || currentPhase == SplashPhase.AUTH_CHOICE || currentPhase == SplashPhase.TERMS_AGREEMENT) && authBgAlpha.value >= 0.95f
        if (!bgAlreadyVisible) {
            authBgAlpha.snapTo(0f)
        }
        authUiAlpha.snapTo(0f)
        currentPhase = SplashPhase.AUTH_CHOICE
        if (!bgAlreadyVisible) {
            delay(120)
            coroutineScope.launch {
                authUiAlpha.animateTo(1f, animationSpec = tween(1600, easing = FastOutSlowInEasing))
            }
            authBgAlpha.animateTo(1f, animationSpec = tween(1600, easing = FastOutSlowInEasing))
        } else {
            authUiAlpha.animateTo(1f, animationSpec = tween(1600, easing = FastOutSlowInEasing))
        }
    }

    // плавный переход к окну принятия соглашения
    suspend fun showTermsAgreementWithFade() {
        if (loadingAlpha.value > 0f) {
            loadingAlpha.animateTo(0f, animationSpec = tween(250, easing = LinearEasing))
        }
        authBgAlpha.snapTo(0f)
        currentPhase = SplashPhase.TERMS_AGREEMENT
        delay(120)
        authBgAlpha.animateTo(1f, animationSpec = tween(1600, easing = FastOutSlowInEasing))
    }

    // выполнение серверных проверок (версия, бан, токен) только после принятия соглашения
    suspend fun performOnlineChecksAndProceed() {
        var checkResult: StartupCheckResult? = null
        val netStartTime = android.os.SystemClock.elapsedRealtime()
        while (android.os.SystemClock.elapsedRealtime() - netStartTime < 5000L) {
            val elapsed = android.os.SystemClock.elapsedRealtime() - netStartTime
            val rem = (5000L - elapsed).coerceAtLeast(1000L)
            val attemptRes = kotlinx.coroutines.withTimeoutOrNull(minOf(3500L, rem)) {
                sys_menu_socket_repository.performClientStartupCheck(context)
            }
            if (attemptRes is StartupCheckResult.Ok || attemptRes is StartupCheckResult.Banned || attemptRes is StartupCheckResult.VersionMismatch || sys_menu_socket_repository.isVersionMismatch) {
                checkResult = attemptRes
                break
            }
            val elapsedAfter = android.os.SystemClock.elapsedRealtime() - netStartTime
            if (elapsedAfter >= 5000L) break
            delay(minOf(400L, 5000L - elapsedAfter))
        }

        val finalCheck = checkResult
        if (finalCheck is StartupCheckResult.VersionMismatch || sys_menu_socket_repository.isVersionMismatch) {
            sys_menu_socket_repository.disconnectAndForceOffline()
            if (loadingAlpha.value > 0f) {
                loadingAlpha.animateTo(0f, animationSpec = tween(250, easing = LinearEasing))
            }
            if (authBgAlpha.value < 0.95f) {
                authBgAlpha.snapTo(0f)
                currentPhase = SplashPhase.VERSION_UPDATE
                delay(120)
                authBgAlpha.animateTo(1f, animationSpec = tween(1600, easing = FastOutSlowInEasing))
            } else {
                currentPhase = SplashPhase.VERSION_UPDATE
            }
            return
        }

        if (finalCheck is StartupCheckResult.Banned) {
            GlobalSecurityDialogState.trigger(finalCheck.accountId, finalCheck.issuedTime)
        } else if (finalCheck is StartupCheckResult.Ok) {
            GlobalSecurityDialogState.clearLocalBan(context)
        } else {
            val isLocallyBanned = AppSettings.getBoolean(context, "is_banned_locally", false)
            if (isLocallyBanned) {
                val savedAcc = AppSettings.getString(context, "banned_account_id", "N/A")
                val savedIssued = AppSettings.getString(context, "banned_issued_time", "")
                GlobalSecurityDialogState.trigger(savedAcc, savedIssued)
            }
        }

        val isLocallyBanned = AppSettings.getBoolean(context, "is_banned_locally", false)
        if (GlobalSecurityDialogState.isVisible || GlobalTempBanDialogState.isVisible || isLocallyBanned) {
            if (loadingAlpha.value > 0f) {
                loadingAlpha.animateTo(0f, animationSpec = tween(250, easing = LinearEasing))
            }
            if (currentPhase == SplashPhase.TERMS_AGREEMENT) {
                authUiAlpha.snapTo(0f)
                currentPhase = SplashPhase.AUTH_CHOICE
            }
            return
        }

        if (finalCheck == null || finalCheck is StartupCheckResult.Offline) {
            sys_menu_socket_repository.disconnectAndForceOffline()
            if (authBgAlpha.value >= 0.95f) {
                currentPhase = SplashPhase.OFFLINE_ERROR
            } else {
                showOfflineErrorWithFade()
            }
            return
        }

        // если сервер онлайн, но соглашение еще не принято — показываем окно соглашения
        val termsAccepted = AppSettings.getBoolean(context, "terms_accepted", false)
        if (!termsAccepted) {
            showTermsAgreementWithFade()
            return
        }

        val savedToken = AppSettings.getString(context, "session_token", "").ifEmpty {
            AppSettings.getString(context, "vk_auth_code", "")
        }

        if (savedToken.isEmpty()) {
            sys_menu_socket_repository.disconnectAndForceOffline()
            sys_menu_socket_repository.currentPlayerId = ""
            sys_menu_socket_repository.currentSessionToken = ""
            showAuthChoiceWithFade()
        } else {
            val res = sys_menu_socket_repository.loginWithTokenOnline(savedToken, context)
            if (res == "SUCCESS") {
                currentPhase = SplashPhase.FADE_OUT
                mainAlpha.animateTo(0f, animationSpec = tween(500, easing = LinearEasing))
                onSplashFinished()
            } else if (res.startsWith("TEMP_BAN") || res.startsWith("CHECK_BAN") || res == "BANNED") {
                showAuthChoiceWithFade()
            } else if (res == "OFFLINE") {
                sys_menu_socket_repository.disconnectAndForceOffline()
                if (authBgAlpha.value >= 0.95f) {
                    currentPhase = SplashPhase.OFFLINE_ERROR
                } else {
                    showOfflineErrorWithFade()
                }
            } else {
                // очистка сохраненной сессии и показ всплывающего тоста при блокировке или удалении
                if (res.startsWith("ACCOUNT_UDALENIYE|")) {
                    val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                    GlobalToastManager.show(Loc.tr(R.string.str_account_login_unavailable_with_id, accId))
                } else if (res.startsWith("ACCOUNT_BANNED|")) {
                    val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                    GlobalToastManager.show(Loc.tr(R.string.str_account_blocked_with_id, accId))
                }
                AppSettings.setString(context, "session_token", "")
                AppSettings.setString(context, "vk_auth_code", "")
                AppSettings.setString(context, "auth_type", "")
                AppSettings.setString(context, "driver_account_id", "")
                sys_menu_socket_repository.disconnectAndForceOffline()
                sys_menu_socket_repository.currentPlayerId = ""
                sys_menu_socket_repository.currentSessionToken = ""
                showAuthChoiceWithFade()
            }
        }
    }

    // запуск кулдауна кнопок входа
    fun triggerAuthCooldown() {
        isAuthCooldown = true
        coroutineScope.launch {
            delay(2000L)
            isAuthCooldown = false
        }
    }

    // запуск авторизации через вк
    fun launchVkAuth() {
        triggerAuthCooldown()
        isAuthLoading = true
        statusText = Loc.tr(R.string.str_vhod_cherez_vk)
        try {
            VKID.instance.authorize(
                lifecycleOwner = lifecycleOwner,
                callback = object : VKIDAuthCallback {
                    override fun onAuth(accessToken: AccessToken) {
                        val tokenStr = "${accessToken.token}:${accessToken.userID}"
                        statusText = Loc.tr(R.string.str_proverka)
                        coroutineScope.launch {
                            try {
                                val res = sys_menu_socket_repository.loginWithVkOnline(tokenStr, context)
                                if (res.startsWith("VK_LOGIN_OK") || res == "SUCCESS") {
                                    statusText = ""
                                    mainAlpha.animateTo(0f, animationSpec = tween(500, easing = LinearEasing))
                                    onSplashFinished()
                                } else if (res == "OFFLINE") {
                                    statusText = ""
                                    currentPhase = SplashPhase.OFFLINE_ERROR
                                } else if (res.startsWith("TEMP_BAN")) {
                                    statusText = ""
                                    val parts = res.split("|")
                                    val acc = parts.getOrNull(1) ?: "N/A"
                                    val reason = parts.getOrNull(2) ?: ""
                                    val exp = parts.getOrNull(3)?.toLongOrNull() ?: 0L
                                    GlobalTempBanDialogState.trigger(acc, reason, exp)
                                } else if (res.startsWith("CHECK_BAN")) {
                                    statusText = ""
                                    val parts = res.split("|")
                                    val issued = parts.getOrNull(2) ?: GlobalSecurityDialogState.formatIssuedTimeNow()
                                    val acc = parts.getOrNull(3) ?: "N/A"
                                    GlobalSecurityDialogState.trigger(acc, issued)
                                } else if (res.startsWith("ACCOUNT_UDALENIYE|")) {
                                    statusText = ""
                                    val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                                    GlobalToastManager.show(Loc.tr(R.string.str_account_login_unavailable_with_id, accId))
                                } else if (res.startsWith("ACCOUNT_BANNED|")) {
                                    statusText = ""
                                    val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                                    GlobalToastManager.show(Loc.tr(R.string.str_account_blocked_with_id, accId))
                                } else if (res.startsWith("VK_LOGIN_ERROR:DISABLED")) {
                                    statusText = Loc.tr(R.string.str_vhod_cherez_vk_otklyuchen)
                                } else {
                                    statusText = sys_menu_socket_repository.translateError(res)
                                }
                            } finally {
                                isAuthLoading = false
                            }
                        }
                    }

                    override fun onFail(fail: VKIDAuthFail) {
                        isAuthLoading = false
                        openVkBrowserAuth(context)
                    }
                }
            )
        } catch (e: Exception) {
            isAuthLoading = false
            openVkBrowserAuth(context)
        }
    }

    // обработка диплинка авторизации вк
    LaunchedEffect(currentIntent) {
        val payload = parseAndConsumeVkDeeplink(currentIntent?.data, context)
        if (payload != null) {
            currentIntent?.data = null
            statusText = Loc.tr(R.string.str_proverka)
            val res = sys_menu_socket_repository.loginWithVkOnline(
                authCodeOrToken = payload.codeOrToken,
                context = context,
                codeVerifier = payload.codeVerifier,
                vkDeviceId = payload.vkDeviceId,
                redirectUri = payload.redirectUri
            )
            if (res.startsWith("VK_LOGIN_OK") || res == "SUCCESS") {
                statusText = ""
                currentPhase = SplashPhase.FADE_OUT
                mainAlpha.animateTo(0f, animationSpec = tween(400, easing = LinearEasing))
                onSplashFinished()
            } else if (res == "OFFLINE") {
                statusText = ""
                currentPhase = SplashPhase.OFFLINE_ERROR
            } else if (res == "VK_LOGIN_ERROR:DISABLED") {
                statusText = Loc.tr(R.string.str_vhod_cherez_vk_otklyuchen)
            } else if (res.startsWith("TEMP_BAN")) {
                statusText = ""
                val parts = res.split("|")
                val acc = parts.getOrNull(1) ?: "N/A"
                val reason = parts.getOrNull(2) ?: ""
                val exp = parts.getOrNull(3)?.toLongOrNull() ?: 0L
                GlobalTempBanDialogState.trigger(acc, reason, exp)
            } else if (res.startsWith("CHECK_BAN")) {
                statusText = ""
                val parts = res.split("|")
                val issued = parts.getOrNull(2) ?: GlobalSecurityDialogState.formatIssuedTimeNow()
                val acc = parts.getOrNull(3) ?: "N/A"
                GlobalSecurityDialogState.trigger(acc, issued)
            } else if (res.startsWith("ACCOUNT_UDALENIYE|")) {
                statusText = ""
                val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                GlobalToastManager.show(Loc.tr(R.string.str_account_login_unavailable_with_id, accId))
            } else if (res.startsWith("ACCOUNT_BANNED|")) {
                statusText = ""
                val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                GlobalToastManager.show(Loc.tr(R.string.str_account_blocked_with_id, accId))
            } else {
                statusText = sys_menu_socket_repository.translateError(res)
            }
        }
    }

    LaunchedEffect(Unit) {
        LiveBackgroundState.isMenuVisible = true
        if (GlobalTempBanDialogState.skipSplashToAuth) {
            GlobalTempBanDialogState.skipSplashToAuth = false
            mainAlpha.snapTo(1f)
            val startupRes = kotlinx.coroutines.withTimeoutOrNull(4000) {
                sys_menu_socket_repository.performClientStartupCheck(context)
            }
            if (startupRes is StartupCheckResult.VersionMismatch || sys_menu_socket_repository.isVersionMismatch) {
                sys_menu_socket_repository.disconnectAndForceOffline()
                currentPhase = SplashPhase.VERSION_UPDATE
                return@LaunchedEffect
            }
            if (startupRes == null || startupRes is StartupCheckResult.Offline) {
                showOfflineErrorWithFade()
            } else {
                val termsAccepted = AppSettings.getBoolean(context, "terms_accepted", false)
                if (!termsAccepted) {
                    showTermsAgreementWithFade()
                } else {
                    showAuthChoiceWithFade()
                }
            }
            return@LaunchedEffect
        }

        // загрузка настроек и игровых ресурсов
        val preloadingJob = launch {
            if (UiGlyphMetricTable.syncFontKerningMetrics(context) != 90) {
                UiGlyphMetricTable.terminateOnInvalidMetrics(context)
                return@launch
            }
            AppSettings.ensureSettingsFile(context)
            ResourceCache.preload(context)
            try {
                sys_menu_socket_repository.javaClass
            } catch (e: Exception) {}
        }

        delay(200)
        skayruxAlpha.snapTo(0f)
        currentPhase = SplashPhase.SKAYRUX
        
        skayruxAlpha.animateTo(1f, animationSpec = tween(1300, easing = LinearEasing))
        delay(200)
        skayruxAlpha.animateTo(0f, animationSpec = tween(800, easing = LinearEasing))
        
        currentPhase = SplashPhase.LOADING
        loadingAlpha.animateTo(0.99f, animationSpec = tween(1200, easing = FastOutSlowInEasing))

        val minLoadingTime = launch {
            delay(1500)
        }

        // сперва дожидаемся полной загрузки ресурсов
        preloadingJob.join()
        minLoadingTime.join()

        // проверка подключения к серверу первым делом перед окном соглашения
        performOnlineChecksAndProceed()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Black)
            .graphicsLayer { alpha = mainAlpha.value },
        contentAlignment = Alignment.Center
    ) {
        if (currentPhase == SplashPhase.SKAYRUX) {
            Text(
                text = "SKAYRUX & Victor Cager",
                color = AppColors.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 12.sp,
                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily,
                modifier = Modifier.graphicsLayer { alpha = skayruxAlpha.value }
            )
        }

        if (currentPhase == SplashPhase.LOADING) {
            SmoothLoadingText(
                alpha = 1f,
                modifier = Modifier.graphicsLayer { alpha = loadingAlpha.value }
            )
        }

        if (currentPhase == SplashPhase.TERMS_AGREEMENT || currentPhase == SplashPhase.OFFLINE_ERROR || currentPhase == SplashPhase.AUTH_CHOICE || currentPhase == SplashPhase.VERSION_UPDATE) {
            val savedGuestTokForCheck = AppSettings.getString(context, "saved_guest_token", "").ifBlank {
                AppSettings.getString(context, "session_token", "")
            }
            val hasValidSavedGuestToken = savedGuestTokForCheck.length >= 16 && (
                sys_menu_socket_repository.isThirtyDayToken(savedGuestTokForCheck) ||
                sys_menu_socket_repository.isSevenDayGuestAuthEnabled
            )
            val vkEnabled = sys_menu_socket_repository.isVkAuthEnabled
            val guestEnabled = sys_menu_socket_repository.isGuestAuthEnabled || hasValidSavedGuestToken
            val tokenEnabled = sys_menu_socket_repository.isTokenAuthEnabled
            val canClickVk = vkEnabled && !isAuthBlocked
            val canClickGuest = guestEnabled && !isAuthBlocked
            val canClickToken = tokenEnabled && !isAuthBlocked

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.Black)
            ) {
                // фоновое изображение с оптимизированным плавным появлением из темноты
                Box(modifier = Modifier.fillMaxSize()) {
                    StaticBackground()
                    val videoPath = LiveBackgroundState.videoPath
                    val isVideoValid = remember(videoPath) {
                        videoPath.isNotEmpty() && java.io.File(videoPath).exists()
                    }
                    if (isVideoValid) {
                        VideoBackground(videoPath, LiveBackgroundState.soundEnabled, LiveBackgroundState.volume)
                    }
                    MenuSmokeOverlay()
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val overlayAlpha = (1f - authBgAlpha.value).coerceIn(0f, 1f)
                            if (overlayAlpha > 0.001f) {
                                drawRect(color = AppColors.Black, alpha = overlayAlpha)
                            }
                        }
                )

                if (currentPhase == SplashPhase.AUTH_CHOICE) {
                    // плашки выбора авторизации внизу
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { alpha = authUiAlpha.value }
                            .padding(bottom = 20.dp, start = 12.dp, end = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            modifier = Modifier.wrapContentWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // кнопка входа вк
                            Box(
                                modifier = Modifier
                                    .width(190.dp)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (vkEnabled) Color(0x590077FF) else Color(0x2E0077FF))
                                    .border(1.dp, if (vkEnabled) AppColors.Border else AppColors.Border.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                    .clickable(enabled = canClickVk) {
                                        if (isAuthBlocked) return@clickable
                                        val savedGuestToken = AppSettings.getString(context, "saved_guest_token", "")
                                        val savedGuestId = AppSettings.getString(context, "saved_guest_id", "")
                                        if (savedGuestToken.isNotBlank() || savedGuestId.isNotBlank()) {
                                            showVkSwitchWarningDialog = true
                                        } else {
                                            launchVkAuth()
                                        }
                                    }
                            ) {
                                Text(
                                    text = "VK",
                                    color = if (vkEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 10.dp)
                                )
                                Text(
                                    text = Loc.tr(R.string.str_vhod_cherez_vk),
                                    color = if (vkEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            // кнопка входа гостя
                            Box(
                                modifier = Modifier
                                    .width(190.dp)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (guestEnabled) AppColors.PanelDark else AppColors.PanelDark.copy(alpha = 0.2f))
                                    .border(1.dp, if (guestEnabled) AppColors.Border else AppColors.Border.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                    .clickable(enabled = canClickGuest) {
                                        if (isAuthBlocked) return@clickable
                                        triggerAuthCooldown()
                                        isAuthLoading = true
                                        statusText = Loc.tr(R.string.str_proverka)
                                        coroutineScope.launch {
                                            try {
                                                val res = sys_menu_socket_repository.loginAsGuestOnline(context)
                                                if (res == "SUCCESS") {
                                                    statusText = ""
                                                    currentPhase = SplashPhase.FADE_OUT
                                                    mainAlpha.animateTo(0f, animationSpec = tween(400, easing = LinearEasing))
                                                    onSplashFinished()
                                                } else if (res == "OFFLINE") {
                                                    statusText = ""
                                                    currentPhase = SplashPhase.OFFLINE_ERROR
                                                } else if (res == "GUEST_ERROR:DISABLED") {
                                                    statusText = Loc.tr(R.string.str_vhod_dlya_gostey_otklyuchen)
                                                } else if (res.startsWith("TEMP_BAN")) {
                                                    statusText = ""
                                                    val parts = res.split("|")
                                                    val acc = parts.getOrNull(1) ?: "N/A"
                                                    val reason = parts.getOrNull(2) ?: ""
                                                    val exp = parts.getOrNull(3)?.toLongOrNull() ?: 0L
                                                    GlobalTempBanDialogState.trigger(acc, reason, exp)
                                                } else if (res.startsWith("CHECK_BAN")) {
                                                    statusText = ""
                                                    val parts = res.split("|")
                                                    val issued = parts.getOrNull(2) ?: GlobalSecurityDialogState.formatIssuedTimeNow()
                                                    val acc = parts.getOrNull(3) ?: "N/A"
                                                    GlobalSecurityDialogState.trigger(acc, issued)
                                                } else if (res.startsWith("ACCOUNT_UDALENIYE|")) {
                                                    statusText = ""
                                                    val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                                                    GlobalToastManager.show(Loc.tr(R.string.str_account_login_unavailable_with_id, accId))
                                                } else if (res.startsWith("ACCOUNT_BANNED|")) {
                                                    statusText = ""
                                                    val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                                                    GlobalToastManager.show(Loc.tr(R.string.str_account_blocked_with_id, accId))
                                                } else {
                                                    statusText = sys_menu_socket_repository.translateError(res)
                                                }
                                            } finally {
                                                isAuthLoading = false
                                            }
                                        }
                                    }
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Login,
                                    contentDescription = "Guest",
                                    tint = if (guestEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 10.dp)
                                        .size(15.dp)
                                )
                                Text(
                                    text = Loc.tr(R.string.str_igrat_kak_gost),
                                    color = if (guestEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // кнопка входа по токену
                        Box(
                            modifier = Modifier
                                .width(190.dp)
                                .height(30.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (tokenEnabled) AppColors.PanelDark else AppColors.PanelDark.copy(alpha = 0.2f))
                                .border(1.dp, if (tokenEnabled) AppColors.Border else AppColors.Border.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                                .clickable(enabled = canClickToken) {
                                    if (isAuthBlocked) return@clickable
                                    triggerAuthCooldown()
                                    showTokenDialog = true
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VpnKey,
                                contentDescription = "Token",
                                tint = if (tokenEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 10.dp)
                                    .size(15.dp)
                            )
                            Text(
                                text = Loc.tr(R.string.str_vhod_cherez_token),
                                color = if (tokenEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        // фиксированный контейнер под текстом статуса
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(25.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (statusText.isNotEmpty()) {
                                // цвет текста статуса
                                val isError = statusText.contains("ошибка", ignoreCase = true) ||
                                              statusText.contains("error", ignoreCase = true) ||
                                              statusText.contains("отключен", ignoreCase = true) ||
                                              statusText.contains("disabled", ignoreCase = true)
                                val isYellow = !isError && (
                                    statusText == Loc.tr(R.string.str_proverka) ||
                                    statusText == Loc.tr(R.string.str_token_avtorizatsii_ne_nayden) ||
                                    statusText == Loc.tr(R.string.str_vhod_cherez_vk) ||
                                    statusText.contains("не найден", ignoreCase = true) ||
                                    statusText.contains("not found", ignoreCase = true) ||
                                    statusText.contains("проверка", ignoreCase = true) ||
                                    statusText.contains("checking", ignoreCase = true) ||
                                    statusText.contains("вход", ignoreCase = true) ||
                                    statusText.contains("входим", ignoreCase = true) ||
                                    statusText.contains("vk", ignoreCase = true) ||
                                    statusText.contains("вк", ignoreCase = true)
                                )
                                Text(
                                    text = statusText,
                                    color = if (isYellow) Color.Yellow else Color(0xFFFF4D4D),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // диалоговое окно лицензионного соглашения и политики конфиденциальности
            if (currentPhase == SplashPhase.TERMS_AGREEMENT) {
                val openExternalDocument: (String) -> Unit = { docUrl ->
                    try {
                        val browserIntent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(docUrl)
                        ).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {}
                }

                UnifiedDialog(
                    title = Loc.tr(R.string.str_preduprezhdenie),
                    dialogWidthDp = 560,
                    showCancelButton = true,
                    cancelText = Loc.tr(R.string.str_terms_btn_decline),
                    confirmText = Loc.tr(R.string.str_terms_btn_accept),
                    confirmEnabled = !isAcceptingTerms,
                    isLoading = isAcceptingTerms,
                    dismissOnConfirm = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    dimBackground = false,
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 210.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    append(Loc.tr(R.string.str_terms_p1_prefix).trim())
                                    append(" ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                                        append(Loc.tr(R.string.str_terms_p1_bold_tos).trim())
                                    }
                                    append(" ")
                                    append(Loc.tr(R.string.str_terms_p1_mid).trim())
                                    append(" ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                                        append(Loc.tr(R.string.str_terms_p1_bold_pp).trim())
                                    }
                                },
                                color = AppColors.TextMuted,
                                fontSize = 13.sp,
                                lineHeight = 17.5.sp,
                                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                            )
                            Text(
                                text = Loc.tr(R.string.str_terms_p2),
                                color = AppColors.TextMuted,
                                fontSize = 13.sp,
                                lineHeight = 17.5.sp,
                                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                            )
                            Text(
                                text = Loc.tr(R.string.str_terms_p3),
                                color = AppColors.TextMuted,
                                fontSize = 13.sp,
                                lineHeight = 17.5.sp,
                                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                            )
                        }

                        // кнопки перехода к документам
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(33.5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1C1D22))
                                    .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                                    .clickable(enabled = !isAcceptingTerms) {
                                        openExternalDocument("https://docs.google.com/document/d/1QjeFEklSDldnPSnqsBzqBd-O9zOexuI4GXqDtS2RN-k/edit?usp=drivesdk")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Loc.tr(R.string.str_btn_privacy_policy),
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(33.5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1C1D22))
                                    .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                                    .clickable(enabled = !isAcceptingTerms) {
                                        openExternalDocument("https://docs.google.com/document/d/1QyC8h6ZC1j8bHhFwO7e_bLnT5HLmkuMWZByaMxui2iE/edit?usp=drivesdk")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = Loc.tr(R.string.str_btn_terms_of_service),
                                    color = Color.White,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                                )
                            }
                        }
                    },
                    onConfirm = {
                        if (isAcceptingTerms) return@UnifiedDialog
                        isAcceptingTerms = true
                        coroutineScope.launch {
                            try {
                                AppSettings.setBoolean(context, "terms_accepted", true)
                                delay(250)
                                performOnlineChecksAndProceed()
                            } finally {
                                isAcceptingTerms = false
                            }
                        }
                    },
                    onDismiss = {
                        findActivity(context)?.finishAffinity()
                        kotlin.system.exitProcess(0)
                    }
                )
            }

            // диалоговое окно ошибки подключения перед авторизацией
            if (currentPhase == SplashPhase.OFFLINE_ERROR) {
                val isReconnectingNow = GlobalReconnectState.isReconnecting
                UnifiedDialog(
                    title = Loc.tr(R.string.str_connection_error_title),
                    showCancelButton = false,
                    dismissOnConfirm = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    dimBackground = false,
                    confirmText = Loc.tr(R.string.str_connection_error_btn_ok),
                    confirmEnabled = !isReconnectingNow,
                    content = {
                        Text(
                            text = Loc.tr(R.string.str_connection_error_desc),
                            color = AppColors.TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    onConfirm = {
                        if (GlobalReconnectState.isReconnecting) return@UnifiedDialog
                        GlobalReconnectState.isReconnecting = true
                        coroutineScope.launch {
                            val startTime = android.os.SystemClock.elapsedRealtime()
                            var checkRes: StartupCheckResult? = null
                            while (android.os.SystemClock.elapsedRealtime() - startTime < 5000L) {
                                val elapsed = android.os.SystemClock.elapsedRealtime() - startTime
                                val rem = (5000L - elapsed).coerceAtLeast(500L)
                                val attemptRes = kotlinx.coroutines.withTimeoutOrNull(minOf(2000L, rem)) {
                                    sys_menu_socket_repository.performClientStartupCheck(context)
                                }
                                if (attemptRes is StartupCheckResult.Ok || attemptRes is StartupCheckResult.Banned || attemptRes is StartupCheckResult.VersionMismatch || sys_menu_socket_repository.isVersionMismatch) {
                                    checkRes = attemptRes
                                    break
                                }
                                val elapsedAfter = android.os.SystemClock.elapsedRealtime() - startTime
                                if (elapsedAfter >= 5000L) break
                                delay(minOf(700L, 5000L - elapsedAfter))
                            }

                            if (checkRes is StartupCheckResult.VersionMismatch || sys_menu_socket_repository.isVersionMismatch) {
                                GlobalReconnectState.isReconnecting = false
                                sys_menu_socket_repository.disconnectAndForceOffline()
                                currentPhase = SplashPhase.VERSION_UPDATE
                                return@launch
                            }

                            if (checkRes is StartupCheckResult.Banned) {
                                GlobalReconnectState.isReconnecting = false
                                GlobalSecurityDialogState.trigger(checkRes.accountId, checkRes.issuedTime)
                                return@launch
                            }

                            if (checkRes is StartupCheckResult.Ok) {
                                GlobalSecurityDialogState.clearLocalBan(context)
                                val isLocallyBanned = AppSettings.getBoolean(context, "is_banned_locally", false)
                                if (GlobalSecurityDialogState.isVisible || GlobalTempBanDialogState.isVisible || isLocallyBanned) {
                                    GlobalReconnectState.isReconnecting = false
                                    return@launch
                                }

                                val savedToken = AppSettings.getString(context, "session_token", "").ifEmpty {
                                    AppSettings.getString(context, "vk_auth_code", "")
                                }
                                if (savedToken.isEmpty()) {
                                    GlobalReconnectState.isReconnecting = false
                                    sys_menu_socket_repository.disconnectAndForceOffline()
                                    sys_menu_socket_repository.currentPlayerId = ""
                                    sys_menu_socket_repository.currentSessionToken = ""
                                    showAuthChoiceWithFade()
                                } else {
                                    val loginRes = sys_menu_socket_repository.loginWithTokenOnline(savedToken, context)
                                    GlobalReconnectState.isReconnecting = false
                                    if (loginRes == "SUCCESS") {
                                        currentPhase = SplashPhase.FADE_OUT
                                        mainAlpha.animateTo(0f, animationSpec = tween(500, easing = LinearEasing))
                                        onSplashFinished()
                                    } else if (loginRes == "OFFLINE") {
                                        sys_menu_socket_repository.disconnectAndForceOffline()
                                    } else if (loginRes.startsWith("TEMP_BAN") || loginRes.startsWith("CHECK_BAN") || loginRes == "BANNED") {
                                        showAuthChoiceWithFade()
                                    } else {
                                        if (loginRes.startsWith("ACCOUNT_UDALENIYE|")) {
                                            val accId = loginRes.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                                            GlobalToastManager.show(Loc.tr(R.string.str_account_login_unavailable_with_id, accId))
                                        } else if (loginRes.startsWith("ACCOUNT_BANNED|")) {
                                            val accId = loginRes.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                                            GlobalToastManager.show(Loc.tr(R.string.str_account_blocked_with_id, accId))
                                        }
                                        AppSettings.setString(context, "session_token", "")
                                        AppSettings.setString(context, "vk_auth_code", "")
                                        AppSettings.setString(context, "auth_type", "")
                                        AppSettings.setString(context, "driver_account_id", "")
                                        sys_menu_socket_repository.disconnectAndForceOffline()
                                        sys_menu_socket_repository.currentPlayerId = ""
                                        sys_menu_socket_repository.currentSessionToken = ""
                                        showAuthChoiceWithFade()
                                    }
                                }
                            } else {
                                val remaining = 5000L - (android.os.SystemClock.elapsedRealtime() - startTime)
                                if (remaining > 0L) {
                                    delay(remaining)
                                }
                                GlobalReconnectState.isReconnecting = false
                                sys_menu_socket_repository.disconnectAndForceOffline()
                            }
                        }
                    },
                    onDismiss = {}
                )
            }

            // диалоговое окно требования обновления игры
            if (currentPhase == SplashPhase.VERSION_UPDATE) {
                BackHandler(enabled = true) {
                    try {
                        (context as? android.app.Activity)?.finishAffinity()
                    } catch (e: Exception) {
                        try {
                            (context as? android.app.Activity)?.finish()
                        } catch (e2: Exception) {}
                    }
                }
                UnifiedDialog(
                    title = Loc.tr(R.string.str_version_update_title),
                    showCancelButton = false,
                    dismissOnConfirm = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    dimBackground = false,
                    confirmText = Loc.tr(R.string.str_version_update_btn),
                    confirmEnabled = true,
                    content = {
                        Text(
                            text = Loc.tr(R.string.str_version_update_desc),
                            color = AppColors.TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    onConfirm = {
                        try {
                            (context as? android.app.Activity)?.finishAffinity()
                        } catch (e: Exception) {
                            try {
                                (context as? android.app.Activity)?.finish()
                            } catch (e2: Exception) {}
                        }
                    },
                    onDismiss = {
                        try {
                            (context as? android.app.Activity)?.finishAffinity()
                        } catch (e: Exception) {
                            try {
                                (context as? android.app.Activity)?.finish()
                            } catch (e2: Exception) {}
                        }
                    }
                )
            }

            // окно ввода токена
            if (currentPhase == SplashPhase.AUTH_CHOICE && showTokenDialog) {
                UnifiedDialog(
                    title = Loc.tr(R.string.str_vvedite_token_dlya_avtorizatsii),
                    cancelText = Loc.tr(R.string.back),
                    confirmText = Loc.tr(R.string.sign_in),
                    confirmEnabled = vkInputCode.isNotBlank() && !isAuthBlocked && !isAuthLoading,
                    isLoading = isAuthLoading,
                    dismissOnConfirm = false,
                    content = {
                        UnifiedTextField(
                            value = vkInputCode,
                            onValueChange = { vkInputCode = it },
                            placeholder = "*  *  *  *  *  *  *",
                            maxLength = 512,
                            enabled = !isAuthLoading,
                            topPaddingDp = 3.0f,
                            modifier = Modifier.fillMaxWidth().padding(top = 3.dp)
                        )
                    },
                    onConfirm = {
                        if (isAuthBlocked || vkInputCode.isBlank()) return@UnifiedDialog
                        triggerAuthCooldown()
                        isAuthLoading = true
                        statusText = Loc.tr(R.string.str_proverka)
                        coroutineScope.launch {
                            try {
                                val res = sys_menu_socket_repository.loginWithTokenOnline(vkInputCode.trim(), context, isManualInput = true)
                                if (res == "SUCCESS") {
                                    showTokenDialog = false
                                    statusText = ""
                                    currentPhase = SplashPhase.FADE_OUT
                                    mainAlpha.animateTo(0f, animationSpec = tween(400, easing = LinearEasing))
                                    onSplashFinished()
                                } else if (res == "OFFLINE") {
                                    showTokenDialog = false
                                    statusText = ""
                                    currentPhase = SplashPhase.OFFLINE_ERROR
                                } else if (res.startsWith("TEMP_BAN") || res.startsWith("CHECK_BAN") || res == "BANNED") {
                                    showTokenDialog = false
                                    statusText = ""
                                } else if (res.startsWith("ACCOUNT_UDALENIYE|")) {
                                    statusText = ""
                                    val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                                    GlobalToastManager.show(Loc.tr(R.string.str_account_login_unavailable_with_id, accId))
                                } else if (res.startsWith("ACCOUNT_BANNED|")) {
                                    statusText = ""
                                    val accId = res.split("|").getOrNull(1)?.ifBlank { "N/A" } ?: "N/A"
                                    GlobalToastManager.show(Loc.tr(R.string.str_account_blocked_with_id, accId))
                                } else if (res == "TOKEN_ERROR:NOT_FOUND") {
                                    statusText = Loc.tr(R.string.str_token_avtorizatsii_ne_nayden)
                                } else if (res == "TOKEN_ERROR:LIMIT_EXCEEDED") {
                                    statusText = Loc.tr(R.string.str_dostignut_limit_poprobuyte_pozzhe)
                                } else if (res == "TOKEN_ERROR:DISABLED") {
                                    statusText = Loc.tr(R.string.str_vhod_po_tokenu_otklyuchen_serverom)
                                } else {
                                    statusText = sys_menu_socket_repository.translateError(res)
                                }
                            } finally {
                                isAuthLoading = false
                            }
                        }
                    },
                    onDismiss = {
                        if (!isAuthLoading) {
                            showTokenDialog = false
                        }
                    }
                )
            }

            // окно предупреждения при переходе с гостя на вк
            if (currentPhase == SplashPhase.AUTH_CHOICE && showVkSwitchWarningDialog) {
                UnifiedDialog(
                    title = Loc.tr(R.string.str_preduprezhdenie),
                    cancelText = Loc.tr(R.string.back),
                    confirmText = Loc.tr(R.string.str_prodolzhit),
                    content = {
                        Text(
                            text = Loc.tr(R.string.str_vk_switch_guest_warning),
                            color = AppColors.TextMuted,
                            fontSize = 14.sp
                        )
                    },
                    onConfirm = {
                        showVkSwitchWarningDialog = false
                        launchVkAuth()
                    },
                    onDismiss = {
                        showVkSwitchWarningDialog = false
                    }
                )
            }
        }
    }
}

// анимированный текст "ЗАГРУЗКА"
@Composable
fun SmoothLoadingText(alpha: Float = 1.0f, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "water_shimmer")
    
    val shimmerPos by transition.animateFloat(
        initialValue = -180f,
        targetValue = 520f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerPos"
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            AppColors.White.copy(alpha = 0.2f * glowAlpha * alpha),
            AppColors.White.copy(alpha = glowAlpha * alpha),
            AppColors.White.copy(alpha = 0.2f * glowAlpha * alpha)
        ),
        start = Offset(shimmerPos, shimmerPos * 0.15f),
        end = Offset(shimmerPos + 220f, shimmerPos * 0.15f + 40f)
    )

    Text(
        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.loading),
        style = TextStyle(
            brush = shimmerBrush,
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 12.sp,
            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
        ),
        modifier = modifier.alpha(alpha)
    )
}
