package skayrux.shootout2d

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

fun sortSearchResults(results: List<PlayerProfile>): List<PlayerProfile> {
    return results.sortedWith { a, b ->
        val aStatus = sys_menu_socket_repository.getUnifiedOnlineStatus(a.status)
        val bStatus = sys_menu_socket_repository.getUnifiedOnlineStatus(b.status)
        val aOnline = aStatus == "Online" || aStatus.startsWith("Online") || aStatus == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba)
        val bOnline = bStatus == "Online" || bStatus.startsWith("Online") || bStatus == skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_seti_49ba)

        when {
            aOnline && !bOnline -> -1
            !aOnline && bOnline -> 1
            else -> {
                // When both are online or both are offline: sort strictly by last seen recency
                val aRaw = a.status.trim()
                val bRaw = b.status.trim()
                if (aRaw.isNotEmpty() && bRaw.isNotEmpty() && aRaw != bRaw && aRaw != "-" && bRaw != "-") {
                    // Descending comparison (most recent timestamp e.g. 2026-09-22 14:00 > 2026-09-22 09:17)
                    bRaw.compareTo(aRaw)
                } else if (aRaw.isNotEmpty() && bRaw.isEmpty()) {
                    -1
                } else if (aRaw.isEmpty() && bRaw.isNotEmpty()) {
                    1
                } else {
                    a.name.compareTo(b.name, ignoreCase = true)
                }
            }
        }
    }
}

@Composable
fun SearchFriendsTabContent(
    isSearchRefreshing: Boolean,
    searchResults: List<PlayerProfile>,
    friendRequests: List<FriendRequest>,
    localSentRequests: Set<String>,
    searchNickText: String,
    onSearchNickTextChange: (String) -> Unit,
    searchIdText: String,
    onSearchIdTextChange: (String) -> Unit,
    onPerformSearchClick: () -> Unit,
    onSendFriendRequestClick: (PlayerProfile) -> Unit,
    onFriendInfoClick: (String, String, String, Int, String?) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var lastSearchClickTime by remember { mutableStateOf(0L) }
    var searchCooldownActive by remember { mutableStateOf(false) }

    // таймер кулдауна кнопки поиска
    LaunchedEffect(searchCooldownActive) {
        if (searchCooldownActive) {
            kotlinx.coroutines.delay(1500L)
            searchCooldownActive = false
        }
    }

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
                    if (isSearchRefreshing) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.08f,
                            targetValue = 0.22f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(800, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = ""
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(3) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AppColors.ShadowBase.copy(alpha = alpha))
                                        .border(1.dp, AppColors.PanelSemiTransparent.copy(alpha = alpha), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(0.25f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(0.25f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(0.25f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(modifier = Modifier.weight(0.25f).height(12.dp).background(AppColors.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp)))
                                }
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.nothing_found),
                                color = AppColors.Gray,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        val lazyListState = rememberLazyListState()
                        val sortedResults = remember(searchResults) { sortSearchResults(searchResults) }
                        val filteredResults = remember(sortedResults) {
                            sortedResults.filter { player -> sys_menu_socket_repository.currentPlayerId.isBlank() || player.id != sys_menu_socket_repository.currentPlayerId }
                                .take(30)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = lazyListState
                        ) {
                            itemsIndexed(filteredResults, key = { _, player -> player.id }) { index, player ->
                                FadingLazyColumnItem(lazyListState, player.id, index) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
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
                                                val isBanned = player.banStatus == 1 || sys_menu_socket_repository.getProfileById(player.id)?.banStatus == 1
                                                UserAvatar(
                                                    userId = player.id,
                                                    userName = player.name,
                                                    sizeDp = 34,
                                                    fontSizeSp = 18,
                                                    shape = RoundedCornerShape(4.dp),
                                                    isBanned = isBanned
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                PlayerNameWithBan(
                                                    name = player.name,
                                                    isBanned = isBanned,
                                                    baseColor = AppColors.White,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = player.id,
                                                color = AppColors.White.copy(alpha = 0.75f),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier.weight(0.7f),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                            val statusText = sys_menu_socket_repository.getUnifiedOnlineStatus(player.status)
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
                                                val isAlreadyFriend = sys_menu_socket_repository.friendsDetails.any { it.id == player.id }
                                                val isSent = (localSentRequests.contains(player.id) || sys_menu_socket_repository.sentRequests.any { it.id == player.id }) && !isAlreadyFriend
                                                val canAdd = player.id != sys_menu_socket_repository.currentPlayerId && !isAlreadyFriend && !friendRequests.any { it.id == player.id } && !isSent && player.banStatus != 1
                                                if (canAdd) {
                                                    Box(
                                                        modifier = Modifier
                                                            .height(32.dp)
                                                            .width(44.dp)
                                                            .clickable {
                                                                focusManager.clearFocus()
                                                                onSendFriendRequestClick(player)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Add,
                                                            contentDescription = null,
                                                            tint = AppColors.OnlineGreen,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                } else if (isSent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .height(32.dp)
                                                            .width(44.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = null,
                                                            tint = AppColors.Gray,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                } else {
                                                    Spacer(modifier = Modifier.width(44.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .height(32.dp)
                                                        .width(44.dp)
                                                        .clickable {
                                                            focusManager.clearFocus()
                                                            onFriendInfoClick(player.id, player.name, player.status, player.banStatus, player.banReason)
                                                        },
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
                    .border(1.dp, AppColors.Border)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val currentSearchNickState = rememberUpdatedState(searchNickText)
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        EditText(ctx).apply {
                            setTextColor(android.graphics.Color.WHITE)
                            setHintTextColor(android.graphics.Color.parseColor("#808080"))
                            hint = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vvedite_nikneym_d805)
                            background = null
                            setPadding(0, 0, 0, 0)
                            textSize = 14.2f
                            setSingleLine(true)
                            maxLines = 1
                            gravity = Gravity.CENTER_VERTICAL
                            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                            imeOptions = EditorInfo.IME_ACTION_DONE
                            setOnEditorActionListener { v, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
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
                                    if (currentText != currentSearchNickState.value) {
                                        onSearchNickTextChange(currentText)
                                        if (currentText.isNotEmpty()) onSearchIdTextChange("")
                                    }
                                }
                            })
                        }
                    },
                    update = { editText ->
                        if (editText.text.toString() != searchNickText) {
                            val selection = editText.selectionStart
                            editText.setText(searchNickText)
                            editText.setSelection(selection.coerceIn(0, searchNickText.length))
                        }
                    }
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                val currentSearchIdState = rememberUpdatedState(searchIdText)
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        EditText(ctx).apply {
                            setTextColor(android.graphics.Color.WHITE)
                            setHintTextColor(android.graphics.Color.parseColor("#808080"))
                            hint = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_vvedite_aydi_e8df)
                            background = null
                            setPadding(0, 0, 0, 0)
                            textSize = 14.2f
                            setSingleLine(true)
                            maxLines = 1
                            gravity = Gravity.CENTER_VERTICAL
                            inputType = android.text.InputType.TYPE_CLASS_NUMBER
                            imeOptions = EditorInfo.IME_ACTION_DONE
                            setOnEditorActionListener { v, actionId, _ ->
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
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
                                    if (currentText != currentSearchIdState.value) {
                                        val filtered = currentText.filter { it.isDigit() }
                                        if (filtered != currentText) {
                                            isUpdating = true
                                            setText(filtered)
                                            setSelection(filtered.length)
                                            isUpdating = false
                                        } else {
                                            onSearchIdTextChange(currentText)
                                            if (currentText.isNotEmpty()) onSearchNickTextChange("")
                                        }
                                    }
                                }
                            })
                        }
                    },
                    update = { editText ->
                        if (editText.text.toString() != searchIdText) {
                            val selection = editText.selectionStart
                            editText.setText(searchIdText)
                            editText.setSelection(selection.coerceIn(0, searchIdText.length))
                        }
                    }
                )
            }

            val isNickTooShort = searchNickText.isNotBlank() && searchNickText.trim().length < 2
            val canSearch = !isSearchRefreshing && !searchCooldownActive && !isNickTooShort

            // кнопка поиска
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(100.dp)
                    .background(if (canSearch) AppColors.ActionButtonBg else AppColors.ActionButtonDisabledBg)
                    .border(1.dp, if (canSearch) AppColors.ActionButtonBorder else AppColors.ActionButtonDisabledBorder)
                    .clickable(enabled = canSearch) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        searchCooldownActive = true
                        onPerformSearchClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                ShimmerPlayText(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.find),
                    isShimmering = isSearchRefreshing,
                    enabled = canSearch,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
    }
}
