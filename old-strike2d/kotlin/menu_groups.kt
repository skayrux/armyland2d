package skayrux.shootout2d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@Composable
fun GroupsTabContent(
    groups: List<Group>,
    userJoinedGroup: String?,
    onUserJoinedGroupChange: (String?) -> Unit,
    groupSubTab: String,
    onGroupSubTabChange: (String) -> Unit,
    isSelectingFriendToAdd: Boolean,
    onIsSelectingFriendToAddChange: (Boolean) -> Unit,
    groupMessagesMap: Map<String, List<ChatMessage>>,
    groupMembersWithRolesMap: Map<String, List<GroupMember>>,
    groupDescriptionsMap: Map<String, String>,
    groupCreatedTimeMap: Map<String, String>,
    groupInputText: String,
    onGroupInputTextChange: (String) -> Unit,
    isMessagesLoading: Boolean,
    addingMemberIds: Set<String>,
    onAddMemberClick: (String, FriendData) -> Unit,
    onCreateGroupClick: () -> Unit,
    onLeaveGroupClick: () -> Unit,
    onDeleteGroupClick: () -> Unit,
    onSendGroupMessage: () -> Unit,
    onMemberStatusChangeClick: (GroupMember) -> Unit,
    onMemberKickClick: (GroupMember) -> Unit,
    onMemberInfoClick: (String, String) -> Unit,
    onGroupAvatarClick: () -> Unit,
    coroutineScope: CoroutineScope,
    canCreateGroup: Boolean = true
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
                if (isSelectingFriendToAdd) {
                    val membersInGroup = remember(userJoinedGroup, groupMembersWithRolesMap) {
                        if (userJoinedGroup != null) groupMembersWithRolesMap[userJoinedGroup] ?: emptyList() else emptyList()
                    }
                    val friendsDetails = sys_menu_socket_repository.friendsDetails
                    val availableFriends = remember(friendsDetails, membersInGroup) {
                        friendsDetails.filter { friend ->
                            !membersInGroup.any { it.id == friend.id }
                        }
                    }

                    if (availableFriends.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "—",
                                color = AppColors.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val lazyListState = rememberLazyListState()
                        val sortedFriends = availableFriends
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState
                        ) {
                            itemsIndexed(sortedFriends, key = { _, friend -> friend.id }) { index, friend ->
                                FadingLazyColumnItem(lazyListState, friend.id, index) {
                                    val isAdding = friend.id in addingMemberIds
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(AppColors.Transparent)
                                                .border(1.dp, Color.Transparent)
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
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .menuActionButtonStyle(enabled = !isAdding, shape = RoundedCornerShape(4.dp))
                                                    .clickable(enabled = !isAdding) {
                                                        val grp = userJoinedGroup
                                                        if (grp != null && !isAdding) {
                                                            onAddMemberClick(grp, friend)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isAdding) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(14.dp),
                                                        color = AppColors.Success,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Filled.Add,
                                                        contentDescription = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_dobavit_5eba),
                                                        tint = AppColors.OnlineGreen,
                                                        modifier = Modifier.size(20.dp)
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
                } else {
                    if (groups.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("—", color = AppColors.Gray, fontSize = 14.sp)
                        }
                    } else {
                        val sortedGroups = remember(groups, sys_menu_socket_repository.unreadSmsIds, sys_menu_socket_repository.readGroupMessagesCountMap, sys_menu_socket_repository.groupMessagesCountMap) {
                            groups.sortedWith(compareByDescending { group: Group ->
                                sys_menu_socket_repository.hasGroupUnread(group.id) || sys_menu_socket_repository.unreadSmsIds.contains(group.id)
                            })
                        }
                        val lazyListState = rememberLazyListState()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState
                        ) {
                            itemsIndexed(sortedGroups, key = { _, group -> group.id }) { index, group ->
                                FadingLazyColumnItem(lazyListState, group.id, index) {
                                    val isSelected = (userJoinedGroup == group.id)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onUserJoinedGroupChange(group.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) AppColors.TabActive else AppColors.Transparent)
                                                .border(1.dp, if (isSelected) AppColors.BorderActive else Color.Transparent)
                                                .padding(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GroupPlateStyle(
                                                id = group.id,
                                                name = group.name,
                                                description = group.description,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            val isSelectedAndActive = isSelected && (groupSubTab == "messages")
                                            val count = sys_menu_socket_repository.groupMessagesCountMap[group.id] 
                                                ?: sys_menu_socket_repository.groupMessagesMap[group.id]?.size ?: 0
                                            val readCount = sys_menu_socket_repository.readGroupMessagesCountMap[group.id] ?: count
                                            val unreadCount = if (sys_menu_socket_repository.unreadSmsIds.contains(group.id)) {
                                                (count - readCount).coerceAtLeast(1)
                                            } else {
                                                (count - readCount).coerceAtLeast(0)
                                            }
                                            if (unreadCount > 0 && !isSelectedAndActive) {
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
                                                            color = AppColors.BlueInfo,
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
            }

            Spacer(Modifier.height(16.dp))

            if (isSelectingFriendToAdd) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.TabInactive)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                        .clickable { onIsSelectingFriendToAddChange(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.back), color = AppColors.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            } else if (groupSubTab == "members") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.TabInactive)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                        .clickable { onIsSelectingFriendToAddChange(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.add_friends), color = AppColors.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                val currentGroup = groups.firstOrNull { it.id == userJoinedGroup }
                val isOwner = currentGroup?.myRank == 0
                val ownedCount = groups.count { it.myRank == 0 }
                val isCreateAllowed = canCreateGroup && sys_menu_socket_repository.isServerOnline && ownedCount < sys_menu_socket_repository.maxOwnedGroups
                Row(
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (isCreateAllowed) AppColors.ActionButtonBg else AppColors.ActionButtonDisabledBg)
                            .border(1.dp, if (isCreateAllowed) AppColors.ActionButtonBorder else AppColors.ActionButtonDisabledBorder)
                            .clickable(enabled = isCreateAllowed) {
                                if (isCreateAllowed) {
                                    onCreateGroupClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        CustomGroupIcons.IconCreateGroup(tint = if (isCreateAllowed) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(if (userJoinedGroup != null) AppColors.ActionButtonBg else AppColors.ActionButtonDisabledBg)
                            .border(1.dp, if (userJoinedGroup != null) AppColors.ActionButtonBorder else AppColors.ActionButtonDisabledBorder)
                            .clickable(enabled = userJoinedGroup != null) {
                                if (userJoinedGroup != null) {
                                    if (isOwner) {
                                        onDeleteGroupClick()
                                    } else {
                                        onLeaveGroupClick()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOwner) {
                            CustomGroupIcons.IconDeleteGroup(tint = if (userJoinedGroup != null) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText)
                        } else {
                            CustomGroupIcons.IconLeaveGroup(tint = if (userJoinedGroup != null) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText)
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }

        Column(
            modifier = Modifier
                .weight(0.77f)
                .fillMaxHeight()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FriendsTabButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.messages),
                    isActive = groupSubTab == "messages",
                    modifier = Modifier.weight(1f)
                ) {
                    onGroupSubTabChange("messages")
                    onIsSelectingFriendToAddChange(false)
                }

                FriendsTabButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.participants),
                    isActive = groupSubTab == "members",
                    modifier = Modifier.weight(1f)
                ) {
                    onGroupSubTabChange("members")
                    onIsSelectingFriendToAddChange(false)
                }

                FriendsTabButton(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_informatsiya_a7fd),
                    isActive = groupSubTab == "info",
                    modifier = Modifier.weight(1f)
                ) {
                    onGroupSubTabChange("info")
                    onIsSelectingFriendToAddChange(false)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, AppColors.Divider)
                    .clipToBounds(),
                contentAlignment = Alignment.TopStart
            ) {
                if (userJoinedGroup == null) {
                    Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                        Text("—", color = AppColors.Gray, fontSize = 14.sp)
                    }
                } else if (groupSubTab == "messages") {
                    if (isMessagesLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
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
                        val activeMsgs = groupMessagesMap[userJoinedGroup] ?: emptyList()
                        if (activeMsgs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.Center) {
                                Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.no_messages), color = AppColors.Gray, fontSize = 14.sp)
                            }
                        } else {
                            key(userJoinedGroup) {
                                val lazyListState = rememberLazyListState()
                                val msgCount = activeMsgs.size
                                var prevMsgCount by remember { mutableStateOf(0) }

                                LaunchedEffect(msgCount) {
                                    if (msgCount > 0) {
                                        val isMyMessage = activeMsgs.last().isMe
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
                                        activeMsgs,
                                        key = { index, msg -> "${msg.sender}_${msg.timestamp}_$index" }
                                    ) { index, msg ->
                                        val showDateHeader = remember(index, activeMsgs) {
                                            val currDate = formatChatDate(msg.timestamp)
                                            if (currDate.isEmpty()) false
                                            else if (index == 0) true
                                            else {
                                                val prevDate = formatChatDate(activeMsgs[index - 1].timestamp)
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
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                        val timeStr = formatChatTime(msg.timestamp)
                                        Text(
                                            text = buildAnnotatedString {
                                                if (msg.isSystem || msg.sender == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_sistema_8d8c)) {
                                                    withStyle(style = SpanStyle(
                                                        color = AppColors.TextGray.copy(alpha = 0.7f),
                                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                        fontSize = 13.sp
                                                    )) {
                                                        append(msg.text)
                                                    }
                                                    if (timeStr.isNotEmpty()) {
                                                        withStyle(style = SpanStyle(
                                                            color = AppColors.TextGray.copy(alpha = 0.5f),
                                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Normal,
                                                            fontSize = 11.sp
                                                        )) {
                                                            append(" $timeStr")
                                                        }
                                                    }
                                                } else {
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
                } else if (groupSubTab == "members") {
                    val activeMembers = (groupMembersWithRolesMap[userJoinedGroup] ?: emptyList()).sortedBy { it.rank }
                    if (activeMembers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("—", color = AppColors.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item(key = "members_header") {
                                val viewerRank = activeMembers.find { m -> m.id == sys_menu_socket_repository.currentPlayerId }?.rank ?: 2
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 2.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1.25f), contentAlignment = Alignment.CenterStart) {
                                        Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.group_members_count, activeMembers.size), color = AppColors.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
                                    }
                                    Box(modifier = Modifier.weight(0.6f), contentAlignment = Alignment.Center) {
                                        Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_aydi_4349), color = AppColors.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    }
                                    Box(modifier = Modifier.weight(0.85f), contentAlignment = Alignment.Center) {
                                        Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_rol_5d62), color = AppColors.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    }
                                    Box(modifier = Modifier.width(104.dp), contentAlignment = Alignment.Center) {
                                        Text(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_deystvie_50a7), color = AppColors.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }

                            items(activeMembers, key = { it.id }) { m ->
                                val viewerRank = activeMembers.find { mem -> mem.id == sys_menu_socket_repository.currentPlayerId }?.rank ?: 2
                                val mRole = when(m.rank) {
                                    0 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.owner)
                                    1 -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.admin)
                                    else -> skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_uchastnik_cffb)
                                }
                                val rawLastSeen = if (m.lastSeen.isNotEmpty()) m.lastSeen else (sys_menu_socket_repository.friendsDetails.find { it.id == m.id }?.lastSeen ?: sys_menu_socket_repository.getProfileById(m.id)?.status ?: "")
                                val memberStatus = getRobustOnlineStatus(rawLastSeen)
                                val isOnline = memberStatus == "Online" || memberStatus.startsWith("Online") || memberStatus == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba)
                                val profile = sys_menu_socket_repository.getProfileById(m.id)
                                val isBanned = m.banStatus == 1 || profile?.banStatus == 1 || sys_menu_socket_repository.friendsDetails.find { it.id == m.id }?.banStatus == 1

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 2.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1.25f)
                                            .padding(end = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        UserAvatar(
                                            userId = m.id,
                                            userName = m.name,
                                            sizeDp = 34,
                                            fontSizeSp = 18,
                                            shape = RoundedCornerShape(4.dp),
                                            isBanned = isBanned
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            PlayerNameWithBan(
                                                name = m.name,
                                                isBanned = isBanned,
                                                baseColor = AppColors.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (memberStatus.isNotEmpty()) {
                                                Text(
                                                    text = memberStatus,
                                                    color = if (isOnline) AppColors.OnlineGreen else AppColors.Gray,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Spacer(Modifier.height(2.dp))
                                                StatusLoadingBar()
                                            }
                                        }
                                    }
                                    Text(
                                        text = m.id,
                                        color = AppColors.TextGray,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.6f)
                                    )
                                    Text(
                                        text = mRole,
                                        color = when (m.rank) {
                                            0 -> AppColors.RoleOwner
                                            1 -> AppColors.RoleAdmin
                                            else -> AppColors.RoleMember
                                        },
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.85f)
                                    )
                                    Row(
                                        modifier = Modifier.width(104.dp),
                                        horizontalArrangement = Arrangement.spacedBy(1.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val canChangeStatus = (viewerRank == 0 && m.id != sys_menu_socket_repository.currentPlayerId)
                                        val canKick = ((viewerRank == 0 || (viewerRank == 1 && m.rank == 2)) && m.id != sys_menu_socket_repository.currentPlayerId)
                                        val showInfo = (m.id != sys_menu_socket_repository.currentPlayerId)

                                        if (canChangeStatus) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable { onMemberStatusChangeClick(m) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = null,
                                                    tint = AppColors.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        if (showInfo) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable { onMemberInfoClick(m.id, m.name) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                iconInfo(
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        if (canKick) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clickable { onMemberKickClick(m) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = null,
                                                    tint = AppColors.DangerRed,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (groupSubTab == "info") {
                    val groupName = userJoinedGroup ?: ""
                    val groupRealName = groups.firstOrNull { it.id == groupName }?.name ?: groupName
                    val activeMembers = groupMembersWithRolesMap[groupName] ?: emptyList()
                    val description = groupDescriptionsMap[groupName] ?: ""
                    val createdTime = groupCreatedTimeMap[groupName] ?: "22.06.26"
                    val formattedDate = remember(createdTime) {
                        if (createdTime.contains("-")) {
                            val datePart = createdTime.split(" ")[0]
                            val parts = datePart.split("-")
                            if (parts.size == 3) {
                                val year = parts[0].trim()
                                val month = parts[1].trim()
                                val day = parts[2].trim()
                                "$day.$month.$year"
                            } else {
                                createdTime
                            }
                        } else {
                            val parts = createdTime.split(".")
                            if (parts.size == 3) {
                                val day = parts[0].trim()
                                val month = parts[1].trim()
                                val yearPart = parts[2].trim()
                                val fullYear = if (yearPart.length == 2) "20$yearPart" else yearPart
                                "$day.$month.$fullYear"
                            } else {
                                createdTime
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(72.dp)
                            ) {
                                GroupAvatar(
                                    groupId = groupName,
                                    groupName = groupRealName,
                                    sizeDp = 64,
                                    fontSizeSp = 32,
                                    shape = RoundedCornerShape(8.dp),
                                    onClick = { onGroupAvatarClick() }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .width(180.dp)
                                    .padding(top = 6.dp)
                            ) {
                                Text(
                                    text = groupRealName,
                                    color = AppColors.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "ID: $groupName",
                                    color = AppColors.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.created_date, formattedDate),
                                    color = AppColors.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = 6.dp)
                            ) {
                                Text(
                                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_opisanie_c2e7),
                                    color = AppColors.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = description.ifEmpty { skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_opisanie_otsutstvuet_e8c7) },
                                    color = AppColors.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            if (groupSubTab == "messages") {
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
                        val currentGroupInputTextState = rememberUpdatedState(groupInputText)
                        val onGroupSendState = rememberUpdatedState(onSendGroupMessage)
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
                                            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                                            imm?.showSoftInput(v, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                                        }
                                    }
                                    gravity = android.view.Gravity.CENTER_VERTICAL
                                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                                    filters = arrayOf(android.text.InputFilter.LengthFilter(150))
                                    imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                                    setOnEditorActionListener { v, actionId, _ ->
                                        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                                            onGroupSendState.value()
                                            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
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
                                            if (currentText != currentGroupInputTextState.value) {
                                                onGroupInputTextChange(currentText)
                                            }
                                        }
                                    })
                                }
                            },
                            update = { editText ->
                                if (editText.text.toString() != groupInputText) {
                                    val selection = editText.selectionStart
                                    editText.setText(groupInputText)
                                    editText.setSelection(selection.coerceIn(0, groupInputText.length))
                                }
                            }
                        )
                    }

                    val sendEnabled = (userJoinedGroup != null)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(100.dp)
                            .background(if (sendEnabled) AppColors.ActionButtonBg else AppColors.ActionButtonDisabledBg)
                            .border(1.dp, if (sendEnabled) AppColors.ActionButtonBorder else AppColors.ActionButtonDisabledBorder)
                            .clickable(enabled = sendEnabled) {
                                onSendGroupMessage()
                                keyboardController?.hide()
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
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

object CustomGroupIcons {

    val IconTwoPeople: ImageVector
        get() = ImageVector.Builder(
            name = "TwoPeople",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val stroke = SolidColor(AppColors.White)
            val dx1 = -1.5f
            path(
                stroke = stroke,
                strokeLineWidth = 2.25f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f + dx1, 21f)
                verticalLineToRelative(-2f)
                arcToRelative(4f, 4f, 0f, false, false, -4f, -4f)
                horizontalLineTo(5f + dx1)
                arcToRelative(4f, 4f, 0f, false, false, -4f, 4f)
                verticalLineToRelative(2f)
            }
            path(
                stroke = stroke,
                strokeLineWidth = 2.25f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.5f + dx1, 7f)
                arcTo(4f, 4f, 0f, true, false, 4.5f + dx1, 7f)
                arcTo(4f, 4f, 0f, true, false, 12.5f + dx1, 7f)
            }

            val dx2 = 4.5f
            path(
                stroke = stroke,
                strokeLineWidth = 2.25f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(16f + dx2, 21f)
                verticalLineToRelative(-2f)
                arcToRelative(4f, 4f, 0f, false, false, -4f, -4f)
                horizontalLineTo(5f + dx2)
                arcToRelative(4f, 4f, 0f, false, false, -4f, 4f)
                verticalLineToRelative(2f)
            }
            path(
                stroke = stroke,
                strokeLineWidth = 2.25f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12.5f + dx2, 7f)
                arcTo(4f, 4f, 0f, true, false, 4.5f + dx2, 7f)
                arcTo(4f, 4f, 0f, true, false, 12.5f + dx2, 7f)
            }
        }.build()

    val IconCreate: ImageVector
        get() = ImageVector.Builder(
            name = "CreateGroup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val stroke = SolidColor(AppColors.White)
            path(
                stroke = stroke,
                strokeLineWidth = 2f
            ) {
                moveTo(6f, 4f)
                lineTo(18f, 4f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
                lineTo(20f, 18f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
                lineTo(6f, 20f)
                arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
                lineTo(4f, 6f)
                arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
                close()
            }
            path(
                stroke = stroke,
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 8f)
                verticalLineToRelative(8f)
                moveTo(8f, 12f)
                horizontalLineToRelative(8f)
            }
        }.build()

    val IconLeave: ImageVector
        get() = ImageVector.Builder(
            name = "LeaveGroup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val fill = SolidColor(AppColors.White)
            path(
                fill = fill
            ) {
                moveTo(10.09f, 15.59f)
                lineTo(11.5f, 17f)
                lineToRelative(5f, -5f)
                lineToRelative(-5f, -5f)
                lineToRelative(-1.41f, 1.41f)
                lineTo(12.67f, 11f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(9.67f)
                lineToRelative(-2.58f, 2.59f)
                close()

                moveTo(19f, 3f)
                horizontalLineTo(5f)
                curveToRelative(-1.11f, 0f, -2f, 0.9f, -2f, 2f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(2f)
                verticalLineTo(5f)
                horizontalLineToRelative(14f)
                verticalLineToRelative(14f)
                horizontalLineTo(5f)
                verticalLineToRelative(-4f)
                horizontalLineTo(3f)
                verticalLineToRelative(4f)
                curveToRelative(0f, 1.1f, 0.89f, 2f, 2f, 2f)
                horizontalLineToRelative(14f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(5f)
                curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
                close()
            }
        }.build()

    val IconDelete: ImageVector
        get() = ImageVector.Builder(
            name = "DeleteGroup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            val fill = SolidColor(AppColors.White)
            path(
                fill = fill
            ) {
                moveTo(16f, 6f)
                verticalLineTo(4.5f)
                curveTo(16f, 3.67f, 15.33f, 3f, 14.5f, 3f)
                horizontalLineToRelative(-5f)
                curveTo(8.67f, 3f, 8f, 3.67f, 8f, 4.5f)
                verticalLineTo(6f)
                horizontalLineTo(3f)
                verticalLineToRelative(2f)
                horizontalLineToRelative(1f)
                verticalLineToRelative(12f)
                curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
                horizontalLineToRelative(12f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                verticalLineTo(8f)
                horizontalLineToRelative(1f)
                verticalLineTo(6f)
                horizontalLineToRelative(-5f)
                close()

                moveTo(10f, 5f)
                horizontalLineToRelative(4f)
                verticalLineToRelative(1f)
                horizontalLineToRelative(-4f)
                verticalLineTo(5f)
                close()

                moveTo(6f, 8f)
                horizontalLineToRelative(12f)
                verticalLineToRelative(12f)
                horizontalLineTo(6f)
                verticalLineTo(8f)
                close()
            }
            path(
                stroke = fill,
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 10f)
                verticalLineToRelative(8f)
                moveTo(12f, 10f)
                verticalLineToRelative(8f)
                moveTo(15f, 10f)
                verticalLineToRelative(8f)
            }
        }.build()

    @Composable
    fun IconCreateGroup(tint: Color = AppColors.White) {
        Icon(IconCreate, contentDescription = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.create_a_new_group), tint = tint, modifier = Modifier.size(19.dp))
    }

    @Composable
    fun IconLeaveGroup(tint: Color = AppColors.White) {
        Icon(IconLeave, contentDescription = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.leave_the_group), tint = tint, modifier = Modifier.size(19.dp))
    }

    @Composable
    fun IconDeleteGroup(tint: Color = AppColors.White) {
        Icon(IconDelete, contentDescription = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.delete_group), tint = tint, modifier = Modifier.size(20.dp))
    }
}
