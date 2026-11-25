package com.prateek.taro.ui

import com.prateek.taro.ui.components.BoneRows
import com.prateek.taro.ui.components.BoneTile
import com.prateek.taro.ui.components.Skeleton
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.ui.components.ChangeOrRemove
import com.prateek.taro.ui.components.EventCard
import com.prateek.taro.ui.components.CardStats
import com.prateek.taro.ui.components.CardHead
import com.prateek.taro.ui.components.CardActions
import com.prateek.taro.energy.MinuteSample
import com.prateek.taro.energy.ActivityTotals
import com.prateek.taro.energy.ActivityEnergy
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.energy.WeightJournal
import com.prateek.taro.data.WeightLog
import com.prateek.taro.data.FoodLog
import com.prateek.taro.data.LoggedActivity
import com.prateek.taro.data.SleepSession
import com.prateek.taro.energy.DetectedSession
import com.prateek.taro.sleep.SleepRepository
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.DaySwitcher
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import com.prateek.taro.util.Walk
import com.prateek.taro.util.Walks
import java.time.LocalDate
import java.util.Date
import kotlin.math.roundToInt

class DayScreen(private val start: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        DayPage(LocalDate.parse(start), onBack = { navigator.pop() })
    }
}

private sealed interface DayEvent {
    val at: Long
    val key: String

    data class Sleep(val session: SleepSession, val usualMinutes: Long?) : DayEvent {
        override val at get() = session.startAt
        override val key get() = "sleep${session.id}"
    }

    data class Meal(val name: String, val meal: String, val logs: List<FoodLog>) : DayEvent {
        override val at get() = logs.minOf { it.loggedAt }
        override val key get() = "meal$meal"
    }

    data class Stroll(val walk: Walk, val longest: Boolean, val totals: ActivityTotals, val climbM: Double) : DayEvent {
        override val at get() = walk.start
        override val key get() = "walk${walk.start}"
    }

    data class Workout(val activity: LoggedActivity) : DayEvent {
        override val at get() = activity.loggedAt - (activity.durationMinutes ?: DEFAULT_WORKOUT_MINUTES) * MINUTE_MS
        override val key get() = "workout${activity.id}"
    }

    data class Ride(val ride: DetectedSession) : DayEvent {
        override val at get() = ride.start
        override val key get() = "ride${ride.start}"
    }

    data class Weigh(val entry: WeightLog, override val at: Long, val timed: Boolean, val change: Double?, val since: LocalDate?, val trend: Double?) : DayEvent {
        override val key get() = "weight${entry.date}"
    }

    data class Now(override val at: Long) : DayEvent {
        override val key get() = "now"
    }
}

private fun loadDay(context: Context, date: LocalDate): List<DayEvent> {
    val database = Database.getInstance(context)
    val day = date.toString()
    val meals = AppPreferences.meals(context).associate { it.key to it.name }
    val other = context.getString(R.string.meal_other)
    val minuteRows = database.getMinutes(day, day)
    val walks = Walks.merged(Walks.all(minuteRows.map { it.minuteStart to it.steps }), WALK_MERGE_MS)
    fun samples(walk: Walk) = minuteRows.filter { it.minuteStart >= walk.start && it.minuteStart < walk.end }
    val nights = SleepRepository.sessions(context, USUAL_NIGHTS).filter { it.wakeDate != day }
    val usualSleep = nights.takeIf { it.isNotEmpty() }?.map { it.durationMinutes }?.average()?.toLong()
    val longest = walks.maxByOrNull { it.minutes }
    val dayStart = Util.dayStartMillis(date)
    val dayEnd = Util.dayStartMillis(date.plusDays(1))
    val weights = database.weightsSince(date.minusDays(WEIGHT_LOOKBACK_DAYS).toString()).filter { it.date <= day }.sortedBy { it.date }
    val weigh = weights.lastOrNull()?.takeIf { it.date == day }?.let { entry ->
        val previous = weights.getOrNull(weights.lastIndex - 1)
        val timed = entry.loggedAt in dayStart until dayEnd
        val trend = WeightJournal.points(context).firstOrNull { it.date == date }?.trend
        DayEvent.Weigh(entry, if (timed) entry.loggedAt else dayStart, timed, previous?.let { entry.kg - it.kg }, previous?.let { LocalDate.parse(it.date) }, trend)
    }
    return listOfNotNull(weigh) + database.sleepsSince(date.minusDays(1).toString()).filter { it.wakeDate == day }.map { DayEvent.Sleep(it, usualSleep.takeIf { _ -> !it.nap }) } +
        database.foodOn(day).groupBy { it.meal }.map { (key, logs) -> DayEvent.Meal(meals[key] ?: other, key, logs.sortedBy { it.loggedAt }) } +
        walks.map { walk ->
            val rows = samples(walk)
            DayEvent.Stroll(walk, walk == longest && walks.size > 1, ActivityEnergy.minutes(rows.map { MinuteSample(it.steps, it.ascentMeters.toDouble()) }), rows.sumOf { it.ascentMeters.toDouble() })
        } +
        database.activitiesOn(day).map { DayEvent.Workout(it) }
}

@Composable
private fun DayPage(initial: LocalDate, onBack: () -> Unit) {
    val context = LocalContext.current
    val toast = LocalToast.current
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors
    var date by remember { mutableStateOf(initial) }
    val today = Util.logicalToday()
    val dataVersion = rememberDataVersion()
    val rides by AppPreferences.detectedActivitiesFlow().collectAsStateWithLifecycle(emptyList())
    val loaded = rememberInBackground(date, dataVersion) { loadDay(context, date) }?.value
    val now = System.currentTimeMillis()
    val events = loaded?.let { list ->
        (list + rides.filter { Util.millisToDateString(it.end) == date.toString() }.map { DayEvent.Ride(it) } +
            listOfNotNull(DayEvent.Now(now).takeIf { date == today })).sortedBy { it.at }
    }
    val quickLog = rememberQuickLog()
    var editingSleep by remember { mutableStateOf<Pair<SleepSession?, Boolean>?>(null) }
    var editingFood by remember { mutableStateOf<FoodLog?>(null) }
    var editingActivity by remember { mutableStateOf<LoggedActivity?>(null) }
    var editingWeight by remember { mutableStateOf<WeightLog?>(null) }
    var open by remember(date) { mutableStateOf(setOf<String>()) }
    var adding by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.currentOrThrow

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.tight, vertical = spacing.hair),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(androidx.appcompat.R.string.abc_action_bar_up_description), tint = MaterialTheme.colorScheme.onSurface)
            }
            DaySwitcher(date = date, today = today, onChange = { date = it })
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.loose),
        ) {
            Text(stringResource(R.string.day_title), style = TaroTheme.text.greeting, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = spacing.hair, top = spacing.tight))
            BentoNote(stringResource(R.string.day_intro), Modifier.padding(start = spacing.hair, bottom = spacing.loose))
            DayTotals(date, Modifier.padding(bottom = spacing.loose))
            when {
                events == null -> Skeleton { BoneTile(Modifier.fillMaxWidth()) { BoneRows(4) } }
                events.none { it !is DayEvent.Now } -> BentoNote(stringResource(R.string.day_empty), Modifier.padding(top = spacing.loose))
                else -> events.forEachIndexed { index, event ->
                    val expanded = event.key in open
                    fun toggle() {
                        open = if (expanded) open - event.key else open + event.key
                    }
                    TimelineRow(
                        at = event.at,
                        timed = (event as? DayEvent.Weigh)?.timed ?: true,
                        dot = dotColor(event),
                        first = index == 0,
                        last = index == events.lastIndex,
                        now = event is DayEvent.Now,
                    ) {
                        when (event) {
                            is DayEvent.Sleep -> SleepCard(
                                session = event.session,
                                usualMinutes = event.usualMinutes,
                                expanded = expanded,
                                onToggle = ::toggle,
                                onRemove = {
                                    SleepRepository.delete(context, event.session)
                                    toast.show(context.getString(R.string.day_sleep_removed), ToastKind.INFO)
                                },
                                onConfirm = {
                                    SleepRepository.confirm(context, event.session)
                                    toast.show(context.getString(R.string.sleep_saved), ToastKind.SUCCESS)
                                },
                                onChange = { editingSleep = event.session to event.session.nap },
                            )
                            is DayEvent.Meal -> MealCard(
                                event = event,
                                expanded = expanded,
                                onToggle = ::toggle,
                                onEdit = { editingFood = it },
                                onAdd = { navigator.push(AddFoodScreen(date, event.meal)) },
                            )
                            is DayEvent.Stroll -> WalkCard(event, expanded, ::toggle)
                            is DayEvent.Workout -> WorkoutCard(
                                activity = event.activity,
                                expanded = expanded,
                                onToggle = ::toggle,
                                onChange = { editingActivity = event.activity },
                                onRemove = {
                                    Database.getInstance(context).deleteActivity(event.activity.id)
                                    toast.show(context.getString(R.string.activity_deleted), ToastKind.INFO)
                                },
                            )
                            is DayEvent.Ride -> RideCard(
                                ride = event.ride,
                                onLog = { quickLog.detected = event.ride },
                                onDismiss = { AppPreferences.dismissDetected(event.ride.start) },
                            )
                            is DayEvent.Weigh -> WeighCard(
                                event = event,
                                expanded = expanded,
                                onToggle = ::toggle,
                                onChange = { editingWeight = event.entry },
                                onRemove = {
                                    WeightJournal.delete(context, event.entry.date)
                                    toast.show(context.getString(R.string.day_weight_removed), ToastKind.INFO)
                                },
                            )
                            is DayEvent.Now -> NowLine(date)
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier.padding(horizontal = spacing.loose, vertical = spacing.gap)) {
            PrimaryButton(
                text = stringResource(R.string.day_add),
                onClick = { adding = true },
                icon = R.drawable.ic_add,
                modifier = Modifier.fillMaxWidth(),
            )
            MenuDropdown(
                open = adding,
                onDismiss = { adding = false },
                entries = quickLogActions(quickLog, date) + listOf(
                    MenuEntry(R.string.day_add_sleep, R.string.day_add_sleep_hint, R.drawable.ic_sleep, colors.sleep) { editingSleep = null to false },
                    MenuEntry(R.string.day_add_nap, R.string.day_add_nap_hint, R.drawable.ic_sleep, colors.sleep) { editingSleep = null to true },
                ),
            )
        }
    }

    editingSleep?.let { (session, nap) ->
        SleepEntryDialog(
            initialDate = date,
            session = session,
            initialNap = nap,
            onSave = { start, end, isNap ->
                saveSleepEntry(context, session, start, end, isNap)
                editingSleep = null
                toast.show(context.getString(if (isNap) R.string.nap_saved else R.string.sleep_saved), ToastKind.SUCCESS)
            },
            onDismiss = { editingSleep = null },
        )
    }
    editingFood?.let { EditFoodDialog(it, onDismiss = { editingFood = null }) }
    editingActivity?.let { old ->
        LogActivityDialog(
            weightKg = AppPreferences.weight,
            editing = old,
            onSave = { name, minutes, kcal, day, finishedAt ->
                Database.getInstance(context).updateActivity(old.copy(date = day.toString(), name = name, kcal = kcal, durationMinutes = minutes, loggedAt = finishedAt))
                editingActivity = null
                toast.show(context.getString(R.string.activity_saved), ToastKind.SUCCESS)
            },
            onDismiss = { editingActivity = null },
        )
    }
    editingWeight?.let { old ->
        LogWeightDialog(
            title = stringResource(R.string.day_weight_edit),
            initialKg = old.kg,
            initialDate = LocalDate.parse(old.date),
            onSave = { kg, day ->
                WeightJournal.move(context, old.date, day.toString(), kg)
                editingWeight = null
                toast.show(context.getString(R.string.weight_saved), ToastKind.SUCCESS)
            },
            onDismiss = { editingWeight = null },
        )
    }
    QuickLogDialogs(quickLog, date)
}

@Composable
private fun DayTotals(date: LocalDate, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val isToday = date == Util.logicalToday()
    val live = if (isToday) LocalTracking.current.steps else 0
    val dataVersion = rememberDataVersion()
    val totals = rememberInBackground(date, dataVersion, live / LIVE_STEP_BUCKET) {
        val database = Database.getInstance(context)
        val day = date.toString()
        Triple(
            if (isToday) summary(context, Selection.Range(StepRange.TODAY), live) else daySummary(context, date),
            database.foodOn(day).totals(),
            database.sleepsSince(date.minusDays(1).toString()).filter { it.wakeDate == day && !it.nap }.sumOf { it.durationMinutes },
        )
    }?.value
    Panel(modifier = modifier) {
        if (totals == null) return@Panel
        val (steps, food, slept) = totals
        Column(verticalArrangement = Arrangement.spacedBy(spacing.loose), modifier = Modifier.padding(vertical = spacing.gap)) {
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                Fact(Util.formatSteps(steps.steps), stringResource(R.string.day_steps), colors.goal, icon = R.drawable.ic_steps)
                Fact(steps.distanceValue, steps.distanceUnit, colors.goal, icon = R.drawable.ic_distance)
                Fact(steps.caloriesValue, stringResource(R.string.day_active_kcal), colors.flame, icon = R.drawable.ic_calories)
            }
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                Fact(number(food.kcal), stringResource(R.string.day_eaten_kcal), colors.special, icon = R.drawable.ic_calorie_goal)
                Fact(number(food.protein), stringResource(R.string.day_protein_g), PROTEIN_BLUE, icon = R.drawable.ic_badge_bolt)
                Fact(if (slept > 0) minutesText(slept) else "0", stringResource(R.string.day_slept_label), colors.sleep, icon = R.drawable.ic_sleep)
            }
        }
    }
}

@Composable
private fun dotColor(event: DayEvent): Color = when (event) {
    is DayEvent.Sleep -> TaroTheme.colors.sleep
    is DayEvent.Meal -> TaroTheme.colors.special
    is DayEvent.Stroll -> TaroTheme.colors.goal
    is DayEvent.Workout, is DayEvent.Ride -> TaroTheme.colors.flame
    is DayEvent.Weigh, is DayEvent.Now -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun TimelineRow(at: Long, timed: Boolean, dot: Color, first: Boolean, last: Boolean, now: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val timeWidth = with(density) { measurer.measure(TIME_SAMPLE, type.note).size.width.toDp() }
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val label = when {
        now -> stringResource(R.string.day_now)
        timed -> format.format(Date(at)).substringBefore(' ')
        else -> ""
    }
    val titleCenter = with(density) { spacing.gap + type.meterLabel.lineHeight.toDp() / 2 }
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA)
    val dotSize = with(density) { type.note.fontSize.toDp() * DOT_SHARE }
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Text(
            text = label,
            style = type.note,
            color = TaroTheme.colors.accent,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier
                .width(timeWidth)
                .padding(top = titleCenter - with(density) { type.note.lineHeight.toDp() / 2 }),
        )
        Canvas(
            modifier = Modifier
                .width(dotSize * RAIL_SHARE)
                .fillMaxHeight(),
        ) {
            val center = titleCenter.toPx()
            val x = size.width / 2
            val line = dotSize.toPx() * LINE_SHARE
            drawLine(track, Offset(x, if (first) center else 0f), Offset(x, if (last) center else size.height), strokeWidth = line)
            drawCircle(dot, radius = dotSize.toPx() / 2, center = Offset(x, center))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = spacing.gap),
        ) { content() }
    }
}

@Composable
private fun MealCard(event: DayEvent.Meal, expanded: Boolean, onToggle: () -> Unit, onEdit: (FoodLog) -> Unit, onAdd: () -> Unit) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val totals = event.logs.totals()
    EventCard(colors.special, onClick = onToggle) {
        Column {
            CardHead(event.name, number(totals.kcal), colors.special)
            BentoNote(
                text = listOfNotNull(
                    event.logs.joinToString(", ") { it.name },
                    stringResource(R.string.day_protein, number(totals.protein)).takeIf { totals.protein >= 1 },
                ).joinToString(" · "),
            )
            AnimatedVisibility(visible = expanded) {
                Column {
                    val carbs = event.logs.mapNotNull { it.carbs }.takeIf { it.isNotEmpty() }?.sum()
                    val fat = event.logs.mapNotNull { it.fat }.takeIf { it.isNotEmpty() }?.sum()
                    CardStats(
                        listOfNotNull(
                            Triple(colors.special, number(totals.kcal), stringResource(R.string.day_stat_kcal)),
                            Triple(PROTEIN_BLUE, grams(totals.protein), stringResource(R.string.food_protein)),
                            carbs?.let { Triple(colors.accent, grams(it), stringResource(R.string.food_carbs)) },
                            fat?.let { Triple(colors.accent, grams(it), stringResource(R.string.food_fat)) },
                        ),
                    )
                    event.logs.forEach { log ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(spacing.tight))
                                .clickable { onEdit(log) }
                                .padding(vertical = spacing.tight),
                        ) {
                            Text(log.name, style = type.meterValue, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text(number(log.kcal), style = type.meterValue, color = colors.accent, modifier = Modifier.padding(start = spacing.tight))
                        }
                    }
                    LinkText(stringResource(R.string.food_add_more), colors.special, onClick = onAdd, icon = R.drawable.ic_add)
                }
            }
        }
    }
}

@Composable
private fun WalkCard(event: DayEvent.Stroll, expanded: Boolean, onToggle: () -> Unit) {
    val colors = TaroTheme.colors
    val walk = event.walk
    val minutes = walk.minutes.coerceAtLeast(1)
    val cadence = (walk.steps.toDouble() / minutes).roundToInt()
    val distance = Util.metersToDistance(event.totals.distanceM)
    EventCard(colors.goal, onClick = onToggle) {
        Column {
            CardHead(stringResource(R.string.day_walk, walk.minutes), Util.formatSteps(walk.steps), colors.goal, found = true)
            BentoNote(
                text = listOfNotNull(
                    stringResource(R.string.day_walk_longest).takeIf { event.longest },
                    stringResource(R.string.day_walk_cadence, cadence),
                ).joinToString(", "),
            )
            AnimatedVisibility(visible = expanded) {
                CardStats(
                    listOfNotNull(
                        Triple(colors.goal, "%.2f %s".format(java.util.Locale.getDefault(), distance, Util.distanceUnit()), stringResource(R.string.day_stat_distance)),
                        Triple(colors.flame, number(event.totals.activeKcal * AppPreferences.energyFactors.activity), stringResource(R.string.day_stat_kcal)),
                        Triple(colors.accent, cadence.toString(), stringResource(R.string.day_stat_cadence)),
                        event.climbM.takeIf { it >= 1 }?.let { Triple(colors.accent, stringResource(R.string.day_climb, it.roundToInt()), stringResource(R.string.day_stat_climb)) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun WorkoutCard(activity: LoggedActivity, expanded: Boolean, onToggle: () -> Unit, onChange: () -> Unit, onRemove: () -> Unit) {
    val colors = TaroTheme.colors
    EventCard(colors.flame, onClick = onToggle) {
        Column {
            CardHead(
                title = activity.durationMinutes?.let { stringResource(R.string.day_workout, activity.name, it) } ?: activity.name,
                value = number(activity.kcal),
                valueColor = colors.flame,
            )
            BentoNote(stringResource(R.string.day_workout_note))
            AnimatedVisibility(visible = expanded) {
                Column {
                    val minutes = activity.durationMinutes?.takeIf { it > 0 }
                    CardStats(
                        listOfNotNull(
                            minutes?.let { Triple(colors.flame, stringResource(R.string.home_minutes, it.toLong()), stringResource(R.string.day_stat_time)) },
                            Triple(colors.flame, number(activity.kcal), stringResource(R.string.day_stat_kcal)),
                            minutes?.let { Triple(colors.accent, "%.1f".format(java.util.Locale.getDefault(), activity.kcal / it), stringResource(R.string.day_stat_per_minute)) },
                        ),
                    )
                    ChangeOrRemove(true, onChange, onRemove)
                }
            }
        }
    }
}

@Composable
private fun WeighCard(event: DayEvent.Weigh, expanded: Boolean, onToggle: () -> Unit, onChange: () -> Unit, onRemove: () -> Unit) {
    val context = LocalContext.current
    val dayName = event.since?.let { remember(it) { java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(Date(Util.dayStartMillis(it))) } }
    EventCard(TaroTheme.colors.accent, onClick = onToggle) {
        Column {
            CardHead(stringResource(R.string.day_weighed), Util.formatWeight(event.entry.kg), MaterialTheme.colorScheme.onSurface)
            event.change?.let { change ->
                val amount = Util.formatWeight(kotlin.math.abs(change))
                BentoNote(
                    when {
                        kotlin.math.abs(change) < SAME_WEIGHT_KG -> stringResource(R.string.day_weight_same, dayName.orEmpty())
                        change < 0 -> stringResource(R.string.day_weight_down, amount, dayName.orEmpty())
                        else -> stringResource(R.string.day_weight_up, amount, dayName.orEmpty())
                    },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    CardStats(
                        listOfNotNull(
                            Triple(MaterialTheme.colorScheme.onSurface, Util.formatWeight(event.entry.kg), stringResource(R.string.day_stat_scale)),
                            event.change?.let { Triple(TaroTheme.colors.accent, Util.formatWeightChange(it), stringResource(R.string.day_stat_change)) },
                            event.trend?.let { Triple(TaroTheme.colors.goal, Util.formatWeight(it), stringResource(R.string.day_stat_trend)) },
                        ),
                    )
                    ChangeOrRemove(true, onChange, onRemove)
                }
            }
        }
    }
}

@Composable
private fun RideCard(ride: DetectedSession, onLog: () -> Unit, onDismiss: () -> Unit) {
    val colors = TaroTheme.colors
    EventCard(colors.flame) {
        Column {
            CardHead(stringResource(R.string.day_ride, ride.minutes), found = true)
            BentoNote(stringResource(R.string.day_ride_note))
            CardActions {
                LinkText(stringResource(R.string.ride_log), colors.flame, onClick = onLog, icon = R.drawable.ic_add)
                LinkText(stringResource(R.string.day_not_ride), colors.accent, onClick = onDismiss, icon = R.drawable.ic_close)
            }
        }
    }
}

@Composable
private fun NowLine(date: LocalDate) {
    val context = LocalContext.current
    val format = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val dataVersion = rememberDataVersion()
    val left = rememberInBackground(date, dataVersion) {
        AppPreferences.eatTarget(date.toString())?.let { it - Database.getInstance(context).foodOn(date.toString()).totals().kcal }
    }?.value
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = TaroTheme.spacing.gap, vertical = TaroTheme.spacing.gap),
    ) {
        Text(format.format(Date()), style = TaroTheme.text.meterLabel, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        left?.let {
            Text(
                text = if (it >= 0) stringResource(R.string.day_kcal_left, number(it)) else stringResource(R.string.day_kcal_over, number(-it)),
                style = TaroTheme.text.note,
                color = TaroTheme.colors.accent,
            )
        }
    }
}

private const val TIME_SAMPLE = "00:00"
private const val TRACK_ALPHA = 0.12f
private const val DOT_SHARE = 0.7f
private const val RAIL_SHARE = 2.6f
private const val LINE_SHARE = 0.22f
private const val DEFAULT_WORKOUT_MINUTES = 30
private const val MINUTE_MS = 60_000L
private const val USUAL_NIGHTS = 14L
private const val WEIGHT_LOOKBACK_DAYS = 90L
private const val SAME_WEIGHT_KG = 0.05
