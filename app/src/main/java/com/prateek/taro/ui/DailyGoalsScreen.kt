package com.prateek.taro.ui

import com.prateek.taro.ui.theme.TaroTheme
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import com.prateek.taro.util.StepMoments
import com.prateek.taro.util.StepTiers
import com.prateek.taro.ui.components.tierStyles
import com.prateek.taro.ui.components.TIER_NAMES
import com.prateek.taro.ui.components.MeterBar
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.TaroSwitch
import com.prateek.taro.ui.components.SectionHeader
import com.prateek.taro.ui.components.PreferenceDivider
import com.prateek.taro.ui.components.PreferenceRow
import com.prateek.taro.util.Util
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.edit
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.service.MotionService
import com.prateek.taro.ui.components.SettingsCard
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.AppPreferences.PreferenceKeys
import kotlinx.coroutines.launch

object DailyGoalsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        DailyGoalsContent(onBack = { navigator.pop() })
    }
}

@Composable
private fun DailyGoalsContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val goal by AppPreferences.dailyGoalTargetFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalTarget)
    val goalMoment by AppPreferences.dailyGoalNotificationFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalNotification)
    val progressbar by AppPreferences.dailyGoalNotificationProgressbarFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalNotificationProgressbar)
    val encouraging by AppPreferences.encouragingNotificationsFlow().collectAsStateWithLifecycle(AppPreferences.encouragingNotifications)
    val chartLine by AppPreferences.dailyGoalChartLineFlow().collectAsStateWithLifecycle(AppPreferences.dailyGoalChartLine)
    val everyTier by AppPreferences.tierMomentsAllFlow().collectAsStateWithLifecycle(AppPreferences.tierMomentsAll)
    val sprees by AppPreferences.spreeMomentsFlow().collectAsStateWithLifecycle(AppPreferences.spreeMoments)
    val steps = LocalTracking.current.steps
    var editGoal by remember { mutableStateOf(false) }
    val today = Util.logicalToday()
    val average = rememberInBackground(today) { goalSuggestions(context, today) }?.value?.average
    val hasGoal = goal > 0

    fun update(key: Preferences.Key<Boolean>, value: Boolean) {
        scope.launch { AppPreferences.dataStore.edit { it[key] = value } }
        if (key == PreferenceKeys.DAILY_GOAL_NOTIFICATION_PROGRESSBAR) {
            context.startService(
                Intent(context, MotionService::class.java).apply {
                    action = "UPDATE_NOTIFICATION"
                    putExtra("show_progressbar", value)
                    putExtra("daily_target", goal)
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(back = true, title = stringResource(R.string.daily_goals), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            SettingsCard(tint = colors.goal) {
                PreferenceRow(
                    R.drawable.ic_small_target,
                    stringResource(R.string.daily_goal_target),
                    summary = average?.takeIf { it > 0 }?.let { stringResource(R.string.goals_average, Util.formatSteps(it)) },
                    value = if (hasGoal) Util.formatSteps(goal) else stringResource(R.string.pref_not_set),
                    onClick = { editGoal = true },
                )
            }

            if (hasGoal) {
                SectionHeader(stringResource(R.string.goals_your_notification))
                NotificationPreview(steps = steps, goal = goal, progressbar = progressbar)
                BentoNote(stringResource(R.string.goals_preview_note), Modifier.padding(horizontal = spacing.tight, vertical = spacing.tight))
            }

            Column(modifier = Modifier.alpha(if (hasGoal) 1f else DISABLED_ALPHA)) {
                SectionHeader(stringResource(R.string.goals_tell_me))
                SettingsCard {
                    GoalSwitch(R.drawable.ic_small_target, R.string.goals_reach, null, colors.goal, goalMoment, hasGoal) { update(PreferenceKeys.DAILY_GOAL_NOTIFICATION, it) }
                    PreferenceDivider()
                    GoalSwitch(R.drawable.ic_calories, R.string.goals_each_tier, R.string.goals_each_tier_note, colors.flame, everyTier, hasGoal && goalMoment) { update(PreferenceKeys.TIER_MOMENTS_ALL, it) }
                    PreferenceDivider()
                    GoalSwitch(R.drawable.ic_badge_star, R.string.goals_spree, R.string.goals_spree_note, colors.special, sprees, hasGoal) { update(PreferenceKeys.SPREE_MOMENTS, it) }
                    PreferenceDivider()
                    GoalSwitch(R.drawable.ic_schedule, R.string.goals_behind, R.string.goals_behind_note, colors.goal, encouraging, hasGoal) { update(PreferenceKeys.ENCOURAGING_NOTIFICATIONS, it) }
                }
                SectionHeader(stringResource(R.string.goals_show))
                SettingsCard {
                    GoalSwitch(R.drawable.ic_notification, R.string.progress_in_notification, null, colors.goal, progressbar, hasGoal) { update(PreferenceKeys.DAILY_GOAL_NOTIFICATION_PROGRESSBAR, it) }
                    PreferenceDivider()
                    GoalSwitch(R.drawable.ic_trends, R.string.goal_chart_line, null, colors.goal, chartLine, hasGoal) { update(PreferenceKeys.DAILY_GOAL_CHART_LINE, it) }
                }
            }
        }
    }

    if (editGoal) StepGoalDialog(onDismiss = { editGoal = false })
}

@Composable
private fun GoalSwitch(icon: Int, title: Int, note: Int?, color: Color, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    PreferenceRow(
        icon = icon,
        title = stringResource(title),
        summary = note?.let { stringResource(it) },
        enabled = enabled,
        color = color,
        showChevron = false,
        onClick = { onChange(!checked) },
    ) {
        TaroSwitch(checked = checked, onCheckedChange = onChange, enabled = enabled, color = color)
    }
}

@Composable
private fun NotificationPreview(steps: Int, goal: Int, progressbar: Boolean) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val tiers = tierStyles()
    val tier = StepTiers.reached(steps, goal)
    val stepsText = Util.stepsPlural(context, steps)
    val info = remember(steps / LIVE_STEP_BUCKET, goal) { StepMoments.paceInfo(context, steps, goal) }
    val title = if (tier >= 0) stringResource(R.string.notification_title_tier, stepsText, stringResource(if (tier == 0) R.string.notification_goal_reached else TIER_NAMES[tier])) else stepsText
    val text = when {
        tier < 0 -> {
            val left = goal - steps
            val walk = info?.cadence?.let { stringResource(R.string.notification_to_go_walk, Util.formatSteps(left), (left / it).roundToInt().coerceAtLeast(1)) }
                ?: stringResource(R.string.notification_to_go, Util.formatSteps(left))
            val usually = info?.goalAt?.takeIf { it > System.currentTimeMillis() }?.let { " " + stringResource(R.string.notification_usually_by, StepMoments.timeText(context, it)) }.orEmpty()
            walk + usually
        }
        tier < StepTiers.LAST -> stringResource(R.string.notification_next_tier, Util.formatSteps(StepTiers.threshold(tier + 1, goal) - steps), stringResource(TIER_NAMES[tier + 1]))
        else -> stringResource(R.string.notification_top)
    }
    val fraction = when {
        tier < 0 -> steps.toFloat() / goal
        tier >= StepTiers.LAST -> 1f
        else -> {
            val from = StepTiers.threshold(tier, goal)
            (steps - from).toFloat() / (StepTiers.threshold(tier + 1, goal) - from).coerceAtLeast(1)
        }
    }
    val barColor = if (tier < 0) colors.goal else tiers[(tier + 1).coerceAtMost(StepTiers.LAST)].color
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.tight)) {
            Icon(painterResource(R.drawable.ic_notification), contentDescription = null, tint = colors.goal, modifier = Modifier.size(with(LocalDensity.current) { type.axis.lineHeight.toDp() }))
            Text(stringResource(R.string.goals_preview_app, stringResource(R.string.app_name)), style = type.axis, color = colors.accent)
        }
        Text(title, style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = spacing.tight))
        Text(text, style = type.note, color = colors.accent, modifier = Modifier.padding(top = spacing.hair))
        AnimatedVisibility(visible = progressbar) {
            MeterBar(fraction.coerceIn(0f, 1f), barColor, Modifier.padding(top = spacing.gap))
        }
    }
}

private const val DISABLED_ALPHA = 0.4f
