package skayrux.shootout2d

import androidx.compose.material.icons.outlined.Lock
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context

@Composable
fun TrashIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(12.dp)) {
        val w = size.width
        val h = size.height
        drawLine(AppColors.White, androidx.compose.ui.geometry.Offset(w * 0.15f, h * 0.25f), androidx.compose.ui.geometry.Offset(w * 0.85f, h * 0.25f), strokeWidth = w * 0.1f)
        drawRect(AppColors.White, androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.12f), androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.13f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.08f))
        drawRect(AppColors.White, androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.35f), androidx.compose.ui.geometry.Size(w * 0.5f, h * 0.53f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.08f))
        drawLine(AppColors.White, androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.45f), androidx.compose.ui.geometry.Offset(w * 0.42f, h * 0.75f), strokeWidth = w * 0.08f)
        drawLine(AppColors.White, androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.45f), androidx.compose.ui.geometry.Offset(w * 0.58f, h * 0.75f), strokeWidth = w * 0.08f)
    }
}

@Composable
fun settings_menu(isInGame: Boolean = false) {
    var selectedTab by remember { mutableStateOf(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igra_e059)) }

    // сброс режима настройки управления при выходе из меню настроек
    DisposableEffect(Unit) {
        onDispose {
            LiveBackgroundState.isEditingControls = false
        }
    }

    if (LiveBackgroundState.isEditingControls) {
        menu_upravleniye(onBack = { LiveBackgroundState.isEditingControls = false })
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val tabs = listOf(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igra_e059), skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_video_a095), skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_upravlenie_6fdd), skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.sound), skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_prochee_57af))
                tabs.forEach { tab ->
                    SettingsTabButton(
                        text = tab,
                        isSelected = selectedTab == tab,
                        onClick = {
                            LiveBackgroundState.isEditingControls = false
                            selectedTab = tab
                        }
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Overlay10)
            )
            
            Spacer(Modifier.height(10.dp))

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                androidx.compose.animation.Crossfade(
                    targetState = selectedTab,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 50),
                    label = "settings_tab_crossfade"
                ) { tab ->
                    when (tab) {
                        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igra_e059) -> TabGame(isInGame = isInGame)
                        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_video_a095) -> TabVideo(isInGame = isInGame)
                        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_upravlenie_6fdd) -> TabControls(onConfigureControls = { LiveBackgroundState.isEditingControls = true })
                        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.sound) -> TabAudio()
                        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_prochee_57af) -> TabMisc(isInGame = isInGame)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabGame(isInGame: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var friendlyFire by remember { mutableStateOf(AppSettings.getBoolean(context, "friendlyFire", false)) }
    var autobalance by remember { mutableStateOf(AppSettings.getBoolean(context, "autobalance", true)) }
    var weaponsAtStart by remember { mutableStateOf(AppSettings.getBoolean(context, "weaponsAtStart", true)) }
    var spectateEnemy by remember { mutableStateOf(AppSettings.getBoolean(context, "spectate_enemy", true)) }

    var drawAlive by remember { mutableStateOf(AppSettings.getBoolean(context, "draw_alive", true)) }
    var autoUse by remember { mutableStateOf(AppSettings.getBoolean(context, "auto_use", true)) }
    var blackText by remember { mutableStateOf(AppSettings.getBoolean(context, "black_text", false)) }

    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igrovoy_protsess_dlya_vseh_79dd))
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.frendly_fire), isLocked = isInGame) {
                    CustomSwitch(friendlyFire, enabled = !isInGame) {
                        if (!isInGame) {
                            friendlyFire = it
                            AppSettings.setBoolean(context, "friendlyFire", it)
                        }
                    }
                }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.autobalance), isLocked = isInGame) {
                    CustomSwitch(autobalance, enabled = !isInGame) {
                        if (!isInGame) {
                            autobalance = it
                            AppSettings.setBoolean(context, "autobalance", it)
                        }
                    }
                }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.weapons_on_the_map), isLocked = isInGame) {
                    CustomSwitch(weaponsAtStart, enabled = !isInGame) {
                        if (!isInGame) {
                            weaponsAtStart = it
                            AppSettings.setBoolean(context, "weaponsAtStart", it)
                        }
                    }
                }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.spectate_enemy_title), drawBottomBorder = false, isLocked = isInGame) {
                    CustomSwitch(spectateEnemy, enabled = !isInGame) {
                        if (!isInGame) {
                            spectateEnemy = it
                            AppSettings.setBoolean(context, "spectate_enemy", it)
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.gameplay_local_title))
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.alive_players_counter)) {
                    CustomSwitch(drawAlive) {
                        drawAlive = it
                        AppSettings.setBoolean(context, "draw_alive", it)
                        datagame_main.instance?.reloadInGameSettings()
                    }
                }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.auto_pickup_items)) {
                    CustomSwitch(autoUse) {
                        autoUse = it
                        AppSettings.setBoolean(context, "auto_use", it)
                        datagame_main.instance?.reloadInGameSettings()
                    }
                }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.black_text_color), drawBottomBorder = false) {
                    CustomSwitch(blackText) {
                        blackText = it
                        AppSettings.setBoolean(context, "black_text", it)
                        datagame_main.instance?.reloadInGameSettings()
                    }
                }
            }
        }
    }
}

@Composable
private fun TabVideo(isInGame: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var bloom by remember { mutableStateOf(AppSettings.getBoolean(context, "bloom", false)) }
    var shadows by remember { mutableStateOf(AppSettings.getBoolean(context, "shadows", false)) }
    var centerCamera by remember { mutableStateOf(AppSettings.getBoolean(context, "centerCamera", true)) }
    var smoothCamera by remember { mutableStateOf(AppSettings.getBoolean(context, "smoothCamera", false)) }
    
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    
    var isPickerLaunching by remember { mutableStateOf(false) }
    var lastPickerLaunchTime by remember { mutableStateOf(0L) }
    val pickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        isPickerLaunching = false
        if (uri != null) {
            selectedUri = uri
        }
    }

    var showImageDialog by remember { mutableStateOf(false) }
    var showDeleteImageDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    var isImagePickerLaunching by remember { mutableStateOf(false) }
    var lastImagePickerLaunchTime by remember { mutableStateOf(0L) }
    val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        isImagePickerLaunching = false
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    val isAnyDialogOpen = showBackgroundDialog || showDeleteDialog || showImageDialog || showDeleteImageDialog
    val isLiveActive = LiveBackgroundState.videoPath.isNotEmpty()
    val isStaticActive = LiveBackgroundState.imagePath.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igrovoy_protsess_e950))
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_svechenie_4fa6), isLocked = true) { CustomSwitch(bloom, enabled = false) { bloom = it; AppSettings.setBoolean(context, "bloom", it) } }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_teni_bc83), isLocked = true) { CustomSwitch(shadows, enabled = false) { shadows = it; AppSettings.setBoolean(context, "shadows", it) } }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.center_camera)) {
                    CustomSwitch(centerCamera) {
                        centerCamera = it
                        AppSettings.setBoolean(context, "centerCamera", it)
                        datagame_main.instance?.reloadInGameSettings()
                    }
                }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_plavnaya_kamera_7db1), drawBottomBorder = false) {
                    CustomSwitch(smoothCamera) {
                        smoothCamera = it
                        AppSettings.setBoolean(context, "smoothCamera", it)
                        datagame_main.instance?.reloadInGameSettings()
                    }
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_interfeys_4f3b))
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vkl_dym_na_fone), drawBottomBorder = true) {
                    CustomSwitch(LiveBackgroundState.smokeEnabled) {
                        LiveBackgroundState.smokeEnabled = it
                        AppSettings.setBoolean(context, "smoke_background", it)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            drawLine(
                                color = AppColors.White.copy(alpha = 0.05f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsActionButton(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_statichnyy_fon_3de6),
                        isDanger = isStaticActive,
                        enabled = !isInGame && !isLiveActive,
                        isLocked = isInGame,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!isInGame && !isAnyDialogOpen) {
                            if (isStaticActive) {
                                showDeleteImageDialog = true
                            } else {
                                showImageDialog = true
                            }
                        }
                    }
                    SettingsActionButton(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zhivoy_fon_7822),
                        isDanger = isLiveActive,
                        enabled = !isInGame && !isStaticActive,
                        isLocked = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!isInGame && !isAnyDialogOpen) {
                            if (isLiveActive) {
                                showDeleteDialog = true
                            } else {
                                showBackgroundDialog = true
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showBackgroundDialog) {
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzit_video_70d2),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .drawBehind {
                            val stroke = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                            drawRoundRect(
                                color = AppColors.SemiWhite66,
                                style = stroke,
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                        .background(AppColors.InputContainer, RoundedCornerShape(8.dp))
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (!isPickerLaunching && now - lastPickerLaunchTime > 1000L) {
                                isPickerLaunching = true
                                lastPickerLaunchTime = now
                                pickerLauncher.launch("video/mp4")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vybrat_mp4_video_b226), color = AppColors.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_video_vybrano_a2af), color = AppColors.TextGreenSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_primenit_2cd8),
            confirmEnabled = selectedUri != null,
            onConfirm = {
                selectedUri?.let { uri ->
                    val path = copyVideoToInternalStorage(context, uri)
                    if (path != null) {
                        LiveBackgroundState.videoPath = path
                        AppSettings.setString(context, "live_background_path", path)
                    }
                }
                showBackgroundDialog = false
            },
            onDismiss = {
                selectedUri = null
                showBackgroundDialog = false
            }
        )
    }
    
    if (showDeleteDialog) {
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_udalit_zhivoy_fon_2306),
            content = {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_uvereny_chto_hotite_udalit__1b96),
                    color = AppColors.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
            onConfirm = {
                val path = LiveBackgroundState.videoPath
                if (path.isNotEmpty()) {
                    try {
                        java.io.File(path).delete()
                    } catch (e: Exception) {}
                }
                LiveBackgroundState.videoPath = ""
                AppSettings.setString(context, "live_background_path", "")
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    if (showImageDialog) {
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzit_statichnyy_fon_13d8),
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .drawBehind {
                            val stroke = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                            drawRoundRect(
                                color = AppColors.SemiWhite66,
                                style = stroke,
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                        .background(AppColors.InputContainer, RoundedCornerShape(8.dp))
                        .clickable {
                            val now = System.currentTimeMillis()
                            if (!isImagePickerLaunching && now - lastImagePickerLaunchTime > 1000L) {
                                isImagePickerLaunching = true
                                lastImagePickerLaunchTime = now
                                imagePickerLauncher.launch("image/*")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vybrat_kartinku_jpg_png_6c38), color = AppColors.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_kartinka_vybrana_8bc9), color = AppColors.TextGreenSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_primenit_2cd8),
            confirmEnabled = selectedImageUri != null,
            onConfirm = {
                selectedImageUri?.let { uri ->
                    val path = copyImageToInternalStorage(context, uri)
                    if (path != null) {
                        LiveBackgroundState.imagePath = path
                        AppSettings.setString(context, "live_background_image_path", path)
                    }
                }
                showImageDialog = false
            },
            onDismiss = {
                selectedImageUri = null
                showImageDialog = false
            }
        )
    }

    if (showDeleteImageDialog) {
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_udalit_statichnyy_fon_8435),
            content = {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_uvereny_chto_hotite_udalit__38fb),
                    color = AppColors.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
            onConfirm = {
                val path = LiveBackgroundState.imagePath
                if (path.isNotEmpty()) {
                    try {
                        java.io.File(path).delete()
                    } catch (e: Exception) {}
                }
                LiveBackgroundState.imagePath = ""
                AppSettings.setString(context, "live_background_image_path", "")
                showDeleteImageDialog = false
            },
            onDismiss = {
                showDeleteImageDialog = false
            }
        )
    }
}

@Composable
private fun CrosshairPreview(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(45.dp)
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, AppColors.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        TexturePreview(
            textureName = "t01",
            modifier = Modifier.fillMaxSize()
        )
        if (color.alpha > 0.001f) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(24.dp)) {
                val strokeWidth = 3.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = strokeWidth
                )
            }
        } else {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(22.dp)) {
                val strokeWidth = 2.dp.toPx()
                val radius = size.minDimension / 2 - strokeWidth
                val center = Offset(size.width / 2, size.height / 2)
                val iconColor = AppColors.Gray99
                drawCircle(
                    color = iconColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                val angle = 45f
                val dx = radius * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat()
                val dy = radius * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                drawLine(
                    color = iconColor,
                    start = Offset(center.x - dx, center.y - dy),
                    end = Offset(center.x + dx, center.y + dy),
                    strokeWidth = strokeWidth
                )
            }
        }
    }
}

@Composable
private fun ColorAlphaSlider(
    baseColor: Color,
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnAlphaChange by rememberUpdatedState(onAlphaChange)

    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(3.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        val initialFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                        currentOnAlphaChange(initialFraction)
                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (!anyPressed) break
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change != null) {
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                currentOnAlphaChange(fraction)
                            }
                        }
                    }
                }
            }
    ) {
        val checkerSize = 4.dp.toPx()
        val cols = (size.width / checkerSize).toInt() + 1
        val rows = (size.height / checkerSize).toInt() + 1
        for (i in 0 until cols) {
            for (j in 0 until rows) {
                if ((i + j) % 2 == 0) {
                    drawRect(
                        color = AppColors.White.copy(alpha = 0.15f),
                        topLeft = Offset(i * checkerSize, j * checkerSize),
                        size = androidx.compose.ui.geometry.Size(checkerSize, checkerSize)
                    )
                } else {
                    drawRect(
                        color = AppColors.Black.copy(alpha = 0.15f),
                        topLeft = Offset(i * checkerSize, j * checkerSize),
                        size = androidx.compose.ui.geometry.Size(checkerSize, checkerSize)
                    )
                }
            }
        }

        drawRect(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(Color.Transparent, baseColor.copy(alpha = 1f))
            )
        )
        
        val thumbX = alpha * size.width
        drawRect(
            color = AppColors.White,
            topLeft = Offset(thumbX - 1.5.dp.toPx(), 0f),
            size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height)
        )
        drawRect(
            color = AppColors.Black.copy(alpha = 0.5f),
            topLeft = Offset(thumbX - 2.5.dp.toPx(), 0f),
            size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height)
        )
        drawRect(
            color = AppColors.Black.copy(alpha = 0.5f),
            topLeft = Offset(thumbX + 1.5.dp.toPx(), 0f),
            size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height)
        )
    }
}

private data class CrosshairColorItem(
    val id: String,
    val nameRes: Int,
    val color: Color
)

@Composable
private fun TabControls(onConfigureControls: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val crosshairs = remember {
        listOf(
            CrosshairColorItem("white", skayrux.shootout2d.R.string.whitepriz, AppColors.White),
            CrosshairColorItem("red", skayrux.shootout2d.R.string.str_krasnyy_4dfe, AppColors.Red),
            CrosshairColorItem("yellow", skayrux.shootout2d.R.string.yellowpriz, Color(0xFFFFD600)),
            CrosshairColorItem("blue", skayrux.shootout2d.R.string.str_siniy_5447, AppColors.Blue),
            CrosshairColorItem("lightblue", skayrux.shootout2d.R.string.bluepriz, AppColors.LightBlue),
            CrosshairColorItem("green", skayrux.shootout2d.R.string.greenpriz, AppColors.Green),
            CrosshairColorItem("orange", skayrux.shootout2d.R.string.orangepriz, AppColors.Orange),
            CrosshairColorItem("purple", skayrux.shootout2d.R.string.str_fioletovyy_04a7, AppColors.Purple),
            CrosshairColorItem("black", skayrux.shootout2d.R.string.blackpriz, AppColors.Black)
        )
    }

    val initialColorId = remember {
        val savedId = AppSettings.getCrosshairColorId(context)
        if (crosshairs.any { it.id == savedId }) savedId else "red"
    }

    var selectedColorId by remember { mutableStateOf(initialColorId) }
    var crosshairAlpha by remember {
        mutableStateOf(
            if (AppSettings.isCrosshairEnabled(context))
                AppSettings.getFloat(context, "crosshairAlpha", 0.9f).coerceIn(0f, 1f)
            else
                0f
        )
    }
    var uiEnable by remember { mutableStateOf(AppSettings.getUiEnable(context)) }

    val activeItem = crosshairs.firstOrNull { it.id == selectedColorId } ?: crosshairs[1]
    val activeColor = activeItem.color
    val activeName = skayrux.shootout2d.Loc.tr(activeItem.nameRes)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val displayName = if (crosshairAlpha <= 0.001f) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_bez_pritsela_48cc) else "${activeName.uppercase()} (${(crosshairAlpha * 100).toInt()}%)"
            CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.crosshair_title, displayName))
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppColors.PanelDark)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
                    .padding(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            crosshairs.forEach { item ->
                                val isColorSelected = selectedColorId == item.id && crosshairAlpha > 0.001f
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 18.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isColorSelected) item.color else item.color.copy(alpha = 0.4f))
                                        .border(
                                            width = if (isColorSelected) 1.5.dp else 1.dp,
                                            color = if (isColorSelected) AppColors.MediumGray85 else AppColors.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                        .clickable {
                                            selectedColorId = item.id
                                            val targetAlpha = if (crosshairAlpha <= 0.001f) 0.9f else crosshairAlpha
                                            crosshairAlpha = targetAlpha
                                            val name = skayrux.shootout2d.Loc.tr(item.nameRes)
                                            AppSettings.setCrosshair(context, item.id, item.color.toArgb(), targetAlpha, name)
                                        }
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        ColorAlphaSlider(
                            baseColor = activeColor,
                            alpha = crosshairAlpha,
                            onAlphaChange = { newAlpha ->
                                crosshairAlpha = newAlpha
                                AppSettings.setCrosshair(context, activeItem.id, activeColor.toArgb(), newAlpha, activeName)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    CrosshairPreview(
                        color = activeColor.copy(alpha = crosshairAlpha),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_upravlenie_6fdd))
            SettingRow(title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.show_hud_interface)) {
                CustomSwitch(
                    checked = uiEnable,
                    onCheckedChange = {
                        uiEnable = it
                        AppSettings.setUiEnable(context, it)
                    }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawWithContent {
                        drawContent()
                        drawLine(
                            color = AppColors.White.copy(alpha = 0.05f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsActionButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nastroit_hud_a56d),
                    enabled = false,
                    isLocked = true,
                    modifier = Modifier.weight(1f)
                ) { }
                SettingsActionButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nastroit_upravlenie_415c),
                    modifier = Modifier.weight(1f)
                ) { onConfigureControls() }
            }
        }
    }
}

@Composable
private fun TabAudio() {
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.sound))
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_gromkost_zvuka_fa25), drawBottomBorder = false) { 
                    CustomSlider(
                        value = LiveBackgroundState.volume,
                        onValueChange = { 
                            LiveBackgroundState.volume = it
                            AppSettings.setFloat(context, "volume", it)
                            datagame_main.instance?.reloadInGameSettings()
                        },
                        tooltipText = {
                            Text(
                                text = "${it.toInt()}%",
                                color = AppColors.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_interfeys_4f3b))
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zvuk_zhivogo_fona_86bd), drawBottomBorder = false, isLocked = true) {
                    CustomSwitch(LiveBackgroundState.soundEnabled, enabled = false) {
                        LiveBackgroundState.soundEnabled = it
                        AppSettings.setBoolean(context, "live_background_sound", it)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabMisc(isInGame: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLang = AppLanguageManager.currentLanguage
    val currentLangLabel = if (currentLang == "ru") skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.lang_ru) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.lang_en)
    var pendingLangChange by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    if (pendingLangChange != null && !isInGame) {
        val newLang = pendingLangChange!!
        val langName = if (newLang == "ru") skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.lang_ru) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.lang_en)
        val title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.change_language_title)
        val desc = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.change_language_desc, langName)
        val btnNo = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.no)
        val btnYes = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.yes)

        UnifiedDialog(
            title = title,
            content = {
                Text(
                    text = desc,
                    color = AppColors.White,
                    fontSize = 14.sp
                )
            },
            confirmText = btnYes,
            onConfirm = {
                AppLanguageManager.setLanguage(context, newLang)
                pendingLangChange = null
                val intent = android.content.Intent(context, system::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                (context as? android.app.Activity)?.finish()
            },
            onDismiss = { pendingLangChange = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_yazyk_language_fd62))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            drawContent()
                            drawLine(
                                color = AppColors.White.copy(alpha = 0.05f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .padding(vertical = 7.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.current_language, currentLangLabel),
                            color = if (isInGame) AppColors.Border else AppColors.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LanguageFlagButton(
                                drawableRes = R.drawable.ru,
                                isSelected = currentLang == "ru",
                                contentDescription = "Russian",
                                isLocked = isInGame,
                                onClick = {
                                    if (!isInGame && currentLang != "ru") {
                                        pendingLangChange = "ru"
                                    }
                                }
                            )
                            LanguageFlagButton(
                                drawableRes = R.drawable.gb,
                                isSelected = currentLang == "en",
                                contentDescription = "English",
                                isLocked = isInGame,
                                onClick = {
                                    if (!isInGame && currentLang != "en") {
                                        pendingLangChange = "en"
                                    }
                                }
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                CategoryTitle(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_monitoring_2fe6))
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_pokazyvat_fps_cbd6)) {
                    CustomSwitch(PerformanceMonitorState.showFps) {
                        PerformanceMonitorState.showFps = it
                        AppSettings.setBoolean(context, "show_fps", it)
                    }
                }
                SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_monitoring_seti_20b0), drawBottomBorder = false) {
                    CustomSwitch(PerformanceMonitorState.showPing) {
                        PerformanceMonitorState.showPing = it
                        AppSettings.setBoolean(context, "show_ping", it)
                        AppSettings.setBoolean(context, "timer_lost", it)
                        datagame_main.instance?.reloadInGameSettings()
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageFlagButton(
    drawableRes: Int,
    isSelected: Boolean,
    contentDescription: String,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = when {
        isLocked -> AppColors.Border.copy(alpha = 0.5f)
        isSelected -> AppColors.OnlineGreen
        else -> AppColors.Border
    }
    val borderWidth = if (isSelected && !isLocked) 2.dp else 1.dp
    val alpha = when {
        isLocked -> 0.3f
        isSelected -> 1f
        else -> 0.5f
    }

    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.PanelDark)
            .border(borderWidth, borderColor, RoundedCornerShape(4.dp))
            .then(
                if (!isLocked) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(3.dp))
                .alpha(alpha),
            contentScale = ContentScale.Crop
        )
        if (isLocked) {
            Icon(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_lock),
                contentDescription = "Locked",
                tint = AppColors.White.copy(alpha = 0.5f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun SettingsTabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = if (isSelected) AppColors.TabActive else AppColors.PanelDark
    val borderColor = if (isSelected) AppColors.BorderActive else AppColors.Border
    val textColor = AppColors.White
    
    Box(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(vertical = 7.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun CategoryTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = AppColors.TextDarkGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier.padding(top = 2.dp, bottom = 7.dp)
    )
}

@Composable
fun SettingRow(
    title: String,
    drawBottomBorder: Boolean = true,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                drawContent()
                if (drawBottomBorder) {
                    drawLine(
                        color = AppColors.White.copy(alpha = 0.05f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f
                    )
                }
            }
            .padding(vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = if (isLocked) AppColors.Border else AppColors.White,
                    fontSize = 14.sp
                )
                if (isLocked) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_lock),
                        contentDescription = "Locked",
                        tint = AppColors.Border,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun CustomSwitch(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit = {}) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 18.dp else 0.dp,
        animationSpec = tween(durationMillis = 200)
    )
    val bgColor by animateColorAsState(
        targetValue = if (!enabled) AppColors.PanelDark.copy(alpha = 0.5f) else if (checked) AppColors.OnlineGreen else AppColors.PanelDark,
        animationSpec = tween(durationMillis = 200)
    )
    val borderColor by animateColorAsState(
        targetValue = if (!enabled) AppColors.Border else if (checked) AppColors.OnlineGreen else AppColors.Border,
        animationSpec = tween(durationMillis = 200)
    )
    val thumbColor = if (!enabled) AppColors.White.copy(alpha = 0.3f) else AppColors.White

    Box(
        modifier = Modifier
            .width(38.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { onCheckedChange(!checked) }
                } else Modifier
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(thumbColor)
        )
    }
}

@Composable
private fun ColorPickerCircle(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { }
    )
}

@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    tooltipText: @Composable ((Float) -> Unit)? = null
) {
    val sliderWidth = 140.dp
    val thumbSize = 16.dp
    
    var isDragging by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .width(sliderWidth)
            .height(thumbSize)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        isDragging = true
                        val initialFraction = (down.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(initialFraction * 100f)
                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val anyPressed = event.changes.any { it.pressed }
                            if (!anyPressed) {
                                isDragging = false
                                break
                            }
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change != null) {
                                change.consume()
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                onValueChange(fraction * 100f)
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val fraction = (value / 100f).coerceIn(0f, 1f)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(AppColors.Panel)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(4.dp)
                .clip(CircleShape)
                .background(AppColors.Gray)
        )
        Box(
            modifier = Modifier
                .offset(x = (sliderWidth - thumbSize) * fraction)
                .size(thumbSize)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(AppColors.White)
            )
            
            val animatedAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isDragging) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                label = "tooltip_alpha"
            )
            
            if (animatedAlpha > 0f && tooltipText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-24).dp)
                        .wrapContentSize(unbounded = true)
                        .graphicsLayer(alpha = animatedAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    tooltipText(value)
                }
            }
        }
    }
}


@Composable
private fun SettingsDropdown(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var itemWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .onSizeChanged { size ->
                if (itemWidthPx != size.width) itemWidthPx = size.width
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.PanelDark)
                .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 7.dp, horizontal = 10.dp)
        ) {
            Text(
                text = selectedItem,
                color = AppColors.White,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Text(
                text = "▼",
                color = AppColors.TextDarkGray,
                fontSize = 9.sp,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        if (expanded && itemWidthPx > 0) {
            val widthDp = remember(itemWidthPx, density) { with(density) { itemWidthPx.toDp() } }
            val offsetPx = remember(density) { with(density) { 33.dp.roundToPx() } }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, offsetPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(
                    modifier = Modifier
                        .width(widthDp)
                        .heightIn(max = 175.dp)
                        .background(AppColors.DropdownBg.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                        .border(1.dp, AppColors.Border, RoundedCornerShape(6.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        items.forEach { item ->
                            val isSelected = selectedItem == item
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onItemSelected(item)
                                        expanded = false
                                    }
                                    .background(if (isSelected) AppColors.Panel else AppColors.Transparent)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = item,
                                    color = if (isSelected) AppColors.White else AppColors.TextGray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SettingsActionButton(
    text: String,
    isDanger: Boolean = false,
    enabled: Boolean = true,
    isLocked: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val effectiveEnabled = enabled && !isLocked
    val baseBgColor = if (isDanger) AppColors.DangerBg else AppColors.PanelDark
    val baseBorderColor = if (isDanger) AppColors.DangerBorder else AppColors.Border
    val bgColor = if (effectiveEnabled) baseBgColor else baseBgColor.copy(alpha = baseBgColor.alpha * 0.5f)
    val borderColor = baseBorderColor
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 31.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .then(
                if (effectiveEnabled) {
                    Modifier.clickable { onClick() }
                } else Modifier
            )
            .padding(vertical = 2.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (isLocked) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(R.drawable.ic_lock),
                    contentDescription = "Locked",
                    tint = AppColors.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            androidx.compose.material3.Text(
                text = text,
                color = if (effectiveEnabled) AppColors.White else AppColors.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 14.sp,
                softWrap = true
            )
        }
    }
}

fun copyVideoToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val outFile = java.io.File(context.filesDir, "live_background.mp4")
        if (outFile.exists()) {
            outFile.delete()
        }
        inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        outFile.absolutePath
    } catch (e: Exception) {
        null
    }
}

fun copyImageToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val outFile = java.io.File(context.filesDir, "static_background")
        if (outFile.exists()) {
            outFile.delete()
        }
        inputStream.use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        outFile.absolutePath
    } catch (e: Exception) {
        null
    }
}
