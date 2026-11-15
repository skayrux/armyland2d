package skayrux.shootout2d

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

@Composable
fun menu_upravleniye(onBack: () -> Unit) {
    // плотность 1к1 с координатами управления в datagame_main
    val customDensity = getGameHudDensity()
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides customDensity
    ) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val upravleniyeBitmap = remember { ResourceCache.getUpravleniye(context) }
        val buttonsBitmap = remember { ResourceCache.getAtlasButtons(context) }
        val textureBitmap = remember { ResourceCache.getObjects(context) }
        val leftJoystickState = remember { JoystickState() }
        val rightJoystickState = remember { AttackJoystickState() }
        var resetTrigger by remember { mutableIntStateOf(0) }
        var selectedElementKey by remember { mutableStateOf<String?>(null) }
        var showImportDialog by remember { mutableStateOf(false) }
        var toastMessage by remember { mutableStateOf("") }
        var showToast by remember { mutableStateOf(false) }

        val initialConfig = remember { UpravleniyeStorage.loadConfig(context) }
        var globalAlpha by remember {
            mutableFloatStateOf(initialConfig.alpha.toFloat())
        }

        LaunchedEffect(showToast) {
            if (showToast) {
                kotlinx.coroutines.delay(2000)
                showToast = false
            }
        }

        val keys = SystemControls.map { it.key }

        var scales by remember {
            mutableStateOf(keys.associateWith { key ->
                initialConfig.items[key]?.scale ?: (SystemControls.find { it.key == key }?.defScale ?: 1.0f)
            })
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                            val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                            if (down != null && !down.isConsumed) {
                                selectedElementKey = null
                            }
                        }
                    }
                }
        ) {
            SystemControls.forEach { control ->
                val scaleValue = scales[control.key] ?: control.defScale
                DraggableControlElement(
                    key = control.key,
                    context = context,
                    alignment = control.alignment,
                    defaultX = control.defX,
                    defaultY = control.defY,
                    baseWidth = control.baseWidth,
                    baseHeight = control.baseHeight,
                    isResetTriggered = resetTrigger,
                    isSelected = selectedElementKey == control.key,
                    onSelect = { selectedElementKey = control.key },
                    scale = scaleValue,
                    globalAlpha = globalAlpha / 100f
                ) { actualScale ->
                    control.renderer(
                        upravleniyeBitmap,
                        buttonsBitmap,
                        textureBitmap,
                        actualScale,
                        selectedElementKey == control.key,
                        if (control.key == "left_joystick" || control.key == "right_joystick") leftJoystickState else null,
                        if (control.key == "right_joystick") rightJoystickState else null
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .align(Alignment.TopCenter)
                    .zIndex(10f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    ControlActionButton(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.set_default)
                    ) {
                        val defData = UpravleniyeStorage.resetToDefaults(context)
                        scales = defData.items.mapValues { it.value.scale }
                        globalAlpha = defData.alpha.toFloat()
                        resetTrigger++
                    }
                    ControlActionButton(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_eksport_228f),
                        modifier = Modifier.width(110.dp)
                    ) {
                        val currentData = UpravleniyeData(
                            id = sys_menu_socket_repository.currentPlayerId.ifBlank { "0" },
                            alpha = globalAlpha.toInt(),
                            items = SystemControls.associate { control ->
                                val x = AppSettings.getInt(context, "hud_${control.key}_offsetX", control.defX.toInt()).toFloat()
                                val y = AppSettings.getInt(context, "hud_${control.key}_offsetY", control.defY.toInt()).toFloat()
                                val sc = scales[control.key] ?: control.defScale
                                control.key to UpravleniyeConfigItem(control.key, x, y, sc)
                            }
                        )
                        val configText = UpravleniyeStorage.exportToString(currentData)
                        UpravleniyeStorage.saveConfig(context, currentData)
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("Control Config", configText)
                        clipboardManager.setPrimaryClip(clipData)
                    }
                    ControlActionButton(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_import_d0ce),
                        modifier = Modifier.width(110.dp)
                    ) {
                        showImportDialog = true
                    }
                    ControlActionButton(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back),
                        modifier = Modifier.width(110.dp)
                    ) {
                        onBack()
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_prozrachnost_f20b), color = AppColors.White, fontSize = 14.sp)
                    CustomSlider(
                        value = globalAlpha,
                        onValueChange = { 
                            globalAlpha = it
                            val curr = UpravleniyeStorage.loadConfig(context)
                            UpravleniyeStorage.saveConfig(context, curr.copy(alpha = it.toInt()))
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

                // кнопки изменения размера элемента
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.PanelDark)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    down.consume()
                                    selectedElementKey?.let { key ->
                                        val currentScale = scales[key] ?: 1.0f
                                        val newScale = (currentScale + 0.05f).coerceAtMost(2.0f)
                                        scales = scales.toMutableMap().apply { put(key, newScale) }
                                        val curr = UpravleniyeStorage.loadConfig(context)
                                        val items = curr.items.toMutableMap()
                                        items[key] = items[key]?.copy(scale = newScale) ?: UpravleniyeConfigItem(key, 0f, 0f, newScale)
                                        UpravleniyeStorage.saveConfig(context, curr.copy(items = items))
                                    }
                                    val holdJob = coroutineScope.launch {
                                        kotlinx.coroutines.delay(300L)
                                        while (true) {
                                            selectedElementKey?.let { key ->
                                                val currentScale = scales[key] ?: 1.0f
                                                val newScale = (currentScale + 0.02f).coerceAtMost(2.0f)
                                                scales = scales.toMutableMap().apply { put(key, newScale) }
                                                val curr = UpravleniyeStorage.loadConfig(context)
                                                val items = curr.items.toMutableMap()
                                                items[key] = items[key]?.copy(scale = newScale) ?: UpravleniyeConfigItem(key, 0f, 0f, newScale)
                                                UpravleniyeStorage.saveConfig(context, curr.copy(items = items))
                                            }
                                            kotlinx.coroutines.delay(80L)
                                        }
                                    }
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                break
                                            }
                                        }
                                    } finally {
                                        holdJob.cancel()
                                    }
                                }
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            color = AppColors.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .width(50.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.PanelDark)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    down.consume()
                                    selectedElementKey?.let { key ->
                                        val currentScale = scales[key] ?: 1.0f
                                        val newScale = (currentScale - 0.05f).coerceAtLeast(0.3f)
                                        scales = scales.toMutableMap().apply { put(key, newScale) }
                                        val curr = UpravleniyeStorage.loadConfig(context)
                                        val items = curr.items.toMutableMap()
                                        items[key] = items[key]?.copy(scale = newScale) ?: UpravleniyeConfigItem(key, 0f, 0f, newScale)
                                        UpravleniyeStorage.saveConfig(context, curr.copy(items = items))
                                    }
                                    val holdJob = coroutineScope.launch {
                                        kotlinx.coroutines.delay(300L)
                                        while (true) {
                                            selectedElementKey?.let { key ->
                                                val currentScale = scales[key] ?: 1.0f
                                                val newScale = (currentScale - 0.02f).coerceAtLeast(0.3f)
                                                scales = scales.toMutableMap().apply { put(key, newScale) }
                                                val curr = UpravleniyeStorage.loadConfig(context)
                                                val items = curr.items.toMutableMap()
                                                items[key] = items[key]?.copy(scale = newScale) ?: UpravleniyeConfigItem(key, 0f, 0f, newScale)
                                                UpravleniyeStorage.saveConfig(context, curr.copy(items = items))
                                            }
                                            kotlinx.coroutines.delay(80L)
                                        }
                                    }
                                    try {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id }
                                            if (change == null || !change.pressed) {
                                                break
                                            }
                                        }
                                    } finally {
                                        holdJob.cancel()
                                    }
                                }
                            }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
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

            if (showImportDialog) {
                var textInput by remember { mutableStateOf("") }

                UnifiedDialog(
                    title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_import_upravleniya_0ec3),
                    confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_import_d0ce),
                    confirmEnabled = textInput.isNotBlank(),
                    onDismiss = {
                        showImportDialog = false
                    },
                    onConfirm = {
                        val parsed = UpravleniyeStorage.parseFromString(textInput)
                        if (parsed != null) {
                            UpravleniyeStorage.saveConfig(context, parsed)
                            scales = parsed.items.mapValues { it.value.scale }
                            globalAlpha = parsed.alpha.toFloat()
                            showImportDialog = false
                            resetTrigger++
                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_upravlenie_primeneno_a15b)
                            showToast = true
                        } else {
                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nevernyy_format_upravleniya_e485)
                            showToast = true
                        }
                    },
                    dismissOnConfirm = isValidConfig(textInput),
                    dismissOnClickOutside = false,
                    content = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UnifiedTextField(
                                value = textInput,
                                onValueChange = {
                                    textInput = it
                                },
                                singleLine = false,
                                heightDp = 100,
                                placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vstavte_kod_upravleniya_0380),
                                maxLength = 1500,
                                requestFocus = true
                            )
                        }
                    }
                )
            }

            UnifiedToast(message = toastMessage, visible = showToast)
        }
    }
}

@Composable
fun DraggableControlElement(
    key: String,
    context: android.content.Context,
    alignment: Alignment,
    defaultX: Float,
    defaultY: Float,
    baseWidth: Float,
    baseHeight: Float,
    isResetTriggered: Int,
    isSelected: Boolean,
    onSelect: () -> Unit,
    scale: Float,
    globalAlpha: Float = 1f,
    content: @Composable (Float) -> Unit
) {
    var offsetX by remember(key) {
        mutableStateOf(AppSettings.getInt(context, "hud_${key}_offsetX", defaultX.toInt()).toFloat())
    }
    var offsetY by remember(key) {
        mutableStateOf(AppSettings.getInt(context, "hud_${key}_offsetY", defaultY.toInt()).toFloat())
    }

    LaunchedEffect(isResetTriggered) {
        if (isResetTriggered > 0) {
            offsetX = AppSettings.getInt(context, "hud_${key}_offsetX", defaultX.toInt()).toFloat()
            offsetY = AppSettings.getInt(context, "hud_${key}_offsetY", defaultY.toInt()).toFloat()
        }
    }

    val density = LocalDensity.current.density
    val factorX = when (alignment) {
        Alignment.BottomStart, Alignment.TopStart, Alignment.CenterStart -> -0.5f
        Alignment.BottomEnd, Alignment.TopEnd, Alignment.CenterEnd -> 0.5f
        else -> 0f
    }
    val factorY = when (alignment) {
        Alignment.TopStart, Alignment.TopEnd, Alignment.TopCenter -> -0.5f
        Alignment.BottomStart, Alignment.BottomEnd, Alignment.BottomCenter -> 0.5f
        else -> 0f
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .offset {
                    val x = offsetX + (scale - 1f) * baseWidth * factorX
                    val y = offsetY + (scale - 1f) * baseHeight * factorY
                    androidx.compose.ui.unit.IntOffset(x.dp.roundToPx(), y.dp.roundToPx())
                }
        ) {
            Box(
                modifier = Modifier
                    .pointerInput(key) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            down.consume()
                            onSelect()
                            val dragPointerId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val dragEvent = event.changes.firstOrNull { it.id == dragPointerId } ?: break
                                if (!dragEvent.pressed) break
                                val dragAmount = dragEvent.position - dragEvent.previousPosition
                                if (dragAmount != Offset.Zero) {
                                    dragEvent.consume()
                                    offsetX += dragAmount.x / density
                                    offsetY += dragAmount.y / density
                                }
                            }
                            val savedX = kotlin.math.round(offsetX)
                            val savedY = kotlin.math.round(offsetY)
                            offsetX = savedX
                            offsetY = savedY
                            AppSettings.setInt(context, "hud_${key}_offsetX", savedX.toInt())
                            AppSettings.setInt(context, "hud_${key}_offsetY", savedY.toInt())
                            val curr = UpravleniyeStorage.loadConfig(context)
                            val items = curr.items.toMutableMap()
                            items[key] = items[key]?.copy(x = savedX, y = savedY) ?: UpravleniyeConfigItem(key, savedX, savedY, scale)
                            UpravleniyeStorage.saveConfig(context, curr.copy(items = items))
                        }
                    }
                    .alpha(globalAlpha)
            ) {
                content(scale)
            }
        }
    }
}

@Composable
private fun ControlActionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bgColor = if (enabled) AppColors.PanelDark else AppColors.PanelDark.copy(alpha = 0.4f)
    val borderColor = if (enabled) AppColors.Border else AppColors.Border.copy(alpha = 0.4f)
    val textColor = if (enabled) AppColors.White else AppColors.White.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .height(30.dp)
            .defaultMinSize(minWidth = 110.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun exportConfig(context: Context, currentScales: Map<String, Float>): String {
    val currentData = UpravleniyeData(
        id = sys_menu_socket_repository.currentPlayerId.ifBlank { "0" },
        alpha = AppSettings.getInt(context, "hud_global_alpha", 90),
        items = SystemControls.associate { control ->
            val x = AppSettings.getInt(context, "hud_${control.key}_offsetX", control.defX.toInt()).toFloat()
            val y = AppSettings.getInt(context, "hud_${control.key}_offsetY", control.defY.toInt()).toFloat()
            val sc = currentScales[control.key] ?: control.defScale
            control.key to UpravleniyeConfigItem(control.key, x, y, sc)
        }
    )
    UpravleniyeStorage.saveConfig(context, currentData)
    return UpravleniyeStorage.exportToString(currentData)
}

private fun isValidConfig(input: String): Boolean {
    return UpravleniyeStorage.parseFromString(input) != null
}

private fun importConfig(context: Context, input: String, onApplyScales: (Map<String, Float>) -> Unit): Boolean {
    val parsed = UpravleniyeStorage.parseFromString(input) ?: return false
    UpravleniyeStorage.saveConfig(context, parsed)
    onApplyScales(parsed.items.mapValues { it.value.scale })
    return true
}
