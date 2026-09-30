package com.prateek.taro.ui

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
    meals: List<MealSlot>,
    logs: List<FoodLog>,
    onAdd: (String) -> Unit,
    onEdit: (FoodLog) -> Unit,
    onDelete: (FoodLog) -> Unit,
) {
    var managing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val timeFormat = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val known = meals.map { it.key }.toSet()
    val orphans = logs.filter { it.meal !in known }
    val groups = meals + listOfNotNull(orphans.takeIf { it.isNotEmpty() }?.let { MealSlot("", stringResource(R.string.meal_other)) })
    groups.forEach { meal ->
        val entries = if (meal.key.isEmpty()) orphans else logs.filter { it.meal == meal.key }
        Panel(modifier = Modifier.padding(bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = meal.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .then(if (meal.key.isEmpty()) Modifier else Modifier.clickable { managing = true })
                        .padding(vertical = 6.dp),
                )
                if (entries.isNotEmpty()) {
                    val totals = entries.totals()
                    Text(
                        text = stringResource(R.string.food_meal_total, number(totals.kcal), number(totals.protein)),
                        fontSize = 13.sp,
                        color = TaroTheme.colors.accent,
                    )
                }
                if (meal.key.isNotEmpty()) {
                    IconButton(onClick = { onAdd(meal.key) }) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.food_add_to, meal.name))
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
                        Text(
                            text = listOfNotNull(timeFormat.format(Date(log.loggedAt)), log.amount, log.grams?.let(::grams)).distinct().joinToString(" · "),
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
    SecondaryButton(
        text = stringResource(R.string.meal_edit_all),
        onClick = { managing = true },
        modifier = Modifier.fillMaxWidth(),
    )

    if (managing) {
        ManageMealsDialog(
            meals = meals,
            onSave = {
                AppPreferences.saveMeals(it)
                managing = false
            },
            onDismiss = { managing = false },
        )
    }
}

@Composable
private fun ManageMealsDialog(meals: List<MealSlot>, onSave: (List<MealSlot>) -> Unit, onDismiss: () -> Unit) {
    var list by remember { mutableStateOf(meals) }
    var naming by remember { mutableStateOf<MealSlot?>(null) }
    var dragKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeight = 52.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }

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
                                .pointerInput(meal.key) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragKey = meal.key
                                            dragOffset = 0f
                                        },
                                        onDragEnd = {
                                            dragKey = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            dragKey = null
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val index = list.indexOfFirst { it.key == meal.key }
                                            if (dragOffset > rowPx / 2 && index < list.lastIndex) {
                                                move(index, index + 1)
                                                dragOffset -= rowPx
                                            } else if (dragOffset < -rowPx / 2 && index > 0) {
                                                move(index, index - 1)
                                                dragOffset += rowPx
                                            }
                                        },
                                    )
                                }
                                .padding(12.dp),
                        )
                        Text(
                            text = meal.name,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { naming = meal }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                        )
                        if (list.size > 1) {
                            IconButton(onClick = { list = list.filterNot { it.key == meal.key } }) {
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
            onSave = { name ->
                list = if (list.any { it.key == meal.key }) {
                    list.map { if (it.key == meal.key) it.copy(name = name) else it }
                } else {
                    list + meal.copy(name = name)
                }
                naming = null
            },
            onDismiss = { naming = null },
        )
    }
}

@Composable
private fun MealDialog(title: String, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial.selectedAll()) }
    val clean = Meals.cleanName(name.text)
    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onSave(clean) },
        confirmEnabled = clean.isNotEmpty(),
    ) {
        TaroTextField(
            value = name,
            onValueChange = { if (it.text.length <= Meals.MAX_NAME) name = it },
            label = stringResource(R.string.meal_name),
            modifier = Modifier.fillMaxWidth(),
        )
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
}

@Composable
private fun AddFoodContent(initialDate: LocalDate, initialMeal: String, onDone: () -> Unit) {
    val context = LocalContext.current
    val navigator = LocalNavigator.currentOrThrow
    val toast = LocalToast.current
    var date by remember { mutableStateOf(initialDate) }
    var meal by remember { mutableStateOf(initialMeal) }
    var minute by remember { mutableIntStateOf(DayClock.minuteOfDay(System.currentTimeMillis())) }
    var query by remember { mutableStateOf(TextFieldValue()) }
    var dialog by remember { mutableStateOf<FoodDialog?>(null) }
    val dataVersion = rememberDataVersion()
    val meals by AppPreferences.mealsFlow(context).collectAsStateWithLifecycle(AppPreferences.meals(context))
    var catalog by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    LaunchedEffect(Unit) { catalog = withContext(Dispatchers.IO) { FoodCatalog.load(context) } }
    val custom = remember(dataVersion) { Database.getInstance(context).customFoods().map(FoodCatalog::custom) }
    val recent = remember(dataVersion) {
        Database.getInstance(context).recentFood(60).distinctBy { it.name.lowercase() to it.grams }.take(12)
    }
    val results = remember(query.text, catalog, custom) { FoodCatalog.search(custom + catalog, query.text) }

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
                options = meals.map { it.key to it.name },
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
                onChange = { minute = it },
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
                if (recent.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.food_recent))
                    Panel {
                        recent.forEach { log ->
                            FoodRow(
                                title = log.name,
                                subtitle = listOfNotNull(log.amount, log.grams?.let(::grams)).distinct().joinToString(" · "),
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
                                subtitle = stringResource(if (item.recipeId != null) R.string.recipe_per_100g else R.string.food_per_100g),
                                kcal = item.kcal,
                                protein = item.protein,
                                perHundred = true,
                                onClick = { dialog = FoodDialog.Amount(item) },
                                onEdit = item.recipeId?.let { id -> { navigator.push(RecipeScreen(id)) } },
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
        null -> Unit
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
    onEdit: (() -> Unit)? = null,
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
                text = stringResource(if (perHundred) R.string.food_kcal_per_100 else R.string.energy_kcal, number(kcal)),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(stringResource(R.string.food_protein_short, number(protein)), fontSize = 12.sp, color = PROTEIN_BLUE)
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.recipe_edit, title), tint = TaroTheme.colors.accent, modifier = Modifier.size(20.dp))
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
    var text by remember { mutableStateOf(TextFieldValue(Util.formatMeasure(start))) }
    var portion by remember { mutableStateOf<Portion?>(item.portions.firstOrNull { it.grams == start }) }
    val amount = Util.parseMeasure(text.text)?.takeIf { it > 0 && it < 5_000 }

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
                    amount = portion?.takeIf { it.grams == grams }?.label,
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
                suffix = "g",
                large = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (item.portions.isNotEmpty()) {
                FlowRow {
                    item.portions.forEach { option ->
                        RangeChip(
                            label = "${option.label} (${grams(option.grams)})",
                            selected = portion == option,
                            onClick = {
                                portion = option
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
                text = stringResource(R.string.food_per_100g_values, number(item.kcal), number(item.protein)),
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
internal fun CreateFoodDialog(onSave: (CustomFood) -> Unit, onDismiss: () -> Unit, initialName: String = "") {
    var name by remember { mutableStateOf(TextFieldValue(initialName)) }
    var kcal by remember { mutableStateOf(TextFieldValue()) }
    var protein by remember { mutableStateOf(TextFieldValue()) }
    var carbs by remember { mutableStateOf(TextFieldValue()) }
    var fat by remember { mutableStateOf(TextFieldValue()) }
    var serving by remember { mutableStateOf(TextFieldValue()) }
    fun optional(field: TextFieldValue) = if (field.text.isBlank()) null else Util.parseMeasure(field.text)?.takeIf { it in 0.0..100.0 }
    val carbsValue = optional(carbs)
    val fatValue = optional(fat)
    val macrosValid = (carbs.text.isBlank() || carbsValue != null) && (fat.text.isBlank() || fatValue != null)
    val kcalValue = Util.parseMeasure(kcal.text)?.takeIf { it in 0.0..900.0 }
    val proteinValue = if (protein.text.isBlank()) 0.0 else Util.parseMeasure(protein.text)?.takeIf { it in 0.0..100.0 }
    val servingValue = if (serving.text.isBlank()) null else Util.parseMeasure(serving.text)?.takeIf { it > 0 }

    TaroDialog(
        title = stringResource(R.string.food_create),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        confirmEnabled = name.text.isNotBlank() && kcalValue != null && proteinValue != null && macrosValid && (serving.text.isBlank() || servingValue != null),
        onConfirm = {
            onSave(
                CustomFood(
                    name = name.text.trim(),
                    kcal = kcalValue ?: 0.0,
                    protein = proteinValue ?: 0.0,
                    carbs = carbsValue,
                    fat = fatValue,
                    servingLabel = servingValue?.let { "1 serving" },
                    servingGrams = servingValue,
                    createdAt = System.currentTimeMillis(),
                )
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.food_create_hint), fontSize = 13.sp, color = TaroTheme.colors.accent)
            TaroTextField(name, { name = it }, stringResource(R.string.food_name), Modifier.fillMaxWidth())
            NumberField(kcal, { kcal = it }, stringResource(R.string.food_kcal_100), Modifier.fillMaxWidth(), decimal = true, suffix = "kcal")
            NumberField(protein, { protein = it }, stringResource(R.string.food_protein_100), Modifier.fillMaxWidth(), decimal = true, suffix = "g")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NumberField(carbs, { carbs = it }, stringResource(R.string.food_carbs_100), Modifier.weight(1f), decimal = true, suffix = "g")
                NumberField(fat, { fat = it }, stringResource(R.string.food_fat_100), Modifier.weight(1f), decimal = true, suffix = "g")
            }
            NumberField(serving, { serving = it }, stringResource(R.string.food_serving_optional), Modifier.fillMaxWidth(), decimal = true, suffix = "g")
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
