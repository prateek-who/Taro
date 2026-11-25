package com.prateek.taro.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay

private const val BONE_ALPHA = 0.07f
private const val SHINE_ALPHA = 0.09f
private const val SWEEP_MS = 1_400
private const val SHOW_AFTER_MS = 120L
private const val FADE_MS = 300
private const val BAND_SHARE = 0.35f
private const val TEXT_SHARE = 0.62f

@Composable
fun boneColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = BONE_ALPHA)

@Composable
fun Skeleton(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val enabled = rememberAnimationsEnabled()
    val shine = MaterialTheme.colorScheme.onSurface.copy(alpha = SHINE_ALPHA)
    val sweep: State<Float>? = if (enabled) {
        rememberInfiniteTransition(label = "skeleton").animateFloat(0f, 1f, infiniteRepeatable(tween(SWEEP_MS, easing = LinearEasing), RepeatMode.Restart), label = "sweep")
    } else {
        null
    }
    val shown = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(SHOW_AFTER_MS)
        shown.animateTo(1f, tween(FADE_MS))
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = shown.value
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                val progress = sweep?.value ?: return@drawWithContent
                val band = size.width * BAND_SHARE
                val x = -band + (size.width + band * 2) * progress
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, shine, Color.Transparent),
                        start = Offset(x - band, 0f),
                        end = Offset(x + band, band),
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            },
        content = content,
    )
}

@Composable
fun Bone(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(TaroTheme.spacing.tight)) {
    Box(modifier = modifier.clip(shape).background(boneColor()))
}

@Composable
fun BoneText(style: TextStyle, fraction: Float, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val line = with(density) { style.lineHeight.toDp() }
    Box(modifier = modifier.height(line), contentAlignment = Alignment.CenterStart) {
        Bone(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(line * TEXT_SHARE),
            shape = CircleShape,
        )
    }
}

@Composable
fun BoneTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit = { TileBones() }) {
    val spacing = TaroTheme.spacing
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(spacing.loose + spacing.tight))
            .background(TaroTheme.colors.accentOpaque)
            .padding(spacing.loose),
        content = content,
    )
}

@Composable
fun ColumnScope.TileBones(chart: Boolean = true) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    BoneText(type.label, 0.35f)
    BoneText(type.value, 0.55f, Modifier.padding(top = spacing.tight))
    BoneText(type.note, 0.75f)
    if (chart) {
        Spacer(Modifier.weight(1f))
        Bone(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = spacing.gap),
        )
    }
}

@Composable
fun BoneRows(count: Int, modifier: Modifier = Modifier) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val disc = with(LocalDensity.current) { type.meterLabel.lineHeight.toDp() + type.note.lineHeight.toDp() }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.loose), modifier = modifier.fillMaxWidth()) {
        repeat(count) { index ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Bone(Modifier.size(disc), CircleShape)
                Column(modifier = Modifier.padding(start = spacing.gap).weight(1f)) {
                    BoneText(type.meterLabel, if (index % 2 == 0) 0.6f else 0.45f)
                    BoneText(type.note, if (index % 2 == 0) 0.35f else 0.5f)
                }
            }
        }
    }
}

@Composable
fun BoneTilePair(aspect: Float, modifier: Modifier = Modifier) {
    val spacing = TaroTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = modifier.fillMaxWidth()) {
        BoneTile(Modifier.weight(1f).aspectRatio(aspect))
        BoneTile(Modifier.weight(1f).aspectRatio(aspect))
    }
}
