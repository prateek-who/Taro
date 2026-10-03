package com.prateek.taro.ui

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.report.Period
import com.prateek.taro.report.PeriodReport
import com.prateek.taro.report.ReportData
import com.prateek.taro.report.ReportKind
import com.prateek.taro.report.WeightVerdict
import com.prateek.taro.ui.theme.ChivoHeavy
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Util
import java.time.format.TextStyle as DayStyle
import java.util.Locale
import kotlin.math.abs

class ReportStoryScreen(private val periodKey: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val period = Period.parse(periodKey) ?: ReportData.latestComplete(ReportKind.WEEK)
        ReportStory(
            period = period,
            onClose = { navigator.pop() },
            onDetails = { navigator.replace(ReportScreen(period.key)) },
        )
    }
}

private const val SLIDE_MS = 6_000
private const val PITCH_M = 105.0
private const val MARATHON_M = 42_195.0
private const val ROTI_KCAL = 119.0
private const val SAMOSA_KCAL = 203.0
private val DARK_INK = Color(0xFF101210)
private val SKY = Color(0xFF4FC3F7)
private val PINK = Color(0xFFFF6FAE)

private class Slide(val color: Color, val content: @Composable ColumnScope.(Color) -> Unit)

@Composable
private fun ReportStory(period: Period, onClose: () -> Unit, onDetails: () -> Unit) {
    val context = LocalContext.current
    val loaded = rememberInBackground(period) { ReportData.load(context, period) }
    val report = loaded?.value
    val slides = if (report != null) storySlides(report, onDetails) else emptyList()
    var index by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val shown = remember { intArrayOf(-1) }

    fun next() {
        if (index < slides.lastIndex) index++
    }

    fun previous() {
        if (index > 0) index--
    }

    LaunchedEffect(index, paused, slides.size) {
        if (slides.isEmpty()) return@LaunchedEffect
        if (shown[0] != index) {
            progress.snapTo(0f)
            shown[0] = index
        }
        if (paused || index == slides.lastIndex) return@LaunchedEffect
        progress.animateTo(1f, tween(((1f - progress.value) * SLIDE_MS).toInt(), easing = LinearEasing))
        next()
    }

    val background by animateColorAsState(slides.getOrNull(index)?.color ?: TaroTheme.colors.goal, tween(500), label = "story color")
    val ink = if (background.luminance() > 0.35f) DARK_INK else Color.White

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .pointerInput(slides.size) {
                detectTapGestures(
                    onPress = {
                        paused = true
                        tryAwaitRelease()
                        paused = false
                    },
                    onTap = { offset -> if (offset.x < size.width / 3f) previous() else next() },
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 10.dp)) {
                slides.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .drawBehind {
                                val radius = CornerRadius(size.height / 2)
                                drawRoundRect(ink.copy(alpha = 0.25f), cornerRadius = radius)
                                val fill = when {
                                    i < index -> 1f
                                    i == index -> progress.value
                                    else -> 0f
                                }
                                drawRoundRect(ink, size = Size(size.width * fill, size.height), cornerRadius = radius)
                            },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    color = ink,
                    fontSize = 13.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.report_dismiss),
                    tint = ink,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .padding(6.dp)
                        .size(24.dp),
                )
            }
            if (slides.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(if (loaded == null) R.string.story_loading else R.string.report_empty), color = ink, fontSize = 18.sp)
                }
            } else {
                AnimatedContent(
                    targetState = index,
                    transitionSpec = { (fadeIn(tween(350)) + scaleIn(tween(350), initialScale = 0.94f)) togetherWith fadeOut(tween(200)) },
                    label = "story slide",
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 48.dp),
                    ) {
                        slides[page].content(this, ink)
                    }
                }
            }
        }
    }
}

@Composable
private fun Appear(delay: Int, content: @Composable () -> Unit) {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(Unit) { shown.animateTo(1f, tween(500, delayMillis = delay, easing = FastOutSlowInEasing)) }
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = shown.value
            translationY = (1f - shown.value) * 40.dp.toPx()
        },
    ) { content() }
}

@Composable
private fun CountUp(target: Double, ink: Color, format: (Double) -> String, delay: Int = 300) {
    val value = remember { Animatable(0f) }
    LaunchedEffect(Unit) { value.animateTo(1f, tween(1_400, delayMillis = delay, easing = FastOutSlowInEasing)) }
    Text(
        text = format(target * value.value),
        style = TextStyle(fontFamily = ChivoHeavy, fontSize = 72.sp, lineHeight = 76.sp, fontWeight = FontWeight.Black, color = ink),
    )
}

@Composable
private fun Lead(text: String, ink: Color, delay: Int = 0) = Appear(delay) {
    Text(text, color = ink, fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, fontFamily = ChivoHeavy)
}

@Composable
private fun Note(text: String, ink: Color, delay: Int) = Appear(delay) {
    Text(text, color = ink.copy(alpha = 0.85f), fontSize = 18.sp, lineHeight = 24.sp, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Big(text: String, ink: Color, delay: Int = 300) = Appear(delay) {
    Text(text, style = TextStyle(fontFamily = ChivoHeavy, fontSize = 64.sp, lineHeight = 68.sp, fontWeight = FontWeight.Black, color = ink))
}

@Composable
private fun MiniBars(values: List<Int>, best: Int, ink: Color) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, delayMillis = 500, easing = FastOutSlowInEasing)) }
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(top = 24.dp),
    ) {
        values.forEachIndexed { i, value ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight((value.toFloat() / max * grow.value).coerceAtLeast(0.02f))
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(ink.copy(alpha = if (i == best) 1f else 0.3f)),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun storySlides(report: PeriodReport, onDetails: () -> Unit): List<Slide> {
    val colors = TaroTheme.colors
    val inputs = report.inputs
    val week = inputs.period.kind == ReportKind.WEEK
    val periodName = stringResource(if (week) R.string.story_week else R.string.story_month)
    val label = if (week) {
        stringResource(R.string.report_week_range, inputs.period.start.format(java.time.format.DateTimeFormatter.ofPattern("d MMM")), inputs.period.end.format(java.time.format.DateTimeFormatter.ofPattern("d MMM")))
    } else {
        "${inputs.period.start.month.getDisplayName(DayStyle.FULL, Locale.getDefault())} ${inputs.period.start.year}"
    }
    val km = Util.metersToDistance(report.distanceM).toDouble()
    val unit = Util.distanceUnit()
    val burnedAll = inputs.days.filter { it.complete }.sumOf { it.burned }

    val slides = mutableListOf<Slide>()
    slides += Slide(colors.goal) { ink ->
        Lead(stringResource(R.string.story_intro, periodName), ink)
        Big(label, ink, delay = 250)
        Note(stringResource(R.string.story_tap_hint), ink, delay = 900)
    }
    if (report.steps > 0) {
        slides += Slide(colors.flame) { ink ->
            Lead(stringResource(R.string.story_steps_lead), ink)
            CountUp(report.steps.toDouble(), ink, ::number)
            Note(stringResource(R.string.story_steps_note, Util.formatSteps(report.averageSteps)), ink, delay = 1_400)
        }
        slides += Slide(SKY) { ink ->
            Lead(stringResource(R.string.story_distance_lead), ink)
            CountUp(km, ink, { "%.1f %s".format(Locale.getDefault(), it, unit) })
            Note(
                if (report.distanceM >= MARATHON_M) {
                    stringResource(R.string.story_marathons, "%.1f".format(Locale.getDefault(), report.distanceM / MARATHON_M))
                } else {
                    stringResource(R.string.story_pitches, number(report.distanceM / PITCH_M))
                },
                ink,
                delay = 1_400,
            )
        }
    }
    report.bestDay?.takeIf { week && inputs.days.size > 1 }?.let { best ->
        slides += Slide(colors.special) { ink ->
            Lead(stringResource(R.string.story_best_lead), ink)
            Big(best.date.dayOfWeek.getDisplayName(DayStyle.FULL, Locale.getDefault()), ink)
            Note(stringResource(R.string.story_best_note, Util.formatSteps(best.steps)), ink, delay = 700)
            MiniBars(inputs.days.map { it.steps }, inputs.days.indexOf(best), ink)
        }
    }
    if (report.goalDaysPossible > 0) {
        slides += Slide(colors.goal) { ink ->
            Lead(stringResource(R.string.story_goal_lead), ink)
            Big(stringResource(R.string.story_goal_value, report.goalDays, report.goalDaysPossible), ink)
            Note(
                stringResource(
                    when {
                        report.goalDays == report.goalDaysPossible -> R.string.story_goal_all
                        report.goalDays * 2 >= report.goalDaysPossible -> R.string.story_goal_most
                        else -> R.string.story_goal_some
                    }
                ),
                ink,
                delay = 700,
            )
        }
    }
    if (report.activeKcal >= 1) {
        slides += Slide(colors.flame) { ink ->
            Lead(stringResource(R.string.story_active_lead), ink)
            CountUp(report.activeKcal, ink, { "${number(it)} kcal" })
            Note(stringResource(R.string.story_samosas, number(report.activeKcal / SAMOSA_KCAL)), ink, delay = 1_400)
        }
    }
    if (burnedAll > 0) {
        slides += Slide(PINK) { ink ->
            Lead(stringResource(R.string.story_burned_lead), ink)
            CountUp(burnedAll, ink, { "${number(it)} kcal" })
            Note(stringResource(R.string.story_rotis, number(burnedAll / ROTI_KCAL)), ink, delay = 1_400)
        }
    }
    report.expectedKg?.let { expected ->
        slides += Slide(colors.special) { ink ->
            Lead(stringResource(R.string.story_balance_lead), ink)
            CountUp(abs(report.balance), ink, { "${number(it)} kcal" })
            Note(stringResource(if (report.balance <= 0) R.string.story_under else R.string.story_over, "%.2f".format(Locale.getDefault(), abs(expected))), ink, delay = 1_400)
            report.actualKg?.let { actual ->
                val verdict = when (report.verdict) {
                    WeightVerdict.MATCHES -> R.string.story_scale_agrees
                    WeightVerdict.LOST_MORE -> R.string.story_scale_more
                    WeightVerdict.LOST_LESS -> R.string.story_scale_less
                    null -> R.string.story_scale_moved
                }
                Note(stringResource(verdict, Util.formatWeightChange(actual)), ink, delay = 2_200)
            }
        }
    }
    inputs.topFoods.firstOrNull()?.let { top ->
        slides += Slide(colors.goal) { ink ->
            Lead(stringResource(R.string.story_food_lead), ink)
            Big(top, ink)
            inputs.topFoods.drop(1).takeIf { it.isNotEmpty() }?.let { rest ->
                Note(stringResource(R.string.story_food_rest, rest.joinToString(stringResource(R.string.story_and))), ink, delay = 900)
            }
        }
    }
    if (inputs.nightMinutes.isNotEmpty()) {
        val average = inputs.nightMinutes.average().toLong()
        slides += Slide(colors.sleep) { ink ->
            Lead(stringResource(R.string.story_sleep_lead), ink)
            Big(stringResource(R.string.sleep_duration, (average / 60).toInt(), (average % 60).toInt()), ink)
            Note(stringResource(R.string.story_sleep_note), ink, delay = 700)
            if (inputs.napMinutes.isNotEmpty()) {
                Note(stringResource(R.string.story_naps, inputs.napMinutes.size), ink, delay = 1_200)
            }
        }
    }
    if (inputs.badges.isNotEmpty()) {
        slides += Slide(colors.special) { ink ->
            Lead(stringResource(R.string.story_badges_lead), ink)
            Big(stringResource(R.string.story_badges_count, inputs.badges.size), ink)
            Appear(900) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    inputs.badges.take(8).forEach {
                        Text(
                            text = stringResource(it),
                            color = ink,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(ink.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
    slides += Slide(colors.goal) { ink ->
        Lead(stringResource(R.string.story_summary, periodName), ink)
        Appear(300) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    SummaryTile(stringResource(R.string.report_steps_word), Util.formatSteps(report.steps), ink, Modifier.weight(1f))
                    SummaryTile(unit, "%.1f".format(Locale.getDefault(), km), ink, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    SummaryTile(stringResource(R.string.story_active_short), number(report.activeKcal), ink, Modifier.weight(1f))
                    SummaryTile(stringResource(R.string.report_goal_days), "${report.goalDays}/${report.goalDaysPossible}", ink, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Appear(700) {
            Text(
                text = stringResource(R.string.story_details),
                color = if (ink == DARK_INK) Color.White else DARK_INK,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(ink)
                    .clickable(onClick = onDetails)
                    .padding(vertical = 16.dp),
            )
        }
    }
    return slides
}

@Composable
private fun SummaryTile(label: String, value: String, ink: Color, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(ink.copy(alpha = 0.12f))
            .padding(16.dp),
    ) {
        Text(label.uppercase(), color = ink.copy(alpha = 0.8f), fontSize = 11.sp, letterSpacing = 1.5.sp)
        Text(value, color = ink, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = ChivoHeavy, modifier = Modifier.padding(top = 4.dp))
    }
}
