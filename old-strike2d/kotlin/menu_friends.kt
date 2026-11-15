package skayrux.shootout2d

import androidx.compose.material.icons.filled.Block
import androidx.compose.ui.window.Popup


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset

import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.MarkUnreadChatAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius






fun getRobustOnlineStatus(lastSeenStr: String): String {
    return sys_menu_socket_repository.getUnifiedOnlineStatus(lastSeenStr)
}

fun formatChatTime(timestamp: String): String {
    if (timestamp.isBlank()) return ""
    return try {
        val timePart = if (timestamp.contains(" ")) timestamp.substringAfter(" ").trim() else timestamp.trim()
        val colonParts = timePart.split(":")
        if (colonParts.size >= 2) {
            val h = colonParts[0].takeLast(2)
            val m = colonParts[1].take(2)
            "[$h:$m]"
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}

fun formatChatDate(timestamp: String): String {
    if (timestamp.isBlank()) return ""
    return try {
        val datePart = if (timestamp.contains(" ")) timestamp.substringBefore(" ").trim() else timestamp.trim()
        val dateSplits = if (datePart.contains("-")) datePart.split("-") else if (datePart.contains(".")) datePart.split(".") else emptyList()
        if (dateSplits.size == 3) {
            val (year, month, day) = if (datePart.contains("-")) {
                Triple(dateSplits[0].toIntOrNull() ?: 2026, dateSplits[1].toIntOrNull() ?: 1, dateSplits[2].toIntOrNull() ?: 1)
            } else {
                val y = dateSplits[2].toIntOrNull() ?: 2026
                val fullY = if (y < 100) 2000 + y else y
                Triple(fullY, dateSplits[1].toIntOrNull() ?: 1, dateSplits[0].toIntOrNull() ?: 1)
            }
            val monthName = when (month) {
                1 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_yanvarya_d861)
                2 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_fevralya_fcf9)
                3 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_marta_bb77)
                4 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_aprelya_2b5a)
                5 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_maya_4dbb)
                6 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_iyunya_adcb)
                7 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_iyulya_3236)
                8 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_avgusta_e3aa)
                9 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sentyabrya_a146)
                10 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_oktyabrya_7abd)
                11 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_noyabrya_6e78)
                12 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_dekabrya_29cc)
                else -> ""
            }
            if (monthName.isNotEmpty()) {
                "$day $monthName $year"
            } else {
                datePart
            }
        } else {
            ""
        }
    } catch (e: Exception) {
        ""
    }
}


@Composable
fun FriendsTabButton(
    text: String,
    isActive: Boolean,
    notificationColor: Color? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(30.dp)
            .width(110.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) AppColors.TabActive else AppColors.TabInactive)
            .border(1.dp, if (isActive) AppColors.BorderActive else AppColors.Border, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .drawWithContent {
                drawContent()
                if (notificationColor != null) {
                    val w = size.width * 0.6f
                    val h = 1.3.dp.toPx()
                    val x = (size.width - w) / 2f
                    val y = size.height - h - 1.2.dp.toPx()
                    drawRoundRect(
                        color = notificationColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                }
            }
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = AppColors.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun FriendsTabIconButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(30.dp)
            .width(110.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isActive) AppColors.TabActive else AppColors.TabInactive)
            .border(1.dp, if (isActive) AppColors.BorderActive else AppColors.Border, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                color = AppColors.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

object MenuFriendsState {
    var isAnyDialogOpen by mutableStateOf(false)
    var isSearchFieldFocused by mutableStateOf(false)
}

// глобальный кэш поиска друзей вне жизненного цикла экрана
object FriendsSearchGlobalCache {
    var cachedResults: List<PlayerProfile> = emptyList()
    var lastLoadTime: Long = 0L
    private const val TTL_MS = 5000L

    fun isValid(): Boolean {
        val now = android.os.SystemClock.elapsedRealtime()
        return cachedResults.isNotEmpty() && (now - lastLoadTime < TTL_MS)
    }

    fun update(results: List<PlayerProfile>) {
        cachedResults = results
        lastLoadTime = android.os.SystemClock.elapsedRealtime()
    }

    fun clear() {
        cachedResults = emptyList()
        lastLoadTime = 0L
    }
}

@Composable
fun StatusLoadingBar(modifier: Modifier = Modifier, width: androidx.compose.ui.unit.Dp = 48.dp, height: androidx.compose.ui.unit.Dp = 8.dp) {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "status_loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.25f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(800, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(AppColors.Gray.copy(alpha = alpha))
    )
}

@Composable
fun PlayerPlateStyle(
    id: String,
    name: String,
    status: String,
    banStatus: Int = 0,
    modifier: Modifier = Modifier
) {
    val isOnline = status == "Online" || status.startsWith("Online") || status == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba)
    
    val profile = sys_menu_socket_repository.getProfileById(id)
    val friendObj = sys_menu_socket_repository.friendsDetails.find { it.id == id }
    val isBanned = banStatus == 1 || profile?.banStatus == 1 || friendObj?.banStatus == 1

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            userId = id,
            userName = name,
            sizeDp = 34,
            fontSizeSp = 18,
            shape = RoundedCornerShape(4.dp),
            isBanned = isBanned
        )
        Column(
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "ID: $id",
                color = AppColors.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
            PlayerNameWithBan(
                name = name,
                isBanned = isBanned,
                baseColor = AppColors.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            if (status.isNotEmpty()) {
                Text(
                    text = status,
                    color = if (isOnline) AppColors.OnlineGreen else AppColors.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            } else {
                Spacer(Modifier.height(2.dp))
                StatusLoadingBar()
            }
        }
    }
}


@Composable
fun GroupPlateStyle(
    id: String,
    name: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupAvatar(
            groupId = id,
            groupName = name,
            sizeDp = 34,
            fontSizeSp = 18,
            shape = RoundedCornerShape(4.dp)
        )
        Column(
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                text = "ID: $id",
                color = AppColors.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    color = AppColors.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Text(
                text = if (description.isNotEmpty()) description else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_bez_opisaniya_c6b0),
                color = AppColors.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

// оптимизированный расчет плавного исчезания элемента у краев
@Composable
fun FadingLazyColumnItem(
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    key: Any,
    index: Int,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.graphicsLayer {
            val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
            val itemInfo = if (visibleItems.isNotEmpty()) {
                val candidateIdx = index - visibleItems[0].index
                if (candidateIdx in visibleItems.indices && visibleItems[candidateIdx].key == key) {
                    visibleItems[candidateIdx]
                } else {
                    visibleItems.find { it.key == key }
                }
            } else null

            this.alpha = if (itemInfo != null) {
                val itemTop = itemInfo.offset
                val itemBottom = itemInfo.offset + itemInfo.size
                val viewportTop = 0
                val viewportBottom = lazyListState.layoutInfo.viewportEndOffset

                val visibleSize = (minOf(itemBottom, viewportBottom) - maxOf(itemTop, viewportTop)).coerceAtLeast(0)
                val visibilityFactor = if (itemInfo.size > 0) visibleSize.toFloat() / itemInfo.size.toFloat() else 1f

                if (visibilityFactor < 0.6f) {
                    (visibilityFactor / 0.6f).coerceIn(0f, 1f)
                } else {
                    1f
                }
            } else {
                1f
            }
        }
    ) {
        content()
    }
}

@Composable
fun FriendsTabContent(
    friendsDetails: List<FriendData>,
    selectedFriendId: String,
    onSelectedFriendIdChange: (String) -> Unit,
    playerName: String,
    onShowFriendInfo: (String, String, String, Int, String?) -> Unit,
    onShowDeleteFriendConfirm: () -> Unit,
    friendsMessagesMap: Map<String, List<ChatMessage>>,
    isFriendMessagesLoading: Boolean,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    chatFocusRequester: FocusRequester
) {
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(0.23f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, AppColors.Divider)
                    .padding(4.dp),
                contentAlignment = Alignment.TopStart
            ) {
                if (friendsDetails.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "—",
                            color = AppColors.Gray,
                            fontSize = 14.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    val lazyListState = rememberLazyListState()
                    val sortedFriends = friendsDetails
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState
                    ) {
                        itemsIndexed(sortedFriends, key = { _, friend -> friend.id }) { index, friend ->
                            FadingLazyColumnItem(lazyListState, friend.id, index) {
                                val isSelected = (selectedFriendId == friend.id)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectedFriendIdChange(friend.id) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isSelected) AppColors.TabActive else AppColors.Transparent)
                                            .border(1.dp, if (isSelected) AppColors.BorderActive else Color.Transparent)
                                            .padding(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        PlayerPlateStyle(
                                            id = friend.id,
                                            name = friend.name,
                                            status = getRobustOnlineStatus(friend.lastSeen),
                                            banStatus = friend.banStatus,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val unreadCount = if (friend.id == selectedFriendId) 0 else friend.unreadSmsCount.coerceAtLeast(sys_menu_socket_repository.getFriendUnreadCount(friend.id))
                                        if (unreadCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(end = 4.dp)
                                                    .background(AppColors.TabInactive, RoundedCornerShape(4.dp))
                                                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 7.5.dp, vertical = 3.5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (unreadCount > 99) "99" else "$unreadCount",
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        color = AppColors.GoldOrange,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    Spacer(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(AppColors.Border)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(AppColors.TabInactive)
                        .border(1.dp, AppColors.Border)
                        .clickable {
                            if (selectedFriendId.isNotEmpty()) {
                                val friend = friendsDetails.find { it.id == selectedFriendId }
                                if (friend != null && selectedFriendId != sys_menu_socket_repository.currentPlayerId) {
                                    onShowFriendInfo(friend.id, friend.name, friend.lastSeen, friend.banStatus, friend.banReason)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    iconInfo(modifier = Modifier.size(20.dp), outlined = true)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .background(AppColors.TabInactive)
                        .border(1.dp, AppColors.Border)
                        .clickable {
                            if (selectedFriendId.isNotEmpty()) {
                                onShowDeleteFriendConfirm()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CustomGroupIcons.IconDelete,
                        contentDescription = null,
                        tint = AppColors.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        Column(
            modifier = Modifier
                .weight(0.77f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, AppColors.Divider)
                    .clipToBounds(),
                contentAlignment = Alignment.TopStart
            ) {
                if (selectedFriendId.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vyberite_druga_dlya_obscheniya_7dd1), color = AppColors.Gray, fontSize = 14.sp)
                    }
                } else if (isFriendMessagesLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AppColors.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.loading),
                                color = AppColors.TextGray,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    val messages = friendsMessagesMap[selectedFriendId] ?: emptyList()
                    val msgCount = messages.size
                    
                    androidx.compose.runtime.key(selectedFriendId) {
                        val lazyListState = rememberLazyListState()
                        var prevMsgCount by remember { mutableStateOf(0) }
                        
                        LaunchedEffect(msgCount) {
                            if (msgCount > 0) {
                                val isMyMessage = messages.last().isMe
                                if (prevMsgCount == 0) {
                                    lazyListState.scrollToItem(msgCount - 1)
                                } else if (isMyMessage) {
                                    lazyListState.animateScrollToItem(msgCount - 1)
                                } else if (msgCount > prevMsgCount) {
                                    val lastVisibleIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    if (prevMsgCount - 1 - lastVisibleIndex <= 7) {
                                        lazyListState.animateScrollToItem(msgCount - 1)
                                    }
                                }
                            }
                            prevMsgCount = msgCount
                        }
    
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState,
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            itemsIndexed(
                                messages,
                                key = { index, msg -> "${msg.sender}_${msg.timestamp}_$index" }
                            ) { index, msg ->
                                val showDateHeader = remember(index, messages) {
                                    val currDate = formatChatDate(msg.timestamp)
                                    if (currDate.isEmpty()) false
                                    else if (index == 0) true
                                    else {
                                        val prevDate = formatChatDate(messages[index - 1].timestamp)
                                        currDate != prevDate
                                    }
                                }
                                if (showDateHeader) {
                                    val dateText = formatChatDate(msg.timestamp)
                                    if (dateText.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = if (index == 0) 4.dp else 12.dp, bottom = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dateText,
                                                color = AppColors.TextGray.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                val timeStr = formatChatTime(msg.timestamp)
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(style = SpanStyle(
                                            color = if (msg.isMe) AppColors.Success else AppColors.WarningYellow,
                                            fontWeight = FontWeight.Normal
                                        )) {
                                            append("${msg.sender}: ")
                                        }
                                        withStyle(style = SpanStyle(
                                            color = Color(0xFFD2D4D6),
                                            fontWeight = FontWeight.Light
                                        )) {
                                            append(msg.text)
                                        }
                                        if (timeStr.isNotEmpty()) {
                                            withStyle(style = SpanStyle(
                                                color = AppColors.TextGray.copy(alpha = 0.6f),
                                                fontWeight = FontWeight.Normal,
                                                fontSize = 12.sp
                                            )) {
                                                append(" $timeStr")
                                            }
                                        }
                                    },
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(AppColors.TabInactive)
                        .border(1.dp, AppColors.Border),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val currentMessageTextState = androidx.compose.runtime.rememberUpdatedState(messageText)
                    val onSendMessageState = androidx.compose.runtime.rememberUpdatedState(onSendMessage)
                    val onMessageTextChangeState = androidx.compose.runtime.rememberUpdatedState(onMessageTextChange)
                    androidx.compose.ui.viewinterop.AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            android.widget.EditText(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setTextColor(android.graphics.Color.WHITE)
                                setHintTextColor(android.graphics.Color.parseColor("#808080"))
                                hint = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.enter_your_message)
                                background = null
                                val pxHorizontal = (10 * ctx.resources.displayMetrics.density).toInt()
                                val pxVertical = (6 * ctx.resources.displayMetrics.density).toInt()
                                setPadding(pxHorizontal, pxVertical, pxHorizontal, pxVertical)
                                textSize = 14.2f
                                setSingleLine(true)
                                maxLines = 1
                                isFocusable = true
                                isFocusableInTouchMode = true
                                onFocusChangeListener = android.view.View.OnFocusChangeListener { v, hasFocus ->
                                    if (hasFocus) {
                                        val imm = ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                                        imm?.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                                    }
                                }
                                gravity = android.view.Gravity.CENTER_VERTICAL
                                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                                setOnEditorActionListener { v, actionId, _ ->
                                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                                        onSendMessageState.value()
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
                                        if (currentText != currentMessageTextState.value) {
                                            onMessageTextChangeState.value(currentText)
                                        }
                                    }
                                })
                            }
                        },
                        update = { editText ->
                            if (editText.text.toString() != messageText) {
                                val selection = editText.selectionStart
                                editText.setText(messageText)
                                editText.setSelection(selection.coerceIn(0, messageText.length))
                            }
                        }
                    )
                }

                val sendEnabled = selectedFriendId.isNotEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(100.dp)
                        .background(if (sendEnabled) AppColors.ActionButtonBg else AppColors.ActionButtonDisabledBg)
                        .border(1.dp, if (sendEnabled) AppColors.ActionButtonBorder else AppColors.ActionButtonDisabledBorder)
                        .clickable(enabled = sendEnabled) {
                            onSendMessage()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.send),
                        color = if (sendEnabled) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun friends_menu(playerName: String, initialTab: String = "friends") {
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            kotlinx.coroutines.delay(3000)
            toastMessage = null
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    var noGroupInvites by remember {
        mutableStateOf(sys_menu_socket_repository.noGroupInvites)
    }
    LaunchedEffect(sys_menu_socket_repository.noGroupInvites) {
        noGroupInvites = sys_menu_socket_repository.noGroupInvites
    }
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    var messageText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedFriendId by remember { mutableStateOf("") }
    val chatFocusRequester = remember { FocusRequester() }
    var localSentRequests by remember { mutableStateOf(setOf<String>()) }
    var friendsMessagesMap by remember { mutableStateOf(sys_menu_socket_repository.friendsMessagesMap) }
    LaunchedEffect(friendsMessagesMap) { sys_menu_socket_repository.friendsMessagesMap = friendsMessagesMap }
    LaunchedEffect(sys_menu_socket_repository.friendsMessagesMap) { friendsMessagesMap = sys_menu_socket_repository.friendsMessagesMap }

    LaunchedEffect(selectedFriendId, activeTab) {
        sys_menu_socket_repository.activeDirectChatFriendId = if (activeTab == "friends") selectedFriendId else ""
    }

    DisposableEffect(Unit) {
        onDispose {
            sys_menu_socket_repository.activeDirectChatFriendId = ""
        }
    }

    var isFriendMessagesLoading by remember { mutableStateOf(false) }
    LaunchedEffect(selectedFriendId) {
        if (selectedFriendId.isNotEmpty()) {
            val cached = ChatCacheManager.loadPersonalMessages(context, selectedFriendId)
            if (cached.isNotEmpty()) {
                friendsMessagesMap = friendsMessagesMap + (selectedFriendId to cached)
                isFriendMessagesLoading = false
            } else {
                isFriendMessagesLoading = true
            }
            try {
                val msgs = sys_menu_socket_repository.fetchPersonalMessagesOnline(selectedFriendId)
                friendsMessagesMap = friendsMessagesMap + (selectedFriendId to msgs)
                ChatCacheManager.savePersonalMessages(context, selectedFriendId, msgs)
                sys_menu_socket_repository.markDirectChatAsRead(selectedFriendId, msgs.size)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isFriendMessagesLoading = false
            }
        }
    }

    LaunchedEffect(sys_menu_socket_repository.friendsDetails) {
        if (selectedFriendId.isNotEmpty() && !sys_menu_socket_repository.friendsDetails.any { it.id == selectedFriendId }) {
            selectedFriendId = ""
        }
    }

    var groups by remember { mutableStateOf(sys_menu_socket_repository.groups) }
    LaunchedEffect(groups) { sys_menu_socket_repository.groups = groups }
    LaunchedEffect(sys_menu_socket_repository.groups) { groups = sys_menu_socket_repository.groups }

    var userJoinedGroup by remember { mutableStateOf(sys_menu_socket_repository.userJoinedGroup) }
    LaunchedEffect(userJoinedGroup) { sys_menu_socket_repository.userJoinedGroup = userJoinedGroup }
    LaunchedEffect(sys_menu_socket_repository.userJoinedGroup) { userJoinedGroup = sys_menu_socket_repository.userJoinedGroup }

    LaunchedEffect(groups) {
        if (userJoinedGroup != null && !groups.any { it.id == userJoinedGroup }) {
            userJoinedGroup = null
        }
    }

    var groupSubTab by remember { mutableStateOf("messages") }
    var groupInputText by remember { mutableStateOf("") }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var isAskingCreateGroup by remember { mutableStateOf(false) }
    var isAskingGroupAvatar by remember { mutableStateOf(false) }
    var groupCooldownTickMs by remember { mutableStateOf(System.currentTimeMillis()) }
    val createGroupCooldownSeconds = sys_menu_socket_repository.getCreateGroupCooldownRemainingSec(groupCooldownTickMs)
    val groupAvatarCooldownSeconds = sys_menu_socket_repository.getGroupAvatarCooldownRemainingSec(groupCooldownTickMs)

    LaunchedEffect(sys_menu_socket_repository.createGroupCooldownUntilMs, sys_menu_socket_repository.groupAvatarCooldownUntilMs) {
        while (
            sys_menu_socket_repository.getCreateGroupCooldownRemainingSec(System.currentTimeMillis()) > 0 ||
            sys_menu_socket_repository.getGroupAvatarCooldownRemainingSec(System.currentTimeMillis()) > 0
        ) {
            groupCooldownTickMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
        groupCooldownTickMs = System.currentTimeMillis()
    }

    var showGroupAvatarDialog by remember { mutableStateOf(false) }
    var selectedGroupAvatarUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var isGroupAvatarPickerLaunching by remember { mutableStateOf(false) }
    var lastGroupAvatarPickerLaunchTime by remember { mutableStateOf(0L) }
    val groupAvatarPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        isGroupAvatarPickerLaunching = false
        if (uri != null) {
            selectedGroupAvatarUri = uri
        }
    }

    var groupDescriptionsMap by remember { mutableStateOf(sys_menu_socket_repository.groupDescriptionsMap) }
    LaunchedEffect(groupDescriptionsMap) { sys_menu_socket_repository.groupDescriptionsMap = groupDescriptionsMap }
    LaunchedEffect(sys_menu_socket_repository.groupDescriptionsMap) { groupDescriptionsMap = sys_menu_socket_repository.groupDescriptionsMap }

    var groupCreatedTimeMap by remember { mutableStateOf(sys_menu_socket_repository.groupCreatedTimeMap) }
    LaunchedEffect(groupCreatedTimeMap) { sys_menu_socket_repository.groupCreatedTimeMap = groupCreatedTimeMap }
    LaunchedEffect(sys_menu_socket_repository.groupCreatedTimeMap) { groupCreatedTimeMap = sys_menu_socket_repository.groupCreatedTimeMap }

    var groupMessagesMap by remember { mutableStateOf(sys_menu_socket_repository.groupMessagesMap) }
    LaunchedEffect(groupMessagesMap) { sys_menu_socket_repository.groupMessagesMap = groupMessagesMap }
    LaunchedEffect(sys_menu_socket_repository.groupMessagesMap) { groupMessagesMap = sys_menu_socket_repository.groupMessagesMap }

    var groupMembersWithRolesMap by remember { mutableStateOf(sys_menu_socket_repository.groupMembersMap) }
    LaunchedEffect(groupMembersWithRolesMap) { sys_menu_socket_repository.groupMembersMap = groupMembersWithRolesMap }
    LaunchedEffect(sys_menu_socket_repository.groupMembersMap) { groupMembersWithRolesMap = sys_menu_socket_repository.groupMembersMap }

    var isMessagesLoading by remember { mutableStateOf(false) }

    LaunchedEffect(userJoinedGroup) {
        val gId = userJoinedGroup
        if (gId != null) {
            val cached = ChatCacheManager.loadGroupMessages(context, gId)
            if (cached.isNotEmpty()) {
                groupMessagesMap = groupMessagesMap + (gId to cached)
                isMessagesLoading = false
            } else {
                isMessagesLoading = true
            }
            try {
                val members = sys_menu_socket_repository.getGroupMembers(gId)
                groupMembersWithRolesMap = groupMembersWithRolesMap + (gId to members)
                
                val msgs = sys_menu_socket_repository.getGroupMessages(gId)
                val chatMsgs = msgs.mapNotNull { msg ->
                    try {
                        val decodedBytes = android.util.Base64.decode(msg.textBase64, android.util.Base64.DEFAULT)
                        ChatMessage(msg.senderName, String(decodedBytes, Charsets.UTF_8), msg.senderId == sys_menu_socket_repository.currentPlayerId, msg.senderId == "system", timestamp = msg.timestamp)
                    } catch (e: Exception) {
                        null
                    }
                }
                groupMessagesMap = groupMessagesMap + (gId to chatMsgs)
                ChatCacheManager.saveGroupMessages(context, gId, chatMsgs)
                if (activeTab == "groups" && groupSubTab == "messages") {
                    sys_menu_socket_repository.markGroupChatAsRead(gId, chatMsgs.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isMessagesLoading = false
            }
        }
    }

    var groupCreators by remember { mutableStateOf(sys_menu_socket_repository.groupCreators) }
    LaunchedEffect(groupCreators) { sys_menu_socket_repository.groupCreators = groupCreators }
    LaunchedEffect(sys_menu_socket_repository.groupCreators) { groupCreators = sys_menu_socket_repository.groupCreators }

    var showLeaveGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var deleteCountdown by remember { mutableStateOf(2) }
    LaunchedEffect(showDeleteGroupDialog) {
        if (showDeleteGroupDialog) {
            deleteCountdown = 2
            while (deleteCountdown > 0) {
                kotlinx.coroutines.delay(1000)
                deleteCountdown--
            }
        }
    }
    var statusTargetMember by remember { mutableStateOf<GroupMember?>(null) }
    var showChangeStatusDialog by remember { mutableStateOf(false) }
    var kickTargetMember by remember { mutableStateOf<GroupMember?>(null) }
    var showKickConfirmDialog by remember { mutableStateOf(false) }
    var infoTargetMember by remember { mutableStateOf<GroupMember?>(null) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    var newMemberNameText by remember { mutableStateOf("") }
    var isSelectingFriendToAdd by remember { mutableStateOf(false) }
    var addingMemberIds by remember { mutableStateOf(setOf<String>()) }
    var showFriendInfoDialog by remember { mutableStateOf(false) }
    var showDeleteFriendConfirmDialog by remember { mutableStateOf(false) }
    var infoTargetFriendId by remember { mutableStateOf("") }
    var infoTargetFriendName by remember { mutableStateOf("") }
    var infoTargetFriendStatus by remember { mutableStateOf("") }
    var infoTargetFriendBanStatus by remember { mutableStateOf(0) }
    var infoTargetFriendBanReason by remember { mutableStateOf<String?>(null) }
    var showAcceptAllRequestsConfirmDialog by remember { mutableStateOf(false) }
    var showDeclineAllRequestsConfirmDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(
        showCreateGroupDialog, showLeaveGroupDialog, showDeleteGroupDialog,
        showChangeStatusDialog, showKickConfirmDialog, showAddMemberDialog,
        showFriendInfoDialog, showDeleteFriendConfirmDialog, showGroupAvatarDialog,
        showAcceptAllRequestsConfirmDialog, showDeclineAllRequestsConfirmDialog
    ) {
        MenuFriendsState.isAnyDialogOpen = showCreateGroupDialog || showLeaveGroupDialog ||
                showDeleteGroupDialog || showChangeStatusDialog || showKickConfirmDialog ||
                showAddMemberDialog || showFriendInfoDialog || showDeleteFriendConfirmDialog ||
                showGroupAvatarDialog || showAcceptAllRequestsConfirmDialog || showDeclineAllRequestsConfirmDialog
    }
    LaunchedEffect(activeTab, selectedFriendId, userJoinedGroup, groupSubTab) {
        delay(200)
        try {
            sys_menu_socket_repository.fetchFriendsOnline()
            sys_menu_socket_repository.fetchFriendRequestsOnline()
            if (activeTab == "search") {
                sys_menu_socket_repository.fetchSentRequestsOnline()
            }
            if (activeTab == "groups") {
                val myGroups = sys_menu_socket_repository.getMyGroups()
                groups = myGroups
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
        }

        while (true) {
            try {
                if (kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]?.isActive == false) {
                    throw kotlinx.coroutines.CancellationException("Cancelled")
                }
                sys_menu_socket_repository.fetchFriendsOnline()
                sys_menu_socket_repository.fetchFriendRequestsOnline()
                
                if (activeTab == "search") {
                    sys_menu_socket_repository.fetchSentRequestsOnline()
                }

                if (activeTab == "friends" && selectedFriendId.isNotEmpty()) {
                    val msgs = sys_menu_socket_repository.fetchPersonalMessagesOnline(selectedFriendId)
                    friendsMessagesMap = friendsMessagesMap + (selectedFriendId to msgs)
                    ChatCacheManager.savePersonalMessages(context, selectedFriendId, msgs)
                    sys_menu_socket_repository.markDirectChatAsRead(selectedFriendId, msgs.size)
                }

                if (activeTab == "groups") {
                    val myGroups = sys_menu_socket_repository.getMyGroups()
                    groups = myGroups
                    myGroups.forEach { group ->
                        if (!sys_menu_socket_repository.groupAvatarsMap.containsKey(group.id)) {
                            launch {
                                try {
                                    sys_menu_socket_repository.getGroupAvatar(group.id)
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: Exception) {}
                            }
                        }
                        if (!sys_menu_socket_repository.groupMessagesMap.containsKey(group.id)) {
                            launch {
                                try {
                                    val cached = ChatCacheManager.loadGroupMessages(context, group.id)
                                    groupMessagesMap = groupMessagesMap + (group.id to cached)
                                    if (sys_menu_socket_repository.readGroupMessagesCountMap[group.id] == null) {
                                        sys_menu_socket_repository.readGroupMessagesCountMap = sys_menu_socket_repository.readGroupMessagesCountMap + (group.id to cached.size)
                                    }
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: Exception) {}
                            }
                        }
                    }
                    val newDescMap = myGroups.associate { it.id to it.description }
                    groupDescriptionsMap = groupDescriptionsMap + newDescMap
                    myGroups.forEach { group ->
                        val hasUnreadOnServer = sys_menu_socket_repository.unreadSmsIds.contains(group.id)
                        if (hasUnreadOnServer && group.id != userJoinedGroup) {
                            launch {
                                try {
                                    val msgs = sys_menu_socket_repository.getGroupMessages(group.id)
                                    val chatMsgs = msgs.mapNotNull { msg ->
                                        try {
                                            val decodedBytes = android.util.Base64.decode(msg.textBase64, android.util.Base64.DEFAULT)
                                            ChatMessage(msg.senderName, String(decodedBytes, Charsets.UTF_8), msg.senderId == sys_menu_socket_repository.currentPlayerId, msg.senderId == "system", timestamp = msg.timestamp)
                                        } catch (e: kotlinx.coroutines.CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    groupMessagesMap = groupMessagesMap + (group.id to chatMsgs)
                                } catch (e: kotlinx.coroutines.CancellationException) {
                                    throw e
                                } catch (e: Exception) {}
                            }
                        }
                    }
                    val gId = userJoinedGroup
                    if (gId != null) {
                        val members = sys_menu_socket_repository.getGroupMembers(gId)
                        groupMembersWithRolesMap = groupMembersWithRolesMap + (gId to members)
                        
                        val msgs = sys_menu_socket_repository.getGroupMessages(gId)
                        val chatMsgs = msgs.mapNotNull { msg ->
                            try {
                                val decodedBytes = android.util.Base64.decode(msg.textBase64, android.util.Base64.DEFAULT)
                                ChatMessage(msg.senderName, String(decodedBytes, Charsets.UTF_8), msg.senderId == sys_menu_socket_repository.currentPlayerId, msg.senderId == "system", timestamp = msg.timestamp)
                            } catch (e: kotlinx.coroutines.CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                null
                            }
                        }
                        groupMessagesMap = groupMessagesMap + (gId to chatMsgs)
                        ChatCacheManager.saveGroupMessages(context, gId, chatMsgs)
                        if (groupSubTab == "messages") {
                            sys_menu_socket_repository.markGroupChatAsRead(gId, chatMsgs.size)
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
            }
            kotlinx.coroutines.delay(1400)
        }
    }

    LaunchedEffect(activeTab, userJoinedGroup, groupSubTab) {
        val grpId = userJoinedGroup
        if (activeTab == "groups" && grpId != null && groupSubTab == "messages") {
            sys_menu_socket_repository.markGroupChatAsRead(grpId)
        }
    }

    LaunchedEffect(activeTab, selectedFriendId) {
        if (activeTab == "friends" && selectedFriendId.isNotEmpty()) {
            sys_menu_socket_repository.markDirectChatAsRead(selectedFriendId)
        }
    }

    val friendsList = sys_menu_socket_repository.friendsList
    val friendRequests = sys_menu_socket_repository.friendRequests

    var searchNickText by remember { mutableStateOf("") }
    var searchIdText by remember { mutableStateOf("") }
    var activeSearchField by remember { mutableStateOf("nick") }
    var searchResults by remember { mutableStateOf(FriendsSearchGlobalCache.cachedResults) }
    var isSearchRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // автозагрузка поиска при переходе на вкладку с глобальным кэшем 5 сек
    LaunchedEffect(activeTab) {
        if (activeTab == "search") {
            if (FriendsSearchGlobalCache.isValid()) {
                searchResults = FriendsSearchGlobalCache.cachedResults
                isSearchRefreshing = false
            } else {
                isSearchRefreshing = true
                try {
                    sys_menu_socket_repository.fetchSentRequestsOnline()
                    val res = sys_menu_socket_repository.searchPlayersOnline("_poisk_")
                    if (res.isNotEmpty() || searchResults.isEmpty()) {
                        searchResults = res
                        if (res.isNotEmpty()) {
                            FriendsSearchGlobalCache.update(res)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isSearchRefreshing = false
            }
        }
    }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    MenuFriendsState.isSearchFieldFocused = false

    val hasUnreadGroupExcludingCurrent = remember(
        sys_menu_socket_repository.unreadSmsIds,
        sys_menu_socket_repository.groups,
        sys_menu_socket_repository.groupMessagesCountMap,
        sys_menu_socket_repository.readGroupMessagesCountMap,
        sys_menu_socket_repository.groupMessagesMap,
        activeTab,
        groupSubTab,
        userJoinedGroup
    ) {
        val isCurrentlyInGroupChat = (activeTab == "groups" && groupSubTab == "messages" && userJoinedGroup != null)
        sys_menu_socket_repository.groups.any { group ->
            val isSelectedAndActive = isCurrentlyInGroupChat && (group.id == userJoinedGroup)
            if (isSelectedAndActive) {
                false
            } else {
                val hasUnread = sys_menu_socket_repository.hasGroupUnread(group.id)
                val isUnreadOnServer = sys_menu_socket_repository.unreadSmsIds.contains(group.id)
                hasUnread || isUnreadOnServer
            }
        }
    }

    val hasUnreadDirectExcludingCurrent = remember(
        sys_menu_socket_repository.friendsDetails,
        sys_menu_socket_repository.directMessagesCountMap,
        sys_menu_socket_repository.readDirectMessagesCountMap,
        activeTab,
        selectedFriendId
    ) {
        val isCurrentlyInDirectChat = (activeTab == "friends" && selectedFriendId.isNotEmpty())
        sys_menu_socket_repository.friendsDetails.any { friend ->
            if (isCurrentlyInDirectChat && friend.id == selectedFriendId) {
                false
            } else {
                val unreadCount = friend.unreadSmsCount.coerceAtLeast(sys_menu_socket_repository.getFriendUnreadCount(friend.id))
                unreadCount > 0
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(bottom = 10.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FriendsTabButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.friends),
                    isActive = activeTab == "friends",
                    notificationColor = if (hasUnreadDirectExcludingCurrent) AppColors.GoldOrange else null
                ) {
                    activeTab = "friends"
                    isSelectingFriendToAdd = false
                }
                FriendsTabButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.groups),
                    isActive = activeTab == "groups",
                    notificationColor = if (hasUnreadGroupExcludingCurrent) AppColors.BlueInfo else null
                ) {
                    activeTab = "groups"
                    isSelectingFriendToAdd = false
                }
                FriendsTabButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zayavki_v_druzya_8d72),
                    isActive = activeTab == "requests",
                    notificationColor = if (sys_menu_socket_repository.friendRequests.isNotEmpty()) AppColors.GreenCircle else null
                ) {
                    activeTab = "requests"
                    isSelectingFriendToAdd = false
                }
                FriendsTabButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_poisk_druzey_c79f),
                    isActive = activeTab == "search",
                    fontSize = 12.5.sp
                ) {
                    activeTab = "search"
                    isSelectingFriendToAdd = false
                }
                FriendsTabButton(text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.settings), isActive = activeTab == "settings") { activeTab = "settings"; isSelectingFriendToAdd = false }
            }

            if (activeTab != "search" && activeTab != "requests") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppColors.Divider)
                )
                Spacer(Modifier.height(10.dp))
            }

            Box(
                modifier = Modifier.weight(1f)
            ) {
                if (activeTab == "friends") {
                    FriendsTabContent(
                        friendsDetails = sys_menu_socket_repository.friendsDetails,
                        selectedFriendId = selectedFriendId,
                        onSelectedFriendIdChange = { selectedFriendId = it },
                        playerName = playerName,
                        onShowFriendInfo = { id, name, status, banStatus, banReason ->
                            infoTargetFriendId = id
                            infoTargetFriendName = name
                            infoTargetFriendStatus = status
                            infoTargetFriendBanStatus = banStatus
                            infoTargetFriendBanReason = banReason
                            showFriendInfoDialog = true
                        },
                        onShowDeleteFriendConfirm = { showDeleteFriendConfirmDialog = true },
                        friendsMessagesMap = friendsMessagesMap,
                        isFriendMessagesLoading = isFriendMessagesLoading,
                        messageText = messageText,
                        onMessageTextChange = { messageText = it },
                        onSendMessage = {
                            if (messageText.isNotBlank() && selectedFriendId.isNotEmpty()) {
                                val textToSend = messageText
                                messageText = ""
                                coroutineScope.launch {
                                    sys_menu_socket_repository.sendPersonalMessageOnline(selectedFriendId, textToSend)
                                    val msgs = sys_menu_socket_repository.fetchPersonalMessagesOnline(selectedFriendId)
                                    friendsMessagesMap = friendsMessagesMap + (selectedFriendId to msgs)
                                    ChatCacheManager.savePersonalMessages(context, selectedFriendId, msgs)
                                    sys_menu_socket_repository.markDirectChatAsRead(selectedFriendId, msgs.size)
                                }
                            }
                        },
                        chatFocusRequester = chatFocusRequester
                    )
                } else if (activeTab == "groups") {
                    GroupsTabContent(
                        groups = groups,
                        userJoinedGroup = userJoinedGroup,
                        onUserJoinedGroupChange = { userJoinedGroup = it },
                        groupSubTab = groupSubTab,
                        onGroupSubTabChange = { groupSubTab = it },
                        isSelectingFriendToAdd = isSelectingFriendToAdd,
                        onIsSelectingFriendToAddChange = { isSelectingFriendToAdd = it },
                        groupMessagesMap = groupMessagesMap,
                        groupMembersWithRolesMap = groupMembersWithRolesMap,
                        groupDescriptionsMap = groupDescriptionsMap,
                        groupCreatedTimeMap = groupCreatedTimeMap,
                        groupInputText = groupInputText,
                        onGroupInputTextChange = { groupInputText = it },
                        isMessagesLoading = isMessagesLoading,
                        addingMemberIds = addingMemberIds,
                        onAddMemberClick = { grpId, friend ->
                            addingMemberIds = addingMemberIds + friend.id
                            coroutineScope.launch {
                                try {
                                    val result = sys_menu_socket_repository.addGroupMember(grpId, friend.id)
                                    if (result == "ADD_MEMBER_OK") {
                                        toastMessage = context.getString(R.string.toast_friend_added_to_group, friend.name)
                                        val members = sys_menu_socket_repository.getGroupMembers(grpId)
                                        groupMembersWithRolesMap = groupMembersWithRolesMap + (grpId to members)
                                    } else {
                                        toastMessage = sys_menu_socket_repository.translateError(result)
                                    }
                                } finally {
                                    kotlinx.coroutines.delay(400)
                                    addingMemberIds = addingMemberIds - friend.id
                                }
                            }
                        },
                        onCreateGroupClick = {
                            val ownedCount = groups.count { it.myRank == 0 }
                            if (!isAskingCreateGroup && createGroupCooldownSeconds == 0 && ownedCount < sys_menu_socket_repository.maxOwnedGroups && sys_menu_socket_repository.isServerOnline) {
                                isAskingCreateGroup = true
                                coroutineScope.launch {
                                    try {
                                        val askRes = sys_menu_socket_repository.askCreateGroup()
                                        if (askRes == "ASK_CREATE_GROUP_OK") {
                                            showCreateGroupDialog = true
                                        } else if (askRes.startsWith("CREATE_GROUP_ERROR:RATE_LIMIT")) {
                                            val sec = askRes.substringAfter("|", "60").toIntOrNull()?.coerceIn(1, 60) ?: 60
                                            sys_menu_socket_repository.setCreateGroupCooldown(sec)
                                            groupCooldownTickMs = System.currentTimeMillis()
                                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_limit_dostignut_povtorite_pozzhe)
                                        } else if (askRes.startsWith("CREATE_GROUP_ERROR:LIMIT_REACHED")) {
                                            val maxG = askRes.substringAfter("|", "").toIntOrNull()
                                            if (maxG != null && maxG > 0) {
                                                sys_menu_socket_repository.maxOwnedGroups = maxG
                                            }
                                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_uzhe_vladeete_2_gruppami_822d)
                                        } else if (askRes.contains("NOT_ENOUGH_SCORE")) {
                                            sys_menu_socket_repository.refreshProfileConfig()
                                            toastMessage = skayrux.shootout2d.Loc.tr(
                                                skayrux.shootout2d.R.string.str_nedostatochno_ochkov_nuzhno_75_00_1b14,
                                                sys_menu_socket_repository.formattedMinScoreCreateGroup
                                            )
                                        } else {
                                            toastMessage = sys_menu_socket_repository.translateError(askRes)
                                        }
                                    } finally {
                                        isAskingCreateGroup = false
                                    }
                                }
                            }
                        },
                        onLeaveGroupClick = { showLeaveGroupDialog = true },
                        onDeleteGroupClick = { showDeleteGroupDialog = true },
                        onSendGroupMessage = {
                            val grpId = userJoinedGroup
                            if (groupInputText.isNotBlank() && grpId != null) {
                                val textToSend = groupInputText
                                groupInputText = ""
                                coroutineScope.launch {
                                    sys_menu_socket_repository.sendGroupMessage(grpId, textToSend)
                                    val msgs = sys_menu_socket_repository.getGroupMessages(grpId)
                                    val chatMsgs = msgs.map { 
                                        ChatMessage(it.senderName, String(android.util.Base64.decode(it.textBase64, android.util.Base64.DEFAULT)), it.senderId == sys_menu_socket_repository.currentPlayerId, it.senderId == "system", timestamp = it.timestamp)
                                    }
                                    groupMessagesMap = groupMessagesMap + (grpId to chatMsgs)
                                    ChatCacheManager.saveGroupMessages(context, grpId, chatMsgs)
                                }
                            }
                        },
                        onMemberStatusChangeClick = { member ->
                            statusTargetMember = member
                            showChangeStatusDialog = true
                        },
                        onMemberKickClick = { member ->
                            kickTargetMember = member
                            showKickConfirmDialog = true
                        },
                        onMemberInfoClick = { id, name ->
                            val memberProfile = sys_menu_socket_repository.getProfileById(id)
                            infoTargetFriendId = id
                            infoTargetFriendName = name
                            infoTargetFriendStatus = memberProfile?.status ?: ""
                            infoTargetFriendBanStatus = memberProfile?.banStatus ?: 0
                            infoTargetFriendBanReason = memberProfile?.banReason
                            showFriendInfoDialog = true
                        },
                        onGroupAvatarClick = {
                            val gId = userJoinedGroup
                            if (gId != null && !isAskingGroupAvatar && sys_menu_socket_repository.isServerOnline) {
                                val curAvatar = sys_menu_socket_repository.groupAvatarsMap[gId] ?: ""
                                if (curAvatar.isNotEmpty() && curAvatar != "none") {
                                    showGroupAvatarDialog = true
                                } else if (groupAvatarCooldownSeconds == 0) {
                                    isAskingGroupAvatar = true
                                    coroutineScope.launch {
                                        try {
                                            val askResp = sys_menu_socket_repository.askChangeGroupAvatar(gId)
                                            if (askResp == "ASK_CHANGE_GROUP_AVATAR_OK") {
                                                showGroupAvatarDialog = true
                                            } else if (askResp.startsWith("ASK_CHANGE_GROUP_AVATAR_ERROR:SPAM")) {
                                                val sec = askResp.substringAfter("|", "60").toIntOrNull()?.coerceIn(1, 60) ?: 60
                                                sys_menu_socket_repository.setGroupAvatarCooldown(sec)
                                                groupCooldownTickMs = System.currentTimeMillis()
                                                toastMessage = context.getString(R.string.toast_avatar_cooldown, sec.toString())
                                            } else if (askResp.startsWith("ASK_CHANGE_GROUP_AVATAR_ERROR:NO_PERMISSION")) {
                                                toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_u_vas_net_prav_dlya_izmeneniya_a_34c1)
                                            } else {
                                                toastMessage = sys_menu_socket_repository.translateError(askResp)
                                            }
                                        } finally {
                                            isAskingGroupAvatar = false
                                        }
                                    }
                                }
                            }
                        },
                        coroutineScope = coroutineScope,
                        canCreateGroup = !isAskingCreateGroup && createGroupCooldownSeconds == 0 && groups.count { it.myRank == 0 } < sys_menu_socket_repository.maxOwnedGroups
                    )
                } else if (activeTab == "settings") {
                    FriendsSettingsTabContent(
                        noGroupInvites = noGroupInvites,
                        onNoGroupInvitesChange = { newVal ->
                            noGroupInvites = newVal
                            AppSettings.setBoolean(context, "no_group_invites", newVal)
                            coroutineScope.launch {
                                sys_menu_socket_repository.updateGroupInviteSettingOnline(newVal)
                            }
                        }
                    )
                } else if (activeTab == "requests") {
                    FriendRequestsTabContent(
                        friendRequests = sys_menu_socket_repository.friendRequests,
                        onAcceptRequest = { req ->
                            coroutineScope.launch {
                                val res = sys_menu_socket_repository.acceptFriendRequestOnline(req.id)
                                if (res.startsWith("FRIEND_OK")) {
                                    sys_menu_socket_repository.fetchFriendsOnline()
                                    sys_menu_socket_repository.fetchFriendRequestsOnline()
                                    toastMessage = context.getString(R.string.toast_request_accepted, req.name)
                                } else {
                                    toastMessage = sys_menu_socket_repository.translateError(res)
                                }
                            }
                        },
                        onDeclineRequest = { req ->
                            coroutineScope.launch {
                                val res = sys_menu_socket_repository.declineFriendRequestOnline(req.id)
                                if (res.startsWith("FRIEND_OK")) {
                                    sys_menu_socket_repository.fetchFriendRequestsOnline()
                                    toastMessage = context.getString(R.string.toast_request_declined, req.name)
                                } else {
                                    toastMessage = sys_menu_socket_repository.translateError(res)
                                }
                            }
                        },
                        onFriendInfoClick = { id, name, status, banStatus, banReason ->
                            infoTargetFriendId = id
                            infoTargetFriendName = name
                            infoTargetFriendStatus = status
                            infoTargetFriendBanStatus = banStatus
                            infoTargetFriendBanReason = banReason
                            showFriendInfoDialog = true
                        },
                        onAcceptAllClick = { showAcceptAllRequestsConfirmDialog = true },
                        onDeclineAllClick = { showDeclineAllRequestsConfirmDialog = true }
                    )
                } else if (activeTab == "search") {
                    SearchFriendsTabContent(
                        isSearchRefreshing = isSearchRefreshing,
                        searchResults = searchResults,
                        friendRequests = sys_menu_socket_repository.friendRequests,
                        localSentRequests = localSentRequests,
                        searchNickText = searchNickText,
                        onSearchNickTextChange = { searchNickText = it },
                        searchIdText = searchIdText,
                        onSearchIdTextChange = { searchIdText = it },
                        onPerformSearchClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            coroutineScope.launch {
                                isSearchRefreshing = true
                                val query = when {
                                    searchIdText.isNotBlank() -> "_poisk_id_${searchIdText.trim()}_"
                                    searchNickText.isNotBlank() -> {
                                        val trimmed = searchNickText.trim()
                                        if (trimmed.length < 2) "" else "_poisk_nick_${trimmed}_"
                                    }
                                    else -> "_poisk_"
                                }
                                if (query.isEmpty()) {
                                    searchResults = emptyList()
                                    isSearchRefreshing = false
                                    return@launch
                                }
                                val fetchJob = async { sys_menu_socket_repository.fetchSentRequestsOnline() }
                                val searchJob = async { sys_menu_socket_repository.searchPlayersOnline(query) }
                                fetchJob.await()
                                val fresh = searchJob.await()
                                searchResults = fresh
                                if (query == "_poisk_") {
                                    FriendsSearchGlobalCache.update(fresh)
                                }
                                isSearchRefreshing = false
                            }
                        },
                        onSendFriendRequestClick = { player ->
                            focusManager.clearFocus()
                            coroutineScope.launch {
                                val res = sys_menu_socket_repository.sendFriendRequestOnline(player.id)
                                if (res.startsWith("FRIEND_OK")) {
                                    localSentRequests = localSentRequests + player.id
                                    toastMessage = context.getString(R.string.toast_request_sent, player.name)
                                } else {
                                    toastMessage = sys_menu_socket_repository.translateError(res)
                                }
                            }
                        },
                        onFriendInfoClick = { id, name, status, banStatus, banReason ->
                            focusManager.clearFocus()
                            infoTargetFriendId = id
                            infoTargetFriendName = name
                            infoTargetFriendStatus = status
                            infoTargetFriendBanStatus = banStatus
                            infoTargetFriendBanReason = banReason
                            showFriendInfoDialog = true
                        }
                    )
                }
            }
        }





        if (showGroupAvatarDialog) {
            val currentAvatar = remember(userJoinedGroup, sys_menu_socket_repository.groupAvatarsMap) {
                userJoinedGroup?.let { sys_menu_socket_repository.groupAvatarsMap[it] } ?: ""
            }
            val hasAvatar = currentAvatar.isNotEmpty() && currentAvatar != "none" && currentAvatar != "LOADING"
            
            UnifiedDialog(
                title = if (hasAvatar) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_udalit_avatarku_gruppy_ab9b) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_zagruzit_avatarku_gruppy_6289),
                content = {
                    if (hasAvatar) {
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_uvereny_chto_hotite_udalit__3e1b),
                            color = AppColors.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    } else {
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
                                    if (!isGroupAvatarPickerLaunching && now - lastGroupAvatarPickerLaunchTime > 1000L) {
                                        isGroupAvatarPickerLaunching = true
                                        lastGroupAvatarPickerLaunchTime = now
                                        groupAvatarPickerLauncher.launch("image/*")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedGroupAvatarUri == null) {
                                Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vybrat_kartinku_jpg_png_6c38), color = AppColors.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            } else {
                                Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_kartinka_vybrana_8bc9), color = AppColors.TextGreenSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmText = if (hasAvatar) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_primenit_2cd8),
                confirmEnabled = hasAvatar || selectedGroupAvatarUri != null,
                onConfirm = {
                    userJoinedGroup?.let { gId ->
                        if (hasAvatar) {
                            coroutineScope.launch {
                                val resp = sys_menu_socket_repository.updateGroupAvatar(gId, "")
                                if (resp.startsWith("UPDATE_GROUP_AVATAR_OK")) {
                                    AvatarkiCacheManager.saveGroupAvatar(context, gId, "none")
                                    sys_menu_socket_repository.groupAvatarsMap = sys_menu_socket_repository.groupAvatarsMap + (gId to "none")
                                    toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_avatarka_gruppy_uspeshno_udalen_e819)
                                } else {
                                    toastMessage = context.getString(R.string.toast_avatar_delete_error, resp)
                                }
                            }
                        } else {
                            selectedGroupAvatarUri?.let { uri ->
                                val base64 = uriToBase64(context, uri)
                                if (base64 != null) {
                                    coroutineScope.launch {
                                        val uploadResp = sys_menu_socket_repository.updateGroupAvatar(gId, base64)
                                        if (uploadResp.startsWith("UPDATE_GROUP_AVATAR_OK")) {
                                            AvatarkiCacheManager.saveGroupAvatar(context, gId, base64)
                                            sys_menu_socket_repository.groupAvatarsMap = sys_menu_socket_repository.groupAvatarsMap + (gId to base64)
                                            sys_menu_socket_repository.setGroupAvatarCooldown(60)
                                            groupCooldownTickMs = System.currentTimeMillis()
                                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_avatarka_gruppy_uspeshno_obnovl_c4e0)
                                        } else if (uploadResp.startsWith("UPDATE_GROUP_AVATAR_ERROR:RATE_LIMIT") || uploadResp.startsWith("UPDATE_GROUP_AVATAR_ERROR:SPAM")) {
                                            val sec = uploadResp.substringAfter("|", "60").toIntOrNull()?.coerceIn(1, 60) ?: 60
                                            sys_menu_socket_repository.setGroupAvatarCooldown(sec)
                                            groupCooldownTickMs = System.currentTimeMillis()
                                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_limit_dostignut_povtorite_pozzhe)
                                        } else {
                                            toastMessage = context.getString(R.string.toast_avatar_update_error, sys_menu_socket_repository.translateError(uploadResp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    selectedGroupAvatarUri = null
                    showGroupAvatarDialog = false
                },
                onDismiss = {
                    selectedGroupAvatarUri = null
                    showGroupAvatarDialog = false
                }
            )
        }

        if (showCreateGroupDialog) {
            var createGroupStep by remember { mutableIntStateOf(1) }
            var newGroupNameText by remember { mutableStateOf("") }
            var newGroupDescText by remember { mutableStateOf("") }
            UnifiedDialog(
                title = if (createGroupStep == 1) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sozdanie_gruppy_1_2_ae12) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sozdanie_gruppy_2_2_09f3),
                content = {
                    if (createGroupStep == 1) {
                        UnifiedTextField(
                            value = newGroupNameText,
                            onValueChange = { input ->
                                val filtered = sanitizeSafeText(input)
                                if (filtered.length <= 25) {
                                    newGroupNameText = filtered
                                }
                            },
                            inputFilter = ::sanitizeSafeText,
                            placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_nazvanie_gruppy_9a39),
                            maxLength = 25,
                            regex = AllowedChars.REGEX
                        )
                    } else {
                        UnifiedTextField(
                            value = newGroupDescText,
                            onValueChange = { input ->
                                val filtered = sanitizeSafeText(input)
                                if (filtered.length <= 50) {
                                    newGroupDescText = filtered
                                }
                            },
                            inputFilter = ::sanitizeSafeText,
                            placeholder = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_opisanie_gruppy_39dd),
                            maxLength = 50,
                            regex = AllowedChars.REGEX
                        )
                    }
                },
                confirmText = if (createGroupStep == 1) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.next) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sozdat_b059),
                confirmEnabled = if (createGroupStep == 1) newGroupNameText.isNotBlank() else newGroupDescText.isNotBlank(),
                dismissOnConfirm = (createGroupStep == 2 && newGroupNameText.isNotBlank() && newGroupDescText.isNotBlank()),
                onConfirm = {
                    if (createGroupStep == 1) {
                        createGroupStep = 2
                    } else {
                        val name = sanitizeSafeText(newGroupNameText).trim()
                        val desc = sanitizeSafeText(newGroupDescText).trim()
                        if (name.isNotEmpty() && desc.isNotEmpty()) {
                            coroutineScope.launch {
                                val result = sys_menu_socket_repository.createGroup(name, desc)
                                if (result.startsWith("CREATE_GROUP_OK|")) {
                                    val parts = result.split("|")
                                    val groupId = parts[1]
                                    val cd = parts.getOrNull(2)?.toIntOrNull() ?: 0
                                    if (cd > 0) {
                                        sys_menu_socket_repository.setCreateGroupCooldown(cd)
                                        groupCooldownTickMs = System.currentTimeMillis()
                                    }
                                    userJoinedGroup = groupId
                                    toastMessage = context.getString(R.string.toast_group_created, name)

                                    groups = sys_menu_socket_repository.getMyGroups()
                                } else {
                                    if (result.startsWith("CREATE_GROUP_ERROR:RATE_LIMIT")) {
                                        val sec = result.substringAfter("|", "60").toIntOrNull()?.coerceIn(1, 60) ?: 60
                                        sys_menu_socket_repository.setCreateGroupCooldown(sec)
                                        groupCooldownTickMs = System.currentTimeMillis()
                                    }
                                    toastMessage = when {
                                        result.contains("RATE_LIMIT") -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_limit_dostignut_povtorite_pozzhe)
                                        result.contains("NOT_ENOUGH_SCORE") -> skayrux.shootout2d.Loc.tr(
                                            skayrux.shootout2d.R.string.str_nedostatochno_ochkov_nuzhno_75_00_1b14,
                                            sys_menu_socket_repository.formattedMinScoreCreateGroup
                                        )
                                        result.contains("LIMIT_REACHED") -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_uzhe_vladeete_2_gruppami_822d)
                                        else -> sys_menu_socket_repository.translateError(result)
                                    }
                                }
                            }
                        }
                        showCreateGroupDialog = false
                    }
                },
                onDismiss = {
                    showCreateGroupDialog = false
                }
            )
        }

        if (showLeaveGroupDialog) {
            val grpName = groups.find { it.id == userJoinedGroup }?.name ?: ""
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vyhod_iz_gruppy_b288),
                content = {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.dialog_leave_group, grpName), color = AppColors.White, fontSize = 14.sp)
                },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.sign_out),
                onConfirm = {
                    val grpId = userJoinedGroup
                    if (grpId != null) {
                        coroutineScope.launch {
                            val result = sys_menu_socket_repository.removeGroupMember(grpId, sys_menu_socket_repository.currentPlayerId)
                            if (result == "REMOVE_MEMBER_OK") {
                                toastMessage = context.getString(R.string.toast_left_group, grpName)
                                groups = sys_menu_socket_repository.getMyGroups()
                                userJoinedGroup = groups.firstOrNull()?.id
                            } else {
                                toastMessage = sys_menu_socket_repository.translateError(result)
                            }
                        }
                    }
                    showLeaveGroupDialog = false
                },
                onDismiss = { showLeaveGroupDialog = false }
            )
        }

        if (showDeleteGroupDialog) {
            val grpName = groups.find { it.id == userJoinedGroup }?.name ?: ""
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_udalenie_gruppy_a824),
                titleColor = AppColors.White,
                content = {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.dialog_delete_group, grpName), color = AppColors.White, fontSize = 14.sp)
                },
                confirmText = if (deleteCountdown > 0) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.btn_delete_countdown, deleteCountdown) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
                confirmEnabled = (deleteCountdown == 0),
                onConfirm = {
                    val grpId = userJoinedGroup
                    if (grpId != null) {
                        coroutineScope.launch {
                            val result = sys_menu_socket_repository.deleteGroup(grpId)
                            if (result == "DELETE_GROUP_OK") {
                                toastMessage = context.getString(R.string.toast_group_deleted, grpName)
                                groups = sys_menu_socket_repository.getMyGroups()
                                userJoinedGroup = groups.firstOrNull()?.id
                            } else {
                                toastMessage = sys_menu_socket_repository.translateError(result)
                            }
                        }
                    }
                    showDeleteGroupDialog = false
                },
                onDismiss = { showDeleteGroupDialog = false }
            )
        }

        val currentStatusTarget = statusTargetMember
        if (showChangeStatusDialog && currentStatusTarget != null) {
            val currentRank = currentStatusTarget.rank
            val newRank = if (currentRank == 1) 2 else 1
            val newRoleName = if (newRank == 1) skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.admin) else skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_uchastnik_cffb)
            val targetName = currentStatusTarget.name
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_smenit_status_5341),
                content = {
                    Text(
                        text = if (currentRank == 2) {
                            skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.dialog_make_admin, targetName)
                        } else {
                            skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.dialog_make_member, targetName)
                        },
                        color = AppColors.White,
                        fontSize = 14.sp
                    )
                },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_izmenit_47e2),
                onConfirm = {
                    val grp = userJoinedGroup
                    if (grp != null) {
                        coroutineScope.launch {
                            val result = sys_menu_socket_repository.changeGroupRank(grp, currentStatusTarget.id, newRank)
                            if (result == "CHANGE_RANK_OK") {
                                toastMessage = context.getString(R.string.toast_status_changed, currentStatusTarget.name, newRoleName.lowercase())
                                val members = sys_menu_socket_repository.getGroupMembers(grp)
                                groupMembersWithRolesMap = groupMembersWithRolesMap + (grp to members)
                            } else {
                                toastMessage = sys_menu_socket_repository.translateError(result)
                            }
                        }
                    }
                    showChangeStatusDialog = false
                    statusTargetMember = null
                },
                onDismiss = {
                    showChangeStatusDialog = false
                    statusTargetMember = null
                }
            )
        }

        if (showKickConfirmDialog) {
            val currentKickTarget = kickTargetMember
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_isklyuchit_uchastnika_5dd1),
                content = {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.dialog_kick_member, currentKickTarget?.name ?: ""), color = AppColors.White, fontSize = 14.sp)
                },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
                onConfirm = {
                    val grp = userJoinedGroup
                    if (grp != null && currentKickTarget != null) {
                        coroutineScope.launch {
                            val result = sys_menu_socket_repository.removeGroupMember(grp, currentKickTarget.id)
                            if (result == "REMOVE_MEMBER_OK") {
                                toastMessage = context.getString(R.string.toast_member_kicked, currentKickTarget.name)
                                val members = sys_menu_socket_repository.getGroupMembers(grp)
                                groupMembersWithRolesMap = groupMembersWithRolesMap + (grp to members)
                            } else {
                                toastMessage = sys_menu_socket_repository.translateError(result)
                            }
                        }
                    }
                    showKickConfirmDialog = false
                },
                onDismiss = { showKickConfirmDialog = false }
            )
        }

        if (showFriendInfoDialog && (infoTargetFriendId.isNotEmpty() || infoTargetFriendName.isNotEmpty())) {
            androidx.compose.runtime.key(infoTargetFriendId, infoTargetFriendName) {
                FriendInfoDialog(
                    playerId = infoTargetFriendId,
                    playerNameFallback = infoTargetFriendName,
                    currentUserName = playerName,
                    initialStatus = infoTargetFriendStatus,
                    initialBanStatus = infoTargetFriendBanStatus,
                    initialBanReason = infoTargetFriendBanReason,
                    onDismiss = {
                        showFriendInfoDialog = false
                        infoTargetFriendId = ""
                        infoTargetFriendName = ""
                        infoTargetFriendStatus = ""
                        infoTargetFriendBanStatus = 0
                        infoTargetFriendBanReason = null
                    }
                )
            }
        }

        if (showDeleteFriendConfirmDialog && selectedFriendId.isNotEmpty()) {
            val friendName = sys_menu_socket_repository.friendsDetails.find { it.id == selectedFriendId }?.name ?: ""
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_udalit_iz_druzey_3d57),
                titleColor = AppColors.White,
                content = {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.dialog_delete_friend, friendName), color = AppColors.TextMuted, fontSize = 14.sp)
                },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete),
                onConfirm = {
                    coroutineScope.launch {
                        sys_menu_socket_repository.removeFriendOnline(selectedFriendId)
                        sys_menu_socket_repository.fetchFriendsOnline()
                        selectedFriendId = ""
                    }
                    toastMessage = context.getString(R.string.toast_friend_deleted, friendName)
                    showDeleteFriendConfirmDialog = false
                },
                onDismiss = { showDeleteFriendConfirmDialog = false }
            )
        }

        if (showAcceptAllRequestsConfirmDialog) {
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_prinyat_vse_zayavki_d07c),
                titleColor = AppColors.White,
                content = {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_deystvitelno_hotite_prinyat_8e1d), color = AppColors.TextMuted, fontSize = 14.sp)
                },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.yes),
                onConfirm = {
                    if (sys_menu_socket_repository.friendRequests.isNotEmpty()) {
                        coroutineScope.launch {
                            for (req in sys_menu_socket_repository.friendRequests) {
                                sys_menu_socket_repository.acceptFriendRequestOnline(req.id)
                            }
                            sys_menu_socket_repository.fetchFriendsOnline()
                            sys_menu_socket_repository.fetchFriendRequestsOnline()
                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vse_zayavki_prinyaty_274f)
                        }
                    }
                    showAcceptAllRequestsConfirmDialog = false
                },
                onDismiss = { showAcceptAllRequestsConfirmDialog = false }
            )
        }

        if (showDeclineAllRequestsConfirmDialog) {
            UnifiedDialog(
                title = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_otklonit_vse_zayavki_3b55),
                titleColor = AppColors.White,
                content = {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vy_deystvitelno_hotite_otklon_6d14), color = AppColors.TextMuted, fontSize = 14.sp)
                },
                confirmText = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.yes),
                onConfirm = {
                    if (sys_menu_socket_repository.friendRequests.isNotEmpty()) {
                        coroutineScope.launch {
                            for (req in sys_menu_socket_repository.friendRequests) {
                                sys_menu_socket_repository.declineFriendRequestOnline(req.id)
                            }
                            sys_menu_socket_repository.fetchFriendRequestsOnline()
                            toastMessage = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vse_zayavki_otkloneny_9f33)
                        }
                    }
                    showDeclineAllRequestsConfirmDialog = false
                },
                onDismiss = { showDeclineAllRequestsConfirmDialog = false }
            )
        }

        UnifiedToast(
            message = toastMessage ?: "",
            visible = toastMessage != null,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun CategoryTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = AppColors.TextDarkGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 13.dp, bottom = 7.dp)
    )
}

@Composable
fun GroupAvatar(
    groupId: String,
    groupName: String,
    sizeDp: Int,
    fontSizeSp: Int,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val avatarBase64 = remember(groupId, sys_menu_socket_repository.groupAvatarsMap) {
        val inMap = sys_menu_socket_repository.groupAvatarsMap[groupId]
        if (inMap != null) {
            inMap
        } else {
            val saved = AvatarkiCacheManager.getCachedGroupAvatarBase64(context, groupId)
            if (saved.isNotEmpty()) {
                sys_menu_socket_repository.groupAvatarsMap = sys_menu_socket_repository.groupAvatarsMap + (groupId to saved)
            }
            saved
        }
    }

    // фоновая сверка аватарки группы при открытии и каждые 10 секунд
    LaunchedEffect(groupId) {
        if (groupId.isNotEmpty()) {
            while (true) {
                try {
                    if (sys_menu_socket_repository.isServerOnline) {
                        sys_menu_socket_repository.getGroupAvatar(groupId, forceRefresh = true)
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {}
                kotlinx.coroutines.delay(10_000L)
            }
        }
    }

    val imageBitmap = remember(avatarBase64) {
        AvatarBitmapCacheManager.getBitmap(avatarBase64)
    }

    val modifier = Modifier
        .requiredSize(sizeDp.dp)
        .clip(shape)
        .background(AppColors.Gray22, shape)
        .border(1.dp, AppColors.Border, shape)
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
            Image(
                bitmap = imageBitmap,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(shape)
            )
        } else {
            Text(
                text = groupName.firstOrNull()?.uppercase()?.toString() ?: "?",
                color = AppColors.White,
                fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { fontSizeSp.dp.toSp() },
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

fun uriToBase64(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        if (originalBitmap == null) return null
        
        val maxDim = 512
        val width = originalBitmap.width
        val height = originalBitmap.height
        val (newWidth, newHeight) = if (width > height) {
            val ratio = width.toFloat() / height
            if (width > maxDim) {
                maxDim to (maxDim / ratio).toInt()
            } else {
                width to height
            }
        } else {
            val ratio = height.toFloat() / width
            if (height > maxDim) {
                (maxDim / ratio).toInt() to maxDim
            } else {
                width to height
            }
        }
        val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        
        val outputStream = java.io.ByteArrayOutputStream()
        resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

