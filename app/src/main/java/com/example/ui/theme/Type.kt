package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

// основной игровой шрифт из ресурсов
val ShriftIgry = FontFamily(Font(R.font.handjet, FontWeight.Normal))

// базовая типографика приложения
val Typography =
  Typography(
    displayLarge = TextStyle(fontFamily = ShriftIgry, fontWeight = FontWeight.Normal, fontSize = 36.sp),
    titleLarge = TextStyle(fontFamily = ShriftIgry, fontWeight = FontWeight.Normal, fontSize = 24.sp),
    bodyLarge = TextStyle(fontFamily = ShriftIgry, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    labelLarge = TextStyle(fontFamily = ShriftIgry, fontWeight = FontWeight.Normal, fontSize = 14.sp),
  )
