package skayrux.shootout2d

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val TAB_GLAVNYY_RAZDELCHIK = "glavnyy_razdelchik"
const val TAB_FRIENDS = "friends"
const val TAB_SETTINGS = "settings"

object GameMenuState {
    var isVisible by mutableStateOf(false)
    var selectedTab by mutableStateOf(TAB_GLAVNYY_RAZDELCHIK)

    fun resetToGlavnyyRazdelchik() {
        LiveBackgroundState.isEditingControls = false
        selectedTab = TAB_GLAVNYY_RAZDELCHIK
    }
}

data class MatchPlayer(
    val unitIndex: Int,
    val clientId: Int = -1,
    val id: String,
    val name: String,
    val team: Int,
    val isBot: Boolean,
    val isLocal: Boolean
)

fun isGameAdminSecure(): Boolean {
    if (sys_menu_socket_repository.currentPlayerAdminStatus == 1) return true
    val game = datagame_main.instance ?: return false
    val isStatusValid = (game.acstatus == game.acEnc1(game.userstatus)) &&
            (game.ac2status == game.acEnc2(game.userstatus))
    if (!isStatusValid) return false
    if (game.tmplogin && game.userstatus == 1) return false
    return game.userstatus == 1
}

fun isServerHostSecure(): Boolean {
    val game = datagame_main.instance ?: return false
    val isNetServerValid = (game.acnetserver == game.acEnc1(game.netserver.toInt())) &&
            (game.ac2netserver == game.acEnc2(game.netserver.toInt()))
    if (!isNetServerValid) return false
    return (game.netserver.toInt() == 1) || (game.clientid.toInt() == 0)
}

// проверка админа
fun isPlayerAdmin(player: MatchPlayer): Boolean {
    if (player.id.isNotBlank() && sys_menu_socket_repository.isAdminId(player.id)) return true
    val game = datagame_main.instance
    if (game != null) {
        val cid = if (player.clientId in 0 until game.maxunittr) player.clientId
        else if (player.unitIndex in 0 until game.maxunittr && game.uclientid[player.unitIndex] in 0 until game.maxunittr) game.uclientid[player.unitIndex]
        else (0 until game.maxunittr).firstOrNull { game.clientname[it] == player.name } ?: -1
        if (cid in 0 until game.maxunittr) {
            if (game.isClientAdmin(cid)) return true
        }
    }
    val profile = if (player.id.isNotBlank() && player.id != "BOT") sys_menu_socket_repository.getProfileById(player.id) else null
    if (profile?.adminStatus == 1) return true
    return false
}

fun isHostOrAdminSecure(): Boolean {
    return isGameAdminSecure() || isServerHostSecure()
}

fun executeKickPlayer(player: MatchPlayer) {
    val game = datagame_main.instance ?: return
    if (isPlayerAdmin(player)) {
        game.showtoast(Loc.tr(R.string.error_this_is_game_admin))
        return
    }
    val currentIsAdmin = isGameAdminSecure()

    val targetCid = if (player.clientId in 0 until game.maxunittr) {
        player.clientId
    } else if (player.unitIndex in 0 until game.maxunittr && game.uclientid[player.unitIndex] in 0 until game.maxunittr) {
        game.uclientid[player.unitIndex]
    } else {
        (0 until game.maxunittr).firstOrNull { game.clientname[it] == player.name } ?: -1
    }

    if (targetCid !in 0 until game.maxunittr) return
    if (targetCid == game.clientid.toInt()) return // Cannot kick self

    val srvid = game.clientsrvid[targetCid].takeIf { it != 0 } ?: (player.id.toIntOrNull() ?: 0)
    val rid = game.clientrandomid[targetCid]

    if (currentIsAdmin) {
        game.addsysmsg(14, game.clientid.toInt(), targetCid, true)
        game.sendgamedata()
        game.senddisconnectban(targetCid, srvid, rid)
        if (srvid != 0 && !game.bansidlist.contains(srvid)) game.bansidlist.add(srvid)
        if (rid != 0 && !game.banridlist.contains(rid)) game.banridlist.add(rid)

        // бан игрока на сервере в сессии
        if (targetCid in 0 until game.maxunittr) {
            val ip = game.clientip[targetCid]
            if (ip != null) {
                val ipStr = ip.hostAddress
                if (!ipStr.isNullOrBlank() && !game.baniplist.contains(ipStr)) {
                    game.baniplist.add(ipStr)
                }
            }
            val devId = player.id.takeIf { it.isNotBlank() && it != "0" && !it.startsWith("bot_") }
                ?: game.clientsrvstrid.getOrNull(targetCid)?.takeIf { it.isNotBlank() }
            if (!devId.isNullOrBlank() && !game.bandvidlist.contains(devId)) {
                game.bandvidlist.add(devId)
            }
        }
    } else {
        if (game.bangolostimer > 0) {
            game.showtoast(game.getString(R.string.wait_until_the_end))
            return
        }
        if (game.banreqcount > 0) {
            game.showtoast(game.getString(R.string.wait_for_the_next_round))
            return
        }
        game.bangolostimer = game.bangolosper
        game.bandclid = targetCid
        game.bansclid = game.clientid.toInt()
        game.bandsrvid = srvid
        game.bandrid = rid
        game.banza = 1
        game.banprotiv = 0
        game.banreqcount = 1
        game.sendbanreq(game.clientid.toInt(), targetCid, srvid, rid, game.netserver == 1)
        game.sendgamedata()
        game.showtoast(game.getString(R.string.voting_started))
    }
}

// очистка айди игрока от токенов и пустых значений
private fun cleanValidPlayerId(raw: String?, fallbackSrvId: Int = 0): String {
    val trimmed = raw?.trim() ?: ""
    if (trimmed.isNotEmpty()
        && trimmed != "0"
        && !trimmed.equals("N/A", ignoreCase = true)
        && !trimmed.equals("null", ignoreCase = true)
        && !trimmed.startsWith("bot_", ignoreCase = true)
        && !trimmed.contains("-")
        && trimmed.length < 12
    ) {
        return trimmed
    }
    return if (fallbackSrvId > 0) fallbackSrvId.toString() else ""
}

fun getMatchPlayers(): List<MatchPlayer> {
    val game = datagame_main.instance
    val list = mutableListOf<MatchPlayer>()

    if (game != null) {
        val myClientId = game.clientid.toInt()
        val myId = cleanValidPlayerId(sys_menu_socket_repository.currentPlayerId, 0)
            .ifEmpty { cleanValidPlayerId(game.usersrvstrid, game.usersrvid) }
            .ifEmpty { cleanValidPlayerId(AppSettings.getString(game, "driver_account_id", ""), game.usersrvid) }
        val myName = game.plname.takeIf { it.isNotBlank() }
            ?: sys_menu_socket_repository.currentPlayerName.takeIf { it.isNotBlank() } ?: "Player"

        if (!game.netgame) {
            list.add(
                MatchPlayer(
                    unitIndex = if (game.plid != -1) game.plid else 0,
                    clientId = 0,
                    id = myId,
                    name = myName,
                    team = if (game.plteam != -1) game.plteam else 0,
                    isBot = false,
                    isLocal = true
                )
            )
            return list
        }

        list.add(
            MatchPlayer(
                unitIndex = if (game.plid != -1) game.plid else 0,
                clientId = myClientId,
                id = myId,
                name = myName,
                team = if (game.plteam != -1) game.plteam else (if (game.plid in 0 until game.maxunittr && game.untip[game.plid] != -1) game.uteam[game.plid] else 0),
                isBot = false,
                isLocal = true
            )
        )

        for (i in 0 until game.playersclidlist.size) {
            val cid = game.playersclidlist.getOrNull(i) ?: continue
            if (cid == myClientId) continue
            val name = game.playersniklist.getOrNull(i) ?: continue
            if (name.isBlank() || name.equals("null", ignoreCase = true) || name.startsWith("Bot-") || name.startsWith("Bot ")) continue
            val strId = if (i < game.playersstridlist.size) game.playersstridlist.getOrNull(i) else null
            val srvId = (game.playersidlist.getOrNull(i) ?: 0).let { if (it > 0) it else if (cid in 0 until game.maxunittr) game.clientsrvid[cid] else 0 }
            val idStr = cleanValidPlayerId(strId, srvId)
                .ifEmpty { if (cid in 0 until game.maxunittr) cleanValidPlayerId(game.clientsrvstrid.getOrNull(cid), game.clientsrvid[cid]) else "" }
            val uid = (0 until game.maxunittr).firstOrNull { game.uclientid[it] == cid } ?: -1
            val team = if (uid in 0 until game.maxunittr && game.untip[uid] != -1) game.uteam[uid] else 0
            
            if (list.none { it.clientId == cid || (it.name == name && it.id == idStr) }) {
                list.add(
                    MatchPlayer(
                        unitIndex = uid,
                        clientId = cid,
                        id = idStr,
                        name = name,
                        team = team,
                        isBot = false,
                        isLocal = false
                    )
                )
            }
        }

        for (c in 0 until game.maxunittr) {
            if (c == myClientId) continue
            val name = game.clientname[c]
            if (name.isNullOrBlank() || name.equals("null", ignoreCase = true) || name.startsWith("Bot-") || name.startsWith("Bot ")) continue
            val strId = if (c < game.clientsrvstrid.size) game.clientsrvstrid[c] else null
            val srvId = game.clientsrvid[c]
            val idStr = cleanValidPlayerId(strId, srvId)
            val uid = (0 until game.maxunittr).firstOrNull { game.uclientid[it] == c } ?: -1
            val team = if (uid in 0 until game.maxunittr && game.untip[uid] != -1) game.uteam[uid] else 0

            val existingIndex = list.indexOfFirst { it.clientId == c || it.name == name }
            if (existingIndex == -1) {
                list.add(
                    MatchPlayer(
                        unitIndex = uid,
                        clientId = c,
                        id = idStr,
                        name = name,
                        team = team,
                        isBot = false,
                        isLocal = false
                    )
                )
            } else if (list[existingIndex].id.isBlank() && idStr.isNotBlank()) {
                list[existingIndex] = list[existingIndex].copy(id = idStr)
            }
        }

        for (i in 0 until game.maxunittr) {
            if (game.untip[i] == -1 || (game.utip[i] == -1 && game.udeath[i] == 0)) continue
            if (game.ubot[i]) continue
            val cid = game.uclientid[i]
            if (cid == myClientId) continue
            val name = game.uname[i] ?: ""
            if (name.isBlank() || name.equals("null", ignoreCase = true) || name.startsWith("Bot-") || name.startsWith("Bot ")) continue
            val team = game.uteam[i]
            val strId = if (cid in 0 until game.maxunittr && cid < game.clientsrvstrid.size) game.clientsrvstrid[cid] else null
            val srvId = if (cid in 0 until game.maxunittr) game.clientsrvid[cid] else 0
            val idStr = cleanValidPlayerId(strId, srvId)

            val existingIndex = list.indexOfFirst { it.clientId == cid || it.name == name }
            if (existingIndex != -1) {
                val current = list[existingIndex]
                list[existingIndex] = current.copy(unitIndex = i, team = team, id = if (current.id.isBlank()) idStr else current.id)
            } else {
                list.add(
                    MatchPlayer(
                        unitIndex = i,
                        clientId = cid,
                        id = idStr,
                        name = name,
                        team = team,
                        isBot = false,
                        isLocal = false
                    )
                )
            }
        }
    }

    val localPlayer = list.firstOrNull { it.isLocal }
    val otherPlayers = list.filter { !it.isLocal }

    val serverAdmin = otherPlayers.firstOrNull { player ->
        if (game == null) false
        else {
            val cid = player.clientId
            (cid == 0) || (cid in 0 until game.maxunittr && game.clientstatus[cid] == 1)
        }
    }

    val remainingPlayers = if (serverAdmin != null) {
        otherPlayers.filter { it != serverAdmin }.sortedBy { it.clientId }
    } else {
        otherPlayers.sortedBy { it.clientId }
    }

    val orderedList = mutableListOf<MatchPlayer>()
    if (localPlayer != null) {
        orderedList.add(localPlayer)
    }
    if (serverAdmin != null) {
        orderedList.add(serverAdmin)
    }
    orderedList.addAll(remainingPlayers)

    return orderedList
}

@Composable
fun GameMenuScreen(
    onResume: () -> Unit,
    onExit: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }
    var selectedPlayerForInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var kickTargetPlayer by remember { mutableStateOf<MatchPlayer?>(null) }

    BackHandler(enabled = true) {
        if (LiveBackgroundState.isEditingControls) {
            LiveBackgroundState.isEditingControls = false
        } else if (showExitDialog) {
            showExitDialog = false
        } else if (kickTargetPlayer != null) {
            kickTargetPlayer = null
        } else if (selectedPlayerForInfo != null) {
            selectedPlayerForInfo = null
        } else if (GameMenuState.selectedTab != TAB_GLAVNYY_RAZDELCHIK) {
            GameMenuState.selectedTab = TAB_GLAVNYY_RAZDELCHIK
        } else {
            onResume()
        }
    }

    DisposableEffect(Unit) {
        GameMenuState.resetToGlavnyyRazdelchik()
        onDispose {
            GameMenuState.resetToGlavnyyRazdelchik()
        }
    }

    if (showExitDialog) {
        UnifiedDialog(
            title = Loc.tr(R.string.game_menu_leave_server_title),
            content = null,
            confirmText = Loc.tr(R.string.yes),
            cancelText = Loc.tr(R.string.cancel),
            dimBackground = true,
            onConfirm = {
                showExitDialog = false
                onExit()
            },
            onDismiss = {
                showExitDialog = false
            }
        )
    }

    if (kickTargetPlayer != null) {
        val target = kickTargetPlayer!!
        val isAdmin = isGameAdminSecure()
        val titleText = if (isAdmin) Loc.tr(R.string.game_menu_ban_player_title) else Loc.tr(R.string.game_menu_vote_kick_title)
        val descText = if (isAdmin) Loc.tr(R.string.game_menu_ban_confirm_desc, target.name) else Loc.tr(R.string.game_menu_vote_kick_desc, target.name)
        UnifiedDialog(
            title = titleText,
            content = {
                Text(
                    text = descText,
                    color = AppColors.White,
                    fontSize = 14.sp
                )
            },
            confirmText = Loc.tr(R.string.yes),
            cancelText = Loc.tr(R.string.cancel),
            dimBackground = true,
            onConfirm = {
                executeKickPlayer(target)
                kickTargetPlayer = null
            },
            onDismiss = {
                kickTargetPlayer = null
            }
        )
    }

    if (selectedPlayerForInfo != null) {
        val (pId, pName) = selectedPlayerForInfo!!
        FriendInfoDialog(
            playerId = pId,
            playerNameFallback = pName,
            currentUserName = sys_menu_socket_repository.currentPlayerName,
            onDismiss = { selectedPlayerForInfo = null }
        )
    }

    val isEditingControls = GameMenuState.selectedTab == TAB_SETTINGS && LiveBackgroundState.isEditingControls

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0181A20))
    ) {
        if (!isEditingControls) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .zIndex(2f)
            ) {
                GameBokovoeMenu(
                    selectedTab = GameMenuState.selectedTab,
                    onBack = {
                        LiveBackgroundState.isEditingControls = false
                        onResume()
                    },
                    onSelectTab = { tab ->
                        LiveBackgroundState.isEditingControls = false
                        GameMenuState.selectedTab = tab
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isEditingControls) {
                        Modifier.padding(start = 61.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            when (GameMenuState.selectedTab) {
                TAB_GLAVNYY_RAZDELCHIK -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp)
                ) {
                    GlavnyyRazdelchikContent(
                        onExitClick = { showExitDialog = true },
                        onInfoClick = { pId, pName ->
                            selectedPlayerForInfo = Pair(pId, pName)
                        },
                        onKickClick = { player ->
                            if (isPlayerAdmin(player)) {
                                val game = datagame_main.instance
                                game?.showtoast(Loc.tr(R.string.error_this_is_game_admin))
                            } else {
                                kickTargetPlayer = player
                            }
                        }
                    )
                }
                TAB_FRIENDS -> friends_menu(
                    playerName = sys_menu_socket_repository.currentPlayerName,
                    initialTab = "friends"
                )
                TAB_SETTINGS -> settings_menu(isInGame = true)
            }
        }
    }
}

@Composable
fun GlavnyyRazdelchikContent(
    onExitClick: () -> Unit,
    onInfoClick: (String, String) -> Unit,
    onKickClick: (MatchPlayer) -> Unit
) {
    var players by remember { mutableStateOf(getMatchPlayers()) }
    var canKick by remember { mutableStateOf(isHostOrAdminSecure()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            players = getMatchPlayers()
            canKick = isHostOrAdminSecure()
        }
    }
    val rowCount = (players.size + 1) / 2
    val sentRequestsMap = remember { mutableStateMapOf<String, Boolean>() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Loc.tr(R.string.game_menu_pause_title),
                color = AppColors.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = Loc.tr(R.string.game_menu_players_count, players.size),
                color = Color(0xFF8E95A5),
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(AppColors.Divider)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(rowCount) { rowIndex ->
                    val leftIndex = rowIndex * 2
                    val rightIndex = leftIndex + 1
                    val leftPlayer = players.getOrNull(leftIndex)
                    val rightPlayer = players.getOrNull(rightIndex)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (leftPlayer != null) {
                                PlayerMatchRow(
                                    player = leftPlayer,
                                    canKick = canKick,
                                    isRequestSent = sentRequestsMap[leftPlayer.id] == true,
                                    onAddFriend = { targetPlayer ->
                                        sentRequestsMap[targetPlayer.id] = true
                                        scope.launch {
                                            try {
                                                sys_menu_socket_repository.sendFriendRequestOnline(targetPlayer.id)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    onInfoClick = onInfoClick,
                                    onKickClick = onKickClick
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(1.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (rightPlayer != null) {
                                PlayerMatchRow(
                                    player = rightPlayer,
                                    canKick = canKick,
                                    isRequestSent = sentRequestsMap[rightPlayer.id] == true,
                                    onAddFriend = { targetPlayer ->
                                        sentRequestsMap[targetPlayer.id] = true
                                        scope.launch {
                                            try {
                                                sys_menu_socket_repository.sendFriendRequestOnline(targetPlayer.id)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    onInfoClick = onInfoClick,
                                    onKickClick = onKickClick
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .widthIn(min = 110.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(4.dp))
                    .clickable { onExitClick() }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Loc.tr(R.string.game_menu_leave_match),
                    color = AppColors.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PlayerMatchRow(
    player: MatchPlayer,
    canKick: Boolean,
    isRequestSent: Boolean,
    onAddFriend: (MatchPlayer) -> Unit,
    onInfoClick: (String, String) -> Unit,
    onKickClick: (MatchPlayer) -> Unit
) {
    val myId = cleanValidPlayerId(sys_menu_socket_repository.currentPlayerId, 0)
    val cleanId = cleanValidPlayerId(player.id, 0)
    val isLocalUser = player.isLocal || (myId.isNotBlank() && cleanId == myId)
    val isRealPlayer = !player.isBot && cleanId.isNotBlank() && cleanId != "BOT"
    val profile = if (isRealPlayer) sys_menu_socket_repository.getProfileById(cleanId) else null
    val isBanned = profile?.banStatus == 1

    val isAlreadyFriend = isRealPlayer && sys_menu_socket_repository.friendsDetails.any { it.id == cleanId }
    val isSent = isRealPlayer && (isRequestSent || sys_menu_socket_repository.sentRequests.any { it.id == cleanId })
    val canAddFriend = isRealPlayer && !isLocalUser && !isAlreadyFriend && !isSent && !isBanned

    val buttonSize = 29.5.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            userId = if (isRealPlayer) cleanId else "",
            userName = player.name,
            sizeDp = 34,
            fontSizeSp = 15,
            shape = RoundedCornerShape(2.dp),
            isBanned = isBanned,
            onClick = {
                if (isRealPlayer) {
                    onInfoClick(cleanId, player.name)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, end = 4.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (cleanId.isNotBlank()) {
                Text(
                    text = "ID: $cleanId",
                    color = Color(0xFF8E95A5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1
                )
            }
            FormattedMatchPlayerName(
                name = player.name,
                isBanned = isBanned,
                isLocal = isLocalUser,
                alignment = Alignment.Start
            )
        }

        if (canAddFriend) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(3.dp))
                    .clickable { onAddFriend(player) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = Loc.tr(R.string.game_menu_add_friend),
                    tint = AppColors.OnlineGreen,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        } else if (isSent && !isAlreadyFriend && !isLocalUser) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(3.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = Loc.tr(R.string.game_menu_request_sent),
                    tint = AppColors.Gray,
                    modifier = Modifier.size(17.5.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        val isTargetAdmin = isPlayerAdmin(player)
        val canShowKickIcon = canKick && !player.isBot && !isLocalUser && !isTargetAdmin
        if (canShowKickIcon) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(3.dp))
                    .clickable { onKickClick(player) },
                contentAlignment = Alignment.Center
            ) {
                BanIcon(
                    modifier = Modifier.size(16.5.dp),
                    color = Color(0xFFEF5350)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (cleanId.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(RoundedCornerShape(3.dp))
                    .background(AppColors.TabInactive)
                    .border(1.dp, AppColors.Border, RoundedCornerShape(3.dp))
                    .clickable {
                        onInfoClick(cleanId, player.name)
                    },
                contentAlignment = Alignment.Center
            ) {
                iconInfo(modifier = Modifier.size(18.5.dp))
            }
        }
    }
}

@Composable
fun FormattedMatchPlayerName(
    name: String,
    isBanned: Boolean,
    isLocal: Boolean = false,
    alignment: Alignment.Horizontal
) {
    val tagRegex = remember { Regex("^\\[(.*?)\\]\\s*(.*)$") }
    val match = tagRegex.find(name.trim())
    val tag = match?.groupValues?.getOrNull(1)
    val nick = match?.groupValues?.getOrNull(2) ?: name

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignment == Alignment.End) Arrangement.End else Arrangement.Start
    ) {
        if (!tag.isNullOrBlank()) {
            Text(
                text = "[$tag] ",
                color = Color(0xFF8E95A5),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1
            )
        }
        Text(
            text = nick,
            color = if (isBanned) BanStyle.NameColor else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (isBanned) {
            Spacer(modifier = Modifier.width(4.dp))
            BanIcon(
                color = BanStyle.IconColor,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
fun GameBokovoeMenu(
    selectedTab: String,
    onBack: () -> Unit,
    onSelectTab: (String) -> Unit
) {
    // боковое меню паузы с адаптивной высотой элементов
    BoxWithConstraints(
        modifier = Modifier
            .width(61.dp)
            .fillMaxHeight()
    ) {
        val itemH = ((maxHeight - 16.dp) / 4.2f).coerceIn(48.dp, 70.dp)

        Column(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight()
                .background(AppColors.Transparent)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameSideMenuItem(
                painter = painterResource(R.drawable.ic_reply),
                isSelected = false,
                onClick = onBack,
                itemHeight = itemH
            )

            GameSideMenuItem(
                icon = Icons.Filled.PlayArrow,
                isSelected = selectedTab == TAB_GLAVNYY_RAZDELCHIK,
                onClick = { onSelectTab(TAB_GLAVNYY_RAZDELCHIK) },
                itemHeight = itemH
            )

            val showBadge = sys_menu_socket_repository.friendsDetails.any {
                it.unreadSmsCount > 0 && it.id != sys_menu_socket_repository.activeDirectChatFriendId
            }
            GameSideMenuItem(
                icon = Icons.Filled.People,
                isSelected = selectedTab == TAB_FRIENDS,
                onClick = { onSelectTab(TAB_FRIENDS) },
                showBadge = showBadge,
                itemHeight = itemH
            )

            GameSideMenuItem(
                icon = Icons.Filled.Settings,
                isSelected = selectedTab == TAB_SETTINGS,
                onClick = { onSelectTab(TAB_SETTINGS) },
                itemHeight = itemH
            )

            Spacer(modifier = Modifier.weight(1f))
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
fun GameSideMenuItem(
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    painter: Painter? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    showBadge: Boolean = false,
    itemHeight: androidx.compose.ui.unit.Dp = 70.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .width(60.dp)
            .height(itemHeight)
            .background(if (isSelected) AppColors.SurfaceHover else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(3.dp)
                .background(if (isSelected) AppColors.White else Color.Transparent)
                .align(Alignment.CenterStart)
        )

        Box(contentAlignment = Alignment.Center) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = if (isSelected) AppColors.White else AppColors.TextDarkGray,
                    modifier = Modifier.size(24.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AppColors.White else AppColors.TextDarkGray,
                    modifier = Modifier.size(24.dp)
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

object GameMenuHelper {
    @JvmStatic
    fun resetToGlavnyyRazdelchik() {
        GameMenuState.resetToGlavnyyRazdelchik()
    }

    @JvmStatic
    fun setVisible(visible: Boolean) {
        LiveBackgroundState.isEditingControls = false
        GameMenuState.isVisible = visible
        if (visible) {
            GameMenuState.resetToGlavnyyRazdelchik()
        }
    }

    // создание compose view меню паузы
    @JvmStatic
    fun createView(
        context: Context,
        onResume: Runnable,
        onExit: Runnable
    ): ComposeView {
        GameMenuState.resetToGlavnyyRazdelchik()
        return ComposeView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            setContent {
                val customDensity = getCustomDensity()
                val currentLang = AppLanguageManager.currentLanguage
                val baseConfig = LocalConfiguration.current
                val localizedConfig = remember(currentLang, baseConfig, context) {
                    AppLanguageManager.getLocalizedConfiguration(context, currentLang, baseConfig)
                }

                CompositionLocalProvider(
                    LocalDensity provides customDensity,
                    LocalConfiguration provides localizedConfig
                ) {
                    skayrux.shootout2d.ui.theme.Shootout2DTheme {
                        RootDialogOverlayHost {
                            if (GameMenuState.isVisible) {
                                GameMenuScreen(
                                    onResume = { onResume.run() },
                                    onExit = { onExit.run() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
