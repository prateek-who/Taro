package com.prateek.taro.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.data.LoggedActivity
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.food.Meals
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import com.prateek.taro.util.WeightReminderScheduler
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@Stable
class QuickLogState {
    var activity by mutableStateOf(false)
    var weight by mutableStateOf(false)
}

@Composable
fun rememberQuickLog() = remember { QuickLogState() }

@Composable
fun quickLogActions(state: QuickLogState, date: LocalDate): List<MenuEntry> {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    val colors = TaroTheme.colors
    return listOf(
        MenuEntry(R.string.food_log_button, R.string.quick_food_hint, R.drawable.ic_calorie_goal, colors.goal) {
            rootNavigator.push(AddFoodScreen(date, Meals.forTime(AppPreferences.meals(context))))
        },
        MenuEntry(R.string.energy_add_activity, R.string.quick_activity_hint, R.drawable.ic_calories, colors.special) { state.activity = true },
        MenuEntry(R.string.weight_log, R.string.quick_weight_hint, R.drawable.ic_weight, colors.sleep) { state.weight = true },
    )
}

@Composable
fun QuickLogDialogs(state: QuickLogState, date: LocalDate) {
    val context = LocalContext.current
    val toast = LocalToast.current

    if (state.weight) {
        LogWeightDialog(
            title = stringResource(R.string.weight_dialog_title),
            initialKg = remember { WeightJournal.points(context).lastOrNull()?.kg ?: AppPreferences.weight },
            onSave = { kg, day ->
                WeightJournal.log(context, day.toString(), kg)
                WeightReminderScheduler.dismissNotification(context)
                state.weight = false
                toast.show(context.getString(R.string.weight_saved), ToastKind.SUCCESS)
            },
            onDismiss = { state.weight = false },
        )
    }

    if (state.activity) {
        LogActivityDialog(
            weightKg = AppPreferences.weight,
            onSave = { name, minutes, kcal, day, finishedAt ->
                Database.getInstance(context).addActivity(
                    LoggedActivity(date = day.toString(), name = name, kcal = kcal, durationMinutes = minutes, loggedAt = finishedAt)
                )
                state.activity = false
                val message = if (day == Util.logicalToday()) {
                    context.getString(R.string.activity_saved)
                } else {
                    val format = SimpleDateFormat(AppPreferences.dateFormatString, Locale.getDefault())
                    context.getString(R.string.saved_for_date, format.format(Date(Util.dateStringToCalendarMillis(day.toString()))))
                }
                toast.show(message, ToastKind.SUCCESS)
            },
            onDismiss = { state.activity = false },
            initialDate = date,
        )
    }
}
