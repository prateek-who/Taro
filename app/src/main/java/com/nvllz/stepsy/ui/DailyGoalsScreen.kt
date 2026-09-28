package com.nvllz.stepsy.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.R
import com.nvllz.stepsy.service.MotionService
import com.nvllz.stepsy.ui.components.ScrollingColumn
import com.nvllz.stepsy.ui.components.SettingsCard
import com.nvllz.stepsy.ui.components.SettingsDivider
import com.nvllz.stepsy.ui.components.StepsyScaffold
import com.nvllz.stepsy.ui.components.stepsyTextFieldColors
import com.nvllz.stepsy.ui.components.SwitchRow
import com.nvllz.stepsy.util.AppPreferences
import com.nvllz.stepsy.util.AppPreferences.PreferenceKeys
import com.nvllz.stepsy.util.GoalNotificationWorker
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
    val focusManager = LocalFocusManager.current

    var targetFocused by remember { mutableStateOf(false) }
    var targetText by remember { mutableStateOf(AppPreferences.dailyGoalTarget.toString()) }
    val notification by AppPreferences.dailyGoalNotificationFlow()
        .collectAsStateWithLifecycle(AppPreferences.dailyGoalNotification)
    val progressbar by AppPreferences.dailyGoalNotificationProgressbarFlow()
        .collectAsStateWithLifecycle(AppPreferences.dailyGoalNotificationProgressbar)
    val encouraging by AppPreferences.encouragingNotificationsFlow()
        .collectAsStateWithLifecycle(AppPreferences.encouragingNotifications)
    val chartLine by AppPreferences.dailyGoalChartLineFlow()
        .collectAsStateWithLifecycle(AppPreferences.dailyGoalChartLine)

    val target = targetText.toIntOrNull() ?: 0
    val switchesEnabled = target != 0

    fun updateNotification(showProgressbar: Boolean = progressbar) {
        context.startService(
            Intent(context, MotionService::class.java).apply {
                action = "UPDATE_NOTIFICATION"
                putExtra("show_progressbar", showProgressbar)
                putExtra("daily_target", target)
            }
        )
    }

    fun <T> save(key: Preferences.Key<T>, value: T, afterSave: () -> Unit = {}) {
        scope.launch {
            AppPreferences.dataStore.edit { it[key] = value }
            afterSave()
        }
    }

    fun saveTarget() = save(PreferenceKeys.DAILY_GOAL_TARGET, target) { updateNotification() }

    fun leave() {
        saveTarget()
        onBack()
    }

    BackHandler(onBack = ::leave)

    StepsyScaffold(title = stringResource(R.string.daily_goals), onBack = ::leave) { padding ->
        ScrollingColumn(padding) {
            SettingsCard {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { input -> targetText = input.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.daily_goal_target)) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = stepsyTextFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .onFocusChanged {
                            if (targetFocused && !it.isFocused) saveTarget()
                            targetFocused = it.isFocused
                        },
                )
            }

            SettingsCard(modifier = Modifier.alpha(if (switchesEnabled) 1f else 0.4f)) {
                Column {
                    GoalSwitch(R.string.daily_goal_notification, notification, switchesEnabled) {
                        save(PreferenceKeys.DAILY_GOAL_NOTIFICATION, it)
                    }
                    SettingsDivider()
                    GoalSwitch(R.string.progress_in_notification, progressbar, switchesEnabled) {
                        save(PreferenceKeys.DAILY_GOAL_NOTIFICATION_PROGRESSBAR, it) { updateNotification(it) }
                    }
                    SettingsDivider()
                    GoalSwitch(R.string.encouraging_notifications, encouraging, switchesEnabled) { checked ->
                        save(PreferenceKeys.ENCOURAGING_NOTIFICATIONS, checked) {
                            if (checked) {
                                GoalNotificationWorker.resetEncouragingNotificationFlags()
                                GoalNotificationWorker.showEncouragingNotification(
                                    context.applicationContext,
                                    AppPreferences.dailyGoalTarget,
                                    AppPreferences.steps,
                                    demo = true,
                                )
                            }
                        }
                    }
                    SettingsDivider()
                    GoalSwitch(R.string.goal_chart_line, chartLine, switchesEnabled) {
                        save(PreferenceKeys.DAILY_GOAL_CHART_LINE, it) { updateNotification() }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalSwitch(textRes: Int, checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    SwitchRow(
        text = stringResource(textRes),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        contentPadding = PaddingValues(start = 36.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
    )
}
