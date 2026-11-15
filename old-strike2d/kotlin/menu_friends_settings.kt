package skayrux.shootout2d

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FriendsSettingsTabContent(
    noGroupInvites: Boolean,
    onNoGroupInvitesChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SettingRow(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.no_group), drawBottomBorder = false) {
                CustomSwitch(noGroupInvites) {
                    onNoGroupInvitesChange(it)
                }
            }
        }
        Box(modifier = Modifier.weight(1f))
    }
}

