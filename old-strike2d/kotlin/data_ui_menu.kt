package skayrux.shootout2d

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object GlobalToastManager {
    var toastMessage by mutableStateOf<String?>(null)
    private var dismissJob: kotlinx.coroutines.Job? = null
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())

    @JvmStatic
    fun show(message: String?, durationMs: Long = 3000L) {
        if (message.isNullOrBlank()) return
        dismissJob?.cancel()
        toastMessage = message
        dismissJob = scope.launch {
            kotlinx.coroutines.delay(durationMs)
            if (toastMessage == message) {
                toastMessage = null
            }
        }
    }

    @JvmStatic
    fun createToastView(context: android.content.Context): androidx.compose.ui.platform.ComposeView {
        return androidx.compose.ui.platform.ComposeView(context).apply {
            isClickable = false
            isFocusable = false
            setContent {
                val customDensity = getCustomDensity()
                val currentLang = AppLanguageManager.currentLanguage
                val baseConfig = androidx.compose.ui.platform.LocalConfiguration.current
                val localizedConfig = remember(currentLang, baseConfig, context) {
                    AppLanguageManager.getLocalizedConfiguration(context, currentLang, baseConfig)
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides customDensity,
                    androidx.compose.ui.platform.LocalConfiguration provides localizedConfig
                ) {
                    skayrux.shootout2d.ui.theme.Shootout2DTheme {
                        RootDialogOverlayHost {
                            GlobalToastHost()
                        }
                    }
                }
            }
        }
    }
}

// состояние переподключения к серверу в игре
object GlobalReconnectState {
    var isInGameSession by mutableStateOf(false)
    var isReconnecting by mutableStateOf(false)
    var showConnectionErrorDialog by mutableStateOf(false)
    var isIntentionalDisconnect by mutableStateOf(false)
    var requestSwitchToSplash by mutableStateOf(false)
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // запуск 5-секундного переподключения в игре по нажатию кнопки
    fun startInGameReconnect() {
        if (isReconnecting) return
        isReconnecting = true
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val startTime = android.os.SystemClock.elapsedRealtime()
            var reconnected = false
            while (android.os.SystemClock.elapsedRealtime() - startTime < 5000L) {
                val ok = sys_menu_socket_repository.tryQuickReconnect()
                if (ok || sys_menu_socket_repository.isServerOnline) {
                    reconnected = true
                    break
                }
                val elapsed = android.os.SystemClock.elapsedRealtime() - startTime
                if (elapsed >= 5000L) break
                delay(minOf(700L, 5000L - elapsed))
            }
            if (reconnected || sys_menu_socket_repository.isServerOnline) {
                isReconnecting = false
                showConnectionErrorDialog = false
                sys_menu_socket_repository.syncFullAccountData(force = true)
            } else {
                val remaining = 5000L - (android.os.SystemClock.elapsedRealtime() - startTime)
                if (remaining > 0L) {
                    delay(remaining)
                }
                isReconnecting = false
                if (!sys_menu_socket_repository.isServerOnline && !GlobalSecurityDialogState.isVisible && !GlobalTempBanDialogState.isVisible && !isIntentionalDisconnect) {
                    showConnectionErrorDialog = true
                    sys_menu_socket_repository.disconnectAndForceOffline()
                }
            }
        }
    }

    fun reset() {
        reconnectJob?.cancel()
        reconnectJob = null
        isInGameSession = false
        isReconnecting = false
        showConnectionErrorDialog = false
        isIntentionalDisconnect = false
        requestSwitchToSplash = false
    }
}

@Composable
fun GlobalToastHost() {
    val message = GlobalToastManager.toastMessage
    val isReconnecting = GlobalReconnectState.isReconnecting
    val showErrorDialog = GlobalReconnectState.showConnectionErrorDialog

    UnifiedToast(
        message = message ?: "",
        visible = !message.isNullOrEmpty() && !isReconnecting && !showErrorDialog
    )

    UnifiedReconnectToast(
        visible = isReconnecting
    )

    if (showErrorDialog) {
        UnifiedDialog(
            title = Loc.tr(R.string.str_connection_error_title),
            showCancelButton = false,
            dismissOnConfirm = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            dimBackground = false,
            confirmText = Loc.tr(R.string.str_connection_error_btn_ok),
            confirmEnabled = !isReconnecting,
            content = {
                Text(
                    text = Loc.tr(R.string.str_connection_error_desc),
                    color = AppColors.TextMuted,
                    fontSize = 14.sp
                )
            },
            onConfirm = {
                GlobalReconnectState.startInGameReconnect()
            },
            onDismiss = {}
        )
    }
}

// поиск активити из контекста
fun findActivity(context: android.content.Context): android.app.Activity? {
    var ctx: android.content.Context? = context
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return ctx as? android.app.Activity
}

// получение реальных размеров экрана в пикселях
fun getRealScreenSizePx(context: android.content.Context): Pair<Int, Int> {
    val dm = context.resources.displayMetrics
    var w = dm.widthPixels
    var h = dm.heightPixels
    val activity = findActivity(context)
    if (activity != null) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val bounds = activity.windowManager.currentWindowMetrics.bounds
                if (bounds.width() > 0 && bounds.height() > 0) {
                    w = bounds.width()
                    h = bounds.height()
                }
            } else {
                val realDm = android.util.DisplayMetrics()
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay.getRealMetrics(realDm)
                if (realDm.widthPixels > 0 && realDm.heightPixels > 0) {
                    w = realDm.widthPixels
                    h = realDm.heightPixels
                }
            }
        } catch (e: Exception) {}
    }
    val landscapeW = maxOf(w, h)
    val landscapeH = minOf(w, h)
    return Pair(landscapeW, landscapeH)
}

@Composable
fun getCustomDensity(): androidx.compose.ui.unit.Density {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenSize = androidx.compose.runtime.remember(configuration, context) {
        getRealScreenSizePx(context)
    }
    return androidx.compose.runtime.remember(screenSize, context) {
        val physicalWidthPx = screenSize.first.toFloat()
        val physicalHeightPx = screenSize.second.toFloat()
        val defaultDensity = context.resources.displayMetrics.density
        val smallestWidthDp = physicalHeightPx / defaultDensity
        val isTablet = smallestWidthDp >= 600f
        val targetDensity = if (isTablet) {
            defaultDensity
        } else {
            val densityBasedOnHeight = physicalHeightPx / 365f
            val densityBasedOnWidth = physicalWidthPx / 730f
            val calculatedDensity = minOf(densityBasedOnHeight, densityBasedOnWidth)
            if (calculatedDensity > 0.1f) calculatedDensity else defaultDensity
        }
        object : androidx.compose.ui.unit.Density {
            override val density: Float get() = targetDensity
            override val fontScale: Float get() = 1f
        }
    }
}

// плотность экрана 1к1 с игровым движком для управления
@Composable
fun getGameHudDensity(): androidx.compose.ui.unit.Density {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenSize = androidx.compose.runtime.remember(configuration, context) {
        getRealScreenSizePx(context)
    }
    val gameRs = datagame_main.instance?.rs ?: 0f
    return androidx.compose.runtime.remember(screenSize, gameRs, context) {
        val defaultDensity = context.resources.displayMetrics.density
        val d = maxOf(screenSize.first, screenSize.second).toFloat()
        val calculatedRs = (d / 480f) / 1.7f
        val targetDensity = when {
            gameRs > 0.1f -> gameRs
            calculatedRs > 0.1f -> calculatedRs
            else -> defaultDensity
        }
        object : androidx.compose.ui.unit.Density {
            override val density: Float get() = targetDensity
            override val fontScale: Float get() = 1f
        }
    }
}

// элемент полноэкранного оверлея
class FullscreenOverlayEntry(
    val id: Any,
    val zOrder: Int,
    val seq: Long,
    val contentState: State<@Composable () -> Unit>
)

// контроллер полноэкранных диалогов и уведомлений
class FullscreenOverlayController {
    private var nextSeq = 0L
    val entries = mutableStateListOf<FullscreenOverlayEntry>()

    fun register(id: Any, zOrder: Int, contentState: State<@Composable () -> Unit>) {
        val existingIdx = entries.indexOfFirst { it.id === id }
        if (existingIdx >= 0) {
            entries[existingIdx] = FullscreenOverlayEntry(id, zOrder, entries[existingIdx].seq, contentState)
        } else {
            entries.add(FullscreenOverlayEntry(id, zOrder, ++nextSeq, contentState))
        }
        entries.sortWith(compareBy<FullscreenOverlayEntry> { it.zOrder }.thenBy { it.seq })
    }

    fun unregister(id: Any) {
        entries.removeAll { it.id === id }
    }
}

val LocalFullscreenOverlayController = staticCompositionLocalOf<FullscreenOverlayController?> { null }

// корневой контейнер для отрисовки диалогов и затемнения на весь экран
@Composable
fun RootDialogOverlayHost(content: @Composable () -> Unit) {
    val controller = remember { FullscreenOverlayController() }
    CompositionLocalProvider(LocalFullscreenOverlayController provides controller) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
            val snapshot = controller.entries.toList()
            for (entry in snapshot) {
                key(entry.id) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        entry.contentState.value.invoke()
                    }
                }
            }
        }
    }
}

// поиск окна диалога
fun findDialogWindow(view: android.view.View): android.view.Window? {
    var current: android.view.ViewParent? = view.parent
    while (current != null) {
        if (current is androidx.compose.ui.window.DialogWindowProvider) {
            return current.window
        }
        current = current.parent
    }
    var ctx: android.content.Context? = view.context
    while (ctx != null) {
        if (ctx is androidx.compose.ui.window.DialogWindowProvider) {
            return ctx.window
        }
        if (ctx is android.app.Dialog) {
            return ctx.window
        }
        if (ctx is android.content.ContextWrapper) {
            ctx = ctx.baseContext
        } else {
            break
        }
    }
    return (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
}

// настройка полноэкранного окна диалога
fun configureFullscreenDialogWindow(window: android.view.Window?, blurRadius: Int = 10) {
    window?.let { w ->
        w.setWindowAnimations(0)
        w.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN)
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR)
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        w.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        w.decorView.setPadding(0, 0, 0, 0)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(w.decorView) { _, _ ->
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }
        val lp = w.attributes
        lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT
        lp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT
        lp.horizontalMargin = 0f
        lp.verticalMargin = 0f
        lp.x = 0
        lp.y = 0
        lp.gravity = android.view.Gravity.CENTER
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            lp.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        w.attributes = lp
        w.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.MATCH_PARENT
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            w.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        @Suppress("DEPRECATION")
        w.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        if (blurRadius > 0 && android.os.Build.VERSION.SDK_INT >= 31) {
            w.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            w.setBackgroundBlurRadius(blurRadius)
        }
    }
}

// универсальный полноэкранный слой для диалогов и тостов
@Composable
fun FullscreenDialogOverlay(
    zOrder: Int = 20,
    onDismissRequest: () -> Unit = {},
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = false,
    isTouchable: Boolean = true,
    blurRadius: Int = 10,
    content: @Composable () -> Unit
) {
    val controller = LocalFullscreenOverlayController.current
    if (controller != null) {
        val overlayId = remember { Any() }
        val currentContentState = rememberUpdatedState(content)
        DisposableEffect(controller, overlayId, zOrder) {
            controller.register(overlayId, zOrder, currentContentState)
            onDispose {
                controller.unregister(overlayId)
            }
        }
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
                dismissOnBackPress = dismissOnBackPress,
                dismissOnClickOutside = dismissOnClickOutside
            )
        ) {
            val customDensity = getCustomDensity()
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides customDensity
            ) {
                val view = androidx.compose.ui.platform.LocalView.current
                DisposableEffect(view) {
                    fun applyConfig() {
                        val window = findDialogWindow(view)
                        if (window != null) {
                            view.layoutParams = android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            configureFullscreenDialogWindow(window, blurRadius = blurRadius)
                            if (!isTouchable) {
                                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                            }
                        }
                    }
                    applyConfig()
                    view.post { applyConfig() }
                    val attachListener = object : android.view.View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: android.view.View) {
                            applyConfig()
                        }
                        override fun onViewDetachedFromWindow(v: android.view.View) {}
                    }
                    view.addOnAttachStateChangeListener(attachListener)
                    onDispose {
                        view.removeOnAttachStateChangeListener(attachListener)
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun UnifiedToast(
    message: String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White
) {
    var lastMessage by remember { mutableStateOf("") }
    if (message.isNotEmpty()) {
        lastMessage = message
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible && message.isNotEmpty()) 1f else 0f,
        animationSpec = tween(400)
    )
    if (alpha > 0.01f) {
        FullscreenDialogOverlay(
            zOrder = 100,
            onDismissRequest = {},
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            isTouchable = false,
            blurRadius = 0
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = modifier
                        .graphicsLayer(alpha = alpha)
                        .background(AppColors.ToastBg.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .border(1.dp, AppColors.Overlay20, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lastMessage,
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// плашка уведомления о переподключении к серверу с индикатором загрузки
@Composable
fun UnifiedReconnectToast(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "reconnectToastAlpha"
    )
    if (alpha > 0.01f) {
        FullscreenDialogOverlay(
            zOrder = 105,
            onDismissRequest = {},
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            isTouchable = false,
            blurRadius = 0
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = modifier
                        .graphicsLayer(alpha = alpha)
                        .background(AppColors.ToastBg.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .border(1.dp, AppColors.Overlay20, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = Loc.tr(R.string.str_reconnecting_to_server),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = AppColors.White,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedDialog(
    title: String,
    content: (@Composable () -> Unit)? = null,
    confirmText: String,
    confirmEnabled: Boolean = true,
    isLoading: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    dismissOnConfirm: Boolean = true,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = false,
    titleColor: Color = Color.White,
    confirmColor: Color = AppColors.DialogButtonActive,
    cancelText: String = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.cancel),
    showCancelButton: Boolean = true,
    neutralText: String? = null,
    neutralEnabled: Boolean = true,
    onNeutral: (() -> Unit)? = null,
    neutralContent: (@Composable () -> Unit)? = null,
    confirmContent: (@Composable () -> Unit)? = null,
    dialogWidthDp: Int? = null,
    dimBackground: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var isVisible by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    DisposableEffect(Unit) {
        (findActivity(context) as? system)?.scheduleHideSystemBars()
        onDispose {
            (findActivity(context) as? system)?.scheduleHideSystemBars()
        }
    }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            try {
                kotlinx.coroutines.delay(250)
            } finally {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    pendingAction?.invoke()
                }
            }
        }
    }

    val animProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = ""
    )

    fun dismissWithAnimation(action: () -> Unit) {
        if (pendingAction != null) return
        pendingAction = action
        isVisible = false
    }

    FullscreenDialogOverlay(
        zOrder = 20,
        onDismissRequest = {
            keyboardController?.hide()
            focusManager.clearFocus()
            dismissWithAnimation(onDismiss)
        },
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside,
        blurRadius = 10
    ) {
        androidx.activity.compose.BackHandler(enabled = true) {
            keyboardController?.hide()
            focusManager.clearFocus()
            if (dismissOnBackPress) {
                dismissWithAnimation(onDismiss)
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .then(if (dimBackground) Modifier.effectZatemneniya(animProgress) else Modifier.background(Color.Transparent))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        if (dismissOnClickOutside) {
                            dismissWithAnimation(onDismiss)
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val targetWidth = if (dialogWidthDp != null) dialogWidthDp.dp else 295.dp
            val actualDialogWidth = minOf(targetWidth, maxWidth * 0.90f)
            val actualMaxHeight = maxHeight * 0.92f
            Box(
                modifier = Modifier
                    .width(actualDialogWidth)
                    .heightIn(max = actualMaxHeight)
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val scale = 0.95f + 0.05f * animProgress
                        alpha = 1f * animProgress
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.DialogWindowBg)
                    .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // блок заголовка
                        if (title.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1C1D22))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = title,
                                    color = titleColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                                )
                            }
                            // разделитель
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF2B2D35))
                            )
                        }

                        // содержимое окна
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = if (title.isNotEmpty()) 8.dp else 12.dp,
                                    bottom = 12.dp
                                ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                        if (content != null) {
                            content()
                        }
                        if (showCancelButton || (neutralText != null || neutralContent != null) || confirmText.isNotEmpty() || confirmContent != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showCancelButton) {
                                    // кнопка отмены
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(33.5.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(AppColors.DialogButtonActive)
                                            .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                                            .clickable {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                dismissWithAnimation(onDismiss)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(cancelText, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily)
                                    }
                                }
                                if ((neutralText != null || neutralContent != null) && onNeutral != null) {
                                    val neutralFontSize = if ((neutralText?.length ?: 0) > 10) 11.sp else 14.sp
                                    // нейтральная кнопка
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(33.5.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (neutralEnabled) AppColors.DialogButtonActive else AppColors.DialogButtonInactive)
                                            .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                                            .clickable(enabled = neutralEnabled) {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                dismissWithAnimation(onNeutral)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (neutralContent != null) {
                                            neutralContent()
                                        } else if (neutralText != null) {
                                            Text(
                                                text = neutralText,
                                                color = if (neutralEnabled) Color.White else Color.White.copy(alpha = 0.4f),
                                                fontSize = neutralFontSize,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                                            )
                                        }
                                    }
                                }
                                if (confirmText.isNotEmpty() || confirmContent != null) {
                                    val hasOtherButtons = showCancelButton || neutralText != null || neutralContent != null
                                    val confirmFontSize = if (hasOtherButtons && confirmText.length > 12) 11.sp else 14.sp
                                    // кнопка подтверждения
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(33.5.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (confirmEnabled) confirmColor else AppColors.DialogButtonInactive)
                                            .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                                            .clickable(enabled = confirmEnabled && !isLoading) {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                if (onConfirm != null) {
                                                    if (dismissOnConfirm) {
                                                        dismissWithAnimation {
                                                            onConfirm()
                                                            onDismiss()
                                                        }
                                                    } else {
                                                        onConfirm()
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLoading) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else if (confirmContent != null) {
                                            confirmContent()
                                        } else {
                                            Text(confirmText, color = if (confirmEnabled) Color.White else Color.White.copy(alpha = 0.4f), fontSize = confirmFontSize, fontWeight = FontWeight.Medium, maxLines = 1, fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// единый класс разрешенных символов
object AllowedChars {
    const val SYMBOLS = "@{}()[]\$%#/?!<>+»«^=*„,~_; -.:'₽&`•√π÷×§∆£€¢°"
    const val REGEX = "^[a-zA-Zа-яА-ЯёЁ0-9@{}()\\[\\]\$%#/?!<>+»«^=*„,~_; \\-.:'₽&`•√π÷×§∆£€¢°]*$"

    fun isAllowed(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z') ||
               (c in 'а'..'я') || (c in 'А'..'Я') || c == 'ё' || c == 'Ё' ||
               (c in '0'..'9') ||
               SYMBOLS.contains(c)
    }

    fun sanitize(input: String, maxLength: Int = 16): String {
        val sb = StringBuilder()
        for (c in input) {
            if (isAllowed(c)) {
                sb.append(c)
                if (sb.length == maxLength) break
            }
        }
        return sb.toString()
    }
}

const val ALLOWED_CHARS_REGEX = AllowedChars.REGEX

// проверка допустимости символа по правилам сервера
fun isAllowedChar(c: Char): Boolean = AllowedChars.isAllowed(c)

// очистка строки от недопустимых символов
fun sanitizeAllowedText(input: String, maxLength: Int = 16): String = AllowedChars.sanitize(input, maxLength)

fun sanitizeSafeText(input: String): String {
    return input
        .replace("\n", "")
        .replace("\r", "")
        .replace("\t", "")
        .replace("|", "")
        .replace("\"", "")
        .replace(Regex("[\\x00-\\x1F\\x7F]"), "")
}

@Composable
fun UnifiedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    isPassword: Boolean = false,
    regex: String? = null,
    inputFilter: ((String) -> String)? = null,
    maxLength: Int = 64,
    singleLine: Boolean = true,
    heightDp: Int? = null,
    fontSizeSp: Float? = null,
    requestFocus: Boolean = false,
    transparentBorder: Boolean = false,
    backgroundColor: Color? = null,
    cornerRadiusDp: Int = 4,
    enabled: Boolean = true,
    topPaddingDp: Float = 0f,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    val containerBg = backgroundColor ?: Color(0xFF1C1D22)
    val borderColor = if (transparentBorder) Color(0xFF2B2D35).copy(alpha = 0.3f) else Color(0xFF2B2D35)
    val fieldHeight = if (heightDp != null) heightDp.dp else if (singleLine) 42.dp else 96.dp
    val hPadding = if (heightDp != null && heightDp <= 30) 6.dp else 12.dp
    val calcTextSize = fontSizeSp ?: if (heightDp != null && heightDp <= 30) 12.5f else 17.5f

    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentInputFilter by rememberUpdatedState(inputFilter)
    val currentRegex by rememberUpdatedState(regex)
    val currentValue by rememberUpdatedState(value)

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier
            .height(fieldHeight)
            .background(if (enabled) containerBg else containerBg.copy(alpha = 0.5f), RoundedCornerShape(cornerRadiusDp.dp))
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadiusDp.dp))
            .padding(horizontal = hPadding, vertical = if (singleLine) 0.dp else 8.dp),
        factory = { ctx ->
            android.widget.EditText(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setTextColor(android.graphics.Color.WHITE)
                setHintTextColor(android.graphics.Color.parseColor("#80838D"))
                hint = placeholder
                background = null
                val topPadPx = (topPaddingDp * ctx.resources.displayMetrics.density).toInt()
                setPadding(0, topPadPx, 0, 0)
                textSize = calcTextSize
                tag = calcTextSize
                setSingleLine(singleLine)
                includeFontPadding = false
                isEnabled = enabled
                isFocusable = enabled
                isFocusableInTouchMode = enabled
                if (singleLine) {
                    maxLines = 1
                    gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.START
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    if (isPassword) {
                        transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
                    }
                } else {
                    gravity = android.view.Gravity.TOP or android.view.Gravity.START
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                }

                val filters = ArrayList<android.text.InputFilter>()
                filters.add(android.text.InputFilter.LengthFilter(maxLength))
                if (inputFilter != null) {
                    filters.add(android.text.InputFilter { source, start, end, _, _, _ ->
                        val sub = source.subSequence(start, end).toString()
                        val filtered = currentInputFilter?.invoke(sub) ?: sub
                        if (filtered == sub) null else filtered
                    })
                }
                if (regex != null) {
                    filters.add(android.text.InputFilter { source, start, end, _, _, _ ->
                        val reg = currentRegex
                        val singleCharPattern = try {
                            val inner = reg?.removeSurrounding("^", "$")?.let { 
                                if (it.endsWith("*") || it.endsWith("+")) it.dropLast(1) else it 
                            }
                            if (inner != null) Regex(inner) else null
                        } catch (e: Exception) {
                            null
                        }
                        if (singleCharPattern != null) {
                            val sb = StringBuilder()
                            for (i in start until end) {
                                val c = source[i]
                                if (c.toString().matches(singleCharPattern)) {
                                    sb.append(c)
                                }
                            }
                            if (sb.length == end - start) null else sb.toString()
                        } else null
                    })
                }
                this.filters = filters.toTypedArray()

                // установка текста
                setText(value)
                if (value.isNotEmpty()) {
                    setSelection(value.length)
                }

                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                setOnEditorActionListener { v, actionId, _ ->
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                        val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.hideSoftInputFromWindow(v.windowToken, 0)
                        v.clearFocus()
                        true
                    } else false
                }

                addTextChangedListener(object : android.text.TextWatcher {
                    private var isUpdating = false
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(editable: android.text.Editable?) {
                        if (isUpdating) return
                        val currentText = editable?.toString() ?: ""
                        if (currentText != currentValue) {
                            var processedText = currentText
                            val filter = currentInputFilter
                            if (filter != null) {
                                processedText = filter(processedText)
                            }
                            val reg = currentRegex
                            if (reg == null || processedText.matches(Regex(reg))) {
                                if (processedText != currentText) {
                                    isUpdating = true
                                    val selection = selectionStart
                                    setText(processedText)
                                    setSelection(selection.coerceIn(0, processedText.length))
                                    isUpdating = false
                                }
                                currentOnValueChange(processedText)
                            } else {
                                val singleCharPattern = try {
                                    val inner = reg.removeSurrounding("^", "$").let { 
                                        if (it.endsWith("*") || it.endsWith("+")) it.dropLast(1) else it 
                                    }
                                    Regex(inner)
                                } catch (e: Exception) {
                                    null
                                }
                                val filtered = if (singleCharPattern != null) {
                                    processedText.filter { it.toString().matches(singleCharPattern) }
                                } else {
                                    processedText
                                }
                                isUpdating = true
                                val selection = selectionStart
                                setText(filtered)
                                setSelection(selection.coerceIn(0, filtered.length))
                                isUpdating = false
                                currentOnValueChange(filtered)
                            }
                        }
                    }
                })

                if (requestFocus && enabled) {
                    postDelayed({
                        requestFocus()
                        val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                        imm?.showSoftInput(this, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                    }, 100)
                }
            }
        },
        update = { editText ->
            if (editText.isEnabled != enabled) {
                editText.isEnabled = enabled
                editText.isFocusable = enabled
                editText.isFocusableInTouchMode = enabled
                if (!enabled) {
                    val imm = editText.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                    imm?.hideSoftInputFromWindow(editText.windowToken, 0)
                    editText.clearFocus()
                }
            }
            val topPadPx = (editText.context.resources.displayMetrics.density * topPaddingDp).toInt()
            if (editText.paddingTop != topPadPx) {
                editText.setPadding(0, topPadPx, 0, 0)
            }
            if (editText.hint != placeholder) {
                editText.hint = placeholder
            }
            val expectedTrans = if (isPassword) android.text.method.PasswordTransformationMethod.getInstance() else null
            if (editText.transformationMethod != expectedTrans) {
                editText.transformationMethod = expectedTrans
            }
            val currentCalcSize = editText.tag as? Float
            if (currentCalcSize != calcTextSize) {
                editText.textSize = calcTextSize
                editText.tag = calcTextSize
            }
            if (editText.text.toString() != value) {
                val selection = if (editText.isFocused) editText.selectionStart else value.length
                editText.setText(value)
                editText.setSelection(selection.coerceIn(0, value.length))
            }
        }
    )
}

@Composable
fun UnifiedLoadingDialog(
    text: String,
    onCancel: () -> Unit
) {
    FullscreenDialogOverlay(
        zOrder = 30,
        onDismissRequest = onCancel,
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        blurRadius = 10
    ) {
        androidx.activity.compose.BackHandler(enabled = true) {
            onCancel()
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .effectZatemneniya()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel
                ),
            contentAlignment = Alignment.Center
        ) {
            val actualDialogWidth = minOf(260.dp, maxWidth * 0.85f)
            Box(
                modifier = Modifier
                    .width(actualDialogWidth)
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = 1f }
                    .clip(RoundedCornerShape(8.dp))
                    .background(AppColors.DialogWindowBg)
                    .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // блок заголовка
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1C1D22))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.loading),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                            )
                        }
                        // разделитель
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0xFF2B2D35))
                        )

                        // содержимое окна
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = text,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily,
                                textAlign = TextAlign.Center
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(33.5.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AppColors.DialogButtonActive)
                                    .border(1.dp, Color(0xFF2B2D35), RoundedCornerShape(4.dp))
                                    .clickable { onCancel() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.cancel),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
                                )
                            }
                        }
                    }
                }
            }
    }
}


fun Color.lighten8(): Color {
    return Color(
        red = (red + 0.08f).coerceAtMost(1f),
        green = (green + 0.08f).coerceAtMost(1f),
        blue = (blue + 0.08f).coerceAtMost(1f),
        alpha = alpha
    )
}

fun Color.lighten3(): Color {
    return Color(
        red = (red + 0.03f).coerceAtMost(1f),
        green = (green + 0.03f).coerceAtMost(1f),
        blue = (blue + 0.03f).coerceAtMost(1f),
        alpha = alpha
    )
}

fun Color.darken5(): Color {
    return Color(
        red = (red - 0.05f).coerceAtLeast(0f),
        green = (green - 0.05f).coerceAtLeast(0f),
        blue = (blue - 0.05f).coerceAtLeast(0f),
        alpha = alpha
    )
}

val GoldColor = Color(0xFFFFD700)
val SilverColor = Color(0xFFC8CED9)
val BronzeColor = Color(0xFFCD7F32)

object AppColors {
    val GoldColor = skayrux.shootout2d.GoldColor
    val SilverColor = skayrux.shootout2d.SilverColor
    val BronzeColor = skayrux.shootout2d.BronzeColor
    val White = Color.White
    // матовый полупрозрачный оттенок интерфейса
    val Transparent = Color(0x301E2127)
    val Gray = Color.Gray
    val Black = Color.Black
    val Yellow = Color.Yellow
    val Cyan = Color.Cyan
    val Magenta = Color.Magenta
    val DarkGray = Color.DarkGray
    val LightGray = Color.LightGray
    // --- Полупрозрачные наложения (Overlay Colors) ---
    val Overlay05 = Color(0x05FFFFFF)
    val OverlayVeryLight = Color(0x0DFFFFFF)
    val ButtonBorder = Color(0x10FFFFFF)
    val Overlay10 = Color(0x1AFFFFFF)
    val ShadowBase = Color(0x261F2227)
    val Overlay20 = Color(0x33FFFFFF)
    val Overlay40 = Color(0x40FFFFFF)
    val PanelSemiTransparent = Color(0x4D2A2D34)
    val BackgroundDarkened = Color(0x6614161A)
    val BackgroundOverlay = Color(0x6616171B)
    val SurfaceOverlay = Color(0x661A1C1F)
    val PanelOverlay = Color(0x662A2C34)
    val TabActiveOverlay = Color(0x6633363D)
    val BorderSemiTransparent = Color(0x99555555)
    val GreenAlpha = Color(0xAA00FF00)
    val WhiteAlpha = Color(0xAAFFFFFF)

    // --- Централизованные системные цвета (System Core Colors) ---
    val OnlineGreen = Color(0xFF4CAF50)
    val GoldOrange = Color(0xFFBD710F)
    val BlueInfo = Color(0xFF0C759C)
    val GreenCircle = Color(0xFF039608)
    val TextGreenSuccess = Color(0xFF81C784)
    val DangerRed = Color(0xFFFF4D4D)
    val SemiWhite66 = Color(0x66FFFFFF)
    val DarkOverlayB3 = Color(0xB3212121)
    val Overlay11 = Color(0x11FFFFFF)
    val DarkGray1E = Color(0xFF1E1E1E)
    val PureGreen = Color(0xFF00FF00)
    val Gray55 = Color(0xFF555555)
    val CodeBg = Color(0x33AAAAAA)
    val LinkBlue = Color(0xFF44AAFF)
    val RedE5 = Color(0xFFE53935)
    val Gray5c = Color(0xFF5C5C5C)
    val Panel2A = Color(0xFF2A2D34)
    // --- Editor Colors ---
    val EditorMainBg = Color(0xFF0A0B0E)
    val EditorPanelBg = Color(0xB315181E)
    val EditorPanelBorder = Color(0x402C2F36)
    val EditorBtnPressedBg = Color(0x8023262D)
    val EditorBtnNormalBg = Color(0x4D1A1D24)
    val EditorBtnBorder = Color(0x332E313A)
    val EditorIconRed = Color(0xCCBD1E1E)

    // --- Цвета прогресс-баров профилей (Profile Statistics Colors) ---
    val ProgressBlueStart = Color(0xFF268ABF)
    val ProgressBlueEnd = Color(0xFF114E75)
    val ProgressOrangeStart = Color(0xFFE56A32)
    val ProgressOrangeEnd = Color(0xFF8F3510)
    val ProgressBlueBg = Color(0xFF1A6BA1)
    val ProgressOrangeBg = Color(0xFFC95726)

    // --- Градиентные цвета кастомных экранов (Gradient UI Colors) ---
    val GradientDriverStart = Color(0xFF282B36)
    val GradientDriverCenter = Color(0xFF23262F)
    val GradientDriverEnd = Color(0xFF181A20)
    val GradientOverlayDark = Color(0x66111216)

    val DialogWindowBg = Color(0xFF1C1D22)
    val FriendCardBg = Color(0xFF1D1E23)
    val DialogHeaderBg = Color(0xFF1C1D22)
    val DialogBorder = Color(0xFF2B2D35)
    val DialogButtonActive = Color(0xFF212227)
    val DialogButtonInactive = Color(0xFF1C1D22)
    val DialogInputBg = Color(0xFF1C1D22)

    // --- Динамические цвета меню (Dynamic Adaptive UI Colors) ---
    val ButtonBgDark: Color
        get() = AppColors.Transparent
    val DialogBg: Color
        get() = AppColors.DialogWindowBg
    val SurfaceDarkAlpha: Color
        get() = AppColors.Transparent
    val ToastBg = Color(0xD91F2125)
    val InputContainerAlpha: Color
        get() = AppColors.Transparent
    val PanelAlpha: Color
        get() = AppColors.Transparent
    val Background: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x7823252C) else Color(0xFF23252C)
    val SurfaceDim: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x991A1C30) else Color(0xFF1A1C30)
    val SurfaceLight: Color
        get() = AppColors.Transparent
    val SurfaceElevated: Color
        get() = AppColors.Transparent
    val InputContainer: Color
        get() = AppColors.Transparent
    val PanelHeader: Color
        get() = AppColors.Transparent
    val Gray22: Color
        get() = AppColors.Transparent
    val PanelDark: Color
        get() = AppColors.Transparent
    val SurfaceHover: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x8D1F2228) else Color(0xFF26292E)
    val DropdownBg: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0xB31E2126) else Color(0xFF1E2126)
    val HeaderBg: Color
        get() = Color(0xFF191C22)
    val DangerBg: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x593D1A1A) else Color(0xFF3D1A1A)
    val DangerBorder: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x665C2727) else Color(0xFF5C2727)
    val PlayBg: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x594CAF50) else Color(0xFF4CAF50)
    val PlayBorder: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x6681C784) else Color(0xFF81C784)
    val DisabledContainerBorder: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x18FFFFFF) else Color(0xFF494F59)
    // цвета кнопок действий
    val ActionButtonBg: Color
        get() = AppColors.Transparent
    val ActionButtonDisabledBg: Color
        get() = AppColors.Transparent
    val ActionButtonBorder: Color
        get() = AppColors.Border
    val ActionButtonDisabledBorder: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x18FFFFFF) else Color(0xFF262626)
    val ActionButtonText: Color
        get() = AppColors.White
    val ActionButtonDisabledText: Color
        get() = Color(0x66FFFFFF)
    val DarkGray2A: Color
        get() = AppColors.Transparent
    val Panel: Color
        get() = AppColors.Transparent
    val MultiplayersCellBg: Color
        get() = AppColors.Transparent
    val MultiplayersCellBorder: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x28FFFFFF) else Color(0xFF5C6370)
    val DarkGray2D = Color(0xFF2D2D2D)
    val SurfaceLightHover = Color(0xFF2E3138)
    val Primary = Color(0xFF2E7D32)
    val Gray33 = Color(0xFF333333)
    val TabActive: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x9026292E) else Color(0xFF33363D)
    val BlueGray = Color(0xFF37474F)
    val SurfaceHighlight = Color(0xFF383C45)
    val DangerDark = Color(0xFF3D1A1A)
    val GreenTech = Color(0xFF43A047)
    val Border: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x18FFFFFF) else Color(0xFF262626)
    val BlueGrayLight = Color(0xFF455A64)
    val Success = Color(0xFF4CAF50)
    val BrownDark = Color(0xFF4E342E)
    val BorderActive: Color
        get() = if (LiveBackgroundState.isMenuVisible) Color(0x28FFFFFF) else Color(0xFF4D4D4D)
    val DangerHover = Color(0xFF5C2727)
    val Brown = Color(0xFF5D4037)
    val PurpleGray = Color(0xFF625B71)
    val PurplePrimary = Color(0xFF6650A4)
    val TextPlaceholder = Color(0xFF666666)
    val Pinkish = Color(0xFF7D5260)
    val TextDarkGray = Color(0xFF888888)
    val DarkRed = Color(0xFF8B0000)
    val Gray99 = Color(0xFF999999)
    val TextMuted = Color(0xFFAAAAAA)
    val GrayBB = Color(0xFFBBBBBB)
    val ErrorRedSoft = Color(0xFFBD1E1E)
    val PurpleLight = Color(0xFFCCC2DC)
    val TextGray = Color(0xFFCCCCCC)
    val PurpleLighter = Color(0xFFD0BCFF)
    val GrayD0 = Color(0xFFD0D0D0)
    val GrayE0 = Color(0xFFE0E0E0)
    val Error = Color(0xFFE53935)
    val PinkLight = Color(0xFFEFB8C8)
    val WarningYellow = Color(0xFFF1C40F)
    val RublesGreen = Color(0xFF008A0B)
    val RedMaterial = Color(0xFFF44336)
    val BrightRed = Color(0xFFFF5555)
    val YellowMaterial = Color(0xFFFFEB3B)
    
    val ButtonBg = Panel
    val TabInactive = PanelDark
    val Divider = Overlay10
    val EmptyBorder = Overlay20
    val EmptyText = TextDarkGray
    val RoleOwner = Color(0xFF00FFFF)
    val RoleAdmin = Color(0xFF4CAF50)
    val RoleMember = Color(0xFFFFFFFF)
    val ProfileDeleteBg = Color(0x338A0002)
    val ProfileDeleteBorder = Color(0x668A0002)
    val GradientBlueStart = Color(0xFF0D51A3)
    val GradientBlueEnd = Color(0xFF062C5B)
    val GradientOrangeStart = Color(0xFFA3530D)
    val GradientOrangeEnd = Color(0xFF5B2B04)
    val FpsPingGreen = Color(0xFF69F0AE)
    val FpsPingYellow = Color(0xFFFFD740)
    val FpsPingRed = Color(0xFFD32F2F)
    val DarkGray4A = Color(0xFF4A4A4A)
    val Red = Color(0xFFFF0000)
    val Blue = Color(0xFF0000FF)
    val LightBlue = Color(0xFF0257C7)
    val Green = Color(0xFF00A113)
    val Orange = Color(0xFFD18100)
    val Purple = Color(0xFF5B00A6)
    val MediumGray85 = Color(0xFF858585)
}

object AppShapes {
    val Button = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
    val MapItem = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
}

// стиль кнопки действия меню
fun Modifier.menuActionButtonStyle(
    enabled: Boolean = true,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    activeBg: Color = AppColors.ActionButtonBg,
    disabledBg: Color = AppColors.ActionButtonDisabledBg,
    activeBorder: Color = AppColors.ActionButtonBorder,
    disabledBorder: Color = AppColors.ActionButtonDisabledBorder
): Modifier = this
    .clip(shape)
    .background(if (enabled) activeBg else disabledBg, shape)
    .border(1.dp, if (enabled) activeBorder else disabledBorder, shape)

object BanStyle {
    val IconColor = AppColors.DarkGray4A
    val NameColor = AppColors.DarkGray4A
    val BorderColor = Color(0xFF333333)
    val BackgroundColor = Color(0xFF161616)
    val DefaultIconSize = 9.5.dp
    val FriendsInfoIconSize = 9.5.dp
}

@Composable
fun BanIcon(
    modifier: Modifier = Modifier,
    color: Color = BanStyle.IconColor
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val strokeWidth = size.width * 0.15f
        val radius = (size.width - strokeWidth) / 2f
        drawCircle(
            color = color,
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
        )
        val angleRad = Math.toRadians(45.0)
        val dx = (radius * Math.cos(angleRad)).toFloat()
        val dy = (radius * Math.sin(angleRad)).toFloat()
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(center.x - dx, center.y - dy),
            end = androidx.compose.ui.geometry.Offset(center.x + dx, center.y + dy),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun PlayerNameWithBan(
    name: String,
    isBanned: Boolean,
    modifier: Modifier = Modifier,
    baseColor: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    isFriendsInfo: Boolean = false,
    maxLines: Int = 1,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
) {
    val textColor = if (isBanned) BanStyle.NameColor else baseColor
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = name,
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (isBanned) {
            Spacer(modifier = Modifier.width(4.dp))
            BanIcon(
                color = BanStyle.IconColor,
                modifier = Modifier.size(if (isFriendsInfo) BanStyle.FriendsInfoIconSize else BanStyle.DefaultIconSize)
            )
        }
    }
}

@Composable
fun UserAvatar(
    userId: String,
    userName: String,
    sizeDp: Int,
    fontSizeSp: Int,
    shape: androidx.compose.ui.graphics.Shape,
    isBanned: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val avatarBase64 = remember(userId, sys_menu_socket_repository.userAvatarsMap, isBanned) {
        if (isBanned) {
            ""
        } else {
            val inMap = sys_menu_socket_repository.userAvatarsMap[userId]
            if (inMap != null) {
                inMap
            } else {
                val saved = AvatarkiCacheManager.getCachedAvatarBase64(context, userId)
                if (saved.isNotEmpty()) {
                    sys_menu_socket_repository.userAvatarsMap = sys_menu_socket_repository.userAvatarsMap + (userId to saved)
                }
                saved
            }
        }
    }

    // фоновая сверка аватарки игрока при открытии и каждые 10 секунд
    LaunchedEffect(userId, isBanned) {
        if (userId.isNotEmpty()) {
            if (isBanned) {
                AvatarkiCacheManager.deleteAvatar(context, userId)
                sys_menu_socket_repository.userAvatarsMap = sys_menu_socket_repository.userAvatarsMap + (userId to "")
            } else {
                while (true) {
                    try {
                        if (sys_menu_socket_repository.isServerOnline) {
                            sys_menu_socket_repository.getUserAvatar(userId, forceRefresh = true)
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {}
                    kotlinx.coroutines.delay(10_000L)
                }
            }
        }
    }

    val imageBitmap = remember(avatarBase64) {
        AvatarBitmapCacheManager.getBitmap(avatarBase64)
    }

    val borderColor = AppColors.Border
    val backgroundColor = AppColors.Gray22
    val modifier = Modifier
        .requiredSize(sizeDp.dp)
        .clip(shape)
        .background(backgroundColor, shape)
        .border(1.dp, borderColor, shape)
        .let {
            if (onClick != null) {
                it.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
            } else {
                it
            }
        }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = imageBitmap,
                contentDescription = "Avatar",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape)
            )
        }
        if (imageBitmap != null && isBanned) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .effectZatemneniya()
            )
        }
        if (imageBitmap == null && !isBanned) {
            Text(
                text = userName.firstOrNull()?.uppercase()?.toString() ?: "?",
                color = Color.White,
                fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { fontSizeSp.dp.toSp() },
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (isBanned) {
            BanIcon(
                color = BanStyle.IconColor,
                modifier = Modifier.size((sizeDp * 0.65f).dp)
            )
        }
    }
}

@androidx.compose.runtime.Composable
fun androidx.compose.ui.Modifier.effectZatemneniya(
    animProgress: Float = 1f,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.ui.graphics.RectangleShape
): androidx.compose.ui.Modifier = this.then(
    androidx.compose.ui.Modifier.background(AppColors.Black.copy(alpha = 0.45f * animProgress), shape)
)

@Composable
fun iconInfo(
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = AppColors.White.darken5(),
    outlined: Boolean = false,
    contentDescription: String? = null
) {
    Icon(
        imageVector = if (outlined) Icons.Outlined.Info else Icons.Filled.Info,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun KdDonutChart(
    kills: Float,
    deaths: Float,
    zombies: Float,
    juggernauts: Float,
    ghosts: Float,
    kdValText: String,
    animTrigger: Int = 0,
    shouldAnimate: Boolean = true,
    modifier: Modifier = Modifier,
    chartSizeDp: Int = 80
) {
    val animProgress = remember { androidx.compose.animation.core.Animatable(if (shouldAnimate) 0f else 1f) }

    LaunchedEffect(animTrigger, shouldAnimate) {
        if (shouldAnimate) {
            animProgress.snapTo(0f)
            animProgress.animateTo(
                1f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 900,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        } else {
            animProgress.snapTo(1f)
        }
    }

    val total = kills + deaths + zombies + juggernauts + ghosts
    val killsAngle = if (total > 0f) (kills / total) * 360f else 0f
    val zombiesAngle = if (total > 0f) (zombies / total) * 360f else 0f
    val juggernautsAngle = if (total > 0f) (juggernauts / total) * 360f else 0f
    val ghostsAngle = if (total > 0f) (ghosts / total) * 360f else 0f
    val deathsAngle = if (total > 0f) (360f - killsAngle - zombiesAngle - juggernautsAngle - ghostsAngle).coerceAtLeast(0f) else 0f

    val progress = animProgress.value
    val curKillsAngle = killsAngle * progress
    val curZombiesAngle = zombiesAngle * progress
    val curJuggernautsAngle = juggernautsAngle * progress
    val curGhostsAngle = ghostsAngle * progress
    val curDeathsAngle = deathsAngle * progress

    val killsGradient = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(AppColors.GradientBlueStart, AppColors.GradientBlueEnd))
    val zombiesGradient = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF538236), Color(0xFF456C2D)))
    val juggernautsGradient = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFD32F2F), Color(0xFFB71C1C)))
    val ghostsGradient = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFFC8C8C8), Color(0xFF9E9E9E)))
    val deathsGradient = androidx.compose.ui.graphics.SolidColor(AppColors.GradientOrangeStart)

    val labelKills = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.kills)
    val labelDeaths = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.death)
    val labelZombies = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.zombie)
    val labelJuggernauts = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.juggernaut)
    val labelGhosts = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ghosts)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(chartSizeDp.dp)
                .background(AppColors.Overlay05, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                val topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
                val arcSizeObj = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)

                drawCircle(
                    color = AppColors.TextDarkGray.copy(alpha = 0.25f),
                    radius = radius,
                    center = center,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )

                if (total > 0f) {
                    var curAngle = -90f

                    if (curKillsAngle > 0.05f) {
                        drawArc(
                            brush = killsGradient,
                            startAngle = curAngle,
                            sweepAngle = curKillsAngle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
                            size = arcSizeObj,
                            topLeft = topLeft
                        )
                    }
                    curAngle += curKillsAngle

                    if (curZombiesAngle > 0.05f) {
                        drawArc(
                            brush = zombiesGradient,
                            startAngle = curAngle,
                            sweepAngle = curZombiesAngle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
                            size = arcSizeObj,
                            topLeft = topLeft
                        )
                    }
                    curAngle += curZombiesAngle

                    if (curJuggernautsAngle > 0.05f) {
                        drawArc(
                            brush = juggernautsGradient,
                            startAngle = curAngle,
                            sweepAngle = curJuggernautsAngle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
                            size = arcSizeObj,
                            topLeft = topLeft
                        )
                    }
                    curAngle += curJuggernautsAngle

                    if (curGhostsAngle > 0.05f) {
                        drawArc(
                            brush = ghostsGradient,
                            startAngle = curAngle,
                            sweepAngle = curGhostsAngle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
                            size = arcSizeObj,
                            topLeft = topLeft
                        )
                    }
                    curAngle += curGhostsAngle

                    if (curDeathsAngle > 0.05f) {
                        drawArc(
                            brush = deathsGradient,
                            startAngle = curAngle,
                            sweepAngle = curDeathsAngle,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
                            size = arcSizeObj,
                            topLeft = topLeft
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.height((chartSizeDp * 0.58f).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val kdFontSize = when {
                    chartSizeDp <= 66 -> if (kdValText.length <= 4) 16.sp else 13.sp
                    kdValText.length <= 4 -> 22.sp
                    kdValText.length == 5 -> 18.sp
                    kdValText.length == 6 -> 15.sp
                    else -> 12.sp
                }
                Text(
                    text = kdValText,
                    fontSize = kdFontSize,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.White,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = "K/D",
                    fontSize = if (chartSizeDp <= 66) 8.sp else 9.5.sp,
                    color = AppColors.White.copy(alpha = 0.75f),
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Clean strict leader lines pointing strictly from each color slice outward
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            if (total > 0f) {
                val rOut = (chartSizeDp / 2f).dp.toPx()
                val cx = size.width / 2f
                val cy = size.height / 2f

                class CalloutItem(
                    val label: String,
                    val midAngle: Float
                )

                val minDisplayAngle = 4f
                val candidateCallouts = mutableListOf<CalloutItem>()

                var angleAcc = -90f
                if (kills > 0f) {
                    if (killsAngle >= minDisplayAngle) {
                        candidateCallouts.add(CalloutItem(labelKills, angleAcc + killsAngle / 2f))
                    }
                    angleAcc += killsAngle
                }
                if (zombies > 0f) {
                    if (zombiesAngle >= minDisplayAngle) {
                        candidateCallouts.add(CalloutItem(labelZombies, angleAcc + zombiesAngle / 2f))
                    }
                    angleAcc += zombiesAngle
                }
                if (juggernauts > 0f) {
                    if (juggernautsAngle >= minDisplayAngle) {
                        candidateCallouts.add(CalloutItem(labelJuggernauts, angleAcc + juggernautsAngle / 2f))
                    }
                    angleAcc += juggernautsAngle
                }
                if (ghosts > 0f) {
                    if (ghostsAngle >= minDisplayAngle) {
                        candidateCallouts.add(CalloutItem(labelGhosts, angleAcc + ghostsAngle / 2f))
                    }
                    angleAcc += ghostsAngle
                }
                if (deaths > 0f) {
                    if (deathsAngle >= minDisplayAngle) {
                        candidateCallouts.add(CalloutItem(labelDeaths, angleAcc + deathsAngle / 2f))
                    }
                    angleAcc += deathsAngle
                }

                val linePhase = ((progress - 0.2f) / 0.5f).coerceIn(0f, 1f)
                val textAlpha = ((progress - 0.45f) / 0.55f).coerceIn(0f, 1f)

                if (linePhase > 0f && candidateCallouts.isNotEmpty()) {
                    val lineColor = Color.White.copy(alpha = 0.45f * linePhase)
                    val textPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.argb((225 * textAlpha).toInt(), 255, 255, 255)
                        textSize = 7.5.sp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }

                    class ProcessedCallout(
                        val label: String,
                        val p0X: Float,
                        val p0Y: Float,
                        val p1X: Float,
                        val p1Y: Float,
                        var targetY: Float,
                        val isRight: Boolean
                    )

                    val radialExt = 5.dp.toPx()
                    val maxBottomY = cy + (rOut + radialExt) * kotlin.math.sin(Math.toRadians(83.0)).toFloat()

                    val processed = candidateCallouts.mapNotNull { item ->
                        val normAngle = ((item.midAngle % 360f) + 360f) % 360f
                        if (normAngle in 83f..97f) return@mapNotNull null
                        val rad = Math.toRadians(item.midAngle.toDouble())
                        val cosA = kotlin.math.cos(rad).toFloat()
                        val sinA = kotlin.math.sin(rad).toFloat()
                        val p0X = cx + rOut * cosA
                        val p0Y = cy + rOut * sinA
                        val p1X = cx + (rOut + radialExt) * cosA
                        val p1Y = cy + (rOut + radialExt) * sinA
                        val isRight = cosA >= 0f
                        ProcessedCallout(item.label, p0X, p0Y, p1X, p1Y, p1Y, isRight)
                    }

                    // фильтрация и смещение выносок по вертикали
                    val minGapY = 11.dp.toPx()
                    val visibleCallouts = mutableListOf<ProcessedCallout>()
                    for (isRightSide in listOf(true, false)) {
                        val sideList = processed.filter { it.isRight == isRightSide }.sortedBy { it.targetY }
                        var prev: ProcessedCallout? = null
                        for (curr in sideList) {
                            val prevItem = prev
                            if (prevItem != null && curr.targetY - prevItem.targetY < minGapY) {
                                val nextY = prevItem.targetY + minGapY
                                if (nextY > maxBottomY) {
                                    continue
                                }
                                curr.targetY = nextY
                            }
                            if (curr.targetY <= maxBottomY) {
                                visibleCallouts.add(curr)
                                prev = curr
                            }
                        }
                    }

                    for (item in visibleCallouts) {
                        val radialProgress = (linePhase / 0.5f).coerceIn(0f, 1f)
                        val curP1X = item.p0X + (item.p1X - item.p0X) * radialProgress
                        val curP1Y = item.p0Y + (item.p1Y - item.p0Y) * radialProgress

                        drawLine(
                            color = lineColor,
                            start = androidx.compose.ui.geometry.Offset(item.p0X, item.p0Y),
                            end = androidx.compose.ui.geometry.Offset(curP1X, curP1Y),
                            strokeWidth = 1.dp.toPx()
                        )

                        if (linePhase > 0.5f) {
                            val seg2Progress = ((linePhase - 0.5f) / 0.5f).coerceIn(0f, 1f)
                            val fullStickLen = 8.5.dp.toPx()
                            val curStickLen = fullStickLen * seg2Progress
                            val p2X = if (item.isRight) item.p1X + curStickLen else item.p1X - curStickLen

                            drawLine(
                                color = lineColor,
                                start = androidx.compose.ui.geometry.Offset(item.p1X, item.p1Y),
                                end = androidx.compose.ui.geometry.Offset(p2X, item.targetY),
                                strokeWidth = 1.dp.toPx()
                            )

                            if (textAlpha > 0.05f) {
                                textPaint.textAlign = if (item.isRight) android.graphics.Paint.Align.LEFT else android.graphics.Paint.Align.RIGHT
                                val fullP2X = if (item.isRight) item.p1X + fullStickLen else item.p1X - fullStickLen
                                val textX = if (item.isRight) fullP2X + 2.dp.toPx() else fullP2X - 2.dp.toPx()
                                val textY = item.targetY + 2.5.dp.toPx()
                                drawContext.canvas.nativeCanvas.drawText(item.label, textX, textY, textPaint)
                            }
                        }
                    }
                }
            }
        }
    }
}

