package com.prateek.taro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.prateek.taro.R

val Chivo = FontFamily(
    Font(R.font.chivo_thin, FontWeight.Thin),
    Font(R.font.chivo_extralight, FontWeight.Light),
    Font(R.font.chivo_light, FontWeight.Normal),
    Font(R.font.chivo_regular, FontWeight.SemiBold),
)

val ChivoHeavy = FontFamily(
    Font(R.font.chivo_bold, FontWeight.Bold),
    Font(R.font.chivo_black, FontWeight.Black),
)

private val base = Typography()

private fun TextStyle.chivo() = copy(fontFamily = Chivo)

val TaroTypography = Typography(
    displayLarge = base.displayLarge.chivo(),
    displayMedium = base.displayMedium.chivo(),
    displaySmall = base.displaySmall.chivo(),
    headlineLarge = base.headlineLarge.chivo(),
    headlineMedium = base.headlineMedium.chivo(),
    headlineSmall = base.headlineSmall.chivo(),
    titleLarge = base.titleLarge.chivo(),
    titleMedium = base.titleMedium.chivo(),
    titleSmall = base.titleSmall.chivo(),
    bodyLarge = base.bodyLarge.chivo(),
    bodyMedium = base.bodyMedium.chivo(),
    bodySmall = base.bodySmall.chivo(),
    labelLarge = base.labelLarge.chivo(),
    labelMedium = base.labelMedium.chivo(),
    labelSmall = base.labelSmall.chivo(),
)
