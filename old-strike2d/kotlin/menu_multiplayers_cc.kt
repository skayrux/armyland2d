package skayrux.shootout2d

import android.content.Context
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.Crossfade
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.Canvas
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.util.zip.GZIPOutputStream
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.decodeFromByteArray
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.EditText
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import android.net.wifi.WifiManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown

val SessionOnlineMaps = mutableStateListOf<Pair<String, String>>()

enum class ServerPreJoinStatus {
    OK,
    SERVER_CLOSED,
    WRONG_PASSWORD,
    BANNED,
    FULL
}

// проверка статуса сервера перед входом с таймаутом 1.5 секунды
suspend fun checkGameServerStatus(
    serverIp: String,
    serverPort: Int,
    srvId: Int,
    tmpId: String,
    pass: String = ""
): ServerPreJoinStatus = withContext(Dispatchers.IO) {
    var socket: DatagramSocket? = null
    try {
        socket = DatagramSocket()
        socket.soTimeout = 750
        val baos = ByteArrayOutputStream()
        val daos = DataOutputStream(baos)
        daos.writeByte(-44)
        daos.writeInt(srvId)
        daos.writeInt(0)
        daos.writeUTF(tmpId)
        daos.writeUTF(pass.trim())
        daos.close()
        val bytes = baos.toByteArray()
        val address = InetAddress.getByName(serverIp)
        val packet = DatagramPacket(bytes, bytes.size, address, serverPort)

        val startTime = System.currentTimeMillis()
        var attempts = 0
        val recvBuf = ByteArray(64)
        while (attempts < 2 && System.currentTimeMillis() - startTime < 1500L) {
            try {
                val remaining = (1500L - (System.currentTimeMillis() - startTime)).toInt().coerceIn(200, 750)
                socket.soTimeout = remaining
                socket.send(packet)
                val recvPacket = DatagramPacket(recvBuf, recvBuf.size)
                socket.receive(recvPacket)
                val bais = ByteArrayInputStream(recvPacket.data, 0, recvPacket.length)
                val dais = DataInputStream(bais)
                val respType = dais.readByte()
                if (respType == (-45).toByte()) {
                    val isBanned = dais.readBoolean()
                    val isFull = if (dais.available() > 0) dais.readBoolean() else false
                    val isPassCorrect = if (dais.available() > 0) dais.readBoolean() else true
                    return@withContext when {
                        isBanned -> ServerPreJoinStatus.BANNED
                        isFull -> ServerPreJoinStatus.FULL
                        !isPassCorrect -> ServerPreJoinStatus.WRONG_PASSWORD
                        else -> ServerPreJoinStatus.OK
                    }
                }
            } catch (e: SocketTimeoutException) {
                attempts++
            }
        }
        ServerPreJoinStatus.SERVER_CLOSED
    } catch (e: Exception) {
        // таймаут или недоступность сокета
        ServerPreJoinStatus.SERVER_CLOSED
    } finally {
        try { socket?.close() } catch (ignored: Exception) {}
    }
}

suspend fun isGameServerPortReachable(host: String, port: Int): Boolean = withContext(Dispatchers.IO) {
    var socket: DatagramSocket? = null
    try {
        socket = DatagramSocket()
        socket.soTimeout = 1200
        val buf = byteArrayOf(-37)
        val address = InetAddress.getByName(host)
        val packet = DatagramPacket(buf, buf.size, address, port)
        socket.send(packet)
        true
    } catch (e: Exception) {
        false
    } finally {
        try { socket?.close() } catch (e: Exception) {}
    }
}

enum class ServerNoticeType {
    NONE,
    MAINTENANCE,
    LIMIT_SERVERS,
    LIMIT_BR,
    VERSION_MISMATCH,
    UNREACHABLE
}

data class ServerNoticeState(
    val type: ServerNoticeType,
    val title: String,
    val message: String
)

var globalServerNotice by mutableStateOf<ServerNoticeState?>(null)

@Composable
fun ServerStatusNoticeBanner(
    notice: ServerNoticeState?,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notice != null && notice.type != ServerNoticeType.UNREACHABLE,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        if (notice != null) {
            val (bgColor, borderColor, iconColor) = when (notice.type) {
                ServerNoticeType.MAINTENANCE -> Triple(Color(0xFF261D0C), Color(0xFFF59E0B), Color(0xFFFBBF24))
                ServerNoticeType.LIMIT_SERVERS -> Triple(Color(0xFF2B180A), Color(0xFFEA580C), Color(0xFFFB923C))
                ServerNoticeType.LIMIT_BR -> Triple(Color(0xFF22162B), Color(0xFFA855F7), Color(0xFFC084FC))
                ServerNoticeType.VERSION_MISMATCH -> Triple(Color(0xFF2C1010), Color(0xFFDC2626), Color(0xFFF87171))
                ServerNoticeType.UNREACHABLE -> Triple(Color(0xFF1E232A), Color(0xFF64748B), Color(0xFF94A3B8))
                ServerNoticeType.NONE -> Triple(Color(0xFF1A1A1A), Color(0xFF444444), Color(0xFFAAAAAA))
            }

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = notice.title,
                                color = iconColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = notice.message,
                                color = AppColors.TextGray,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            color = AppColors.TextDarkGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// защита от повторных нажатий кнопки играть при пинге
object MultiplayersLaunchGuard {
    @Volatile
    private var lastLaunchTime = 0L
    @Volatile
    private var isInFlight = false

    @Synchronized
    fun canLaunch(delayMs: Long = 3000L): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        if (isInFlight && now - lastLaunchTime < 15000L) {
            return false
        }
        if (now - lastLaunchTime < delayMs) {
            return false
        }
        lastLaunchTime = now
        isInFlight = true
        return true
    }

    @Synchronized
    fun release() {
        isInFlight = false
        lastLaunchTime = 0L
    }

    @Synchronized
    fun markLaunched() {
        isInFlight = false
        lastLaunchTime = android.os.SystemClock.elapsedRealtime()
    }
}





class BubbleLeftShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val arrowWidth = with(density) { 6.dp.toPx() }
        val arrowHeight = with(density) { 10.dp.toPx() }
        val path = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width - arrowWidth,
                    bottom = size.height,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )
            )
            moveTo(size.width, size.height / 2f)
            lineTo(size.width - arrowWidth, size.height / 2f - arrowHeight / 2f)
            lineTo(size.width - arrowWidth, size.height / 2f + arrowHeight / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun ShimmerPlayText(
    text: String,
    isShimmering: Boolean,
    enabled: Boolean,
    fontSize: androidx.compose.ui.unit.TextUnit = 14.sp,
    modifier: Modifier = Modifier
) {
    if (isShimmering) {
        val transition = rememberInfiniteTransition(label = "play_shimmer")
        val shimmerPos by transition.animateFloat(
            initialValue = -100f,
            targetValue = 220f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerPos"
        )
        val glowAlpha by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
        val shimmerBrush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                AppColors.White.copy(alpha = 0.2f * glowAlpha),
                AppColors.White.copy(alpha = glowAlpha),
                AppColors.White.copy(alpha = 0.2f * glowAlpha)
            ),
            start = androidx.compose.ui.geometry.Offset(shimmerPos, shimmerPos * 0.15f),
            end = androidx.compose.ui.geometry.Offset(shimmerPos + 80f, shimmerPos * 0.15f + 20f)
        )
        Text(
            text = text,
            style = androidx.compose.ui.text.TextStyle(
                brush = shimmerBrush,
                fontSize = fontSize,
                fontWeight = FontWeight.Normal
            ),
            modifier = modifier
        )
    } else {
        Text(
            text = text,
            color = if (enabled) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
            fontSize = fontSize,
            modifier = modifier
        )
    }
}

@Composable
fun GameButton(
    text: String? = null,
    enabled: Boolean = true,
    containerColor: Color = AppColors.ActionButtonBg,
    borderColor: Color = AppColors.ActionButtonBorder,
    disabledContainerColor: Color = AppColors.ActionButtonDisabledBg,
    disabledBorderColor: Color = AppColors.ActionButtonDisabledBorder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clip(AppShapes.Button)
            .background(if (enabled) containerColor else disabledContainerColor)
            .border(1.dp, if (enabled) borderColor else disabledBorderColor, AppShapes.Button)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (text != null) {
            Text(
                text = text,
                color = if (enabled) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        if (content != null) {
            content()
        }
    }
}

@Composable
fun RepeatingButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown()
                    currentOnClick()
                    val job = scope.launch {
                        delay(300)
                        while (true) {
                            delay(80)
                            currentOnClick()
                        }
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                }
            }
        },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun SpinnerInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    minVal: Int = 0,
    maxVal: Int = 20
) {
    val currentValState = rememberUpdatedState(value)
    val minValState = rememberUpdatedState(minVal)
    val maxValState = rememberUpdatedState(maxVal)
    val onValueChangeState = rememberUpdatedState(onValueChange)

    Row(
        modifier = modifier
            .background(AppColors.PanelDark, RoundedCornerShape(4.dp))
            .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RepeatingButton(
            onClick = {
                if (value > minVal) onValueChange((value - 1).coerceIn(minVal, maxVal))
            },
            modifier = Modifier
                .width(33.dp)
                .fillMaxHeight()
        ) {
            Text("<", color = if (value > minVal) AppColors.White else AppColors.TextPlaceholder, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    EditText(ctx).apply {
                        setTextColor(android.graphics.Color.WHITE)
                        setHintTextColor(android.graphics.Color.parseColor("#808080"))
                        background = null
                        setPadding(0, 0, 0, 0)
                        textSize = 14.2f
                        setSingleLine(true)
                        maxLines = 1
                        gravity = Gravity.CENTER
                        inputType = android.text.InputType.TYPE_CLASS_NUMBER
                        imeOptions = EditorInfo.IME_ACTION_DONE
                        setText(currentValState.value.toString())
                        setOnEditorActionListener { v, actionId, _ ->
                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.hideSoftInputFromWindow(v.windowToken, 0)
                                v.clearFocus()
                                true
                            } else false
                        }
                        setOnFocusChangeListener { v, hasFocus ->
                            if (!hasFocus) {
                                val curParsed = text?.toString()?.toIntOrNull() ?: minValState.value
                                val clamped = curParsed.coerceIn(minValState.value, maxValState.value)
                                if (curParsed != clamped || text.toString() != clamped.toString()) {
                                    setText(clamped.toString())
                                }
                                onValueChangeState.value(clamped)
                            }
                        }
                        addTextChangedListener(object : android.text.TextWatcher {
                            private var isUpdating = false
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(editable: android.text.Editable?) {
                                if (isUpdating) return
                                val currentText = editable?.toString() ?: ""
                                val digitsOnly = currentText.filter { it.isDigit() }
                                val parsed = digitsOnly.toIntOrNull()
                                val min = minValState.value
                                val max = maxValState.value
                                val targetText = when {
                                    digitsOnly.isEmpty() -> ""
                                    parsed != null && parsed > max -> max.toString()
                                    else -> digitsOnly
                                }
                                if (targetText != currentText) {
                                    isUpdating = true
                                    setText(targetText)
                                    setSelection(targetText.length)
                                    isUpdating = false
                                }
                                val finalVal = targetText.toIntOrNull() ?: min
                                if (finalVal != currentValState.value) {
                                    onValueChangeState.value(finalVal.coerceIn(min, max))
                                }
                            }
                        })
                    }
                },
                update = { editText ->
                    val curText = editText.text.toString()
                    val targetText = value.toString()
                    if (curText != targetText && !editText.hasFocus()) {
                        editText.setText(targetText)
                        editText.setSelection(targetText.length)
                    }
                }
            )
        }
        RepeatingButton(
            onClick = {
                if (value < maxVal) onValueChange((value + 1).coerceIn(minVal, maxVal))
            },
            modifier = Modifier
                .width(33.dp)
                .fillMaxHeight()
        ) {
            Text(">", color = if (value < maxVal) AppColors.White else AppColors.TextPlaceholder, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CustomDropdown(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = expanded
    var itemWidth by remember { mutableIntStateOf(0) }

    val boxShape = if (expanded) RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 0.dp, bottomEnd = 0.dp) else RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .height(38.5.dp)
            .onSizeChanged { size -> itemWidth = size.width }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.TabInactive, boxShape)
                .border(1.dp, AppColors.Border, boxShape)
                .clickable { expanded = !expanded }
        ) {
            Text(
                text = selectedItem,
                color = AppColors.White,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 10.dp, end = 30.dp)
                    .align(Alignment.CenterStart)
            )
            Text(
                text = if (expanded) "▲" else "▼", 
                color = AppColors.TextDarkGray, 
                fontSize = 9.sp, 
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
            )
        }
        
        if (expanded || transitionState.currentState) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(LocalDensity.current) { 39.dp.roundToPx() }),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true)
            ) {
                Box(modifier = Modifier.width(with(LocalDensity.current) { itemWidth.toDp() })) {
                    AnimatedVisibility(
                        visibleState = transitionState,
                        enter = fadeIn(tween(300, easing = LinearOutSlowInEasing)) + expandVertically(tween(300, easing = LinearOutSlowInEasing), expandFrom = Alignment.Top),
                        exit = fadeOut(tween(250, easing = FastOutLinearInEasing)) + shrinkVertically(tween(250, easing = FastOutLinearInEasing), shrinkTowards = Alignment.Top)
                    ) {
                        val popupShape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 175.dp)
                                .background(AppColors.DropdownBg, popupShape)
                                .border(1.dp, AppColors.Border, popupShape)
                                .verticalScroll(rememberScrollState())
                        ) {
                            items.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onItemSelected(item)
                                            expanded = false
                                        }
                                        .background(if (selectedItem == item) AppColors.Panel else AppColors.Transparent)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = item,
                                        color = if (selectedItem == item) AppColors.White else AppColors.TextGray,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiplayerTabButton(
    text: String,
    isActive: Boolean,
    width: androidx.compose.ui.unit.Dp? = 135.dp,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) AppColors.TabActive else AppColors.PanelDark
    val borderColor = if (isActive) AppColors.BorderActive else AppColors.Border
    val textColor = AppColors.White

    Box(
        modifier = Modifier
            .height(30.dp)
            .then(if (width != null) Modifier.width(width) else Modifier.padding(horizontal = 14.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .then(if (width != null) Modifier.padding(horizontal = 4.dp) else Modifier),
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
fun create_game_menu(
    accountId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("online_servers") }
    var hideTopBar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val saved = AppSettings.getString(context, "host_initial_tab", "")
        if (saved.isNotEmpty()) {
            activeTab = if (saved == "local_servers") "online_servers" else saved
            AppSettings.setString(context, "host_initial_tab", "online_servers")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
    ) {
        if (!hideTopBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .height(30.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MultiplayerTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_onlayn_servera_330b), activeTab == "online_servers") { activeTab = "online_servers" }
                    MultiplayerTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_onlayn_igra_68b1), activeTab == "online_game") { activeTab = "online_game" }
                    MultiplayerTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_lokalnaya_igra_b3c0), activeTab == "local_game") { activeTab = "local_game" }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (activeTab != "online_servers") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppColors.Overlay10)
                )
                Spacer(Modifier.height(10.dp))
            }

            if (activeTab != "local_game") {
                ServerStatusNoticeBanner(
                    notice = globalServerNotice,
                    onDismiss = { globalServerNotice = null }
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f)
        ) {
            when (activeTab) {
                "online_servers" -> {
                    connect_game_panel(
                        isOnline = true,
                        accountId = accountId,
                        onBack = onBack
                    )
                }
                "online_game" -> {
                    create_game_settings(
                        accountId = accountId,
                        isOnline = true,
                        onHideTopBar = { hideTopBar = it },
                        onBack = onBack
                    )
                }
                "local_game" -> {
                    create_game_settings(
                        accountId = accountId,
                        isOnline = false,
                        onHideTopBar = { hideTopBar = it },
                        onBack = onBack
                    )
                }
            }
        }
    }
}

@Composable
fun LocalMapsActionButton(text: String, enabled: Boolean = true, onClick: () -> Unit = {}) {
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



@Composable
fun LocalMapsTabButton(text: String, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .width(110.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) AppColors.TabActive else AppColors.PanelDark)
            .border(1.dp, if (isActive) AppColors.BorderActive else AppColors.Border, RoundedCornerShape(4.dp))
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

@Composable
fun LocalMapsPanel(
    selectedTypeId: Int = 1,
    isOnline: Boolean = false,
    onMapSelected: (String, String) -> Unit,
    onShowToast: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var maps by remember { mutableStateOf<List<MapMetadata>>(emptyList()) }
    val accountId = sys_menu_socket_repository.currentPlayerId
    var selectedUid by remember { mutableStateOf<String?>(null) }
    var mapToDelete by remember { mutableStateOf<String?>(null) }
    var activeTab by remember { mutableStateOf("downloaded") }

    val officialMaps = remember(context, selectedTypeId) {
        BaseGamemodeMultiplayers.getOfficialMapsList(context, selectedTypeId)
    }

    fun refresh() {
        maps = MapSavior.listMapMetadata(context, accountId)
    }

    LaunchedEffect(accountId) {
        refresh()
    }

    if (mapToDelete != null) {
        val deleteMapMetadata = maps.find { it.uid == mapToDelete }
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.removal),
            content = { Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete_map_confirm, deleteMapMetadata?.name ?: mapToDelete.toString()), color = AppColors.TextMuted, fontSize = 14.sp) },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.yes),
            onConfirm = {
                val uid = mapToDelete ?: ""
                MapSavior.deleteMap(context, uid)
                if (selectedUid == uid) selectedUid = null
                mapToDelete = null
                refresh()
            },
            onDismiss = { mapToDelete = null }
        )
    }
    
    val displayMaps = remember(maps, activeTab, accountId, isOnline) {
        if (isOnline) {
            emptyList()
        } else {
            maps.filter {
                val isMyMap = MapSavior.isUserOwnMap(it, accountId)
                when (activeTab) {
                    "downloaded" -> it.isDownloaded || (!isMyMap && it.creatorSrvId != 0)
                    "my" -> isMyMap
                    else -> false
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .height(30.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LocalMapsTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_skachannye_1ced), activeTab == "downloaded") { 
                activeTab = "downloaded" 
                selectedUid = null
            }
            LocalMapsTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.my_maps), activeTab == "my") { 
                activeTab = "my" 
                selectedUid = null
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppColors.Overlay10)
        )
        
        Spacer(Modifier.height(10.dp))

        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(1.dp, AppColors.Divider)
            ) {
                if (displayMaps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_kart_da30), color = AppColors.EmptyText, fontSize = 18.sp)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    ) {
                        items(displayMaps.size) { index ->
                            val mMetadata = displayMaps[index]
                            val mUid = mMetadata.uid
                            val mName = mMetadata.name
                            val isSel = selectedUid == mUid
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedUid = mUid }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSel) AppColors.TabActive else AppColors.Transparent)
                                        .border(1.dp, if (isSel) AppColors.BorderActive else Color.Transparent)
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mName,
                                        color = AppColors.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    
                                    Text(
                                        text = "${mMetadata.width}x${mMetadata.height}",
                                        color = if (isSel) AppColors.White.copy(alpha = 0.85f) else AppColors.TextMuted,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                    
                                    val dateStr = try {
                                        val millis = mMetadata.timestamp.toLongOrNull()?.takeIf { it > 1000000000000L } ?: mMetadata.lastModified
                                        val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).apply {
                                            timeZone = TimeZone.getTimeZone("Europe/Moscow")
                                        }
                                        sdf.format(Date(millis))
                                    } catch (e: Exception) { "" }
                                    
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

            Column(
                modifier = Modifier.width(150.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LocalMapsActionButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.select_map1),
                    enabled = selectedUid != null,
                    onClick = {
                        selectedUid?.let { uid ->
                            if (selectedTypeId == 12) {
                                val meta = maps.find { it.uid == uid }
                                val isSuitable = if (uid.startsWith("official_")) {
                                    val id = uid.removePrefix("official_").toIntOrNull()
                                    id == -1
                                } else if (meta != null) {
                                    meta.width >= BaseGamemodeMultiplayers.MIN_BR_MAP_W && meta.height >= BaseGamemodeMultiplayers.MIN_BR_MAP_H
                                } else {
                                    BaseGamemodeMultiplayers.isMapSuitableForMode(context, selectedTypeId, uid)
                                }
                                if (!isSuitable) {
                                    onShowToast(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_minimalnyy_razmer_karty_dolzhe_6edc))
                                    return@LocalMapsActionButton
                                }
                            }
                            if (isOnline && !uid.startsWith("official_") && !MapSavior.isMapPublished(context, uid)) {
                                onShowToast(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_karta_ne_opublikovana_na_serve_8e57))
                                return@LocalMapsActionButton
                            }
                            val name = if (uid.startsWith("official_")) {
                                officialMaps.find { it.first == uid }?.second ?: uid
                            } else {
                                maps.find { it.uid == uid }?.name ?: "Unknown"
                            }
                            onMapSelected(uid, name)
                        }
                    }
                )
                if (activeTab != "official") {
                    LocalMapsActionButton(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete_map),
                        enabled = selectedUid != null,
                        onClick = {
                            if (selectedUid != null) {
                                mapToDelete = selectedUid
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.TabInactive)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back),
                        color = AppColors.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LikeIcon(modifier: Modifier = Modifier, color: Color = AppColors.Success, filled: Boolean = false) {
    Icon(
        imageVector = if (filled) {
            Icons.Filled.ThumbUp
        } else {
            Icons.Outlined.ThumbUp
        },
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}

@Composable
fun DislikeIcon(modifier: Modifier = Modifier, color: Color = AppColors.RedMaterial, filled: Boolean = false) {
    Icon(
        imageVector = if (filled) {
            Icons.Filled.ThumbDown
        } else {
            Icons.Outlined.ThumbDown
        },
        contentDescription = null,
        modifier = modifier,
        tint = color
    )
}

fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> {
            val formatted = count / 1_000_000.0
            val s = String.format(Locale.US, "%.1f", formatted)
            val clean = if (s.endsWith(".0")) s.substringBefore(".0") else s
            clean.replace(".", ",") + "M"
        }
        count >= 1_000 -> {
            val formatted = count / 1_000.0
            val s = String.format(Locale.US, "%.1f", formatted)
            val clean = if (s.endsWith(".0")) s.substringBefore(".0") else s
            clean.replace(".", ",") + "K"
        }
        else -> count.toString()
    }
}

data class OnlineMapMetadata(
    val uid: String,
    val name: String,
    val width: Int,
    val height: Int,
    val uploadDate: String,
    val updateDate: String,
    val authorId: String,
    val likes: Int,
    val dislikes: Int,
    val authorNick: String = "N/A",
    val displayId: Int = 0,
    val lastModified: Long = 0L
)

@Composable
fun CompactSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = AppColors.White,
            fontSize = 15.5.sp
        ),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.White),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.InputContainer)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, color = AppColors.TextPlaceholder, fontSize = 15.5.sp, maxLines = 1)
                }
                innerTextField()
            }
        },
        modifier = modifier.height(38.dp)
    )
}

@Composable
fun OnlineMapsPanel(
    selectedTypeId: Int = 1,
    onMapSelected: (String, String) -> Unit,
    onShowToast: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("popular") }
    var selectedUid by remember { mutableStateOf<String?>(null) }
    var showMapInfoId by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    
    var searchName by remember { mutableStateOf("") }
    var searchMapId by remember { mutableStateOf("") }
    var searchAuthorId by remember { mutableStateOf("") }
    var appliedSearchName by remember { mutableStateOf("") }
    var appliedSearchMapId by remember { mutableStateOf("") }
    var appliedSearchAuthorId by remember { mutableStateOf("") }
    var isSearchingAnimation by remember { mutableStateOf(false) }
    val userRatings = remember { mutableStateMapOf<String, Int>() }
    val serverRatings = remember { mutableStateMapOf<String, Int>() }
    
    val onlineMaps = remember { mutableStateListOf<OnlineMapMetadata>() }
    
    var lastRefreshTime by remember { mutableStateOf(0L) }

    fun parseMapsData(res: String): List<OnlineMapMetadata> {
        if (!res.startsWith("MAPS_LIST|")) return emptyList()
        val data = res.substringAfter("MAPS_LIST|").split("|").filter { it.isNotBlank() }
        val newMaps = mutableListOf<OnlineMapMetadata>()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Europe/Moscow")
        }
        val outSdfShort = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Europe/Moscow")
        }
        val outSdfLong = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Europe/Moscow")
        }
        for (d in data) {
            val p = d.split(",")
            if (p.size >= 9) {
                val mapId = p[0]
                val authorId = p[1]
                val name = p[2]
                val width = p[3].toIntOrNull() ?: 128
                val height = p[4].toIntOrNull() ?: 128
                val uploadDateStr = p[5]
                val updateDateStr = p[6]
                val likes = p[7].toIntOrNull() ?: 0
                val dislikes = p[8].toIntOrNull() ?: 0
                val authorNick = if (p.size >= 10) p[9] else "N/A"
                val displayId = if (p.size >= 11) p[10].toIntOrNull() ?: 0 else 0
                
                val uploadFmt = try {
                    val d = sdf.parse(uploadDateStr)
                    if (d != null) outSdfShort.format(d) else uploadDateStr
                } catch(e: Exception) { uploadDateStr }
                
                val updateFmt = if (updateDateStr == uploadDateStr) "—" else try {
                    val d = sdf.parse(updateDateStr)
                    if (d != null) outSdfLong.format(d) else updateDateStr
                } catch(e: Exception) { updateDateStr }
                
                val rawUpload = try { sdf.parse(uploadDateStr)?.time ?: 0L } catch(e: Exception) { 0L }
                val rawUpdate = try { sdf.parse(updateDateStr)?.time ?: 0L } catch(e: Exception) { 0L }
                val lastMod = if (rawUpdate > 0L) rawUpdate else rawUpload
                
                newMaps.add(OnlineMapMetadata(mapId, name, width, height, uploadFmt, updateFmt, authorId, likes, dislikes, authorNick, displayId, lastMod))
            }
        }
        return newMaps
    }
    
    fun fetchMaps() {
        if (isRefreshing) return
        val now = System.currentTimeMillis()
        if (now - lastRefreshTime < 300) {
            return
        }
        isRefreshing = true
        lastRefreshTime = now
        val pid = sys_menu_socket_repository.currentPlayerId
        scope.launch(Dispatchers.IO) {
            val res = sys_menu_socket_repository.getMapsList()
            val ratings = sys_menu_socket_repository.getUserMapRatings(pid)
            withContext(Dispatchers.Main) {
                if (res.startsWith("MAPS_LIST|")) {
                    val newMaps = parseMapsData(res)
                    onlineMaps.clear()
                    onlineMaps.addAll(newMaps)
                    newMaps.forEach { map ->
                        MapSavior.setMapPublished(context, map.uid, true)
                    }
                    userRatings.clear()
                    userRatings.putAll(ratings)
                    serverRatings.clear()
                    serverRatings.putAll(ratings)
                    if (sys_menu_socket_repository.currentPlayerScore < 15000) {
                        onlineMaps.forEach { map ->
                            val localVote = AppSettings.getInt(context, "local_rating_${map.uid}", 0)
                            if (localVote != 0) {
                                userRatings[map.uid] = localVote
                            }
                        }
                    }
                } else if (res == "OFFLINE") {
                    onlineMaps.clear()
                    userRatings.clear()
                    serverRatings.clear()
                }
                isRefreshing = false
            }
        }
    }

    fun performSearch(name: String, mapId: String, author: String) {
        isRefreshing = true
        val pid = sys_menu_socket_repository.currentPlayerId
        scope.launch(Dispatchers.IO) {
            val res = if (name.isBlank() && mapId.isBlank() && author.isBlank()) {
                sys_menu_socket_repository.getMapsList()
            } else {
                sys_menu_socket_repository.searchMaps(name, mapId, author)
            }
            val ratings = sys_menu_socket_repository.getUserMapRatings(pid)
            withContext(Dispatchers.Main) {
                if (res.startsWith("MAPS_LIST|")) {
                    val newMaps = parseMapsData(res)
                    onlineMaps.clear()
                    onlineMaps.addAll(newMaps)
                    newMaps.forEach { map ->
                        MapSavior.setMapPublished(context, map.uid, true)
                    }
                    userRatings.clear()
                    userRatings.putAll(ratings)
                    serverRatings.clear()
                    serverRatings.putAll(ratings)
                } else if (res == "OFFLINE") {
                    onlineMaps.clear()
                    userRatings.clear()
                    serverRatings.clear()
                }
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchMaps()
    }

    val sortedAndFilteredMaps = remember(onlineMaps.toList(), activeTab, appliedSearchName, appliedSearchMapId, appliedSearchAuthorId) {
        when (activeTab) {
            "popular" -> onlineMaps.sortedByDescending { it.likes }.take(100)
            "new" -> onlineMaps.sortedByDescending { it.lastModified }.take(100)
            "search" -> {
                val qName = appliedSearchName.trim()
                val qMapId = appliedSearchMapId.trim()
                val qAuthor = appliedSearchAuthorId.trim()
                if (qName.isBlank() && qMapId.isBlank() && qAuthor.isBlank()) {
                    onlineMaps.take(100)
                } else {
                    onlineMaps.filter { map ->
                        val matchName = qName.isBlank() || map.name.contains(qName, ignoreCase = true)
                        val matchUid = if (qMapId.isBlank()) true else {
                            val num = qMapId.toIntOrNull()
                            if (num != null) {
                                map.displayId == num || map.uid == qMapId
                            } else {
                                map.uid.equals(qMapId, ignoreCase = true) || map.uid.startsWith(qMapId, ignoreCase = true)
                            }
                        }
                        val matchAuthor = if (qAuthor.isBlank()) true else {
                            map.authorId == qAuthor || map.authorNick.equals(qAuthor, ignoreCase = true) || map.authorNick.contains(qAuthor, ignoreCase = true)
                        }
                        matchName && matchUid && matchAuthor
                    }.take(100)
                }
            }
            else -> onlineMaps.take(100)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (activeTab == "search") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MultiplayerTabButton(
                    text = "»",
                    isActive = false,
                    width = 32.dp,
                    onClick = {
                        activeTab = "popular"
                    }
                )

                CompactSearchTextField(
                    value = searchName,
                    onValueChange = { input ->
                        val clean = input.replace("\n", "")
                        if (clean.length <= 25) {
                            searchName = clean
                        }
                    },
                    placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nazvanie_78e6),
                    modifier = Modifier.weight(1f)
                )

                CompactSearchTextField(
                    value = searchMapId,
                    onValueChange = { input ->
                        val clean = input.replace("\n", "").trim()
                        if (clean.length <= 25) {
                            searchMapId = clean
                        }
                    },
                    placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_id_karty_9ee1),
                    modifier = Modifier.weight(1f)
                )

                CompactSearchTextField(
                    value = searchAuthorId,
                    onValueChange = { input ->
                        val clean = input.replace("\n", "").trim()
                        if (clean.length <= 25) {
                            searchAuthorId = clean
                        }
                    },
                    placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_id_avtora_8dda),
                    modifier = Modifier.weight(1f)
                )

                MultiplayerTabButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.find),
                    isActive = false,
                    width = 75.dp,
                    onClick = {
                        if (!isSearchingAnimation) {
                            scope.launch {
                                isSearchingAnimation = true
                                appliedSearchName = searchName.trim()
                                appliedSearchMapId = searchMapId.trim()
                                appliedSearchAuthorId = searchAuthorId.trim()
                                performSearch(appliedSearchName, appliedSearchMapId, appliedSearchAuthorId)
                                isSearchingAnimation = false
                            }
                        }
                    }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .height(30.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MultiplayerTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.popular_maps), activeTab == "popular") { 
                    activeTab = "popular" 
                    fetchMaps()
                }
                MultiplayerTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.new_maps), activeTab == "new") { 
                    activeTab = "new" 
                    fetchMaps()
                }
                MultiplayerTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.find_map), activeTab == "search") {
                    activeTab = "search"
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.PanelDark)
                .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 7.4.dp, bottom = 7.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map1), color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.4f))
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.size), color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center)
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.last_update), color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.author), color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center)
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_otsenka_45b3), color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(1.4f), textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_deystvie_50a7), color = AppColors.TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppColors.Overlay10)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
            if (isRefreshing || isSearchingAnimation) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(3) {
                        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.08f,
                            targetValue = 0.22f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                animation = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.LinearEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.ShadowBase.copy(alpha = alpha))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1.4f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(0.7f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1.1f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1.1f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1.4f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(0.7f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(2.dp)))
                        }
                    }
                }
            } else if (sortedAndFilteredMaps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (activeTab == "search") skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.nothing_found) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.no_custom_maps),
                        color = AppColors.EmptyText,
                        fontSize = 18.sp
                    )
                }
            } else {
                @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
                CompositionLocalProvider(
                    androidx.compose.foundation.LocalOverscrollConfiguration provides null
                ) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedAndFilteredMaps.size) { index ->
                            val mMetadata = sortedAndFilteredMaps[index]
                            val mUid = mMetadata.uid
                            val mName = mMetadata.name
                            val isSel = selectedUid == mUid
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { selectedUid = mUid }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSel) AppColors.TabActive else AppColors.Transparent)
                                        .border(1.dp, if (isSel) AppColors.BorderActive else Color.Transparent)
                                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mName,
                                        color = AppColors.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    
                                    Text(
                                        text = "${mMetadata.width}x${mMetadata.height}",
                                        color = if (isSel) AppColors.White.copy(alpha = 0.85f) else AppColors.TextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(0.7f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Text(
                                        text = mMetadata.updateDate,
                                        color = if (isSel) AppColors.White.copy(alpha = 0.85f) else AppColors.TextMuted,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1.1f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Text(
                                        text = mMetadata.authorNick,
                                        color = if (isSel) AppColors.White.copy(alpha = 0.9f) else AppColors.TextGray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1.1f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Row(
                                        modifier = Modifier
                                            .weight(1.4f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (isSel) {
                                                        val score = sys_menu_socket_repository.currentPlayerScore
                                                        val hasVotedLocally = if (score < 15000) {
                                                             AppSettings.getInt(context, "local_rating_$mUid", 0) != 0
                                                        } else false
                                                        val hasVotedOnServer = serverRatings[mUid] != null && serverRatings[mUid] != 0
                                                        val isBlocked = score < 15000 && hasVotedLocally && !hasVotedOnServer
                                                        val pid = sys_menu_socket_repository.currentPlayerId
                                                        val currentRating = userRatings[mUid] ?: 0
                                                        
                                                        // лайк ставится один раз или меняется с дизлайка
                                                        if (!isBlocked && pid.isNotBlank() && currentRating != 1) {
                                                            val newRating = 1
                                                            userRatings[mUid] = newRating
                                                            serverRatings[mUid] = newRating
                                                            if (score < 15000) {
                                                                AppSettings.setInt(context, "local_rating_$mUid", newRating)
                                                            }
                                                            val idx = onlineMaps.indexOfFirst { it.uid == mUid }
                                                            if (idx != -1) {
                                                                val map = onlineMaps[idx]
                                                                val diffLikes = 1
                                                                val diffDislikes = if (currentRating == -1) -1 else 0
                                                                onlineMaps[idx] = map.copy(
                                                                    likes = (map.likes + diffLikes).coerceAtLeast(0),
                                                                    dislikes = (map.dislikes + diffDislikes).coerceAtLeast(0)
                                                                )
                                                            }
                                                            scope.launch(Dispatchers.IO) {
                                                                sys_menu_socket_repository.rateMap(pid, mUid, newRating)
                                                            }
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            LikeIcon(
                                                modifier = Modifier.size(14.dp),
                                                color = AppColors.OnlineGreen,
                                                filled = (userRatings[mUid] == 1)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = formatCount(mMetadata.likes),
                                                color = AppColors.OnlineGreen,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Row(
                                            modifier = Modifier
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    if (isSel) {
                                                        val score = sys_menu_socket_repository.currentPlayerScore
                                                        val hasVotedLocally = if (score < 15000) {
                                                            AppSettings.getInt(context, "local_rating_$mUid", 0) != 0
                                                        } else false
                                                        val hasVotedOnServer = serverRatings[mUid] != null && serverRatings[mUid] != 0
                                                        val isBlocked = score < 15000 && hasVotedLocally && !hasVotedOnServer
                                                        val pid = sys_menu_socket_repository.currentPlayerId
                                                        val currentRating = userRatings[mUid] ?: 0
                                                        
                                                        // дизлайк ставится один раз или меняется с лайка
                                                        if (!isBlocked && pid.isNotBlank() && currentRating != -1) {
                                                            val newRating = -1
                                                            userRatings[mUid] = newRating
                                                            serverRatings[mUid] = newRating
                                                            if (score < 15000) {
                                                                AppSettings.setInt(context, "local_rating_$mUid", newRating)
                                                            }
                                                            val idx = onlineMaps.indexOfFirst { it.uid == mUid }
                                                            if (idx != -1) {
                                                                val map = onlineMaps[idx]
                                                                val diffDislikes = 1
                                                                val diffLikes = if (currentRating == 1) -1 else 0
                                                                onlineMaps[idx] = map.copy(
                                                                    dislikes = (map.dislikes + diffDislikes).coerceAtLeast(0),
                                                                    likes = (map.likes + diffLikes).coerceAtLeast(0)
                                                                )
                                                            }
                                                            scope.launch(Dispatchers.IO) {
                                                                sys_menu_socket_repository.rateMap(pid, mUid, newRating)
                                                            }
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            DislikeIcon(
                                                modifier = Modifier.size(14.dp),
                                                color = AppColors.DangerRed,
                                                filled = (userRatings[mUid] == -1)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = formatCount(mMetadata.dislikes),
                                                color = AppColors.DangerRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier.weight(0.7f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = null
                                                ) {
                                                    showMapInfoId = mUid
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Outlined.Info,
                                                contentDescription = null,
                                                tint = AppColors.White.copy(alpha = 0.75f),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            if (showMapInfoId == mUid) {
                                                val density = androidx.compose.ui.platform.LocalDensity.current
                                                val positionProvider = remember {
                                                    object : androidx.compose.ui.window.PopupPositionProvider {
                                                        override fun calculatePosition(
                                                            anchorBounds: androidx.compose.ui.unit.IntRect,
                                                            windowSize: androidx.compose.ui.unit.IntSize,
                                                            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                                            popupContentSize: androidx.compose.ui.unit.IntSize
                                                        ): androidx.compose.ui.unit.IntOffset {
                                                            val overlapPx = with(density) { 2.dp.toPx() }.toInt()
                                                            val x = anchorBounds.left - popupContentSize.width + overlapPx
                                                            val y = anchorBounds.top + (anchorBounds.height - popupContentSize.height) / 2
                                                            return androidx.compose.ui.unit.IntOffset(x, y)
                                                        }
                                                    }
                                                }
                                                Popup(
                                                    onDismissRequest = { showMapInfoId = null },
                                                    properties = PopupProperties(focusable = true, dismissOnClickOutside = true),
                                                    popupPositionProvider = positionProvider
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .wrapContentWidth()
                                                            .height(32.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .matchParentSize()
                                                                .background(AppColors.Panel2A.copy(alpha = 0.9f), BubbleLeftShape())
                                                                .border(1.dp, AppColors.Border, BubbleLeftShape())
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .wrapContentWidth()
                                                                .height(32.dp)
                                                                .padding(start = 12.dp, end = 18.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            val mapDisplayIdStr = if (mMetadata.displayId > 0) mMetadata.displayId.toString() else mMetadata.uid
                                                            Text(
                                                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map_info_string, mapDisplayIdStr, mMetadata.authorId.toString(), mMetadata.uploadDate.toString()),
                                                                color = AppColors.White,
                                                                fontSize = 11.sp,
                                                                maxLines = 1,
                                                                textAlign = TextAlign.Start
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                        .height(1.dp)
                                        .background(AppColors.Border)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back),
                    color = AppColors.White,
                    fontSize = 14.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                    .clickable {
                        if (activeTab != "search") {
                            searchName = ""
                            searchMapId = ""
                            searchAuthorId = ""
                        }
                        fetchMaps()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.refresh),
                    color = AppColors.White,
                    fontSize = 14.sp
                )
            }

            val isSelectEnabled = selectedUid != null && !isDownloading
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .menuActionButtonStyle(enabled = isSelectEnabled, shape = RoundedCornerShape(4.dp))
                    .clickable(enabled = isSelectEnabled) {
                        selectedUid?.let { uid ->
                            val mapObj = onlineMaps.find { it.uid == uid }
                            if (mapObj != null) {
                                if (selectedTypeId == 12 && (mapObj.width < BaseGamemodeMultiplayers.MIN_BR_MAP_W || mapObj.height < BaseGamemodeMultiplayers.MIN_BR_MAP_H)) {
                                    onShowToast(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_minimalnyy_razmer_karty_dolzhe_6edc))
                                    return@clickable
                                }
                                isDownloading = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val fMap = File(context.filesDir, "${uid}.map")
                                        if (!fMap.exists() || fMap.length() == 0L) {
                                            val res = sys_menu_socket_repository.getMapData(uid)
                                            if (res.startsWith("MAP_DATA_OK|")) {
                                                val base64 = res.substringAfter("MAP_DATA_OK|")
                                                MapSavior.unpackMapData(context, uid, base64)
                                                MapSavior.markMapAsDownloaded(context, uid)
                                            }
                                        } else {
                                            MapSavior.markMapAsDownloaded(context, uid)
                                        }
                                        withContext(Dispatchers.Main) {
                                            isDownloading = false
                                            onMapSelected(uid, mapObj.name)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        withContext(Dispatchers.Main) { isDownloading = false }
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDownloading) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.loading) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vybrat_2b02),
                    color = if (isSelectEnabled) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun SettingsRow(
    label: String,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = AppColors.TextGray,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.4f)
        )
        Row(
            modifier = Modifier.weight(0.6f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun BotSettings(
    label: String,
    bots: Int,
    onBotsChange: (Int) -> Unit,
    botsDiff: String,
    onBotsDiffChange: (String) -> Unit,
    maxBots: Int = 28
) {
    val context = LocalContext.current
    val diffOptions = remember(context) { BaseGamemodeMultiplayers.getBotDifficulties(context) }
    SettingsRow(label = label) {
        SpinnerInput(
            value = bots.coerceIn(0, maxBots),
            onValueChange = onBotsChange,
            modifier = Modifier.width(102.dp).height(38.5.dp),
            minVal = 0,
            maxVal = maxBots
        )
        CustomDropdown(
            items = diffOptions,
            selectedItem = botsDiff,
            onItemSelected = onBotsDiffChange,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun create_game_settings(accountId: String, isOnline: Boolean = false, onHideTopBar: (Boolean) -> Unit = {}, onBack: () -> Unit) {
    var currentSubScreen by remember { mutableStateOf("settings") }
    val context = LocalContext.current
    var isLaunching by remember { mutableStateOf(false) }
    var lastLaunchTime by remember { mutableLongStateOf(0L) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isLaunching = false
                MultiplayersLaunchGuard.release()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            onHideTopBar(false)
        }
    }

    val gameModeItems = remember(context) { BaseGamemodeMultiplayers.getGameModeItems(context) }
    val gameModeNames = remember(gameModeItems) { gameModeItems.map { it.name } }

    // начальные настройки сервера
    val defaultInitialized = remember {
        val inited = AppSettings.getBoolean(context, "server_create_defaults_v2", false)
        if (!inited) {
            AppSettings.setInt(context, "settings_selected_type_id", 6)
            AppSettings.setString(context, "settings_selected_type", BaseGamemodeMultiplayers.getGameModeName(context, 6))
            AppSettings.setInt(context, "settings_red_bots", 10)
            AppSettings.setInt(context, "settings_blue_bots", 10)
            AppSettings.setInt(context, "settings_red_bots_diff_idx", 1)
            AppSettings.setInt(context, "settings_blue_bots_diff_idx", 1)
            AppSettings.setString(context, "settings_selected_map_uid", "official_9")
            AppSettings.setString(context, "settings_selected_map_name", BaseGamemodeMultiplayers.getOfficialMapName(context, 9))
            AppSettings.setBoolean(context, "server_create_defaults_v2", true)
        }
        true
    }

    var selectedTypeId by remember {
        val savedId = AppSettings.getInt(context, "settings_selected_type_id", 6)
        if (savedId in 1..12) {
            mutableStateOf(savedId)
        } else {
            val savedName = AppSettings.getString(context, "settings_selected_type", "")
            val matched = gameModeItems.find { it.name.equals(savedName, ignoreCase = true) }
            mutableStateOf(matched?.typeId ?: 6)
        }
    }
    val selectedModeName = gameModeItems.find { it.typeId == selectedTypeId }?.name ?: gameModeItems.first().name

    val officialMaps = remember(context, selectedTypeId) { 
        BaseGamemodeMultiplayers.getOfficialMapsList(context, selectedTypeId) 
    }
    val availableMaps = remember(officialMaps, SessionOnlineMaps.size, selectedTypeId, isOnline) {
        val list = mutableListOf<Pair<String, String>>()
        list.addAll(officialMaps)
        for (om in SessionOnlineMaps) {
            if (list.none { it.first == om.first }) {
                if (isOnline && !om.first.startsWith("official_") && !MapSavior.isMapPublished(context, om.first)) {
                    continue
                }
                list.add(om)
            }
        }
        list.filter { BaseGamemodeMultiplayers.isMapSuitableForMode(context, selectedTypeId, it.first) }
    }

    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000)
            toastMessage = null
        }
    }

    var selectedMapUid by remember { 
        val savedUid = AppSettings.getString(context, "settings_selected_map_uid", "official_9")
        val initUid = if (savedUid.isNotEmpty()) {
            if (savedUid.startsWith("official_")) {
                val id = savedUid.removePrefix("official_").toIntOrNull()
                if (id != null && (id in 1..16 || id == -1)) savedUid else "official_9"
            } else if (isOnline && !MapSavior.isMapPublished(context, savedUid)) {
                if (selectedTypeId == 12) "official_-1" else "official_9"
            } else {
                savedUid
            }
        } else if (selectedTypeId == 12) {
            "official_-1"
        } else {
            "official_9"
        }
        mutableStateOf(
            if (selectedTypeId == 12 && !BaseGamemodeMultiplayers.isMapSuitableForMode(context, 12, initUid)) "official_-1" else initUid
        )
    }
    var selectedMapName by remember { 
        val savedName = AppSettings.getString(context, "settings_selected_map_name", "")
        val initName = if (savedName.isNotEmpty() && selectedMapUid.isNotEmpty()) {
            if (selectedMapUid.startsWith("official_")) {
                val id = selectedMapUid.removePrefix("official_").toIntOrNull()
                if (id != null && (id in 1..16 || id == -1)) {
                    BaseGamemodeMultiplayers.getOfficialMapName(context, id)
                } else {
                    BaseGamemodeMultiplayers.getOfficialMapName(context, 9)
                }
            } else {
                savedName
            }
        } else if (selectedTypeId == 12) {
            BaseGamemodeMultiplayers.getOfficialMapName(context, -1)
        } else {
            BaseGamemodeMultiplayers.getOfficialMapName(context, 9)
        }
        mutableStateOf(
            if (selectedTypeId == 12 && selectedMapUid == "official_-1") BaseGamemodeMultiplayers.getOfficialMapName(context, -1) else initName
        )
    }

    // лимит ботов из конфига профильного сервера
    val hasProfCfg = isOnline || sys_menu_socket_repository.isServerOnline
    val maxNormalBotLimit = if (hasProfCfg) sys_menu_socket_repository.onlineServerConfig.maxBotsNormalServer.coerceIn(1, 1000) else 28
    val maxBrBotLimit = if (hasProfCfg) sys_menu_socket_repository.onlineServerConfig.maxBrBots.coerceIn(1, 1000) else 50
    val maxBugsLimit = if (hasProfCfg) sys_menu_socket_repository.onlineServerConfig.maxBugs.coerceAtLeast(1) else 150

    var redBots by remember {
        val saved = AppSettings.getInt(context, "settings_red_bots", 10)
        val limit = when {
            BaseGamemodeMultiplayers.isBattleRoyale(selectedTypeId) -> maxBrBotLimit
            BaseGamemodeMultiplayers.isTeamGame(selectedTypeId) -> maxNormalBotLimit / 2
            else -> maxNormalBotLimit
        }
        mutableStateOf(saved.coerceIn(0, limit))
    }
    var blueBots by remember {
        val saved = AppSettings.getInt(context, "settings_blue_bots", 10)
        val initVal = when {
            BaseGamemodeMultiplayers.isTeamGame(selectedTypeId) -> saved.coerceIn(0, maxNormalBotLimit / 2)
            BaseGamemodeMultiplayers.isBattleRoyale(selectedTypeId) -> if (isOnline) saved.coerceIn(2, 10).takeIf { it >= 2 } ?: 2 else 0
            BaseGamemodeMultiplayers.isBugsAttack(selectedTypeId) -> if (saved <= 0 || saved > maxBugsLimit) maxBugsLimit else saved.coerceIn(0, maxBugsLimit)
            else -> 0
        }
        mutableStateOf(initVal)
    }
    var redBotsDiffIndex by remember { mutableStateOf(AppSettings.getInt(context, "settings_red_bots_diff_idx", 1)) }
    var blueBotsDiffIndex by remember { mutableStateOf(AppSettings.getInt(context, "settings_blue_bots_diff_idx", 1)) }
    var password by remember { mutableStateOf(AppSettings.getString(context, "settings_password", "")) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedMapUid) {
        val uid = selectedMapUid
        if (uid.startsWith("official_")) {
            val id = uid.removePrefix("official_").toIntOrNull()
            if (id != null) {
                selectedMapName = BaseGamemodeMultiplayers.getOfficialMapName(context, id)
            }
        }
    }

    LaunchedEffect(selectedMapUid, selectedMapName) {
        val uid = selectedMapUid
        val name = selectedMapName
        if (uid.isNotEmpty() && !uid.startsWith("official_") && name.isNotEmpty() && name != skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_kart_da30) && SessionOnlineMaps.none { it.first == uid }) {
            SessionOnlineMaps.add(Pair(uid, name))
        }
    }
    LaunchedEffect(selectedMapName) {
        AppSettings.setString(context, "settings_selected_map_name", selectedMapName)
    }
    LaunchedEffect(selectedMapUid) {
        AppSettings.setString(context, "settings_selected_map_uid", selectedMapUid)
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(3000L)
            toastMessage = null
        }
    }

    LaunchedEffect(selectedTypeId) {
        AppSettings.setInt(context, "settings_selected_type_id", selectedTypeId)
        AppSettings.setString(context, "settings_selected_type", BaseGamemodeMultiplayers.getGameModeName(context, selectedTypeId))
        if (selectedTypeId == 12) {
            if (!BaseGamemodeMultiplayers.isMapSuitableForMode(context, 12, selectedMapUid)) {
                selectedMapUid = "official_-1"
                selectedMapName = BaseGamemodeMultiplayers.getOfficialMapName(context, -1)
            }
        } else {
            if (selectedMapUid == "official_-1") {
                selectedMapUid = "official_9"
                selectedMapName = BaseGamemodeMultiplayers.getOfficialMapName(context, 9)
            }
        }
        if (BaseGamemodeMultiplayers.isTeamGame(selectedTypeId)) {
            val teamMax = maxNormalBotLimit / 2
            redBots = redBots.coerceIn(0, teamMax)
            blueBots = blueBots.coerceIn(0, teamMax)
        } else if (BaseGamemodeMultiplayers.isBattleRoyale(selectedTypeId)) {
            redBots = redBots.coerceIn(0, maxBrBotLimit)
            if (isOnline) {
                blueBots = blueBots.coerceIn(2, 10)
                if (blueBots < 2) blueBots = 2
            } else {
                blueBots = 0
            }
        } else if (BaseGamemodeMultiplayers.isBugsAttack(selectedTypeId)) {
            redBots = redBots.coerceIn(0, maxNormalBotLimit)
            if (blueBots <= 0 || blueBots > maxBugsLimit) blueBots = maxBugsLimit
            blueBots = blueBots.coerceIn(0, maxBugsLimit)
        } else {
            redBots = redBots.coerceIn(0, maxNormalBotLimit)
            blueBots = 0
        }
    }

    LaunchedEffect(redBots) {
        AppSettings.setInt(context, "settings_red_bots", redBots)
    }
    LaunchedEffect(blueBots) {
        AppSettings.setInt(context, "settings_blue_bots", blueBots)
    }
    LaunchedEffect(redBotsDiffIndex) {
        AppSettings.setInt(context, "settings_red_bots_diff_idx", redBotsDiffIndex)
        val legacy = listOf("Низко", "Нормально", "Высоко")
        AppSettings.setString(context, "settings_red_bots_diff", legacy.getOrNull(redBotsDiffIndex) ?: "Нормально")
        if (!BaseGamemodeMultiplayers.isTeamGame(selectedTypeId)) {
            blueBotsDiffIndex = redBotsDiffIndex
        }
    }
    LaunchedEffect(blueBotsDiffIndex) {
        AppSettings.setInt(context, "settings_blue_bots_diff_idx", blueBotsDiffIndex)
        val legacy = listOf("Низко", "Нормально", "Высоко")
        AppSettings.setString(context, "settings_blue_bots_diff", legacy.getOrNull(blueBotsDiffIndex) ?: "Нормально")
    }
    LaunchedEffect(password) {
        AppSettings.setString(context, "settings_password", password)
    }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var tempPassword by remember { mutableStateOf("") }
    var activeGameWorld by remember { mutableStateOf<SessionWorld?>(null) }
    
    LaunchedEffect(currentSubScreen) {
        if (currentSubScreen == "settings" && !isOnline) {
            val uid = selectedMapUid
            if (!uid.startsWith("official_")) {
                val exists = listOf(
                    context.filesDir,
                    java.io.File(context.filesDir, "from_my_map"),
                    java.io.File(context.filesDir, "from_online_map"),
                    java.io.File(context.filesDir, "maps")
                ).any { dir ->
                    java.io.File(dir, "$uid.map").exists()
                }
                if (!exists) {
                    selectedMapUid = "official_9"
                    selectedMapName = BaseGamemodeMultiplayers.getOfficialMapName(context, 9)
                }
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp, start = 0.dp, end = 0.dp, bottom = 4.dp)
        ) {
            when (currentSubScreen) {
                "maps" -> {
                    LocalMapsPanel(
                        selectedTypeId = selectedTypeId,
                        isOnline = isOnline,
                        onMapSelected = { uid, name -> 
                            selectedMapUid = uid
                            selectedMapName = name
                            if (uid.isNotEmpty() && name.isNotEmpty() && name != skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_kart_da30) && SessionOnlineMaps.none { it.first == uid }) {
                                SessionOnlineMaps.add(Pair(uid, name))
                            }
                            currentSubScreen = "settings"
                            onHideTopBar(false)
                        },
                        onShowToast = { msg -> toastMessage = msg },
                        onBack = { 
                            currentSubScreen = "settings" 
                            onHideTopBar(false)
                        }
                    )
                }
                "online_maps" -> {
                    OnlineMapsPanel(
                        selectedTypeId = selectedTypeId,
                        onMapSelected = { uid, name -> 
                            selectedMapUid = uid
                            selectedMapName = name
                            if (uid.isNotEmpty() && name.isNotEmpty() && name != skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_kart_da30) && SessionOnlineMaps.none { it.first == uid }) {
                                SessionOnlineMaps.add(Pair(uid, name))
                            }
                            currentSubScreen = "settings"
                            onHideTopBar(false)
                        },
                        onShowToast = { msg -> toastMessage = msg },
                        onBack = { 
                            currentSubScreen = "settings" 
                            onHideTopBar(false)
                        }
                    )
                }
                "options" -> {
                    start_game_options(onBack = { 
                        currentSubScreen = "settings" 
                        onHideTopBar(false)
                    })
                }
                else -> {
                    // окно пароля сервера
                    if (showPasswordDialog) {
                        var dialogPassword by remember(showPasswordDialog) { mutableStateOf(password) }
                        LaunchedEffect(showPasswordDialog) {
                            dialogPassword = password
                        }
                        UnifiedDialog(
                            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_parol_servera_eee8),
                            content = {
                                UnifiedTextField(
                                    value = dialogPassword,
                                    onValueChange = { dialogPassword = AllowedChars.sanitize(it, 16) },
                                    placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vvedite_parol_0d49),
                                    inputFilter = { AllowedChars.sanitize(it, 16) },
                                    regex = AllowedChars.REGEX,
                                    maxLength = 16
                                )
                            },
                            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sohranit_74ea),
                            onConfirm = {
                                password = dialogPassword
                                tempPassword = dialogPassword
                                showPasswordDialog = false
                            },
                            onDismiss = { showPasswordDialog = false }
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 720.dp)
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SettingsRow(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.select_map)) {
                                val mapDisplayNames = availableMaps.map { it.second }
                                val currentMapDisplayName = availableMaps.find { it.first == selectedMapUid }?.second 
                                    ?: if (selectedMapName.isNotEmpty()) selectedMapName else if (isOnline) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.select_map1) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_kart_da30)

                                if (selectedTypeId == 12) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .height(38.5.dp)
                                            .padding(horizontal = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = currentMapDisplayName,
                                            color = AppColors.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    CustomDropdown(
                                        items = if (mapDisplayNames.isNotEmpty()) mapDisplayNames else listOf(currentMapDisplayName),
                                        selectedItem = currentMapDisplayName,
                                        onItemSelected = { chosenName ->
                                            val match = availableMaps.find { it.second == chosenName }
                                            if (match != null) {
                                                if (selectedTypeId == 12 && !BaseGamemodeMultiplayers.isMapSuitableForMode(context, selectedTypeId, match.first)) {
                                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_minimalnyy_razmer_karty_dolzhe_6edc)
                                                } else {
                                                    selectedMapUid = match.first
                                                    selectedMapName = match.second
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1.2f)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(0.8f)
                                        .height(38.5.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AppColors.TabInactive)
                                        .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                        .clickable {
                                            if (isOnline) {
                                                currentSubScreen = "online_maps"
                                                onHideTopBar(true)
                                            } else {
                                                currentSubScreen = "maps"
                                                onHideTopBar(true)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isOnline) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_onlayn_karty_539e) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_lokalnye_karty_9f7a),
                                        color = AppColors.White,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                        SettingsRow(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.game_type)) {
                            CustomDropdown(
                                items = gameModeNames,
                                selectedItem = selectedModeName,
                                onItemSelected = { chosenName ->
                                    val match = gameModeItems.find { it.name == chosenName }
                                    if (match != null) {
                                        selectedTypeId = match.typeId
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        val diffOptions = remember(context) { BaseGamemodeMultiplayers.getBotDifficulties(context) }
                        val currentRedDiff = diffOptions.getOrNull(redBotsDiffIndex) ?: diffOptions.getOrElse(1) { "Normal" }
                        val currentBlueDiff = diffOptions.getOrNull(blueBotsDiffIndex) ?: diffOptions.getOrElse(1) { "Normal" }

                        if (BaseGamemodeMultiplayers.isTeamGame(selectedTypeId)) {
                            val teamMax = (maxNormalBotLimit / 2).coerceIn(1, 500)
                            BotSettings(
                                label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.red_team_bot),
                                bots = redBots,
                                onBotsChange = { redBots = it.coerceIn(0, teamMax) },
                                botsDiff = currentRedDiff,
                                onBotsDiffChange = { selected -> redBotsDiffIndex = diffOptions.indexOf(selected).takeIf { it != -1 } ?: 1 },
                                maxBots = teamMax
                            )
                            BotSettings(
                                label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.blue_team_bot),
                                bots = blueBots,
                                onBotsChange = { blueBots = it.coerceIn(0, teamMax) },
                                botsDiff = currentBlueDiff,
                                onBotsDiffChange = { selected -> blueBotsDiffIndex = diffOptions.indexOf(selected).takeIf { it != -1 } ?: 1 },
                                maxBots = teamMax
                            )
                        } else if (BaseGamemodeMultiplayers.isBattleRoyale(selectedTypeId)) {
                            BotSettings(
                                label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bot_count),
                                bots = redBots,
                                onBotsChange = { redBots = it.coerceIn(0, maxBrBotLimit) },
                                botsDiff = currentRedDiff,
                                onBotsDiffChange = { selected -> redBotsDiffIndex = diffOptions.indexOf(selected).takeIf { it != -1 } ?: 1 },
                                maxBots = maxBrBotLimit
                            )
                            if (isOnline) {
                                SettingsRow(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.minimum_of_players)) {
                                    SpinnerInput(
                                        value = blueBots.coerceIn(2, 10),
                                        onValueChange = { blueBots = it.coerceIn(2, 10) },
                                        modifier = Modifier.width(102.dp).height(38.5.dp),
                                        minVal = 2,
                                        maxVal = 10
                                    )
                                }
                            }
                        } else if (BaseGamemodeMultiplayers.isBugsAttack(selectedTypeId)) {
                            BotSettings(
                                label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bot_count),
                                bots = redBots,
                                onBotsChange = { redBots = it.coerceIn(0, maxNormalBotLimit) },
                                botsDiff = currentRedDiff,
                                onBotsDiffChange = { selected -> redBotsDiffIndex = diffOptions.indexOf(selected).takeIf { it != -1 } ?: 1 },
                                maxBots = maxNormalBotLimit
                            )
                            SettingsRow(label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bugs_count)) {
                                SpinnerInput(
                                    value = blueBots.coerceIn(0, maxBugsLimit),
                                    onValueChange = { blueBots = it.coerceIn(0, maxBugsLimit) },
                                    modifier = Modifier.width(102.dp).height(38.5.dp),
                                    minVal = 0,
                                    maxVal = maxBugsLimit
                                )
                            }
                        } else {
                            BotSettings(
                                label = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.bot_count),
                                bots = redBots,
                                onBotsChange = { redBots = it.coerceIn(0, maxNormalBotLimit) },
                                botsDiff = currentRedDiff,
                                onBotsDiffChange = { selected -> redBotsDiffIndex = diffOptions.indexOf(selected).takeIf { it != -1 } ?: 1 },
                                maxBots = maxNormalBotLimit
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(35.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.TabInactive)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                .clickable { onBack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back),
                                color = AppColors.White,
                                fontSize = 14.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(35.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.TabInactive)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                .clickable {
                                    tempPassword = password
                                    showPasswordDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.password),
                                color = AppColors.White,
                                fontSize = 14.sp
                            )
                            if (password.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 3.dp)
                                        .height(2.dp)
                                        .width(40.dp)
                                        .background(AppColors.Success, RoundedCornerShape(1.dp))
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(35.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.TabInactive)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                .clickable { 
                                    currentSubScreen = "options" 
                                    onHideTopBar(true)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.options),
                                color = AppColors.White,
                                fontSize = 14.sp
                            )
                        }

                        val isPlayEnabled = !isLaunching && sys_menu_socket_repository.isServerOnline && !GlobalReconnectState.isReconnecting && !GlobalReconnectState.showConnectionErrorDialog
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(35.dp)
                                .menuActionButtonStyle(enabled = isPlayEnabled, shape = RoundedCornerShape(4.dp))
                                .clickable(enabled = isPlayEnabled) {
                                    val now = android.os.SystemClock.elapsedRealtime()
                                    if (isLaunching || !MultiplayersLaunchGuard.canLaunch()) {
                                        return@clickable
                                    }

                                    lastLaunchTime = now
                                    isLaunching = true

                                    if (!isOnline) {
                                        val config = ServerConfigManager.load(context)
                                        val localBlueBots = when {
                                            BaseGamemodeMultiplayers.isTeamGame(selectedTypeId) -> blueBots
                                            BaseGamemodeMultiplayers.isBugsAttack(selectedTypeId) -> blueBots
                                            BaseGamemodeMultiplayers.isBattleRoyale(selectedTypeId) -> 0
                                            else -> 0
                                        }
                                        val effectiveBlueDiff = if (BaseGamemodeMultiplayers.isTeamGame(selectedTypeId)) blueBotsDiffIndex else redBotsDiffIndex
                                        val intent = android.content.Intent(context, datagame_main::class.java).apply {
                                            addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                            putExtra("ACTION_START_LOCAL_GAME", true)
                                            putExtra("IS_ADMIN", sys_menu_socket_repository.currentPlayerAdminStatus == 1)
                                            putExtra("ACCOUNT_ID", accountId)
                                            putExtra("APP_LANGUAGE", AppLanguageManager.currentLanguage)
                                            putExtra("SELECTED_MAP_UID", selectedMapUid)
                                            putExtra("SELECTED_MAP_NAME", selectedMapName)
                                            putExtra("SELECTED_TYPE_ID", selectedTypeId)
                                            putExtra("RED_BOTS", redBots)
                                            putExtra("BLUE_BOTS", localBlueBots)
                                            putExtra("RED_BOTS_DIFF", redBotsDiffIndex)
                                            putExtra("BLUE_BOTS_DIFF", effectiveBlueDiff)
                                            putExtra("GAME_PASSWORD", password.trim())
                                            putExtra("START_MONEY", config.startMoney)
                                            putExtra("ROUND_TIME_MINUTES", config.roundTimeMinutes)
                                            putExtra("SLOT_WEAPON_0", config.slotWeapon0 ?: "")
                                            putExtra("SLOT_WEAPON_1", config.slotWeapon1 ?: "ob_p1")
                                            putExtra("SLOT_WEAPON_2", config.slotWeapon2 ?: "")
                                            putStringArrayListExtra("DISABLED_ITEMS", java.util.ArrayList(config.disabledItems))
                                        }
                                        MultiplayersLaunchGuard.markLaunched()
                                        context.startActivity(intent)
                                        (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                        scope.launch {
                                            kotlinx.coroutines.delay(2500L)
                                            isLaunching = false
                                        }
                                        return@clickable
                                    }

                                    scope.launch {
                                        var launchedOk = false
                                        try {
                                            // параллельная проверка бана, конфига и списка серверов без зависаний
                                            val host = try { c2dv2.getHost() } catch (e: Exception) { "151.243.182.150" }
                                            val (banResult, cfg, onlineServers) = coroutineScope {
                                                val banDeferred = async(Dispatchers.IO) {
                                                    withTimeoutOrNull(1500L) {
                                                        sys_menu_socket_repository.checkIsPlayerBanned()
                                                    } ?: Pair(
                                                        sys_menu_socket_repository.currentPlayerBanStatus == 1,
                                                        sys_menu_socket_repository.currentPlayerBanReason
                                                    )
                                                }
                                                val cfgDeferred = async(Dispatchers.IO) {
                                                    withTimeoutOrNull(1500L) {
                                                        sys_menu_socket_repository.fetchOnlineServerConfig()
                                                    } ?: sys_menu_socket_repository.onlineServerConfig
                                                }
                                                val serversDeferred = async(Dispatchers.IO) {
                                                    withTimeoutOrNull(1600L) {
                                                        discoverOnlineServers(context)
                                                    } ?: emptyList()
                                                }
                                                Triple(banDeferred.await(), cfgDeferred.await(), serversDeferred.await())
                                            }

                                            if (banResult.first) {
                                                val reason = banResult.second
                                                toastMessage = if (!reason.isNullOrBlank()) {
                                                    "${skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.you_were_banned)}: $reason"
                                                } else {
                                                    skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.you_were_banned)
                                                }
                                                return@launch
                                            }

                                            // 1) Название причины если сервер недоступен (выключен)
                                            if (!cfg.isServerEnabled || globalServerNotice?.type == ServerNoticeType.MAINTENANCE) {
                                                toastMessage = if (cfg.maintenanceMessage.isNotBlank()) cfg.maintenanceMessage else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_maintenance)
                                                return@launch
                                            }

                                            // 2) Неактуальна версия, обновите игру
                                            if (BuildConfig.VERSION_CODE < cfg.minRequiredVersionCode || sys_menu_socket_repository.isVersionMismatch) {
                                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_outdated_version)
                                                return@launch
                                            }

                                            // 4) Необходима регистрация аккаунта для создания сервера
                                            if (cfg.requireAuthToCreateServer && (accountId.isBlank() || !sys_menu_socket_repository.currentPlayerIsRegistered)) {
                                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_registration_required)
                                                return@launch
                                            }

                                            if (BaseGamemodeMultiplayers.isBattleRoyale(selectedTypeId)) {
                                                // 11) Превышен лимит ботов в КБ (X)
                                                if (redBots > cfg.maxBrBots) {
                                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_limit_br_bots, cfg.maxBrBots)
                                                    return@launch
                                                }
                                            } else if (BaseGamemodeMultiplayers.isBugsAttack(selectedTypeId)) {
                                                // 12) Превышен лимит жуков и ботов
                                                if (redBots > cfg.maxBotsNormalServer) {
                                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_limit_bots, cfg.maxBotsNormalServer)
                                                    return@launch
                                                }
                                                if (blueBots > cfg.maxBugs) {
                                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_limit_bugs, cfg.maxBugs)
                                                    return@launch
                                                }
                                            } else if (BaseGamemodeMultiplayers.isTeamGame(selectedTypeId)) {
                                                // 8) Превышен лимит ботов на сервере (X) в командной игре
                                                val totalBots = redBots + blueBots
                                                if (totalBots > cfg.maxBotsNormalServer) {
                                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_limit_bots, cfg.maxBotsNormalServer)
                                                    return@launch
                                                }
                                            } else {
                                                // 8) Превышен лимит ботов на сервере (X) в одиночных режимах (Каждый за себя и др.)
                                                if (redBots > cfg.maxBotsNormalServer) {
                                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_limit_bots, cfg.maxBotsNormalServer)
                                                    return@launch
                                                }
                                            }

                                            if (selectedTypeId == 12 && !BaseGamemodeMultiplayers.isMapSuitableForMode(context, selectedTypeId, selectedMapUid)) {
                                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_minimalnyy_razmer_karty_dolzhe_6edc)
                                                return@launch
                                            }

                                            // 3) Достигнут лимит созданных серверов (X) и 9) Превышен лимит королевских битв (X)
                                            if (onlineServers.size >= cfg.maxServers) {
                                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_limit_servers, cfg.maxServers)
                                                return@launch
                                            }
                                            if (selectedTypeId == 12) {
                                                 val brCount = onlineServers.count { it.gameMode.contains("Королевская", true) || it.gameMode.contains("Battle Royale", true) || it.rawNmap == -1 }
                                                if (brCount >= cfg.maxBattleRoyaleMatches) {
                                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_limit_br, cfg.maxBattleRoyaleMatches)
                                                    return@launch
                                                }
                                            }

                                            // 5) Не удалось запустить сервер [COD: A57]
                                            if (!cfg.hasProfileServer) {
                                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.err_start_server_code_a57)
                                                return@launch
                                            }

                                            // 6) Не удалось запустить сервер [COD: G57]
                                            val isPortOk = onlineServers.isNotEmpty() || isGameServerPortReachable(host, cfg.mainPort)
                                            if (!isPortOk) {
                                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.err_start_server_code_g57)
                                                return@launch
                                            }

                                            // все проверки пройдены: запускаем создание и экран загрузки
                                            launchedOk = true
                                            MultiplayersLaunchGuard.markLaunched()
                                            if (selectedMapUid.isNotEmpty() && selectedMapName.isNotEmpty() && selectedMapName != skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_net_kart_da30) && selectedMapName != skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.select_map1)) {
                                                if (SessionOnlineMaps.none { it.first == selectedMapUid }) {
                                                    SessionOnlineMaps.add(Pair(selectedMapUid, selectedMapName))
                                                }
                                            }
                                            val config = ServerConfigManager.load(context)
                                            val onlineBlueBots = when {
                                                BaseGamemodeMultiplayers.isTeamGame(selectedTypeId) -> blueBots
                                                BaseGamemodeMultiplayers.isBugsAttack(selectedTypeId) -> blueBots
                                                BaseGamemodeMultiplayers.isBattleRoyale(selectedTypeId) -> blueBots.coerceIn(2, 10)
                                                else -> 0
                                            }
                                            val intent = android.content.Intent(context, datagame_main::class.java).apply {
                                                addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                putExtra("ACTION_START_ONLINE_GAME", true)
                                                putExtra("IS_ADMIN", sys_menu_socket_repository.currentPlayerAdminStatus == 1)
                                                putExtra("SERVER_IP", host)
                                                putExtra("SERVER_PORT", cfg.mainPort)
                                                putExtra("ACCOUNT_ID", accountId)
                                                putExtra("APP_LANGUAGE", AppLanguageManager.currentLanguage)
                                                putExtra("SELECTED_MAP_UID", selectedMapUid)
                                                putExtra("SELECTED_MAP_NAME", selectedMapName)
                                                putExtra("SELECTED_TYPE_ID", selectedTypeId)
                                                putExtra("RED_BOTS", redBots)
                                                putExtra("BLUE_BOTS", onlineBlueBots)
                                                val effectiveOnlineBlueDiff = if (BaseGamemodeMultiplayers.isTeamGame(selectedTypeId)) blueBotsDiffIndex else redBotsDiffIndex
                                                putExtra("RED_BOTS_DIFF", redBotsDiffIndex)
                                                putExtra("BLUE_BOTS_DIFF", effectiveOnlineBlueDiff)
                                                putExtra("GAME_PASSWORD", password.trim())
                                                putExtra("START_MONEY", config.startMoney)
                                                putExtra("ROUND_TIME_MINUTES", config.roundTimeMinutes)
                                                putExtra("SLOT_WEAPON_0", config.slotWeapon0 ?: "")
                                                putExtra("SLOT_WEAPON_1", config.slotWeapon1 ?: "ob_p1")
                                                putExtra("SLOT_WEAPON_2", config.slotWeapon2 ?: "")
                                                putStringArrayListExtra("DISABLED_ITEMS", java.util.ArrayList(config.disabledItems))
                                            }
                                            context.startActivity(intent)
                                            (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                                            kotlinx.coroutines.delay(2500L)
                                            isLaunching = false
                                        } finally {
                                            if (!launchedOk) {
                                                isLaunching = false
                                                MultiplayersLaunchGuard.release()
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ShimmerPlayText(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.play),
                                isShimmering = isLaunching,
                                enabled = isPlayEnabled,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    UnifiedToast(
        message = toastMessage ?: "",
        visible = toastMessage != null,
        modifier = Modifier.align(Alignment.TopCenter)
    )
}
}

data class PreviewServer(
    val ownerName: String,
    val mapName: String,
    val mapType: String,
    val gameMode: String,
    val playersCount: Int,
    val botsCount: Int,
    val version: String,
    val ping: Int,
    val requiresPassword: Boolean = false,
    val password: String = "123",
    val ip: String = "",
    val port: Int = 37491,
    val rawVersion: Int = 39,
    val rawNmap: Int = 1,
    val isCustomMap: Boolean = false,
    val mapUid: String = "",
    val status: Int = 0
)

object ServerDatabase {
    val servers = emptyList<PreviewServer>()
}

fun getMapDisplayName(context: Context, nmap: Int): String {
    return when (nmap) {
        -1 -> context.getString(R.string.battle_royale)
        1 -> context.getString(R.string.armory_room)
        2 -> context.getString(R.string.forest)
        3 -> context.getString(R.string.garden)
        4 -> context.getString(R.string.sands)
        5 -> context.getString(R.string.survival)
        6 -> context.getString(R.string.trenches)
        7 -> context.getString(R.string.military_warehouses)
        8 -> context.getString(R.string.map8)
        9 -> context.getString(R.string.map9)
        10 -> context.getString(R.string.map10)
        11 -> context.getString(R.string.map11)
        12 -> context.getString(R.string.map12)
        13 -> context.getString(R.string.map13)
        14 -> context.getString(R.string.map14)
        15 -> context.getString(R.string.map15)
        16 -> context.getString(R.string.map16)
        else -> context.getString(R.string.map_not_found)
    }
}

fun getGameModeDisplayName(context: Context, gtype: Int): String {
    return when (gtype) {
        1 -> context.getString(R.string.team_game)
        2 -> context.getString(R.string.free_for_all)
        3 -> context.getString(R.string.base_defence)
        4 -> context.getString(R.string.capture_the_flag)
        5 -> context.getString(R.string.bugs_attack)
        6 -> context.getString(R.string.team_game_score)
        7 -> context.getString(R.string.zombie_infection)
        8 -> context.getString(R.string.juggernaut)
        9 -> context.getString(R.string.bomb)
        10 -> context.getString(R.string.arms_race)
        11 -> context.getString(R.string.ghost)
        12 -> context.getString(R.string.battle_royale)
        else -> "N/A"
    }
}

suspend fun discoverLocalWifiServers(context: Context): List<PreviewServer> = withContext(Dispatchers.IO) {
    val servers = mutableListOf<PreviewServer>()
    val seenIps = mutableSetOf<String>()
    var lock: WifiManager.MulticastLock? = null
    var socket: DatagramSocket? = null
    try {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (wm != null) {
            lock = wm.createMulticastLock("Shootout2DWiFiDiscoveryLock")
            lock.setReferenceCounted(true)
            lock.acquire()
        }

        val broadcastAddresses = mutableListOf<InetAddress>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    val ni = interfaces.nextElement()
                    if (ni.isLoopback || !ni.isUp) continue
                    for (ia in ni.interfaceAddresses) {
                        val b = ia.broadcast
                        if (b != null) broadcastAddresses.add(b)
                    }
                }
            }
        } catch (ignored: Exception) {}
        try {
            broadcastAddresses.add(InetAddress.getByName("255.255.255.255"))
        } catch (ignored: Exception) {}

        socket = DatagramSocket()
        socket.broadcast = true
        socket.soTimeout = 1200

        val sendBuf = byteArrayOf(7) // byte 7: _server?_
        val sTime = System.currentTimeMillis()
        for (addr in broadcastAddresses.distinct()) {
            try {
                val p = DatagramPacket(sendBuf, sendBuf.size, addr, 37491)
                socket.send(p)
            } catch (ignored: Exception) {}
        }

        val buf = ByteArray(1024)
        while (System.currentTimeMillis() - sTime < 1300) {
            try {
                val rPacket = DatagramPacket(buf, buf.size)
                socket.receive(rPacket)
                val ipStr = rPacket.address.hostAddress ?: continue
                if (seenIps.contains(ipStr)) continue
                seenIps.add(ipStr)

                val bais = ByteArrayInputStream(rPacket.data, rPacket.offset, rPacket.length)
                val dais = DataInputStream(bais)
                val pname = dais.readByte()
                if (pname == 8.toByte()) { // _srvinfo_
                    val srvport = dais.readInt()
                    val nmapi = dais.readByte().toInt()
                    val custommap = dais.readBoolean()
                    val mapname = if (custommap) dais.readUTF() else null
                    val gtypei = dais.readByte().toInt()
                    val plcounti = dais.readByte().toInt()
                    val botcounti = dais.readByte().toInt()
                    val veri = dais.readByte().toInt()
                    val sver = dais.readUTF()
                    val hostName = dais.readUTF()
                    val ping = (System.currentTimeMillis() - sTime).toInt().coerceAtLeast(1)

                    val dispMapName = if (custommap) {
                        mapname ?: "Custom"
                    } else {
                        getMapDisplayName(context, nmapi)
                    }
                    val dispGameMode = getGameModeDisplayName(context, gtypei)

                    servers.add(
                        PreviewServer(
                            ownerName = hostName,
                            mapName = dispMapName,
                            mapType = if (custommap) "Custom" else "Official",
                            gameMode = dispGameMode,
                            playersCount = plcounti,
                            botsCount = botcounti,
                            version = sver,
                            ping = ping,
                            requiresPassword = false,
                            password = "",
                            ip = ipStr,
                            port = srvport,
                            rawVersion = veri,
                            rawNmap = nmapi,
                            isCustomMap = custommap
                        )
                    )
                }
            } catch (e: SocketTimeoutException) {
                break
            } catch (e: Exception) {
                break
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        try {
            socket?.close()
        } catch (ignored: Exception) {}
        try {
            if (lock != null && lock.isHeld) {
                lock.release()
            }
        } catch (ignored: Exception) {}
    }
    servers
}

suspend fun discoverOnlineServers(context: Context): List<PreviewServer> = withContext(Dispatchers.IO) {
    val servers = mutableListOf<PreviewServer>()
    var socket: DatagramSocket? = null
    try {
        val host = try { c2dv2.getHost() } catch (e: Exception) { "151.243.182.150" }
        val srvAddr = InetAddress.getByName(host)
        socket = DatagramSocket()
        socket.soTimeout = 750

        val baos = ByteArrayOutputStream()
        val daos = DataOutputStream(baos)
        daos.writeByte(-7) // _serverlist_
        daos.writeByte(23) // sver
        daos.flush()
        val sendBuf = baos.toByteArray()
        val sTime = System.currentTimeMillis()
        val sendPacket = DatagramPacket(sendBuf, sendBuf.size, srvAddr, 37491)
        socket.send(sendPacket)

        val buf = ByteArray(8192)
        var received = false
        var attempts = 0
        while (System.currentTimeMillis() - sTime < 1500 && !received && attempts < 2) {
            try {
                val rPacket = DatagramPacket(buf, buf.size)
                socket.receive(rPacket)
                val bais = ByteArrayInputStream(rPacket.data, rPacket.offset, rPacket.length)
                val dais = DataInputStream(bais)
                val pname = dais.readByte()
                if (pname == (-4).toByte()) {
                    globalServerNotice = ServerNoticeState(
                        type = ServerNoticeType.MAINTENANCE,
                        title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_status_maintenance_title),
                        message = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_status_maintenance_msg)
                    )
                    received = true
                } else if (pname == (-2).toByte()) {
                    received = true
                } else if (pname == (-3).toByte()) {
                    globalServerNotice = ServerNoticeState(
                        type = ServerNoticeType.VERSION_MISMATCH,
                        title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_status_version_mismatch_title),
                        message = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_status_version_mismatch_msg)
                    )
                    received = true
                } else if (pname == (-7).toByte()) {
                    globalServerNotice = null
                    val count = dais.readShort().toInt()
                    for (i in 0 until count) {
                        val sport = dais.readInt()
                        val nmap = dais.readByte().toInt()
                        val custommap = dais.readBoolean()
                        var mapname = ""
                        var mapuid = ""
                        if (custommap) {
                            mapname = dais.readUTF()
                            mapuid = dais.readUTF()
                        }
                        val gtype = dais.readByte().toInt()
                        val plcount = dais.readByte().toInt()
                        val botcount = dais.readByte().toInt()
                        val ver = dais.readByte().toInt()
                        val sver = dais.readUTF()
                        val srvPing = dais.readShort().toInt()
                        val name = dais.readUTF()
                        val hasPass = dais.readBoolean()
                        val status = dais.readByte().toInt()

                        val measuredPing = (System.currentTimeMillis() - sTime).toInt().coerceAtLeast(1)
                        // расчет пинга до хостера
                        val clientPing = when {
                            sys_menu_socket_repository.lastPingValue in 1..998 -> sys_menu_socket_repository.lastPingValue
                            PerformanceMonitorState.currentPing in 1..998 -> PerformanceMonitorState.currentPing
                            else -> measuredPing.coerceIn(1, 200)
                        }
                        val ping = when {
                            srvPing <= 0 -> -1
                            srvPing >= 999 || clientPing >= 999 -> 999
                            else -> (srvPing + clientPing).coerceIn(1, 999)
                        }

                        val dispMapName = if (custommap) {
                            mapname.ifEmpty { "Custom" }
                        } else {
                            getMapDisplayName(context, nmap)
                        }
                        val dispGameMode = getGameModeDisplayName(context, gtype)

                        servers.add(
                            PreviewServer(
                                ownerName = name,
                                mapName = dispMapName,
                                mapType = if (custommap) "Custom" else "Official",
                                gameMode = dispGameMode,
                                playersCount = plcount.coerceAtLeast(1),
                                botsCount = botcount.coerceAtLeast(0),
                                version = sver,
                                ping = ping,
                                requiresPassword = hasPass,
                                password = "",
                                ip = host,
                                port = sport,
                                rawVersion = ver,
                                rawNmap = nmap,
                                isCustomMap = custommap,
                                mapUid = mapuid,
                                status = status
                            )
                        )
                    }
                    received = true
                }
            } catch (e: SocketTimeoutException) {
                attempts++
                if (!received && attempts < 2) {
                    try {
                        socket.send(sendPacket)
                    } catch (ignore: Exception) {}
                } else {
                    break
                }
            } catch (e: Exception) {
                break
            }
        }
        if (!received && servers.isEmpty() && globalServerNotice == null) {
            globalServerNotice = ServerNoticeState(
                type = ServerNoticeType.UNREACHABLE,
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_status_unreachable_title),
                message = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_status_unreachable_msg)
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
        if (servers.isEmpty() && globalServerNotice == null) {
            globalServerNotice = ServerNoticeState(
                type = ServerNoticeType.UNREACHABLE,
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_status_unreachable_title),
                message = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_status_unreachable_msg)
            )
        }
    } finally {
        try {
            socket?.close()
        } catch (ignored: Exception) {}
    }
    servers
}

@Composable
fun connect_game_panel(
    isOnline: Boolean,
    accountId: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedMapFilter by remember { mutableStateOf(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.all)) }
    var selectedTypeFilter by remember { mutableStateOf(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.all)) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<PreviewServer?>(null) }
    var showConnectingDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var showPasswordInputByServer by remember { mutableStateOf<PreviewServer?>(null) }
    var bannerMessage by remember { mutableStateOf<String?>(null) }
    var isConnectingServer by remember { mutableStateOf(false) }
    var lastConnectTime by remember { mutableLongStateOf(0L) }
    
    var allServers by remember {
        mutableStateOf(emptyList<PreviewServer>())
    }

    fun doRefresh() {
        scope.launch {
            isRefreshing = true
            selectedServer = null
            if (!isOnline) {
                allServers = discoverLocalWifiServers(context)
            } else {
                allServers = discoverOnlineServers(context)
            }
            isRefreshing = false
        }
    }

    LaunchedEffect(isOnline) {
        doRefresh()
    }

    // автообновление списка серверов при возврате из матча в меню
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var hasResumedOnce by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner, isOnline) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isConnectingServer = false
                MultiplayersLaunchGuard.release()
                if (hasResumedOnce) {
                    scope.launch {
                        kotlinx.coroutines.delay(120L)
                        doRefresh()
                    }
                } else {
                    hasResumedOnce = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun launchConnectServer(server: PreviewServer, pass: String = "") {
        val now = android.os.SystemClock.elapsedRealtime()
        if (isConnectingServer || !MultiplayersLaunchGuard.canLaunch()) {
            return
        }

        if (!isOnline) {
            lastConnectTime = now
            isConnectingServer = true
            val intent = android.content.Intent(context, datagame_main::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("ACTION_CONNECT_WIFI_SERVER", true)
                putExtra("IS_ADMIN", sys_menu_socket_repository.currentPlayerAdminStatus == 1)
                putExtra("SERVER_IP", server.ip)
                putExtra("SERVER_PORT", server.port)
                putExtra("SERVER_NAME", server.ownerName)
                putExtra("GAME_PASSWORD", pass.trim())
                putExtra("ACCOUNT_ID", accountId)
                putExtra("APP_LANGUAGE", AppLanguageManager.currentLanguage)
            }
            MultiplayersLaunchGuard.markLaunched()
            context.startActivity(intent)
            (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            scope.launch {
                kotlinx.coroutines.delay(2500L)
                isConnectingServer = false
            }
            return
        }

        lastConnectTime = now
        isConnectingServer = true

        scope.launch {
            var connectedOk = false
            try {
                // параллельная проверка бана, конфига и статуса сервера перед входом
                val srvId = accountId.toIntOrNull() ?: 0
                val (banResult, cfg, serverCheck) = coroutineScope {
                    val banDeferred = async(Dispatchers.IO) {
                        withTimeoutOrNull(1500L) {
                            sys_menu_socket_repository.checkIsPlayerBanned()
                        } ?: Pair(
                            sys_menu_socket_repository.currentPlayerBanStatus == 1,
                            sys_menu_socket_repository.currentPlayerBanReason
                        )
                    }
                    val cfgDeferred = async(Dispatchers.IO) {
                        withTimeoutOrNull(1500L) {
                            sys_menu_socket_repository.fetchOnlineServerConfig()
                        } ?: sys_menu_socket_repository.onlineServerConfig
                    }
                    val statusDeferred = async(Dispatchers.IO) {
                        checkGameServerStatus(server.ip, server.port, srvId, accountId, pass)
                    }
                    Triple(banDeferred.await(), cfgDeferred.await(), statusDeferred.await())
                }
                if (banResult.first) {
                    val reason = banResult.second
                    bannerMessage = if (!reason.isNullOrBlank()) {
                        "${skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.you_were_banned)}: $reason"
                    } else {
                        skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.you_were_banned)
                    }
                    return@launch
                }

                if (!cfg.isServerEnabled || globalServerNotice?.type == ServerNoticeType.MAINTENANCE) {
                    bannerMessage = if (cfg.maintenanceMessage.isNotBlank()) cfg.maintenanceMessage else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_maintenance)
                    return@launch
                }
                if (server.rawVersion != 39 || BuildConfig.VERSION_CODE < cfg.minRequiredVersionCode || sys_menu_socket_repository.isVersionMismatch) {
                    bannerMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_outdated_version)
                    return@launch
                }

                if (!sys_menu_socket_repository.isServerOnline) {
                    bannerMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.notice_profile_server_unavailable)
                    return@launch
                }

                val isBrMode = server.gameMode.contains("Королевская", ignoreCase = true) || server.gameMode.contains("Battle Royale", ignoreCase = true) || server.rawNmap == -1
                val maxLimit = if (isBrMode) cfg.maxBrPlayers else cfg.maxPlayersPerServer

                // проверка лимита игроков
                if (server.playersCount >= maxLimit) {
                    bannerMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_is_full)
                    return@launch
                }
                when (serverCheck) {
                    ServerPreJoinStatus.SERVER_CLOSED -> {
                        bannerMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_is_closed)
                        doRefresh()
                        return@launch
                    }
                    ServerPreJoinStatus.WRONG_PASSWORD -> {
                        bannerMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.wrong_password)
                        return@launch
                    }
                    ServerPreJoinStatus.BANNED -> {
                        bannerMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.you_were_banned)
                        return@launch
                    }
                    ServerPreJoinStatus.FULL -> {
                        bannerMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.server_is_full)
                        return@launch
                    }
                    ServerPreJoinStatus.OK -> {}
                }

                if (server.isCustomMap && !server.mapUid.isNullOrEmpty()) {
                    val mapUid = server.mapUid.trim()
                    val fMap = java.io.File(context.filesDir, "${mapUid}.map")
                    val fMyMap = java.io.File(java.io.File(context.filesDir, "from_my_map"), "${mapUid}.map")
                    val fOnlineMap = java.io.File(java.io.File(context.filesDir, "from_online_map"), "${mapUid}.map")
                    val fMaps = java.io.File(java.io.File(context.filesDir, "maps"), "${mapUid}.map")
                    val mapExists = (fMap.exists() && fMap.length() > 0) ||
                                    (fMyMap.exists() && fMyMap.length() > 0) ||
                                    (fOnlineMap.exists() && fOnlineMap.length() > 0) ||
                                    (fMaps.exists() && fMaps.length() > 0)

                    if (!mapExists) {
                        val res = withContext(Dispatchers.IO) {
                            try {
                                sys_menu_socket_repository.getMapData(mapUid)
                            } catch (e: Exception) {
                                ""
                            }
                        }
                        if (res.startsWith("MAP_DATA_OK|")) {
                            val base64 = res.substringAfter("MAP_DATA_OK|")
                            withContext(Dispatchers.IO) {
                                try {
                                    MapSavior.unpackMapData(context, mapUid, base64)
                                    MapSavior.markMapAsDownloaded(context, mapUid)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }

                connectedOk = true
                MultiplayersLaunchGuard.markLaunched()
                val intent = android.content.Intent(context, datagame_main::class.java).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("ACTION_CONNECT_ONLINE_SERVER", true)
                    putExtra("IS_ADMIN", sys_menu_socket_repository.currentPlayerAdminStatus == 1)
                    putExtra("SERVER_IP", server.ip)
                    putExtra("SERVER_PORT", server.port)
                    putExtra("SERVER_NAME", server.ownerName)
                    putExtra("GAME_PASSWORD", pass.trim())
                    putExtra("ACCOUNT_ID", accountId)
                    putExtra("APP_LANGUAGE", AppLanguageManager.currentLanguage)
                    putExtra("IS_CUSTOM_MAP", server.isCustomMap)
                    putExtra("MAP_NAME", server.mapName)
                    putExtra("MAP_UID", server.mapUid)
                    putExtra("RAW_NMAP", server.rawNmap)
                }
                context.startActivity(intent)
                (context as? android.app.Activity)?.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                kotlinx.coroutines.delay(2500L)
                isConnectingServer = false
            } finally {
                if (!connectedOk) {
                    isConnectingServer = false
                    MultiplayersLaunchGuard.release()
                }
            }
        }
    }

    var dotCount by remember { mutableStateOf(2) }
    LaunchedEffect(showConnectingDialog) {
        if (showConnectingDialog) {
            while (true) {
                delay(400)
                dotCount = if (dotCount >= 4) 2 else dotCount + 1
            }
        }
    }
    val dotString = ".".repeat(dotCount)

    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            delay(3000)
            bannerMessage = null
        }
    }

    if (showPasswordInputByServer != null) {
        val server = showPasswordInputByServer!!
        UnifiedDialog(
            title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_parol_servera_eee8),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UnifiedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = AllowedChars.sanitize(it, 16) },
                        placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vvedite_parol_0d49),
                        isPassword = true,
                        transparentBorder = false,
                        inputFilter = { AllowedChars.sanitize(it, 16) },
                        regex = AllowedChars.REGEX,
                        maxLength = 16,
                        requestFocus = true
                    )
                }
            },
            confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.play),
            confirmEnabled = !isConnectingServer && sys_menu_socket_repository.isServerOnline && !GlobalReconnectState.isReconnecting && !GlobalReconnectState.showConnectionErrorDialog,
            onConfirm = {
                if (!isConnectingServer) {
                    val input = passwordInput
                    showPasswordInputByServer = null
                    launchConnectServer(server, input)
                }
            },
            onDismiss = { showPasswordInputByServer = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp, start = 0.dp, end = 0.dp, bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.PanelDark)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 20.dp, top = 7.4.dp, bottom = 7.4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(2.6f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(0.dp))
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_imya_d38d),
                                color = AppColors.TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.map1),
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1.7f)
                        )
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.game_type1),
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(2.1f)
                        )
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igroki_boty_135f),
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1.3f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Ver.",
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(0.65f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Ping",
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(0.65f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(AppColors.Overlay10)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(4.dp)
                    ) {
            if (isRefreshing) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    items(3) {
                        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.08f,
                            targetValue = 0.22f,
                            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                                animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
                                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AppColors.ShadowBase.copy(alpha = alpha))
                                .border(1.dp, AppColors.PanelSemiTransparent.copy(alpha = alpha))
                                .padding(start = 6.dp, end = 16.dp, top = 9.5.dp, bottom = 9.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(2.6f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f)))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1.7f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(2.1f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(1.3f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(0.65f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.weight(0.65f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f)))
                        }
                    }
                }
            } else {
                if (allServers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_netu_serverov_e3fc),
                            color = AppColors.TextDarkGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allServers.size) { index ->
                            val server = allServers[index]
                            val isSelected = selectedServer == server
                            val isAdminServer = server.status == 1
                            // цвет админ сервера
                            val adminColor = if (isAdminServer) Color(0xFF1FEC1F) else null

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isSelected) AppColors.PanelDark.lighten3() else AppColors.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) AppColors.BorderSemiTransparent else Color.Transparent,
                                            RoundedCornerShape(3.dp)
                                        )
                                        .clickable { selectedServer = server }
                                        .padding(start = 6.dp, end = 16.dp, top = 9.5.dp, bottom = 9.5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(2.6f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (server.requiresPassword) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .drawBehind {
                                                            val strokeWidth = 1.5.dp.toPx()
                                                            drawArc(
                                                                color = AppColors.RedMaterial,
                                                                startAngle = 180f,
                                                                sweepAngle = 180f,
                                                                useCenter = false,
                                                                style = Stroke(width = strokeWidth),
                                                                topLeft = androidx.compose.ui.geometry.Offset(2.dp.toPx(), 1.dp.toPx()),
                                                                size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 8.dp.toPx())
                                                            )
                                                            drawRect(
                                                                color = AppColors.RedMaterial,
                                                                topLeft = androidx.compose.ui.geometry.Offset(1.dp.toPx(), 4.5.dp.toPx()),
                                                                size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 5.5.dp.toPx())
                                                            )
                                                        }
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = server.ownerName,
                                            color = adminColor ?: AppColors.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Text(
                                        text = server.mapName,
                                        color = if (server.isCustomMap) Color(0xFF5FCFFF) else (adminColor ?: AppColors.TextGray),
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1.7f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = server.gameMode,
                                        color = adminColor ?: AppColors.TextGray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(2.1f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${server.playersCount} [${server.botsCount}]",
                                        color = adminColor ?: AppColors.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1.3f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = server.version,
                                        color = adminColor ?: AppColors.TextGray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.weight(0.65f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    val pingText = when {
                                        server.ping < 0 -> "???"
                                        server.ping >= 999 -> "999+"
                                        else -> "${server.ping}"
                                    }
                                    val pingColor = adminColor ?: when {
                                        server.ping < 0 -> AppColors.TextGray
                                        server.ping <= 100 -> AppColors.FpsPingGreen
                                        server.ping <= 220 -> AppColors.FpsPingYellow
                                        else -> AppColors.FpsPingRed
                                    }
                                    Text(
                                        text = pingText,
                                        color = pingColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.65f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Box(
                                    modifier = Modifier
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back),
                    color = AppColors.White,
                    fontSize = 14.sp
                )
            }
            
            val scope = rememberCoroutineScope()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                    .clickable {
                        doRefresh()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.refresh),
                    color = AppColors.White,
                    fontSize = 14.sp
                )
            }
            
            val isPlayEnabled = selectedServer != null && !isConnectingServer && sys_menu_socket_repository.isServerOnline && !GlobalReconnectState.isReconnecting && !GlobalReconnectState.showConnectionErrorDialog
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .menuActionButtonStyle(enabled = isPlayEnabled, shape = RoundedCornerShape(4.dp))
                    .clickable(enabled = isPlayEnabled) {
                        val server = selectedServer
                        if (server != null) {
                            if (server.requiresPassword) {
                                showPasswordInputByServer = server
                                passwordInput = ""
                            } else {
                                launchConnectServer(server)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                ShimmerPlayText(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.play),
                    isShimmering = isConnectingServer,
                    enabled = isPlayEnabled,
                    fontSize = 14.sp
                )
            }
        }
    }

    UnifiedToast(
        message = bannerMessage ?: "",
        visible = bannerMessage != null,
        modifier = Modifier.align(Alignment.TopCenter)
    )
}
}
