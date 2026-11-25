package com.prateek.taro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateFloatAsState
import com.prateek.taro.ui.components.selectedAll
import com.prateek.taro.ui.components.StepperField
import com.prateek.taro.ui.components.MeterBar
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.IconTileGrid
import com.prateek.taro.ui.components.IconTile
import com.prateek.taro.ui.components.Fact
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.TextTabs
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.datastore.preferences.core.edit
import com.prateek.taro.R
import com.prateek.taro.energy.Sex
import com.prateek.taro.ui.components.PrimaryButton
import com.prateek.taro.ui.components.SecondaryButton
import com.prateek.taro.ui.components.StatPill
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
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
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
                .padding(horizontal = TaroTheme.spacing.wide, vertical = TaroTheme.spacing.loose),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(TaroTheme.spacing.wide + TaroTheme.spacing.loose)) {
                if (page == 0) ProgressDots(page = page, modifier = Modifier.weight(1f)) else PageProgress(page, Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = TaroTheme.colors.accent,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { finish(save = false) }
                        .padding(horizontal = TaroTheme.spacing.gap, vertical = TaroTheme.spacing.tight),
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
                            TextTabs(
                                options = unitValues.zip(unitEntries),
                                selected = if (imperial) "imperial" else "metric",
                                onSelect = { imperial = it == "imperial" },
                                color = colors.goal,
                            )
                            if (imperial) {
                                MeasureTile(R.string.pref_height, colors.goal) {
                                    MeasureStepper(heightFt, { heightFt = it }, "ft", 1.0, 5.0, 1.0..8.0)
                                    MeasureStepper(heightIn, { heightIn = it }, "in", 1.0, 7.0, 0.0..11.0)
                                }
                            } else {
                                MeasureTile(R.string.pref_height, colors.goal) {
                                    MeasureStepper(heightCm, { heightCm = it }, "cm", 1.0, 170.0, 100.0..250.0, decimal = true)
                                }
                            }
                            MeasureTile(R.string.pref_weight, colors.goal) {
                                if (imperial) {
                                    MeasureStepper(weight, { weight = it }, "lbs", 1.0, 155.0, 50.0..1100.0, decimal = true)
                                } else {
                                    MeasureStepper(weight, { weight = it }, "kg", 0.5, 70.0, 20.0..500.0, decimal = true)
                                }
                            }
                        }
                        else -> {
                            PageHeader(R.string.onboarding_more_title, R.string.onboarding_more_hint)
                            MeasureTile(R.string.pref_age, colors.flame) {
                                MeasureStepper(ageText, { ageText = it }, "", 1.0, 30.0, AGE_RANGE.first.toDouble()..AGE_RANGE.last.toDouble())
                            }
                            IconTileGrid(Sex.entries, columns = Sex.entries.size, modifier = Modifier.padding(top = spacing.gap)) { option, tileModifier ->
                                IconTile(
                                    icon = when (option) {
                                        Sex.FEMALE -> R.drawable.ic_female
                                        Sex.MALE -> R.drawable.ic_male
                                        Sex.UNSPECIFIED -> R.drawable.ic_sex
                                    },
                                    label = sexLabel(option),
                                    color = colors.flame,
                                    selected = sex == option,
                                    onClick = { sex = if (sex == option) null else option },
                                    modifier = tileModifier,
                                )
                            }
                            MeasureTile(R.string.pref_leg_length, colors.goal) {
                                MeasureStepper(leg, { leg = it }, if (imperial) "in" else "cm", 1.0, if (imperial) 31.0 else 80.0, if (imperial) 12.0..60.0 else 30.0..150.0)
                                Text(
                                    text = stringResource(R.string.leg_length_hint),
                                    style = type.note,
                                    color = colors.accent,
                                )
                            }
                            height?.let {
                                val stepCm = Util.estimateStepLength(it, legCm)
                                val stepText = if (imperial) "%.1f in".format(Locale.getDefault(), stepCm / 2.54f) else "%.0f cm".format(Locale.getDefault(), stepCm)
                                val per = if (imperial) 160_934f else 100_000f
                                BentoTile(color = colors.goal, tinted = true, modifier = Modifier.fillMaxWidth().padding(top = spacing.gap)) {
                                    Row {
                                        Fact(stepText, stringResource(R.string.onboarding_your_step), colors.goal, Modifier.weight(1f), alignment = Alignment.CenterStart)
                                        Fact(
                                            Util.formatSteps((per / stepCm).roundToInt()),
                                            stringResource(if (imperial) R.string.onboarding_steps_mile else R.string.onboarding_steps_km),
                                            MaterialTheme.colorScheme.onSurface,
                                            Modifier.weight(1f),
                                            alignment = Alignment.CenterStart,
                                        )
                                    }
                                }
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
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = spacing.gap)) {
                    PrimaryButton(
                        text = stringResource(if (page == PAGES - 1) R.string.onboarding_save else R.string.onboarding_next),
                        enabled = if (page == 1) bodyValid else bodyValid && moreValid,
                        onClick = { if (page == PAGES - 1) finish(save = true) else page++ },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LinkText(stringResource(R.string.onboarding_back), colors.accent, onClick = { page-- }, modifier = Modifier.padding(top = spacing.tight))
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
    val spacing = TaroTheme.spacing
    Text(
        text = stringResource(title),
        style = TaroTheme.text.greeting,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.loose),
    )
    Text(
        text = stringResource(hint),
        style = TaroTheme.text.note,
        color = TaroTheme.colors.accent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.tight, bottom = spacing.loose),
    )
}

@Composable
private fun MeasureTile(label: Int, color: Color, content: @Composable ColumnScope.() -> Unit) {
    BentoTile(
        color = color,
        label = stringResource(label),
        tinted = true,
        modifier = Modifier.fillMaxWidth().padding(top = TaroTheme.spacing.gap),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TaroTheme.spacing.tight), modifier = Modifier.padding(top = TaroTheme.spacing.tight), content = content)
    }
}

@Composable
private fun MeasureStepper(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    unit: String,
    step: Double,
    start: Double,
    range: ClosedFloatingPointRange<Double>,
    decimal: Boolean = false,
) {
    fun bump(delta: Double) {
        val next = Util.parseMeasure(value.text)?.let { (it + delta).coerceIn(range) } ?: start
        onChange(Util.formatMeasure(next).selectedAll())
    }
    StepperField(
        value = value,
        onValueChange = onChange,
        unit = unit,
        onMinus = { bump(-step) },
        onPlus = { bump(step) },
        decimal = decimal,
    )
}

@Composable
private fun PageProgress(page: Int, modifier: Modifier = Modifier) {
    val spacing = TaroTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight), modifier = modifier) {
        repeat(PAGES - 1) { index ->
            val filled by animateFloatAsState(if (index < page) 1f else 0f, TaroMotion.snappy(), label = "progress")
            MeterBar(filled, TaroTheme.colors.goal, Modifier.weight(1f), height = spacing.hair * 2)
        }
        Spacer(Modifier.weight(1f))
    }
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
