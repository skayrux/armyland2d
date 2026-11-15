package skayrux.shootout2d

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import android.content.Context

class JoystickState(
    var isDragging: Boolean = false,
    var rawDx: Float = 0f,
    var rawDy: Float = 0f
)

class AttackJoystickState(
    var isDragging: Boolean = false,
    var rawDx: Float = 0f,
    var rawDy: Float = 0f
) {
    fun isAttackingTriggered(maxRadius: Float): Boolean {
        val d = kotlin.math.sqrt(rawDx * rawDx + rawDy * rawDy)
        return d > maxRadius * 0.4f
    }
}

data class UpravleniyeConfigItem(
    val key: String = "",
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f
)

data class UpravleniyeData(
    val id: String = "0",
    val alpha: Int = 90,
    val items: Map<String, UpravleniyeConfigItem> = emptyMap()
)

object UpravleniyeStorage {
    fun loadConfig(context: Context): UpravleniyeData {
        val alpha = AppSettings.getInt(context, "hud_global_alpha", 90)
        val items = SystemControls.associate { control ->
            val x = AppSettings.getInt(context, "hud_${control.key}_offsetX", kotlin.math.round(control.defX).toInt()).toFloat()
            val y = AppSettings.getInt(context, "hud_${control.key}_offsetY", kotlin.math.round(control.defY).toInt()).toFloat()
            val scale = AppSettings.getFloat(context, "hud_${control.key}_scale", control.defScale)
            control.key to UpravleniyeConfigItem(control.key, x, y, scale)
        }
        return UpravleniyeData(id = sys_menu_socket_repository.currentPlayerId.ifBlank { "0" }, alpha = alpha, items = items)
    }

    fun saveConfig(context: Context, data: UpravleniyeData) {
        AppSettings.setInt(context, "hud_global_alpha", data.alpha)
        data.items.forEach { (key, item) ->
            AppSettings.setInt(context, "hud_${key}_offsetX", kotlin.math.round(item.x).toInt())
            AppSettings.setInt(context, "hud_${key}_offsetY", kotlin.math.round(item.y).toInt())
            AppSettings.setFloat(context, "hud_${key}_scale", item.scale)
        }
        datagame_main.instance?.reloadInGameSettings()
    }

    fun resetToDefaults(context: Context): UpravleniyeData {
        AppSettings.setInt(context, "hud_global_alpha", 90)
        SystemControls.forEach { control ->
            AppSettings.setInt(context, "hud_${control.key}_offsetX", kotlin.math.round(control.defX).toInt())
            AppSettings.setInt(context, "hud_${control.key}_offsetY", kotlin.math.round(control.defY).toInt())
            AppSettings.setFloat(context, "hud_${control.key}_scale", control.defScale)
        }
        val config = loadConfig(context)
        datagame_main.instance?.reloadInGameSettings()
        return config
    }

    fun exportToString(data: UpravleniyeData): String {
        val sb = StringBuilder()
        sb.append("alpha=${data.alpha}\n")
        data.items.forEach { (k, v) ->
            sb.append("${k}=${v.x},${v.y},${v.scale}\n")
        }
        return sb.toString()
    }

    fun parseFromString(text: String): UpravleniyeData? {
        try {
            var alpha = 90
            val items = mutableMapOf<String, UpravleniyeConfigItem>()
            text.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("alpha=")) {
                    alpha = trimmed.substringAfter("=").toIntOrNull() ?: 90
                } else if (trimmed.contains("=")) {
                    val k = trimmed.substringBefore("=")
                    val parts = trimmed.substringAfter("=").split(",")
                    if (parts.size >= 3) {
                        val x = parts[0].toFloatOrNull() ?: 0f
                        val y = parts[1].toFloatOrNull() ?: 0f
                        val scale = parts[2].toFloatOrNull() ?: 1f
                        items[k] = UpravleniyeConfigItem(k, x, y, scale)
                    }
                }
            }
            if (items.isEmpty()) return null
            return UpravleniyeData(id = sys_menu_socket_repository.currentPlayerId.ifBlank { "0" }, alpha = alpha, items = items)
        } catch (e: Exception) {
            return null
        }
    }

    fun importFromString(context: Context, text: String): Boolean {
        val parsed = parseFromString(text) ?: return false
        saveConfig(context, parsed)
        return true
    }
}

data class SettingsCtrlDefault(
    val key: String,
    val defX: Float,
    val defY: Float,
    val defScale: Float,
    val baseWidth: Float,
    val baseHeight: Float,
    val alignment: Alignment,
    val renderer: @Composable (
        atlasBitmap: ImageBitmap?,
        buttonsBitmap: ImageBitmap?,
        textureBitmap: ImageBitmap?,
        scale: Float,
        isHighlighted: Boolean,
        leftState: JoystickState?,
        rightState: AttackJoystickState?
    ) -> Unit
)

val SystemControls = listOf(
    SettingsCtrlDefault(
        key = "left_joystick",
        defX = 35f, defY = -35f, defScale = 1.0f,
        baseWidth = 190f, baseHeight = 190f,
        alignment = Alignment.BottomStart,
        renderer = { atlas, _, _, scale, isHighlighted, left, _ ->
            if (left != null) {
                JoystickUpravleniye(
                    state = left,
                    atlasBitmap = atlas,
                    isLeft = true,
                    scale = scale,
                    enabled = false,
                    isHighlighted = isHighlighted)
            }
        }
    ),
    SettingsCtrlDefault(
        key = "right_joystick",
        defX = -35f, defY = -35f, defScale = 1.0f,
        baseWidth = 190f, baseHeight = 190f,
        alignment = Alignment.BottomEnd,
        renderer = { atlas, _, _, scale, isHighlighted, left, right ->
            if (left != null) {
                JoystickUpravleniye(
                    state = left,
                    atlasBitmap = atlas,
                    isLeft = false,
                    attackState = right,
                    scale = scale,
                    enabled = false,
                    isHighlighted = isHighlighted)
            }
        }
    ),
    SettingsCtrlDefault(
        key = "grenade",
        defX = 225f, defY = -127.5f, defScale = 0.75f,
        baseWidth = 80f, baseHeight = 60f,
        alignment = Alignment.BottomStart,
        renderer = { _, buttons, tex, scale, isHighlighted, _, _ ->
            AtlasGrenadeButton(
                atlasBitmap = buttons,
                textureBitmap = tex,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    SettingsCtrlDefault(
        key = "weapon_change",
        defX = -197f, defY = -204.5f, defScale = 0.7166f,
        baseWidth = 120f, baseHeight = 60f,
        alignment = Alignment.BottomEnd,
        renderer = { _, buttons, tex, scale, isHighlighted, _, _ ->
            AtlasWeaponChangeButton(
                atlasBitmap = buttons,
                textureBitmap = tex,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    SettingsCtrlDefault(
        key = "reload",
        defX = -212f, defY = -152.5f, defScale = 0.7166f,
        baseWidth = 120f, baseHeight = 60f,
        alignment = Alignment.BottomEnd,
        renderer = { _, buttons, _, scale, isHighlighted, _, _ ->
            AtlasReloadButton(
                atlasBitmap = buttons,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    SettingsCtrlDefault(
        key = "hand",
        defX = -222f, defY = -100.5f, defScale = 0.7166f,
        baseWidth = 120f, baseHeight = 60f,
        alignment = Alignment.BottomEnd,
        renderer = { _, buttons, _, scale, isHighlighted, _, _ ->
            AtlasHandButton(
                atlasBitmap = buttons,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    // нижние кнопки управления, выравнивание по центру
    SettingsCtrlDefault(
        key = "spectators",
        defX = -84f, defY = -32f, defScale = 0.6666f,
        baseWidth = 60f, baseHeight = 60f,
        alignment = Alignment.BottomCenter,
        renderer = { _, buttons, _, scale, isHighlighted, _, _ ->
            AtlasSpectatorsButton(
                atlasBitmap = buttons,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    SettingsCtrlDefault(
        key = "team_change",
        defX = -42f, defY = -32f, defScale = 0.6666f,
        baseWidth = 60f, baseHeight = 60f,
        alignment = Alignment.BottomCenter,
        renderer = { _, buttons, _, scale, isHighlighted, _, _ ->
            AtlasTeamChangeButton(
                atlasBitmap = buttons,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    SettingsCtrlDefault(
        key = "walkie_talkie",
        defX = 0f, defY = -32f, defScale = 0.6666f,
        baseWidth = 60f, baseHeight = 60f,
        alignment = Alignment.BottomCenter,
        renderer = { _, buttons, _, scale, isHighlighted, _, _ ->
            AtlasWalkieTalkieButton(
                atlasBitmap = buttons,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    SettingsCtrlDefault(
        key = "chat",
        defX = 42f, defY = -32f, defScale = 0.6666f,
        baseWidth = 60f, baseHeight = 60f,
        alignment = Alignment.BottomCenter,
        renderer = { _, buttons, _, scale, isHighlighted, _, _ ->
            AtlasChatButton(
                atlasBitmap = buttons,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    SettingsCtrlDefault(
        key = "red_l",
        defX = 84f, defY = -32f, defScale = 0.6666f,
        baseWidth = 60f, baseHeight = 60f,
        alignment = Alignment.BottomCenter,
        renderer = { _, buttons, _, scale, isHighlighted, _, _ ->
            AtlasRedLButton(
                atlasBitmap = buttons,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    ),
    SettingsCtrlDefault(
        key = "hide_chat",
        defX = -8f, defY = -10f, defScale = 0.6666f,
        baseWidth = 60f, baseHeight = 60f,
        alignment = Alignment.CenterStart,
        renderer = { _, buttons, _, scale, isHighlighted, _, _ ->
            AtlasToggleChatButton(
                atlasBitmap = buttons,
                onClick = {},
                scale = scale,
                isHighlighted = isHighlighted)
            }
    )
)

@Composable
fun JoystickUpravleniye(
    state: JoystickState,
    modifier: Modifier = Modifier,
    atlasBitmap: ImageBitmap?,
    isLeft: Boolean = true,
    attackState: AttackJoystickState? = null,
    scale: Float = 1.0f,
    enabled: Boolean = true,
    isHighlighted: Boolean = false) {
    val maxRadiusPx = 80.0f

    val baseModifier = if (enabled) {
        modifier
            .size((190 * scale).dp)
            .pointerInput(isLeft) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val centerPx = size.width / 2f
                    val scaleFactor = size.width / 233f

                    fun updateTouch(offset: Offset) {
                        val dx = offset.x - centerPx
                        val dy = offset.y - centerPx
                        if (isLeft) {
                            state.rawDx = dx / scaleFactor
                            state.rawDy = dy / scaleFactor
                        } else if (attackState != null) {
                            attackState.rawDx = dx / scaleFactor
                            attackState.rawDy = dy / scaleFactor
                        }
                    }

                    if (isLeft) {
                        state.isDragging = true
                    } else if (attackState != null) {
                        attackState.isDragging = true
                    }
                    updateTouch(down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val move = event.changes.firstOrNull { it.pressed } ?: break
                        updateTouch(move.position)
                        move.consume()
                    }

                    if (isLeft) {
                        state.isDragging = false
                        state.rawDx = 0f
                        state.rawDy = 0f
                    } else if (attackState != null) {
                        attackState.isDragging = false
                        attackState.rawDx = 0f
                        attackState.rawDy = 0f
                    }
                }
            }
    } else {
        modifier.size((190 * scale).dp)
    }

    Box(
        modifier = baseModifier
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            if (atlasBitmap != null) {
                if (isLeft) {
                    drawImage(
                        image = atlasBitmap,
                        srcOffset = IntOffset(0, 0),
                        srcSize = IntSize(233, 233),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(canvasW.toInt(), canvasH.toInt()),
                        filterQuality = FilterQuality.High
                    )

                    val lx = state.rawDx
                    val ly = state.rawDy
                    val d = kotlin.math.sqrt(lx * lx + ly * ly)
                    val clampedLx = if (d > maxRadiusPx) (lx / d) * maxRadiusPx else lx
                    val clampedLy = if (d > maxRadiusPx) (ly / d) * maxRadiusPx else ly

                    val centerPx = 233f / 2f
                    val thumbRadius = 56f / 2f
                    val visualXInAtlas = centerPx + clampedLx - thumbRadius
                    val visualYInAtlas = centerPx + clampedLy - thumbRadius

                    val scaleX = canvasW / 233f
                    val scaleY = canvasH / 233f

                    drawImage(
                        image = atlasBitmap,
                        srcOffset = IntOffset(234, 234),
                        srcSize = IntSize(76, 76),
                        dstOffset = IntOffset((visualXInAtlas * scaleX).toInt(), (visualYInAtlas * scaleY).toInt()),
                        dstSize = IntSize((56f * scaleX).toInt(), (56f * scaleY).toInt()),
                        filterQuality = FilterQuality.High
                    )
                } else if (attackState != null) {
                    val attacking = attackState.isAttackingTriggered(maxRadiusPx) || isHighlighted
                    val srcBackgroundX = if (attacking) 0 else 234
                    val srcBackgroundY = if (attacking) 234 else 0

                    drawImage(
                        image = atlasBitmap,
                        srcOffset = IntOffset(srcBackgroundX, srcBackgroundY),
                        srcSize = IntSize(233, 233),
                        dstOffset = IntOffset(0, 0),
                        dstSize = IntSize(canvasW.toInt(), canvasH.toInt()),
                        filterQuality = FilterQuality.High
                    )

                    val lx = attackState.rawDx
                    val ly = attackState.rawDy
                    val d = kotlin.math.sqrt(lx * lx + ly * ly)
                    val clampedLx = if (d > maxRadiusPx) (lx / d) * maxRadiusPx else lx
                    val clampedLy = if (d > maxRadiusPx) (ly / d) * maxRadiusPx else ly

                    val centerPx = 233f / 2f
                    val thumbRadius = 56f / 2f
                    val visualXInAtlas = centerPx + clampedLx - thumbRadius
                    val visualYInAtlas = centerPx + clampedLy - thumbRadius

                    val scaleX = canvasW / 233f
                    val scaleY = canvasH / 233f

                    drawImage(
                        image = atlasBitmap,
                        srcOffset = IntOffset(234, 311),
                        srcSize = IntSize(76, 76),
                        dstOffset = IntOffset((visualXInAtlas * scaleX).toInt(), (visualYInAtlas * scaleY).toInt()),
                        dstSize = IntSize((56f * scaleX).toInt(), (56f * scaleY).toInt()),
                        filterQuality = FilterQuality.High
                    )
                }
            }
        }
    }
}

@Composable
fun AtlasButton(
    atlasBitmap: ImageBitmap?,
    srcX: Int, srcY: Int, srcW: Int, srcH: Int,
    srcPressX: Int, srcPressY: Int, srcPressW: Int, srcPressH: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    isHighlighted: Boolean = false) {
    var isPressed by remember { mutableStateOf(false) }
    val effectivePressed = isPressed || isHighlighted
    Box(
        modifier = modifier
            .size((srcW * scale).dp, (srcH * scale).dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val pointerId = down.id
                    while (true) {
                        val event = awaitPointerEvent()
                        val anyPressed = event.changes.any { it.id == pointerId && it.pressed }
                        if (!anyPressed) {
                            break
                        }
                        event.changes.forEach { if (it.id == pointerId) it.consume() }
                    }
                    isPressed = false
                    onClick()
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (atlasBitmap != null) {
                val currentX = if (effectivePressed) srcPressX else srcX
                val currentY = if (effectivePressed) srcPressY else srcY
                val currentW = if (effectivePressed) srcPressW else srcW
                val currentH = if (effectivePressed) srcPressH else srcH
                drawImage(
                    image = atlasBitmap,
                    srcOffset = IntOffset(currentX, currentY),
                    srcSize = IntSize(currentW, currentH),
                    dstOffset = IntOffset(0, 0),
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                    filterQuality = FilterQuality.High
                )
            }
        }
    }
}

@Composable
fun AtlasReloadButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 0, srcY = 596, srcW = 120, srcH = 60,
        srcPressX = 0, srcPressY = 533, srcPressW = 120, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasWeaponChangeButton(atlasBitmap: ImageBitmap?, textureBitmap: ImageBitmap? = null, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AtlasButton(
            atlasBitmap = atlasBitmap,
            srcX = 229, srcY = 0, srcW = 120, srcH = 60,
            srcPressX = 163, srcPressY = 149, srcPressW = 120, srcPressH = 60,
            onClick = onClick,
            scale = scale,
            isHighlighted = isHighlighted
        )
        if (textureBitmap != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size((120 * scale).dp, (60 * scale).dp)) {
                val dstW = (50 * scale).dp.toPx().toInt()
                val dstH = (50 * scale).dp.toPx().toInt()
                val dstX = ((120 * scale).dp.toPx() / 2 - dstW / 2 + (20 * scale).dp.toPx()).toInt()
                val dstY = ((60 * scale).dp.toPx() / 2 - dstH / 2).toInt()
                drawImage(
                    image = textureBitmap,
                    srcOffset = IntOffset(777, 683),
                    srcSize = IntSize(120, 120),
                    dstOffset = IntOffset(dstX, dstY),
                    dstSize = IntSize(dstW, dstH)
                )
            }
        }
    }
}

@Composable
fun AtlasGrenadeButton(atlasBitmap: ImageBitmap?, textureBitmap: ImageBitmap? = null, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AtlasButton(
            atlasBitmap = atlasBitmap,
            srcX = 352, srcY = 0, srcW = 80, srcH = 60,
            srcPressX = 369, srcPressY = 378, srcPressW = 80, srcPressH = 60,
            onClick = onClick,
            scale = scale,
            isHighlighted = isHighlighted
        )
        if (textureBitmap != null) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size((80 * scale).dp, (60 * scale).dp)) {
                val dstW = (30 * scale).dp.toPx().toInt()
                val dstH = (30 * scale).dp.toPx().toInt()
                val dstX = ((80 * scale).dp.toPx() / 2 - dstW / 2 - (22 * scale).dp.toPx()).toInt()
                val dstY = ((60 * scale).dp.toPx() / 2 - dstH / 2 + (2 * scale).dp.toPx()).toInt()
                drawImage(
                    image = textureBitmap,
                    srcOffset = IntOffset(817, 1052),
                    srcSize = IntSize(40, 40),
                    dstOffset = IntOffset(dstX, dstY),
                    dstSize = IntSize(dstW, dstH)
                )
            }
        }
    }
}

@Composable
fun AtlasHandButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 153, srcY = 290, srcW = 120, srcH = 60,
        srcPressX = 123, srcPressY = 533, srcPressW = 120, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasWalkieTalkieButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 519, srcY = 126, srcW = 60, srcH = 60,
        srcPressX = 561, srcPressY = 0, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasChatButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 456, srcY = 126, srcW = 60, srcH = 60,
        srcPressX = 456, srcPressY = 189, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasPlayerTabButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 452, srcY = 332, srcW = 60, srcH = 60,
        srcPressX = 452, srcPressY = 395, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasRedLButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 609, srcY = 504, srcW = 60, srcH = 60,
        srcPressX = 588, srcPressY = 252, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasSpectatorsButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 483, srcY = 567, srcW = 60, srcH = 60,
        srcPressX = 582, srcPressY = 63, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasTeamChangeButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 582, srcY = 189, srcW = 60, srcH = 60,
        srcPressX = 582, srcPressY = 126, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasToggleChatButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AtlasButton(
            atlasBitmap = atlasBitmap,
            srcX = 578, srcY = 315, srcW = 60, srcH = 60,
            srcPressX = 568, srcPressY = 441, srcPressW = 60, srcPressH = 60,
            onClick = onClick,
            scale = scale,
            isHighlighted = isHighlighted
        )
        Text(
            text = "<",
            color = Color.White,
            fontSize = (28 * scale).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun AtlasBackButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 123, srcY = 596, srcW = 120, srcH = 60,
        srcPressX = 153, srcPressY = 416, srcPressW = 120, srcPressH = 60,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun AtlasDefaultButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 0, srcY = 239, srcW = 160, srcH = 48,
        srcPressX = 0, srcPressY = 188, srcPressW = 160, srcPressH = 48,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun AtlasZoomInButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 578, srcY = 378, srcW = 60, srcH = 60,
        srcPressX = 498, srcPressY = 0, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun AtlasZoomOutButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 525, srcY = 252, srcW = 60, srcH = 60,
        srcPressX = 519, srcPressY = 189, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier
    )
}

enum class MenuButState {
    NORMAL,
    BOUGHT,
    NO_MONEY,
    DISABLED
}

@Composable
fun AtlasB60Button(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 578, srcY = 315, srcW = 60, srcH = 60,
        srcPressX = 568, srcPressY = 441, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasBCamButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 163, srcY = 86, srcW = 120, srcH = 60,
        srcPressX = 163, srcPressY = 212, srcPressW = 120, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasBComButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasWalkieTalkieButton(
        atlasBitmap = atlasBitmap,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasDislikeButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 153, srcY = 479, srcW = 64, srcH = 50,
        srcPressX = 389, srcPressY = 63, srcPressW = 64, srcPressH = 50,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasInfoButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 349, srcY = 615, srcW = 64, srcH = 50,
        srcPressX = 389, srcPressY = 116, srcPressW = 64, srcPressH = 50,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasLikeButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 389, srcY = 169, srcW = 64, srcH = 50,
        srcPressX = 416, srcPressY = 615, srcPressW = 64, srcPressH = 50,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasBMaxButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 609, srcY = 567, srcW = 48, srcH = 48,
        srcPressX = 609, srcPressY = 567, srcPressW = 48, srcPressH = 48,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasBOutButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 123, srcY = 596, srcW = 120, srcH = 60,
        srcPressX = 153, srcPressY = 416, srcPressW = 120, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasPlayMapButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 515, srcY = 315, srcW = 60, srcH = 60,
        srcPressX = 505, srcPressY = 458, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasBPlListButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 452, srcY = 332, srcW = 60, srcH = 60,
        srcPressX = 452, srcPressY = 395, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasRedoButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 442, srcY = 504, srcW = 60, srcH = 60,
        srcPressX = 435, srcPressY = 0, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasSaveButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 456, srcY = 63, srcW = 60, srcH = 60,
        srcPressX = 462, srcPressY = 252, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasUndoButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 519, srcY = 63, srcW = 60, srcH = 60,
        srcPressX = 515, srcPressY = 378, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasBpbButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 0, srcY = 371, srcW = 150, srcH = 78,
        srcPressX = 0, srcPressY = 452, srcPressW = 150, srcPressH = 78,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasRpbButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 0, srcY = 290, srcW = 150, srcH = 78,
        srcPressX = 0, srcPressY = 452, srcPressW = 150, srcPressH = 78,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasChanButton(atlasBitmap: ImageBitmap?, textureBitmap: ImageBitmap? = null, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasWeaponChangeButton(
        atlasBitmap = atlasBitmap,
        textureBitmap = textureBitmap,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasEmpty60Button(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 578, srcY = 315, srcW = 60, srcH = 60,
        srcPressX = 568, srcPressY = 441, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasD2Button(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 349, srcY = 542, srcW = 90, srcH = 70,
        srcPressX = 276, srcPressY = 378, srcPressW = 90, srcPressH = 70,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasD3Button(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 0, srcY = 0, srcW = 226, srcH = 40,
        srcPressX = 0, srcPressY = 43, srcPressW = 226, srcPressH = 40,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasGiftButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 546, srcY = 584, srcW = 60, srcH = 60,
        srcPressX = 546, srcPressY = 521, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasMenuShopButton(
    atlasBitmap: ImageBitmap?,
    state: MenuButState = MenuButState.NORMAL,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    isHighlighted: Boolean = false
) {
    val (curX, curY) = when (state) {
        MenuButState.NORMAL -> Pair(0, 239)
        MenuButState.BOUGHT -> Pair(0, 188)
        MenuButState.NO_MONEY -> Pair(0, 137)
        MenuButState.DISABLED -> Pair(0, 86)
    }
    val pressX = 0
    val pressY = 188
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = curX, srcY = curY, srcW = 160, srcH = 48,
        srcPressX = pressX, srcPressY = pressY, srcPressW = 160, srcPressH = 48,
        onClick = if (state == MenuButState.DISABLED) ({}) else onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasRowBottomButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 286, srcY = 63, srcW = 100, srcH = 100,
        srcPressX = 286, srcPressY = 166, srcPressW = 100, srcPressH = 100,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasRowTopButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 276, srcY = 275, srcW = 100, srcH = 100,
        srcPressX = 246, srcPressY = 542, srcPressW = 100, srcPressH = 100,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasS7ChanButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 153, srcY = 353, srcW = 120, srcH = 60,
        srcPressX = 246, srcPressY = 479, srcPressW = 120, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasS7GrButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 369, srcY = 441, srcW = 80, srcH = 60,
        srcPressX = 379, srcPressY = 269, srcPressW = 80, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasTablButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasButton(
        atlasBitmap = atlasBitmap,
        srcX = 609, srcY = 504, srcW = 60, srcH = 60,
        srcPressX = 588, srcPressY = 252, srcPressW = 60, srcPressH = 60,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

@Composable
fun AtlasUseButton(atlasBitmap: ImageBitmap?, onClick: () -> Unit, modifier: Modifier = Modifier, scale: Float = 1.0f, isHighlighted: Boolean = false) {
    AtlasHandButton(
        atlasBitmap = atlasBitmap,
        onClick = onClick,
        modifier = modifier,
        scale = scale,
        isHighlighted = isHighlighted
    )
}

object AtlasButtonLogic {
    val walkieTalkieAllowedModes = setOf(
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game_score),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.arms_race),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.zombie_infection),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.ghost),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.juggernaut),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.capture_the_flag),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.base_defence),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bugs_attack)
    )

    fun isWalkieTalkieAvailable(gameMode: String): Boolean {
        return walkieTalkieAllowedModes.contains(gameMode.trim().lowercase())
    }

    val teamChangeAllowedModes = setOf(
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.team_game_score),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.arms_race),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.capture_the_flag),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.base_defence),
        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.base_defence)
    )

    fun isTeamChangeAvailable(gameMode: String): Boolean {
        return teamChangeAllowedModes.contains(gameMode.trim().lowercase())
    }

    fun canSwitchCamera(isDead: Boolean, isSpectator: Boolean): Boolean {
        return isDead || isSpectator
    }

    fun canExitVehicle(inVehicle: Boolean): Boolean {
        return inVehicle
    }
}

data class AtlasButtonInfo(
    val id: String,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int,
    val pressX: Int,
    val pressY: Int,
    val pressW: Int,
    val pressH: Int,
    val description: String = ""
)

object AtlasButtonsDatabase {
    val buttons = listOf(
        AtlasButtonInfo("b_60", 578, 315, 60, 60, 568, 441, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_pustaya_knopka_05ae)),
        AtlasButtonInfo("b_admin", 546, 584, 60, 60, 546, 521, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_admin_podarok_95e4)),
        AtlasButtonInfo("b_cam", 163, 86, 120, 60, 163, 212, 120, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_knopka_kamery_3d6c)),
        AtlasButtonInfo("b_com", 519, 126, 60, 60, 561, 0, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.d3s1)),
        AtlasButtonInfo("b_dislike", 153, 479, 64, 50, 389, 63, 64, 50, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_dizlayk_2d27)),
        AtlasButtonInfo("b_info", 349, 615, 64, 50, 389, 116, 64, 50, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.info)),
        AtlasButtonInfo("b_like", 389, 169, 64, 50, 416, 615, 64, 50, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_layk_a6f8)),
        AtlasButtonInfo("b_max", 609, 567, 48, 48, 609, 567, 48, 48, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_dlya_mini_karty_7aff)),
        AtlasButtonInfo("b_out", 123, 596, 120, 60, 153, 416, 120, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vyhod_iz_tehniki_baa8)),
        AtlasButtonInfo("b_playmap", 515, 315, 60, 60, 505, 458, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_knopka_s_treugolnikom_df43)),
        AtlasButtonInfo("b_redo", 442, 504, 60, 60, 435, 0, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_povtorit_b8f7)),
        AtlasButtonInfo("b_save", 456, 63, 60, 60, 462, 252, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sohranit_74ea)),
        AtlasButtonInfo("b_spectators", 483, 567, 60, 60, 582, 63, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nablyudateli_29a5)),
        AtlasButtonInfo("b_undo", 519, 63, 60, 60, 515, 378, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.cancel)),
        AtlasButtonInfo("b_zoomin", 578, 378, 60, 60, 498, 0, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_priblizit_a2e1)),
        AtlasButtonInfo("b_zoomout", 525, 252, 60, 60, 519, 189, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_otdalit_a43f)),
        AtlasButtonInfo("bpb", 0, 371, 150, 78, 0, 452, 150, 78, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_siniy_dzhoystik_6a96)),
        AtlasButtonInfo("chan", 229, 0, 120, 60, 163, 149, 120, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_smena_oruzhiya_c044)),
        AtlasButtonInfo("chat", 456, 126, 60, 60, 456, 189, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_chat_4dde)),
        AtlasButtonInfo("d2_but", 349, 542, 90, 70, 276, 378, 90, 70, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_pustaya_oranzhevaya_knopka_5514)),
        AtlasButtonInfo("d3_but", 0, 0, 226, 40, 0, 43, 226, 40, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_dlinnaya_oranzhevaya_knopka_1e42)),
        AtlasButtonInfo("gift", 546, 584, 60, 60, 546, 521, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_podarok_6076)),
        AtlasButtonInfo("gr", 352, 0, 80, 60, 369, 378, 80, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_granata_d4eb)),
        AtlasButtonInfo("menubut", 0, 239, 160, 48, 0, 188, 160, 48, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_knopka_magazina_e670)),
        AtlasButtonInfo("pb_n", 0, 452, 150, 78, 0, 452, 150, 78, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nazhatie_dzhoystika_5c2f)),
        AtlasButtonInfo("rel", 0, 596, 120, 60, 0, 533, 120, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_perezaryadka_c3bb)),
        AtlasButtonInfo("row_bottom", 286, 63, 100, 100, 286, 166, 100, 100, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_strelka_vniz_4d82)),
        AtlasButtonInfo("row_top", 276, 275, 100, 100, 246, 542, 100, 100, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_strelka_vverh_1c98)),
        AtlasButtonInfo("rpb", 0, 290, 150, 78, 0, 452, 150, 78, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_krasnyy_dzhoystik_8c3d)),
        AtlasButtonInfo("s7_chan", 153, 353, 120, 60, 246, 479, 120, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_demonstratsiya_smeny_oruzhiya_759d)),
        AtlasButtonInfo("s7gr", 369, 441, 80, 60, 379, 269, 80, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_demonstratsiya_granaty_dba8)),
        AtlasButtonInfo("tabl", 609, 504, 60, 60, 588, 252, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_tab_igrokov_09e5)),
        AtlasButtonInfo("team", 582, 189, 60, 60, 582, 126, 60, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_smena_komandy_d9e0)),
        AtlasButtonInfo("use", 153, 290, 120, 60, 123, 533, 120, 60, skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_deystvie_podbor_0dbc))
    )

    fun get(id: String): AtlasButtonInfo? = buttons.find { it.id == id }
}

