package skayrux.shootout2d

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape

object PerformanceMonitorState {
    var showFps by mutableStateOf(false)
    var showCpuGpu by mutableStateOf(false)
    var showGraph by mutableStateOf(false)
    var showPing by mutableStateOf(false)
    var currentPing by mutableIntStateOf(0)
    var isServerConnectionLost by mutableStateOf(false)
}

object LiveBackgroundState {
    var videoPath by mutableStateOf("")
    var imagePath by mutableStateOf("")
    var soundEnabled by mutableStateOf(false)
    var smokeEnabled by mutableStateOf(true)
    var volume by mutableFloatStateOf(100f)
    var isMenuVisible by mutableStateOf(true)
    var isEditingControls by mutableStateOf(false)
}

fun getProcessCpuTicks(): Long {
    return try {
        val stat = java.io.File("/proc/self/stat").readText()
        val index = stat.lastIndexOf(')')
        if (index != -1 && index + 2 < stat.length) {
            val rest = stat.substring(index + 2).trim()
            val parts = rest.split("\\s+".toRegex())
            if (parts.size > 12) {
                parts[11].toLong() + parts[12].toLong()
            } else 0L
        } else 0L
    } catch (e: Exception) {
        0L
    }
}

@Composable
fun PerformanceOverlay() {
    val showFps = PerformanceMonitorState.showFps
    val showCpuGpu = PerformanceMonitorState.showCpuGpu
    val showGraph = PerformanceMonitorState.showGraph
    val showPing = PerformanceMonitorState.showPing

    if (!showFps && !showCpuGpu && !showGraph && !showPing) return

    var fps by remember { mutableIntStateOf(60) }
    
    LaunchedEffect(Unit) {
        var frameCount = 0
        var lastTime = System.currentTimeMillis()
        var lastFrameNanos = 0L

        while (true) {
            withFrameNanos { frameNanos ->
                frameCount++
                lastFrameNanos = frameNanos

                val now = System.currentTimeMillis()
                if (now - lastTime >= 500) {
                    val actualFps = (frameCount * 1000f / (now - lastTime)).toInt().coerceIn(1, 120)
                    fps = actualFps
                    frameCount = 0
                    lastTime = now
                }
            }
        }
    }

    // проверка потери связи с сервером для отображения прочерка пинга красным цветом
    val isConnectionLost = PerformanceMonitorState.isServerConnectionLost ||
        (!sys_menu_socket_repository.isServerOnline && GlobalReconnectState.isInGameSession) ||
        GlobalReconnectState.isReconnecting ||
        GlobalReconnectState.showConnectionErrorDialog

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (showFps || showCpuGpu || showGraph || showPing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 2.dp, top = 2.dp)
            ) {
                Column {
                    if (showFps) {
                        Text(
                            text = "FPS: $fps",
                            color = if (fps >= 55) AppColors.FpsPingGreen else if (fps >= 35) AppColors.FpsPingYellow else AppColors.FpsPingRed,
                            fontSize = 7.sp,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (showPing) {
                        val pVal = PerformanceMonitorState.currentPing
                        // индикация пинга
                        val pingStr = when {
                            isConnectionLost -> "—"
                            pVal >= 999 -> "999+"
                            else -> pVal.toString()
                        }
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ping_ms, pingStr),
                            color = if (isConnectionLost || pVal > 220) AppColors.FpsPingRed else if (pVal > 100) AppColors.FpsPingYellow else AppColors.FpsPingGreen,
                            fontSize = 7.sp,
                            fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

class system : ComponentActivity() {

    private val currentIntentState = mutableStateOf<android.content.Intent?>(null)

    override fun attachBaseContext(newBase: android.content.Context) {
        val savedLang = AppSettings.getString(newBase, "app_language", "")
        val lang = if (savedLang.isNotEmpty()) {
            savedLang
        } else {
            val devLang = java.util.Locale.getDefault().language
            if (devLang.startsWith("en")) "en" else "ru"
        }
        val locale = java.util.Locale(lang)
        java.util.Locale.setDefault(locale)
        val config = AppLanguageManager.getLocalizedConfiguration(newBase, lang)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentState.value = intent
        scheduleHideSystemBars()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            scheduleHideSystemBars()
        }
    }

    // скрытие системных панелей телефона
    fun hideSystemBars() {
        runCatching {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.let { controller ->
                    controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(android.view.WindowInsets.Type.systemBars())
                }
            }
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    // отложенное повторное скрытие системных панелей после переходов и диалогов
    fun scheduleHideSystemBars() {
        hideSystemBars()
        runCatching {
            window.decorView.post { hideSystemBars() }
            window.decorView.postDelayed({ hideSystemBars() }, 150L)
            window.decorView.postDelayed({ hideSystemBars() }, 450L)
        }
    }

    override fun onResume() {
        super.onResume()
        sys_menu_socket_repository.isAppInForeground = true
        scheduleHideSystemBars()
    }

    override fun onPostResume() {
        super.onPostResume()
        scheduleHideSystemBars()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing && datagame_main.instance == null) {
            GlobalReconnectState.isIntentionalDisconnect = true
            GlobalReconnectState.showConnectionErrorDialog = false
            GlobalReconnectState.isReconnecting = false
            sys_menu_socket_repository.isAppInForeground = false
            sys_menu_socket_repository.disconnectAndForceOffline()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (UiGlyphMetricTable.syncFontKerningMetrics(this) != 90) {
            UiGlyphMetricTable.terminateOnInvalidMetrics(this)
            return
        }
        sys_menu_socket_repository.isAppInForeground = true
        LiveBackgroundState.isEditingControls = false
        PerformanceMonitorState.showFps = AppSettings.getBoolean(this, "show_fps", false)
        PerformanceMonitorState.showCpuGpu = AppSettings.getBoolean(this, "show_cpu_gpu", false)
        PerformanceMonitorState.showGraph = AppSettings.getBoolean(this, "show_graph", false)
        PerformanceMonitorState.showPing = AppSettings.getBoolean(this, "show_ping", false)
        LiveBackgroundState.videoPath = AppSettings.getString(this, "live_background_path", "")
        LiveBackgroundState.imagePath = AppSettings.getString(this, "live_background_image_path", "")
        LiveBackgroundState.soundEnabled = AppSettings.getBoolean(this, "live_background_sound", false)
        LiveBackgroundState.smokeEnabled = AppSettings.getBoolean(this, "smoke_background", true)
        LiveBackgroundState.volume = AppSettings.getFloat(this, "volume", 100f)
        AppLanguageManager.init(this)
        skayrux.shootout2d.Loc.appContext = this
        currentIntentState.value = intent
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            window.attributes = lp
        }

        scheduleHideSystemBars()
        runCatching {
            @Suppress("DEPRECATION")
            window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if ((visibility and android.view.View.SYSTEM_UI_FLAG_FULLSCREEN) == 0 ||
                    (visibility and android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    scheduleHideSystemBars()
                }
            }
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
                if (insets.isVisible(WindowInsetsCompat.Type.statusBars()) ||
                    insets.isVisible(WindowInsetsCompat.Type.navigationBars())) {
                    v.postDelayed({ hideSystemBars() }, 100L)
                }
                WindowInsetsCompat.CONSUMED
            }
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val baseConfig = androidx.compose.ui.platform.LocalConfiguration.current
            val customDensity = getCustomDensity()
            val currentLang = AppLanguageManager.currentLanguage
            val localizedConfig = androidx.compose.runtime.remember(currentLang, baseConfig, context) {
                AppLanguageManager.getLocalizedConfiguration(context, currentLang, baseConfig)
            }
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides customDensity,
                androidx.compose.ui.platform.LocalConfiguration provides localizedConfig
            ) {
                skayrux.shootout2d.ui.theme.Shootout2DTheme {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    var showSplash by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
                    val activeIntent = currentIntentState.value
                    val kicked = sys_menu_socket_repository.kickedReason
                    androidx.compose.runtime.LaunchedEffect(kicked) {
                        if (kicked == "ANOTHER_DEVICE") {
                            sys_menu_socket_repository.kickedReason = null
                            GlobalReconnectState.reset()
                            GlobalTempBanDialogState.skipSplashToAuth = true
                            showSplash = true
                            scheduleHideSystemBars()
                            GlobalToastManager.show(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vhod_s_drugogo_ustroystva))
                        }
                    }

                    // переход к экрану авторизации без перезапуска активити
                    val switchToSplash = GlobalReconnectState.requestSwitchToSplash
                    androidx.compose.runtime.LaunchedEffect(switchToSplash) {
                        if (switchToSplash) {
                            GlobalReconnectState.requestSwitchToSplash = false
                            GlobalReconnectState.showConnectionErrorDialog = false
                            GlobalReconnectState.isReconnecting = false
                            GlobalReconnectState.isInGameSession = false
                            currentIntentState.value = null
                            showSplash = true
                            scheduleHideSystemBars()
                        }
                    }

                    // отслеживание состояния сети устройства во время игры
                    androidx.compose.runtime.DisposableEffect(showSplash, context) {
                        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                        val callback = if (!showSplash && cm != null) {
                            object : android.net.ConnectivityManager.NetworkCallback() {
                                override fun onLost(network: android.net.Network) {
                                    val activeNet = cm.activeNetwork
                                    val caps = if (activeNet != null && activeNet != network) cm.getNetworkCapabilities(activeNet) else null
                                    val hasInternet = caps?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                                    if (!hasInternet && !GlobalReconnectState.isIntentionalDisconnect && datagame_main.instance == null) {
                                        sys_menu_socket_repository.disconnectAndForceOffline()
                                    }
                                }
                            }.also { cb ->
                                try {
                                    cm.registerDefaultNetworkCallback(cb)
                                } catch (e: Exception) {}
                            }
                        } else null

                        onDispose {
                            if (callback != null && cm != null) {
                                try {
                                    cm.unregisterNetworkCallback(callback)
                                } catch (e: Exception) {}
                            }
                        }
                    }

                    // отображение окна ошибки подключения при потере связи с сервером в игре
                    androidx.compose.runtime.LaunchedEffect(
                        showSplash,
                        sys_menu_socket_repository.isServerOnline,
                        GlobalSecurityDialogState.isVisible,
                        GlobalTempBanDialogState.isVisible,
                        GlobalReconnectState.isIntentionalDisconnect
                    ) {
                        if (isFinishing || showSplash || GlobalSecurityDialogState.isVisible || GlobalTempBanDialogState.isVisible || GlobalReconnectState.isIntentionalDisconnect) {
                            if (isFinishing || showSplash || GlobalReconnectState.isIntentionalDisconnect) {
                                GlobalReconnectState.showConnectionErrorDialog = false
                                GlobalReconnectState.isInGameSession = false
                            }
                            GlobalReconnectState.isReconnecting = false
                            return@LaunchedEffect
                        }
                        GlobalReconnectState.isInGameSession = true
                        if (sys_menu_socket_repository.isServerOnline) {
                            GlobalReconnectState.isReconnecting = false
                            GlobalReconnectState.showConnectionErrorDialog = false
                        } else {
                            GlobalReconnectState.showConnectionErrorDialog = true
                            try {
                                datagame_main.instance?.disconnectOnProfileServerLost()
                            } catch (e: Exception) {}
                        }
                    }

                    RootDialogOverlayHost {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (GlobalSecurityDialogState.isVisible) {
                                // окно ошибки при вечном бане или блокировке системы
                                SecurityBanErrorDialog()
                            } else if (GlobalTempBanDialogState.isVisible) {
                                // окно временной блокировки аккаунта (1-365 дней)
                                TempAccountBanDialog(
                                    onLogoutToAuth = {
                                        GlobalReconnectState.reset()
                                        showSplash = true
                                        scheduleHideSystemBars()
                                    }
                                )
                            } else if (showSplash) {
                                start_apk(currentIntent = activeIntent) {
                                    GlobalReconnectState.isIntentionalDisconnect = false
                                    GlobalReconnectState.isReconnecting = false
                                    GlobalReconnectState.showConnectionErrorDialog = false
                                    GlobalReconnectState.isInGameSession = true
                                    showSplash = false
                                    scheduleHideSystemBars()
                                }
                            } else {
                                val activeIntent = currentIntentState.value
                                var requestedIndex by androidx.compose.runtime.remember {
                                    val initialIdx = (context as? android.app.Activity)?.intent?.getIntExtra("OPEN_MENU_INDEX", -1) ?: -1
                                    androidx.compose.runtime.mutableIntStateOf(if (initialIdx in 0..8) initialIdx else 0)
                                }
                                var navTrigger by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(0L) }
                                androidx.compose.runtime.LaunchedEffect(activeIntent) {
                                    if (activeIntent != null) {
                                        val vkPayload = parseAndConsumeVkDeeplink(activeIntent.data, context)
                                        if (vkPayload != null) {
                                            activeIntent.data = null
                                            currentIntentState.value = null
                                            val linkRes = sys_menu_socket_repository.linkVkAccountOnline(
                                                authCodeOrToken = vkPayload.codeOrToken,
                                                context = context,
                                                codeVerifier = vkPayload.codeVerifier,
                                                vkDeviceId = vkPayload.vkDeviceId,
                                                redirectUri = vkPayload.redirectUri
                                            )
                                            if (linkRes == "SUCCESS") {
                                                GlobalToastManager.show(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vk_successfully_linked))
                                            } else if (linkRes == "OFFLINE") {
                                                GlobalToastManager.show(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_podklyucheniya_k_serveru_02ed))
                                            } else {
                                                GlobalToastManager.show(sys_menu_socket_repository.translateError(linkRes))
                                            }
                                        }
                                        val idx = activeIntent.getIntExtra("OPEN_MENU_INDEX", -1)
                                        if (idx in 0..9) {
                                            requestedIndex = idx
                                            navTrigger = System.currentTimeMillis()
                                            activeIntent.removeExtra("OPEN_MENU_INDEX")
                                        }
                                    }
                                }
                                driver_menu(
                                    initialIndex = if (requestedIndex in 0..9) requestedIndex else 0,
                                    navTrigger = navTrigger,
                                    onIndexChanged = { idx ->
                                        requestedIndex = idx
                                    }
                                )
                                if (GlobalReconnectState.showConnectionErrorDialog) {
                                    // отображение только фона игры без интерфейса меню при ошибке подключения
                                    val errorBgAlpha = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
                                    androidx.compose.runtime.LaunchedEffect(Unit) {
                                        LiveBackgroundState.isMenuVisible = true
                                        errorBgAlpha.snapTo(0f)
                                        kotlinx.coroutines.delay(120)
                                        errorBgAlpha.animateTo(
                                            1f,
                                            animationSpec = androidx.compose.animation.core.tween(
                                                1600,
                                                easing = androidx.compose.animation.core.FastOutSlowInEasing
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(AppColors.Black)
                                            .clickable(
                                                interactionSource = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null,
                                                onClick = {}
                                            )
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize()) {
                                            StaticBackground()
                                            val videoPath = LiveBackgroundState.videoPath
                                            val isVideoValid = androidx.compose.runtime.remember(videoPath) {
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
                                                    val overlayAlpha = (1f - errorBgAlpha.value).coerceIn(0f, 1f)
                                                    if (overlayAlpha > 0.001f) {
                                                        drawRect(color = AppColors.Black, alpha = overlayAlpha)
                                                    }
                                                }
                                        )
                                    }
                                }
                                PerformanceOverlay()
                            }
                            GlobalToastHost()
                        }
                    }
                }
            }
        }
    }
}
