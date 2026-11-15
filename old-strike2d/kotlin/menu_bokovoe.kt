package skayrux.shootout2d

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Work
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun bokovoe_menu(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    val items = listOf(
        Icons.Filled.Home,
        Icons.Filled.Work,
        Icons.Filled.Map,
        Icons.Filled.People,
        Icons.Filled.Settings
    )
    val isProfileMode = selectedIndex == 5 || selectedIndex == 9

    // боковая панель навигации с адаптивной высотой кнопок под разные экраны
    BoxWithConstraints(
        modifier = Modifier
            .width(61.dp)
            .fillMaxHeight()
    ) {
        val topBottomPad = if (maxHeight < 330.dp) 8.dp else 15.dp
        val itemH = ((maxHeight - topBottomPad * 2) / 6.2f).coerceIn(36.dp, 48.dp)

        // индекс выбранной плашки в текущем наборе кнопок (0..4 в обычном режиме, 1 - профиль, 2 - ключик)
        val activeSlotIndex = when (selectedIndex) {
            5 -> 1
            9 -> 2
            else -> selectedIndex.coerceIn(0, 4)
        }

        // плавная анимация смещения белого выделения по вертикали
        val indicatorYOffset by androidx.compose.animation.core.animateDpAsState(
            targetValue = itemH * activeSlotIndex,
            animationSpec = tween(durationMillis = 240, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            label = "sidebar_indicator_anim"
        )

        Column(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight()
                .background(AppColors.Transparent)
                .padding(top = topBottomPad),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp)
            ) {
                // плавно скользящая плашка выделения (фон и белая полоска слева)
                Box(
                    modifier = Modifier
                        .offset(y = indicatorYOffset)
                        .width(60.dp)
                        .height(itemH)
                        .padding(bottom = 5.dp)
                        .background(AppColors.SurfaceHover)
                ) {
                    // белая полоска активного элемента
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(3.dp)
                            .background(AppColors.White)
                            .align(Alignment.CenterStart)
                    )
                }

                // мягкое минималистичное растворение иконок при переходе в профиль и обратно
                AnimatedContent(
                    targetState = isProfileMode,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                            androidx.compose.animation.scaleIn(initialScale = 0.94f, animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
                                    androidx.compose.animation.scaleOut(targetScale = 0.96f, animationSpec = tween(150, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                            )
                    },
                    label = "bokovoe_menu_switch"
                ) { profileMode ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (profileMode) {
                            // кнопка назад в меню
                            SideMenuItem(
                                painter = painterResource(R.drawable.ic_reply),
                                isSelected = false,
                                onClick = { onItemSelected(0) },
                                itemHeight = itemH
                            )
                            // кнопка профиля (раздел профиля)
                            SideMenuItem(
                                painter = painterResource(R.drawable.ic_person_outlined),
                                isSelected = selectedIndex == 5,
                                onClick = { onItemSelected(5) },
                                itemHeight = itemH
                            )
                            // ключик (настройки профиля) под профилем
                            SideMenuItem(
                                icon = Icons.Filled.VpnKey,
                                isSelected = selectedIndex == 9,
                                onClick = { onItemSelected(9) },
                                itemHeight = itemH
                            )
                        } else {
                            items.forEachIndexed { index, icon ->
                                val showBadge = (index == 3) && sys_menu_socket_repository.friendsDetails.any {
                                    it.unreadSmsCount > 0 && it.id != sys_menu_socket_repository.activeDirectChatFriendId
                                }
                                SideMenuItem(
                                    icon = icon,
                                    isSelected = index == selectedIndex,
                                    onClick = { onItemSelected(index) },
                                    showBadge = showBadge,
                                    itemHeight = itemH
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // кнопка выхода из игры в самом низу на том же месте
            val context = LocalContext.current
            SideMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                isSelected = false,
                onClick = {
                    GlobalReconnectState.isIntentionalDisconnect = true
                    GlobalReconnectState.showConnectionErrorDialog = false
                    GlobalReconnectState.isReconnecting = false
                    sys_menu_socket_repository.isAppInForeground = false
                    sys_menu_socket_repository.disconnectAndForceOffline()
                    (findActivity(context) ?: (context as? Activity))?.finish()
                },
                modifier = Modifier
                    .padding(bottom = topBottomPad),
                iconModifier = Modifier.graphicsLayer {
                    scaleX = -1f
                },
                itemHeight = itemH
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(AppColors.Border)
                .align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun SideMenuItem(
    icon: ImageVector? = null,
    painter: Painter? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    showBadge: Boolean = false,
    itemHeight: androidx.compose.ui.unit.Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    // плавная анимация цвета иконки при смене активности
    val iconTint by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) AppColors.White else AppColors.TextDarkGray,
        animationSpec = tween(durationMillis = 200),
        label = "sidebar_icon_tint"
    )

    Box(
        modifier = modifier
            .width(60.dp)
            .height(itemHeight)
            .padding(bottom = 5.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(22.dp)
                        .then(iconModifier)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(22.dp)
                        .then(iconModifier)
                )
            }
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(AppColors.GoldOrange, shape = CircleShape)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                )
            }
        }
    }
}
