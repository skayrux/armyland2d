package skayrux.shootout2d.ui.theme

import skayrux.shootout2d.AppColors
import skayrux.shootout2d.R

import androidx.compose.material3.Typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.runtime.CompositionLocalProvider

val Purple80 = AppColors.PurpleLighter
val PurpleGrey80 = AppColors.PurpleLight
val Pink80 = AppColors.PinkLight
val Purple40 = AppColors.PurplePrimary
val PurpleGrey40 = AppColors.PurpleGray
val Pink40 = AppColors.Pinkish

val ArimoFontFamily = FontFamily(
    Font(R.font.arimo_regular, FontWeight.Light),
    Font(R.font.arimo_regular, FontWeight.Normal),
    Font(R.font.arimo_medium, FontWeight.Medium),
    Font(R.font.arimo_semibold, FontWeight.SemiBold),
    Font(R.font.arimo_bold, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = ArimoFontFamily),
    displayMedium = TextStyle(fontFamily = ArimoFontFamily),
    displaySmall = TextStyle(fontFamily = ArimoFontFamily),
    headlineLarge = TextStyle(fontFamily = ArimoFontFamily),
    headlineMedium = TextStyle(fontFamily = ArimoFontFamily),
    headlineSmall = TextStyle(fontFamily = ArimoFontFamily),
    titleLarge = TextStyle(fontFamily = ArimoFontFamily),
    titleMedium = TextStyle(fontFamily = ArimoFontFamily),
    titleSmall = TextStyle(fontFamily = ArimoFontFamily),
    bodyLarge = TextStyle(fontFamily = ArimoFontFamily),
    bodyMedium = TextStyle(fontFamily = ArimoFontFamily),
    bodySmall = TextStyle(fontFamily = ArimoFontFamily),
    labelLarge = TextStyle(fontFamily = ArimoFontFamily),
    labelMedium = TextStyle(fontFamily = ArimoFontFamily),
    labelSmall = TextStyle(fontFamily = ArimoFontFamily)
)

// тема оформления
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Shootout2DTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        MaterialTheme(
            typography = Typography,
            content = content
        )
    }
}
