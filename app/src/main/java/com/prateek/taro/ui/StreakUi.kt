package com.prateek.taro.ui

import com.prateek.taro.util.Sprees
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import com.prateek.taro.ui.components.spreeStyles
import com.prateek.taro.ui.components.SpreeBanner
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.prateek.taro.R
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.DayMark
import com.prateek.taro.ui.components.MeterBar
import com.prateek.taro.ui.components.WeekDots
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Challenge
import com.prateek.taro.util.Forgive
import com.prateek.taro.util.StreakMark
import com.prateek.taro.util.Streak
import com.prateek.taro.util.Unanswered
import com.prateek.taro.util.Util
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun rememberStepStreak(dayTotals: Map<LocalDate, Int>, steps: Int): Streak {
    val goalHistory by AppPreferences.goalHistoryFlow().collectAsStateWithLifecycle(AppPreferences.goalHistory)
    val choices by AppPreferences.streakChoicesFlow().collectAsStateWithLifecycle(AppPreferences.streakChoices)
    val preferRest by AppPreferences.streakPreferRestFlow().collectAsStateWithLifecycle(AppPreferences.streakPreferRest)
    return remember(dayTotals, steps, goalHistory, choices, preferRest) { stepStreak(dayTotals, steps, goalHistory, choices, preferRest) }
}

@Composable
fun StreakAskCard(missed: Unanswered, streak: Int, goal: Int, modifier: Modifier = Modifier) {
    val colors = TaroTheme.colors
    val makeUpTarget = Util.formatSteps(goal + missed.shortfall.roundToInt())
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    BentoTile(color = colors.special, label = pluralStringResource(R.plurals.streak_ask_label, streak, streak), tinted = true, modifier = modifier) {
        Column {
            Text(
                text = stringResource(R.string.streak_ask_title, Util.formatSteps(missed.shortfall.roundToInt())),
                style = type.title,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = spacing.tight),
            )
            Text(
                text = if (missed.restAllowed) stringResource(R.string.streak_ask_text) else stringResource(R.string.streak_ask_no_rest, makeUpTarget),
                style = type.note,
                color = colors.accent,
                modifier = Modifier.padding(top = spacing.hair),
            )
            Column(verticalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.padding(top = spacing.loose)) {
                if (missed.restAllowed) {
                    StreakChoice(
                        title = stringResource(R.string.streak_rest),
                        note = stringResource(R.string.streak_rest_note),
                        filled = false,
                        onClick = { AppPreferences.setStreakChoice(missed.date, Forgive.REST) },
                    )
                }
                StreakChoice(
                    title = stringResource(if (missed.restAllowed) R.string.streak_make_up else R.string.streak_make_up_ok),
                    note = stringResource(R.string.streak_make_up_note, makeUpTarget, Util.formatSteps(goal)),
                    filled = true,
                    onClick = { AppPreferences.setStreakChoice(missed.date, Forgive.MAKE_UP) },
                )
            }
        }
    }
}

@Composable
private fun StreakChoice(title: String, note: String, filled: Boolean, onClick: () -> Unit) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val shape = RoundedCornerShape(spacing.loose + spacing.hair)
    val content = if (filled) Color.Black else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (filled) Modifier.background(colors.special) else Modifier.border(spacing.hair / 2, colors.accent.copy(alpha = 0.35f), shape))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.loose, vertical = spacing.gap),
    ) {
        Text(title, style = TaroTheme.text.meterLabel, fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Normal, color = content)
        Text(note, style = TaroTheme.text.note, color = if (filled) content.copy(alpha = 0.7f) else colors.accent)
    }
}

fun weekMarks(streak: Streak, today: LocalDate): List<Pair<LocalDate, DayMark>> {
    val firstMark = streak.marks.keys.minOrNull()
    return (6L downTo 0L).map { today.minusDays(it) }.map { date ->
        date to when (streak.marks[date]) {
            StreakMark.MET -> DayMark.HIT
            StreakMark.MADE_UP -> DayMark.MADE_UP
            StreakMark.REST -> DayMark.REST
            null -> when {
                date == today -> DayMark.TODAY
                firstMark == null || date.isBefore(firstMark) -> DayMark.NONE
                else -> DayMark.MISS
            }
        }
    }
}

@Composable
fun StreakChallengeCard(challenge: Challenge, streak: Streak, steps: Int, goal: Int, today: LocalDate, modifier: Modifier = Modifier) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val target = challenge.target.roundToInt()
    val week = weekMarks(streak, today)
    val dayName = { date: LocalDate -> date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) }
    val notes = week.mapNotNull { (date, mark) ->
        when (mark) {
            DayMark.MADE_UP -> stringResource(R.string.streak_made_up_on, dayName(date), dayName(date.plusDays(1)))
            DayMark.REST -> stringResource(R.string.streak_rested_on, dayName(date))
            else -> null
        }
    }
    BentoTile(color = colors.goal, label = stringResource(R.string.streak_challenge_label), tinted = true, modifier = modifier) {
        BentoValue(Util.formatSteps(steps), MaterialTheme.colorScheme.onSurface, unit = stringResource(R.string.home_of, Util.formatSteps(target)))
        MeterBar(
            fraction = steps.toFloat() / target,
            color = colors.goal,
            marker = goal.toFloat() / target,
            modifier = Modifier.padding(top = spacing.gap),
        )
        Row(modifier = Modifier.padding(top = spacing.tight)) {
            Text(stringResource(R.string.streak_your_goal, Util.formatSteps(goal)), style = TaroTheme.text.note, color = colors.accent, modifier = Modifier.weight(1f))
            Text(
                text = if (steps >= target) stringResource(R.string.streak_made_up_done) else stringResource(R.string.streak_to_go, Util.formatSteps(target - steps)),
                style = TaroTheme.text.note,
                color = colors.accent,
            )
        }
        WeekDots(
            marks = week.map { it.second },
            labels = week.map { it.first.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.loose),
        )
        if (notes.isNotEmpty()) BentoNote(notes.joinToString(" "), Modifier.padding(top = spacing.tight))
    }
}

@Composable
fun StreakSpree(streak: Streak, modifier: Modifier = Modifier) {
    val styles = spreeStyles()
    val days = streak.current
    val spree = Sprees.reached(days)
    val color = styles.getOrNull(spree)?.color ?: TaroTheme.colors.special
    Column(modifier = modifier) {
        BentoValue(days.toString(), color, unit = pluralStringResource(R.plurals.days_word_only, days))
        val next = spree + 1
        if (next > Sprees.LAST) {
            BentoNote(stringResource(R.string.tier_top))
            return@Column
        }
        val target = Sprees.DAYS[next]
        val from = Sprees.DAYS.getOrNull(spree) ?: 0
        BentoNote(pluralStringResource(R.plurals.spree_next, target - days, target - days, stringResource(styles[next].name)))
        MeterBar((days - from).toFloat() / (target - from), color, modifier = Modifier.padding(top = TaroTheme.spacing.tight))
        if (streak.challenge != null) BentoNote(stringResource(R.string.home_making_up), color = TaroTheme.colors.special)
    }
}

@Composable
fun SpreeAnnouncer(streak: Streak, today: LocalDate, modifier: Modifier = Modifier) {
    val animations = rememberAnimationsEnabled()
    var shown by remember { mutableStateOf<Int?>(null) }
    val start = streak.runStart(today)
    val spree = Sprees.reached(streak.current)
    LaunchedEffect(start, spree) {
        if (start == null) return@LaunchedEffect
        val saved = AppPreferences.spreeSeen
        val seen = saved?.split(":")?.takeIf { it.firstOrNull() == start.toString() }?.getOrNull(1)?.toIntOrNull()
        if (seen != null && spree <= seen) return@LaunchedEffect
        AppPreferences.spreeSeen = "$start:$spree"
        if (saved != null && spree >= 0 && animations) shown = spree
    }
    shown?.let { SpreeBanner(it, streak.current, onFinished = { shown = null }, modifier = modifier) }
}
