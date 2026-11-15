package skayrux.shootout2d

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.ComposeView

object SessionZagruzkaState {
    var isVisible by mutableStateOf(true)
}

@Composable
fun SessionZagruzkaScreen() {
    if (!SessionZagruzkaState.isVisible) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Black),
        contentAlignment = Alignment.Center
    ) {
        val transition = rememberInfiniteTransition(label = "water_shimmer")
        
        val shimmerPos by transition.animateFloat(
            initialValue = -180f,
            targetValue = 520f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmerPos"
        )

        val glowAlpha by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        val shimmerBrush = Brush.linearGradient(
            colors = listOf(
                AppColors.White.copy(alpha = 0.2f * glowAlpha),
                AppColors.White.copy(alpha = glowAlpha),
                AppColors.White.copy(alpha = 0.2f * glowAlpha)
            ),
            start = Offset(shimmerPos, shimmerPos * 0.15f),
            end = Offset(shimmerPos + 220f, shimmerPos * 0.15f + 40f)
        )

        Text(
            text = skayrux.shootout2d.Loc.tr(skayrux.shootout2d.R.string.loading),
            style = TextStyle(
                brush = shimmerBrush,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 12.sp,
                fontFamily = skayrux.shootout2d.ui.theme.ArimoFontFamily
            )
        )
    }
}

object SessionZagruzkaHelper {
    @JvmStatic
    fun setVisible(visible: Boolean) {
        SessionZagruzkaState.isVisible = visible
    }

    @JvmStatic
    fun createView(context: Context): ComposeView {
        return ComposeView(context).apply {
            setContent {
                skayrux.shootout2d.ui.theme.Shootout2DTheme {
                    SessionZagruzkaScreen()
                }
            }
        }
    }
}
