package com.prateek.taro.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.prateek.taro.ui.components.NumberField
import com.prateek.taro.ui.components.Panel
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.ScrollingColumn
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.TaroScaffold
import com.prateek.taro.ui.components.TaroTextField
import com.prateek.taro.ui.components.TintChip
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
    val custom = remember(dataVersion) { Database.getInstance(context).customFoods().filter { it.id != recipeId }.map(FoodCatalog::custom) }
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

    TaroScaffold(
        title = stringResource(if (existing == null) R.string.recipe_create else R.string.recipe_edit_title),
        onBack = onDone,
    ) { padding ->
        ScrollingColumn(padding, modifier = Modifier.padding(horizontal = 16.dp)) {
            TaroTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.recipe_name),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            SectionLabel(stringResource(R.string.recipe_ingredients))
            Panel {
                if (items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.recipe_empty),
                        fontSize = 13.sp,
                        color = TaroTheme.colors.accent,
                        modifier = Modifier.padding(8.dp),
                    )
                }
                items.forEachIndexed { index, ingredient ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            FoodRow(
                                title = ingredient.name,
                                subtitle = Units.format(ingredient.grams, ingredient.unit),
                                kcal = ingredient.kcal,
                                protein = ingredient.protein,
                                onClick = { resolve(ingredient)?.let { editing = IngredientEdit.Change(index, it) } },
                            )
                        }
                        IconButton(onClick = { items = items.filterIndexed { i, _ -> i != index } }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_delete),
                                contentDescription = stringResource(R.string.food_delete, ingredient.name),
                                tint = TaroTheme.colors.accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                PrimaryButton(
                    text = stringResource(R.string.recipe_add_ingredient),
                    onClick = { editing = IngredientEdit.Pick },
                    icon = R.drawable.ic_add,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                )
            }

            if (items.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    TintChip(stringResource(R.string.energy_kcal, number(totals.kcal)), TaroTheme.colors.flame)
                    TintChip(stringResource(R.string.food_protein_short, number(totals.protein)), PROTEIN_BLUE)
                    TintChip(stringResource(R.string.recipe_raw_weight, grams(totals.grams)), TaroTheme.colors.accent)
                }
            }

            SectionLabel(stringResource(R.string.recipe_yield))
            Panel {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(8.dp)) {
                    NumberField(
                        value = cooked,
                        onValueChange = { cooked = it },
                        label = stringResource(R.string.recipe_cooked_weight),
                        decimal = true,
                        suffix = "g",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(stringResource(R.string.recipe_cooked_hint), fontSize = 12.sp, color = TaroTheme.colors.accent)
                    NumberField(
                        value = servings,
                        onValueChange = { servings = it },
                        label = stringResource(R.string.recipe_servings),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (items.isNotEmpty() && servingCount != null) {
                        val weight = cookedValue ?: totals.grams
                        Text(
                            text = stringResource(
                                R.string.recipe_per_serving,
                                grams(weight / servingCount),
                                number(totals.kcal / servingCount),
                                number(totals.protein / servingCount),
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            PrimaryButton(
                text = stringResource(R.string.recipe_save),
                enabled = valid,
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
                    .padding(top = 20.dp),
            )
            if (existing != null) {
                SecondaryButton(
                    text = stringResource(R.string.recipe_delete),
                    onClick = {
                        Database.getInstance(context).deleteCustomFood(existing.id)
                        toast.show(context.getString(R.string.recipe_deleted, existing.name), ToastKind.INFO)
                        onDone()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 24.dp),
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
                    .padding(top = 8.dp)
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (query.text.isNotBlank() && results.isEmpty()) {
                    Text(
                        text = stringResource(R.string.recipe_no_match),
                        fontSize = 13.sp,
                        color = TaroTheme.colors.accent,
                        modifier = Modifier.padding(8.dp),
                    )
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
            SecondaryButton(
                text = stringResource(R.string.recipe_create_from_label),
                onClick = { onCreate(query.text.trim()) },
                icon = R.drawable.ic_add,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        }
    }
}
