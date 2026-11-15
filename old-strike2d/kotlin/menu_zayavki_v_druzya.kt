package skayrux.shootout2d

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun FriendRequestsTabContent(
    friendRequests: List<FriendRequest>,
    onAcceptRequest: (FriendRequest) -> Unit,
    onDeclineRequest: (FriendRequest) -> Unit,
    onFriendInfoClick: (String, String, String, Int, String?) -> Unit,
    onAcceptAllClick: () -> Unit,
    onDeclineAllClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.PanelDark)
                .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Box
                // шапка таблицы
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_igrok_a1c5),
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f),
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_aydi_4349),
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.status),
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_deystvie_50a7),
                            color = AppColors.TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.75f),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
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
                    if (friendRequests.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.no_friend_requests),
                                color = AppColors.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        val lazyListState = rememberLazyListState()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState
                        ) {
                            itemsIndexed(friendRequests, key = { _, req -> req.id }) { index, req ->
                                FadingLazyColumnItem(lazyListState, req.id, index) {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1.5f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val isBanned = (sys_menu_socket_repository.getProfileById(req.id)?.banStatus ?: 0) == 1
                                                UserAvatar(
                                                    userId = req.id,
                                                    userName = req.name,
                                                    sizeDp = 34,
                                                    fontSizeSp = 18,
                                                    shape = RoundedCornerShape(4.dp),
                                                    isBanned = isBanned
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                PlayerNameWithBan(
                                                    name = req.name,
                                                    isBanned = isBanned,
                                                    baseColor = AppColors.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = req.id,
                                                color = AppColors.White.copy(alpha = 0.75f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier.weight(0.7f),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            val statusText = getRobustOnlineStatus(req.lastSeen)
                                            val isOnline = statusText == "Online" || statusText.startsWith("Online") || statusText == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba)
                                            Text(
                                                text = statusText,
                                                color = if (isOnline) AppColors.OnlineGreen else AppColors.Gray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(0.7f),
                                                textAlign = TextAlign.Center
                                            )
                                            Row(
                                                modifier = Modifier.weight(0.75f),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .width(44.dp)
                                                        .background(AppColors.TabInactive)
                                                        .clickable { onAcceptRequest(req) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Check,
                                                        contentDescription = null,
                                                        tint = AppColors.OnlineGreen,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .width(44.dp)
                                                        .background(AppColors.TabInactive)
                                                        .clickable { onDeclineRequest(req) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = null,
                                                        tint = AppColors.DangerRed,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                                Spacer(Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .width(44.dp)
                                                        .background(AppColors.TabInactive)
                                                        .clickable { onFriendInfoClick(req.id, req.name, req.lastSeen, req.banStatus, null) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    iconInfo(modifier = Modifier.size(22.dp))
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
        }

        Spacer(Modifier.height(16.dp))

        val hasRequests = friendRequests.isNotEmpty()

        // кнопки отклонить все и принять все
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // кнопка отклонить все
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (hasRequests) AppColors.ActionButtonBg else AppColors.ActionButtonDisabledBg)
                    .border(1.dp, if (hasRequests) AppColors.ActionButtonBorder else AppColors.ActionButtonDisabledBorder)
                    .clickable(enabled = hasRequests) {
                        onDeclineAllClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_otklonit_vse_5b28),
                    color = if (hasRequests) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
                    fontSize = 13.sp
                )
            }

            // кнопка принять все
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (hasRequests) AppColors.ActionButtonBg else AppColors.ActionButtonDisabledBg)
                    .border(1.dp, if (hasRequests) AppColors.ActionButtonBorder else AppColors.ActionButtonDisabledBorder)
                    .clickable(enabled = hasRequests) {
                        onAcceptAllClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_prinyat_vse_12db),
                    color = if (hasRequests) AppColors.ActionButtonText else AppColors.ActionButtonDisabledText,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}
