package skayrux.shootout2d

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import skayrux.shootout2d.MapObjectData
import skayrux.shootout2d.ObjectDatabase
import skayrux.shootout2d.ResourceCache
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun start_game_options(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val initialConfig = remember { ServerConfigManager.load(context) }
    var startMoney by remember { mutableStateOf(initialConfig.startMoney) }
    var roundTime by remember { mutableStateOf(initialConfig.roundTimeMinutes) }
    var currentPage by remember { mutableIntStateOf(0) }
    val objectsBitmap = remember { ResourceCache.getObjects(context) }
    
    val allItems = remember { ServerConfigManager.getOrderedItems() }
    val itemsPerPage = 15
    val pagedItems = allItems.chunked(itemsPerPage)
    val enabledItems = remember { mutableStateMapOf<String, Boolean>().apply { 
        allItems.forEach { put(it.name, !initialConfig.disabledItems.contains(it.name)) } 
    } }
    
    val defaultPistolIndex = allItems.indexOfFirst { it.name == "ob_p1" }.let { if (it != -1) it else allItems.indexOfFirst { item -> item.name == "ob_axe" } }
    val slotWeaponIndices = remember { 
        val s0 = if (initialConfig.slotWeapon0 != null) allItems.indexOfFirst { it.name == initialConfig.slotWeapon0 } else -1
        val s1 = if (initialConfig.slotWeapon1 != null) allItems.indexOfFirst { it.name == initialConfig.slotWeapon1 } else defaultPistolIndex
        val s2 = if (initialConfig.slotWeapon2 != null) allItems.indexOfFirst { it.name == initialConfig.slotWeapon2 } else -1
        mutableStateListOf<Int>(s0, s1, s2)
    }

    fun persistConfig() {
        val s0 = if (slotWeaponIndices[0] in allItems.indices) allItems[slotWeaponIndices[0]].name else null
        val s1 = if (slotWeaponIndices[1] in allItems.indices) allItems[slotWeaponIndices[1]].name else "ob_p1"
        val s2 = if (slotWeaponIndices[2] in allItems.indices) allItems[slotWeaponIndices[2]].name else null
        val disabled = enabledItems.filter { !it.value }.keys.toSet()
        val data = ServerConfigData(
            startMoney = startMoney,
            roundTimeMinutes = roundTime,
            slotWeapon0 = s0,
            slotWeapon1 = s1,
            slotWeapon2 = s2,
            disabledItems = disabled
        )
        ServerConfigManager.save(context, data)
    }

    // сохранение настроек с задержкой
    LaunchedEffect(startMoney, roundTime) {
        delay(350)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            persistConfig()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            persistConfig()
        }
    }

    val slotAllowedItems = mapOf(
        0 to listOf("ob_dr1", "ob_dr2", "ob_dr3", "ob_rifle", "ob_p90", "ob_famas", "ob_ak47", "ob_m16", "ob_aug", "ob_arbalet", "ob_vintorez", "ob_svd", "ob_osv96", "ob_minig", "ob_grl", "ob_rpg", "ob_ognemet"),
        1 to listOf("ob_p1", "ob_p2", "ob_p3", "ob_deagle", "ob_usi", "ob_knife", "ob_axe"),
        2 to listOf("ob_gr", "ob_oskgr", "ob_grslp", "ob_grdim", "ob_molotov", "ob_tur_k", "ob_sack_use", "ob_kor_use", "ob_wall_use", "ob_bug10", "ob_mina")
    )

    fun cycleSlot(slotIndex: Int) {
        val allowedNames = slotAllowedItems[slotIndex] ?: return
        val currentName = if (slotWeaponIndices[slotIndex] in allItems.indices) allItems[slotWeaponIndices[slotIndex]].name else null
        val currentIndexInAllowed = if (currentName != null) allowedNames.indexOf(currentName) else -1
        val nextIndexInAllowed = currentIndexInAllowed + 1
        
        if (nextIndexInAllowed < allowedNames.size) {
            val nextName = allowedNames[nextIndexInAllowed]
            slotWeaponIndices[slotIndex] = allItems.indexOfFirst { it.name == nextName }
        } else {
            if (slotIndex != 1) {
                slotWeaponIndices[slotIndex] = -1
            } else {
                val firstName = allowedNames.firstOrNull()
                slotWeaponIndices[slotIndex] = if (firstName != null) allItems.indexOfFirst { it.name == firstName } else -1
            }
        }
        persistConfig()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            val isSmallScreen = configuration.screenWidthDp < 760
            val weaponW = if (isSmallScreen) 76.dp else 90.dp
            val weaponH = if (isSmallScreen) 36.dp else 40.dp
            val btnW = if (isSmallScreen) 32.dp else 40.dp
            val btnH = if (isSmallScreen) 36.dp else 40.dp
            val boxW = if (isSmallScreen) 64.dp else 100.dp
            val boxH = if (isSmallScreen) 36.dp else 40.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(if (isSmallScreen) 4.dp else 6.dp)) {
                    slotWeaponIndices.forEachIndexed { index, weaponIndex ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.slot_format, index + 1), color = AppColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(weaponW, weaponH)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AppColors.TabInactive)
                                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                    .clickable { cycleSlot(index) },
                                contentAlignment = Alignment.Center
                            ) {
                                WeaponImage(if (weaponIndex != -1) allItems[weaponIndex] else null, isEnabled = true, objectsBitmap = objectsBitmap, isSlot = true, isSlot1 = (index == 0))
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.round_time), color = AppColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.TabInactive)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // уменьшение времени
                        SnappyRepeatingButton(
                            onClick = { if (roundTime > 2) roundTime -= 1 },
                            enabled = roundTime > 2,
                            modifier = Modifier.width(btnW).height(btnH)
                        ) {
                            Text("<", color = if (roundTime > 2) AppColors.White else AppColors.White.copy(alpha = 0.35f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier.width(boxW).height(boxH),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${roundTime}:00", color = AppColors.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        // увеличение времени
                        SnappyRepeatingButton(
                            onClick = { if (roundTime < 120) roundTime += 1 },
                            enabled = roundTime < 120,
                            modifier = Modifier.width(btnW).height(btnH)
                        ) {
                            Text(">", color = if (roundTime < 120) AppColors.White else AppColors.White.copy(alpha = 0.35f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.width(if (isSmallScreen) 4.dp else 10.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.money), color = AppColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppColors.TabInactive)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // уменьшение денег
                        SnappyRepeatingButton(
                            onClick = { if (startMoney > 100) startMoney -= 100 },
                            enabled = startMoney > 100,
                            modifier = Modifier.width(btnW).height(btnH)
                        ) {
                            Text("-", color = if (startMoney > 100) AppColors.White else AppColors.White.copy(alpha = 0.35f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier.width(boxW).height(boxH),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$startMoney$", color = AppColors.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        // увеличение денег
                        SnappyRepeatingButton(
                            onClick = { if (startMoney < 25000) startMoney += 100 },
                            enabled = startMoney < 25000,
                            modifier = Modifier.width(btnW).height(btnH)
                        ) {
                            Text("+", color = if (startMoney < 25000) AppColors.White else AppColors.White.copy(alpha = 0.35f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(0.dp))
            Text(
                skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.available_weapon), 
                modifier = Modifier.offset(y = 1.dp),
                color = AppColors.White, 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Bold
            )
 
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .offset(y = 1.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pagedItems.getOrNull(currentPage)?.chunked(5)?.forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowItems.forEach { item ->
                            val isEnabled = enabledItems[item.name] ?: true
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(AppShapes.Button)
                                    .clickable { 
                                        val newVal = !isEnabled
                                        enabledItems[item.name] = newVal 
                                        persistConfig()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                WeaponImage(item, isEnabled = isEnabled, objectsBitmap = objectsBitmap)
                            }
                        }
                        repeat(5 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        val canGoBack = currentPage > 0
        val canGoForward = currentPage < pagedItems.size - 1

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // кнопка переключения списка назад
            SnappyGameButton(
                text = "<",
                enabled = true,
                onClick = { if (canGoBack) currentPage-- },
                repeatable = true,
                modifier = Modifier.weight(1f).height(35.dp)
            )
            // кнопка сброса настроек по умолчанию
            SnappyGameButton(
                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.set_default),
                onClick = { 
                    startMoney = 100
                    roundTime = 5
                    currentPage = 0
                    enabledItems.keys.forEach { 
                        enabledItems[it] = true 
                    } 
                    slotWeaponIndices[0] = -1
                    slotWeaponIndices[1] = defaultPistolIndex
                    slotWeaponIndices[2] = -1
                    persistConfig()
                },
                modifier = Modifier.weight(1f).height(35.dp)
            )
            // кнопка возврата назад в меню
            SnappyGameButton(
                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back),
                onClick = onBack,
                modifier = Modifier.weight(1f).height(35.dp)
            )
            // кнопка переключения списка вперед
            SnappyGameButton(
                text = ">",
                enabled = true,
                onClick = { if (canGoForward) currentPage++ },
                repeatable = true,
                modifier = Modifier.weight(1f).height(35.dp)
            )
        }
    }
}

// кнопка с плавным повторением при удержании
@Composable
fun SnappyRepeatingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // сброс корутины при выключении кнопки
    LaunchedEffect(enabled) {
        if (!enabled) {
            repeatJob?.cancel()
            repeatJob = null
        }
    }

    // сброс корутины при уничтожении кнопки
    DisposableEffect(Unit) {
        onDispose {
            repeatJob?.cancel()
            repeatJob = null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        if (!currentEnabled) {
                            waitForUpOrCancellation()
                            continue
                        }
                        currentOnClick()
                        try {
                            val job = scope.launch {
                                delay(220)
                                while (isActive && currentEnabled) {
                                    delay(45)
                                    if (!isActive || !currentEnabled) break
                                    currentOnClick()
                                }
                            }
                            repeatJob = job
                            waitForUpOrCancellation()
                        } finally {
                            repeatJob?.cancel()
                            repeatJob = null
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
        content = content
    )
}

// кнопка игрового меню с поддержкой отключенного состояния
@Composable
fun SnappyGameButton(
    text: String,
    enabled: Boolean = true,
    containerColor: Color = AppColors.ActionButtonBg,
    borderColor: Color = AppColors.ActionButtonBorder,
    disabledContainerColor: Color = AppColors.ActionButtonDisabledBg,
    disabledBorderColor: Color = AppColors.ActionButtonDisabledBorder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    repeatable: Boolean = false
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    var repeatJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // сброс корутины при выключении кнопки
    LaunchedEffect(enabled) {
        if (!enabled) {
            repeatJob?.cancel()
            repeatJob = null
        }
    }

    // сброс корутины при уничтожении кнопки
    DisposableEffect(Unit) {
        onDispose {
            repeatJob?.cancel()
            repeatJob = null
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (enabled) containerColor else disabledContainerColor)
            .border(1.dp, if (enabled) borderColor else disabledBorderColor, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        if (!currentEnabled) {
                            waitForUpOrCancellation()
                            continue
                        }
                        currentOnClick()
                        if (repeatable) {
                            try {
                                val job = scope.launch {
                                    delay(220)
                                    while (isActive && currentEnabled) {
                                        delay(80)
                                        if (!isActive || !currentEnabled) break
                                        currentOnClick()
                                    }
                                }
                                repeatJob = job
                                waitForUpOrCancellation()
                            } finally {
                                repeatJob?.cancel()
                                repeatJob = null
                            }
                        } else {
                            waitForUpOrCancellation()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
            fontSize = 14.sp
        )
    }
}

// отрисовка оружия
@Composable
private fun WeaponImage(
    item: MapObjectData?,
    isEnabled: Boolean,
    objectsBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier = Modifier,
    isSlot: Boolean = false,
    isSlot1: Boolean = false
) {
    val darkBg = if (isSlot) AppColors.TabInactive else {
        if (!isEnabled) {
            Color(0x30000000)
        } else {
            AppColors.MultiplayersCellBg
        }
    }
    val borderColor = if (isSlot) AppColors.Border else {
        if (!isEnabled) {
            if (LiveBackgroundState.isMenuVisible) Color(0x20FFFFFF) else Color(0xFF474C58)
        } else {
            AppColors.MultiplayersCellBorder
        }
    }
    
    val boxModifier = modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(4.dp))
        .background(darkBg)
        .border(1.dp, borderColor, RoundedCornerShape(4.dp))

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        if (item != null && objectsBitmap != null) {
            Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                val maxDimension = maxOf(item.atlasW, item.atlasH)
                val containerMinDimension = minOf(size.width, size.height)
                val targetFraction = if (item.name == "ob_car1") {
                    1.04f
                } else if (item.name == "ob_car2") {
                    1.35f
                } else if (item.name == "ob_oskgr") {
                    0.58f
                } else if (item.name in listOf("ob_gr", "ob_grdim", "ob_grslp")) {
                    0.70f
                } else if (item.name == "ob_armor") {
                    0.72f
                } else if (item.name == "ob_deagle") {
                    1.09f
                } else if (item.name in listOf(
                        "ob_dr1", "ob_dr2", "ob_rifle", "ob_osv96", "ob_rpg", "ob_svd", "ob_m16"
                    )) {
                    1.55f
                } else if (item.name == "ob_aug") {
                    1.41f
                } else if (item.name in listOf("ob_minig", "ob_ak47")) {
                    1.36f
                } else if (item.name in listOf(
                        "ob_dr3", "ob_arbalet", "ob_grl",
                        "ob_ognemet", "ob_vintorez", "ob_famas"
                    )) {
                    1.31f
                } else if (item.name == "ob_p90") {
                    1.26f
                } else if (item.name in listOf("ob_axe", "ob_knife")) {
                    1.10f
                } else if (item.name == "ob_p3") {
                    1.06f
                } else if (item.name in listOf("ob_p1", "ob_p2")) {
                    0.90f
                } else if (item.name == "ob_usi") {
                    0.93f
                } else if (item.name == "ob_apt") {
                    0.77f
                } else {
                    0.85f
                }
                var scale = (containerMinDimension * targetFraction) / maxDimension
                if (isSlot) {
                    scale *= if (isSlot1) 1.15f else 1.10f
                }
                
                val dstW = (item.atlasW * scale).toInt()
                val dstH = (item.atlasH * scale).toInt()
                val dx = (size.width - dstW) / 2f
                val yOffset = if (item.name == "ob_famas") -size.height * 0.09f else if (item.name == "ob_ak47") size.height * 0.05f else 0f
                val dy = (size.height - dstH) / 2f + yOffset
                
                if (item.name == "ob_car1" || item.name == "ob_car2") {
                    val centerPoint = this.center
                    withTransform({
                        rotate(-90f, centerPoint)
                    }) {
                        drawImage(
                            image = objectsBitmap,
                            srcOffset = IntOffset(item.atlasX, item.atlasY),
                            srcSize = IntSize(item.atlasW, item.atlasH),
                            dstOffset = IntOffset(dx.toInt(), dy.toInt()),
                            dstSize = IntSize(dstW, dstH),
                            alpha = if (isEnabled) 1.0f else 0.40f
                        )
                    }
                } else {
                    drawImage(
                        image = objectsBitmap,
                        srcOffset = IntOffset(item.atlasX, item.atlasY),
                        srcSize = IntSize(item.atlasW, item.atlasH),
                        dstOffset = IntOffset(dx.toInt(), dy.toInt()),
                        dstSize = IntSize(dstW, dstH),
                        alpha = if (isEnabled) 1.0f else 0.40f
                    )
                }
            }
        } else {
            Text("—", color = AppColors.Gray, fontSize = 20.sp, modifier = Modifier.padding(2.dp))
        }
    }
}
