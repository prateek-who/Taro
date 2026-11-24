package com.prateek.taro.ui

import com.prateek.taro.ui.components.LinkText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.prateek.taro.R
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.drawSparks
import com.prateek.taro.ui.components.tierMultiple
import com.prateek.taro.ui.components.tierStyles
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import com.prateek.taro.util.Bonus
import com.prateek.taro.util.BonusKind
import com.prateek.taro.util.DayScores
import com.prateek.taro.util.Util
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

@Composable
fun TallyCard(tally: DayTally, title: String, play: Boolean, modifier: Modifier = Modifier) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val haptics = LocalHapticFeedback.current
    val score = tally.score
    val animate = play && rememberAnimationsEnabled()
    var shown by remember(tally) { mutableIntStateOf(if (animate) 0 else score.bonuses.size) }
    val counted = remember(tally) { Animatable(if (animate) 0f else score.score.toFloat()) }
    val burst = remember(tally) { Animatable(if (animate) 0f else 1f) }
    val bump = remember(tally) { Animatable(1f) }

    LaunchedEffect(tally, animate) {
        if (!animate) return@LaunchedEffect
        delay(TALLY_START_MS)
        score.bonuses.indices.forEach { index ->
            shown = index + 1
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            launch {
                bump.snapTo(MULT_BUMP)
                bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 500f))
            }
            delay(BONUS_GAP_MS)
        }
        counted.animateTo(score.score.toFloat(), tween(COUNT_MS, easing = FastOutSlowInEasing))
        if (tally.bestOfWeek) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            burst.animateTo(1f, tween(BURST_MS))
        }
    }

    val boxStyle = type.value.copy(fontFamily = type.heavy.fontFamily, fontWeight = type.heavy.fontWeight)
    val best = tally.bestOfWeek && counted.value >= score.score
    BentoTile(color = colors.red, tinted = true, label = title, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.gap),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.gap),
        ) {
            TallyBox(Util.formatSteps(score.steps), colors.goal, boxStyle, Modifier.weight(1f))
            Text("×", style = boxStyle, color = colors.accent)
            TallyBox(
                formatMult(score.multAfter(shown)),
                colors.red,
                boxStyle,
                Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = bump.value
                        scaleY = bump.value
                    },
            )
        }
        Column(modifier = Modifier.padding(top = spacing.gap)) {
            score.bonuses.forEachIndexed { index, bonus ->
                if (index > 0) RowDivider()
                BonusRow(bonus, visible = index < shown)
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.loose),
        ) {
            Text(stringResource(R.string.tally_score).uppercase(), style = type.label, color = colors.accent)
            Box(contentAlignment = Alignment.Center) {
                val flame = colors.flame
                val spark = colors.special
                Canvas(modifier = Modifier.matchParentSize()) {
                    if (tally.bestOfWeek && burst.value < 1f && burst.value > 0f) {
                        drawSparks(burst.value, center, size.width * SPARK_REACH, SPARKS, size.height * SPARK_SIZE) { if (it % 2 == 0) flame else spark }
                    }
                }
                Text(Util.formatSteps(counted.value.roundToInt()), style = type.hero, color = if (best) colors.flame else MaterialTheme.colorScheme.onSurface)
            }
            val note = when {
                best -> stringResource(R.string.tally_week_best)
                tally.weekBest != null -> stringResource(R.string.tally_week_best_so_far, Util.formatSteps(tally.weekBest))
                else -> null
            }
            note?.let { Text(it, style = type.note, color = if (best) colors.flame else colors.accent, textAlign = TextAlign.Center) }
        }
        var rules by rememberSaveable { mutableStateOf(false) }
        LinkText(
            text = stringResource(if (rules) R.string.tally_how_hide else R.string.tally_how),
            color = colors.accent,
            onClick = { rules = !rules },
            trailingIcon = if (rules) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = spacing.gap),
        )
        AnimatedVisibility(visible = rules, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            ScoringRules(score.bonuses.map { it.kind }.toSet())
        }
    }
}

@Composable
private fun ScoringRules(earned: Set<BonusKind>) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val early = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.of(DayScores.EARLY_HOUR, 0)) }
    val plus = { value: Double -> "+" + formatBonus(value) }
    val rules = listOf(
        Triple(BonusKind.GOAL, stringResource(R.string.tally_rule_goal), plus(DayScores.GOAL_BONUS)),
        Triple(BonusKind.TIER, stringResource(R.string.tally_rule_tier), stringResource(R.string.tally_rule_each, plus(DayScores.TIER_BONUS))),
        Triple(BonusKind.STREAK, stringResource(R.string.tally_rule_streak, formatBonus(DayScores.STREAK_MAX_BONUS)), stringResource(R.string.tally_rule_each, plus(DayScores.STREAK_DAY_BONUS))),
        Triple(BonusKind.WALKS, stringResource(R.string.tally_rule_walk, DayScores.WALK_MINUTES), stringResource(R.string.tally_rule_each, plus(DayScores.WALK_BONUS))),
        Triple(BonusKind.EARLY, stringResource(R.string.tally_rule_early, Util.formatSteps(DayScores.EARLY_STEPS), early), plus(DayScores.EARLY_BONUS)),
        Triple(BonusKind.BEST, stringResource(R.string.tally_rule_best), stringResource(R.string.tally_times, formatBonus(DayScores.BEST_TIMES))),
    )
    Column(modifier = Modifier.padding(top = spacing.tight)) {
        rules.forEachIndexed { index, (kind, text, value) ->
            if (index > 0) RowDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.tight),
            ) {
                Text(text, style = type.note, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(end = spacing.gap))
                BonusPill(value, earned = kind in earned)
            }
        }
        Text(stringResource(R.string.tally_rule_note), style = type.note, color = TaroTheme.colors.accent, modifier = Modifier.padding(top = spacing.tight))
    }
}

@Composable
private fun BonusPill(text: String, earned: Boolean) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    Text(
        text = text,
        style = TaroTheme.text.heavy,
        color = if (earned) colors.background else colors.red,
        modifier = Modifier
            .clip(RoundedCornerShape(spacing.tight))
            .background(if (earned) colors.red else colors.red.copy(alpha = UNEARNED_ALPHA))
            .padding(horizontal = spacing.tight, vertical = spacing.hair),
    )
}

@Composable
private fun TallyBox(text: String, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    val spacing = TaroTheme.spacing
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(spacing.gap))
            .background(color)
            .padding(vertical = spacing.gap, horizontal = spacing.tight),
    ) {
        Text(text, style = style, color = TaroTheme.colors.background, maxLines = 1)
    }
}

@Composable
private fun BonusRow(bonus: Bonus, visible: Boolean) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val pop by animateFloatAsState(if (visible) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 420f), label = "bonus pop")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.tight)
            .graphicsLayer {
                alpha = pop.coerceIn(0f, 1f)
                scaleX = POP_FROM + (1 - POP_FROM) * pop
                scaleY = POP_FROM + (1 - POP_FROM) * pop
            },
    ) {
        Text(bonusLabel(bonus), style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        BonusPill(
            text = if (bonus.kind == BonusKind.BEST) stringResource(R.string.tally_times, formatBonus(bonus.value)) else stringResource(R.string.tally_plus, formatBonus(bonus.value)),
            earned = true,
        )
    }
}

@Composable
private fun bonusLabel(bonus: Bonus): String = when (bonus.kind) {
    BonusKind.GOAL -> stringResource(R.string.tally_goal)
    BonusKind.TIER -> stringResource(R.string.tier_multiple, tierMultiple(bonus.count)) + " " + stringResource(tierStyles()[bonus.count].name)
    BonusKind.STREAK -> pluralStringResource(R.plurals.tally_streak, bonus.count, bonus.count)
    BonusKind.WALKS -> pluralStringResource(R.plurals.tally_walks, bonus.count, bonus.count, DayScores.WALK_MINUTES)
    BonusKind.EARLY -> stringResource(R.string.tally_early, remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.of(DayScores.EARLY_HOUR, 0)) })
    BonusKind.BEST -> stringResource(R.string.tally_best)
}

private fun formatMult(value: Double): String = "%.2f".format(value)

private fun formatBonus(value: Double): String = "%.2f".format(value).trimEnd('0').trimEnd('.', ',')

private const val TALLY_START_MS = 400L
private const val BONUS_GAP_MS = 380L
private const val COUNT_MS = 1_100
private const val BURST_MS = 1_000
private const val MULT_BUMP = 1.18f
private const val POP_FROM = 0.8f
private const val UNEARNED_ALPHA = 0.16f
private const val SPARKS = 22
private const val SPARK_REACH = 0.6f
private const val SPARK_SIZE = 0.05f
