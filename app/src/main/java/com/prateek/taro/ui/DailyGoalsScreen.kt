package com.prateek.taro.ui

import com.prateek.taro.util.Util
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.service.MotionService
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.MessageDialog
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.components.SettingsCard
import com.prateek.taro.ui.components.SettingsDivider
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.ui.components.taroTextFieldColors
import com.prateek.taro.ui.components.SwitchRow
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.AppPreferences.PreferenceKeys
import com.prateek.taro.util.GoalNotificationWorker
import kotlinx.coroutines.launch

object DailyGoalsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        DailyGoalsContent(onBack = { navigator.pop() })
    }
}

private data class GoalSettings(
    val targetText: String,
    val notification: Boolean,
    val progressbar: Boolean,
    val encouraging: Boolean,
    val chartLine: Boolean,
) {
    val target: Int get() = targetText.toIntOrNull() ?: 0
}

private fun currentGoalSettings() = GoalSettings(
    targetText = AppPreferences.dailyGoalTarget.toString(),
    notification = AppPreferences.dailyGoalNotification,
    progressbar = AppPreferences.dailyGoalNotificationProgressbar,
    encouraging = AppPreferences.encouragingNotifications,
    chartLine = AppPreferences.dailyGoalChartLine,
)

@Composable
private fun DailyGoalsContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val toast = LocalToast.current

    var saved by remember { mutableStateOf(currentGoalSettings()) }
    var draft by remember { mutableStateOf(saved) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val dirty = draft.copy(targetText = draft.target.toString()) != saved.copy(targetText = saved.target.toString())
    val switchesEnabled = draft.target != 0

    fun save() {
        val settings = draft
        val encouragingTurnedOn = settings.encouraging && !saved.encouraging
        focusManager.clearFocus()
        val today = Util.logicalToday()
        scope.launch {
            AppPreferences.dataStore.edit {
                AppPreferences.setGoal(it, settings.target, today)
                it[PreferenceKeys.DAILY_GOAL_NOTIFICATION] = settings.notification
                it[PreferenceKeys.DAILY_GOAL_NOTIFICATION_PROGRESSBAR] = settings.progressbar
                it[PreferenceKeys.ENCOURAGING_NOTIFICATIONS] = settings.encouraging
                it[PreferenceKeys.DAILY_GOAL_CHART_LINE] = settings.chartLine
            }
            context.startService(
                Intent(context, MotionService::class.java).apply {
                    action = "UPDATE_NOTIFICATION"
                    putExtra("show_progressbar", settings.progressbar)
                    putExtra("daily_target", settings.target)
                }
            )
            if (encouragingTurnedOn) {
                GoalNotificationWorker.resetEncouragingNotificationFlags()
                GoalNotificationWorker.showEncouragingNotification(
                    context.applicationContext,
                    settings.target,
                    AppPreferences.steps,
                    demo = true,
                )
            }
            saved = settings.copy(targetText = settings.target.toString())
            draft = saved
            toast.show(context.getString(R.string.daily_goals_saved), ToastKind.SUCCESS)
        }
    }

    fun leave() {
        if (dirty) confirmDiscard = true else onBack()
    }

    BackHandler(onBack = ::leave)

    TaroScaffold(title = stringResource(R.string.daily_goals), onBack = ::leave) { padding ->
        ScrollingColumn(padding) {
            SettingsCard {
                TextField(
                    value = draft.targetText,
                    onValueChange = { input -> draft = draft.copy(targetText = input.filter(Char::isDigit)) },
                    label = { Text(stringResource(R.string.daily_goal_target)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = taroTextFieldColors(),
                    textStyle = MaterialTheme.typography.headlineSmall,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            SettingsCard(modifier = Modifier.alpha(if (switchesEnabled) 1f else 0.4f)) {
                Column {
                    GoalSwitch(R.string.daily_goal_notification, draft.notification, switchesEnabled) {
                        draft = draft.copy(notification = it)
                    }
                    SettingsDivider()
                    GoalSwitch(R.string.progress_in_notification, draft.progressbar, switchesEnabled) {
                        draft = draft.copy(progressbar = it)
                    }
                    SettingsDivider()
                    GoalSwitch(R.string.encouraging_notifications, draft.encouraging, switchesEnabled) {
                        draft = draft.copy(encouraging = it)
                    }
                    SettingsDivider()
                    GoalSwitch(R.string.goal_chart_line, draft.chartLine, switchesEnabled) {
                        draft = draft.copy(chartLine = it)
                    }
                }
            }

            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = ::save,
                enabled = dirty,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }

    if (confirmDiscard) {
        MessageDialog(
            title = stringResource(R.string.unsaved_changes_title),
            message = stringResource(R.string.unsaved_changes_message),
            confirmText = stringResource(R.string.calibration_discard),
            onConfirm = {
                confirmDiscard = false
                onBack()
            },
            onDismiss = { confirmDiscard = false },
        )
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
