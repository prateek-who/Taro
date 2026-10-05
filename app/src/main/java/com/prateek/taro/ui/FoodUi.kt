package com.prateek.taro.ui

import com.prateek.taro.ui.components.dayLabel
import com.prateek.taro.ui.components.ToggleGroup
import com.prateek.taro.food.FoodUnit
import com.prateek.taro.food.Units
import kotlinx.coroutines.delay
import androidx.compose.runtime.produceState
import com.prateek.taro.food.LabelScanner
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import com.prateek.taro.ui.components.ActionChip
import com.prateek.taro.ui.components.TimePickerDialog
import com.prateek.taro.ui.components.infoOf
import java.util.Date
import com.prateek.taro.util.DayClock
import com.prateek.taro.ui.components.TimeRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.key
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prateek.taro.ui.components.selectedAll
import com.prateek.taro.util.AppPreferences
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.data.CustomFood
import com.prateek.taro.data.FoodLog
import com.prateek.taro.food.FoodCatalog
import com.prateek.taro.food.FoodItem
import com.prateek.taro.food.MealSlot
import com.prateek.taro.food.Meals
import com.prateek.taro.food.Recipes
import com.prateek.taro.food.Ingredient
import com.prateek.taro.food.Portion
import com.prateek.taro.ui.components.DateRow
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.NumberField
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.PillSelector
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.RangeChip
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.ui.components.TaroTextField
import com.prateek.taro.ui.components.TintChip
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

val PROTEIN_BLUE = Color(0xFF4FC3F7)

private const val SEPARATE_TIME_MS = 15 * 60_000L
private const val SEARCH_DELAY_MS = 120L
private const val HISTORY_LOGS = 400
private const val USUAL_COUNT = 6
private const val RECENT_COUNT = 8
private const val PAST_MEAL_DAYS = 14L
private const val PAST_MEAL_COUNT = 4
private const val SAVED_MEAL_NAME = 40
private const val UNWEIGHED_GRAMS = 100.0

data class FoodTotals(val kcal: Double, val protein: Double)

fun List<FoodLog>.totals() = FoodTotals(sumOf { it.kcal }, sumOf { it.protein })

internal fun number(value: Double) = Util.formatSteps(value.roundToInt())

internal fun grams(value: Double) = "%.0f g".format(Locale.getDefault(), value)

@Composable
fun ProgressLine(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val fill by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "progress")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(TaroTheme.colors.accentOpaque),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fill)
                .height(8.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
fun MealsSection(
    date: LocalDate,
    meals: List<MealSlot>,
    logs: List<FoodLog>,
    onAdd: (String) -> Unit,
    onEdit: (FoodLog) -> Unit,
    onDelete: (FoodLog) -> Unit,
) {
    var managing by remember { mutableStateOf(false) }
    var retiming by remember { mutableStateOf<List<FoodLog>?>(null) }
    val context = LocalContext.current
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val known = meals.map { it.key }.toSet()
    val orphans = logs.filter { it.meal !in known }
    val orders by AppPreferences.mealOrdersFlow().collectAsStateWithLifecycle(AppPreferences.mealOrders)
    val arranged = orders.arrange(meals, date)
    val shown = arranged.filter { meal -> meal.plannedOn(date) || logs.any { it.meal == meal.key } }
    val groups = shown + listOfNotNull(orphans.takeIf { it.isNotEmpty() }?.let { MealSlot("", stringResource(R.string.meal_other)) })
    SectionLabel(
        text = stringResource(R.string.food_meals),
        info = infoOf(R.string.info_meals_title, R.string.info_meals),
        action = {
            ActionChip(
                text = stringResource(R.string.meal_edit_short),
                color = MaterialTheme.colorScheme.onSurface,
                onClick = { managing = true },
                icon = R.drawable.ic_edit,
            )
        },
    )
    Panel {
        groups.forEachIndexed { index, meal ->
            val entries = if (meal.key.isEmpty()) orphans else logs.filter { it.meal == meal.key }
            val start = entries.minOfOrNull { it.loggedAt }
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .height(1.dp)
                        .background(TaroTheme.colors.background.copy(alpha = 0.6f)),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .then(if (meal.key.isNotEmpty()) Modifier.clickable { onAdd(meal.key) } else Modifier)
                    .padding(start = 8.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = meal.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        start?.let { first ->
                            ActionChip(
                                text = timeFormat.format(Date(first)),
                                color = TaroTheme.colors.accent,
                                onClick = { retiming = entries },
                                icon = R.drawable.ic_schedule,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                    Text(
                        text = if (entries.isEmpty()) {
                            stringResource(R.string.meal_empty)
                        } else {
                            entries.totals().let { stringResource(R.string.food_meal_total, number(it.kcal), number(it.protein)) }
                        },
                        fontSize = 12.sp,
                        color = TaroTheme.colors.accent,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (meal.key.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(TaroTheme.colors.goal.copy(alpha = 0.16f)),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = stringResource(R.string.food_add_to, meal.name),
                            tint = TaroTheme.colors.goal,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
            entries.forEach { log ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onEdit(log) }
                        .padding(start = 8.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(log.name, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val ownTime = timeFormat.format(Date(log.loggedAt)).takeIf { start != null && log.loggedAt - start >= SEPARATE_TIME_MS }
                        Text(
                            text = listOfNotNull(ownTime, log.amount, log.grams?.let { Units.format(it, log.unit) }).distinct().joinToString(" · "),
                            fontSize = 12.sp,
                            color = TaroTheme.colors.accent,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.energy_kcal, number(log.kcal)), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.food_protein_short, number(log.protein)), fontSize = 12.sp, color = PROTEIN_BLUE)
                    }
                    IconButton(onClick = { onDelete(log) }) {
                        Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.food_delete, log.name), tint = TaroTheme.colors.accent, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    retiming?.let { entries ->
        val first = entries.minOf { it.loggedAt }
        TimePickerDialog(
            title = stringResource(R.string.meal_time),
            initialHour = DayClock.minuteOfDay(first) / 60,
            initialMinute = DayClock.minuteOfDay(first) % 60,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
            onConfirm = { hour, minute ->
                val shift = Util.momentOf(date, hour * 60 + minute) - first
                Database.getInstance(context).updateFoods(entries.map { it.copy(loggedAt = it.loggedAt + shift) })
                retiming = null
            },
            onDismiss = { retiming = null },
        )
    }

    if (managing) {
        ManageMealsDialog(
            meals = arranged,
            date = date,
            onSave = { list ->
                val edited = list.associateBy { it.key }
                val saved = meals.mapNotNull { edited[it.key] } + list.filter { meal -> meals.none { it.key == meal.key } }
                AppPreferences.saveMeals(saved)
                val keys = list.map { it.key }
                if (orders.arrange(saved, date).map { it.key } != keys) AppPreferences.saveMealOrder(date, keys, meals.map { it.key })
                managing = false
            },
            onDismiss = { managing = false },
        )
    }
}

@Composable
private fun ManageMealsDialog(meals: List<MealSlot>, date: LocalDate, onSave: (List<MealSlot>) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val day = date.toString()
    val used = remember { Database.getInstance(context).usedMealKeys() }
    var list by remember { mutableStateOf(meals.filter { it.plannedOn(date) || it.key in used }) }
    var naming by remember { mutableStateOf<MealSlot?>(null) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeight = 52.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    val handlePx = with(LocalDensity.current) { 56.dp.toPx() }

    fun endDrag() {
        dragKey = null
        dragOffset = 0f
    }

    fun move(from: Int, to: Int) {
        list = list.toMutableList().apply { add(to, removeAt(from)) }
    }

    TaroDialog(
        title = stringResource(R.string.meal_edit_all),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onSave(list) },
    ) {
        Column {
            Column(
                modifier = Modifier.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            if (start.x <= handlePx) {
                                list.getOrNull((start.y / rowPx).toInt())?.let {
                                    dragKey = it.key
                                    dragOffset = 0f
                                }
                            }
                        },
                        onDragEnd = ::endDrag,
                        onDragCancel = ::endDrag,
                        onDrag = { change, amount ->
                            val key = dragKey ?: return@detectDragGestures
                            change.consume()
                            dragOffset += amount.y
                            val index = list.indexOfFirst { it.key == key }
                            if (dragOffset > rowPx / 2 && index in 0 until list.lastIndex) {
                                move(index, index + 1)
                                dragOffset -= rowPx
                            } else if (dragOffset < -rowPx / 2 && index > 0) {
                                move(index, index - 1)
                                dragOffset += rowPx
                            }
                        },
                    )
                },
            ) {
                list.forEach { meal ->
                    key(meal.key) {
                        val dragging = dragKey == meal.key
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(rowHeight)
                                .zIndex(if (dragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (dragging) dragOffset else 0f
                                    shadowElevation = if (dragging) 8.dp.toPx() else 0f
                                    shape = RoundedCornerShape(14.dp)
                                    clip = dragging
                                }
                                .background(if (dragging) TaroTheme.colors.accentOpaque else Color.Transparent),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_drag_handle),
                                contentDescription = stringResource(R.string.meal_drag),
                                tint = TaroTheme.colors.accent,
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(12.dp),
                            )
                            Text(
                                text = meal.name,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (meal.plannedOn(date)) MaterialTheme.colorScheme.onSurface else TaroTheme.colors.accent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { naming = meal }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            )
                            if (!meal.daily) {
                                ActionChip(
                                    text = stringResource(R.string.meal_show_daily),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    onClick = { list = list.map { if (it.key == meal.key) it.copy(daily = true, days = emptySet()) else it } },
                                    icon = R.drawable.ic_add,
                                )
                            }
                            if (if (meal.daily) list.count { it.daily } > 1 else meal.key !in used) {
                                IconButton(onClick = {
                                    list = if (meal.key in used) {
                                        list.map { if (it.key == meal.key) it.copy(daily = false) else it }
                                    } else {
                                        list.filterNot { it.key == meal.key }
                                    }
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = stringResource(R.string.meal_delete_named, meal.name),
                                        tint = TaroTheme.colors.accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.meal_order_hint),
                fontSize = 12.sp,
                color = TaroTheme.colors.accent,
                modifier = Modifier.padding(top = 6.dp),
            )
            SecondaryButton(
                text = stringResource(R.string.meal_add),
                onClick = { naming = MealSlot(Meals.newKey(), "") },
                icon = R.drawable.ic_add,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.meal_delete_hint),
                fontSize = 12.sp,
                color = TaroTheme.colors.accent,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }

    naming?.let { meal ->
        MealDialog(
            title = stringResource(if (meal.name.isEmpty()) R.string.meal_add else R.string.meal_rename),
            initial = meal.name,
            daily = if (meal.daily) true else if (day in meal.days) false else null,
            canLeaveDaily = !meal.daily || list.none { it.key == meal.key } || list.count { it.daily } > 1,
            onSave = { name, daily ->
                val saved = when (daily) {
                    true -> meal.copy(name = name, daily = true, days = emptySet())
                    false -> meal.copy(name = name, daily = false, days = meal.days + day)
                    null -> meal.copy(name = name)
                }
                list = if (list.any { it.key == meal.key }) list.map { if (it.key == meal.key) saved else it } else list + saved
                naming = null
            },
            onDismiss = { naming = null },
        )
    }
}

@Composable
private fun MealDialog(
    title: String,
    initial: String,
    daily: Boolean?,
    canLeaveDaily: Boolean,
    onSave: (String, Boolean?) -> Unit,
    onDismiss: () -> Unit,
) {
    var shows by remember { mutableStateOf(daily) }
    NameDialog(
        title = title,
        label = stringResource(R.string.meal_name),
        initial = initial,
        maxLength = Meals.MAX_NAME,
        clean = Meals::cleanName,
        onSave = { onSave(it, shows) },
        onDismiss = onDismiss,
    ) {
        if (canLeaveDaily) {
            ToggleGroup(
                options = listOf(true to stringResource(R.string.meal_every_day), false to stringResource(R.string.meal_this_day)),
                selected = shows,
                onSelect = { shows = it },
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                text = stringResource(if (shows == true) R.string.meal_every_day_hint else R.string.meal_this_day_hint),
                fontSize = 12.sp,
                color = TaroTheme.colors.accent,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    label: String,
    initial: String,
    maxLength: Int,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    clean: (String) -> String = { it.trim() },
    extra: @Composable () -> Unit = {},
) {
    var name by remember { mutableStateOf(initial.selectedAll()) }
    val cleaned = clean(name.text)
    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onSave(cleaned) },
        confirmEnabled = cleaned.isNotEmpty(),
    ) {
        Column {
            TaroTextField(
                value = name,
                onValueChange = { if (it.text.length <= maxLength) name = it },
                label = label,
                modifier = Modifier.fillMaxWidth(),
            )
            extra()
        }
    }
}

class AddFoodScreen(private val date: LocalDate, private val meal: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AddFoodContent(date, meal, onDone = { navigator.pop() })
    }
}

private sealed interface FoodDialog {
    data class Amount(val item: FoodItem) : FoodDialog
    data object Quick : FoodDialog
    data object Create : FoodDialog
    data class EditCustom(val food: CustomFood) : FoodDialog
    data class PastEntries(val food: CustomFood, val count: Int) : FoodDialog
    data class NameMeal(val logs: List<FoodLog>) : FoodDialog
}

@Composable
private fun AddFoodContent(initialDate: LocalDate, initialMeal: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val toast = LocalToast.current
    var date by remember { mutableStateOf(initialDate) }
    var meal by remember { mutableStateOf(initialMeal) }
    var minute by remember { mutableIntStateOf(DayClock.minuteOfDay(System.currentTimeMillis())) }
    var timeTouched by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    var dialog by remember { mutableStateOf<FoodDialog?>(null) }
    val dataVersion = rememberDataVersion()
    val meals by AppPreferences.mealsFlow(context).collectAsStateWithLifecycle(AppPreferences.meals(context))
    var catalog by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    LaunchedEffect(Unit) { catalog = withContext(Dispatchers.IO) { FoodCatalog.load(context) } }
    val custom = remember(dataVersion) { Database.getInstance(context).customFoods().map(FoodCatalog::custom) }
    val history = rememberRefreshed(dataVersion) { Database.getInstance(context).recentFood(HISTORY_LOGS) }
    val usual = remember(history) {
        history.groupBy { it.name.lowercase() to it.grams }.values
            .filter { it.size >= 2 }
            .sortedByDescending { it.size }
            .take(USUAL_COUNT)
            .map { it.first() }
    }
    val recent = remember(history, usual) {
        val shown = usual.map { it.name.lowercase() to it.grams }.toSet()
        history.distinctBy { it.name.lowercase() to it.grams }.filterNot { (it.name.lowercase() to it.grams) in shown }.take(RECENT_COUNT)
    }
    val boost = remember(history) { history.mapNotNull { it.foodKey }.groupingBy { it }.eachCount() }
    val pastMeals = rememberRefreshed(dataVersion, date, meal) {
        Database.getInstance(context).foodBetween(date.minusDays(PAST_MEAL_DAYS).toString(), date.minusDays(1).toString())
            .filter { it.meal == meal }
            .groupBy { it.date }
            .toSortedMap(compareByDescending { it })
            .values.take(PAST_MEAL_COUNT)
    }
    val usedMeals = rememberRefreshed(dataVersion) { Database.getInstance(context).usedMealKeys() }
    val searchable = remember(catalog, custom) { custom + catalog }
    val results = rememberFoodSearch(searchable, query.text, boost = boost)

    fun copy(logs: List<FoodLog>) {
        val first = logs.minOf { it.loggedAt }
        val shift = Util.momentOf(date, minute) - first
        Database.getInstance(context).logFoods(
            logs.map { it.copy(id = 0, date = date.toString(), meal = meal, loggedAt = it.loggedAt + shift) }
        )
        toast.show(context.resources.getQuantityString(R.plurals.food_copied, logs.size, logs.size), ToastKind.SUCCESS)
        onDone()
    }
    LaunchedEffect(date, meal, meals) {
        if (!timeTouched) {
            minute = if (date == Util.logicalToday()) {
                DayClock.minuteOfDay(System.currentTimeMillis())
            } else {
                Database.getInstance(context).foodOn(date.toString()).filter { it.meal == meal }.maxOfOrNull { it.loggedAt }
                    ?.let(DayClock::minuteOfDay) ?: Meals.usualMinute(meals.firstOrNull { it.key == meal })
            }
        }
    }

    fun save(log: FoodLog) {
        Database.getInstance(context).logFood(log.copy(date = date.toString(), meal = meal, loggedAt = Util.momentOf(date, minute)))
        toast.show(context.getString(R.string.food_logged, log.name), ToastKind.SUCCESS)
        dialog = null
        onDone()
    }

    TaroScaffold(title = stringResource(R.string.food_add_title), onBack = onDone) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            TaroTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.food_search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            PillSelector(
                options = AppPreferences.mealOrders.arrange(meals, date).filter { it.plannedOn(date) || it.key in usedMeals || it.key == meal }.map { it.key to it.name },
                selected = meal,
                onSelect = { meal = it },
                modifier = Modifier.padding(top = 12.dp),
            )
            DateRow(
                label = stringResource(R.string.date_label),
                date = date,
                today = Util.logicalToday(),
                onChange = { date = it },
                modifier = Modifier.padding(top = 10.dp),
            )
            TimeRow(
                label = stringResource(R.string.food_time),
                minuteOfDay = minute,
                onChange = {
                    minute = it
                    timeTouched = true
                },
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                SecondaryButton(stringResource(R.string.food_quick_add), onClick = { dialog = FoodDialog.Quick }, modifier = Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.food_create), onClick = { dialog = FoodDialog.Create }, modifier = Modifier.weight(1f))
            }
            SecondaryButton(
                text = stringResource(R.string.recipe_create),
                onClick = { navigator.push(RecipeScreen(null)) },
                icon = R.drawable.ic_add,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            )

            if (query.text.isBlank()) {
                if (pastMeals.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.food_copy_meal), info = infoOf(R.string.food_copy_meal, R.string.info_copy_meal))
                    Panel {
                        pastMeals.forEach { logs ->
                            val totals = logs.totals()
                            FoodRow(
                                title = dayLabel(LocalDate.parse(logs.first().date), Util.logicalToday()),
                                subtitle = logs.joinToString(", ") { it.name },
                                kcal = totals.kcal,
                                protein = totals.protein,
                                onClick = { copy(logs) },
                                onEdit = { dialog = FoodDialog.NameMeal(logs) },
                                editLabel = stringResource(R.string.food_name_meal),
                            )
                        }
                    }
                }
                if (usual.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.food_usual), info = infoOf(R.string.food_usual, R.string.info_usual_foods))
                    Panel {
                        usual.forEach { log ->
                            FoodRow(
                                title = log.name,
                                subtitle = listOfNotNull(log.amount, log.grams?.let { Units.format(it, log.unit) }).distinct().joinToString(" · "),
                                kcal = log.kcal,
                                protein = log.protein,
                                onClick = { save(log.copy(id = 0)) },
                            )
                        }
                    }
                }
                if (recent.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.food_recent))
                    Panel {
                        recent.forEach { log ->
                            FoodRow(
                                title = log.name,
                                subtitle = listOfNotNull(log.amount, log.grams?.let { Units.format(it, log.unit) }).distinct().joinToString(" · "),
                                kcal = log.kcal,
                                protein = log.protein,
                                onClick = { save(log.copy(id = 0)) },
                            )
                        }
                    }
                    Text(stringResource(R.string.food_recent_hint), fontSize = 12.sp, color = TaroTheme.colors.accent, modifier = Modifier.padding(start = 8.dp, top = 6.dp))
                }
                if (custom.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.food_yours))
                    Panel {
                        custom.forEach { item ->
                            FoodRow(
                                title = item.name,
                                subtitle = if (item.recipeId != null) stringResource(R.string.recipe_per_100g) else stringResource(R.string.food_per_100g, item.unit),
                                unit = item.unit,
                                kcal = item.kcal,
                                protein = item.protein,
                                perHundred = true,
                                onClick = { dialog = FoodDialog.Amount(item) },
                                onEdit = {
                                    val id = item.key.removePrefix("custom:").toLongOrNull()
                                    if (item.recipeId != null) {
                                        navigator.push(RecipeScreen(item.recipeId))
                                    } else {
                                        id?.let(Database.getInstance(context)::customFood)?.let { dialog = FoodDialog.EditCustom(it) }
                                    }
                                },
                            )
                        }
                    }
                }
            } else {
                SectionLabel(stringResource(R.string.food_results))
                if (results.isEmpty()) {
                    Text(stringResource(R.string.food_no_results), color = TaroTheme.colors.accent, modifier = Modifier.padding(start = 8.dp))
                    SecondaryButton(
                        text = stringResource(R.string.food_create_named, query.text.trim()),
                        onClick = { dialog = FoodDialog.Create },
                        icon = R.drawable.ic_add,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                    )
                } else {
                    Panel(modifier = Modifier.padding(bottom = 24.dp)) {
                        results.forEach { item ->
                            FoodRow(
                                title = item.name,
                                subtitle = stringResource(
                                    when (item.source) {
                                        FoodCatalog.SOURCE_CUSTOM -> R.string.food_source_custom
                                        FoodCatalog.SOURCE_INDIAN -> R.string.food_source_indian
                                        else -> R.string.food_source_usda
                                    }
                                ),
                                kcal = item.kcal,
                                protein = item.protein,
                                perHundred = true,
                                unit = item.unit,
                                onClick = { dialog = FoodDialog.Amount(item) },
                            )
                        }
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        is FoodDialog.Amount -> AmountDialog(current.item, initialGrams = null, onSave = ::save, onDismiss = { dialog = null })
        FoodDialog.Quick -> QuickAddDialog(initial = null, onSave = ::save, onDismiss = { dialog = null })
        FoodDialog.Create -> CreateFoodDialog(
            initialName = query.text.trim(),
            onSave = { food ->
                val id = Database.getInstance(context).saveCustomFood(food)
                dialog = FoodDialog.Amount(FoodCatalog.custom(food.copy(id = id)))
            },
            onDismiss = { dialog = null },
        )
        is FoodDialog.EditCustom -> CreateFoodDialog(
            initial = current.food,
            onSave = { food ->
                val database = Database.getInstance(context)
                database.saveCustomFood(food)
                val count = database.pastEntryCount(food)
                if (count > 0) {
                    dialog = FoodDialog.PastEntries(food, count)
                } else {
                    toast.show(context.getString(R.string.food_custom_saved, food.name), ToastKind.SUCCESS)
                    dialog = null
                }
            },
            onDelete = {
                Database.getInstance(context).deleteCustomFood(current.food.id)
                toast.show(context.getString(R.string.recipe_deleted, current.food.name), ToastKind.INFO)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        is FoodDialog.PastEntries -> PastEntriesDialog(current.food, current.count, onDone = { dialog = null })
        is FoodDialog.NameMeal -> NameDialog(
            title = stringResource(R.string.food_name_meal),
            label = stringResource(R.string.meal_name),
            initial = "",
            maxLength = SAVED_MEAL_NAME,
            onSave = { name ->
                val items = current.logs.map {
                    Ingredient(it.foodKey, it.name, it.grams ?: UNWEIGHED_GRAMS, it.kcal, it.protein, it.carbs, it.fat, it.unit?.takeIf { unit -> unit != Units.GRAMS })
                }
                Database.getInstance(context).saveCustomFood(
                    Recipes.build(name, items, cookedGrams = null, servings = 1, servingLabel = context.getString(R.string.recipe_serving))
                )
                toast.show(context.getString(R.string.food_meal_named, name), ToastKind.SUCCESS)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
fun rememberFoodSearch(items: List<FoodItem>, query: String, limit: Int = 40, boost: Map<String, Int> = emptyMap()): List<FoodItem> {
    val results by produceState(emptyList<FoodItem>(), items, query, boost) {
        if (query.isBlank()) {
            value = emptyList()
            return@produceState
        }
        delay(SEARCH_DELAY_MS)
        value = withContext(Dispatchers.Default) { FoodCatalog.search(items, query, limit, boost) }
    }
    return results
}

@Composable
fun PastEntriesDialog(food: CustomFood, count: Int, onDone: () -> Unit) {
    val context = LocalContext.current
    val toast = LocalToast.current
    TaroDialog(
        title = stringResource(R.string.food_past_title),
        onDismiss = {
            toast.show(context.getString(R.string.food_custom_saved, food.name), ToastKind.SUCCESS)
            onDone()
        },
        confirmText = stringResource(R.string.food_past_all),
        dismissText = stringResource(R.string.food_past_future),
        onConfirm = {
            val updated = Database.getInstance(context).recalculateFoodLogs(food)
            toast.show(context.resources.getQuantityString(R.plurals.food_past_updated, updated, updated), ToastKind.SUCCESS)
            onDone()
        },
    ) {
        Text(
            text = context.resources.getQuantityString(R.plurals.food_past_message, count, food.name, count),
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
internal fun FoodRow(
    title: String,
    subtitle: String,
    kcal: Double,
    protein: Double,
    onClick: () -> Unit,
    perHundred: Boolean = false,
    unit: String = Units.GRAMS,
    onEdit: (() -> Unit)? = null,
    editLabel: String? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, fontSize = 12.sp, color = TaroTheme.colors.accent)
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = if (perHundred) stringResource(R.string.food_kcal_per_100, number(kcal), unit) else stringResource(R.string.energy_kcal, number(kcal)),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.food_protein_short, number(protein)), fontSize = 12.sp, color = PROTEIN_BLUE)
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = editLabel ?: stringResource(R.string.recipe_edit, title), tint = TaroTheme.colors.accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AmountDialog(
    item: FoodItem,
    initialGrams: Double?,
    onSave: (FoodLog) -> Unit,
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    val start = initialGrams ?: item.portions.firstOrNull()?.grams ?: 100.0
    val baseUnit = Units.base(item)
    var text by remember { mutableStateOf(TextFieldValue(Util.formatMeasure(start))) }
    var portion by remember { mutableStateOf<Portion?>(item.portions.firstOrNull { it.grams == start }) }
    var unit by remember { mutableStateOf(baseUnit) }
    val entered = Util.parseMeasure(text.text)?.takeIf { it > 0 }
    val amount = entered?.let { Units.toBase(it, unit, item) }?.takeIf { it < 5_000 }

    TaroDialog(
        title = item.name,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = amount != null,
        onConfirm = {
            val grams = amount ?: return@TaroDialog
            onSave(
                FoodLog(
                    date = "",
                    meal = "",
                    name = item.name,
                    grams = grams,
                    amount = portion?.takeIf { it.grams == grams }?.label
                        ?: entered?.takeIf { unit != baseUnit }?.let { "${Util.formatMeasure(it)} ${unit.label}" },
                    unit = item.unit.takeIf { it != Units.GRAMS },
                    kcal = item.kcalFor(grams),
                    protein = item.proteinFor(grams),
                    carbs = item.carbsFor(grams),
                    fat = item.fatFor(grams),
                    foodKey = item.key,
                    loggedAt = 0,
                )
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            header?.invoke()
            NumberField(
                value = text,
                onValueChange = {
                    text = it
                    portion = null
                },
                label = stringResource(R.string.food_amount),
                decimal = true,
                suffix = unit.label,
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PillSelector(
                options = FoodUnit.entries.map { it to it.label },
                selected = unit,
                onSelect = {
                    unit = it
                    portion = null
                },
            )
            if (unit != baseUnit) {
                Text(
                    text = stringResource(
                        if (Units.approximate(unit, item)) R.string.food_unit_approx else R.string.food_unit_equals,
                        Units.format(amount ?: 0.0, item.unit),
                    ),
                    fontSize = 12.sp,
                    color = TaroTheme.colors.accent,
                )
            }
            if (item.portions.isNotEmpty()) {
                FlowRow {
                    item.portions.forEach { option ->
                        RangeChip(
                            label = "${option.label} (${Units.format(option.grams, item.unit)})",
                            selected = portion == option,
                            onClick = {
                                portion = option
                                unit = baseUnit
                                text = TextFieldValue(Util.formatMeasure(option.grams))
                            },
                        )
                    }
                }
            }
            val grams = amount ?: 0.0
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TintChip(stringResource(R.string.energy_kcal, number(item.kcalFor(grams))), TaroTheme.colors.flame)
                TintChip(stringResource(R.string.food_protein_short, number(item.proteinFor(grams))), PROTEIN_BLUE)
            }
            Text(
                text = stringResource(R.string.food_per_100g_values, number(item.kcal), number(item.protein), item.unit),
                fontSize = 12.sp,
                color = TaroTheme.colors.accent,
            )
        }
    }
}

@Composable
fun QuickAddDialog(
    initial: FoodLog?,
    onSave: (FoodLog) -> Unit,
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    var name by remember { mutableStateOf(TextFieldValue(initial?.name.orEmpty())) }
    var kcal by remember { mutableStateOf(TextFieldValue(initial?.kcal?.roundToInt()?.toString().orEmpty())) }
    var protein by remember { mutableStateOf(TextFieldValue(initial?.protein?.let(Util::formatMeasure).orEmpty())) }
    val kcalValue = Util.parseMeasure(kcal.text)?.takeIf { it in 0.0..10_000.0 }
    val proteinValue = if (protein.text.isBlank()) 0.0 else Util.parseMeasure(protein.text)?.takeIf { it in 0.0..500.0 }

    TaroDialog(
        title = stringResource(R.string.food_quick_add),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = name.text.isNotBlank() && kcalValue != null && proteinValue != null,
        onConfirm = {
            onSave(
                (initial ?: FoodLog(date = "", meal = "", name = "", grams = null, amount = null, kcal = 0.0, protein = 0.0, carbs = null, fat = null, foodKey = null, loggedAt = 0))
                    .copy(name = name.text.trim(), kcal = kcalValue ?: 0.0, protein = proteinValue ?: 0.0)
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            header?.invoke()
            TaroTextField(name, { name = it }, stringResource(R.string.food_name), Modifier.fillMaxWidth())
            NumberField(kcal, { kcal = it }, stringResource(R.string.food_kcal), Modifier.fillMaxWidth(), decimal = true, suffix = "kcal", large = true)
            NumberField(protein, { protein = it }, stringResource(R.string.food_protein), Modifier.fillMaxWidth(), decimal = true, suffix = "g")
        }
    }
}

@Composable
internal fun CreateFoodDialog(
    onSave: (CustomFood) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initial: CustomFood? = null,
    onDelete: (() -> Unit)? = null,
) {
    fun field(value: Double?) = TextFieldValue(value?.let(Util::formatMeasure).orEmpty())
    var name by remember { mutableStateOf(TextFieldValue(initial?.name ?: initialName)) }
    var kcal by remember { mutableStateOf(field(initial?.kcal)) }
    var protein by remember { mutableStateOf(field(initial?.protein)) }
    var carbs by remember { mutableStateOf(field(initial?.carbs)) }
    var fat by remember { mutableStateOf(field(initial?.fat)) }
    var serving by remember { mutableStateOf(field(initial?.servingGrams)) }
    var unit by remember { mutableStateOf(initial?.unit ?: Units.GRAMS) }
    fun optional(field: TextFieldValue) = if (field.text.isBlank()) null else Util.parseMeasure(field.text)?.takeIf { it in 0.0..100.0 }
    val carbsValue = optional(carbs)
    val fatValue = optional(fat)
    val macrosValid = (carbs.text.isBlank() || carbsValue != null) && (fat.text.isBlank() || fatValue != null)
    val kcalValue = Util.parseMeasure(kcal.text)?.takeIf { it in 0.0..900.0 }
    val proteinValue = if (protein.text.isBlank()) 0.0 else Util.parseMeasure(protein.text)?.takeIf { it in 0.0..100.0 }
    val servingValue = if (serving.text.isBlank()) null else Util.parseMeasure(serving.text)?.takeIf { it > 0 }
    val context = LocalContext.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    val cameraUri = remember { LabelScanner.photoUri(context) }

    fun read(uri: Uri) {
        scanning = true
        scope.launch {
            val values = runCatching { LabelScanner.read(context, uri) }.getOrNull()
            scanning = false
            if (values == null || values.found == 0) {
                toast.show(context.getString(R.string.scan_nothing), ToastKind.ERROR)
                return@launch
            }
            values.kcal?.let { kcal = field(it) }
            values.protein?.let { protein = field(it) }
            values.carbs?.let { carbs = field(it) }
            values.fat?.let { fat = field(it) }
            values.servingGrams?.let { serving = field(it) }
            unit = values.unit
            toast.show(context.resources.getQuantityString(R.plurals.scan_found, values.found, values.found), ToastKind.SUCCESS)
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken -> if (taken) read(cameraUri) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(::read) }

    TaroDialog(
        title = stringResource(if (initial == null) R.string.food_create else R.string.food_edit_custom),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = name.text.isNotBlank() && kcalValue != null && proteinValue != null && macrosValid && (serving.text.isBlank() || servingValue != null),
        onConfirm = {
            onSave(
                CustomFood(
                    id = initial?.id ?: 0,
                    name = name.text.trim(),
                    kcal = kcalValue ?: 0.0,
                    protein = proteinValue ?: 0.0,
                    carbs = carbsValue,
                    fat = fatValue,
                    servingLabel = servingValue?.let { "1 serving" },
                    servingGrams = servingValue,
                    createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                    unit = unit.takeIf { it != Units.GRAMS },
                )
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.food_create_hint), fontSize = 13.sp, color = TaroTheme.colors.accent)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton(
                    text = stringResource(if (scanning) R.string.scan_reading else R.string.scan_label),
                    onClick = { if (!scanning) camera.launch(cameraUri) },
                    icon = R.drawable.ic_camera,
                    tint = TaroTheme.colors.goal,
                    modifier = Modifier.weight(1f),
                )
                SecondaryButton(
                    text = stringResource(R.string.scan_photo),
                    onClick = { if (!scanning) gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    icon = R.drawable.ic_image,
                    modifier = Modifier.weight(1f),
                )
            }
            ToggleGroup(
                options = listOf(Units.GRAMS to stringResource(R.string.food_per_100g, Units.GRAMS), Units.ML to stringResource(R.string.food_per_100g, Units.ML)),
                selected = unit,
                onSelect = { unit = it },
            )
            TaroTextField(name, { name = it }, stringResource(R.string.food_name), Modifier.fillMaxWidth())
            NumberField(kcal, { kcal = it }, stringResource(R.string.food_kcal_100, unit), Modifier.fillMaxWidth(), decimal = true, suffix = "kcal")
            NumberField(protein, { protein = it }, stringResource(R.string.food_protein_100, unit), Modifier.fillMaxWidth(), decimal = true, suffix = "g")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(carbs, { carbs = it }, stringResource(R.string.food_carbs_100), Modifier.weight(1f), decimal = true, suffix = "g")
                NumberField(fat, { fat = it }, stringResource(R.string.food_fat_100), Modifier.weight(1f), decimal = true, suffix = "g")
            }
            NumberField(serving, { serving = it }, stringResource(R.string.food_serving_optional), Modifier.fillMaxWidth(), decimal = true, suffix = unit)
            if (initial != null) {
                Text(stringResource(R.string.food_edit_custom_hint), fontSize = 12.sp, color = TaroTheme.colors.accent)
            }
            if (onDelete != null) {
                SecondaryButton(stringResource(R.string.food_delete_custom), onClick = onDelete, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun EditFoodDialog(log: FoodLog, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val toast = LocalToast.current
    var item by remember { mutableStateOf<FoodItem?>(null) }
    var resolved by remember { mutableStateOf(false) }
    LaunchedEffect(log.foodKey) {
        item = withContext(Dispatchers.IO) {
            val key = log.foodKey ?: return@withContext null
            if (key.startsWith("custom:")) {
                Database.getInstance(context).customFoods().firstOrNull { "custom:${it.id}" == key }?.let(FoodCatalog::custom)
            } else {
                FoodCatalog.load(context).firstOrNull { it.key == key }
            }
        }
        resolved = true
    }
    if (!resolved) return

    var minute by remember { mutableIntStateOf(DayClock.minuteOfDay(log.loggedAt)) }
    val timeRow: @Composable () -> Unit = {
        TimeRow(label = stringResource(R.string.food_time), minuteOfDay = minute, onChange = { minute = it })
    }

    fun update(updated: FoodLog) {
        val moment = Util.momentOf(LocalDate.parse(log.date), minute)
        Database.getInstance(context).updateFood(updated.copy(id = log.id, date = log.date, meal = log.meal, loggedAt = moment))
        toast.show(context.getString(R.string.food_updated), ToastKind.SUCCESS)
        onDismiss()
    }

    val current = item
    if (current != null && log.grams != null) {
        AmountDialog(current, initialGrams = log.grams, onSave = ::update, onDismiss = onDismiss, header = timeRow)
    } else {
        QuickAddDialog(initial = log, onSave = ::update, onDismiss = onDismiss, header = timeRow)
    }
}

@Composable
fun FoodDayChip(date: String, incompleteDays: Set<String>, modifier: Modifier = Modifier) {
    val incomplete = date in incompleteDays
    ActionChip(
        text = stringResource(if (incomplete) R.string.food_day_incomplete else R.string.food_day_complete),
        color = if (incomplete) MaterialTheme.colorScheme.error else TaroTheme.colors.goal,
        onClick = { AppPreferences.setFoodDayIncomplete(date, !incomplete) },
        icon = if (incomplete) R.drawable.ic_close else R.drawable.ic_check,
        modifier = modifier,
        info = infoOf(R.string.info_incomplete_title, R.string.info_incomplete),
    )
}
