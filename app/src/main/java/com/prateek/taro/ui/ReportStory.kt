package com.prateek.taro.ui

import java.time.format.TextStyle as DayStyle
import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.core.content.FileProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.report.Climax
import com.prateek.taro.report.DayInput
import com.prateek.taro.report.Moment
import com.prateek.taro.report.Period
import com.prateek.taro.report.PeriodReport
import com.prateek.taro.report.Plot
import com.prateek.taro.report.ReportData
import com.prateek.taro.report.ReportKind
import com.prateek.taro.report.Stillness
import com.prateek.taro.report.StoryData
import com.prateek.taro.report.StoryFacts
import com.prateek.taro.report.StoryFinder
import com.prateek.taro.report.WeightVerdict
import com.prateek.taro.ui.components.RollingText
import com.prateek.taro.ui.components.SPREE_NAMES
import com.prateek.taro.ui.components.monotonePath
import com.prateek.taro.ui.components.partial
import com.prateek.taro.ui.components.rememberBarGrowth
import com.prateek.taro.ui.components.rememberDrawIn
import com.prateek.taro.ui.components.spreeStyles
import com.prateek.taro.ui.components.tierStyles
import com.prateek.taro.ui.theme.DarkTaroTheme
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import com.prateek.taro.util.Sprees
import com.prateek.taro.util.StepTiers
import com.prateek.taro.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.random.Random

class ReportStoryScreen(private val periodKey: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val period = Period.parse(periodKey) ?: ReportData.latestComplete(ReportKind.WEEK)
        val scope = rememberCoroutineScope()
        DarkTaroTheme {
            ReportStory(
                period = period,
                onClose = { navigator.pop() },
                onDetails = { navigator.replace(ReportScreen(period.key)) },
                onShare = { image -> scope.launch { sharePoster(context, image) } },
            )
        }
    }
}

private const val SLIDE_MS = 7_000
private const val SHORT_SLIDE_MS = 5_000
private val STORY_BACKGROUND = Color(0xFF060807)
private const val AURA_ALPHA = 0.3f
private const val AURA_DRIFT = 0.06f
private const val AURA_MS = 14_000
private const val SLAM_FROM = 1.9f
private const val RISE_SHARE = 2f
private const val SPINE_ASPECT = 10f
private const val SPINE_DIM = 0.22f
private const val SPINE_FLOOR = 0.08f
private const val SPINE_BAR = 0.3f
private const val MAX_PICTOS = 42
private const val PICTO_STEP_MS = 40
private const val SMALL_SLAM = 0.6f
private const val MEDIUM_SLAM = 0.85f
private const val LINE_DIM = 0.75f
private const val ASIDE_DIM = 0.62f
private const val CHART_ASPECT = 2.1f
private const val LANE_ASPECT = 9f
private const val BAR_SHARE = 0.6f
private const val MISSED_ALPHA = 0.5f
private const val UNFOCUSED_ALPHA = 0.25f
private const val HEADROOM = 1.12f
private const val STROKE_SHARE = 0.008f
private const val NIGHT_FROM = 22
private const val NIGHT_HOURS = 9
private const val MAX_CHAIN = 14
private const val STILL_HAIR = 0.15f
private const val POSTER_GLOW = 0.22f

private class StorySlide(
    val aura: Color,
    val focus: Set<LocalDate>? = null,
    val short: Boolean = false,
    val content: @Composable ColumnScope.() -> Unit,
)

private enum class Entrance { RISE, SLAM, POP, FADE }

@Composable
private fun ReportStory(period: Period, onClose: () -> Unit, onDetails: () -> Unit, onShare: (ImageBitmap) -> Unit) {
    val context = LocalContext.current
    val loaded = rememberInBackground(period) { ReportData.load(context, period)?.let { it to StoryData.load(context, it) } }
    val data = loaded?.value
    val slides = data?.let { (report, facts) -> storySlides(report, facts, onDetails, onShare) }.orEmpty()
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    var index by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val shown = remember { intArrayOf(-1) }

    LaunchedEffect(index, paused, slides.size) {
        if (slides.isEmpty()) return@LaunchedEffect
        if (shown[0] != index) {
            progress.snapTo(0f)
            shown[0] = index
        }
        if (paused || index == slides.lastIndex) return@LaunchedEffect
        val length = if (slides[index].short) SHORT_SLIDE_MS else SLIDE_MS
        progress.animateTo(1f, tween(((1f - progress.value) * length).toInt(), easing = LinearEasing))
        if (index < slides.lastIndex) index++
    }

    val aura by animateColorAsState(slides.getOrNull(index)?.aura ?: colors.goal, tween(900), label = "aura")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(STORY_BACKGROUND)
            .pointerInput(slides.size) {
                detectTapGestures(
                    onPress = {
                        paused = true
                        tryAwaitRelease()
                        paused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width / 3f) index = (index - 1).coerceAtLeast(0) else if (index < slides.lastIndex) index++
                    },
                )
            },
    ) {
        Aura(aura)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = spacing.loose),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.hair), modifier = Modifier.padding(top = spacing.gap)) {
                slides.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(spacing.hair)
                            .drawBehind {
                                val radius = CornerRadius(size.height / 2)
                                drawRoundRect(Color.White.copy(alpha = 0.18f), cornerRadius = radius)
                                val fill = when {
                                    i < index -> 1f
                                    i == index -> progress.value
                                    else -> 0f
                                }
                                drawRoundRect(Color.White.copy(alpha = 0.9f), size = Size(size.width * fill, size.height), cornerRadius = radius)
                            },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = spacing.gap)) {
                Box(modifier = Modifier.weight(1f)) {
                    data?.first?.let { Spine(it.inputs.days, slides.getOrNull(index)?.focus) }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.report_dismiss),
                    tint = colors.accent,
                    modifier = Modifier
                        .padding(start = spacing.gap)
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .padding(spacing.tight),
                )
            }
            if (slides.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(if (loaded == null) R.string.story_loading else R.string.report_empty), style = TaroTheme.text.title, color = colors.accent)
                }
            } else {
                AnimatedContent(
                    targetState = index,
                    transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(200)) },
                    label = "story slide",
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = spacing.loose * 2, bottom = spacing.loose),
                        content = slides[page].content,
                    )
                }
            }
        }
    }
}

@Composable
private fun Aura(color: Color) {
    val enabled = rememberAnimationsEnabled()
    val drift = if (enabled) {
        rememberInfiniteTransition(label = "aura").animateFloat(-1f, 1f, infiniteRepeatable(tween(AURA_MS), RepeatMode.Reverse), label = "drift")
    } else {
        null
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val shift = (drift?.value ?: 0f) * size.width * AURA_DRIFT
        drawRect(
            Brush.radialGradient(
                listOf(color.copy(alpha = AURA_ALPHA), Color.Transparent),
                center = Offset(size.width * 0.2f + shift, size.height * 0.12f + shift),
                radius = size.width * 1.1f,
            )
        )
        drawRect(
            Brush.radialGradient(
                listOf(color.copy(alpha = AURA_ALPHA / 2), Color.Transparent),
                center = Offset(size.width * 0.9f - shift, size.height * 0.92f),
                radius = size.width * 0.9f,
            )
        )
    }
}

@Composable
private fun Spine(days: List<DayInput>, focus: Set<LocalDate>?) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val visible by animateFloatAsState(if (focus == null) 0f else 1f, tween(400), label = "spine")
    val max = (days.maxOfOrNull { maxOf(it.steps, it.goal) } ?: 1).coerceAtLeast(1)
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (days.size > 7) spacing.hair / 2 else spacing.tight),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(SPINE_ASPECT)
            .graphicsLayer { alpha = visible },
    ) {
        days.forEach { day ->
            val lit by animateFloatAsState(if (focus?.contains(day.date) == true) 1f else SPINE_DIM, tween(500), label = "day")
            val color = dayColor(day)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .drawBehind {
                        val height = size.height * (day.steps.toFloat() / max).coerceIn(SPINE_FLOOR, 1f)
                        val width = size.width * SPINE_BAR
                        drawRoundRect(
                            color.copy(alpha = color.alpha * lit),
                            topLeft = Offset((size.width - width) / 2, size.height - height),
                            size = Size(width, height),
                            cornerRadius = CornerRadius(minOf(width, height) / 2),
                        )
                    },
            )
        }
    }
}

@Composable
private fun dayColor(day: DayInput): Color =
    if (day.goal > 0 && day.steps >= day.goal) TaroTheme.colors.goal else MaterialTheme.colorScheme.onSurface.copy(alpha = MISSED_ALPHA)

@Composable
private fun Reveal(delay: Int, entrance: Entrance = Entrance.RISE, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val enabled = rememberAnimationsEnabled()
    val haptics = LocalHapticFeedback.current
    val rise = with(LocalDensity.current) { TaroTheme.spacing.loose.toPx() * RISE_SHARE }
    val progress = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(delay.toLong())
        when (entrance) {
            Entrance.SLAM -> {
                launch {
                    delay(SLAM_HAPTIC_MS)
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                progress.animateTo(1f, TaroMotion.bouncy())
            }
            Entrance.POP -> progress.animateTo(1f, TaroMotion.bouncy())
            else -> progress.animateTo(1f, tween(if (entrance == Entrance.FADE) 900 else 700))
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            when (entrance) {
                Entrance.RISE -> {
                    alpha = p.coerceIn(0f, 1f)
                    translationY = (1f - p) * rise
                }
                Entrance.SLAM -> {
                    alpha = (p * 2f).coerceIn(0f, 1f)
                    val scale = SLAM_FROM - (SLAM_FROM - 1f) * p
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
                Entrance.POP -> {
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = p
                    scaleY = p
                }
                Entrance.FADE -> alpha = p.coerceIn(0f, 1f)
            }
        },
    ) { content() }
}

private const val SLAM_HAPTIC_MS = 180L

@Composable
private fun Speaker(where: String? = null, delay: Int = 0) = Reveal(delay) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_pic_phone),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(with(LocalDensity.current) { type.heroLabel.lineHeight.toDp() }),
        )
        Text(
            text = (if (where == null) stringResource(R.string.story_phone) else stringResource(R.string.story_phone_at, where)).uppercase(),
            style = type.heroLabel,
            color = colors.accent,
            modifier = Modifier.padding(start = TaroTheme.spacing.tight),
        )
    }
}

@Composable
private fun Kick(text: String, delay: Int = 0) = Reveal(delay) {
    Text(text.uppercase(), style = TaroTheme.text.heroLabel, color = TaroTheme.colors.accent)
}

@Composable
private fun Line(text: String, highlight: String? = null, color: Color = TaroTheme.colors.goal, delay: Int = 0, style: TextStyle = TaroTheme.text.storyLine) = Reveal(delay) {
    val ink = MaterialTheme.colorScheme.onSurface
    val shown = buildAnnotatedString {
        val at = highlight?.let { text.lastIndexOf(it) } ?: -1
        if (at < 0) {
            append(text)
        } else {
            append(text.substring(0, at))
            withStyle(SpanStyle(color = color)) { append(highlight!!) }
            append(text.substring(at + highlight!!.length))
        }
    }
    Text(shown, style = style, color = ink, modifier = Modifier.padding(top = TaroTheme.spacing.gap))
}

@Composable
private fun Say(text: String, highlight: String? = null, color: Color = TaroTheme.colors.goal, delay: Int = 0) =
    Line(text, highlight, color, delay, TaroTheme.text.meterLabel.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = LINE_DIM)))

@Composable
private fun Aside(text: String, delay: Int) = Reveal(delay) {
    Text(
        text = text,
        style = TaroTheme.text.storyAside,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = ASIDE_DIM),
        modifier = Modifier.padding(top = TaroTheme.spacing.gap),
    )
}

@Composable
private fun Slam(text: String, color: Color = MaterialTheme.colorScheme.onSurface, delay: Int = 0, share: Float = 1f) = Reveal(delay, Entrance.SLAM, Modifier.padding(top = TaroTheme.spacing.gap)) {
    val stamp = TaroTheme.text.stamp
    Text(
        text = text.uppercase(),
        style = stamp.copy(fontSize = stamp.fontSize * share, lineHeight = stamp.lineHeight * share),
        color = color,
    )
}

@Composable
private fun BigNumber(value: String, color: Color, delay: Int = 0) = Reveal(delay) {
    RollingText(value, style = TaroTheme.text.storyNumber.copy(color = color))
}

private class Thing(@DrawableRes val icon: Int, @StringRes val long: Int, @StringRes val short: Int, val size: Double)

private val STEP_THINGS = listOf(
    Thing(R.drawable.ic_pic_tower, R.string.story_thing_eiffel, R.string.story_short_eiffel, 1_665.0),
    Thing(R.drawable.ic_pic_skyscraper, R.string.story_thing_burj, R.string.story_short_burj, 2_909.0),
)
private val DISTANCE_THINGS = listOf(
    Thing(R.drawable.ic_pic_bridge, R.string.story_thing_sealink, R.string.story_short_sealink, 5_600.0),
    Thing(R.drawable.ic_pic_mountain, R.string.story_thing_everest, R.string.story_short_everest, 8_849.0),
    Thing(R.drawable.ic_pic_medal, R.string.story_thing_marathon, R.string.story_short_marathon, 42_195.0),
    Thing(R.drawable.ic_pic_track, R.string.story_thing_track, R.string.story_short_track, 400.0),
)
private val SNACK_THINGS = listOf(
    Thing(R.drawable.ic_pic_samosa, R.string.story_thing_samosa, R.string.story_short_samosa, 203.0),
    Thing(R.drawable.ic_pic_pizza, R.string.story_thing_pizza, R.string.story_short_pizza, 285.0),
    Thing(R.drawable.ic_pic_banana, R.string.story_thing_banana, R.string.story_short_banana, 105.0),
    Thing(R.drawable.ic_pic_cup, R.string.story_thing_chai, R.string.story_short_chai, 68.0),
)
private val SLEEP_THINGS = listOf(
    Thing(R.drawable.ic_pic_film, R.string.story_thing_film, R.string.story_short_film, 2.5 * 60),
    Thing(R.drawable.ic_pic_koala, R.string.story_thing_koala, R.string.story_short_koala, 20.0 * 60),
)
private val WALK_THINGS = listOf(
    Thing(R.drawable.ic_pic_tv, R.string.story_thing_sitcom, R.string.story_short_sitcom, 22.0),
    Thing(R.drawable.ic_pic_note, R.string.story_thing_song, R.string.story_short_song, 3.5),
)
private val WEIGHT_THINGS = listOf(
    Thing(R.drawable.ic_pic_mango, R.string.story_thing_mango, R.string.story_short_mango, 0.3),
    Thing(R.drawable.ic_pic_banana, R.string.story_thing_banana_kg, R.string.story_short_banana, 0.12),
    Thing(R.drawable.ic_pic_phone, R.string.story_thing_phone, R.string.story_short_phone, 0.2),
)

private class Match(val thing: Thing, val count: Double)

private fun pick(random: Random, value: Double, vararg pools: Pair<List<Thing>, Double>): Match? =
    pools.flatMap { (pool, scale) -> pool.map { Match(it, value * scale / it.size) } }
        .filter { it.count >= 1.0 && it.count <= MAX_PICTOS * 4 }
        .randomOrNull(random)

private fun grouped(value: Number): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toDouble().roundToLong())

private fun amount(value: Double): String {
    val tenths = (value * 10).roundToLong()
    return if (value < 10 && tenths % 10 != 0L) "%.1f".format(Locale.getDefault(), tenths / 10.0) else grouped(value)
}

@Composable
private fun Pictos(match: Match, color: Color, delay: Int, columns: Int = 10) {
    val shown = minOf(ceil(match.count).toInt(), MAX_PICTOS)
    val partial = (match.count - floor(match.count)).toFloat().takeIf { it > 0f && match.count < MAX_PICTOS }
    val spacing = TaroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.padding(top = spacing.loose)) {
        (0 until shown).chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight)) {
                row.forEach { i ->
                    val last = i == shown - 1 && partial != null
                    Reveal(delay + i * PICTO_STEP_MS, Entrance.POP, Modifier.weight(1f)) {
                        Icon(
                            painter = painterResource(match.thing.icon),
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .then(if (last) Modifier.drawWithContent { clipRect(right = size.width * partial!!) { this@drawWithContent.drawContent() } } else Modifier),
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun StoryBars(days: List<DayInput>, focus: Set<LocalDate>, until: LocalDate? = null, gap: LocalDate? = null, madeUp: LocalDate? = null, delay: Int = 0) {
    val colors = TaroTheme.colors
    val ink = MaterialTheme.colorScheme.onSurface
    val measurer = rememberTextMeasurer()
    val axis = TaroTheme.text.axis.copy(color = colors.accent)
    val growth = rememberBarGrowth(delay, days.size)
    val week = days.size <= 7
    val max = (days.maxOfOrNull { maxOf(it.steps, it.goal) } ?: 1) * HEADROOM
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CHART_ASPECT)
            .padding(top = TaroTheme.spacing.gap),
    ) {
        val label = if (week) measurer.measure("M", axis).size.height.toFloat() else 0f
        val height = size.height - label * 1.3f
        val slot = size.width / days.size
        val width = slot * BAR_SHARE
        val stroke = size.minDimension * STROKE_SHARE
        fun y(value: Double) = (height - value / max * height).toFloat()
        val shownDays = days.filter { until == null || !it.date.isAfter(until) }
        shownDays.lastOrNull()?.goal?.takeIf { it > 0 }?.let { goal ->
            drawLine(ink.copy(alpha = 0.45f), Offset(0f, y(goal.toDouble())), Offset(size.width, y(goal.toDouble())), stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 3, stroke * 4)))
        }
        days.forEachIndexed { i, day ->
            val left = i * slot + (slot - width) / 2
            if (week) {
                val text = measurer.measure(day.date.dayOfWeek.getDisplayName(DayStyle.NARROW, Locale.getDefault()), axis)
                drawText(text, topLeft = Offset(left + (width - text.size.width) / 2, size.height - text.size.height))
            }
            if (day !in shownDays) return@forEachIndexed
            val grown = growth.bar(i)
            val alpha = if (day.date in focus || focus.isEmpty()) 1f else UNFOCUSED_ALPHA
            val radius = CornerRadius(width * 0.3f)
            if (day.date == madeUp && day.goal > 0) {
                val goalTop = y(day.goal.toDouble())
                drawRoundRect(colors.goal.copy(alpha = alpha), Offset(left, height - (height - goalTop) * grown), Size(width, (height - goalTop) * grown), radius)
                val top = y(day.steps.toDouble())
                drawRoundRect(colors.mint.copy(alpha = alpha), Offset(left, goalTop - (goalTop - top) * grown), Size(width, (goalTop - top) * grown - stroke), radius)
            } else {
                val top = y(day.steps.toDouble())
                val color = if (day.goal > 0 && day.steps >= day.goal) colors.goal else ink.copy(alpha = MISSED_ALPHA)
                drawRoundRect(color.copy(alpha = color.alpha * alpha), Offset(left, height - (height - top) * grown), Size(width, (height - top) * grown), radius)
            }
            if (day.date == gap && day.goal > day.steps) {
                drawRoundRect(
                    colors.red,
                    Offset(left, y(day.goal.toDouble())),
                    Size(width, y(day.steps.toDouble()) - y(day.goal.toDouble())),
                    radius,
                    style = Stroke(stroke * 1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 3, stroke * 3))),
                    alpha = grown.coerceIn(0f, 1f),
                )
            }
        }
    }
}

@Composable
private fun NightBlip(moment: Moment, label: String) {
    val colors = TaroTheme.colors
    val measurer = rememberTextMeasurer()
    val axis = TaroTheme.text.axis.copy(color = colors.accent)
    val note = TaroTheme.text.meterValue.copy(color = colors.special)
    val draw = rememberDrawIn(moment)
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(moment.at).atZone(zone)
    val hour = (time.hour + time.minute / 60.0 - NIGHT_FROM + 24) % 24
    Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(CHART_ASPECT)) {
        val axisHeight = measurer.measure("0", axis).size.height * 1.3f
        val laneHeight = size.height * 0.3f
        val laneTop = size.height - axisHeight - laneHeight
        val stroke = size.minDimension * STROKE_SHARE
        drawRoundRect(colors.sleep.copy(alpha = 0.18f), Offset(0f, laneTop), Size(size.width, laneHeight), CornerRadius(laneHeight / 2))
        val x = (hour / NIGHT_HOURS * size.width).toFloat()
        val top = size.height * 0.12f
        val reach = draw.value
        drawLine(colors.special, Offset(x, laneTop), Offset(x, laneTop - (laneTop - top) * reach), stroke * 2.5f, StrokeCap.Round)
        if (reach > 0.95f) {
            drawCircle(colors.special, stroke * 4, Offset(x, top))
            val text = measurer.measure(label, note)
            val textX = if (x + text.size.width + stroke * 8 < size.width) x + stroke * 6 else x - text.size.width - stroke * 6
            drawText(text, topLeft = Offset(textX, top - text.size.height / 2))
        }
        listOf(0 to "10p", 2 to "12a", 5 to "3a", 8 to "6a").forEach { (h, l) ->
            val text = measurer.measure(l, axis)
            val at = h.toFloat() / NIGHT_HOURS * size.width
            drawText(text, topLeft = Offset((at - text.size.width / 2).coerceIn(0f, size.width - text.size.width), size.height - text.size.height))
        }
    }
}

@Composable
private fun StillLane(minutes: List<Pair<Long, Int>>, still: Stillness) {
    val colors = TaroTheme.colors
    val measurer = rememberTextMeasurer()
    val axis = TaroTheme.text.axis.copy(color = colors.accent)
    val draw = rememberDrawIn(still)
    val zone = ZoneId.systemDefault()
    fun hourOf(at: Long) = Instant.ofEpochMilli(at).atZone(zone).let { it.hour + it.minute / 60f }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(LANE_ASPECT / 1.6f)
            .padding(top = TaroTheme.spacing.loose),
    ) {
        val axisHeight = measurer.measure("0", axis).size.height * 1.3f
        val lane = size.height - axisHeight
        val radius = CornerRadius(lane / 2)
        val stroke = size.minDimension * STROKE_SHARE * 2
        fun x(hour: Float) = hour / 24f * size.width
        drawRoundRect(Color.White.copy(alpha = 0.06f), Offset.Zero, Size(size.width, lane), radius)
        minutes.filter { it.second > 0 }.forEach { (at, _) ->
            val left = x(hourOf(at))
            drawRect(colors.goal.copy(alpha = draw.value), Offset(left, lane * 0.2f), Size(size.width / (24 * 60) * 2, lane * 0.6f))
        }
        val from = x(hourOf(still.from))
        val width = still.minutes / (24f * 60f) * size.width * draw.value
        drawRoundRect(colors.special.copy(alpha = STILL_HAIR), Offset(from, 0f), Size(width, lane), radius)
        drawRoundRect(colors.special, Offset(from, 0f), Size(width, lane), radius, style = Stroke(stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2))))
        listOf(0 to "12a", 6 to "6a", 12 to "12p", 18 to "6p").forEach { (h, l) ->
            val text = measurer.measure(l, axis)
            drawText(text, topLeft = Offset(x(h.toFloat()).coerceAtMost(size.width - text.size.width), size.height - text.size.height))
        }
    }
}

@Composable
private fun HourCurve(hours: List<Int>, color: Color) {
    val measurer = rememberTextMeasurer()
    val axis = TaroTheme.text.axis.copy(color = TaroTheme.colors.accent)
    val draw = rememberDrawIn(hours)
    val peak = hours.indices.maxByOrNull { hours[it] } ?: 0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(CHART_ASPECT * 1.2f),
    ) {
        val axisHeight = measurer.measure("0", axis).size.height * 1.3f
        val height = size.height - axisHeight
        val max = (hours.maxOrNull() ?: 1).coerceAtLeast(1) * HEADROOM
        val points = hours.mapIndexed { i, v -> Offset((i + 0.5f) / hours.size * size.width, (height - v / max * height).toFloat()) }
        val line = monotonePath(points)
        val area = monotonePath(points).apply {
            lineTo(points.last().x, height)
            lineTo(points.first().x, height)
            close()
        }
        drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.4f * draw.fill()), Color.Transparent), startY = 0f, endY = height))
        drawPath(line.partial(draw.value), color, style = Stroke(size.minDimension * STROKE_SHARE * 2.5f, cap = StrokeCap.Round))
        if (draw.value > 0.95f) drawCircle(color, size.minDimension * STROKE_SHARE * 5, points[peak])
        listOf(0 to "12a", 6 to "6a", 12 to "12p", 18 to "6p").forEach { (h, l) ->
            drawText(measurer.measure(l, axis), topLeft = Offset(h / 24f * size.width, size.height - axisHeight / 1.3f))
        }
    }
}

@Composable
private fun ShareBars(shares: List<Int>, color: Color, labels: List<String>) {
    val measurer = rememberTextMeasurer()
    val axis = TaroTheme.text.axis.copy(color = TaroTheme.colors.accent)
    val value = TaroTheme.text.meterValue
    val growth = rememberBarGrowth(shares, shares.size)
    Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(CHART_ASPECT).padding(top = TaroTheme.spacing.loose)) {
        val axisHeight = measurer.measure("0", axis).size.height * 1.4f
        val valueHeight = measurer.measure("0", value).size.height * 1.3f
        val height = size.height - axisHeight - valueHeight
        val slot = size.width / shares.size
        val width = slot * BAR_SHARE
        val top = (shares.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        shares.forEachIndexed { i, share ->
            val last = i == shares.lastIndex
            val alpha = if (last) 1f else 0.35f + 0.12f * i
            val h = share / top * height * growth.bar(i)
            val left = i * slot + (slot - width) / 2
            val top = valueHeight + height - h
            drawRoundRect(color.copy(alpha = alpha), Offset(left, top), Size(width, h), CornerRadius(width * 0.2f))
            val text = measurer.measure("$share%", value.copy(color = if (last) color else Color.White.copy(alpha = 0.6f)))
            drawText(text, topLeft = Offset(left + (width - text.size.width) / 2, top - text.size.height * 1.15f), alpha = growth.bar(i).coerceIn(0f, 1f))
            val label = measurer.measure(labels[i], axis)
            drawText(label, topLeft = Offset(left + (width - label.size.width) / 2, size.height - label.size.height))
        }
    }
}

@Composable
private fun CompareRow(label: String, value: String, fraction: Float, color: Color, delay: Int) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val grow = remember { Animatable(0f) }
    val enabled = rememberAnimationsEnabled()
    LaunchedEffect(Unit) {
        if (!enabled) grow.snapTo(1f) else {
            delay(delay.toLong())
            grow.animateTo(1f, TaroMotion.snappy())
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = spacing.gap)) {
        Text(label, style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface.copy(alpha = LINE_DIM), modifier = Modifier.weight(0.35f))
        Box(
            modifier = Modifier
                .weight(0.45f)
                .height(spacing.gap)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((fraction * grow.value).coerceIn(0f, 1f))
                    .height(spacing.gap)
                    .clip(CircleShape)
                    .background(color),
            )
        }
        Text(value, style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(0.2f).padding(start = spacing.gap))
    }
}

@Composable
private fun StreakChain(count: Int, delay: Int) {
    val colors = TaroTheme.colors
    val shown = count.coerceAtMost(MAX_CHAIN)
    val dot = with(LocalDensity.current) { TaroTheme.text.storyAside.lineHeight.toDp() }
    Row(horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.tight), modifier = Modifier.padding(top = TaroTheme.spacing.loose)) {
        repeat(shown) { i ->
            Reveal(delay + i * 90, Entrance.POP) {
                Box(modifier = Modifier.size(dot).clip(CircleShape).background(colors.goal))
            }
        }
    }
}

@Composable
private fun ProgressLine(fraction: Float, color: Color, delay: Int) {
    val grow = remember { Animatable(0f) }
    val enabled = rememberAnimationsEnabled()
    LaunchedEffect(Unit) {
        if (!enabled) grow.snapTo(1f) else {
            delay(delay.toLong())
            grow.animateTo(1f, TaroMotion.snappy())
        }
    }
    Box(
        modifier = Modifier
            .padding(top = TaroTheme.spacing.loose)
            .fillMaxWidth()
            .height(TaroTheme.spacing.tight)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth((fraction * grow.value).coerceIn(0f, 1f)).height(TaroTheme.spacing.tight).clip(CircleShape).background(color))
    }
}

@Composable
private fun NextWeek(highlight: Int?, firstDay: java.time.DayOfWeek, delay: Int) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.padding(top = spacing.loose)) {
        (0 until 7).forEach { i ->
            val day = firstDay.plus(i.toLong())
            val lit = day.value == highlight
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Reveal(delay + i * 70, Entrance.POP) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.8f)
                            .drawBehind {
                                val stroke = size.minDimension * 0.05f
                                drawRoundRect(
                                    if (lit) colors.goal else Color.White.copy(alpha = 0.22f),
                                    cornerRadius = CornerRadius(size.minDimension * 0.2f),
                                    style = Stroke(stroke, pathEffect = if (lit) null else PathEffect.dashPathEffect(floatArrayOf(stroke * 2, stroke * 2))),
                                )
                                if (lit) drawRoundRect(colors.goal.copy(alpha = 0.15f), cornerRadius = CornerRadius(size.minDimension * 0.2f))
                            },
                    )
                }
                Text(
                    day.getDisplayName(DayStyle.NARROW, Locale.getDefault()),
                    style = TaroTheme.text.axis,
                    color = if (lit) MaterialTheme.colorScheme.onSurface else colors.accent,
                    modifier = Modifier.padding(top = spacing.hair),
                )
            }
        }
    }
}

@Composable
private fun clockOf(minuteOfDay: Int): String {
    val context = LocalContext.current
    val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return LocalTime.of(minuteOfDay / 60 % 24, minuteOfDay % 60).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())).lowercase(Locale.getDefault())
}

@Composable
private fun hourOf(hour: Int): String {
    val context = LocalContext.current
    val pattern = if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h a"
    return LocalTime.of(hour % 24, 0).format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())).lowercase(Locale.getDefault())
}

private fun dayName(date: LocalDate) = date.dayOfWeek.getDisplayName(DayStyle.FULL, Locale.getDefault())

@Composable
private fun minutesText(minutes: Int): String = stringResource(R.string.sleep_duration, minutes / 60, minutes % 60)

private fun personaName(startHour: Int): Int = when (startHour + StoryFinder.PERSONA_HOURS / 2) {
    in 4..8 -> R.string.persona_early
    in 9..11 -> R.string.persona_morning
    in 12..14 -> R.string.persona_lunch
    in 15..17 -> R.string.persona_afternoon
    in 18..21 -> R.string.persona_evening
    else -> R.string.persona_night
}

@Composable
private fun storySlides(report: PeriodReport, facts: StoryFacts, onDetails: () -> Unit, onShare: (ImageBitmap) -> Unit): List<StorySlide> {
    val colors = TaroTheme.colors
    val inputs = report.inputs
    val days = inputs.days
    val week = inputs.period.kind == ReportKind.WEEK
    val random = Random(inputs.period.key.hashCode())
    val word = stringResource(if (week) R.string.story_week else R.string.story_month)
    val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val label = if (week) {
        stringResource(R.string.report_week_range, inputs.period.start.format(dayFormat), inputs.period.end.format(dayFormat))
    } else {
        "${inputs.period.start.month.getDisplayName(DayStyle.FULL, Locale.getDefault())} ${inputs.period.start.year}"
    }
    val everyDay = days.map { it.date }.toSet()
    val tiers = tierStyles()
    val sprees = spreeStyles()
    val stepMatch = pick(random, report.steps.toDouble(), STEP_THINGS to 1.0) to pick(random, report.distanceM, DISTANCE_THINGS to 1.0)
    val steps = listOfNotNull(stepMatch.first, stepMatch.second).randomOrNull(random)
    val snack = pick(random, report.activeKcal, SNACK_THINGS to 1.0)
    val sleep = pick(random, facts.sleepMinutes.toDouble(), SLEEP_THINGS to 1.0)
    val weight = report.actualKg?.let { pick(random, abs(it), WEIGHT_THINGS to 1.0) }
    val slides = mutableListOf<StorySlide>()
    val plotColor = plotColor(facts.plot)

    slides += StorySlide(colors.goal) {
        Kick(label)
        Slam(stringResource(R.string.story_cover_a), delay = 250)
        Slam(stringResource(R.string.story_cover_b), colors.goal, delay = 450)
        Line(stringResource(if (facts.plot == Plot.REST) R.string.story_cover_quiet else R.string.story_cover_line, word), delay = 900)
        Spacer(Modifier.weight(1f))
        Speaker(delay = 1_400)
    }

    if (report.steps > 0) {
        slides += StorySlide(colors.goal, everyDay) {
            Speaker()
            Line(stringResource(R.string.story_counted), delay = 150)
            BigNumber(grouped(report.steps), colors.goal, delay = 250)
            steps?.let { match ->
                val count = amount(match.count)
                Line(stringResource(R.string.story_steps_word) + " " + stringResource(match.thing.long, count), count, delay = 700)
            }
            report.stepsChange?.takeIf { abs(it) >= 5 }?.let { change ->
                val text = stringResource(if (change > 0) R.string.story_steps_more else R.string.story_steps_fewer, abs(change), word)
                Say(text, "${abs(change)}%", if (change > 0) colors.goal else colors.accent, delay = 1_000)
            }
            steps?.let { Pictos(it, colors.goal, delay = 1_200, columns = if (it.count > 30) 14 else 10) }
            Spacer(Modifier.weight(1f))
            val aside = when {
                report.averageSteps >= 10_000 -> R.string.story_steps_aside_high
                report.averageSteps >= 6_000 -> R.string.story_steps_aside_mid
                else -> R.string.story_steps_aside_low
            }
            Aside(stringResource(aside), delay = 2_600)
        }
    }

    facts.odd?.let { odd ->
        val at = Instant.ofEpochMilli(odd.at).atZone(ZoneId.systemDefault())
        val evening = at.toLocalDate().minusDays(1)
        slides += StorySlide(colors.special, setOf(evening).filter { it in everyDay }.toSet()) {
            val clock = clockOf(at.hour * 60 + at.minute)
            Speaker(stringResource(R.string.story_night_of, dayName(evening)))
            Line(stringResource(R.string.story_odd_line, clock, grouped(odd.steps)), clock, colors.special, delay = 200)
            Slam(stringResource(R.string.story_odd_slam), delay = 900, share = MEDIUM_SLAM)
            Spacer(Modifier.weight(1f))
            NightBlip(odd, stringResource(R.string.story_odd_blip, clock, grouped(odd.steps)))
            Aside(stringResource(R.string.story_odd_aside), delay = 1_900)
        }
    }

    val turn = facts.turn
    val still = facts.still
    val stillOnLow = turn != null && still?.date == turn.low.date
    if (still != null && !stillOnLow && facts.odd == null) {
        slides += StorySlide(colors.special, setOf(still.date)) {
            val length = minutesText(still.minutes)
            Speaker(dayName(still.date))
            Line(stringResource(R.string.story_still_line, length), length, colors.special, delay = 200)
            Aside(stringResource(R.string.story_still_aside), delay = 800)
            Spacer(Modifier.weight(1f))
            StillLane(facts.stillMinutes, still)
        }
    }

    if (turn != null) {
        slides += StorySlide(colors.red, setOf(turn.low.date)) {
            Speaker(dayName(turn.low.date))
            Line(stringResource(R.string.story_low_line, dayName(turn.low.date)), delay = 200)
            if (stillOnLow && still != null) {
                val length = minutesText(still.minutes)
                Line(stringResource(R.string.story_still_line, length), length, colors.special, delay = 500)
                Aside(stringResource(R.string.story_still_aside), delay = 900)
                StillLane(facts.stillMinutes, still)
            }
            Spacer(Modifier.weight(1f))
            StoryBars(days, setOf(turn.low.date), until = turn.low.date, gap = turn.low.date, delay = 1)
            val short = stringResource(R.string.story_low_short, grouped(turn.low.goal - turn.low.steps))
            val stakes = if (turn.streakBefore) " " + stringResource(R.string.story_low_stakes) else ""
            Line(short + stakes, short, colors.red, delay = 1_600, style = TaroTheme.text.title)
        }
        slides += StorySlide(colors.mint, setOf(turn.turn.date)) {
            Speaker(dayName(turn.turn.date))
            Line(stringResource(R.string.story_turn_line, dayName(turn.turn.date)), delay = 200)
            Slam(stringResource(if (turn.madeUp) R.string.story_turn_made else R.string.story_turn_back), colors.mint, delay = 800, share = MEDIUM_SLAM)
            val walked = grouped(turn.turn.steps)
            val note = when {
                turn.madeUp -> stringResource(R.string.story_turn_made_note, dayName(turn.low.date), grouped(turn.low.goal - turn.low.steps), walked)
                turn.rested -> stringResource(R.string.story_turn_rest_note, dayName(turn.low.date), walked)
                else -> stringResource(R.string.story_turn_note, walked)
            }
            Say(note, walked, colors.mint, delay = 1_100)
            Spacer(Modifier.weight(1f))
            StoryBars(days, setOf(turn.low.date, turn.turn.date), until = turn.turn.date, madeUp = turn.turn.date.takeIf { turn.madeUp }, delay = 2)
            if (facts.next.streak > 0) {
                StreakChain(facts.next.streak, delay = 1_500)
                Say(pluralStringResource(R.plurals.story_streak_alive, facts.next.streak, facts.next.streak), delay = 1_900)
            }
        }
    }

    facts.climax?.takeIf { days.size > 1 }?.let { climax ->
        val tier = StepTiers.reached(climax.day.steps, climax.day.goal)
        val color = if (tier >= 1) tiers[tier].color else colors.flame
        slides += StorySlide(color, setOf(climax.day.date)) {
            ClimaxSlide(climax, tier, color, facts, random)
        }
    }

    if (report.activeKcal >= 50) {
        slides += StorySlide(colors.flame) {
            val kcal = grouped(report.activeKcal)
            Speaker(stringResource(R.string.story_kitchen))
            Line(stringResource(R.string.story_food_line, kcal) + (snack?.let { " " + stringResource(it.thing.long, amount(it.count)) } ?: ""), "$kcal kcal", colors.flame, delay = 200)
            snack?.let { Pictos(it, colors.flame, delay = 700) }
            Spacer(Modifier.weight(1f))
            facts.topFood?.takeIf { it.second >= 3 }?.let { (name, count) ->
                Line(stringResource(R.string.story_food_habit, name, count), "$count", colors.special, delay = 1_500)
                Aside(stringResource(R.string.story_food_habit_aside), delay = 1_900)
                Pictos(Match(Thing(R.drawable.ic_pic_bowl, 0, 0, 1.0), count.toDouble()), colors.special, delay = 2_100)
            }
        }
    }

    if (facts.sleepMinutes >= 60) {
        slides += StorySlide(colors.sleep) {
            val hours = stringResource(R.string.story_hours, (facts.sleepMinutes / 60).toInt())
            Speaker(stringResource(R.string.story_nightstand))
            Line(stringResource(R.string.story_sleep_line, hours) + (sleep?.let { " " + stringResource(it.thing.long, amount(it.count)) } ?: ""), hours, colors.sleep, delay = 200)
            sleep?.let { Pictos(it, colors.sleep, delay = 700) }
            Spacer(Modifier.weight(1f))
            facts.late?.let { late ->
                Line(stringResource(R.string.story_late_line, clockOf(late.usualMinute)), delay = 1_400)
                Slam(stringResource(R.string.story_late_slam, dayName(late.date)), colors.sleep, delay = 1_900, share = SMALL_SLAM)
                Aside(stringResource(R.string.story_late_aside, clockOf(late.minute)), delay = 2_300)
            }
        }
    }

    report.actualKg?.takeIf { abs(it) >= 0.1 }?.let { actual ->
        slides += StorySlide(colors.goal) {
            val change = Util.formatWeightChange(abs(actual)).removePrefix("+")
            Speaker(stringResource(R.string.story_scale_via))
            Line(stringResource(if (actual < 0) R.string.story_weight_down else R.string.story_weight_up, change), change, if (actual < 0) colors.goal else colors.special, delay = 200)
            weight?.let { match ->
                Pictos(match, colors.special, delay = 800, columns = 6)
                Say(stringResource(match.thing.long, amount(match.count)), delay = 1_100)
            }
            Spacer(Modifier.weight(1f))
            report.expectedKg?.let { expected ->
                val scale = maxOf(abs(expected), abs(actual)) * 1.2
                CompareRow(stringResource(R.string.story_log), Util.formatWeightChange(expected), (abs(expected) / scale).toFloat(), colors.special, 1_300)
                CompareRow(stringResource(R.string.story_scale), Util.formatWeightChange(actual), (abs(actual) / scale).toFloat(), colors.goal, 1_500)
                report.verdict?.let { verdict ->
                    val text = when (verdict) {
                        WeightVerdict.MATCHES -> R.string.story_verdict_match
                        WeightVerdict.LOST_MORE -> R.string.story_verdict_more
                        WeightVerdict.LOST_LESS -> R.string.story_verdict_less
                    }
                    Aside(stringResource(text), delay = 1_900)
                }
            }
        }
    }

    facts.persona?.let { persona ->
        slides += StorySlide(colors.mint) {
            val from = hourOf(persona.startHour)
            val to = hourOf(persona.startHour + StoryFinder.PERSONA_HOURS)
            val now = persona.now
            val then = persona.usual
            val changed = then != null && persona.before.size >= 2 && abs(now - then) >= 5
            val shares = persona.before + now
            Speaker(stringResource(R.string.story_observation))
            if (changed) {
                Line(stringResource(R.string.story_persona_change, then!!, from, to, word, now), "$now%", colors.mint, delay = 200)
            } else {
                Line(stringResource(R.string.story_persona_same, now, from, to), "$now%", colors.mint, delay = 200)
            }
            if (shares.size >= 2) {
                ShareBars(
                    shares,
                    colors.mint,
                    shares.indices.map { i ->
                        if (i == shares.lastIndex) stringResource(R.string.story_this, word) else stringResource(R.string.story_weeks_ago, shares.lastIndex - i)
                    },
                )
            }
            Spacer(Modifier.weight(1f))
            Slam(stringResource(personaName(persona.startHour)), colors.mint, delay = 1_400, share = MEDIUM_SLAM)
            Aside(stringResource(if (changed) R.string.story_persona_aside_change else R.string.story_persona_aside_same), delay = 1_900)
        }
    }

    val next = facts.next
    if (next.spree != null || next.slowDay != null) {
        slides += StorySlide(colors.goal) {
            Speaker(stringResource(R.string.story_next, word))
            next.spree?.let { spree ->
                val name = stringResource(SPREE_NAMES[spree])
                val left = Sprees.DAYS[spree] - next.streak
                Line(pluralStringResource(R.plurals.story_spree_away, left, name, left), name, sprees[spree].color, delay = 200)
                ProgressLine(next.streak.toFloat() / Sprees.DAYS[spree], sprees[spree].color, delay = 500)
                Say(stringResource(R.string.story_spree_progress, next.streak, Sprees.DAYS[spree]), delay = 700)
            }
            Spacer(Modifier.weight(1f))
            next.slowDay?.let { slow ->
                NextWeek(slow.value, ReportData.firstDayOfWeek(), delay = 900)
                val name = slow.getDisplayName(DayStyle.FULL, Locale.getDefault())
                Line(stringResource(R.string.story_next_slow, name, grouped(next.slowSteps)), name + "s", colors.goal, delay = 1_300)
                Aside(stringResource(R.string.story_next_aside), delay = 1_700)
            }
        }
    }

    slides += StorySlide(plotColor) {
        Poster(report, facts, label, word, plotColor, steps, snack, weight, onShare, onDetails)
    }
    return slides
}

@Composable
private fun ColumnScope.ClimaxSlide(climax: Climax, tier: Int, color: Color, facts: StoryFacts, random: Random) {
    val tiers = tierStyles()
    val walk = pick(random, climax.walkMinutes.toDouble(), WALK_THINGS to 1.0)
    val stepsText = grouped(climax.day.steps)
    Speaker(dayName(climax.day.date))
    Slam(if (tier >= 1) stringResource(tiers[tier].name) else dayName(climax.day.date), color, delay = 250, share = MEDIUM_SLAM)
    val line = if (climax.walkMinutes >= 10) {
        stringResource(R.string.story_climax_line, stepsText, climax.walkMinutes) + (walk?.let { " " + stringResource(it.thing.long, amount(it.count)) } ?: "")
    } else {
        stringResource(R.string.story_climax_short, stepsText)
    }
    Line(line, stepsText, color, delay = 700)
    if (climax.walkMinutes >= 10) walk?.let { Pictos(it, color, delay = 1_100) }
    Say(stringResource(R.string.story_climax_score, grouped(climax.score)), grouped(climax.score), color, delay = 1_300)
    Spacer(Modifier.weight(1f))
    HourCurve(climax.hours, color)
    facts.samePeak?.takeIf { climax.day.date in it.dates }?.let { peak ->
        val others = peak.dates.filter { it != climax.day.date }.joinToString(stringResource(R.string.story_and)) { dayName(it) }
        Aside(stringResource(R.string.story_peak_aside, others, hourOf(peak.hour)), delay = 2_000)
    }
}

@Composable
private fun ColumnScope.Poster(
    report: PeriodReport,
    facts: StoryFacts,
    label: String,
    word: String,
    color: Color,
    steps: Match?,
    snack: Match?,
    weight: Match?,
    onShare: (ImageBitmap) -> Unit,
    onDetails: () -> Unit,
) {
    val colors = TaroTheme.colors
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val ink = MaterialTheme.colorScheme.onSurface
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val title = plotTitle(facts.plot, word)
    val rows = buildList {
        add(stringResource(R.string.story_poster_steps, grouped(report.steps)) to (steps?.let { stringResource(it.thing.short, amount(it.count)) } ?: ""))
        if (report.activeKcal >= 50) add(stringResource(R.string.story_poster_active, grouped(report.activeKcal)) to (snack?.let { stringResource(it.thing.short, amount(it.count)) } ?: ""))
        report.actualKg?.takeIf { abs(it) >= 0.1 }?.let { add(Util.formatWeightChange(it) to (weight?.let { w -> stringResource(w.thing.short, amount(w.count)) } ?: "")) }
        facts.odd?.let { odd ->
            val at = Instant.ofEpochMilli(odd.at).atZone(ZoneId.systemDefault())
            add(stringResource(R.string.story_odd_blip, clockOf(at.hour * 60 + at.minute), grouped(odd.steps)) to stringResource(R.string.story_unexplained))
        }
    }
    Kick(stringResource(R.string.story_that_was, word))
    Reveal(200) {
        Column(
            modifier = Modifier
                .padding(top = spacing.loose)
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
                .clip(RoundedCornerShape(spacing.loose * 1.5f))
                .background(STORY_BACKGROUND)
                .background(Brush.linearGradient(listOf(color.copy(alpha = POSTER_GLOW), Color.White.copy(alpha = 0.03f))))
                .padding(spacing.loose),
        ) {
            Text(label.uppercase(), style = type.heroLabel, color = colors.accent)
            Text(title.uppercase(), style = type.stamp.copy(fontSize = type.stamp.fontSize * SMALL_SLAM, lineHeight = type.stamp.lineHeight * SMALL_SLAM), color = ink, modifier = Modifier.padding(top = spacing.tight))
            StoryBars(report.inputs.days, emptySet(), delay = 3)
            rows.forEach { (left, right) ->
                Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.gap), verticalAlignment = Alignment.CenterVertically) {
                    Text(left, style = type.meterLabel, color = ink, modifier = Modifier.weight(1f))
                    Text(right, style = type.meterValue, color = colors.accent)
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = spacing.loose)) {
                Text(stringResource(R.string.app_name).uppercase(), style = type.heroLabel, color = colors.accent, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.story_signed).uppercase(), style = type.heroLabel, color = colors.accent)
            }
        }
    }
    Spacer(Modifier.weight(1f))
    Reveal(900) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.loose * 2, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            com.prateek.taro.ui.components.LinkText(stringResource(R.string.story_share), ink, onClick = { scope.launch { onShare(layer.toImageBitmap()) } })
            com.prateek.taro.ui.components.LinkText(stringResource(R.string.story_full), colors.goal, onClick = onDetails)
        }
    }
}

private suspend fun sharePoster(context: Context, image: ImageBitmap) {
    val uri = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "shares").apply { mkdirs() }
        val file = File(folder, "taro_report.png")
        file.outputStream().use { image.asAndroidBitmap().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null))
}

@Composable
fun plotColor(plot: Plot): Color = when (plot) {
    Plot.COMEBACK -> TaroTheme.colors.mint
    Plot.BREAKTHROUGH -> TaroTheme.colors.flame
    Plot.REST -> TaroTheme.colors.sleep
    else -> TaroTheme.colors.goal
}

@Composable
fun plotTitle(plot: Plot, word: String): String = stringResource(
    when (plot) {
        Plot.COMEBACK -> R.string.story_title_comeback
        Plot.BREAKTHROUGH -> R.string.story_title_breakthrough
        Plot.STEADY -> R.string.story_title_steady
        Plot.REST -> R.string.story_title_rest
        Plot.PLAIN -> R.string.story_title_plain
    },
    word,
)
