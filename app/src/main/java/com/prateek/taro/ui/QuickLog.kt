package com.prateek.taro.ui

import com.prateek.taro.ui.components.TileRow
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.Panel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import com.prateek.taro.energy.ActivityType
import com.prateek.taro.energy.DetectedSession
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
    var detected by mutableStateOf<DetectedSession?>(null)
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

    state.detected?.let { ride ->
        LogActivityDialog(
            weightKg = AppPreferences.weight,
            onSave = { name, minutes, kcal, day, finishedAt ->
                Database.getInstance(context).addActivity(
                    LoggedActivity(date = day.toString(), name = name, kcal = kcal, durationMinutes = minutes, loggedAt = finishedAt)
                )
                AppPreferences.dismissDetected(ride.start)
                state.detected = null
                toast.show(context.getString(R.string.activity_saved), ToastKind.SUCCESS)
            },
            onDismiss = { state.detected = null },
            initialDate = LocalDate.parse(Util.millisToDateString(ride.end)),
            initialType = ActivityType.CYCLING,
            initialMinutes = ride.minutes,
            initialFinish = ride.end,
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

@Composable
fun DetectedRideCards(state: QuickLogState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sessions by AppPreferences.detectedActivitiesFlow().collectAsStateWithLifecycle(emptyList())
    val cutoff = System.currentTimeMillis() - SHOW_DAYS_MS
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val dayFormat = remember { java.text.SimpleDateFormat("EEE d MMM", java.util.Locale.getDefault()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        sessions.filter { it.end >= cutoff }.sortedByDescending { it.start }.forEach { ride ->
            Panel {
                Text(
                    text = stringResource(R.string.ride_title).uppercase(),
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TaroTheme.colors.special,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                )
                Text(
                    text = stringResource(
                        R.string.ride_text,
                        ride.minutes,
                        dayFormat.format(java.util.Date(ride.start)),
                        format.format(java.util.Date(ride.start)),
                        format.format(java.util.Date(ride.end)),
                    ),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp),
                )
                TileRow(modifier = Modifier.padding(8.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.ride_log),
                        onClick = { state.detected = ride },
                        tint = TaroTheme.colors.special,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.ride_dismiss),
                        onClick = { AppPreferences.dismissDetected(ride.start) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private const val SHOW_DAYS_MS = 3 * 24 * 60 * 60 * 1000L
