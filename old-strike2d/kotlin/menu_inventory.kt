package skayrux.shootout2d

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun inventory_menu(initialTab: String = "inventory") {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 10.dp, start = 20.dp, end = 20.dp, bottom = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InventoryTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_inventar_1d4a), activeTab == "inventory") { activeTab = "inventory" }
                    InventoryTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_magazin_8b1d), activeTab == "shop") { activeTab = "shop" }
                    InventoryTabButton(skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_rynok_692e), activeTab == "market") { activeTab = "market" }
                }

                CoinsBalanceContainer(
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AppColors.Overlay10)
            )

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.str_v_razrabotke_4081),
                    color = AppColors.Border,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun InventoryTabButton(text: String, isActive: Boolean, onClick: () -> Unit) {
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
            fontWeight = FontWeight.Medium
        )
    }
}
