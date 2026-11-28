package com.prateek.taro.ui

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import com.prateek.taro.food.Macros
import com.prateek.taro.ui.components.selectedAll
import com.prateek.taro.ui.components.StepperValue
import com.prateek.taro.ui.components.StepperField
import com.prateek.taro.ui.components.StackBar
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.food.Units
import com.prateek.taro.data.CustomFood
import com.prateek.taro.ui.components.TaroDialog
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.data.FoodLog
import com.prateek.taro.food.FoodCatalog
import com.prateek.taro.food.FoodItem
import com.prateek.taro.food.Ingredient
import com.prateek.taro.food.Recipes
import com.prateek.taro.ui.components.LocalToast
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.TaroTextField
import com.prateek.taro.ui.components.TitleField
import com.prateek.taro.ui.components.ToastKind
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Database
import com.prateek.taro.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeScreen(private val recipeId: Long?) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        RecipeContent(recipeId, onDone = { navigator.pop() })
    }
}

private sealed interface IngredientEdit {
    data object Pick : IngredientEdit
    data class Create(val name: String) : IngredientEdit
    data class Add(val item: FoodItem) : IngredientEdit
    data class Change(val index: Int, val item: FoodItem) : IngredientEdit
}

private const val COOKED_STEP = 10.0
private const val MAX_SERVINGS = 100
private const val MAX_NAME = 60
private const val PICKER_SHARE = 0.4f

@Composable
private fun RecipeContent(recipeId: Long?, onDone: () -> Unit) {
    val context = LocalContext.current
    val toast = LocalToast.current
    val existing = remember(recipeId) { recipeId?.let { Database.getInstance(context).customFood(it) } }
    var name by remember { mutableStateOf(TextFieldValue(existing?.name.orEmpty())) }
    var items by remember { mutableStateOf(Recipes.decode(existing?.ingredients)) }
    var cooked by remember { mutableStateOf(TextFieldValue(existing?.cookedGrams?.let(Util::formatMeasure).orEmpty())) }
    var servings by remember { mutableStateOf(TextFieldValue((existing?.servings ?: 1).toString())) }
    var editing by remember { mutableStateOf<IngredientEdit?>(null) }
    var pastEntries by remember { mutableStateOf<Pair<CustomFood, Int>?>(null) }
    var catalog by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    LaunchedEffect(Unit) { catalog = withContext(Dispatchers.IO) { FoodCatalog.load(context) } }
    val dataVersion = rememberDataVersion()
    val custom = remember(dataVersion) { Database.getInstance(context).customFoods().filter { it.id != recipeId && it.unit != Units.SERVING }.map(FoodCatalog::custom) }
    val all = remember(catalog, custom) { custom + catalog }

    val totals = Recipes.totals(items)
    val cookedValue = if (cooked.text.isBlank()) null else Util.parseMeasure(cooked.text)?.takeIf { it > 0 && it < 50_000 }
    val servingCount = servings.text.toIntOrNull()?.takeIf { it in 1..100 }
    val valid = name.text.isNotBlank() && items.isNotEmpty() && servingCount != null && (cooked.text.isBlank() || cookedValue != null)
    val servingLabel = stringResource(R.string.recipe_serving)

    fun resolve(ingredient: Ingredient): FoodItem? = all.firstOrNull { it.key == ingredient.key }
        ?: FoodItem(
            key = ingredient.key ?: "",
            name = ingredient.name,
            source = FoodCatalog.SOURCE_CUSTOM,
            kcal = ingredient.kcal * 100 / ingredient.grams,
            protein = ingredient.protein * 100 / ingredient.grams,
            carbs = ingredient.carbs?.times(100 / ingredient.grams),
            fat = ingredient.fat?.times(100 / ingredient.grams),
            portions = emptyList(),
            unit = ingredient.unit ?: Units.GRAMS,
        ).takeIf { ingredient.grams > 0 }

    val nameFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (existing == null) nameFocus.requestFocus() }
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        AppOverflowMenu(back = true)
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.gap),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = spacing.loose, end = spacing.loose, bottom = spacing.loose),
        ) {
            TitleField(
                name,
                { if (it.text.length <= MAX_NAME) name = it },
                stringResource(R.string.recipe_name_hint),
                colors.special,
                style = type.greeting,
                focusRequester = nameFocus,
                modifier = Modifier.padding(horizontal = spacing.tight),
            )
            SectionLabel(stringResource(R.string.recipe_ingredients))
            Panel {
                Column(modifier = Modifier.padding(horizontal = spacing.tight)) {
                    if (items.isEmpty()) {
                        BentoNote(stringResource(R.string.recipe_empty), Modifier.padding(vertical = spacing.tight))
                    }
                    items.forEachIndexed { index, ingredient ->
                        if (index > 0) RowDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                FoodRow(
                                    title = ingredient.name,
                                    subtitle = Units.format(ingredient.grams, ingredient.unit),
                                    kcal = ingredient.kcal,
                                    protein = ingredient.protein,
                                    carbs = ingredient.carbs,
                                    fat = ingredient.fat,
                                    onClick = { resolve(ingredient)?.let { editing = IngredientEdit.Change(index, it) } },
                                )
                            }
                            IconButton(onClick = { items = items.filterIndexed { i, _ -> i != index } }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_close),
                                    contentDescription = stringResource(R.string.food_delete, ingredient.name),
                                    tint = colors.accent,
                                )
                            }
                        }
                    }
                    LinkText(
                        text = stringResource(R.string.recipe_add_ingredient),
                        color = colors.goal,
                        icon = R.drawable.ic_add,
                        onClick = { editing = IngredientEdit.Pick },
                        modifier = Modifier.padding(vertical = spacing.tight),
                    )
                }
            }

            if (items.isNotEmpty()) {
                BentoTile(color = colors.special, label = stringResource(R.string.recipe_whole_dish), tinted = true, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(top = spacing.tight)) {
                        Fact(number(totals.kcal), stringResource(R.string.recipe_kcal), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), alignment = Alignment.CenterStart)
                        Fact(grams(totals.protein), stringResource(R.string.recipe_protein), PROTEIN_BLUE, Modifier.weight(1f), alignment = Alignment.CenterStart)
                        Fact(grams(totals.grams), stringResource(R.string.recipe_raw), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), alignment = Alignment.CenterStart)
                    }
                    if (totals.carbs != null || totals.fat != null) {
                        StackBar(
                            parts = listOf(
                                totals.protein * Macros.PROTEIN_KCAL to PROTEIN_BLUE,
                                (totals.carbs ?: 0.0) * Macros.CARB_KCAL to CARB_VIOLET,
                                (totals.fat ?: 0.0) * Macros.FAT_KCAL to FAT_CORAL,
                            ),
                            modifier = Modifier.padding(top = spacing.gap),
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.recipe_yield))
            BentoTile(color = colors.accent, label = stringResource(R.string.recipe_cooked_weight), modifier = Modifier.fillMaxWidth()) {
                StepperField(
                    value = cooked,
                    onValueChange = { cooked = it },
                    unit = "g",
                    onMinus = { cooked = Util.formatMeasure(((cookedValue ?: totals.grams) - COOKED_STEP).coerceAtLeast(COOKED_STEP)).selectedAll() },
                    onPlus = { cooked = Util.formatMeasure((cookedValue ?: totals.grams) + COOKED_STEP).selectedAll() },
                    decimal = true,
                    modifier = Modifier.padding(top = spacing.tight),
                )
                BentoNote(stringResource(R.string.recipe_cooked_hint), Modifier.padding(top = spacing.tight))
            }
            BentoTile(color = colors.accent, label = stringResource(R.string.recipe_servings), modifier = Modifier.fillMaxWidth()) {
                StepperValue(
                    text = (servingCount ?: 1).toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    onMinus = { servings = ((servingCount ?: 1) - 1).coerceAtLeast(1).toString().selectedAll() },
                    onPlus = { servings = ((servingCount ?: 1) + 1).coerceAtMost(MAX_SERVINGS).toString().selectedAll() },
                    minusEnabled = (servingCount ?: 1) > 1,
                    plusEnabled = (servingCount ?: 1) < MAX_SERVINGS,
                    modifier = Modifier.padding(top = spacing.tight),
                )
            }

            if (items.isNotEmpty() && servingCount != null) {
                val weight = cookedValue ?: totals.grams
                BentoTile(color = colors.special, label = stringResource(R.string.recipe_one_serving), tinted = true, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(top = spacing.tight)) {
                        Fact(grams(weight / servingCount), stringResource(R.string.recipe_portion), colors.special, Modifier.weight(1f), alignment = Alignment.CenterStart)
                        Fact(number(totals.kcal / servingCount), stringResource(R.string.recipe_kcal), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f), alignment = Alignment.CenterStart)
                        Fact(grams(totals.protein / servingCount), stringResource(R.string.recipe_protein), PROTEIN_BLUE, Modifier.weight(1f), alignment = Alignment.CenterStart)
                    }
                }
            }

            PrimaryButton(
                text = stringResource(R.string.recipe_save),
                enabled = valid,
                tint = colors.special,
                onClick = {
                    val recipe = Recipes.build(
                        name = name.text,
                        items = items,
                        cookedGrams = cookedValue,
                        servings = servingCount ?: 1,
                        servingLabel = servingLabel,
                        id = existing?.id ?: 0,
                        createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                    )
                    val database = Database.getInstance(context)
                    database.saveCustomFood(recipe)
                    val count = if (existing != null) database.pastEntryCount(recipe) else 0
                    if (count > 0) {
                        pastEntries = recipe to count
                    } else {
                        toast.show(context.getString(R.string.recipe_saved, name.text.trim()), ToastKind.SUCCESS)
                        onDone()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.tight),
            )
            if (existing != null) {
                LinkText(
                    text = stringResource(R.string.recipe_delete),
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        Database.getInstance(context).deleteCustomFood(existing.id)
                        toast.show(context.getString(R.string.recipe_deleted, existing.name), ToastKind.INFO)
                        onDone()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }


    fun ingredientFrom(log: FoodLog) = Ingredient(log.foodKey, log.name, log.grams ?: 0.0, log.kcal, log.protein, log.carbs, log.fat, log.unit)

    pastEntries?.let { (recipe, count) -> PastEntriesDialog(recipe, count, onDone = onDone) }

    when (val edit = editing) {
        IngredientEdit.Pick -> IngredientPickerDialog(
            foods = all,
            onPick = { editing = IngredientEdit.Add(it) },
            onCreate = { editing = IngredientEdit.Create(it) },
            onDismiss = { editing = null },
        )
        is IngredientEdit.Create -> CreateFoodDialog(
            initialName = edit.name,
            onSave = { food ->
                val id = Database.getInstance(context).saveCustomFood(food)
                editing = IngredientEdit.Add(FoodCatalog.custom(food.copy(id = id)))
            },
            onDismiss = { editing = null },
        )
        is IngredientEdit.Add -> AmountDialog(
            item = edit.item,
            initialGrams = null,
            onSave = {
                items = items + ingredientFrom(it)
                editing = null
            },
            onDismiss = { editing = null },
        )
        is IngredientEdit.Change -> AmountDialog(
            item = edit.item,
            initialGrams = items.getOrNull(edit.index)?.grams,
            initialValues = items.getOrNull(edit.index)?.let { Nutrition(it.kcal, it.protein, it.carbs, it.fat) },
            onSave = { log ->
                items = items.mapIndexed { i, old -> if (i == edit.index) ingredientFrom(log).copy(key = old.key) else old }
                editing = null
            },
            onDismiss = { editing = null },
        )
        null -> Unit
    }
}

@Composable
private fun IngredientPickerDialog(foods: List<FoodItem>, onPick: (FoodItem) -> Unit, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf(TextFieldValue()) }
    val results = rememberFoodSearch(foods, query.text, limit = 30)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    TaroDialog(title = stringResource(R.string.recipe_add_ingredient), onDismiss = onDismiss) {
        Column {
            TaroTextField(
                value = query,
                onValueChange = { query = it },
                label = stringResource(R.string.food_search),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus),
            )
            Column(
                modifier = Modifier
                    .padding(top = TaroTheme.spacing.tight)
                    .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * PICKER_SHARE)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (query.text.isNotBlank() && results.isEmpty()) {
                    BentoNote(stringResource(R.string.recipe_no_match), Modifier.padding(TaroTheme.spacing.tight))
                }
                results.forEach { item ->
                    FoodRow(
                        title = item.name,
                        subtitle = "",
                        kcal = item.kcal,
                        protein = item.protein,
                        perHundred = true,
                        unit = item.unit,
                        onClick = { onPick(item) },
                    )
                }
            }
            LinkText(
                text = stringResource(R.string.recipe_create_from_label),
                color = TaroTheme.colors.goal,
                icon = R.drawable.ic_add,
                onClick = { onCreate(query.text.trim()) },
                modifier = Modifier.padding(top = TaroTheme.spacing.tight),
            )
        }
    }
}
