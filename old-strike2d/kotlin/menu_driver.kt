package skayrux.shootout2d

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import android.content.Context
import android.view.View
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.SystemClock
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlinx.coroutines.delay

data class SessionConfig(
    val mapUid: String,
    val mapName: String,
    val width: Int,
    val height: Int,
    val defaultTerrain: String,
    val ownerId: String,
    val isLoad: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun driver_menu(
    initialIndex: Int = 0,
    navTrigger: Long = 0L,
    onIndexChanged: (Int) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    remember {
        if (sys_menu_socket_repository.currentPlayerId.isEmpty()) {
            sys_menu_socket_repository.currentPlayerName = AppSettings.getString(context, "driver_player_name", "Player")
            sys_menu_socket_repository.currentPlayerIsRegistered = AppSettings.getBoolean(context, "driver_is_registered", false)
            sys_menu_socket_repository.currentPlayerRegisteredLogin = AppSettings.getString(context, "driver_registered_login", "")
            sys_menu_socket_repository.currentPlayerPassword = AppSettings.getString(context, "driver_password", "")
            sys_menu_socket_repository.currentPlayerId = AppSettings.getString(context, "driver_account_id", "")
            sys_menu_socket_repository.currentPlayerDescription = AppSettings.getString(context, "driver_description", skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_informatsiya_ne_ukazana_40d9))
        }
        true
    }
    var selectedIndex by remember { mutableIntStateOf(initialIndex) }
    var friendsInitialTab by remember { mutableStateOf("friends") }
    var inventoryInitialTab by remember { mutableStateOf("inventory") }
    var lastClickTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    val handleNavigationTarget: (String) -> Unit = { target ->
        val clean = target.trim().lowercase()
        when (clean) {
            "friends" -> {
                friendsInitialTab = "friends"
                selectedIndex = 3
            }
            "profile" -> {
                selectedIndex = 5
            }
            "inventory" -> {
                inventoryInitialTab = "inventory"
                selectedIndex = 1
            }
            "inventory-rynok" -> {
                inventoryInitialTab = "market"
                selectedIndex = 1
            }
            "inventory-magazin" -> {
                inventoryInitialTab = "shop"
                selectedIndex = 1
            }
            "map-editor" -> {
                selectedIndex = 2
            }
            "settings" -> {
                selectedIndex = 4
            }
            "news" -> {
                selectedIndex = 8
            }
            "leaders" -> {
                selectedIndex = 7
            }
            "play" -> {
                selectedIndex = 6
            }
        }
    }
    LaunchedEffect(initialIndex, navTrigger) {
        if (initialIndex in 0..9) {
            selectedIndex = initialIndex
        }
    }
    LaunchedEffect(selectedIndex) {
        onIndexChanged(selectedIndex)
        if (selectedIndex != 3) {
            friendsInitialTab = "friends"
        }
        if (selectedIndex != 4 && LiveBackgroundState.isEditingControls) {
            LiveBackgroundState.isEditingControls = false
        }
    }
    val playerName = sys_menu_socket_repository.currentPlayerName
    val isRegistered = sys_menu_socket_repository.currentPlayerIsRegistered
    val registeredLogin = sys_menu_socket_repository.currentPlayerRegisteredLogin
    val password = sys_menu_socket_repository.currentPlayerPassword
    val accountId = sys_menu_socket_repository.currentPlayerId
    val description = sys_menu_socket_repository.currentPlayerDescription
    val serebroCoins = sys_menu_socket_repository.currentPlayerSerebro
    val zolotoCoins = sys_menu_socket_repository.currentPlayerZoloto
    var userAvatar by remember {
        val savedAvatar = AppSettings.getString(context, "driver_user_avatar", "")
        mutableStateOf<String?>(if (savedAvatar.isEmpty()) null else savedAvatar)
    }
    var showExitDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = true) {
        if (LiveBackgroundState.isEditingControls) {
            LiveBackgroundState.isEditingControls = false
        } else {
            if (selectedIndex == 9) {
                selectedIndex = 5
            } else if (selectedIndex != 0) {
                selectedIndex = 0
            } else {
                showExitDialog = true
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        ResourceCache.stopAllStreamsForce()
        LiveBackgroundState.isMenuVisible = true
        if (selectedIndex != 4) {
            LiveBackgroundState.isEditingControls = false
        }
    }

    androidx.compose.runtime.LaunchedEffect(playerName) {
        AppSettings.setString(context, "driver_player_name", playerName)
    }
    androidx.compose.runtime.LaunchedEffect(isRegistered) {
        AppSettings.setBoolean(context, "driver_is_registered", isRegistered)
    }
    androidx.compose.runtime.LaunchedEffect(registeredLogin) {
        AppSettings.setString(context, "driver_registered_login", registeredLogin)
    }
    androidx.compose.runtime.LaunchedEffect(password) {
        AppSettings.setString(context, "driver_password", password)
    }
    androidx.compose.runtime.LaunchedEffect(accountId) {
        AppSettings.setString(context, "driver_account_id", accountId)
    }
    androidx.compose.runtime.LaunchedEffect(description) {
        AppSettings.setString(context, "driver_description", description)
    }
    androidx.compose.runtime.LaunchedEffect(userAvatar) {
        AppSettings.setString(context, "driver_user_avatar", userAvatar ?: "")
    }

    LaunchedEffect(Unit) {
        var iterationCount = 0
        while (true) {
            try {
                if (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == false) {
                    throw kotlinx.coroutines.CancellationException("Cancelled")
                }
                // проверка локальной блокировки
                if (AppSettings.getBoolean(context, "is_banned_locally", false)) {
                    val savedAcc = AppSettings.getString(context, "banned_account_id", "N/A")
                    val savedIssued = AppSettings.getString(context, "banned_issued_time", "")
                    GlobalSecurityDialogState.trigger(savedAcc, savedIssued)
                    break
                }
                sys_menu_socket_repository.syncFullAccountData()
                if (GlobalSecurityDialogState.isVisible || GlobalTempBanDialogState.isVisible) {
                    break
                }
                val myId = sys_menu_socket_repository.currentPlayerId
                if (myId.isNotEmpty() && sys_menu_socket_repository.isServerOnline) {
                    iterationCount++
                    if (iterationCount >= 5) {
                        iterationCount = 0
                        sys_menu_socket_repository.getUserAvatar(myId, forceRefresh = true)
                        val currentJoinedGroupId = sys_menu_socket_repository.userJoinedGroup
                        if (!currentJoinedGroupId.isNullOrEmpty()) {
                            sys_menu_socket_repository.getGroupAvatar(currentJoinedGroupId, forceRefresh = true)
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(4000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
            StaticBackground()
            val videoPath = LiveBackgroundState.videoPath
            val isVideoValid = remember(videoPath) {
                videoPath.isNotEmpty() && java.io.File(videoPath).exists()
            }
            if (isVideoValid) {
                VideoBackground(videoPath, LiveBackgroundState.soundEnabled, LiveBackgroundState.volume)
            }
            MenuSmokeOverlay()
            Box(modifier = Modifier.fillMaxSize()) {
                val isSearchFocused = selectedIndex == 3 && skayrux.shootout2d.MenuFriendsState.isSearchFieldFocused
                val isFriendsDialogOpen = selectedIndex == 3 && skayrux.shootout2d.MenuFriendsState.isAnyDialogOpen
                val isLeadersDialogOpen = selectedIndex == 7 && skayrux.shootout2d.MenuLeadersState.isProfileDialogOpen
                val isEditingHud = selectedIndex == 4 && LiveBackgroundState.isEditingControls

                if (!isEditingHud) {
                    Box(
                        modifier = Modifier
                            .align(androidx.compose.ui.Alignment.CenterStart)
                            .zIndex(2f)
                    ) {
                        val sideMenuIndex = when (selectedIndex) {
                            6, 7, 8 -> 0
                            else -> selectedIndex
                        }
                        bokovoe_menu(sideMenuIndex) { targetIndex ->
                            val now = System.currentTimeMillis()
                            if (now - lastClickTime >= 50L) {
                                lastClickTime = now
                                LiveBackgroundState.isEditingControls = false
                                selectedIndex = targetIndex
                                if (targetIndex == 3) friendsInitialTab = "friends"
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (!isEditingHud) {
                                Modifier.padding(start = 61.dp)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    when (selectedIndex) {
                        0 -> home_menu(playerName, serebroCoins, zolotoCoins, userAvatar, 
                            onProfileClick = { selectedIndex = 5 },
                            onPlayClick = { selectedIndex = 6 },
                            onAddFriendClick = { friendsInitialTab = "search"; selectedIndex = 3 },
                            onLeadersClick = { selectedIndex = 7 },
                            onNewsClick = { selectedIndex = 8 },
                            onNavigateTarget = handleNavigationTarget
                        )
                        1 -> inventory_menu(inventoryInitialTab)
                        2 -> mapeditor_menu(
                            accountId = accountId,
                            onStartSession = { name, width, height, terrain ->
                                val intent = android.content.Intent(context, datagame_main::class.java).apply {
                                    putExtra("ACTION_MAP_EDITOR", true)
                                    putExtra("MAP_NAME", name)
                                    putExtra("MAP_WIDTH", width)
                                    putExtra("MAP_HEIGHT", height)
                                    putExtra("DEFAULT_TERRAIN", terrain)
                                    putExtra("ACCOUNT_ID", accountId)
                                    putExtra("APP_LANGUAGE", AppLanguageManager.currentLanguage)
                                }
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            },
                            onLoadSession = { mapUid ->
                                val intent = android.content.Intent(context, datagame_main::class.java).apply {
                                    putExtra("ACTION_MAP_EDITOR", true)
                                    putExtra("LOAD_MAP_UID", mapUid)
                                    putExtra("ACCOUNT_ID", accountId)
                                    putExtra("APP_LANGUAGE", AppLanguageManager.currentLanguage)
                                }
                                context.startActivity(intent)
                                (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            }
                        )
                        3 -> friends_menu(playerName, friendsInitialTab)
                        4 -> settings_menu()
                        5 -> my_profile_menu(
                            playerName = playerName,
                            isRegistered = isRegistered,
                            registeredLogin = registeredLogin,
                            accountId = accountId,
                            description = description,
                            userAvatar = userAvatar,
                            isSettingsTabActive = false,
                            onNameChange = { sys_menu_socket_repository.currentPlayerName = it },
                            onRegisterChange = { sys_menu_socket_repository.currentPlayerIsRegistered = it },
                            onRegisteredLoginChange = { sys_menu_socket_repository.currentPlayerRegisteredLogin = it },
                            onIdChange = { sys_menu_socket_repository.currentPlayerId = it },
                            onDescriptionChange = { sys_menu_socket_repository.currentPlayerDescription = it },
                            onBack = { selectedIndex = 0 },
                            onBackToProfile = { selectedIndex = 5 }
                        )
                        6 -> create_game_menu(
                            accountId = accountId,
                            onBack = { selectedIndex = 0 }
                        )
                        7 -> leaders_menu(onBack = { selectedIndex = 0 })
                        8 -> novosti_menu(onBack = { selectedIndex = 0 })
                        9 -> my_profile_menu(
                            playerName = playerName,
                            isRegistered = isRegistered,
                            registeredLogin = registeredLogin,
                            accountId = accountId,
                            description = description,
                            userAvatar = userAvatar,
                            isSettingsTabActive = true,
                            onNameChange = { sys_menu_socket_repository.currentPlayerName = it },
                            onRegisterChange = { sys_menu_socket_repository.currentPlayerIsRegistered = it },
                            onRegisteredLoginChange = { sys_menu_socket_repository.currentPlayerRegisteredLogin = it },
                            onIdChange = { sys_menu_socket_repository.currentPlayerId = it },
                            onDescriptionChange = { sys_menu_socket_repository.currentPlayerDescription = it },
                            onBack = { selectedIndex = 0 },
                            onBackToProfile = { selectedIndex = 5 }
                        )
                    }
                }
            }
        }

        if (showExitDialog) {
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_hotite_vyyti_iz_igry_dc66),
                content = { },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.sign_out),
                onConfirm = {
                    GlobalReconnectState.isIntentionalDisconnect = true
                    sys_menu_socket_repository.isAppInForeground = false
                    sys_menu_socket_repository.disconnectAndForceOffline()
                    val activity = context as? android.app.Activity
                    activity?.finish()
                },
                onDismiss = { showExitDialog = false }
            )
        }
    }

class StretchVideoView(context: android.content.Context) : android.widget.VideoView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(0, widthMeasureSpec)
        val height = getDefaultSize(0, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
}

@Composable
fun VideoBackground(videoUriString: String, soundEnabled: Boolean, volume: Float) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uri = remember(videoUriString) { android.net.Uri.parse(videoUriString) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    androidx.compose.runtime.LaunchedEffect(mediaPlayerRef, soundEnabled, volume) {
        mediaPlayerRef?.let { mp ->
            try {
                if (soundEnabled) {
                    val vol = volume / 100f
                    mp.setVolume(vol, vol)
                } else {
                    mp.setVolume(0f, 0f)
                }
            } catch (e: Exception) {}
        }
    }

    val videoView = remember {
        StretchVideoView(context).apply {
            setOnPreparedListener { mp ->
                mp.isLooping = true
                mediaPlayerRef = mp
                if (soundEnabled) {
                    val vol = volume / 100f
                    mp.setVolume(vol, vol)
                } else {
                    mp.setVolume(0f, 0f)
                }
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, videoUriString) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                videoView.pause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                videoView.setVideoURI(uri)
                videoView.start()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        videoView.setVideoURI(uri)
        videoView.start()
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                videoView.stopPlayback()
                mediaPlayerRef?.release()
                mediaPlayerRef = null
            } catch (e: Exception) {}
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { videoView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun StaticBackground() {
    val imagePath = LiveBackgroundState.imagePath
    val isImageValid = remember(imagePath) {
        imagePath.isNotEmpty() && java.io.File(imagePath).exists()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (isImageValid) {
            val bitmap = remember(imagePath) {
                try {
                    BitmapFactory.decodeFile(imagePath)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                DefaultGradientBackground()
            }
        } else {
            DefaultGradientBackground()
        }
    }
}

// фоновое изображение меню
@Composable
fun DefaultGradientBackground() {
    Image(
        painter = painterResource(R.drawable.fonchik),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
}

// класс частицы дыма в меню
class MenuSmokeParticle(
    var x: Float,
    var y: Float,
    var size: Float,
    var speed: Float,
    var angle: Float
)

// аппаратная отрисовка дыма без нагрузки на compose
class SmokeHardwareView(context: Context) : View(context) {
    private val smokeBitmap = ResourceCache.getSmkAtlas(context)?.asAndroidBitmap()
    private val matrix = Matrix()
    private val paint = Paint().apply {
        isFilterBitmap = true
        isAntiAlias = false
        alpha = (0.03f * 255f).toInt().coerceIn(1, 255)
    }

    private val baseW = 1280f
    private val baseH = 720f
    private val particleCount = 8
    private val particles: Array<MenuSmokeParticle>
    private var lastTimeMs = 0L

    init {
        // начальное распределение облаков с просветами 30-40%
        val spacing = (baseW + 600f) / particleCount
        particles = Array(particleCount) { i ->
            val startX = -300f + i * spacing + (kotlin.random.Random.nextFloat() * 80f - 40f)
            val startY = when (i % 4) {
                0 -> baseH * 0.15f
                1 -> baseH * 0.75f
                2 -> baseH * 0.45f
                else -> baseH * 0.85f
            } + (kotlin.random.Random.nextFloat() * 60f - 30f)
            val size = 760f + kotlin.random.Random.nextFloat() * 120f
            val speed = 0.55f + kotlin.random.Random.nextFloat() * 0.1f
            val angle = kotlin.random.Random.nextFloat() * 360f
            MenuSmokeParticle(startX, startY, size, speed, angle)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastTimeMs = SystemClock.uptimeMillis()
        postInvalidateOnAnimation()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) {
            lastTimeMs = SystemClock.uptimeMillis()
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = smokeBitmap ?: return
        val now = SystemClock.uptimeMillis()
        val dt = if (lastTimeMs == 0L) 1f else ((now - lastTimeMs) / 16.6667f).coerceIn(0.1f, 3f)
        lastTimeMs = now

        val scaleX = width.toFloat() / baseW
        val scaleY = height.toFloat() / baseH
        val scaleAvg = (scaleX + scaleY) / 2f
        val bmpW = bmp.width.toFloat()
        val bmpH = bmp.height.toFloat()

        for (p in particles) {
            p.x += p.speed * dt
            p.size += (p.speed / 2.5f) * dt
            p.angle = (p.angle + 0.032f * dt) % 360f

            // при уходе за правый край плавный возврат слева
            if (p.x - p.size / 2f > baseW) {
                p.x = -p.size / 2f - kotlin.random.Random.nextFloat() * 120f
                p.y = kotlin.random.Random.nextFloat() * baseH
                p.size = 760f + kotlin.random.Random.nextFloat() * 120f
                p.speed = 0.55f + kotlin.random.Random.nextFloat() * 0.1f
                p.angle = kotlin.random.Random.nextFloat() * 360f
            }

            val cx = p.x * scaleX
            val cy = p.y * scaleY
            val drawSize = p.size * scaleAvg
            val half = drawSize / 2f

            matrix.reset()
            matrix.postScale(drawSize / bmpW, drawSize / bmpH)
            matrix.postRotate(p.angle, half, half)
            matrix.postTranslate(cx - half, cy - half)
            canvas.drawBitmap(bmp, matrix, paint)
        }

        if (visibility == VISIBLE && isAttachedToWindow) {
            postInvalidateOnAnimation()
        }
    }
}

// компонент наложения дыма
@Composable
fun MenuSmokeOverlay(modifier: Modifier = Modifier) {
    if (!LiveBackgroundState.smokeEnabled) return
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context -> SmokeHardwareView(context) },
        modifier = modifier.fillMaxSize()
    )
}

