@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package skayrux.shootout2d

import android.content.Context
import java.io.File
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.DialogWindowProvider
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.decodeFromByteArray

private val ColorTabActive: Color get() = AppColors.TabActive
private val ColorTabInactive: Color get() = AppColors.PanelDark
private val ColorBorderActive: Color get() = AppColors.BorderActive
private val ColorBorder: Color get() = AppColors.Border
private val ColorDarkBg: Color get() = AppColors.InputContainer
private val ColorButtonBg: Color get() = AppColors.Panel
private val ColorTextGray: Color get() = AppColors.TextGray
private val ColorDivider: Color get() = AppColors.Overlay10
private val ColorEmptyBorder: Color get() = AppColors.Border
private val ColorEmptyText: Color get() = AppColors.TextDarkGray

fun sanitizeMapName(input: String): String = sanitizeSafeText(input)

class BubbleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val arrowWidth = with(density) { 5.dp.toPx() }
        val arrowHeight = with(density) { 7.dp.toPx() }
        val path = Path().apply {
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
        return Outline.Generic(path)
    }
}

fun getTextureByName(name: String): TextureInfo? {
    return TextureDatabase.getTexture(name)
}

@Composable
fun TexturePreview(
    textureName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val textureBitmap = remember { ResourceCache.getTextures(context) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.SurfaceDim),
        contentAlignment = Alignment.Center
    ) {
        val texture = remember(textureName) {
            getTextureByName(textureName)
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (texture != null) {
                if (textureBitmap != null) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawImage(
                            image = textureBitmap,
                            srcOffset = IntOffset(texture.x, texture.y),
                            srcSize = IntSize(texture.width, texture.height),
                            dstOffset = IntOffset(0, 0),
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val previewColor = when {
                            texture.name.startsWith("flora") || texture.name.startsWith("trav") -> AppColors.Primary
                            texture.name.startsWith("t") || texture.name.startsWith("ground") -> AppColors.BrownDark
                            texture.name.startsWith("p") -> AppColors.BlueGray
                            texture.name.startsWith("snow") -> AppColors.GrayE0
                            else -> AppColors.BlueGrayLight
                        }
                        drawRect(color = previewColor)
                    }
                }
            } else {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_ne_naydeno_0b70),
                    color = AppColors.DangerRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

object MapEditorState {
    var activeTab: String = "new"
    var mapName: String = ""
    var width: Int = 10
    var height: Int = 8
    var activeTerrainIndex: Int = 0
    var selectedUid: String? = null
}

@Composable
fun mapeditor_menu(
    accountId: String,
    onStartSession: (String, Int, Int, String) -> Unit,
    onLoadSession: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(MapEditorState.activeTab) }
    var mapName by remember { mutableStateOf(MapEditorState.mapName) }
    var width by remember { mutableIntStateOf(MapEditorState.width) }
    var height by remember { mutableIntStateOf(MapEditorState.height) }

    var pendingLaunchAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(pendingLaunchAction) {
        val action = pendingLaunchAction
        if (action != null) {
            action.invoke()
        }
    }

    LaunchedEffect(activeTab) {
        MapEditorState.activeTab = activeTab
    }
    LaunchedEffect(mapName) {
        MapEditorState.mapName = mapName
        AppSettings.setString(context, "map_editor_name", mapName)
    }
    LaunchedEffect(width) {
        MapEditorState.width = width
        AppSettings.setInt(context, "map_editor_width", width)
    }
    LaunchedEffect(height) {
        MapEditorState.height = height
        AppSettings.setInt(context, "map_editor_height", height)
    }

    var maps by remember { mutableStateOf<List<MapMetadata>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    val refreshMaps: () -> Unit = {
        coroutineScope.launch(Dispatchers.IO) {
            val list = MapSavior.listMyMaps(context, accountId)
                .sortedByDescending { it.lastModified }
            withContext(Dispatchers.Main) {
                maps = list
            }
        }
    }

    LaunchedEffect(accountId) {
        refreshMaps()
    }

    // cooldown создания карты
    var isCreateCooldown by remember { mutableStateOf(false) }

    LaunchedEffect(isCreateCooldown) {
        if (isCreateCooldown) {
            delay(2500)
            isCreateCooldown = false
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                pendingLaunchAction = null
                isCreateCooldown = false
                refreshMaps()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val defaultTerrains = remember {
        listOf(
            "t01", "t1", "t04", "t7", "t10", "t19", "t33", "t67",
            "p01", "p02", "p10", "flora_79", "p14", "p15", "p3", "p11",
            "streets24", "streets44", "042", "BS02083", "e_004", "e_007", "e_010",
            "snow7", "snow10", "snow12", "snow14", "granite_21",
            "ground_12", "ground_16", "ground_65", "ground_78",
            "k5", "k12", "pk02_plates01a_S_PB", "grnd8", "grnd9",
            "dertex128", "drwtx128", "drtx128", "block184_128", "romb128",
            "met23128", "shahm128", "shahmw128", "block283_128", "romb2128",
            "trav128", "travk128", "trav4128", "plit128"
        )
    }
    var activeTerrainIndex by remember { mutableIntStateOf(MapEditorState.activeTerrainIndex) }
    LaunchedEffect(activeTerrainIndex) {
        MapEditorState.activeTerrainIndex = activeTerrainIndex
    }
    val defaultTerrain = defaultTerrains[activeTerrainIndex]

    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            toastMessage = null
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
                    .padding(bottom = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.new_map), activeTab == "new") { activeTab = "new" }
                TabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.my_maps), activeTab == "my") { activeTab = "my" }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Overlay10)
            )
            
            Spacer(Modifier.height(10.dp))

            if (activeTab == "new") {
                NewMapPanel(
                    context = context,
                    mapName = mapName,
                    onNameChange = { mapName = it },
                    width = width,
                    onWidthChange = { width = it },
                    height = height,
                    onHeightChange = { height = it },
                    activeTerrainIndex = activeTerrainIndex,
                    defaultTerrains = defaultTerrains,
                    onTerrainIndexChange = { activeTerrainIndex = it },
                    onCreateMap = {
                        if (pendingLaunchAction == null && !isCreateCooldown) {
                            isCreateCooldown = true
                            val finalWidth = width
                            val finalHeight = height
                            val finalMapName = sanitizeMapName(mapName).trim().ifBlank { "default_map" }
                            pendingLaunchAction = {
                                onStartSession(finalMapName, finalWidth, finalHeight, defaultTerrain)
                            }
                        }
                    },
                    isCreating = isCreateCooldown || pendingLaunchAction != null
                )
            } else {
                MyMapsPanel(
                    accountId = accountId,
                    onLoad = { uid ->
                        if (pendingLaunchAction == null) {
                            pendingLaunchAction = {
                                onLoadSession?.invoke(uid)
                            }
                        }
                    },
                    maps = maps,
                    refreshMaps = refreshMaps,
                    onShowToast = { toastMessage = it }
                )
            }
        }

        UnifiedToast(
            message = toastMessage ?: "",
            visible = toastMessage != null,
            modifier = Modifier.align(Alignment.TopCenter)
        )


    }
}

@Composable
fun TabButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    val bgColor = if (isActive) AppColors.TabActive else AppColors.PanelDark
    val borderColor = if (isActive) AppColors.BorderActive else AppColors.Border
    val textColor = AppColors.White

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
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun NewMapPanel(
    context: android.content.Context,
    mapName: String,
    onNameChange: (String) -> Unit,
    width: Int,
    onWidthChange: (Int) -> Unit,
    height: Int,
    onHeightChange: (Int) -> Unit,
    activeTerrainIndex: Int,
    defaultTerrains: List<String>,
    onTerrainIndexChange: (Int) -> Unit,
    onCreateMap: () -> Unit,
    isCreating: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // параметры карты слева
        Column(
            modifier = Modifier.weight(0.7f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map_name), color = ColorTextGray, fontSize = 15.sp)
                Box(modifier = Modifier.fillMaxWidth(0.6f)) {
                    UnifiedTextField(
                        value = mapName,
                        onValueChange = { input ->
                            val filtered = sanitizeMapName(input)
                            if (filtered.length <= 20) {
                                onNameChange(filtered)
                            }
                        },
                        inputFilter = ::sanitizeMapName,
                        placeholder = "",
                        maxLength = 20,
                        heightDp = 36,
                        backgroundColor = ColorDarkBg,
                        cornerRadiusDp = 4,
                        enabled = !isCreating
                    )
                }
            }

            SizeControl(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.width), width, { onWidthChange(it.coerceIn(10, 100)) }, 10, 100)
            SizeControl(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.height), height, { onHeightChange(it.coerceIn(8, 100)) }, 8, 100)
        }

        // ландшафт и кнопка создания карты справа
        Column(
            modifier = Modifier.weight(0.25f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(ColorDarkBg, RoundedCornerShape(4.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                TexturePreview(textureName = defaultTerrains[activeTerrainIndex], modifier = Modifier.fillMaxSize())
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                WideCtrlButton("<<") {
                    val prev = if (activeTerrainIndex > 0) activeTerrainIndex - 1 else defaultTerrains.size - 1
                    onTerrainIndexChange(prev)
                }
                WideCtrlButton(">>") {
                    val next = if (activeTerrainIndex < defaultTerrains.size - 1) activeTerrainIndex + 1 else 0
                    onTerrainIndexChange(next)
                }
            }
            
            Spacer(Modifier.weight(1f))
            
            // кнопка создания карты со статичной высотой
            val isEnabled = mapName.isNotBlank() && !isCreating
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .menuActionButtonStyle(enabled = isEnabled, shape = RoundedCornerShape(4.dp))
                    .clickable(enabled = isEnabled) {
                        onCreateMap()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isCreating) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AppColors.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.create_map),
                        color = if (isEnabled) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// контрол изменения размера
@Composable
fun SizeControl(label: String, value: Int, onValueChange: (Int) -> Unit, minVal: Int, maxVal: Int = 100) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
        Text(label, color = ColorTextGray, fontSize = 15.sp, modifier = Modifier.width(70.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            CtrlButton("-") { if (value > minVal) onValueChange((value - 1).coerceAtLeast(minVal)) }
            Box(
                modifier = Modifier
                    .size(60.dp, 35.dp)
                    .background(ColorDarkBg, RoundedCornerShape(4.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(value.toString(), color = AppColors.White, fontSize = 16.sp)
            }
            CtrlButton("+") { if (value < maxVal) onValueChange((value + 1).coerceAtMost(maxVal)) }
        }
    }
}

// кнопка плюс минус
@Composable
fun CtrlButton(text: String, onClick: () -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(35.dp)
            .width(50.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(AppColors.PanelDark)
            .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) {}
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val press = PressInteraction.Press(down.position)
                    coroutineScope.launch {
                        interactionSource.emit(press)
                    }
                    val repeatJob = coroutineScope.launch {
                        currentOnClick()
                        delay(160)
                        while (true) {
                            currentOnClick()
                            delay(55)
                        }
                    }
                    val up = waitForUpOrCancellation()
                    repeatJob.cancel()
                    coroutineScope.launch {
                        if (up != null) {
                            interactionSource.emit(PressInteraction.Release(press))
                        } else {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        }
                    }
                }
            }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (text == "+") {
            Text(
                text = "+",
                color = AppColors.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        } else {
            Text(
                text = "—",
                color = AppColors.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(y = (-2).dp)
            )
        }
    }
}

// кнопка переключения ландшафта
@Composable
fun WideCtrlButton(text: String, onClick: () -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(ColorButtonBg)
            .border(1.dp, ColorBorder, RoundedCornerShape(4.dp))
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) {}
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val press = PressInteraction.Press(down.position)
                    coroutineScope.launch {
                        interactionSource.emit(press)
                    }
                    val repeatJob = coroutineScope.launch {
                        currentOnClick()
                        delay(350)
                        while (true) {
                            currentOnClick()
                            delay(180)
                        }
                    }
                    val up = waitForUpOrCancellation()
                    repeatJob.cancel()
                    coroutineScope.launch {
                        if (up != null) {
                            interactionSource.emit(PressInteraction.Release(press))
                        } else {
                            interactionSource.emit(PressInteraction.Cancel(press))
                        }
                    }
                }
            }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.White,
            fontSize = 14.sp
        )
    }
}

data class MapInfo(val name: String, val sizeText: String, val date: String)

@Composable
fun MyMapsPanel(
    accountId: String,
    onLoad: ((String) -> Unit)?,
    maps: List<MapMetadata>,
    refreshMaps: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val initialSelectedUid = remember { MapEditorState.selectedUid ?: AppSettings.getString(context, "selected_map_uid", "") }
    var selectedUid by remember { mutableStateOf<String?>(if (initialSelectedUid.isEmpty()) null else initialSelectedUid) }
    var mapToDelete by remember { mutableStateOf<String?>(null) }
    var serverMapToDelete by remember { mutableStateOf<String?>(null) }
    var serverMapToUpload by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var isUploadInProgress by remember { mutableStateOf(false) }
    var isAskingUpload by remember { mutableStateOf(false) }
    var cooldownTickNowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val uploadCooldownSeconds = sys_menu_socket_repository.getMapUploadCooldownRemainingSec(cooldownTickNowMs)
    var serverDeleteTimerSeconds by remember { mutableIntStateOf(0) }
    var localDeleteTimerSeconds by remember { mutableIntStateOf(0) }
    var isRenameDialogVisible by remember { mutableStateOf(false) }

    // таймер кулдауна обновления/загрузки карт который сохраняется при переключении окон
    LaunchedEffect(sys_menu_socket_repository.mapUploadCooldownUntilMs) {
        while (sys_menu_socket_repository.getMapUploadCooldownRemainingSec(System.currentTimeMillis()) > 0) {
            cooldownTickNowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
        cooldownTickNowMs = System.currentTimeMillis()
    }

    LaunchedEffect(serverMapToDelete) {
        if (serverMapToDelete != null) {
            serverDeleteTimerSeconds = 2
            while (serverDeleteTimerSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                serverDeleteTimerSeconds--
            }
        } else {
            serverDeleteTimerSeconds = 0
        }
    }

    LaunchedEffect(mapToDelete) {
        if (mapToDelete != null) {
            localDeleteTimerSeconds = 2
            while (localDeleteTimerSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                localDeleteTimerSeconds--
            }
        } else {
            localDeleteTimerSeconds = 0
        }
    }
    var renameInputName by remember { mutableStateOf("") }
    var isMapOnline by remember { mutableStateOf(false) }
    var isCheckingOnline by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedUid) {
        MapEditorState.selectedUid = selectedUid
        AppSettings.setString(context, "selected_map_uid", selectedUid ?: "")
        if (selectedUid != null) {
            isCheckingOnline = true
            val res = sys_menu_socket_repository.checkMapOnline(selectedUid!!)
            val isOnline = res.startsWith("MAP_ONLINE_OK")
            isMapOnline = isOnline
            MapSavior.setMapPublished(context, selectedUid!!, isOnline)
            isCheckingOnline = false
        } else {
            isMapOnline = false
        }
    }

    if (mapToDelete != null) {
        val deleteMapMetadata = maps.find { it.uid == mapToDelete }
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.removal),
            content = { Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete_map_confirm, deleteMapMetadata?.name ?: mapToDelete ?: ""), color = AppColors.TextMuted, fontSize = 14.sp) },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
            cancelText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.cancel),
            confirmEnabled = localDeleteTimerSeconds == 0,
            confirmContent = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
                        color = if (localDeleteTimerSeconds == 0) Color.White else Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (localDeleteTimerSeconds > 0) {
                        Text(
                            text = "($localDeleteTimerSeconds)",
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
            onConfirm = {
                if (mapToDelete != null) {
                    MapSavior.deleteMap(context, mapToDelete!!)
                    if (selectedUid == mapToDelete) selectedUid = null
                    mapToDelete = null
                    refreshMaps()
                }
            },
            onDismiss = { mapToDelete = null }
        )
    }

    if (serverMapToUpload != null) {
        val (uploadUid, wasOnline) = serverMapToUpload!!
        UnifiedDialog(
            title = if (wasOnline) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_obnovlenie_karty_e725) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.upload_map_to_server),
            content = {
                Text(
                    text = if (wasOnline) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_deystvitelno_hotite_obnovi_c5ed) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_primechanie_v_den_mozhno_zagruzh_21a7, sys_menu_socket_repository.formattedDailyMapUploadLimit),
                    color = AppColors.TextMuted,
                    fontSize = 14.sp
                )
            },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.yes),
            cancelText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.no),
            confirmEnabled = !isUploadInProgress && uploadCooldownSeconds == 0,
            onConfirm = {
                if (isUploadInProgress || uploadCooldownSeconds > 0) return@UnifiedDialog
                val targetUid = uploadUid
                val isUpdating = wasOnline
                serverMapToUpload = null
                isUploadInProgress = true
                coroutineScope.launch(Dispatchers.IO) {
                    val base64 = MapSavior.packMapData(context, targetUid)
                    if (base64 != null) {
                        val mapMeta = maps.find { it.uid == targetUid }
                        val name = mapMeta?.name ?: "Unknown"
                        val width = mapMeta?.width ?: 128
                        val height = mapMeta?.height ?: 128
                        val res = sys_menu_socket_repository.uploadMap(targetUid, name, width, height, base64, isUpdating)
                        withContext(Dispatchers.Main) {
                            isUploadInProgress = false
                            if (res.startsWith("UPLOAD_MAP_OK")) {
                                if (selectedUid == targetUid) {
                                    isMapOnline = true
                                }
                                val cd = res.substringAfter("|", "0").toIntOrNull() ?: 0
                                if (cd > 0) {
                                    sys_menu_socket_repository.setMapUploadCooldown(cd)
                                    cooldownTickNowMs = System.currentTimeMillis()
                                }
                                MapSavior.setMapPublished(context, targetUid, true)
                                onShowToast(if (isUpdating) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_karta_uspeshno_obnovlena_na_ser_d504) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_karta_uspeshno_zagruzhena_na_ser_85ed))
                            } else {
                                if (res.startsWith("UPLOAD_MAP_ERROR:RATE_LIMIT")) {
                                    val sec = res.substringAfter("|", "60").toIntOrNull()?.coerceIn(1, 60) ?: 60
                                    sys_menu_socket_repository.setMapUploadCooldown(sec)
                                    cooldownTickNowMs = System.currentTimeMillis()
                                }
                                onShowToast(sys_menu_socket_repository.translateError(res))
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isUploadInProgress = false
                            onShowToast(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_fayl_karty_ne_nayden_9ec3))
                        }
                    }
                }
            },
            onDismiss = { serverMapToUpload = null }
        )
    }

    if (serverMapToDelete != null) {
        val deleteMapMetadata = maps.find { it.uid == serverMapToDelete }
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_udalenie_s_servera_794e),
            content = { Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete_map_server_confirm, deleteMapMetadata?.name ?: serverMapToDelete ?: ""), color = AppColors.TextMuted, fontSize = 14.sp) },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
            cancelText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.cancel),
            confirmEnabled = serverDeleteTimerSeconds == 0,
            confirmContent = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
                        color = if (serverDeleteTimerSeconds == 0) Color.White else Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    if (serverDeleteTimerSeconds > 0) {
                        Text(
                            text = "($serverDeleteTimerSeconds)",
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
            onConfirm = {
                val uid = serverMapToDelete
                serverMapToDelete = null
                if (uid != null) {
                    coroutineScope.launch(Dispatchers.IO) {
                        val res = sys_menu_socket_repository.deleteMap(uid)
                        withContext(Dispatchers.Main) {
                            if (res == "DELETE_MAP_OK") {
                                if (selectedUid == uid) {
                                    isMapOnline = false
                                }
                                MapSavior.setMapPublished(context, uid, false)
                            }
                            onShowToast(sys_menu_socket_repository.translateError(res))
                        }
                    }
                }
            },
            onDismiss = { serverMapToDelete = null }
        )
    }

    var showOwnershipInfo by remember { mutableStateOf<String?>(null) }

    // окно смены имени карты
    if (isRenameDialogVisible && selectedUid != null) {
        val currentMapName = maps.find { it.uid == selectedUid }?.name
            ?: MapSavior.listAllMaps(context).find { it.uid == selectedUid }?.name
            ?: ""
        var dialogMapName by remember(isRenameDialogVisible, selectedUid) {
            mutableStateOf(renameInputName.ifBlank { currentMapName })
        }
        LaunchedEffect(selectedUid, isRenameDialogVisible) {
            val currentName = maps.find { it.uid == selectedUid }?.name
                ?: MapSavior.listAllMaps(context).find { it.uid == selectedUid }?.name
                ?: ""
            dialogMapName = currentName
            renameInputName = currentName
        }
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vvedite_novoe_imya_dlya_karty_0ecf),
            confirmEnabled = dialogMapName.isNotBlank(),
            content = {
                UnifiedTextField(
                    value = dialogMapName,
                    onValueChange = { input ->
                        val filtered = sanitizeMapName(input)
                        if (filtered.length <= 20) {
                            dialogMapName = filtered
                            renameInputName = filtered
                        }
                    },
                    inputFilter = ::sanitizeMapName,
                    placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_imya_karty_dec4),
                    maxLength = 20
                )
            },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sohranit_74ea),
            cancelText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.cancel),
            onConfirm = {
                val safeNewName = dialogMapName.ifBlank { "default_map" }
                if (selectedUid != null) {
                    MapSavior.renameMap(context, selectedUid!!, safeNewName)
                    refreshMaps()
                }
                isRenameDialogVisible = false
            },
            onDismiss = { isRenameDialogVisible = false }
        )
    }

    val ownMaps = remember(maps) {
        maps
    }

    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .border(1.dp, ColorEmptyBorder, RoundedCornerShape(4.dp))
        ) {
            if (ownMaps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.no_custom_maps), color = ColorEmptyText, fontSize = 18.sp)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(4.dp)
                ) {
                    items(count = ownMaps.size, key = { index -> ownMaps[index].uid }) { index ->
                        val mMetadata = ownMaps[index]
                        val mUid = mMetadata.uid
                        val mName = mMetadata.name
                        val isSel = selectedUid == mUid

                        val currentSrvId = accountId.toIntOrNull() ?: 0
                        val isMyMapItem = MapSavior.isUserOwnMap(mMetadata, accountId)
                        val differentAccount = !isMyMapItem && (mMetadata.creatorSrvId != 0 || (mMetadata.ownerId.isNotBlank() && mMetadata.ownerId != "0"))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { 
                                    selectedUid = mUid 
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSel) ColorTabActive else AppColors.Transparent)
                                    .border(1.dp, if (isSel) ColorBorderActive else Color.Transparent)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mName,
                                        color = AppColors.White,
                                        fontSize = 15.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (differentAccount) {
                                        Box(
                                            modifier = Modifier
                                                .padding(start = 8.dp)
                                                .size(17.dp)
                                                .border(1.dp, AppColors.Error, CircleShape)
                                                .clickable { 
                                                    showOwnershipInfo = if (showOwnershipInfo == mUid) null else mUid
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("!", color = AppColors.Error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            if (showOwnershipInfo == mUid) {
                                                val density = androidx.compose.ui.platform.LocalDensity.current
                                                val iconWidthPx = with(density) { 17.dp.toPx() }.toInt()
                                                val popupHeightPx = with(density) { 50.dp.toPx() }.toInt()
                                                Popup(
                                                    onDismissRequest = { showOwnershipInfo = null },
                                                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
                                                    offset = IntOffset(
                                                        iconWidthPx + with(density) { 3.dp.toPx() }.toInt(),
                                                        -popupHeightPx / 2 + iconWidthPx / 2 + with(density) { 2.dp.toPx() }.toInt()
                                                    )
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(200.dp)
                                                            .background(AppColors.PanelAlpha, BubbleShape())
                                                            .border(1.dp, AppColors.Border, BubbleShape())
                                                            .padding(start = 16.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
                                                    ) {
                                                        val ownerDisplay = if (mMetadata.creatorSrvId != 0) mMetadata.creatorSrvId.toString() else mMetadata.ownerId
                                                        Text(
                                                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map_created_on_other_account, ownerDisplay),
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Text(
                                    text = "${mMetadata.width}x${mMetadata.height}",
                                    color = if (isSel) AppColors.White.copy(alpha = 0.85f) else AppColors.TextMuted,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(0.6f),
                                    textAlign = TextAlign.Center
                                )
                                
                                val dateStr = remember(mMetadata.lastModified, mMetadata.timestamp) {
                                    try {
                                        val millis = mMetadata.timestamp.toLongOrNull()?.takeIf { it > 1000000000000L } ?: mMetadata.lastModified
                                        val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).apply {
                                            timeZone = TimeZone.getTimeZone("Europe/Moscow")
                                        }
                                        sdf.format(Date(millis))
                                    } catch (e: Exception) { "" }
                                }
                                
                                Text(
                                    text = dateStr,
                                    color = if (isSel) AppColors.White.copy(alpha = 0.85f) else AppColors.TextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(0.7f),
                                    textAlign = TextAlign.End
                                )
                            }
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp)
                                    .height(1.dp)
                                    .background(AppColors.Border)
                            )
                        }
                    }
                }
            }
        }
        
        Column(modifier = Modifier.width(150.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val hasMap = selectedUid != null
            val selectedMapMeta = ownMaps.find { it.uid == selectedUid }
            
            val currentSrvId = accountId.toIntOrNull() ?: 0
            
            val isMyMap = hasMap && selectedMapMeta != null && MapSavior.isUserOwnMap(selectedMapMeta, accountId)
            val hasForeignOwner = hasMap && selectedMapMeta != null && !isMyMap
            
            ActionButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.load_map), enabled = hasMap, onClick = {
                if (hasMap && onLoad != null) onLoad(selectedUid!!)
            })
            
            val uploadText = if (isMapOnline) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_obnovit_kartu_3f37) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.upload_to_server)
            ActionButton(
                text = uploadText,
                enabled = hasMap && isMyMap && sys_menu_socket_repository.isServerOnline && !isCheckingOnline && !isUploadInProgress && !isAskingUpload && uploadCooldownSeconds == 0,
                onClick = {
                    val targetMapId = selectedUid
                    val targetWasOnline = isMapOnline
                    if (hasMap && isMyMap && targetMapId != null && sys_menu_socket_repository.isServerOnline && !isCheckingOnline && !isUploadInProgress && !isAskingUpload && uploadCooldownSeconds == 0) {
                        // проверка очков перед отправкой запроса на сервер
                        val canBypassScore = sys_menu_socket_repository.currentPlayerAdminStatus == 1
                        if (!canBypassScore && sys_menu_socket_repository.currentPlayerScore < sys_menu_socket_repository.minScoreUploadMap) {
                            onShowToast(
                                skayrux.shootout2d.Loc.tr(
                                    skayrux.shootout2d.R.string.str_nuzhno_minimum_50_000_ochkov_cc77,
                                    sys_menu_socket_repository.formattedMinScoreUploadMap
                                )
                            )
                            return@ActionButton
                        }
                        isAskingUpload = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val askRes = sys_menu_socket_repository.askUploadMap(targetMapId, targetWasOnline)
                            withContext(Dispatchers.Main) {
                                isAskingUpload = false
                                if (askRes == "ASK_UPLOAD_MAP_OK") {
                                    serverMapToUpload = Pair(targetMapId, targetWasOnline)
                                } else {
                                    if (askRes.startsWith("UPLOAD_MAP_ERROR:RATE_LIMIT")) {
                                        val sec = askRes.substringAfter("|", "60").toIntOrNull()?.coerceIn(1, 60) ?: 60
                                        sys_menu_socket_repository.setMapUploadCooldown(sec)
                                        cooldownTickNowMs = System.currentTimeMillis()
                                    }
                                    onShowToast(sys_menu_socket_repository.translateError(askRes))
                                }
                            }
                        }
                    }
                }
            )
            ActionButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.change_name), enabled = hasMap && isMyMap, onClick = {
                if (hasMap && isMyMap) {
                    val currentName = maps.find { it.uid == selectedUid }?.name
                        ?: MapSavior.listAllMaps(context).find { it.uid == selectedUid }?.name
                        ?: ""
                    renameInputName = currentName
                    isRenameDialogVisible = true
                }
            })
            ActionButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete_map), enabled = hasMap, onClick = {
                if (hasMap) {
                    mapToDelete = selectedUid
                }
            })
            ActionButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete_from_server), enabled = hasMap && isMyMap && isMapOnline && sys_menu_socket_repository.isServerOnline && !isCheckingOnline, onClick = {
                if (hasMap && isMyMap && isMapOnline && sys_menu_socket_repository.isServerOnline && !isCheckingOnline) {
                    serverMapToDelete = selectedUid
                }
            })
        }
    }
}



@Composable
fun ActionButton(text: String, enabled: Boolean = true, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .menuActionButtonStyle(enabled = enabled, shape = RoundedCornerShape(4.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp, horizontal = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

data class MapMetadata(
    val uid: String = "",
    val name: String = "",
    val author: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val lastModified: Long = 0L,
    val isDownloaded: Boolean = false,
    val ownerId: String = "",
    val creatorRid: Int = 0,
    val creatorSrvId: Int = 0,
    val timestamp: String = ""
)

object MapSavior {
    fun isSafeUid(uid: String): Boolean {
        return uid.isNotBlank() && uid.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    fun packMapData(context: Context, uid: String): String? {
        if (!isSafeUid(uid)) return null
        val mapFile = File(context.filesDir, "$uid.map")
        val obFile = File(context.filesDir, "$uid.ob")
        if (!mapFile.exists()) return null
        val mapBytes = mapFile.readBytes()
        val obBytes = if (obFile.exists()) obFile.readBytes() else ByteArray(0)
        val bos = java.io.ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(bos)
        dos.writeInt(mapBytes.count())
        dos.write(mapBytes)
        dos.writeInt(obBytes.count())
        dos.write(obBytes)
        dos.close()
        return android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
    }

    fun addToDownloadedMapsList(context: Context, uid: String) {
        if (!isSafeUid(uid)) return
        try {
            val file = File(context.filesDir, "downloaded_maps")
            val lines = if (file.exists()) file.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet() else mutableSetOf()
            if (!lines.contains(uid)) {
                lines.add(uid)
                file.writeText(lines.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getDownloadedMapsUids(context: Context): Set<String> {
        return try {
            val file = File(context.filesDir, "downloaded_maps")
            if (file.exists()) {
                file.readLines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    fun markMapAsDownloaded(context: Context, uid: String) {
        if (!isSafeUid(uid)) return
        AppSettings.setBoolean(context, "map_downloaded_$uid", true)
        AppSettings.setBoolean(context, "map_published_$uid", true)
        addToDownloadedMapsList(context, uid)
        addToSavedMapsList(context, uid)
    }

    fun markMapAsLocal(context: Context, uid: String) {
        if (!isSafeUid(uid)) return
        AppSettings.setBoolean(context, "map_downloaded_$uid", false)
        AppSettings.setBoolean(context, "map_published_$uid", false)
        try {
            val file = File(context.filesDir, "downloaded_maps")
            if (file.exists()) {
                val lines = file.readLines().filter { it.trim().isNotEmpty() && it.trim() != uid }
                file.writeText(lines.joinToString("\n") + if (lines.isNotEmpty()) "\n" else "")
            }
        } catch (e: Exception) {}
        addToSavedMapsList(context, uid)
    }

    fun isMapDownloaded(context: Context, uid: String): Boolean {
        if (!isSafeUid(uid)) return false
        if (AppSettings.getBoolean(context, "map_downloaded_$uid", false)) return true
        if (getDownloadedMapsUids(context).contains(uid)) return true
        return false
    }

    fun isMapPublished(context: Context, uid: String): Boolean {
        if (!isSafeUid(uid)) return false
        if (isMapDownloaded(context, uid)) return true
        return AppSettings.getBoolean(context, "map_published_$uid", false)
    }

    fun setMapPublished(context: Context, uid: String, published: Boolean) {
        if (!isSafeUid(uid)) return
        AppSettings.setBoolean(context, "map_published_$uid", published)
    }

    fun addToSavedMapsList(context: Context, uid: String) {
        if (!isSafeUid(uid)) return
        try {
            val file = File(context.filesDir, "savedmaps")
            val lines = if (file.exists()) file.readLines().filter { it.isNotBlank() }.toMutableList() else mutableListOf()
            if (!lines.contains(uid)) {
                lines.add(uid)
                file.writeText(lines.joinToString("\n") + "\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun unpackMapData(context: Context, uid: String, base64Data: String): Boolean {
        if (!isSafeUid(uid)) return false
        try {
            val bytes = android.util.Base64.decode(base64Data, android.util.Base64.NO_WRAP)
            if (bytes == null || bytes.isEmpty()) return false
            var mapBytes: ByteArray? = null
            var obBytes: ByteArray? = null

            if (bytes.size >= 8) {
                try {
                    val bis = java.io.ByteArrayInputStream(bytes)
                    val dis = java.io.DataInputStream(bis)
                    val mapSize = dis.readInt()
                    if (mapSize > 0 && mapSize <= bytes.size - 8) {
                        val mBytes = ByteArray(mapSize)
                        dis.readFully(mBytes)
                        val obSize = dis.readInt()
                        if (obSize >= 0 && 8 + mapSize + obSize == bytes.size) {
                            val oBytes = ByteArray(obSize)
                            dis.readFully(oBytes)
                            mapBytes = mBytes
                            obBytes = oBytes
                        }
                    }
                    dis.close()
                } catch (e: Exception) {
                    mapBytes = null
                    obBytes = null
                }
            }

            if (mapBytes == null) {
                mapBytes = bytes
                obBytes = ByteArray(0)
            }

            val mapFile = File(context.filesDir, "$uid.map")
            mapFile.writeBytes(mapBytes)
            val obFile = File(context.filesDir, "$uid.ob")
            val obData = if (obBytes != null && obBytes.isNotEmpty()) {
                obBytes
            } else {
                val bos = java.io.ByteArrayOutputStream()
                val dos = java.io.DataOutputStream(bos)
                dos.writeByte(1)
                dos.writeUTF(System.currentTimeMillis().toString())
                dos.close()
                bos.toByteArray()
            }
            obFile.writeBytes(obData)

            val onlineDir = File(context.filesDir, "from_online_map")
            if (!onlineDir.exists()) onlineDir.mkdirs()
            File(onlineDir, "$uid.map").writeBytes(mapBytes)
            File(onlineDir, "$uid.ob").writeBytes(obData)

            markMapAsDownloaded(context, uid)
            addToSavedMapsList(context, uid)
            try {
                val dis = java.io.DataInputStream(java.io.ByteArrayInputStream(mapBytes))
                dis.readByte()
                dis.readUTF()
                val internalUid = dis.readUTF()
                if (internalUid.isNotBlank() && internalUid != uid) {
                    File(context.filesDir, "$internalUid.map").writeBytes(mapBytes)
                    File(context.filesDir, "$internalUid.ob").writeBytes(obData)
                    File(onlineDir, "$internalUid.map").writeBytes(mapBytes)
                    File(onlineDir, "$internalUid.ob").writeBytes(obData)
                    markMapAsDownloaded(context, internalUid)
                    addToSavedMapsList(context, internalUid)
                }
                dis.close()
            } catch (e: Exception) {}
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun readMapMetadataFromFile(context: Context, file: File, accountId: String? = null): MapMetadata? {
        if (!file.exists() || !file.isFile) return null
        if (file.name.endsWith(".map")) {
            val baseUid = file.nameWithoutExtension
            var mapName = baseUid
            var mapUid = baseUid
            var srvId = 0
            var rId = 0
            var mapW = 20
            var mapH = 20
            var timestamp = ""
            var lastMod = file.lastModified()
            try {
                java.io.DataInputStream(java.io.FileInputStream(file)).use { dais ->
                    val ver = dais.readByte()
                    val n = try { dais.readUTF() } catch (e: Exception) { "" }
                    val u = try { dais.readUTF() } catch (e: Exception) { "" }
                    if (n.isNotBlank()) mapName = n
                    if (u.isNotBlank()) mapUid = u
                    try { srvId = dais.readInt() } catch (e: Exception) {}
                    try { rId = dais.readInt() } catch (e: Exception) {}
                    try {
                        val w = dais.readShort().toInt()
                        if (w in 5..500) mapW = w
                    } catch (e: Exception) {}
                    try {
                        val h = dais.readShort().toInt()
                        if (h in 5..500) mapH = h
                    } catch (e: Exception) {}
                    try {
                        timestamp = dais.readUTF()
                        val tsLong = timestamp.toLongOrNull()
                        if (tsLong != null && tsLong > 1000000000000L) {
                            lastMod = tsLong
                        }
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
            }

            val isDl = isMapDownloaded(context, mapUid) ||
                       isMapDownloaded(context, baseUid) ||
                       file.parentFile?.name == "from_online_map"

            return MapMetadata(
                uid = if (baseUid.isNotBlank()) baseUid else mapUid,
                name = mapName,
                author = "",
                width = mapW,
                height = mapH,
                lastModified = lastMod,
                isDownloaded = isDl,
                ownerId = if (srvId != 0) srvId.toString() else "",
                creatorRid = rId,
                creatorSrvId = srvId,
                timestamp = timestamp
            )
        }
        return null
    }

    fun isUserOwnMap(meta: MapMetadata, accountId: String? = null): Boolean {
        val effAccountId = if (!accountId.isNullOrBlank()) accountId else sys_menu_socket_repository.currentPlayerId
        val currentSrvId = effAccountId.toIntOrNull() ?: 0
        if (currentSrvId != 0 && meta.creatorSrvId != 0) {
            return meta.creatorSrvId == currentSrvId
        }
        if (effAccountId.isNotBlank() && meta.ownerId.isNotBlank() && meta.ownerId != "0") {
            return meta.ownerId == effAccountId
        }
        if (meta.creatorSrvId != 0) {
            return false
        }
        if (meta.ownerId.isNotBlank() && meta.ownerId != "0") {
            return false
        }
        if (meta.isDownloaded) {
            return false
        }
        return true
    }

    private fun getAllMapFiles(context: Context): List<File> {
        val files = mutableListOf<File>()
        val baseDir = context.filesDir ?: return files
        val dirsToScan = listOf(
            baseDir,
            File(baseDir, "from_my_map"),
            File(baseDir, "from_online_map"),
            File(baseDir, "maps")
        )
        for (dir in dirsToScan) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".map")) {
                        files.add(file)
                    }
                }
            }
        }
        return files
    }

    fun listAllMaps(context: Context, accountId: String? = null): List<MapMetadata> {
        val mapFiles = getAllMapFiles(context)
        val result = mutableListOf<MapMetadata>()
        val seenUids = mutableSetOf<String>()
        for (file in mapFiles) {
            val meta = readMapMetadataFromFile(context, file, accountId) ?: continue
            if (meta.uid.isNotEmpty() && !seenUids.contains(meta.uid)) {
                seenUids.add(meta.uid)
                result.add(meta)
            }
        }
        return result.sortedByDescending { it.lastModified }
    }

    fun listMyMaps(context: Context, accountId: String? = null): List<MapMetadata> {
        val effAccountId = if (!accountId.isNullOrBlank()) accountId else sys_menu_socket_repository.currentPlayerId
        return listAllMaps(context, effAccountId).filter { meta ->
            isUserOwnMap(meta, effAccountId) || !meta.isDownloaded
        }
    }

    fun listMapMetadata(context: Context, accountId: String? = null): List<MapMetadata> {
        return listAllMaps(context, accountId)
    }

    fun listMaps(context: Context, accountId: String? = null): List<Pair<String, String>> {
        return listMyMaps(context, accountId).map { Pair(it.uid, it.name) }
    }

    fun deleteMap(context: Context, uid: String): Boolean {
        if (uid.isEmpty()) return false
        var deleted = false
        AppSettings.setBoolean(context, "map_downloaded_$uid", false)
        AppSettings.setBoolean(context, "map_published_$uid", false)
        try {
            val file = File(context.filesDir, "savedmaps")
            if (file.exists()) {
                val lines = file.readLines().filter { it.isNotBlank() && it != uid }
                file.writeText(lines.joinToString("\n") + if (lines.isNotEmpty()) "\n" else "")
            }
        } catch (e: Exception) {}
        val dirsToScan = listOf(
            context.filesDir,
            File(context.filesDir, "from_my_map"),
            File(context.filesDir, "from_online_map"),
            File(context.filesDir, "maps")
        )
        for (dir in dirsToScan) {
            if (dir != null && dir.exists()) {
                val f1 = File(dir, "$uid.map")
                if (f1.exists()) { f1.delete(); deleted = true }
                val f2 = File(dir, "$uid.ob")
                if (f2.exists()) { f2.delete(); deleted = true }
            }
        }
        return deleted
    }

    fun renameMap(context: Context, uid: String, newName: String): Boolean {
        val safeName = sanitizeMapName(newName).trim().ifBlank { "default_map" }
        if (uid.isEmpty() || safeName.isEmpty()) return false
        var renamed = false
        val dirsToScan = listOf(
            context.filesDir,
            File(context.filesDir, "from_my_map"),
            File(context.filesDir, "from_online_map"),
            File(context.filesDir, "maps")
        )
        for (dir in dirsToScan) {
            if (dir != null && dir.exists()) {
                val mapFile = File(dir, "$uid.map")
                if (mapFile.exists()) {
                    try {
                        val mapBytes = mapFile.readBytes()
                        val dis = java.io.DataInputStream(java.io.ByteArrayInputStream(mapBytes))
                        val ver = dis.readByte()
                        val oldName = dis.readUTF()
                        val mapUid = dis.readUTF()
                        val srvId = dis.readInt()
                        val rId = dis.readInt()
                        val w = dis.readShort()
                        val h = dis.readShort()
                        val timestamp = try { dis.readUTF() } catch (e: Exception) { "" }
                        val restBytes = ByteArray(dis.available())
                        dis.readFully(restBytes)
                        dis.close()

                        val baos = java.io.ByteArrayOutputStream()
                        val dos = java.io.DataOutputStream(baos)
                        dos.writeByte(ver.toInt())
                        dos.writeUTF(safeName)
                        dos.writeUTF(mapUid)
                        dos.writeInt(srvId)
                        dos.writeInt(rId)
                        dos.writeShort(w.toInt())
                        dos.writeShort(h.toInt())
                        dos.writeUTF(timestamp)
                        dos.write(restBytes)
                        dos.close()

                        mapFile.writeBytes(baos.toByteArray())
                        renamed = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return renamed
    }
}

data class TextureInfo(
    val name: String = "",
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 32,
    val height: Int = 32,
    val physics: String = ""
)

object TextureDatabase {
    val textures = listOf(
        TextureInfo("042", 655, 1178, 128, 128),
        TextureInfo("20250616_143325", 1313, 894, 32, 32),
        TextureInfo("BS02083", 1045, 393, 128, 128),
        TextureInfo("HexagonTile_DIFF", 1281, 1143, 64, 64),
        TextureInfo("HiMCraft32", 858, 917, 32, 32),
        TextureInfo("asph128", 537, 785, 128, 128),
        TextureInfo("asphv128", 390, 131, 128, 128),
        TextureInfo("beton2", 1310, 373, 32, 32),
        TextureInfo("bl", 0, 1364, 2, 2),
        TextureInfo("block184_128", 917, 1179, 128, 128),
        TextureInfo("block283_128", 930, 655, 128, 128),
        TextureInfo("blockk9_32", 1310, 233, 32, 32),
        TextureInfo("blue32", 1310, 198, 32, 32),
        TextureInfo("derbox24", 1303, 1277, 64, 64),
        TextureInfo("dertex128", 1048, 917, 128, 128),
        TextureInfo("dor-b", 1310, 67, 45, 128),
        TextureInfo("dor3-b", 652, 393, 128, 96),
        TextureInfo("dor3-c", 1048, 1179, 96, 128),
        TextureInfo("dor3", 1169, 1313, 32, 32),
        TextureInfo("dor4", 1061, 741, 32, 32),
        TextureInfo("dor5", 858, 952, 32, 32),
        TextureInfo("dor6", 1313, 859, 32, 32),
        TextureInfo("dor7", 799, 821, 32, 32),
        TextureInfo("drb64", 1310, 0, 64, 64),
        TextureInfo("drbox128", 652, 0, 128, 128),
        TextureInfo("drtx128", 537, 654, 128, 128),
        TextureInfo("drwtx128", 521, 523, 128, 128),
        TextureInfo("e_004", 521, 392, 128, 128),
        TextureInfo("e_007", 521, 261, 128, 128),
        TextureInfo("e_009", 521, 131, 128, 127),
        TextureInfo("e_010", 521, 0, 128, 128),
        TextureInfo("flora_79", 465, 917, 128, 128),
        TextureInfo("g_light", 1179, 1143, 32, 32),
        TextureInfo("granite_21", 406, 786, 128, 128),
        TextureInfo("green32", 203, 1146, 32, 32),
        TextureInfo("grnd8", 390, 262, 128, 128),
        TextureInfo("grnd9", 390, 0, 128, 128),
        TextureInfo("ground_10", 259, 393, 128, 128),
        TextureInfo("ground_12", 259, 524, 128, 128),
        TextureInfo("ground_16", 262, 1111, 128, 128),
        TextureInfo("ground_65", 334, 980, 128, 128),
        TextureInfo("ground_78", 262, 1242, 128, 128),
        TextureInfo("imgs64", 1176, 402, 64, 64),
        TextureInfo("inferno1", 406, 917, 32, 32),
        TextureInfo("k12", 1045, 262, 128, 128),
        TextureInfo("k33", 1243, 402, 64, 64),
        TextureInfo("k48", 390, 393, 128, 128),
        TextureInfo("k5", 1045, 131, 128, 128),
        TextureInfo("k54", 393, 1111, 128, 128),
        TextureInfo("kirp128", 406, 655, 128, 128),
        TextureInfo("kirp_2_128", 393, 1242, 128, 128),
        TextureInfo("kor", 259, 722, 32, 32),
        TextureInfo("kor3", 131, 1314, 64, 32),
        TextureInfo("kor4", 198, 1314, 32, 32),
        TextureInfo("kor5", 203, 1111, 32, 32),
        TextureInfo("kor6", 799, 786, 32, 32),
        TextureInfo("light", 465, 1048, 32, 32),
        TextureInfo("matall1", 1211, 469, 64, 64),
        TextureInfo("met", 294, 722, 32, 32),
        TextureInfo("met2", 329, 722, 32, 32),
        TextureInfo("met23128", 524, 1048, 128, 128),
        TextureInfo("metall2", 1187, 741, 64, 64),
        TextureInfo("metall3", 0, 259, 256, 256),
        TextureInfo("metp1", 1179, 1009, 64, 64),
        TextureInfo("newdor128", 524, 1179, 128, 128),
        TextureInfo("newdr64", 1179, 875, 64, 64),
        TextureInfo("obj_box", 364, 722, 32, 32),
        TextureInfo("obj_ferumbox", 524, 1310, 97, 64),
        TextureInfo("obj_ferumbox1", 624, 1310, 96, 64),
        TextureInfo("obj_konte_1", 0, 1314, 128, 47),
        TextureInfo("obj_konte_mini_1", 1262, 670, 32, 32),
        TextureInfo("obj_metalbox", 259, 655, 128, 64),
        TextureInfo("obj_metalbox1", 1176, 0, 64, 64),
        TextureInfo("obj_wall_bunker", 1120, 875, 32, 32),
        TextureInfo("obj_wall_dom", 1176, 469, 32, 32),
        TextureInfo("p01", 652, 492, 128, 128),
        TextureInfo("p02", 655, 1047, 128, 128),
        TextureInfo("p10", 259, 0, 128, 128),
        TextureInfo("p11", 203, 980, 128, 128),
        TextureInfo("p14", 783, 0, 128, 128),
        TextureInfo("p15", 917, 917, 128, 128),
        TextureInfo("p16", 917, 1048, 128, 128),
        TextureInfo("p3", 668, 623, 128, 128),
        TextureInfo("pesh128", 131, 1183, 128, 128),
        TextureInfo("peshv128", 799, 524, 128, 128),
        TextureInfo("pk02_light02a_C_PB", 901, 1310, 64, 64),
        TextureInfo("pk02_light02b_C_PB", 652, 262, 128, 128),
        TextureInfo("pk02_light02c_C_PB", 1035, 1310, 64, 64),
        TextureInfo("pk02_panels01c_C_PB", 0, 777, 200, 200),
        TextureInfo("pk02_plates01a_S_PB", 727, 885, 128, 128),
        TextureInfo("pk02_wall07b_C_PB", 1102, 1310, 64, 64),
        TextureInfo("plit128", 799, 655, 128, 128),
        TextureInfo("polzavod", 968, 1310, 64, 64),
        TextureInfo("r_light", 1211, 536, 32, 32),
        TextureInfo("red32", 1297, 670, 32, 32),
        TextureInfo("romb128", 259, 262, 128, 128),
        TextureInfo("romb2128", 259, 131, 128, 128),
        TextureInfo("shahm128", 1045, 0, 128, 128),
        TextureInfo("shahmw128", 989, 786, 128, 128),
        TextureInfo("snow10", 0, 518, 256, 256),
        TextureInfo("snow11", 203, 777, 200, 200),
        TextureInfo("snow12", 0, 980, 200, 200),
        TextureInfo("snow14", 914, 393, 128, 128),
        TextureInfo("snow7", 0, 0, 256, 256),
        TextureInfo("snow9", 1120, 808, 64, 64),
        TextureInfo("stone-textures_6_20090324_1735731714", 914, 262, 128, 128),
        TextureInfo("streets24", 914, 131, 128, 128),
        TextureInfo("streets44", 914, 0, 128, 128),
        TextureInfo("t01", 858, 786, 128, 128),
        TextureInfo("t04", 1048, 1048, 128, 128),
        TextureInfo("t1", 596, 916, 128, 128),
        TextureInfo("t10", 786, 1016, 128, 128),
        TextureInfo("t19", 783, 393, 128, 128),
        TextureInfo("t33", 783, 262, 128, 128),
        TextureInfo("t67", 783, 131, 128, 128),
        TextureInfo("t7", 786, 1147, 128, 128),
        TextureInfo("tex_stone", 1061, 674, 64, 64),
        TextureInfo("trav128", 390, 524, 128, 128),
        TextureInfo("trav4128", 930, 524, 128, 128),
        TextureInfo("travk128", 0, 1183, 128, 128),
        TextureInfo("wood2", 1061, 607, 64, 64),
        TextureInfo("wood3", 1120, 741, 64, 64),
        TextureInfo("zabor", 1310, 303, 32, 32),
        TextureInfo("zabor2", 1310, 268, 32, 32)
    )
    fun getTexture(name: String): TextureInfo? = textures.find { it.name == name }
}


