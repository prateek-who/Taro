package com.prateek.taro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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

@Immutable
data class TaroTextStyles(
    val greeting: TextStyle,
    val dialogTitle: TextStyle,
    val title: TextStyle,
    val label: TextStyle,
    val value: TextStyle,
    val unit: TextStyle,
    val note: TextStyle,
    val meterLabel: TextStyle,
    val meterValue: TextStyle,
    val ringValue: TextStyle,
    val ringNote: TextStyle,
    val axis: TextStyle,
    val hero: TextStyle,
    val heroLabel: TextStyle,
    val fact: TextStyle,
    val factCompact: TextStyle,
    val heavy: TextStyle,
    val stamp: TextStyle,
    val storyLine: TextStyle,
    val storyAside: TextStyle,
    val storyNumber: TextStyle,
)

val TaroText = TaroTextStyles(
    greeting = TextStyle(fontFamily = Chivo, fontSize = 22.sp, lineHeight = 28.sp),
    dialogTitle = TextStyle(fontFamily = Chivo, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    title = TextStyle(fontFamily = Chivo, fontSize = 17.sp, lineHeight = 22.sp),
    label = TextStyle(fontFamily = Chivo, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 1.7.sp, fontWeight = FontWeight.SemiBold),
    value = TextStyle(fontFamily = Chivo, fontSize = 32.sp, lineHeight = 38.sp),
    unit = TextStyle(fontFamily = Chivo, fontSize = 15.sp, lineHeight = 19.sp),
    note = TextStyle(fontFamily = Chivo, fontSize = 14.sp, lineHeight = 19.sp),
    meterLabel = TextStyle(fontFamily = Chivo, fontSize = 15.sp, lineHeight = 19.sp),
    meterValue = TextStyle(fontFamily = Chivo, fontSize = 14.sp, lineHeight = 19.sp),
    ringValue = TextStyle(fontFamily = Chivo, fontSize = 30.sp, lineHeight = 34.sp),
    ringNote = TextStyle(fontFamily = Chivo, fontSize = 13.5.sp, lineHeight = 17.sp),
    axis = TextStyle(fontFamily = Chivo, fontSize = 10.5.sp, lineHeight = 13.sp),
    hero = TextStyle(fontFamily = Chivo, fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-0.5).sp),
    heroLabel = TextStyle(fontFamily = Chivo, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold),
    fact = TextStyle(fontFamily = Chivo, fontSize = 21.sp, lineHeight = 26.sp),
    factCompact = TextStyle(fontFamily = Chivo, fontSize = 17.sp, lineHeight = 21.sp),
    heavy = TextStyle(fontFamily = ChivoHeavy, fontWeight = FontWeight.Black, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.6.sp),
    stamp = TextStyle(fontFamily = ChivoHeavy, fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 54.sp, letterSpacing = 0.5.sp),
    storyLine = TextStyle(fontFamily = Chivo, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    storyAside = TextStyle(fontFamily = Chivo, fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 23.sp),
    storyNumber = TextStyle(fontFamily = Chivo, fontSize = 64.sp, lineHeight = 66.sp, letterSpacing = (-2).sp),
)
