package com.prateek.taro.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import com.prateek.taro.leaderboard.Board
import com.prateek.taro.leaderboard.BoardData
import com.prateek.taro.leaderboard.BoardRange
import com.prateek.taro.leaderboard.Entry
import com.prateek.taro.leaderboard.LeaderboardData
import com.prateek.taro.leaderboard.Leaderboards
import com.prateek.taro.leaderboard.Pin
import com.prateek.taro.leaderboard.Standing
import com.prateek.taro.ui.components.BentoNote
import com.prateek.taro.ui.components.BentoTile
import com.prateek.taro.ui.components.BentoValue
import com.prateek.taro.ui.components.Bone
import com.prateek.taro.ui.components.BoneTilePair
import com.prateek.taro.ui.components.IconDisc
import com.prateek.taro.ui.components.LinkText
import com.prateek.taro.ui.components.ReorderColumn
import com.prateek.taro.ui.components.RowDivider
import com.prateek.taro.ui.components.SectionLabel
import com.prateek.taro.ui.components.Skeleton
import com.prateek.taro.ui.components.TaroDialog
import com.prateek.taro.ui.components.TaroSwitch
import com.prateek.taro.ui.components.TextTabs
import com.prateek.taro.ui.components.rememberBarGrowth
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.ui.theme.rememberAnimationsEnabled
import com.prateek.taro.util.AppPreferences
import com.prateek.taro.util.Database
import com.prateek.taro.util.BonusKind
import com.prateek.taro.util.DayScore
import com.prateek.taro.util.DayScores
import com.prateek.taro.util.Util
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle as DayStyle
import java.util.Locale
import kotlin.math.roundToInt

object LeaderboardScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        LeaderboardContent(onOpen = { board, range -> navigator.push(BoardScreen(board, range)) })
    }
}

class BoardScreen(private val board: Board, private val range: BoardRange) : Screen {
    @Composable
    override fun Content() {
        BoardContent(board, range)
    }
}

private class StagePalette(val top: Color, val middle: Color, val base: Color, val floor: Color, val footer: Color, val dim: Color)

private val STAGES = mapOf(
    Board.SCORE to StagePalette(Color(0xFF5C1A24), Color(0xFF2E0C12), Color(0xFF150609), Color(0xFF6E2533), Color(0xFF3A1219), Color(0xFFEAAAB4)),
    Board.STEPS to StagePalette(Color(0xFF1E4D21), Color(0xFF0F2711), Color(0xFF071308), Color(0xFF2D6230), Color(0xFF163519), Color(0xFFB3DFB0)),
    Board.DISTANCE to StagePalette(Color(0xFF145045), Color(0xFF0A2823), Color(0xFF051311), Color(0xFF1F6355), Color(0xFF10352E), Color(0xFFAEE3D6)),
    Board.MOVING to StagePalette(Color(0xFF5E2E14), Color(0xFF2F160A), Color(0xFF160A05), Color(0xFF713E20), Color(0xFF3B1E10), Color(0xFFEFC2A5)),
    Board.WALK to StagePalette(Color(0xFF424C17), Color(0xFF21260B), Color(0xFF101205), Color(0xFF55611F), Color(0xFF2B310F), Color(0xFFD6DDA9)),
    Board.SPEED to StagePalette(Color(0xFF48215E), Color(0xFF23102F), Color(0xFF110817), Color(0xFF5B3271), Color(0xFF301941), Color(0xFFDABBEC)),
    Board.EARLY to StagePalette(Color(0xFF5C4612), Color(0xFF2F2409), Color(0xFF161104), Color(0xFF705822), Color(0xFF3B2E10), Color(0xFFEFDEA9)),
    Board.SLEEP to StagePalette(Color(0xFF232A5E), Color(0xFF121637), Color(0xFF090B1C), Color(0xFF323A72), Color(0xFF1A1F45), Color(0xFFAAB3E6)),
    Board.PROTEIN to StagePalette(Color(0xFF13405E), Color(0xFF0A2030), Color(0xFF050F17), Color(0xFF1C5070), Color(0xFF0F2C3F), Color(0xFFA9D5ED)),
)

private val MEDALS = listOf(
    Color(0xFFFFE082) to Color(0xFFE0A92C),
    Color(0xFFE3E7EE) to Color(0xFF9EA6B4),
    Color(0xFFF0B88E) to Color(0xFFB86E3F),
)
private val BLOCK_INK = Color(0xFF151515)
private val SPOT_GOLD = Color(0xFFFFE082)
private val ON_STAGE = Color(0xFFF0F0F0)

private val PODIUM_ORDER = listOf(1, 0, 2)
private val BLOCK_SHARES = listOf(0.68f, 0.52f, 0.4f)
private const val BLOCKS_ASPECT = 1.7f
private const val FOLDS = 26
private const val FOLD_REACH = 0.62f
private const val FOLD_LIGHT = 0.035f
private const val FOLD_DARK = 0.16f
private const val SCALLOPS = 12
private const val SCALLOP_DROP = 0.45f
private const val BLOCK_DEPTH = 0.45f
private const val EDGE_LIGHT = 0.4f
private const val CONTACT_ALPHA = 0.35f
private const val CONTACT_REACH = 0.7f
private const val SHADE_ALPHA = 0.2f
private const val TRACK_ALPHA = 0.14f
private const val RANK_SHARE = 1.2f
private const val STAGE_MS = 1_300
private const val BLOCK_MS = 650f
private val BLOCK_DELAYS = listOf(450f, 250f, 50f)
private const val VALUE_AFTER_MS = 380f
private const val VALUE_MS = 350f
private const val MINI_BARS = 10
private const val MINI_ASPECT = 5f
private const val MINI_DIM = 0.35f
private const val BASE_ALPHA = 0.55f
private const val RECENT_DAYS = 7L
private const val COLUMNS = 2

@StringRes
private fun boardName(board: Board): Int = when (board) {
    Board.SCORE -> R.string.lb_score
    Board.STEPS -> R.string.lb_steps
    Board.DISTANCE -> R.string.lb_distance
    Board.MOVING -> R.string.lb_moving
    Board.WALK -> R.string.lb_walk
    Board.SPEED -> R.string.lb_speed
    Board.EARLY -> R.string.lb_early
    Board.SLEEP -> R.string.lb_sleep
    Board.PROTEIN -> R.string.lb_protein
}

@DrawableRes
private fun boardIcon(board: Board): Int = when (board) {
    Board.SCORE -> R.drawable.ic_badge_star
    Board.STEPS -> R.drawable.ic_steps
    Board.DISTANCE -> R.drawable.ic_distance
    Board.MOVING -> R.drawable.ic_calories
    Board.WALK -> R.drawable.ic_schedule
    Board.SPEED -> R.drawable.ic_frequency
    Board.EARLY -> R.drawable.ic_badge_early
    Board.SLEEP -> R.drawable.ic_sleep
    Board.PROTEIN -> R.drawable.ic_badge_bolt
}

@Composable
private fun boardColor(board: Board): Color {
    val colors = TaroTheme.colors
    return when (board) {
        Board.SCORE -> colors.red
        Board.STEPS, Board.WALK -> colors.goal
        Board.DISTANCE -> colors.mint
        Board.MOVING -> colors.flame
        Board.SPEED -> colors.purple
        Board.EARLY -> colors.special
        Board.SLEEP -> colors.sleep
        Board.PROTEIN -> PROTEIN_BLUE
    }
}

@Composable
private fun boardValue(board: Board, value: Double): String = when (board) {
    Board.DISTANCE -> String.format(Locale.getDefault(), "%.1f", Util.metersToDistance(value))
    Board.WALK, Board.SLEEP -> minutesText(value.toLong())
    else -> Util.formatSteps(value.roundToInt())
}

@Composable
private fun boardUnit(board: Board): String? = when (board) {
    Board.STEPS -> stringResource(R.string.lb_unit_steps)
    Board.DISTANCE -> Util.distanceUnit()
    Board.MOVING -> stringResource(R.string.lb_unit_kcal)
    Board.SPEED -> stringResource(R.string.lb_unit_speed)
    Board.EARLY -> stringResource(R.string.lb_unit_early, remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(LocalTime.of(DayScores.EARLY_HOUR, 0)) })
    Board.PROTEIN -> stringResource(R.string.lb_unit_grams)
    else -> null
}

@StringRes
private fun rangeName(range: BoardRange): Int = when (range) {
    BoardRange.ALL -> R.string.lb_range_all
    BoardRange.MONTH -> R.string.lb_range_month
    BoardRange.WEEKS -> R.string.lb_range_weeks
    BoardRange.WEEKDAY -> R.string.lb_range_weekday
}

private val DAY_FORMAT: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private val SHORT_FORMAT: DateTimeFormatter get() = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())

@Composable
private fun entryLabel(entry: Entry, range: BoardRange): String =
    if (range == BoardRange.WEEKS) stringResource(R.string.lb_week_of, entry.date.format(SHORT_FORMAT)) else entry.date.format(DAY_FORMAT)

@Composable
private fun LeaderboardContent(onOpen: (Board, BoardRange) -> Unit) {
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors
    val context = LocalContext.current
    val tick by Database.changes.collectAsStateWithLifecycle()
    val loaded = rememberInBackground(tick) { LeaderboardData.load(context) }
    var shown by remember { mutableStateOf<Loaded<BoardData?>?>(null) }
    if (loaded != null) shown = loaded
    var saved by remember { mutableStateOf(AppPreferences.leaderboardPins) }
    val pins = shown?.value?.let { data -> saved.filter { data.has(it.board) }.ifEmpty { saved } } ?: saved
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    val pin = pins.firstOrNull { it.board.key == selected } ?: pins.first()
    val color = boardColor(pin.board)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(back = true, title = stringResource(R.string.lb_title))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.loose),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = spacing.loose),
            ) {
                TextTabs(pins.map { it to stringResource(boardName(it.board)) }, pin, { selected = it.board.key }, color)
                LinkText(stringResource(R.string.lb_edit), colors.accent, onClick = { editing = true }, icon = R.drawable.ic_edit, modifier = Modifier.padding(start = spacing.gap))
            }
            val data = shown?.value
            when {
                shown == null -> StageBones()
                data == null -> Text(stringResource(R.string.lb_empty), style = TaroTheme.text.note, color = colors.accent, modifier = Modifier.padding(spacing.loose))
                else -> {
                    Stage(pin.board, pin.range, data, onSeeAll = { onOpen(pin.board, pin.range) }, modifier = Modifier.padding(top = spacing.gap))
                    Column(modifier = Modifier.padding(horizontal = spacing.loose)) {
                        SectionLabel(stringResource(R.string.lb_records))
                        Board.entries.filter { data.has(it) }.chunked(COLUMNS).forEachIndexed { row, boards ->
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = Modifier.padding(top = if (row > 0) spacing.gap else spacing.hair)) {
                                boards.forEach { board -> RecordTile(board, data, row * COLUMNS + boards.indexOf(board), Modifier.weight(1f)) { onOpen(board, BoardRange.ALL) } }
                                repeat(COLUMNS - boards.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (editing) {
        PinsDialog(
            pins = saved,
            available = shown?.value?.let { data -> Board.entries.filter { data.has(it) }.toSet() } ?: Board.entries.toSet(),
            onSave = {
                saved = it
                AppPreferences.leaderboardPins = it
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
}

@Composable
private fun BoardContent(board: Board, initial: BoardRange) {
    val spacing = TaroTheme.spacing
    val colors = TaroTheme.colors
    val context = LocalContext.current
    val tick by Database.changes.collectAsStateWithLifecycle()
    val loaded = rememberInBackground(tick) { LeaderboardData.load(context) }
    var shown by remember { mutableStateOf<Loaded<BoardData?>?>(null) }
    if (loaded != null) shown = loaded
    var range by rememberSaveable { mutableStateOf(initial) }
    val color = boardColor(board)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AppOverflowMenu(back = true, title = stringResource(boardName(board)))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = spacing.loose),
        ) {
            TextTabs(
                listOf(BoardRange.ALL, BoardRange.MONTH, BoardRange.WEEKDAY, BoardRange.WEEKS).map { it to stringResource(rangeName(it)) },
                range,
                { range = it },
                color,
                Modifier.padding(horizontal = spacing.loose),
            )
            val data = shown?.value
            when {
                shown == null -> StageBones()
                data == null -> Text(stringResource(R.string.lb_empty), style = TaroTheme.text.note, color = colors.accent, modifier = Modifier.padding(spacing.loose))
                range == BoardRange.WEEKDAY -> WeekdayRows(board, data, Modifier.padding(horizontal = spacing.loose, vertical = spacing.gap))
                else -> {
                    Stage(board, range, data, onSeeAll = null, modifier = Modifier.padding(top = spacing.gap))
                    Column(modifier = Modifier.padding(horizontal = spacing.loose)) {
                        if (board == Board.SCORE && range != BoardRange.WEEKS) ScoreKey(Modifier.padding(top = spacing.loose))
                        RankRows(board, range, data, Modifier.padding(top = spacing.gap))
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardTile(modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val context = LocalContext.current
    val tick by Database.changes.collectAsStateWithLifecycle()
    val loaded = rememberInBackground(tick) { LeaderboardData.load(context) }
    var shown by remember { mutableStateOf<Loaded<BoardData?>?>(null) }
    if (loaded != null) shown = loaded
    val card = RoundedCornerShape(TaroTheme.spacing.loose + TaroTheme.spacing.tight)
    val current = shown ?: return StageBones(modifier, records = false, shape = card)
    val data = current.value ?: return
    val pin = AppPreferences.leaderboardPins.firstOrNull { data.has(it.board) } ?: return
    Stage(pin.board, pin.range, data, onSeeAll = null, compact = true, modifier = modifier.clip(card).clickable(onClick = onOpen))
}

@Composable
private fun StageBones() = StageBones(Modifier)

@Composable
private fun StageBones(modifier: Modifier, records: Boolean = true, shape: Shape = RoundedCornerShape(0)) {
    val spacing = TaroTheme.spacing
    Skeleton(modifier = modifier.padding(top = spacing.gap)) {
        Bone(
            Modifier
                .fillMaxWidth()
                .aspectRatio(BLOCKS_ASPECT * 0.8f),
            shape,
        )
        if (records) {
            BoneTilePair(1.1f, Modifier.padding(start = spacing.loose, end = spacing.loose, top = spacing.wide))
            BoneTilePair(1.1f, Modifier.padding(start = spacing.loose, end = spacing.loose, top = spacing.gap))
        }
    }
}

@Composable
private fun Stage(board: Board, range: BoardRange, data: BoardData, onSeeAll: (() -> Unit)?, modifier: Modifier = Modifier, compact: Boolean = false) {
    val palette = STAGES.getValue(board)
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val entries = data.entries(board, range)
    val standing = data.standing(board, range)
    val enabled = rememberAnimationsEnabled()
    val clock = remember(board, range) { Animatable(if (enabled) 0f else 1f) }
    LaunchedEffect(board, range, enabled) {
        if (enabled) clock.animateTo(1f, tween(STAGE_MS, easing = LinearEasing)) else clock.snapTo(1f)
    }
    val count = if (range == BoardRange.WEEKS) pluralStringResource(R.plurals.lb_weeks, entries.size, entries.size) else pluralStringResource(R.plurals.lb_days, entries.size, entries.size)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(palette.top, palette.middle, palette.base), center = Offset(size.width / 2, 0f), radius = maxOf(size.width, size.height)))
                val fold = size.width / FOLDS
                val reach = size.height * FOLD_REACH
                repeat(FOLDS) { i ->
                    val shade = if (i % 2 == 0) Color.White.copy(alpha = FOLD_LIGHT) else Color.Black.copy(alpha = FOLD_DARK)
                    drawRect(Brush.verticalGradient(listOf(shade, Color.Transparent), endY = reach), topLeft = Offset(i * fold, 0f), size = Size(fold, reach))
                }
                val scallop = size.width / SCALLOPS / 2
                repeat(SCALLOPS) { i -> drawCircle(palette.floor, scallop, Offset(scallop * (2 * i + 1), -scallop * SCALLOP_DROP)) }
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = spacing.loose, end = spacing.loose, top = spacing.wide)) {
            Text(stringResource(if (compact) R.string.lb_title else boardName(board)).uppercase(), style = type.label, color = palette.dim, modifier = Modifier.weight(1f))
            Text(
                if (compact) stringResource(R.string.lb_stage_head, stringResource(boardName(board)), stringResource(rangeName(range)).lowercase()) else stringResource(R.string.lb_stage_head, stringResource(rangeName(range)), count),
                style = type.note,
                color = palette.dim,
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(BLOCKS_ASPECT)
                .padding(horizontal = spacing.loose),
        ) {
            val area = maxHeight
            if (entries.isEmpty()) {
                Text(stringResource(R.string.lb_nothing_yet), style = type.note, color = palette.dim, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight), verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxSize()) {
                PODIUM_ORDER.forEach { place ->
                    val entry = entries.getOrNull(place)
                    if (entry == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val grow = blockProgress(clock.value, place)
                        val pop = ((clock.value * STAGE_MS - BLOCK_DELAYS[place] - VALUE_AFTER_MS) / VALUE_MS).coerceIn(0f, 1f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).fillMaxHeight()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.graphicsLayer {
                                    alpha = if (enabled) pop else 1f
                                    translationY = (1f - pop) * spacing.gap.toPx()
                                },
                            ) {
                                Text(boardValue(board, entry.value), style = if (place == 0) type.fact else type.factCompact, color = if (place == 0) SPOT_GOLD else ON_STAGE, maxLines = 1)
                                boardUnit(board)?.let { Text(it, style = type.axis, color = palette.dim, maxLines = 1) }
                            }
                            Block(entry, place, range, data.today, Modifier.padding(top = spacing.tight).fillMaxWidth().height(area * BLOCK_SHARES[place]).graphicsLayer {
                                scaleY = grow
                                transformOrigin = TransformOrigin(0.5f, 1f)
                            })
                        }
                    }
                }
            }
        }
        val gap = with(LocalDensity.current) { spacing.tight.toPx() }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.loose)
                .clipToBounds()
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(palette.floor, palette.footer)))
                    val pad = spacing.loose.toPx()
                    val slot = (size.width - pad * 2 - gap * 2) / 3
                    PODIUM_ORDER.forEachIndexed { column, place ->
                        if (entries.getOrNull(place) != null) {
                            drawRect(
                                Brush.verticalGradient(listOf(Color.Black.copy(alpha = CONTACT_ALPHA), Color.Transparent), endY = size.height * CONTACT_REACH),
                                topLeft = Offset(pad + (slot + gap) * column, 0f),
                                size = Size(slot, size.height * CONTACT_REACH),
                            )
                        }
                    }
                },
        )
        Footer(board, range, standing, palette, entries.size, onSeeAll, compact)
    }
}

private fun blockProgress(clock: Float, place: Int): Float {
    val local = ((clock * STAGE_MS - BLOCK_DELAYS[place]) / BLOCK_MS).coerceIn(0f, 1f)
    return EaseOutBack.transform(local)
}

@Composable
private fun Block(entry: Entry, place: Int, range: BoardRange, today: LocalDate, modifier: Modifier) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val (light, deep) = MEDALS[place]
    val recent = range != BoardRange.WEEKS && !entry.date.isBefore(today.minusDays(RECENT_DAYS))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(topStart = spacing.gap, topEnd = spacing.gap))
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(light, lerp(light, deep, BLOCK_DEPTH))))
                val edge = spacing.hair.toPx()
                drawRect(Color.White.copy(alpha = EDGE_LIGHT), size = Size(size.width, edge))
                drawRect(Color.White.copy(alpha = EDGE_LIGHT / 2), size = Size(edge / 2, size.height))
                drawRect(Color.Black.copy(alpha = SHADE_ALPHA / 2), topLeft = Offset(size.width - edge / 2, 0f), size = Size(edge / 2, size.height))
            }
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        BasicText(
            text = (place + 1).toString(),
            style = type.stamp.copy(color = BLOCK_INK, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(maxFontSize = if (place == 0) type.stamp.fontSize * RANK_SHARE else type.stamp.fontSize),
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.weight(1f))
        BasicText(
            text = entryLabel(entry, range) + if (recent) "\n" + stringResource(R.string.lb_new) else "",
            style = type.axis.copy(color = BLOCK_INK, textAlign = TextAlign.Center),
            maxLines = 2,
            autoSize = TextAutoSize.StepBased(maxFontSize = type.note.fontSize),
        )
    }
}

@Composable
private fun Footer(board: Board, range: BoardRange, standing: Standing, palette: StagePalette, ranked: Int, onSeeAll: (() -> Unit)?, compact: Boolean = false) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val color = boardColor(board)
    val next = standing.next
    val unit = boardUnit(board)
    val growth = rememberBarGrowth(board to range, 1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.footer)
            .padding(start = spacing.loose, end = spacing.loose, top = spacing.gap, bottom = spacing.loose),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(if (range == BoardRange.WEEKS) R.string.lb_this_week else R.string.lb_today), style = type.note, color = palette.dim)
                Text(
                    buildAnnotatedString {
                        append(boardValue(board, standing.value))
                        unit?.let { withStyle(type.note.toSpanStyle().copy(color = palette.dim)) { append(" $it") } }
                    },
                    style = type.fact,
                    color = ON_STAGE,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.lb_if_now), style = type.note, color = palette.dim)
                Text(
                    buildAnnotatedString {
                        append(stringResource(R.string.lb_rank, standing.rank))
                        withStyle(type.note.toSpanStyle().copy(color = palette.dim)) { append(" " + stringResource(R.string.lb_of, standing.of)) }
                    },
                    style = type.fact,
                    color = ON_STAGE,
                )
            }
        }
        if (!compact) {
            val fill = if (next == null) 1f else (standing.value / next.value).toFloat().coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .padding(top = spacing.gap)
                    .fillMaxWidth()
                    .height(spacing.gap),
            ) {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(Color.White.copy(alpha = TRACK_ALPHA), cornerRadius = radius)
                drawRoundRect(color, size = Size(size.width * fill * growth.bar(0), size.height), cornerRadius = radius)
                drawRect(ON_STAGE, topLeft = Offset(size.width - spacing.hair.toPx(), -size.height / 2), size = Size(spacing.hair.toPx(), size.height * 2))
            }
            Text(
                text = if (next != null) {
                    val gap = boardValue(board, next.value - standing.value)
                    val line = stringResource(R.string.lb_to_pass, gap, entryLabel(next, range), standing.rank - 1)
                    buildAnnotatedString {
                        append(line)
                        addStyle(SpanStyle(color = ON_STAGE), 0, gap.length.coerceAtMost(line.length))
                    }
                } else {
                    buildAnnotatedString { append(stringResource(if (range == BoardRange.WEEKS) R.string.lb_ahead_week else R.string.lb_ahead)) }
                },
                style = type.note,
                color = palette.dim,
                modifier = Modifier.padding(top = spacing.tight),
            )
            onSeeAll?.let {
                LinkText(
                    text = if (range == BoardRange.WEEKS) pluralStringResource(R.plurals.lb_see_weeks, ranked, ranked) else pluralStringResource(R.plurals.lb_see_days, ranked, ranked),
                    color = SPOT_GOLD,
                    onClick = it,
                    trailingIcon = R.drawable.ic_chevron_right,
                    modifier = Modifier.padding(top = spacing.tight),
                )
            }
        }
    }
}

@Composable
private fun RecordTile(board: Board, data: BoardData, order: Int, modifier: Modifier, onClick: () -> Unit) {
    val color = boardColor(board)
    val spacing = TaroTheme.spacing
    val entries = data.entries(board, BoardRange.ALL)
    val top = entries.first()
    val standing = data.standing(board, BoardRange.ALL)
    val recentFrom = data.today.minusDays(RECENT_DAYS)
    val shown = entries.take(MINI_BARS)
    val growth = rememberBarGrowth(board, shown.size)
    BentoTile(color = color, tinted = true, label = stringResource(boardName(board)), onClick = onClick, order = order, modifier = modifier) {
        BentoValue(boardValue(board, top.value), color)
        BentoNote(top.date.format(DAY_FORMAT))
        Canvas(
            modifier = Modifier
                .padding(top = spacing.gap)
                .fillMaxWidth()
                .aspectRatio(MINI_ASPECT),
        ) {
            val slot = size.width / MINI_BARS
            val width = slot * 0.75f
            shown.forEachIndexed { i, entry ->
                val height = size.height * (entry.value / top.value).toFloat() * growth.bar(i)
                drawRoundRect(
                    color.copy(alpha = if (entry.date.isBefore(recentFrom)) MINI_DIM else 1f),
                    topLeft = Offset(slot * i, size.height - height),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(width / 4),
                )
            }
        }
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.lb_hub_today, standing.rank))
                if (standing.rank <= MINI_BARS) withStyle(SpanStyle(color = color)) { append("  " + stringResource(R.string.lb_top_ten)) }
            },
            style = TaroTheme.text.note,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = spacing.gap),
        )
    }
}

private class Part(val share: Double, val color: Color)

@Composable
private fun scoreParts(score: DayScore): List<Part> {
    val colors = TaroTheme.colors
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = BASE_ALPHA)
    val best = score.bonuses.any { it.kind == BonusKind.BEST }
    val times = if (best) DayScores.BEST_TIMES else 1.0
    val added = 1 + score.bonuses.filter { it.kind != BonusKind.BEST }.sumOf { it.value }
    return buildList {
        add(Part(times, base))
        score.bonuses.filter { it.kind != BonusKind.BEST }.forEach { add(Part(it.value * times, bonusColor(it.kind, colors.goal, colors.flame, colors.red, colors.mint, colors.special, colors.purple))) }
        if (best) add(Part(added * (DayScores.BEST_TIMES - 1), colors.purple))
    }.let { parts -> parts.sumOf { it.share }.let { total -> parts.map { Part(it.share / total, it.color) } } }
}

private fun bonusColor(kind: BonusKind, goal: Color, flame: Color, red: Color, mint: Color, special: Color, purple: Color): Color = when (kind) {
    BonusKind.GOAL -> goal
    BonusKind.TIER -> flame
    BonusKind.STREAK -> red
    BonusKind.WALKS -> mint
    BonusKind.EARLY -> special
    BonusKind.BEST -> purple
}

@Composable
private fun ScoreKey(modifier: Modifier = Modifier) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    val keys = listOf(
        MaterialTheme.colorScheme.onSurface.copy(alpha = BASE_ALPHA) to R.string.lb_part_steps,
        colors.goal to R.string.lb_part_goal,
        colors.flame to R.string.lb_part_tiers,
        colors.red to R.string.lb_part_streak,
        colors.mint to R.string.lb_part_walks,
        colors.special to R.string.lb_part_early,
        colors.purple to R.string.lb_part_best,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.loose), modifier = modifier.horizontalScroll(rememberScrollState())) {
        keys.forEach { (color, label) -> KeyDot(color, stringResource(label)) }
    }
}

@Composable
private fun KeyDot(color: Color, label: String) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val dot = with(LocalDensity.current) { type.note.fontSize.toDp() } * 0.55f
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.tight)) {
        Box(Modifier.width(dot).height(dot).clip(RoundedCornerShape(50)).background(color))
        Text(label, style = type.note, color = TaroTheme.colors.accent, maxLines = 1)
    }
}

@Composable
private fun RankRows(board: Board, range: BoardRange, data: BoardData, modifier: Modifier = Modifier) {
    val entries = data.entries(board, range)
    val rest = entries.drop(3)
    if (rest.isEmpty()) return
    val max = entries.first().value
    val growth = rememberBarGrowth(board to range, rest.size)
    val rankWidth = rememberRankWidth()
    val recentFrom = data.today.minusDays(RECENT_DAYS)
    Column(modifier = modifier) {
        rest.forEachIndexed { i, entry ->
            if (i > 0) RowDivider()
            RankRow(
                rank = (i + 4).toString(),
                label = entryLabel(entry, range),
                value = boardValue(board, entry.value),
                share = entry.value / max,
                parts = entry.score?.takeIf { board == Board.SCORE && range != BoardRange.WEEKS }?.let { scoreParts(it) },
                color = boardColor(board),
                recent = range != BoardRange.WEEKS && !entry.date.isBefore(recentFrom),
                grow = growth.bar(i),
                rankWidth = rankWidth,
            )
        }
    }
}

@Composable
private fun WeekdayRows(board: Board, data: BoardData, modifier: Modifier = Modifier) {
    val entries = data.entries(board, BoardRange.WEEKDAY)
    if (entries.isEmpty()) return
    val max = entries.maxOf { it.value }
    val growth = rememberBarGrowth(board to BoardRange.WEEKDAY, entries.size)
    val rankWidth = rememberRankWidth(sample = "Wed")
    Column(modifier = modifier) {
        entries.forEachIndexed { i, entry ->
            if (i > 0) RowDivider()
            RankRow(
                rank = entry.date.dayOfWeek.getDisplayName(DayStyle.SHORT, Locale.getDefault()),
                label = entry.date.format(SHORT_FORMAT),
                value = boardValue(board, entry.value),
                share = entry.value / max,
                parts = entry.score?.takeIf { board == Board.SCORE }?.let { scoreParts(it) },
                color = boardColor(board),
                recent = false,
                grow = growth.bar(i),
                rankWidth = rankWidth,
                strongRank = true,
            )
        }
    }
}

@Composable
private fun rememberRankWidth(sample: String = "000"): androidx.compose.ui.unit.Dp {
    val measurer = rememberTextMeasurer()
    val style = TaroTheme.text.meterLabel
    val density = LocalDensity.current
    return remember(sample, style) { with(density) { measurer.measure(sample, style).size.width.toDp() } }
}

@Composable
private fun RankRow(
    rank: String,
    label: String,
    value: String,
    share: Double,
    parts: List<Part>?,
    color: Color,
    recent: Boolean,
    grow: Float,
    rankWidth: androidx.compose.ui.unit.Dp,
    strongRank: Boolean = false,
) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val colors = TaroTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = spacing.gap)) {
        Text(
            rank,
            style = type.meterLabel,
            color = if (strongRank) MaterialTheme.colorScheme.onSurface else colors.accent,
            textAlign = if (strongRank) TextAlign.Start else TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(rankWidth),
        )
        Column(modifier = Modifier.weight(1f).padding(start = spacing.gap)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                if (recent) Text(stringResource(R.string.lb_new), style = type.note, color = color, modifier = Modifier.padding(start = spacing.tight))
                Spacer(Modifier.weight(1f))
                Text(value, style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            }
            Canvas(
                modifier = Modifier
                    .padding(top = spacing.tight)
                    .fillMaxWidth()
                    .height(spacing.tight),
            ) {
                val radius = CornerRadius(size.height / 2)
                drawRoundRect(colors.accent.copy(alpha = TRACK_ALPHA * 1.3f), cornerRadius = radius)
                val total = size.width * share.toFloat() * grow
                if (parts == null) {
                    drawRoundRect(color, size = Size(total, size.height), cornerRadius = radius)
                } else {
                    var x = 0f
                    parts.forEach { part ->
                        val width = total * part.share.toFloat()
                        drawRect(part.color, topLeft = Offset(x, 0f), size = Size(width, size.height))
                        x += width
                    }
                }
            }
        }
    }
}

@Composable
private fun PinsDialog(pins: List<Pin>, available: Set<Board>, onSave: (List<Pin>) -> Unit, onDismiss: () -> Unit) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val colors = TaroTheme.colors
    val density = LocalDensity.current
    var list by remember { mutableStateOf(pins) }
    val tabs = with(density) { type.meterValue.lineHeight.toDp() } + spacing.tight * 2
    val rowHeight = with(density) { type.title.lineHeight.toDp() } + tabs + spacing.gap * 2

    TaroDialog(
        title = stringResource(R.string.lb_pins_title),
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onSave(list) },
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(stringResource(R.string.lb_pins_note), style = type.note, color = colors.accent, modifier = Modifier.padding(bottom = spacing.tight))
            ReorderColumn(list, { it.board }, rowHeight, stringResource(R.string.lb_drag), { from, to -> list = list.toMutableList().apply { add(to, removeAt(from)) } }) { pin ->
                val color = boardColor(pin.board)
                IconDisc(boardIcon(pin.board), color)
                Column(modifier = Modifier.weight(1f).padding(start = spacing.gap)) {
                    Text(stringResource(boardName(pin.board)), style = type.title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                    TextTabs(
                        Leaderboards.PINNABLE.map { it to stringResource(rangeName(it)) },
                        pin.range,
                        { range -> list = list.map { if (it.board == pin.board) it.copy(range = range) else it } },
                        color,
                    )
                }
                TaroSwitch(checked = true, onCheckedChange = { list = list.filterNot { it.board == pin.board } }, enabled = list.size > 1, color = color)
            }
            val more = Board.entries.filter { board -> board in available && list.none { it.board == board } }
            if (more.isNotEmpty()) {
                Text(
                    stringResource(R.string.lb_more_boards).uppercase(),
                    style = type.label,
                    color = colors.accent,
                    modifier = Modifier.padding(top = spacing.loose, bottom = spacing.tight),
                )
                more.forEachIndexed { i, board ->
                    if (i > 0) RowDivider()
                    val color = boardColor(board)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = spacing.tight)) {
                        IconDisc(boardIcon(board), color)
                        Text(stringResource(boardName(board)), style = type.title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, modifier = Modifier.weight(1f).padding(start = spacing.gap))
                        TaroSwitch(checked = false, onCheckedChange = { list = list + Pin(board, BoardRange.ALL) }, color = color)
                    }
                }
            }
        }
    }
}
