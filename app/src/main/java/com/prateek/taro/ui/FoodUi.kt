package com.prateek.taro.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.text.style.TextAlign
import com.prateek.taro.food.Macros
import com.prateek.taro.ui.components.IconTile
import com.prateek.taro.ui.components.IconTileGrid
import com.prateek.taro.ui.components.StepperField
import com.prateek.taro.ui.components.WhenDialog
import com.prateek.taro.ui.components.TextTabs
import com.prateek.taro.ui.components.StackBar
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.RowDivider
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
import com.prateek.taro.util.AppPreferences
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.ui.components.NameDialog
import androidx.compose.foundation.layout.Spacer
import com.prateek.taro.ui.components.LinkText
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.AnimatedVisibility
import com.prateek.taro.data.CustomFood
import com.prateek.taro.data.FoodLog
import com.prateek.taro.food.FoodCatalog
import com.prateek.taro.food.FoodItem
import com.prateek.taro.food.MealSlot
import com.prateek.taro.food.Meals
import com.prateek.taro.food.Recipes
import com.prateek.taro.food.Ingredient
import com.prateek.taro.food.Portion
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.NumberField
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TaroTextField
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
val CARB_VIOLET = Color(0xFFB39DFF)
val FAT_CORAL = Color(0xFFFF8A65)

private const val SEPARATE_TIME_MS = 15 * 60_000L
private const val SEARCH_DELAY_MS = 120L
private const val HISTORY_LOGS = 400
private const val USUAL_COUNT = 6
private const val RECENT_COUNT = 8
private const val PAST_MEAL_DAYS = 14L
private const val PAST_MEAL_COUNT = 4
private const val SAVED_MEAL_NAME = 40
private const val UNWEIGHED_GRAMS = 100.0
private const val ACTION_COLUMNS = 3
private const val TRAY_ROOM = 6
private const val ADD_DISC_SHARE = 1.6f
private const val ADD_DISC_ALPHA = 0.18f
private const val BASE_STEP = 10.0
private const val UNIT_STEP = 0.5

data class FoodTotals(val kcal: Double, val protein: Double, val carbs: Double = 0.0, val fat: Double = 0.0)

fun List<FoodLog>.totals() = FoodTotals(sumOf { it.kcal }, sumOf { it.protein }, sumOf { it.carbs ?: 0.0 }, sumOf { it.fat ?: 0.0 })

internal fun number(value: Double) = Util.formatSteps(value.roundToInt())

internal fun grams(value: Double) = "%.0f g".format(Locale.getDefault(), value)

@Composable
fun MealsSection(
    date: LocalDate,
    meals: List<MealSlot>,
    logs: List<FoodLog>,
    onAdd: (String) -> Unit,
    onEdit: (FoodLog) -> Unit,
    onDelete: (FoodLog) -> Unit,
    extraAction: (@Composable () -> Unit)? = null,
) {
    var managing by remember { mutableStateOf(false) }
    var open by remember(date) { mutableStateOf(setOf<String>()) }
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
    )
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors
    val iconSize = with(LocalDensity.current) { type.note.lineHeight.toDp() }
    Panel {
        groups.forEachIndexed { index, meal ->
            val entries = if (meal.key.isEmpty()) orphans else logs.filter { it.meal == meal.key }
            val start = entries.minOfOrNull { it.loggedAt }
            val expanded = meal.key in open && entries.isNotEmpty()
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.tight)
                        .height(Dp.Hairline)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA)),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing.hair)
                    .clip(RoundedCornerShape(spacing.gap + spacing.hair))
                    .clickable(enabled = entries.isNotEmpty() || meal.key.isNotEmpty()) {
                        if (entries.isEmpty()) onAdd(meal.key) else open = if (expanded) open - meal.key else open + meal.key
                    }
                    .padding(horizontal = spacing.tight, vertical = spacing.gap),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.name,
                        style = type.title,
                        color = if (entries.isEmpty()) colors.accent else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (start == null) {
                            stringResource(R.string.meal_usually, timeFormat.format(Date(Util.momentOf(date, Meals.usualMinute(meal)))))
                        } else {
                            (listOf(timeFormat.format(Date(start))) + entries.joinToString(", ") { it.name }).joinToString(" · ")
                        },
                        style = type.note,
                        color = colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = spacing.hair),
                    )
                }
                if (entries.isEmpty()) {
                    if (meal.key.isNotEmpty()) AddCircle(stringResource(R.string.food_add_to, meal.name), iconSize)
                } else {
                    Text(
                        text = number(entries.totals().kcal),
                        style = type.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = spacing.gap),
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(bottom = spacing.tight)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.tight),
                        modifier = Modifier.padding(horizontal = spacing.tight, vertical = spacing.hair),
                    ) {
                        start?.let { first ->
                            LinkText(
                                text = stringResource(R.string.meal_eaten_at, timeFormat.format(Date(first))),
                                color = colors.accent,
                                onClick = { retiming = entries },
                                icon = R.drawable.ic_schedule,
                            )
                        }
                        if (meal.key.isNotEmpty()) {
                            LinkText(
                                text = stringResource(R.string.food_add_more),
                                color = colors.special,
                                onClick = { onAdd(meal.key) },
                                icon = R.drawable.ic_add,
                            )
                        }
                    }
                    entries.forEach { log ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(spacing.gap + spacing.hair))
                                .clickable { onEdit(log) }
                                .padding(start = spacing.tight),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(log.name, style = type.meterValue, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val ownTime = timeFormat.format(Date(log.loggedAt)).takeIf { start != null && log.loggedAt - start >= SEPARATE_TIME_MS }
                                Text(
                                    text = (listOfNotNull(ownTime, log.amount, log.grams?.let { Units.format(it, log.unit) }).distinct() + stringResource(R.string.food_protein_short, number(log.protein))).joinToString(" · "),
                                    style = type.note,
                                    color = colors.accent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(number(log.kcal), style = type.meterValue, color = MaterialTheme.colorScheme.onSurface)
                            IconButton(onClick = { onDelete(log) }) {
                                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.food_delete, log.name), tint = colors.accent, modifier = Modifier.size(iconSize))
                            }
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.tight, vertical = spacing.hair),
        ) {
            LinkText(
                text = stringResource(R.string.meal_edit_all),
                color = colors.accent,
                onClick = { managing = true },
                icon = R.drawable.ic_edit,
            )
            Spacer(Modifier.weight(1f))
            extraAction?.invoke()
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
private fun AddCircle(description: String, iconSize: Dp) {
    val color = TaroTheme.colors.special
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(start = TaroTheme.spacing.gap)
            .clip(CircleShape)
            .background(color)
            .padding(TaroTheme.spacing.tight + TaroTheme.spacing.hair),
    ) {
        Icon(painterResource(R.drawable.ic_add), contentDescription = description, tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(iconSize))
    }
}

private const val DIVIDER_ALPHA = 0.08f

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
                            fun change(updated: MealSlot) {
                                list = list.map { if (it.key == meal.key) updated else it }
                            }
                            if (!meal.plannedOn(date)) {
                                ActionChip(
                                    text = stringResource(R.string.meal_show_today),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    onClick = { change(meal.copy(days = meal.days + day)) },
                                    modifier = Modifier.padding(end = 6.dp),
                                )
                            }
                            if (!meal.daily) {
                                ActionChip(
                                    text = stringResource(R.string.meal_every_day),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    onClick = { change(meal.copy(daily = true, days = emptySet())) },
                                )
                            }
                            if (if (meal.daily) list.count { it.daily } > 1 else day in meal.days) {
                                IconButton(onClick = {
                                    if (meal.key in used) {
                                        change(if (meal.daily) meal.copy(daily = false) else meal.copy(days = meal.days - day))
                                    } else {
                                        list = list.filterNot { it.key == meal.key }
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

class AddFoodScreen(private val date: LocalDate, private val meal: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AddFoodContent(date, meal, onDone = { navigator.pop() })
    }
}

private sealed interface FoodDialog {
    data class Amount(val item: FoodItem, val grams: Double? = null) : FoodDialog
    data class QuickFrom(val log: FoodLog) : FoodDialog
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
    var added by remember { mutableStateOf<List<FoodLog>>(emptyList()) }
    var pickingWhen by remember { mutableStateOf(false) }
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
        val copies = logs.map { it.copy(id = 0, date = date.toString(), meal = meal, loggedAt = it.loggedAt + shift) }
        Database.getInstance(context).logFoods(copies)
        added = added + copies
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
        val entry = log.copy(id = 0, date = date.toString(), meal = meal, loggedAt = Util.momentOf(date, minute))
        Database.getInstance(context).logFood(entry)
        added = added + entry
        dialog = null
    }

    fun open(log: FoodLog) {
        val item = log.foodKey?.let { key -> searchable.firstOrNull { it.key == key } }
        dialog = if (item != null) FoodDialog.Amount(item, log.grams) else FoodDialog.QuickFrom(log)
    }

    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors
    val mealName = meals.firstOrNull { it.key == meal }?.name ?: stringResource(R.string.meal_other)
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val addLabel = stringResource(R.string.food_add_to_meal, mealName.lowercase())
    fun summary(log: FoodLog) = listOfNotNull(log.amount, log.grams?.let { Units.format(it, log.unit) }).distinct().joinToString(" · ")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(
            back = true,
            title = mealName,
            center = {
                LinkText(
                    stringResource(R.string.food_when_value, dayLabel(date, Util.logicalToday()).lowercase(), timeFormat.format(Date(Util.momentOf(date, minute)))),
                    colors.accent,
                    onClick = { pickingWhen = true },
                    trailingIcon = R.drawable.ic_edit,
                )
            },
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = spacing.loose, end = spacing.loose, bottom = if (added.isEmpty()) spacing.loose else spacing.loose * TRAY_ROOM),
            ) {
                SearchField(query, { query = it }, stringResource(R.string.food_search))
                IconTileGrid(
                    listOf(
                        Triple(R.drawable.ic_badge_bolt, stringResource(R.string.food_quick_add), colors.special) to { dialog = FoodDialog.Quick },
                        Triple(R.drawable.ic_camera, stringResource(R.string.food_create), colors.goal) to { dialog = FoodDialog.Create },
                        Triple(R.drawable.ic_recipe, stringResource(R.string.food_recipe_tile), colors.flame) to { navigator.push(RecipeScreen(null)) },
                    ),
                    columns = ACTION_COLUMNS,
                    modifier = Modifier.padding(top = spacing.gap),
                ) { (look, action), modifier ->
                    IconTile(look.first, look.second, look.third, modifier, selected = false, onClick = action)
                }

                if (query.text.isBlank()) {
                    if (usual.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.food_usual), info = infoOf(R.string.food_usual, R.string.info_usual_foods))
                        Panel {
                            usual.forEachIndexed { index, log ->
                                if (index > 0) RowDivider()
                                FoodRow(
                                    title = log.name,
                                    subtitle = summary(log),
                                    kcal = log.kcal,
                                    protein = log.protein,
                                    carbs = log.carbs,
                                    fat = log.fat,
                                    onClick = { open(log) },
                                    onAdd = { save(log) },
                                )
                            }
                        }
                    }
                    if (pastMeals.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.food_copy_meal), info = infoOf(R.string.food_copy_meal, R.string.info_copy_meal))
                        Panel {
                            pastMeals.forEachIndexed { index, logs ->
                                if (index > 0) RowDivider()
                                val totals = logs.totals()
                                FoodRow(
                                    title = dayLabel(LocalDate.parse(logs.first().date), Util.logicalToday()),
                                    subtitle = logs.joinToString(", ") { it.name },
                                    kcal = totals.kcal,
                                    protein = totals.protein,
                                    carbs = totals.carbs,
                                    fat = totals.fat,
                                    onClick = { copy(logs) },
                                    action = stringResource(R.string.food_copy),
                                    onEdit = { dialog = FoodDialog.NameMeal(logs) },
                                    editLabel = stringResource(R.string.food_name_meal),
                                )
                            }
                        }
                    }
                    if (recent.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.food_recent))
                        Panel {
                            recent.forEachIndexed { index, log ->
                                if (index > 0) RowDivider()
                                FoodRow(
                                    title = log.name,
                                    subtitle = summary(log),
                                    kcal = log.kcal,
                                    protein = log.protein,
                                    carbs = log.carbs,
                                    fat = log.fat,
                                    onClick = { open(log) },
                                    onAdd = { save(log) },
                                )
                            }
                        }
                    }
                    if (custom.isNotEmpty()) {
                        SectionLabel(stringResource(R.string.food_yours))
                        Panel {
                            custom.forEachIndexed { index, item ->
                                if (index > 0) RowDivider()
                                FoodRow(
                                    title = item.name,
                                    subtitle = if (item.recipeId != null) stringResource(R.string.recipe_per_100g) else stringResource(R.string.food_per_100g, item.unit),
                                    unit = item.unit,
                                    kcal = item.kcal,
                                    protein = item.protein,
                                    carbs = item.carbs,
                                    fat = item.fat,
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
                        BentoNote(stringResource(R.string.food_no_results), Modifier.padding(start = spacing.tight))
                        LinkText(
                            stringResource(R.string.food_create_named, query.text.trim()),
                            colors.special,
                            onClick = { dialog = FoodDialog.Create },
                            icon = R.drawable.ic_add,
                            modifier = Modifier.padding(top = spacing.tight),
                        )
                    } else {
                        Panel {
                            results.forEachIndexed { index, item ->
                                if (index > 0) RowDivider()
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
                                    carbs = item.carbs,
                                    fat = item.fat,
                                    perHundred = true,
                                    unit = item.unit,
                                    onClick = { dialog = FoodDialog.Amount(item) },
                                )
                            }
                        }
                    }
                }
            }
            if (added.isNotEmpty()) {
                FoodTray(added, mealName, onDone, Modifier.align(Alignment.BottomCenter).padding(spacing.gap))
            }
        }
    }

    if (pickingWhen) {
        var draftMeal by remember { mutableStateOf(meal) }
        WhenDialog(
            title = stringResource(R.string.food_where_title),
            date = date,
            minuteOfDay = minute,
            timeLabel = stringResource(R.string.food_time),
            onSave = { day, at ->
                date = day
                minute = at
                timeTouched = true
                meal = draftMeal
                pickingWhen = false
            },
            onDismiss = { pickingWhen = false },
            top = {
                Column {
                    AppPreferences.mealOrders.arrange(meals, date).filter { it.plannedOn(date) || it.key in usedMeals || it.key == meal }.forEach { slot ->
                        ChoiceRow(slot.name, slot.key == draftMeal) { draftMeal = slot.key }
                    }
                }
            },
        )
    }

    when (val current = dialog) {
        is FoodDialog.Amount -> AmountDialog(current.item, initialGrams = current.grams, onSave = ::save, onDismiss = { dialog = null }, confirmLabel = addLabel)
        is FoodDialog.QuickFrom -> QuickAddDialog(initial = current.log, onSave = ::save, onDismiss = { dialog = null })
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
    carbs: Double? = null,
    fat: Double? = null,
    onAdd: (() -> Unit)? = null,
    action: String? = null,
) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val colors = TaroTheme.colors
    val iconSize = with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = type.title, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotBlank()) Text(subtitle, style = type.note, color = colors.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(IntrinsicSize.Max)) {
            Text(
                text = if (perHundred) stringResource(R.string.food_kcal_per_100, number(kcal), unit) else stringResource(R.string.energy_kcal, number(kcal)),
                style = type.meterValue,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (carbs != null || fat != null) {
                StackBar(
                    parts = listOf(
                        protein * Macros.PROTEIN_KCAL to PROTEIN_BLUE,
                        (carbs ?: 0.0) * Macros.CARB_KCAL to CARB_VIOLET,
                        (fat ?: 0.0) * Macros.FAT_KCAL to FAT_CORAL,
                    ),
                    height = spacing.hair,
                    modifier = Modifier.fillMaxWidth().padding(top = spacing.hair),
                )
            } else {
                Text(stringResource(R.string.food_protein_short, number(protein)), style = type.note, color = PROTEIN_BLUE)
            }
        }
        action?.let { LinkText(it, colors.special, onClick = onClick) }
        if (onEdit != null) {
            Icon(
                painterResource(R.drawable.ic_edit),
                contentDescription = editLabel ?: stringResource(R.string.recipe_edit, title),
                tint = colors.accent,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onEdit).padding(spacing.hair).size(iconSize),
            )
        }
        if (onAdd != null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(iconSize * ADD_DISC_SHARE)
                    .clip(CircleShape)
                    .background(colors.special.copy(alpha = ADD_DISC_ALPHA))
                    .clickable(onClick = onAdd),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.food_add_one, title), tint = colors.special, modifier = Modifier.size(iconSize))
            }
        }
    }
}

@Composable
private fun SearchField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, hint: String) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val colors = TaroTheme.colors
    val iconSize = with(LocalDensity.current) { type.title.lineHeight.toDp() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.loose))
            .background(colors.accentOpaque)
            .padding(horizontal = spacing.loose, vertical = spacing.gap),
    ) {
        Icon(painterResource(R.drawable.ic_search), contentDescription = null, tint = colors.accent, modifier = Modifier.size(iconSize))
        Box(modifier = Modifier.weight(1f)) {
            if (value.text.isEmpty()) Text(hint, style = type.title, color = colors.accent)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = type.title.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(colors.special),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.text.isNotEmpty()) {
            Icon(
                painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.food_search_clear),
                tint = colors.accent,
                modifier = Modifier.clip(CircleShape).clickable { onValueChange(TextFieldValue()) }.size(iconSize),
            )
        }
    }
}

@Composable
private fun FoodTray(added: List<FoodLog>, mealName: String, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val colors = TaroTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(spacing.gap, RoundedCornerShape(spacing.loose + spacing.tight))
            .clip(RoundedCornerShape(spacing.loose + spacing.tight))
            .background(colors.dialogSurface)
            .padding(horizontal = spacing.loose, vertical = spacing.gap),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (added.all { it.meal == added.first().meal }) stringResource(R.string.food_tray_title, mealName.lowercase()) else stringResource(R.string.food_tray_mixed),
                style = type.title,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                stringResource(R.string.food_tray_note, added.joinToString(", ") { it.name }, number(added.sumOf { it.kcal })),
                style = type.note,
                color = colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LinkText(stringResource(R.string.food_done), colors.special, onClick = onDone)
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        Text(label, style = type.title, color = if (selected) MaterialTheme.colorScheme.onSurface else TaroTheme.colors.accent, modifier = Modifier.weight(1f))
        if (selected) Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = TaroTheme.colors.special, modifier = Modifier.size(with(LocalDensity.current) { type.title.lineHeight.toDp() }))
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
    confirmLabel: String? = null,
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
        confirmText = confirmLabel ?: stringResource(R.string.action_save),
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
        val spacing = TaroTheme.spacing
        val colors = TaroTheme.colors
        val type = TaroTheme.text
        fun step(direction: Int) {
            val size = if (unit == FoodUnit.GRAM || unit == FoodUnit.ML) BASE_STEP else UNIT_STEP
            val next = (((entered ?: 0.0) / size).roundToInt() + direction).coerceAtLeast(1) * size
            text = TextFieldValue(Util.formatMeasure(next))
            portion = null
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.verticalScroll(rememberScrollState())) {
            header?.invoke()
            StepperField(
                value = text,
                onValueChange = {
                    text = it
                    portion = null
                },
                unit = unit.label,
                onMinus = { step(-1) },
                onPlus = { step(1) },
                decimal = true,
            )
            TextTabs(
                options = FoodUnit.entries.map { it to it.label },
                selected = unit,
                onSelect = {
                    unit = it
                    portion = null
                },
                color = colors.special,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            if (unit != baseUnit) {
                Text(
                    text = stringResource(
                        if (Units.approximate(unit, item)) R.string.food_unit_approx else R.string.food_unit_equals,
                        Units.format(amount ?: 0.0, item.unit),
                    ),
                    style = type.note,
                    color = colors.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (item.portions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.gap, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                    item.portions.forEach { option ->
                        LinkText(
                            "${option.label} · ${Units.format(option.grams, item.unit)}",
                            if (portion == option) colors.special else colors.accent,
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
            val protein = item.proteinFor(grams)
            val carbs = item.carbsFor(grams)
            val fat = item.fatFor(grams)
            BentoTile(color = colors.flame, tinted = true, modifier = Modifier.fillMaxWidth()) {
                BentoValue(number(item.kcalFor(grams)), colors.flame, unit = "kcal")
                if (carbs != null || fat != null) {
                    StackBar(
                        parts = listOf(
                            protein * Macros.PROTEIN_KCAL to PROTEIN_BLUE,
                            (carbs ?: 0.0) * Macros.CARB_KCAL to CARB_VIOLET,
                            (fat ?: 0.0) * Macros.FAT_KCAL to FAT_CORAL,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.tight),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Fact(stringResource(R.string.food_grams_value, number(protein)), stringResource(R.string.food_protein), PROTEIN_BLUE, Modifier.weight(1f), style = type.factCompact)
                    Fact(carbs?.let { stringResource(R.string.food_grams_value, number(it)) } ?: "-", stringResource(R.string.food_carbs), CARB_VIOLET, Modifier.weight(1f), style = type.factCompact)
                    Fact(fat?.let { stringResource(R.string.food_grams_value, number(it)) } ?: "-", stringResource(R.string.food_fat), FAT_CORAL, Modifier.weight(1f), style = type.factCompact)
                }
                BentoNote(stringResource(R.string.food_per_100g_values, number(item.kcal), number(item.protein), item.unit), Modifier.padding(top = spacing.tight))
            }
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
fun FoodDayToggle(date: String, incompleteDays: Set<String>, modifier: Modifier = Modifier) {
    val incomplete = date in incompleteDays
    LinkText(
        text = stringResource(if (incomplete) R.string.food_day_incomplete else R.string.food_day_complete),
        color = if (incomplete) MaterialTheme.colorScheme.error else TaroTheme.colors.goal,
        onClick = { AppPreferences.setFoodDayIncomplete(date, !incomplete) },
        icon = if (incomplete) R.drawable.ic_close else R.drawable.ic_check,
        modifier = modifier,
        info = infoOf(R.string.info_incomplete_title, R.string.info_incomplete),
    )
}
