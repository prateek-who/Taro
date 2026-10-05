package com.prateek.taro.ui

import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieAnimation
import kotlin.math.roundToLong
import java.text.NumberFormat
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import com.prateek.taro.report.DayInput
import kotlin.random.Random
import androidx.annotation.StringRes
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

private const val SLIDE_MS = 8_000
private const val MAX_ICONS = 10
private val HERO_SIZE = 140.dp
private const val WORLD_STEPS = 5_000
private const val WHO_WEEKLY_MIN = 150
private val DARK_INK = Color(0xFF101210)
private val SKY = Color(0xFF4FC3F7)
private val PINK = Color(0xFFFF6FAE)

private class Slide(
    val color: Color,
    val art: String,
    val scene: Scene = Scene.BLOBS,
    val content: @Composable ColumnScope.(Color) -> Unit,
)

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
                    Box(modifier = Modifier.fillMaxSize()) {
                        val slide = slides[page]
                        StoryScene(slide.scene, ink, seed = page + period.key.hashCode(), modifier = Modifier.fillMaxSize())
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 48.dp),
                        ) {
                            HeroArt(slides[page].art)
                            slides[page].content(this, ink)
                        }
                    }
                }
            }
        }
    }
}

private fun codepoints(emoji: String) = emoji.codePoints().toArray().filter { it != 0xFE0F }.joinToString("_") { Integer.toHexString(it) }

@Composable
private fun HeroArt(emoji: String) {
    val context = LocalContext.current
    val animated = remember { context.assets.list("noto").orEmpty().toSet() }
    val file = "${codepoints(emoji)}.json"
    val enabled = rememberAnimationsEnabled()
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
    val modifier = Modifier
        .padding(bottom = 12.dp)
        .size(HERO_SIZE)
        .graphicsLayer {
            scaleX = 0.6f + 0.4f * pop.value
            scaleY = 0.6f + 0.4f * pop.value
            alpha = pop.value
        }
    if (file in animated) {
        val composition by rememberLottieComposition(LottieCompositionSpec.Asset("noto/$file"))
        LottieAnimation(
            composition = composition,
            iterations = LottieConstants.IterateForever,
            isPlaying = enabled,
            modifier = modifier,
        )
    } else {
        Box(contentAlignment = Alignment.Center, modifier = modifier) { Text(emoji, fontSize = 88.sp) }
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

private fun grouped(value: Number): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toDouble().roundToLong())

private class Thing(@StringRes val text: Int, val size: Double, val emoji: String)

private val STEP_THINGS = listOf(
    Thing(R.string.cmp_eiffel, 1_665.0, "🗼"),
    Thing(R.string.cmp_burj, 2_909.0, "🏙️"),
    Thing(R.string.cmp_qutub, 379.0, "🕌"),
    Thing(R.string.cmp_liberty, 354.0, "🗽"),
)
private val DISTANCE_THINGS = listOf(
    Thing(R.string.cmp_pitch, 105.0, "⚽"),
    Thing(R.string.cmp_track, 400.0, "🏟️"),
    Thing(R.string.cmp_cricket, 20.12, "🏏"),
    Thing(R.string.cmp_marine, 3_600.0, "🌊"),
    Thing(R.string.cmp_sealink, 5_600.0, "🌉"),
    Thing(R.string.cmp_everest, 8_849.0, "🏔️"),
    Thing(R.string.cmp_marathon, 42_195.0, "🏃"),
)
private val SNACK_THINGS = listOf(
    Thing(R.string.cmp_samosa, 203.0, "🥟"),
    Thing(R.string.cmp_vadapav, 290.0, "🍔"),
    Thing(R.string.cmp_jamun, 152.0, "🍡"),
    Thing(R.string.cmp_banana, 105.0, "🍌"),
    Thing(R.string.cmp_chai, 68.0, "☕"),
    Thing(R.string.cmp_pizza, 285.0, "🍕"),
    Thing(R.string.cmp_icecream, 137.0, "🍦"),
    Thing(R.string.cmp_chocolate, 230.0, "🍫"),
)
private val MEAL_THINGS = listOf(
    Thing(R.string.cmp_roti, 119.0, "🫓"),
    Thing(R.string.cmp_biryani, 612.0, "🍛"),
    Thing(R.string.cmp_dosa, 134.0, "🥞"),
    Thing(R.string.cmp_rice, 195.0, "🍚"),
    Thing(R.string.cmp_mango, 120.0, "🥭"),
)
private val SLEEP_THINGS = listOf(
    Thing(R.string.cmp_movie, 2.5, "🎬"),
    Thing(R.string.cmp_t20, 3.5, "🏏"),
    Thing(R.string.cmp_flight, 2.2, "✈️"),
    Thing(R.string.cmp_episode, 0.75, "📺"),
)
private val WALK_THINGS = listOf(
    Thing(R.string.cmp_song, 3.5, "🎵"),
    Thing(R.string.cmp_ted, 18.0, "🎤"),
    Thing(R.string.cmp_sitcom, 22.0, "📺"),
    Thing(R.string.cmp_chai_brew, 5.0, "☕"),
)
private val WEIGHT_THINGS = listOf(
    Thing(R.string.cmp_ball, 0.16, "🏏"),
    Thing(R.string.cmp_phone, 0.2, "📱"),
    Thing(R.string.cmp_mango_kg, 0.3, "🥭"),
    Thing(R.string.cmp_butter, 0.5, "🧈"),
    Thing(R.string.cmp_banana_kg, 0.12, "🍌"),
)
private val SLEEP_FACTS = listOf(R.string.fact_koala, R.string.fact_giraffe, R.string.fact_sloth, R.string.fact_dolphin)

private fun pick(random: Random, pool: List<Thing>, value: Double): Pair<Thing, Double>? =
    pool.map { it to value / it.size }.filter { it.second >= 1.0 && it.second < 100_000 }.randomOrNull(random)

private fun amount(value: Double) = if (value < 10) "%.1f".format(Locale.getDefault(), value) else grouped(value)

@Composable
private fun Compare(match: Pair<Thing, Double>?, ink: Color, delay: Int) {
    match ?: return
    Note(stringResource(match.first.text, amount(match.second)), ink, delay)
    EmojiRow(match.first.emoji, match.second, ink, delay + 300)
}

@Composable
private fun EmojiRow(emoji: String, count: Double, ink: Color, delay: Int) {
    val shown = count.toInt().coerceIn(1, MAX_ICONS)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
        repeat(shown) { index ->
            Appear(delay + index * 70) { Text(emoji, fontSize = 26.sp, modifier = Modifier.padding(end = 2.dp)) }
        }
        if (count.toInt() > MAX_ICONS) {
            Appear(delay + MAX_ICONS * 70) {
                Text("×${grouped(count)}", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun Chip(text: String, ink: Color, delay: Int) = Appear(delay) {
    Text(
        text = text,
        color = ink,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        modifier = Modifier
            .padding(top = 14.dp)
            .clip(CircleShape)
            .background(ink.copy(alpha = 0.15f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayDots(days: List<DayInput>, ink: Color) {
    Appear(600) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 20.dp),
        ) {
            days.forEach { day ->
                val hit = day.goal > 0 && day.steps >= day.goal
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (days.size > 7) 26.dp else 36.dp)
                        .clip(CircleShape)
                        .background(if (hit) ink else ink.copy(alpha = 0.15f)),
                ) {
                    if (days.size <= 7) {
                        Text(
                            text = day.date.dayOfWeek.getDisplayName(DayStyle.NARROW, Locale.getDefault()),
                            color = if (hit) Color.White.takeIf { ink == DARK_INK } ?: DARK_INK else ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourChart(hours: List<Int>, peak: Int, ink: Color) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(900, delayMillis = 500, easing = FastOutSlowInEasing)) }
    val max = (hours.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
        ) {
            hours.forEachIndexed { hour, value ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight((value.toFloat() / max * grow.value).coerceAtLeast(0.02f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(ink.copy(alpha = if (hour == peak) 1f else 0.3f)),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 4.dp)) {
            listOf("12a", "6a", "12p", "6p").forEach {
                Text(it, color = ink.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RatioBar(fraction: Float, ink: Color, label: String) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(fraction.coerceIn(0f, 1f), tween(1_000, delayMillis = 600, easing = FastOutSlowInEasing)) }
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(ink.copy(alpha = 0.18f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(grow.value)
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(ink),
            )
        }
        Text(label, color = ink.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

private fun stepsVerdict(average: Int): List<Int> = when {
    average < 3_000 -> listOf(R.string.verdict_steps_1a, R.string.verdict_steps_1b, R.string.verdict_steps_1c)
    average < 5_000 -> listOf(R.string.verdict_steps_2a, R.string.verdict_steps_2b)
    average < 8_000 -> listOf(R.string.verdict_steps_3a, R.string.verdict_steps_3b, R.string.verdict_steps_3c)
    average < 10_000 -> listOf(R.string.verdict_steps_4a, R.string.verdict_steps_4b)
    else -> listOf(R.string.verdict_steps_5a, R.string.verdict_steps_5b, R.string.verdict_steps_5c)
}

private fun sleepVerdict(minutes: Long): List<Int> = when {
    minutes < 6 * 60 -> listOf(R.string.verdict_sleep_1a, R.string.verdict_sleep_1b, R.string.verdict_sleep_1c)
    minutes < 7 * 60 -> listOf(R.string.verdict_sleep_2a, R.string.verdict_sleep_2b)
    minutes <= 9 * 60 -> listOf(R.string.verdict_sleep_3a, R.string.verdict_sleep_3b, R.string.verdict_sleep_3c)
    else -> listOf(R.string.verdict_sleep_4a, R.string.verdict_sleep_4b)
}

private fun balanceVerdict(perDay: Double): List<Int> = when {
    perDay < -700 -> listOf(R.string.verdict_cut_hard_a, R.string.verdict_cut_hard_b)
    perDay < -300 -> listOf(R.string.verdict_cut_steady_a, R.string.verdict_cut_steady_b)
    perDay < 0 -> listOf(R.string.verdict_cut_gentle_a, R.string.verdict_cut_gentle_b)
    else -> listOf(R.string.verdict_surplus_a, R.string.verdict_surplus_b)
}

private fun goalVerdict(hit: Int, possible: Int): List<Int> = when {
    hit == possible -> listOf(R.string.story_goal_all, R.string.verdict_goal_all_b, R.string.verdict_goal_all_c)
    hit * 2 >= possible -> listOf(R.string.story_goal_most, R.string.verdict_goal_most_b)
    hit > 0 -> listOf(R.string.story_goal_some, R.string.verdict_goal_some_b)
    else -> listOf(R.string.verdict_goal_none_a, R.string.verdict_goal_none_b)
}

private fun personality(hour: Int): Pair<Int, String> = when (hour) {
    in 4..8 -> R.string.persona_early to "🐦"
    in 9..11 -> R.string.persona_morning to "🌞"
    in 12..14 -> R.string.persona_lunch to "🍔"
    in 15..17 -> R.string.persona_afternoon to "😎"
    in 18..20 -> R.string.persona_evening to "🌟"
    else -> R.string.persona_night to "🦉"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun storySlides(report: PeriodReport, onDetails: () -> Unit): List<Slide> {
    val colors = TaroTheme.colors
    val inputs = report.inputs
    val week = inputs.period.kind == ReportKind.WEEK
    val random = Random(inputs.period.key.hashCode())
    val periodName = stringResource(if (week) R.string.story_week else R.string.story_month)
    val lastPeriod = stringResource(if (week) R.string.report_last_week else R.string.report_last_month)
    val dayFormat = java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val label = if (week) {
        stringResource(R.string.report_week_range, inputs.period.start.format(dayFormat), inputs.period.end.format(dayFormat))
    } else {
        "${inputs.period.start.month.getDisplayName(DayStyle.FULL, Locale.getDefault())} ${inputs.period.start.year}"
    }
    val km = Util.metersToDistance(report.distanceM).toDouble()
    val unit = Util.distanceUnit()
    val burnedAll = inputs.days.filter { it.complete }.sumOf { it.burned }
    val stepThing = pick(random, STEP_THINGS, report.steps.toDouble())
    val distanceThing = pick(random, DISTANCE_THINGS, report.distanceM)
    val snackThing = pick(random, SNACK_THINGS, report.activeKcal)
    val mealThing = pick(random, MEAL_THINGS, burnedAll)
    val sleepFact = SLEEP_FACTS.random(random)

    val slides = mutableListOf<Slide>()
    slides += Slide(colors.goal, listOf("🎉", "🥳", "🤩").random(random), scene = Scene.CONFETTI) { ink ->
        Lead(stringResource(R.string.story_intro, periodName), ink)
        Big(label, ink, delay = 250)
        Note(stringResource(listOf(R.string.story_intro_a, R.string.story_intro_b, R.string.story_intro_c).random(random)), ink, delay = 700)
        Note(stringResource(R.string.story_tap_hint), ink, delay = 1_100)
    }
    if (report.steps > 0) {
        slides += Slide(colors.flame, "👣") { ink ->
            Lead(stringResource(R.string.story_steps_lead), ink)
            CountUp(report.steps.toDouble(), ink, ::number)
            Note(stringResource(R.string.story_steps_note, grouped(report.averageSteps)), ink, delay = 1_400)
            Note(stringResource(R.string.bench_steps, grouped(WORLD_STEPS)), ink.copy(alpha = 0.8f), delay = 1_600)
            Note(stringResource(stepsVerdict(report.averageSteps).random(random)), ink, delay = 1_900)
            report.stepsChange?.let { change ->
                Chip(
                    stringResource(if (change >= 0) R.string.report_change_up else R.string.report_change_down, abs(change), lastPeriod),
                    ink,
                    delay = 1_700,
                )
            }
            Compare(stepThing, ink, delay = 2_400)
        }
        slides += Slide(SKY, distanceThing?.first?.emoji ?: "👟") { ink ->
            Lead(stringResource(R.string.story_distance_lead), ink)
            CountUp(km, ink, { "%.1f %s".format(Locale.getDefault(), it, unit) })
            Compare(distanceThing, ink, delay = 1_400)
            Note(stringResource(R.string.story_distance_daily, "%.1f".format(Locale.getDefault(), km / inputs.days.size), unit), ink, delay = 2_400)
            Note(stringResource(R.string.bench_distance), ink.copy(alpha = 0.8f), delay = 2_700)
        }
    }
    report.bestDay?.takeIf { inputs.days.size > 1 }?.let { best ->
        slides += Slide(colors.special, "🏆", scene = Scene.CONFETTI) { ink ->
            Lead(stringResource(R.string.story_best_lead), ink)
            Big(if (week) best.date.dayOfWeek.getDisplayName(DayStyle.FULL, Locale.getDefault()) else best.date.format(dayFormat), ink)
            Note(stringResource(R.string.story_best_note, grouped(best.steps)), ink, delay = 700)
            if (report.averageSteps > 0) {
                Chip(stringResource(R.string.story_best_vs, ((best.steps - report.averageSteps) * 100 / report.averageSteps)), ink, delay = 1_000)
            }
            if (week) MiniBars(inputs.days.map { it.steps }, inputs.days.indexOf(best), ink)
        }
    }
    if (report.goalDaysPossible > 0) {
        slides += Slide(colors.goal, if (report.goalDays == report.goalDaysPossible) "🔥" else "🎯") { ink ->
            Lead(stringResource(R.string.story_goal_lead), ink)
            Big(stringResource(R.string.story_goal_value, report.goalDays, report.goalDaysPossible), ink)
            Note(stringResource(goalVerdict(report.goalDays, report.goalDaysPossible).random(random)), ink, delay = 700)
            DayDots(inputs.days, ink)
        }
    }
    val hours = inputs.hourSteps
    if (hours.sum() > 0) {
        val peak = hours.indices.maxBy { hours[it] }
        val (persona, emoji) = personality(peak)
        slides += Slide(PINK, emoji) { ink ->
            Lead(stringResource(R.string.story_hour_lead), ink)
            Big(stringResource(persona), ink)
            Note(stringResource(R.string.story_hour_note, java.time.LocalTime.of(peak, 0).format(java.time.format.DateTimeFormatter.ofPattern("h a", Locale.getDefault()))), ink, delay = 700)
            HourChart(hours, peak, ink)
        }
    }
    if (inputs.longestWalkMin >= 5) {
        slides += Slide(SKY, "⏰") { ink ->
            Lead(stringResource(R.string.story_walk_lead), ink)
            CountUp(inputs.longestWalkMin.toDouble(), ink, { "${it.toInt()} min" })
            Compare(pick(random, WALK_THINGS, inputs.longestWalkMin.toDouble()), ink, delay = 1_400)
            val target = WHO_WEEKLY_MIN * inputs.days.size / 7
            Chip(stringResource(R.string.bench_brisk, inputs.briskMinutes, target), ink, delay = 2_200)
            Note(
                if (inputs.briskMinutes >= target) stringResource(R.string.verdict_brisk_done)
                else stringResource(R.string.verdict_brisk_short, ((target - inputs.briskMinutes + 14) / 15).coerceAtLeast(1)),
                ink,
                delay = 2_500,
            )
        }
    }
    if (report.activeKcal >= 1) {
        slides += Slide(colors.flame, snackThing?.first?.emoji ?: "🔥") { ink ->
            Lead(stringResource(R.string.story_active_lead), ink)
            CountUp(report.activeKcal, ink, { "${grouped(it)} kcal" })
            Compare(snackThing, ink, delay = 1_400)
        }
    }
    if (burnedAll > 0) {
        slides += Slide(PINK, mealThing?.first?.emoji ?: "🍽️") { ink ->
            Lead(stringResource(R.string.story_burned_lead), ink)
            CountUp(burnedAll, ink, { "${grouped(it)} kcal" })
            Compare(mealThing, ink, delay = 1_400)
            Note(stringResource(R.string.story_burned_daily, grouped(burnedAll / inputs.days.count { it.complete }.coerceAtLeast(1))), ink, delay = 2_400)
        }
    }
    report.expectedKg?.let { expected ->
        val weightThing = pick(random, WEIGHT_THINGS, abs(expected))
        slides += Slide(colors.special, "⚖️") { ink ->
            Lead(stringResource(R.string.story_balance_lead), ink)
            CountUp(abs(report.balance), ink, { "${grouped(it)} kcal" })
            Note(stringResource(if (report.balance <= 0) R.string.story_under else R.string.story_over, "%.2f".format(Locale.getDefault(), abs(expected))), ink, delay = 1_400)
            Compare(weightThing, ink, delay = 1_900)
            Note(stringResource(balanceVerdict(report.balance / report.loggedDays.coerceAtLeast(1)).random(random)), ink, delay = 2_300)
            report.actualKg?.let { actual ->
                val verdict = when (report.verdict) {
                    WeightVerdict.MATCHES -> R.string.story_scale_agrees
                    WeightVerdict.LOST_MORE -> R.string.story_scale_more
                    WeightVerdict.LOST_LESS -> R.string.story_scale_less
                    null -> R.string.story_scale_moved
                }
                Chip(stringResource(verdict, Util.formatWeightChange(actual)), ink, delay = 2_800)
            }
        }
    }
    inputs.topFoods.firstOrNull()?.let { top ->
        val loggedDays = inputs.days.count { it.eaten != null }
        slides += Slide(colors.goal, listOf("😋", "🥗", "🤤").random(random)) { ink ->
            Lead(stringResource(R.string.story_food_lead), ink)
            Big(top, ink)
            inputs.topFoods.drop(1).takeIf { it.isNotEmpty() }?.let { rest ->
                Note(stringResource(R.string.story_food_rest, rest.joinToString(stringResource(R.string.story_and))), ink, delay = 900)
            }
            Chip(stringResource(R.string.story_food_days, loggedDays, inputs.days.size), ink, delay = 1_300)
            if (inputs.proteinTarget > 0) Chip(stringResource(R.string.story_protein_days, report.proteinDays), ink, delay = 1_600)
        }
    }
    if (inputs.nightMinutes.isNotEmpty()) {
        val average = inputs.nightMinutes.average().toLong()
        val totalHours = (inputs.nightMinutes.sum() + inputs.napMinutes.sum()) / 60.0
        slides += Slide(colors.sleep, listOf("🌛", "😴", "🥱").random(random)) { ink ->
            Lead(stringResource(R.string.story_sleep_lead), ink)
            Big(stringResource(R.string.sleep_duration, (average / 60).toInt(), (average % 60).toInt()), ink)
            Note(stringResource(R.string.story_sleep_note), ink, delay = 700)
            RatioBar(average / 480f, ink, stringResource(R.string.bench_sleep))
            Note(stringResource(sleepVerdict(average).random(random)), ink, delay = 1_300)
            Compare(pick(random, SLEEP_THINGS, totalHours), ink, delay = 1_800)
            if (inputs.napMinutes.isNotEmpty()) Chip(stringResource(R.string.story_naps, inputs.napMinutes.size), ink, delay = 2_300)
            Note(stringResource(sleepFact), ink.copy(alpha = 0.8f), delay = 2_700)
        }
    }
    if (inputs.badges.isNotEmpty()) {
        slides += Slide(colors.special, "🌟", scene = Scene.CONFETTI) { ink ->
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
                            text = "🏅 " + stringResource(it),
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
    slides += Slide(colors.goal, "💯", scene = Scene.CONFETTI) { ink ->
        Lead(stringResource(R.string.story_summary, periodName), ink)
        Appear(300) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    SummaryTile("👣 " + stringResource(R.string.report_steps_word), grouped(report.steps), ink, Modifier.weight(1f))
                    SummaryTile("🗺️ $unit", "%.1f".format(Locale.getDefault(), km), ink, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
                    SummaryTile("🔥 " + stringResource(R.string.story_active_short), grouped(report.activeKcal), ink, Modifier.weight(1f))
                    SummaryTile("🎯 " + stringResource(R.string.report_goal_days), "${report.goalDays}/${report.goalDaysPossible}", ink, Modifier.weight(1f))
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
