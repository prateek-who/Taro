package com.prateek.taro.ui

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Image
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import com.prateek.taro.R
import com.prateek.taro.energy.Sex
import com.prateek.taro.ui.components.NumberField
import com.prateek.taro.ui.components.PillSelector
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.StatPill
import com.prateek.taro.ui.components.TintChip
import com.prateek.taro.ui.components.ToggleGroup
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.AppPreferences.PreferenceKeys
import com.prateek.taro.util.Util
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private fun TextFieldValue.intIn(range: IntRange) = text.toIntOrNull()?.takeIf { it in range }

private fun TextFieldValue.measureIn(range: ClosedFloatingPointRange<Double>) = Util.parseMeasure(text)?.takeIf { it in range }

private fun inchesToCm(inches: Int) = (inches * 2.54).roundToInt()

private const val PAGES = 3

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingDialog(onDone: () -> Unit, onRestore: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    var imperial by remember { mutableStateOf(AppPreferences.unitSystem == Util.UnitSystem.IMPERIAL) }
    var heightCm by remember { mutableStateOf(TextFieldValue()) }
    var heightFt by remember { mutableStateOf(TextFieldValue()) }
    var heightIn by remember { mutableStateOf(TextFieldValue()) }
    var weight by remember { mutableStateOf(TextFieldValue()) }
    var leg by remember { mutableStateOf(TextFieldValue()) }
    var ageText by remember { mutableStateOf(TextFieldValue()) }
    var sex by remember { mutableStateOf<Sex?>(null) }

    val height = if (imperial) {
        val totalInches = (heightFt.text.toIntOrNull() ?: 0) * 12 + (heightIn.text.toIntOrNull() ?: 0)
        totalInches.takeIf { it in 12..98 }?.let { inchesToCm(it).toDouble() }
    } else {
        heightCm.measureIn(1.0..250.0)
    }
    val weightKg = if (imperial) weight.measureIn(1.0..1100.0)?.let { (it / 2.20462).coerceIn(1.0, 500.0) } else weight.measureIn(1.0..500.0)
    val legCm = if (imperial) leg.intIn(12..60)?.let(::inchesToCm) else leg.intIn(30..150)
    val age = ageText.intIn(AGE_RANGE)
    val bodyValid = height != null && weightKg != null
    val moreValid = (leg.text.isBlank() || legCm != null) && (ageText.text.isBlank() || age != null)

    val unitEntries = stringArrayResource(R.array.unit_system_entries).toList()
    val unitValues = stringArrayResource(R.array.unit_system_values).toList()

    fun finish(save: Boolean) {
        runBlocking {
            AppPreferences.dataStore.edit { prefs ->
                if (save && height != null && weightKg != null) {
                    prefs[PreferenceKeys.UNIT_SYSTEM] = if (imperial) "imperial" else "metric"
                    prefs[PreferenceKeys.HEIGHT] = height.toString()
                    prefs[PreferenceKeys.WEIGHT] = weightKg.toString()
                    legCm?.let { prefs[PreferenceKeys.LEG_LENGTH] = it.toString() }
                    age?.let { prefs[PreferenceKeys.BIRTH_YEAR] = Calendar.getInstance().get(Calendar.YEAR) - it }
                    sex?.let { prefs[PreferenceKeys.SEX] = it.key }
                }
                prefs[PreferenceKeys.ONBOARDING_DONE] = true
            }
        }
        onDone()
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TaroTheme.colors.background)
                .systemBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(40.dp)) {
                ProgressDots(page = page, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = TaroTheme.colors.accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { finish(save = false) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally(TaroMotion.snappy()) { if (forward) it / 3 else -it / 3 } + fadeIn())
                        .togetherWith(slideOutHorizontally(TaroMotion.snappy()) { if (forward) -it / 3 else it / 3 } + fadeOut())
                },
                label = "onboarding page",
                modifier = Modifier.weight(1f),
            ) { current ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (current) {
                        0 -> WelcomePage()
                        1 -> {
                            PageHeader(R.string.onboarding_body_title, R.string.onboarding_body_hint)
                            Label(R.string.onboarding_units)
                            ToggleGroup(
                                options = unitValues.zip(unitEntries),
                                selected = if (imperial) "imperial" else "metric",
                                onSelect = { imperial = it == "imperial" },
                            )
                            Spacer(Modifier.height(20.dp))
                            if (imperial) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    NumberField(heightFt, { heightFt = it }, stringResource(R.string.pref_height), Modifier.weight(1f), suffix = "ft", large = true)
                                    NumberField(heightIn, { heightIn = it }, "", Modifier.weight(1f), suffix = "in", large = true)
                                }
                            } else {
                                NumberField(heightCm, { heightCm = it }, stringResource(R.string.pref_height), Modifier.fillMaxWidth(), decimal = true, suffix = "cm", large = true)
                            }
                            Spacer(Modifier.height(12.dp))
                            NumberField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = stringResource(R.string.pref_weight),
                                decimal = true,
                                suffix = if (imperial) "lbs" else "kg",
                                large = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        else -> {
                            PageHeader(R.string.onboarding_more_title, R.string.onboarding_more_hint)
                            NumberField(
                                value = ageText,
                                onValueChange = { ageText = it },
                                label = stringResource(R.string.age_optional),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(20.dp))
                            Label(R.string.onboarding_sex)
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                PillSelector(
                                    options = Sex.entries.map { it to sexLabel(it) },
                                    selected = sex,
                                    onSelect = { sex = it },
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            NumberField(
                                value = leg,
                                onValueChange = { leg = it },
                                label = stringResource(R.string.leg_length_optional),
                                suffix = if (imperial) "in" else "cm",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(R.string.leg_length_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = TaroTheme.colors.accent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp, top = 8.dp, bottom = 16.dp),
                            )
                            height?.let {
                                val stepCm = Util.estimateStepLength(it, legCm)
                                val label = if (imperial) "%.1f in".format(Locale.getDefault(), stepCm / 2.54f)
                                else "%.1f cm".format(Locale.getDefault(), stepCm)
                                TintChip(
                                    text = stringResource(R.string.onboarding_estimated_step, label),
                                    color = TaroTheme.colors.goal,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            when (page) {
                0 -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(
                        text = stringResource(R.string.onboarding_get_started),
                        onClick = { page = 1 },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SecondaryButton(
                        text = stringResource(R.string.onboarding_restore),
                        onClick = {
                            finish(save = false)
                            onRestore()
                        },
                        icon = R.drawable.ic_import,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SecondaryButton(
                        text = stringResource(R.string.onboarding_back),
                        onClick = { page-- },
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = stringResource(if (page == PAGES - 1) R.string.onboarding_save else R.string.onboarding_next),
                        enabled = if (page == 1) bodyValid else bodyValid && moreValid,
                        onClick = { if (page == PAGES - 1) finish(save = true) else page++ },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.WelcomePage() {
    Spacer(Modifier.weight(1f))
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, TaroMotion.gentle()) }
    Image(
        painter = painterResource(R.drawable.taro_knot),
        contentDescription = null,
        modifier = Modifier
            .size(168.dp)
            .graphicsLayer {
                val t = entrance.value
                alpha = t.coerceIn(0f, 1f)
                scaleX = 0.7f + 0.3f * t
                scaleY = scaleX
                rotationZ = (1f - t) * -45f
            },
    )
    Image(
        painter = painterResource(R.drawable.taro_wordmark),
        contentDescription = stringResource(R.string.brand_name),
        modifier = Modifier
            .padding(top = 28.dp)
            .height(30.dp)
            .graphicsLayer { alpha = ((entrance.value - 0.4f) / 0.6f).coerceIn(0f, 1f) },
    )
    Text(
        text = stringResource(R.string.onboarding_message),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 24.dp, bottom = 20.dp),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        StatPill(R.drawable.ic_steps, stringResource(R.string.energy_steps_label), TaroTheme.colors.goal, lit = false)
        StatPill(R.drawable.ic_calories, stringResource(R.string.tab_energy), TaroTheme.colors.flame, lit = false)
        StatPill(R.drawable.ic_sleep, stringResource(R.string.tab_sleep), TaroTheme.colors.sleep, lit = false)
    }
    Text(
        text = stringResource(R.string.onboarding_private),
        style = MaterialTheme.typography.bodySmall,
        color = TaroTheme.colors.accent,
        modifier = Modifier.padding(top = 20.dp),
    )
    Spacer(Modifier.weight(1f))
}

@Composable
private fun PageHeader(title: Int, hint: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
    )
    Text(
        text = stringResource(hint),
        style = MaterialTheme.typography.bodyMedium,
        color = TaroTheme.colors.accent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 28.dp),
    )
}

@Composable
private fun Label(text: Int) {
    Text(
        text = stringResource(text).uppercase(),
        fontSize = 12.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.SemiBold,
        color = TaroTheme.colors.accent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, bottom = 10.dp),
    )
}

@Composable
private fun ProgressDots(page: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        repeat(PAGES) { index ->
            val width by animateDpAsState(if (index == page) 24.dp else 8.dp, TaroMotion.snappy(), label = "dot")
            Box(
                modifier = Modifier
                    .width(width)
                    .size(height = 8.dp, width = width)
                    .clip(CircleShape)
                    .background(if (index <= page) MaterialTheme.colorScheme.onSurface else TaroTheme.colors.accentOpaque),
            )
        }
    }
}
