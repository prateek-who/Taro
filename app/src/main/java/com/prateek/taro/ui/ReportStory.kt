package com.prateek.taro.ui

import java.time.format.TextStyle as DayStyle
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.core.content.FileProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.report.FastMinute
import com.prateek.taro.report.Period
import com.prateek.taro.report.PeriodReport
import com.prateek.taro.report.Plot
import com.prateek.taro.report.ReportData
import com.prateek.taro.report.ReportKind
import com.prateek.taro.report.StoryData
import com.prateek.taro.report.StoryFacts
import com.prateek.taro.report.StoryType
import com.prateek.taro.report.TwinKind
import com.prateek.taro.report.VersusKind
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.theme.DarkTaroTheme
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
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
import java.time.temporal.IsoFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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

private data class Duo(val bg: Color, val fg: Color)

private val TOMATO = Duo(Color(0xFFFF5B3A), Color(0xFF1C0702))
private val FOREST = Duo(Color(0xFF0D2414), Color(0xFFA6F57E))
private val MIDNIGHT = Duo(Color(0xFF10153F), Color(0xFFB4BDFF))
private val PLUM = Duo(Color(0xFF2C0B33), Color(0xFFFF9DE2))
private val COCOA = Duo(Color(0xFF24160A), Color(0xFFFFD36B))
private val TEAL = Duo(Color(0xFF0A2B28), Color(0xFF6FF0C8))
private val CREAM = Duo(Color(0xFFF3EFE4), Color(0xFF14800F))
private val INK = Duo(Color(0xFF0E0E0E), Color(0xFFFFD36B))
private val COUCH = Duo(Color(0xFF1D1D1D), Color(0xFF8A8A8A))
private val CREAM_INK = Color(0xFF151816)
private val THIS_PERIOD = Color(0xFFFF8A50)

private val LocalDuo = staticCompositionLocalOf { FOREST }

private const val SLIDE_MS = 7_000
private const val SOFT = 0.14f
private const val DIM = 0.75f
private const val SLAM_FROM = 2.2f
private const val RISE_SHARE = 2f
private const val SPIN_FROM = -200f
private const val SLAM_HAPTIC_MS = 180L
private const val STICKER_TILT = -6f
private const val ANSWER_MS = 650L
private const val COUNT_MS = 1_400
private const val DOT_STEP_MS = 30
private const val MAX_DOTS = 120
private const val CADENCE_ZONES = 4
private val CADENCE_EDGES = listOf(60, 100, 120, 140, 185)
private const val BRISK = 120
private const val RUN = 140
private const val TWIST_TIMES = 3
private const val POWER_STEPS = 1_000
private const val WIDE_MARQUEE = 1.5f
private const val MARQUEE_TILT = -7f
private const val MAX_RANK = 10
private const val CLOSE_SHARE = 0.05
private const val FAVOURITE_DAY_KCAL = 2_000.0
private const val WEEK_DAYS = 14
private const val MARATHON_KM = 42.195
private val HALF_MILLION = 500_000L
private val MILLION = 1_000_000L

private class StorySlide(
    val duo: Duo,
    val gated: Boolean = false,
    val bleed: Boolean = false,
    val content: @Composable ColumnScope.(answered: Boolean, release: () -> Unit) -> Unit,
)

private enum class Entrance { RISE, SLAM, POP, SPIN }

@Composable
private fun ReportStory(period: Period, onClose: () -> Unit, onDetails: () -> Unit, onShare: (ImageBitmap) -> Unit) {
    val context = LocalContext.current
    val loaded = rememberInBackground(period) { ReportData.load(context, period)?.let { it to StoryData.load(context, it) } }
    val data = loaded?.value
    val slides = data?.let { (report, facts) -> storySlides(report, facts, onDetails, onShare) }.orEmpty()
    val spacing = TaroTheme.spacing
    var index by remember { mutableIntStateOf(0) }
    var paused by remember { mutableStateOf(false) }
    val released = remember { mutableStateListOf<Int>() }
    val progress = remember { Animatable(0f) }
    val shown = remember { intArrayOf(-1) }
    val waiting = slides.getOrNull(index)?.let { it.gated && index !in released } ?: false

    LaunchedEffect(index, paused, slides.size, waiting) {
        if (slides.isEmpty()) return@LaunchedEffect
        if (shown[0] != index) {
            progress.snapTo(0f)
            shown[0] = index
        }
        if (paused || waiting || index == slides.lastIndex) return@LaunchedEffect
        progress.animateTo(1f, tween(((1f - progress.value) * SLIDE_MS).toInt(), easing = LinearEasing))
        if (index < slides.lastIndex) index++
    }

    val duo = slides.getOrNull(index)?.duo ?: FOREST
    val bg by animateColorAsState(duo.bg, tween(500), label = "story bg")
    val fg by animateColorAsState(duo.fg, tween(500), label = "story fg")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
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
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.hair), modifier = Modifier.padding(start = spacing.loose, end = spacing.loose, top = spacing.gap)) {
                slides.indices.forEach { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(spacing.hair)
                            .drawBehind {
                                val radius = CornerRadius(size.height / 2)
                                drawRoundRect(fg.copy(alpha = 0.22f), cornerRadius = radius)
                                val fill = when {
                                    i < index -> 1f
                                    i == index -> progress.value
                                    else -> 0f
                                }
                                drawRoundRect(fg, size = Size(size.width * fill, size.height), cornerRadius = radius)
                            },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = spacing.loose, end = spacing.tight)) {
                Text(
                    text = data?.first?.let { periodLabel(it.inputs.period) }.orEmpty(),
                    style = TaroTheme.text.note,
                    color = fg.copy(alpha = DIM),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.report_dismiss),
                    tint = fg,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                        .padding(spacing.gap),
                )
            }
            if (slides.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(if (loaded == null) R.string.story_loading else R.string.report_empty), style = TaroTheme.text.title, color = fg)
                }
            } else {
                AnimatedContent(
                    targetState = index,
                    transitionSpec = { fadeIn(tween(260, delayMillis = 160)) togetherWith fadeOut(tween(160)) },
                    label = "story slide",
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val slide = slides[page]
                    CompositionLocalProvider(LocalDuo provides slide.duo) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (slide.bleed) Modifier else Modifier.navigationBarsPadding().padding(start = spacing.loose + spacing.tight, end = spacing.loose + spacing.tight, top = spacing.loose, bottom = spacing.loose)),
                        ) {
                            slide.content(this, page in released) { if (page !in released) released += page }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun periodLabel(period: Period): String {
    val dayFormat = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    return if (period.kind == ReportKind.WEEK) {
        stringResource(R.string.report_week_range, period.start.format(dayFormat), period.end.format(dayFormat))
    } else {
        "${period.start.month.getDisplayName(DayStyle.FULL, Locale.getDefault())} ${period.start.year}"
    }
}

@Composable
private fun Reveal(delay: Int, entrance: Entrance = Entrance.RISE, modifier: Modifier = Modifier, tilt: Float = 0f, content: @Composable () -> Unit) {
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
            Entrance.POP, Entrance.SPIN -> progress.animateTo(1f, TaroMotion.bouncy())
            Entrance.RISE -> progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            rotationZ = tilt
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
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                Entrance.POP -> {
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = p
                    scaleY = p
                }
                Entrance.SPIN -> {
                    alpha = p.coerceIn(0f, 1f)
                    scaleX = p
                    scaleY = p
                    rotationZ = SPIN_FROM + (tilt - SPIN_FROM) * p
                }
            }
        },
    ) { content() }
}

@Composable
private fun Kick(text: String, delay: Int = 0, color: Color = LocalDuo.current.fg) = Reveal(delay) {
    Text(text.uppercase(), style = TaroTheme.text.storyKick, color = color.copy(alpha = DIM))
}

@Composable
private fun Head(text: String, delay: Int = 0, color: Color = LocalDuo.current.fg, entrance: Entrance = Entrance.RISE, accent: String? = null, accentColor: Color = color) =
    Reveal(delay, entrance, Modifier.padding(top = TaroTheme.spacing.gap)) {
        Text(highlighted(text, accent, accentColor), style = TaroTheme.text.storyHead, color = color)
    }

@Composable
private fun Mid(text: String, delay: Int = 0, color: Color = LocalDuo.current.fg, modifier: Modifier = Modifier) = Reveal(delay, modifier = modifier) {
    Text(text, style = TaroTheme.text.storyMid, color = color)
}

@Composable
private fun Say(text: String, delay: Int = 0, bold: String? = null, color: Color = LocalDuo.current.fg, alpha: Float = 1f, modifier: Modifier = Modifier) =
    Reveal(delay, modifier = modifier.padding(top = TaroTheme.spacing.gap)) {
        Text(highlighted(text, bold, color, FontWeight.Black), style = TaroTheme.text.storySay, color = color.copy(alpha = alpha))
    }

private fun highlighted(text: String, part: String?, color: Color, weight: FontWeight? = null) = buildAnnotatedString {
    val at = part?.let { text.indexOf(it) } ?: -1
    if (at < 0) {
        append(text)
    } else {
        append(text.substring(0, at))
        withStyle(SpanStyle(color = color, fontWeight = weight)) { append(part!!) }
        append(text.substring(at + part!!.length))
    }
}

@Composable
private fun Giant(text: String, delay: Int = 0, color: Color = LocalDuo.current.fg, entrance: Entrance = Entrance.SLAM, scale: Float = 1f) =
    Reveal(delay, entrance, Modifier.fillMaxWidth()) {
        val style = TaroTheme.text.storyGiant.let { it.copy(fontSize = it.fontSize * scale, lineHeight = it.lineHeight * scale, color = color) }
        BasicText(text, style = style, maxLines = 1, autoSize = TextAutoSize.StepBased(maxFontSize = style.fontSize))
    }

@Composable
private fun CountGiant(value: Long, delay: Int = 0, color: Color = LocalDuo.current.fg, scale: Float = 1f, prefix: String = "") {
    val enabled = rememberAnimationsEnabled()
    val count = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(delay.toLong())
        count.animateTo(1f, tween(COUNT_MS, easing = FastOutSlowInEasing))
    }
    val style = TaroTheme.text.storyGiant.let { it.copy(fontSize = it.fontSize * scale, lineHeight = it.lineHeight * scale, color = color) }
    BasicText(
        text = prefix + grouped((value * count.value).roundToLong()),
        style = style,
        maxLines = 1,
        autoSize = TextAutoSize.StepBased(maxFontSize = style.fontSize),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Sticker(text: String, delay: Int, bg: Color = LocalDuo.current.fg, fg: Color = LocalDuo.current.bg, tilt: Float = STICKER_TILT, modifier: Modifier = Modifier) =
    Reveal(delay, Entrance.SPIN, modifier.padding(top = TaroTheme.spacing.loose), tilt) {
        Text(
            text = text.uppercase(),
            style = TaroTheme.text.storyMid,
            color = fg,
            modifier = Modifier
                .clip(RoundedCornerShape(TaroTheme.spacing.gap))
                .background(bg)
                .padding(horizontal = TaroTheme.spacing.loose, vertical = TaroTheme.spacing.tight),
        )
    }

@Composable
private fun ColumnScope.Fill() = Spacer(Modifier.weight(1f))

@Composable
private fun Marquee(text: String, bg: Color, fg: Color, tilt: Float, alpha: Float = 1f) {
    val animate = rememberAnimationsEnabled()
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .requiredWidth(maxWidth * WIDE_MARQUEE)
                .graphicsLayer {
                    rotationZ = tilt
                    this.alpha = alpha
                }
                .background(bg)
                .padding(vertical = TaroTheme.spacing.gap),
        ) {
            Text(
                text = text.repeat(4),
                style = TaroTheme.text.storyMid,
                color = fg,
                maxLines = 1,
                softWrap = false,
                modifier = if (animate) Modifier.basicMarquee(iterations = Int.MAX_VALUE, repeatDelayMillis = 0, initialDelayMillis = 0) else Modifier,
            )
        }
    }
}

private class Choice(val label: String, val reveal: String? = null)

@Composable
private fun ColumnScope.Choices(choices: List<Choice>, answer: Int, retry: Boolean, big: Boolean, onDone: () -> Unit) {
    val duo = LocalDuo.current
    val spacing = TaroTheme.spacing
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val wrong = remember { mutableStateListOf<Int>() }
    var picked by remember { mutableStateOf<Int?>(null) }
    val shift = with(LocalDensity.current) { spacing.tight.toPx() }
    val style = if (big) TaroTheme.text.storyGiant.let { it.copy(fontSize = it.fontSize * 0.6f, lineHeight = it.lineHeight * 0.6f) } else TaroTheme.text.storyHead
    Column(
        verticalArrangement = Arrangement.spacedBy(if (big) spacing.gap else spacing.tight),
        modifier = Modifier.weight(1f).fillMaxWidth().padding(top = spacing.wide),
    ) {
        choices.forEachIndexed { i, choice ->
            val shake = remember { Animatable(0f) }
            val right = picked == answer && i == answer
            val missed = i in wrong
            val lift by animateFloatAsState(if (right) 1f else 0f, TaroMotion.bouncy(), label = "pick")
            Reveal(300 + i * 60, modifier = Modifier.weight(1f).fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = shake.value * shift
                            scaleX = 1f + lift * 0.04f
                            scaleY = 1f + lift * 0.04f
                        }
                        .clip(RoundedCornerShape(spacing.loose))
                        .background(
                            when {
                                right -> duo.fg
                                missed -> duo.fg.copy(alpha = SOFT / 2)
                                else -> duo.fg.copy(alpha = SOFT)
                            }
                        )
                        .clickable(enabled = picked == null && !missed) {
                            if (i == answer) {
                                picked = i
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                scope.launch {
                                    delay(ANSWER_MS)
                                    onDone()
                                }
                            } else {
                                wrong += i
                                if (!retry) picked = i
                                haptics.performHapticFeedback(HapticFeedbackType.Reject)
                                scope.launch {
                                    shake.animateTo(0f, keyframes {
                                        durationMillis = 400
                                        -1f at 80
                                        1f at 160
                                        -1f at 240
                                        1f at 320
                                    })
                                    if (!retry) {
                                        delay(ANSWER_MS)
                                        onDone()
                                    }
                                }
                            }
                        }
                        .padding(horizontal = spacing.loose + spacing.tight),
                ) {
                    Text(
                        choice.label,
                        style = style,
                        color = if (right) duo.bg else duo.fg.copy(alpha = if (missed) 0.4f else 1f),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    if (missed && choice.reveal != null) Text(choice.reveal, style = TaroTheme.text.storyMid, color = duo.fg.copy(alpha = DIM), maxLines = 1)
                }
            }
        }
    }
}

private class TileSpec(val duo: Duo, val value: String, val line: String, val wide: Boolean = false)

@Composable
private fun ColumnScope.Bento(tiles: List<TileSpec>) {
    val spacing = TaroTheme.spacing
    val rows = buildList {
        var pending: TileSpec? = null
        tiles.forEach { tile ->
            when {
                tile.wide -> add(listOf(tile))
                pending == null -> pending = tile
                else -> {
                    add(listOf(pending!!, tile))
                    pending = null
                }
            }
        }
        pending?.let { add(listOf(it)) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.weight(1f).fillMaxWidth().padding(top = spacing.loose)) {
        var order = 0
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.weight(1f).fillMaxWidth()) {
                row.forEach { tile ->
                    Reveal(100 + order++ * 150, Entrance.POP, Modifier.weight(1f).fillMaxHeight()) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(spacing.loose * 1.25f))
                                .background(tile.duo.bg)
                                .padding(spacing.loose),
                        ) {
                            BasicText(tile.value, style = TaroTheme.text.storyTile.copy(color = tile.duo.fg), maxLines = 1, autoSize = TextAutoSize.StepBased(maxFontSize = TaroTheme.text.storyTile.fontSize))
                            Text(tile.line, style = TaroTheme.text.note, color = tile.duo.fg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowBar(text: String, fraction: Float, delay: Int, alpha: Float = 1f) {
    val duo = LocalDuo.current
    val enabled = rememberAnimationsEnabled()
    val grow = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(delay.toLong())
        grow.animateTo(1f, tween(1_200, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = Modifier
            .padding(top = TaroTheme.spacing.gap)
            .fillMaxWidth((fraction * grow.value).coerceIn(0.01f, 1f))
            .clip(RoundedCornerShape(TaroTheme.spacing.loose))
            .background(duo.fg.copy(alpha = alpha))
            .padding(horizontal = TaroTheme.spacing.loose, vertical = TaroTheme.spacing.gap),
    ) {
        Text(text, style = TaroTheme.text.storyMid, color = duo.bg, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun Dots(count: Int, columns: Int, delay: Int, color: Color = LocalDuo.current.fg, highlightFrom: Int = Int.MAX_VALUE, highlight: Color = THIS_PERIOD) {
    val enabled = rememberAnimationsEnabled()
    val rows = ceil(count / columns.toFloat()).toInt().coerceAtLeast(1)
    val total = (count * DOT_STEP_MS + 400).toFloat()
    val time = remember { Animatable(if (enabled) 0f else total) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(delay.toLong())
        time.animateTo(total, tween(total.toInt(), easing = LinearEasing))
    }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = TaroTheme.spacing.loose)
            .aspectRatio(columns / rows.toFloat()),
    ) {
        val cell = size.width / columns
        val radius = cell * 0.4f
        repeat(count) { i ->
            val local = ((time.value - i * DOT_STEP_MS) / 300f).coerceIn(0f, 1f)
            val pop = 1f + 2.70158f * (local - 1f) * (local - 1f) * (local - 1f) + 1.70158f * (local - 1f) * (local - 1f)
            if (local > 0f) {
                drawCircle(
                    color = if (i >= highlightFrom) highlight else color,
                    radius = radius * pop,
                    center = Offset(cell * (i % columns) + cell / 2, cell * (i / columns) + cell / 2),
                )
            }
        }
    }
}

@Composable
private fun CadenceGauge(steps: Int, labels: List<String>) {
    val duo = LocalDuo.current
    val spacing = TaroTheme.spacing
    val enabled = rememberAnimationsEnabled()
    val zone = (CADENCE_EDGES.indexOfLast { steps >= it }).coerceIn(0, CADENCE_ZONES - 1)
    val within = ((steps - CADENCE_EDGES[zone]).toFloat() / (CADENCE_EDGES[zone + 1] - CADENCE_EDGES[zone])).coerceIn(0f, 1f)
    val target = ((zone + within) / CADENCE_ZONES).coerceIn(0.02f, 0.98f)
    val needle = remember { Animatable(if (enabled) 0f else target) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(600)
        needle.animateTo(target, TaroMotion.bouncy())
    }
    Canvas(modifier = Modifier.fillMaxWidth().padding(top = spacing.wide).aspectRatio(7f)) {
        val bar = size.height * 0.34f
        val top = (size.height - bar) / 2
        val segment = size.width / CADENCE_ZONES
        repeat(CADENCE_ZONES) { i ->
            drawRect(duo.fg.copy(alpha = 0.18f + i * 0.2f), topLeft = Offset(segment * i, top), size = Size(segment, bar))
        }
        val x = size.width * needle.value
        val width = size.height * 0.11f
        drawRoundRect(duo.bg, topLeft = Offset(x - width, 0f), size = Size(width * 3, size.height), cornerRadius = CornerRadius(width))
        drawRoundRect(duo.fg, topLeft = Offset(x - width / 2, 0f), size = Size(width * 2, size.height), cornerRadius = CornerRadius(width))
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            Text(
                label,
                style = TaroTheme.text.note,
                color = duo.fg.copy(alpha = DIM),
                textAlign = when (i) {
                    0 -> TextAlign.Start
                    labels.lastIndex -> TextAlign.End
                    else -> TextAlign.Center
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private class AwardSpec(val category: String, val winner: String, val why: String)

@Composable
private fun Envelope(award: AwardSpec, delay: Int, startOpen: Boolean, onOpen: () -> Unit) {
    val duo = LocalDuo.current
    val spacing = TaroTheme.spacing
    val haptics = LocalHapticFeedback.current
    var open by remember { mutableStateOf(startOpen) }
    val angle by animateFloatAsState(if (open) 180f else 0f, TaroMotion.bouncy(), label = "envelope")
    val shape = RoundedCornerShape(spacing.loose)
    Reveal(delay, modifier = Modifier.padding(top = spacing.gap)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    rotationX = angle
                    cameraDistance = 14f * density
                }
                .clip(shape)
                .clickable {
                    if (!open) {
                        open = true
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onOpen()
                    }
                },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = if (angle <= 90f) 1f else 0f }
                    .background(duo.fg.copy(alpha = SOFT))
                    .drawBehind {
                        drawRoundRect(
                            duo.fg.copy(alpha = 0.45f),
                            cornerRadius = CornerRadius(spacing.loose.toPx()),
                            style = Stroke(spacing.hair.toPx() * 0.75f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(spacing.tight.toPx(), spacing.tight.toPx() / 2))),
                        )
                    }
                    .padding(spacing.loose),
            ) {
                Text(award.category.uppercase(), style = TaroTheme.text.storyKick, color = duo.fg)
                Text(stringResource(R.string.story_tap_open), style = TaroTheme.text.storyMid, color = duo.fg.copy(alpha = 0.6f))
                Text(" ", style = TaroTheme.text.note)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationX = 180f
                        alpha = if (angle > 90f) 1f else 0f
                    }
                    .background(duo.fg)
                    .padding(spacing.loose),
            ) {
                Text(award.category.uppercase(), style = TaroTheme.text.storyKick, color = duo.bg)
                Text(award.winner, style = TaroTheme.text.storyMid, color = duo.bg, maxLines = 1)
                Text(award.why, style = TaroTheme.text.note, color = duo.bg, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TraitRow(label: String, value: Int, delay: Int, ink: Color) {
    val enabled = rememberAnimationsEnabled()
    val grow = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(delay.toLong())
        grow.animateTo(1f, tween(1_000, easing = FastOutSlowInEasing))
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = TaroTheme.spacing.tight)) {
        Text(label, style = TaroTheme.text.note, color = ink, modifier = Modifier.weight(0.4f))
        Canvas(modifier = Modifier.weight(0.6f).aspectRatio(18f)) {
            val radius = CornerRadius(size.height / 2)
            drawRoundRect(ink.copy(alpha = 0.18f), cornerRadius = radius)
            drawRoundRect(ink, size = Size(size.width * value / 100f * grow.value, size.height), cornerRadius = radius)
        }
    }
}

@Composable
private fun BigProgress(fraction: Float, delay: Int) {
    val duo = LocalDuo.current
    val enabled = rememberAnimationsEnabled()
    val grow = remember { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (!enabled) return@LaunchedEffect
        delay(delay.toLong())
        grow.animateTo(1f, tween(2_000, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = Modifier.fillMaxWidth().padding(top = TaroTheme.spacing.wide).aspectRatio(4.5f)) {
        val radius = CornerRadius(size.height * 0.3f)
        drawRoundRect(duo.fg.copy(alpha = 0.15f), cornerRadius = radius)
        drawRoundRect(duo.fg, size = Size(size.width * fraction.coerceIn(0.03f, 1f) * grow.value, size.height), cornerRadius = radius)
    }
}

@Composable
private fun storySlides(report: PeriodReport, facts: StoryFacts, onDetails: () -> Unit, onShare: (ImageBitmap) -> Unit): List<StorySlide> {
    val context = LocalContext.current
    val inputs = report.inputs
    val days = inputs.days
    val week = inputs.period.kind == ReportKind.WEEK
    val word = stringResource(if (week) R.string.story_week else R.string.story_month)
    val zone = ZoneId.systemDefault()
    val slides = mutableListOf<StorySlide>()
    val title = storyTitle(report, facts, word)
    val awards = awards(facts, word, days.size)
    val persona = facts.persona
    val fastest = facts.fastest.maxByOrNull { it.steps }

    val marquee = listOfNotNull(
        periodLabel(inputs.period),
        stringResource(R.string.story_mq_steps, grouped(report.steps)),
        facts.favourite?.let { stringResource(R.string.story_mq_food, it.now, it.name) },
        facts.turn?.takeIf { it.times >= TWIST_TIMES }?.let { stringResource(R.string.story_mq_missing, dayName(it.low.date)) },
        fastest?.takeIf { it.steps >= BRISK }?.let { stringResource(R.string.story_mq_speed, it.steps) },
        facts.power?.let { stringResource(R.string.story_mq_hour, hourOf(it.hour)) },
    ).joinToString("  ·  ", postfix = "  ·  ").uppercase(Locale.getDefault())

    slides += StorySlide(TOMATO) { _, _ ->
        Fill()
        Marquee(marquee, LocalDuo.current.fg, LocalDuo.current.bg, MARQUEE_TILT)
        Fill()
        Head(stringResource(R.string.story_cover_title, word), delay = 200)
        val line = when (facts.plot) {
            Plot.COMEBACK -> facts.turn?.let { stringResource(R.string.story_cover_comeback, days.size, dayName(it.low.date)) }
            Plot.BREAKTHROUGH -> stringResource(R.string.story_cover_breakthrough, days.size)
            Plot.STEADY -> stringResource(R.string.story_cover_steady, days.size)
            Plot.REST -> stringResource(R.string.story_cover_rest, word)
            Plot.PLAIN -> null
        } ?: stringResource(R.string.story_cover_plain, days.size)
        Say(line, delay = 600)
        Marquee(marquee, Color.Transparent, LocalDuo.current.fg, -MARQUEE_TILT / 2, alpha = 0.25f)
        Fill()
        Say(stringResource(R.string.story_tap_start), delay = 900, alpha = DIM)
    }

    facts.biggest?.let { big ->
        val tracked = days.filter { it.steps > 0 }
        if (week && tracked.size >= 3) {
            slides += StorySlide(FOREST, gated = true) { answered, release ->
                var done by remember { mutableStateOf(answered) }
                Kick(stringResource(R.string.story_quick))
                Head(stringResource(R.string.story_quiz_day), delay = 120)
                if (!done) {
                    Choices(days.map { Choice(dayName(it.date), stringResource(R.string.story_mq_steps, grouped(it.steps))) }, days.indexOf(big.day), retry = true, big = false) {
                        done = true
                        release()
                    }
                    days.filter { it.complete && it.steps > 0 }.minByOrNull { it.steps }?.takeIf { it.date != big.day.date }?.let { low ->
                        Say(stringResource(R.string.story_quiz_hint, dayName(low.date)), delay = 500, alpha = DIM)
                    }
                } else {
                    Giant(grouped(big.day.steps), delay = 0)
                    Mid(dayName(big.day.date) + ".", delay = 300)
                    when {
                        big.rank == 1 -> Sticker(stringResource(R.string.story_rank_record), delay = 700)
                        big.rank <= MAX_RANK -> Sticker(stringResource(R.string.story_rank_n, big.rank), delay = 700)
                    }
                    if (big.rank > 1) {
                        val gap = big.record - big.day.steps
                        Say(
                            stringResource(R.string.story_quiz_off, grouped(gap), big.recordDate.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))) +
                                if (gap <= big.record * CLOSE_SHARE) " " + stringResource(R.string.story_quiz_close) else "",
                            delay = 1_000,
                        )
                    } else {
                        Say(stringResource(R.string.story_quiz_best), delay = 1_000)
                    }
                }
            }
        }
    }

    if (report.steps > 0) {
        slides += StorySlide(CREAM) { _, _ ->
            Kick(stringResource(R.string.story_numbers, word), color = CREAM_INK)
            val change = report.stepsChange
            val tiles = buildList {
                add(
                    TileSpec(
                        Duo(Color(0xFF14800F), CREAM.bg),
                        grouped(report.steps),
                        when {
                            change != null && change >= 5 -> stringResource(R.string.story_tile_steps_up, change, word)
                            change != null && change <= -5 -> stringResource(R.string.story_tile_steps_down, -change, word)
                            else -> stringResource(R.string.story_tile_steps)
                        },
                        wide = true,
                    )
                )
                val km = report.distanceM / 1_000
                add(
                    TileSpec(
                        TOMATO,
                        "${Util.formatMeasure((Util.metersToDistance(report.distanceM) * 10).roundToInt() / 10.0)} ${Util.distanceUnit()}",
                        when {
                            km >= MARATHON_KM -> stringResource(R.string.story_tile_km_marathons, Util.formatMeasure((km / MARATHON_KM * 10).roundToInt() / 10.0))
                            km >= MARATHON_KM * 0.7 -> stringResource(R.string.story_tile_km_nearly)
                            else -> stringResource(R.string.story_tile_km)
                        },
                    )
                )
                if (facts.numbers.sleepMinutes > 0) {
                    val nights = facts.bedtimes.count { it != null }.coerceAtLeast(1)
                    add(
                        TileSpec(
                            MIDNIGHT,
                            stringResource(R.string.story_hours_short, (facts.numbers.sleepMinutes / 60).toInt()),
                            if (facts.twin?.kind == TwinKind.SLEEP) stringResource(R.string.story_tile_sleep_hold) else stringResource(R.string.story_tile_sleep, minutesText((facts.numbers.sleepMinutes / nights).toInt())),
                        )
                    )
                }
                facts.numbers.eaten?.let { eaten ->
                    val favourite = facts.favourite
                    add(
                        TileSpec(
                            COCOA,
                            grouped(eaten),
                            if (favourite != null && favourite.now > 0) {
                                val share = eaten.takeIf { it > 0 }?.let { favourite.kcal / favourite.count * favourite.now }
                                stringResource(R.string.story_tile_eaten_fav, grouped(share ?: 0.0), favourite.name)
                            } else {
                                stringResource(R.string.story_tile_eaten)
                            },
                        )
                    )
                }
                facts.numbers.protein?.takeIf { it > 0 }?.let { protein ->
                    val proteinChange = facts.numbers.proteinChange
                    add(
                        TileSpec(
                            PLUM,
                            stringResource(R.string.food_grams_value, grouped(protein)),
                            when {
                                proteinChange != null && proteinChange >= 10 -> stringResource(R.string.story_tile_protein_up, proteinChange)
                                proteinChange != null && proteinChange <= -10 -> stringResource(R.string.story_tile_protein_down, -proteinChange)
                                else -> stringResource(R.string.story_tile_protein)
                            },
                        )
                    )
                }
                if (facts.numbers.walkingMinutes > 0) {
                    add(TileSpec(TEAL, minutesText(facts.numbers.walkingMinutes), stringResource(R.string.story_tile_walking, days.size * 24), wide = true))
                }
            }
            Bento(tiles)
        }
    }

    val twin = facts.twin
    val odd = facts.odd
    val still = facts.still
    val late = facts.late
    if (twin != null || odd != null || still != null || late != null) {
        slides += StorySlide(MIDNIGHT) { _, _ ->
            Kick(stringResource(R.string.story_odd_kick))
            when {
                twin != null -> {
                    val sleep = twin.kind == TwinKind.SLEEP
                    val now = if (sleep) minutesText(twin.now.toInt()) else grouped(twin.now)
                    val before = if (sleep) minutesText(twin.before.toInt()) else grouped(twin.before)
                    val wide = maxOf(twin.now, twin.before).toFloat()
                    Say(stringResource(if (sleep) R.string.story_twin_slept else R.string.story_twin_walked, word), delay = 150)
                    GrowBar(now, twin.now / wide, delay = 400)
                    Say(stringResource(if (sleep) R.string.story_twin_slept_before else R.string.story_twin_walked_before, word), delay = 900)
                    GrowBar(before, twin.before / wide, delay = 1_100, alpha = 0.55f)
                    if (sleep && week && facts.bedtimes.isNotEmpty()) {
                        Say(stringResource(R.string.story_twin_bedtimes), delay = 1_500, alpha = DIM)
                        val none = stringResource(R.string.story_none)
                        Mid(facts.bedtimes.joinToString("  ·  ") { minute -> minute?.let { clockText(context, it) } ?: none }, delay = 1_700)
                    }
                    Fill()
                    val gap = abs(twin.now - twin.before)
                    Head(
                        if (sleep) pluralStringResource(R.plurals.story_twin_minutes, gap.toInt(), gap.toInt()) else pluralStringResource(R.plurals.story_twin_steps, gap.toInt(), grouped(gap)),
                        delay = 1_900,
                        entrance = Entrance.SLAM,
                    )
                    Say(stringResource(if (sleep) R.string.story_twin_sleep_aside else R.string.story_twin_steps_aside), delay = 2_300)
                }
                odd != null -> {
                    val at = Instant.ofEpochMilli(odd.at).atZone(zone)
                    Fill()
                    Giant(clockOf(at.hour * 60 + at.minute), delay = 200)
                    Mid(stringResource(R.string.story_odd_line, grouped(odd.steps), dayName(at.toLocalDate())), delay = 500)
                    Fill()
                    Head(stringResource(R.string.story_odd_stamp), delay = 1_200, entrance = Entrance.SLAM)
                    Say(stringResource(R.string.story_odd_aside), delay = 1_600)
                }
                still != null -> {
                    Fill()
                    Giant(minutesText(still.minutes), delay = 200)
                    Mid(stringResource(R.string.story_still_line, dayName(still.date)), delay = 500)
                    Fill()
                    Head(stringResource(R.string.story_still_stamp), delay = 1_200, entrance = Entrance.SLAM)
                    Say(stringResource(R.string.story_still_aside), delay = 1_600)
                }
                late != null -> {
                    Fill()
                    Giant(clockOf(late.minute), delay = 200)
                    Mid(stringResource(R.string.story_late_line, clockOf(late.usualMinute), dayName(late.date)), delay = 500)
                    Fill()
                    Head(stringResource(R.string.story_late_stamp), delay = 1_200, entrance = Entrance.SLAM)
                }
            }
        }
    }

    facts.power?.takeIf { it.steps >= POWER_STEPS }?.let { power ->
        slides += StorySlide(COCOA) { _, _ ->
            Kick(stringResource(R.string.story_power_kick))
            Giant(hourOf(power.hour), delay = 0)
            val stepsText = stringResource(R.string.story_mq_steps, grouped(power.steps))
            val tail = when (power.hour) {
                in 18..23, in 0..2 -> " " + stringResource(R.string.story_power_night)
                in 4..8 -> " " + stringResource(R.string.story_power_morning)
                in 11..14 -> " " + stringResource(R.string.story_power_lunch)
                else -> ""
            }
            Say(stringResource(R.string.story_power_line, stepsText, hourOf(power.hour), hourOf(power.hour + 1), word) + tail, delay = 400, bold = stepsText)
            val unit = power.unit
            if (unit != null) {
                Dots(power.units.coerceAtMost(MAX_DOTS), 9, delay = 900)
                Fill()
                Mid(stringResource(R.string.story_power_units, power.units, dayName(unit.date)), delay = 900 + power.units.coerceAtMost(MAX_DOTS) * DOT_STEP_MS + 300)
            } else {
                Fill()
                Mid(stringResource(R.string.story_power_share, power.steps * 100 / report.steps.coerceAtLeast(1), word), delay = 1_200)
            }
        }
    }

    fastest?.takeIf { it.steps >= BRISK }?.let { top ->
        slides += StorySlide(PLUM) { _, _ ->
            val at = Instant.ofEpochMilli(top.at).atZone(zone)
            Kick(stringResource(R.string.story_speed_kick, dayName(at.toLocalDate()), clockOf(at.hour * 60 + at.minute)))
            Giant(top.steps.toString(), delay = 0)
            Mid(stringResource(R.string.story_speed_unit), delay = 300)
            CadenceGauge(
                top.steps,
                listOf(stringResource(R.string.story_zone_stroll), stringResource(R.string.story_zone_walk), stringResource(R.string.story_zone_brisk), stringResource(R.string.story_zone_run)),
            )
            if (facts.fastest.size > 1) {
                Say(pluralStringResource(R.plurals.story_speed_top, facts.fastest.size, facts.fastest.size), delay = 1_400, alpha = DIM, modifier = Modifier.padding(top = TaroTheme.spacing.loose))
                facts.fastest.forEachIndexed { i, minute -> FastRow(minute, zone, 1_600 + i * 200) }
            }
            Fill()
            Say(stringResource(if (top.steps >= RUN) R.string.story_speed_run else R.string.story_speed_brisk), delay = 2_000)
            Sticker(stringResource(R.string.story_speed_sticker), delay = 2_400)
        }
    }

    facts.turn?.takeIf { it.times >= TWIST_TIMES }?.let { turn ->
        slides += StorySlide(COUCH, bleed = true) { _, _ ->
            val spacing = TaroTheme.spacing
            val pad = Modifier.padding(horizontal = spacing.loose + spacing.tight)
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f).fillMaxWidth().then(pad)) {
                        Kick(dayName(turn.low.date))
                        Giant(grouped(turn.low.steps), delay = 200, entrance = Entrance.RISE, scale = 0.8f)
                        val napped = facts.nap?.takeIf { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() == turn.low.date }
                        Say(
                            if (napped != null) stringResource(R.string.story_twist_nap, minutesText(napped.minutes)) else stringResource(R.string.story_twist_low),
                            delay = 400,
                        )
                    }
                    CompositionLocalProvider(LocalDuo provides FOREST) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(FOREST.bg)
                                .navigationBarsPadding()
                                .then(pad),
                        ) {
                            Kick(dayName(turn.turn.date), delay = 900)
                            Giant(grouped(turn.turn.steps), delay = 1_100, scale = 0.8f)
                            Say(
                                when {
                                    turn.madeUp -> stringResource(R.string.story_twist_made, dayName(turn.low.date), grouped(turn.turn.steps - turn.turn.goal - (turn.low.goal - turn.low.steps)))
                                    turn.rested -> stringResource(R.string.story_twist_rest, dayName(turn.low.date))
                                    else -> stringResource(R.string.story_twist_back)
                                },
                                delay = 1_400,
                            )
                        }
                    }
                }
                Sticker(
                    stringResource(R.string.story_times, turn.times),
                    delay = 1_800,
                    bg = TOMATO.bg,
                    fg = TOMATO.fg,
                    tilt = 8f,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = spacing.loose),
                )
            }
        }
    }

    if (awards.size >= 2) {
        slides += StorySlide(INK, gated = true) { answered, release ->
            var opened by remember { mutableIntStateOf(0) }
            Kick(stringResource(R.string.story_awards_kick))
            Head(stringResource(R.string.story_awards_title), delay = 100)
            awards.forEachIndexed { i, award ->
                Envelope(award, 300 + i * 150, answered) {
                    opened++
                    if (opened == awards.size) release()
                }
            }
        }
    }

    facts.favourite?.let { fav ->
        slides += StorySlide(COCOA) { _, _ ->
            Kick(stringResource(R.string.story_award_lifetime))
            Head(fav.name, delay = 100)
            Reveal(200, modifier = Modifier.padding(top = TaroTheme.spacing.loose)) { CountGiant(fav.kcal.roundToLong(), delay = 200, scale = 0.8f) }
            Mid(stringResource(R.string.story_fav_since, fav.since.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))), delay = 400)
            val perDot = if (fav.count > MAX_DOTS) ceil(fav.count / MAX_DOTS.toFloat()).toInt() else 1
            Dots(ceil(fav.count / perDot.toFloat()).toInt(), 10, delay = 600, highlightFrom = (fav.count - fav.now) / perDot)
            Say(
                if (perDot == 1) stringResource(R.string.story_fav_dots, word) else stringResource(R.string.story_fav_dots_many, perDot, word),
                delay = 2_100,
                alpha = DIM,
            )
            Fill()
            val dayKcal = facts.numbers.eaten?.let { it / days.size }?.takeIf { it > 0 } ?: FAVOURITE_DAY_KCAL
            val fed = (fav.kcal / dayKcal).roundToInt()
            val span = if (fed >= WEEK_DAYS) pluralStringResource(R.plurals.story_weeks_of_you, fed / 7, fed / 7) else pluralStringResource(R.plurals.story_days_of_you, fed, fed)
            Say(stringResource(R.string.story_fav_line, fav.count, span), delay = 2_300, bold = span)
            Sticker(stringResource(R.string.story_fav_next, fav.next - fav.count, fav.next), delay = 2_700, tilt = 5f)
        }
    }

    persona?.let { type ->
        slides += StorySlide(TEAL) { _, _ ->
            Kick(stringResource(R.string.story_type_kick, word))
            PersonaCard(type.type, persona, facts, word, delay = 300)
            facts.habit?.let { habit ->
                Say(stringResource(R.string.story_spotted, hourOf(habit.hour)), delay = 1_400, alpha = DIM, modifier = Modifier.padding(top = TaroTheme.spacing.gap))
                Mid(habit.starts.joinToString("  ·  ") { clockText(context, Instant.ofEpochMilli(it).atZone(zone).let { at -> at.hour * 60 + at.minute }) }, delay = 1_600)
            }
            Fill()
            val others = StoryType.entries.filter { it != type.type }.map { stringResource(typeName(it)) }
            Say(stringResource(R.string.story_type_others, others.joinToString(", ")), delay = 1_900, alpha = DIM)
        }
    }

    facts.versus?.let { versus ->
        slides += StorySlide(TOMATO, gated = true) { answered, release ->
            var done by remember { mutableStateOf(answered) }
            val more = versus.now > versus.before
            Kick(stringResource(R.string.story_hl_kick))
            Head(stringResource(if (versus.kind == VersusKind.EATEN) R.string.story_hl_eaten else R.string.story_hl_steps, word), delay = 100)
            if (!done) {
                Choices(
                    listOf(Choice(stringResource(R.string.story_more), stringResource(R.string.story_nope)), Choice(stringResource(R.string.story_less), stringResource(R.string.story_nope))),
                    if (more) 0 else 1,
                    retry = false,
                    big = true,
                ) {
                    done = true
                    release()
                }
            } else {
                val gap = abs(versus.now - versus.before)
                Giant((if (more) "+" else "-") + grouped(gap), delay = 0, scale = 0.8f)
                if (versus.kind == VersusKind.EATEN) {
                    Mid(stringResource(if (more) R.string.story_hl_eaten_more else R.string.story_hl_eaten_less, grouped(versus.now)), delay = 300)
                    val protein = versus.proteinChange
                    when {
                        more && protein != null && protein >= 10 -> Say(stringResource(R.string.story_hl_gains, protein), delay = 700, bold = "$protein%")
                        !more -> Say(stringResource(R.string.story_hl_lighter, word), delay = 700)
                        else -> Say(stringResource(R.string.story_hl_more_line), delay = 700)
                    }
                    if (protein != null && protein >= 10) Sticker(stringResource(R.string.story_hl_protein_sticker, protein), delay = 1_100)
                } else {
                    Mid(stringResource(if (more) R.string.story_hl_steps_more else R.string.story_hl_steps_less, word), delay = 300)
                    Say(stringResource(if (more) R.string.story_hl_steps_more_line else R.string.story_hl_steps_less_line, word), delay = 700)
                }
            }
        }
    }

    facts.milestone?.let { goal ->
        slides += StorySlide(CREAM) { _, _ ->
            Kick(stringResource(R.string.story_next_kick, word), color = CREAM_INK)
            Fill()
            Giant(grouped(goal.left), delay = 0)
            val target = when (goal.target) {
                HALF_MILLION -> stringResource(R.string.story_half_million)
                MILLION -> stringResource(R.string.story_million)
                else -> grouped(goal.target)
            }
            Mid(stringResource(R.string.story_next_to, target), delay = 300, color = CREAM_INK)
            BigProgress(goal.total.toFloat() / goal.target, delay = 500)
            Say(stringResource(R.string.story_next_so_far, grouped(goal.total)), delay = 600, color = CREAM_INK, alpha = DIM)
            Fill()
            val best = goal.best
            val line = if (best != null && goal.left <= best.steps) {
                stringResource(R.string.story_next_one, dayName(best.date))
            } else {
                val usual = goal.average.coerceAtLeast(1)
                pluralStringResource(R.plurals.story_next_days, ceil(goal.left / usual.toDouble()).toInt(), ceil(goal.left / usual.toDouble()).toInt())
            }
            val move = stringResource(R.string.story_next_move)
            Head("$line\n$move", delay = 1_800, color = CREAM_INK, accent = move, accentColor = CREAM.fg)
        }
    }

    slides += StorySlide(PLUM) { _, _ ->
        ShareCard(report, facts, title, awards, word, fastest, onShare, onDetails)
    }
    return slides
}

@Composable
private fun FastRow(minute: FastMinute, zone: ZoneId, delay: Int) {
    val duo = LocalDuo.current
    val at = Instant.ofEpochMilli(minute.at).atZone(zone)
    val line = TaroTheme.spacing.hair
    Reveal(delay) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(duo.fg.copy(alpha = 0.25f), Offset(0f, size.height), Offset(size.width, size.height), line.toPx() / 2)
                }
                .padding(vertical = TaroTheme.spacing.tight),
        ) {
            Text(clockOf(at.hour * 60 + at.minute), style = TaroTheme.text.storyMid, color = duo.fg, modifier = Modifier.weight(1f))
            Text(minute.steps.toString(), style = TaroTheme.text.storyMid, color = duo.fg)
        }
    }
}

@Composable
private fun PersonaCard(type: StoryType, persona: com.prateek.taro.report.Persona, facts: StoryFacts, word: String, delay: Int) {
    val duo = LocalDuo.current
    val spacing = TaroTheme.spacing
    val ink = duo.bg
    val zone = ZoneId.systemDefault()
    Reveal(delay, Entrance.POP, Modifier.padding(top = spacing.loose)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(spacing.wide))
                .background(duo.fg)
                .padding(spacing.loose),
        ) {
            Text(stringResource(R.string.story_type_card, word).uppercase(), style = TaroTheme.text.storyKick, color = ink.copy(alpha = DIM))
            Text(stringResource(typeName(type)), style = TaroTheme.text.storyHead, color = ink, modifier = Modifier.padding(top = spacing.tight))
            Text(stringResource(typeLine(type)), style = TaroTheme.text.note, color = ink, modifier = Modifier.padding(top = spacing.tight))
            Column(modifier = Modifier.padding(top = spacing.tight)) {
                TraitRow(stringResource(R.string.story_trait_night), persona.nightOwl, delay + 400, ink)
                TraitRow(stringResource(R.string.story_trait_routine), persona.routine, delay + 550, ink)
                TraitRow(stringResource(R.string.story_trait_weekend), persona.weekend, delay + 700, ink)
                persona.loyalty?.let { TraitRow(stringResource(R.string.story_trait_loyalty), it, delay + 850, ink) }
            }
            val moves = buildList {
                facts.habit?.let { add(stringResource(R.string.story_move_signature) to stringResource(R.string.story_move_walk, hourOf(it.hour))) }
                persona.weakDay?.let { add(stringResource(R.string.story_move_weak) to stringResource(R.string.story_move_days, it.getDisplayName(DayStyle.FULL, Locale.getDefault()))) }
                persona.secret?.takeIf { secret -> facts.habit == null || Instant.ofEpochMilli(secret.start).atZone(zone).let { (it.hour * 60 + it.minute + 30) / 60 % 24 } != facts.habit.hour }?.let { secret ->
                    val at = Instant.ofEpochMilli(secret.start).atZone(zone)
                    add(stringResource(R.string.story_move_secret) to stringResource(R.string.story_move_secret_walk, clockOf(at.hour * 60 + at.minute), minutesText(secret.minutes)))
                }
            }
            moves.forEach { (label, value) ->
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append(label) }
                        append("  ")
                        append(value)
                    },
                    style = TaroTheme.text.note,
                    color = ink,
                    modifier = Modifier.padding(top = spacing.tight),
                )
            }
        }
    }
}

private fun typeName(type: StoryType) = when (type) {
    StoryType.NIGHT_SHIFT -> R.string.story_type_night
    StoryType.EARLY_BIRD -> R.string.story_type_early
    StoryType.LUNCH_WALKER -> R.string.story_type_lunch
    StoryType.WEEKEND_WARRIOR -> R.string.story_type_weekend
    StoryType.CLOCKWORK -> R.string.story_type_clockwork
    StoryType.WANDERER -> R.string.story_type_wanderer
}

private fun typeLine(type: StoryType) = when (type) {
    StoryType.NIGHT_SHIFT -> R.string.story_type_night_line
    StoryType.EARLY_BIRD -> R.string.story_type_early_line
    StoryType.LUNCH_WALKER -> R.string.story_type_lunch_line
    StoryType.WEEKEND_WARRIOR -> R.string.story_type_weekend_line
    StoryType.CLOCKWORK -> R.string.story_type_clockwork_line
    StoryType.WANDERER -> R.string.story_type_wanderer_line
}

@Composable
private fun awards(facts: StoryFacts, word: String, days: Int): List<AwardSpec> {
    val zone = ZoneId.systemDefault()
    fun at(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone)
    return buildList {
        facts.habit?.let {
            add(AwardSpec(stringResource(R.string.story_award_reliable), stringResource(R.string.story_award_reliable_win, hourOf(it.hour)), stringResource(R.string.story_award_reliable_why, it.days, days)))
        }
        val turn = facts.turn
        val big = facts.biggest
        if (turn != null && turn.times >= TWIST_TIMES) {
            add(AwardSpec(stringResource(R.string.story_award_comeback, word), dayName(turn.turn.date), stringResource(R.string.story_award_comeback_why, grouped(turn.low.steps), grouped(turn.turn.steps))))
        } else if (big != null) {
            add(AwardSpec(stringResource(R.string.story_award_biggest), dayName(big.day.date), stringResource(R.string.story_mq_steps, grouped(big.day.steps))))
        }
        facts.nap?.let { nap ->
            val from = at(nap.start)
            val to = at(nap.end)
            add(
                AwardSpec(
                    stringResource(R.string.story_award_nap),
                    stringResource(R.string.story_award_nap_win, dayName(from.toLocalDate()), clockOf(from.hour * 60 + from.minute), clockOf(to.hour * 60 + to.minute)),
                    stringResource(R.string.story_award_nap_why, minutesText(nap.minutes)),
                )
            )
        }
        facts.favourite?.let { add(AwardSpec(stringResource(R.string.story_award_lifetime), it.name, stringResource(R.string.story_award_lifetime_why, it.count))) }
        facts.earliest?.let { walk ->
            val start = at(walk.start)
            add(AwardSpec(stringResource(R.string.story_award_early), stringResource(R.string.story_award_when, dayName(start.toLocalDate()), clockOf(start.hour * 60 + start.minute)), stringResource(R.string.story_award_early_why)))
        }
        facts.late?.let { late ->
            add(AwardSpec(stringResource(R.string.story_award_owl), stringResource(R.string.story_award_when, dayName(late.date), clockOf(late.minute)), stringResource(R.string.story_award_owl_why)))
        }
    }.take(4)
}

@Composable
private fun storyTitle(report: PeriodReport, facts: StoryFacts, word: String): String = when (facts.plot) {
    Plot.COMEBACK -> facts.turn?.let { stringResource(R.string.story_name_comeback, dayName(it.low.date)) }
    Plot.BREAKTHROUGH -> facts.biggest?.let { stringResource(R.string.story_name_breakthrough, dayName(it.day.date)) }
    Plot.STEADY -> stringResource(R.string.story_name_steady, report.goalDays)
    Plot.REST -> stringResource(R.string.story_name_rest, word)
    Plot.PLAIN -> null
} ?: stringResource(R.string.story_name_plain, word)

@Composable
private fun ColumnScope.ShareCard(
    report: PeriodReport,
    facts: StoryFacts,
    title: String,
    awards: List<AwardSpec>,
    word: String,
    fastest: FastMinute?,
    onShare: (ImageBitmap) -> Unit,
    onDetails: () -> Unit,
) {
    val duo = LocalDuo.current
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    val period = report.inputs.period
    val ink = duo.bg
    val stats = buildList {
        add(grouped(report.steps) to stringResource(R.string.story_card_steps))
        facts.power?.let { add(hourOf(it.hour) to stringResource(R.string.story_card_hour)) }
        fastest?.takeIf { it.steps >= BRISK }?.let { add(it.steps.toString() to stringResource(R.string.story_card_speed)) }
        val turn = facts.turn
        if (turn != null && turn.times >= TWIST_TIMES) {
            add(stringResource(R.string.story_times, turn.times) to stringResource(R.string.story_card_twist, shortDay(turn.low.date), shortDay(turn.turn.date)))
        } else {
            facts.biggest?.let { add(grouped(it.day.steps) to stringResource(R.string.story_card_best)) }
        }
    }.take(4)
    Reveal(100, Entrance.POP, Modifier.weight(1f).padding(top = spacing.loose)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
                .clip(RoundedCornerShape(spacing.wide))
                .background(duo.fg)
                .padding(spacing.loose + spacing.tight),
        ) {
            Row {
                Text(stringResource(R.string.story_card_head, periodLabel(period)).uppercase(), style = type.storyKick, color = ink.copy(alpha = DIM), modifier = Modifier.weight(1f))
                if (period.kind == ReportKind.WEEK) Text(stringResource(R.string.story_card_week, period.start.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)).uppercase(), style = type.storyKick, color = ink.copy(alpha = DIM))
            }
            facts.persona?.let { Text(stringResource(typeName(it.type)), style = type.storyHead, color = ink, modifier = Modifier.padding(top = spacing.gap)) }
            stats.chunked(2).forEach { row ->
                Row(modifier = Modifier.padding(top = spacing.loose)) {
                    row.forEach { (value, label) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(value, style = type.storyTile, color = ink, maxLines = 1)
                            Text(label, style = type.note, color = ink)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Column(modifier = Modifier.padding(top = spacing.loose)) {
                awards.take(3).forEach { award ->
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Black)) { append(award.category) }
                            append("  ")
                            append(award.winner)
                        },
                        style = type.note,
                        color = ink,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Text(title, style = type.storyMid, color = ink)
            Text(stringResource(R.string.story_card_sign, stringResource(R.string.app_name)), style = type.note, color = ink.copy(alpha = DIM), modifier = Modifier.padding(top = spacing.tight))
        }
    }
    Reveal(700, modifier = Modifier.padding(top = spacing.loose)) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.loose * 2, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            LinkText(stringResource(R.string.story_share), duo.fg, onClick = { scope.launch { onShare(layer.toImageBitmap()) } })
            LinkText(stringResource(R.string.story_full), duo.fg.copy(alpha = DIM), onClick = onDetails)
        }
    }
}

private fun grouped(value: Number): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value.toDouble().roundToLong())

@Composable
private fun clockOf(minuteOfDay: Int): String = clockText(LocalContext.current, minuteOfDay)

private fun clockText(context: Context, minuteOfDay: Int): String {
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

private fun shortDay(date: LocalDate) = date.dayOfWeek.getDisplayName(DayStyle.SHORT, Locale.getDefault())

@Composable
private fun minutesText(minutes: Int): String = stringResource(R.string.sleep_duration, minutes / 60, minutes % 60)

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
