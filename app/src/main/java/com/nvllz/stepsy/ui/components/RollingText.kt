package com.nvllz.stepsy.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle

private const val DIGIT_STAGGER_MS = 35

@Composable
fun RollingText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val tabular = remember(style) { style.copy(fontFeatureSettings = "tnum") }
    fun zeroed() = text.map { if (it.isDigit()) '0' else it }.joinToString("")
    var shown by remember { mutableStateOf(zeroed()) }
    val active = LocalTabActive.current
    LaunchedEffect(text, active) { shown = if (active) text else zeroed() }

    val number = shown.filter(Char::isDigit).toLongOrNull() ?: 0
    val last = remember { mutableLongStateOf(number) }
    val rising = remember(number) { (number >= last.longValue).also { last.longValue = number } }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clearAndSetSemantics { contentDescription = text },
    ) {
        shown.forEachIndexed { index, char ->
            val fromRight = shown.length - index
            key(fromRight) {
                AnimatedContent(
                    targetState = char,
                    transitionSpec = {
                        val direction = if (rising) 1 else -1
                        val delay = fromRight * DIGIT_STAGGER_MS
                        (slideInVertically(tween(420, delay, EaseOutCubic)) { direction * it } + fadeIn(tween(300, delay)))
                            .togetherWith(slideOutVertically(tween(300, delay)) { -direction * it } + fadeOut(tween(200, delay)))
                            .using(SizeTransform(clip = true))
                    },
                    label = "digit",
                ) { Text(it.toString(), style = tabular) }
            }
        }
    }
}
