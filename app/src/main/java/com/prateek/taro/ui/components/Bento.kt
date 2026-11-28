package com.prateek.taro.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.drawscope.scale
import com.prateek.taro.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.prateek.taro.ui.theme.TaroTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.hypot

private const val GLOW_ALPHA = 0.2f
private const val GLOW_REACH = 1.1f
private const val LINE_SHARE = 0.045f
private const val DOT_SHARE = 0.62f
private const val BAND_TO_LABEL = 2.4f

@Composable
fun BentoTile(
    color: Color,
    modifier: Modifier = Modifier,
    label: String? = null,
    tinted: Boolean = false,
    onClick: (() -> Unit)? = null,
    padding: Dp = TaroTheme.spacing.loose,
    order: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = TaroTheme.colors
    val spacing = TaroTheme.spacing
    Column(
        modifier = modifier
            .then(if (order != null) Modifier.enter(order) else Modifier)
            .clip(RoundedCornerShape(spacing.loose + spacing.tight))
            .background(colors.accentOpaque)
            .then(if (tinted) Modifier.cornerGlow(color) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
    ) {
        if (label != null) {
            Text(text = label.uppercase(), style = TaroTheme.text.label, color = if (tinted) color else colors.accent)
        }
        content()
    }
}

fun Modifier.cornerGlow(color: Color): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            listOf(color.copy(alpha = GLOW_ALPHA), color.copy(alpha = 0f)),
            center = Offset.Zero,
            radius = hypot(size.width, size.height) * GLOW_REACH,
        )
    )
}

@Composable
fun BentoTitle(text: String, modifier: Modifier = Modifier, note: String? = null) {
    val type = TaroTheme.text
    val accent = TaroTheme.colors.accent
    Text(
        text = buildAnnotatedString {
            append(text)
            note?.let { withStyle(type.note.toSpanStyle().copy(color = accent)) { append("  $it") } }
        },
        style = type.title,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

@Composable
fun BentoValue(value: String, color: Color, modifier: Modifier = Modifier, unit: String? = null) {
    val accent = TaroTheme.colors.accent
    val type = TaroTheme.text
    Text(
        text = buildAnnotatedString {
            append(value)
            unit?.let { withStyle(type.unit.toSpanStyle().copy(color = accent)) { append(" $it") } }
        },
        style = type.value,
        color = color,
        modifier = modifier.padding(top = TaroTheme.spacing.tight),
    )
}

@Composable
fun ValueField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
    focusRequester: FocusRequester? = null,
) {
    val type = TaroTheme.text
    Row(modifier = modifier.padding(top = TaroTheme.spacing.tight)) {
        NumberInput(value, onValueChange, type.value, color, Modifier.alignByBaseline(), TextAlign.Start, focusRequester)
        unit?.let { Text(" $it", style = type.unit, color = TaroTheme.colors.accent, modifier = Modifier.alignByBaseline()) }
    }
}

@Composable
fun TitleField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    color: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = TaroTheme.text.greeting,
    focusRequester: FocusRequester? = null,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val line = TaroTheme.spacing.hair
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = style.copy(color = ink),
        cursorBrush = SolidColor(color),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .drawBehind {
                val stroke = line.toPx() * FIELD_LINE_SHARE
                val y = size.height - stroke / 2
                drawLine((if (value.text.isEmpty()) ink else color).copy(alpha = FIELD_LINE_ALPHA), Offset(0f, y), Offset(size.width, y), stroke)
            },
        decorationBox = { inner ->
            Box(modifier = Modifier.padding(bottom = line)) {
                if (value.text.isEmpty()) Text(placeholder, style = style, color = TaroTheme.colors.accent, maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
fun FactField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    unit: String? = null,
    style: TextStyle = TaroTheme.text.fact,
) {
    val type = TaroTheme.text
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(horizontal = TaroTheme.spacing.tight, vertical = TaroTheme.spacing.hair)) {
        Row {
            NumberInput(value, onValueChange, style, color, Modifier.alignByBaseline(), TextAlign.Center)
            unit?.let { Text(" $it", style = style, color = color, modifier = Modifier.alignByBaseline()) }
        }
        BasicText(
            text = label,
            style = type.note.copy(color = TaroTheme.colors.accent),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(maxFontSize = type.note.fontSize),
        )
    }
}

@Composable
private fun NumberInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    style: TextStyle,
    color: Color,
    modifier: Modifier,
    align: TextAlign,
    focusRequester: FocusRequester? = null,
) {
    val measurer = rememberTextMeasurer()
    val line = TaroTheme.spacing.hair
    val width = with(LocalDensity.current) {
        val text = measurer.measure(value.text.ifEmpty { "0" }, style).size.width.toDp() + line * 2
        maxOf(text, style.fontSize.toDp() * FIELD_MIN_SHARE)
    }
    BasicTextField(
        value = value,
        onValueChange = { input -> if (input.text.all { it.isDigit() || it == '.' || it == ',' }) onValueChange(input) },
        singleLine = true,
        textStyle = style.copy(color = color, textAlign = align),
        cursorBrush = SolidColor(color),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
            .width(width)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .drawBehind {
                val stroke = line.toPx() * FIELD_LINE_SHARE
                val y = size.height - stroke / 2
                drawLine(color.copy(alpha = FIELD_LINE_ALPHA), Offset(0f, y), Offset(size.width, y), stroke)
            },
    )
}

@Composable
fun BentoNote(text: String, modifier: Modifier = Modifier, color: Color = TaroTheme.colors.accent) {
    Text(text, style = TaroTheme.text.note, color = color, modifier = modifier.padding(top = TaroTheme.spacing.hair))
}

@Composable
fun Sparkline(values: List<Double?>, color: Color, modifier: Modifier = Modifier, floor: Double? = null) {
    val progress = rememberDrawIn(values.size)
    Canvas(modifier = modifier) {
        val known = values.withIndex().filter { it.value != null }
        if (known.size < 2) return@Canvas
        val low = floor ?: known.minOf { it.value!! }
        val high = known.maxOf { it.value!! }.coerceAtLeast(low + 1e-6)
        val line = size.height * LINE_SHARE
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        val points = known.map { (index, value) ->
            Offset(index * step, line + (size.height - 2 * line) * (1 - ((value!! - low) / (high - low)).toFloat()))
        }
        val fill = progress.fill()
        if (fill > 0f) {
            val area = monotonePath(points).apply {
                lineTo(points.last().x, size.height)
                lineTo(points.first().x, size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.24f * fill), color.copy(alpha = 0f))))
        }
        drawPath(monotonePath(points).partial(progress.value), color, style = Stroke(line, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun PairedBars(values: List<Pair<Double, Double?>>, first: Color, second: Color, modifier: Modifier = Modifier, labels: List<String>? = null) {
    if (labels == null) {
        Pairs(values, first, second, modifier)
        return
    }
    Column(modifier = modifier) {
        Pairs(values, first, second, Modifier.fillMaxWidth().weight(1f))
        Row(modifier = Modifier.fillMaxWidth().padding(top = TaroTheme.spacing.tight)) {
            labels.forEach {
                Text(it, style = TaroTheme.text.axis, color = TaroTheme.colors.accent, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Pairs(values: List<Pair<Double, Double?>>, first: Color, second: Color, modifier: Modifier) {
    val growth = rememberBarGrowth(values.size, values.size * 2)
    val morph = rememberMorph(values.flatMap { listOf(it.first, it.second ?: 0.0) })
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val flat = morph()
        val values = values.mapIndexed { index, (a, b) -> (flat.getOrNull(index * 2) ?: a) to b?.let { flat.getOrNull(index * 2 + 1) ?: it } }
        val max = values.maxOf { maxOf(it.first, it.second ?: 0.0) }.takeIf { it > 0 } ?: 1.0
        val slot = size.width / values.size
        val gap = slot * 0.06f
        val bar = slot * 0.36f
        val radius = CornerRadius(bar / 2)
        values.forEachIndexed { index, (a, b) ->
            val pair = listOfNotNull(a to first, b?.takeIf { it > 0 }?.let { it to second })
            val left = index * slot + (slot - bar * pair.size - gap * (pair.size - 1)) / 2
            pair.forEachIndexed { k, (value, color) ->
                val height = (value / max * size.height).toFloat() * growth.bar(index * 2 + k)
                if (height > 0f) {
                    drawRoundRect(color, topLeft = Offset(left + k * (bar + gap), size.height - height), size = Size(bar, height), cornerRadius = radius)
                }
            }
        }
    }
}

@Composable
fun NightColumns(hours: List<Double?>, target: Double, labels: List<String>, targetLabel: String, color: Color, modifier: Modifier = Modifier) {
    val growth = rememberBarGrowth(labels, hours.size)
    val type = TaroTheme.text
    val accent = TaroTheme.colors.accent
    val measurer = rememberTextMeasurer()
    val spacing = TaroTheme.spacing
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val label = measurer.measure(targetLabel, type.axis.copy(color = accent))
            val left = label.size.width + spacing.tight.toPx()
            val max = maxOf(target * TARGET_HEADROOM, hours.filterNotNull().maxOrNull() ?: 0.0)
            val slot = (size.width - left) / hours.size
            val bar = slot * NIGHT_BAR_SHARE
            fun y(value: Double) = size.height - (value / max * size.height).toFloat()
            val line = y(target)
            drawLine(color, Offset(left, line), Offset(size.width, line), strokeWidth = Stroke.HairlineWidth + spacing.hair.toPx() / 2, pathEffect = PathEffect.dashPathEffect(floatArrayOf(bar / 2, bar / 2)))
            drawText(label, topLeft = Offset(0f, line - label.size.height / 2))
            hours.forEachIndexed { index, value ->
                val height = ((value ?: 0.0) / max * size.height).toFloat() * growth.bar(index)
                if (height <= 0f) return@forEachIndexed
                drawRoundRect(
                    color.copy(alpha = if (index == hours.lastIndex) 1f else NIGHT_PAST_ALPHA),
                    topLeft = Offset(left + index * slot + (slot - bar) / 2, size.height - height),
                    size = Size(bar, height),
                    cornerRadius = CornerRadius(minOf(bar / 2, height / 2)),
                )
            }
        }
        val left = with(LocalDensity.current) { measurer.measure(targetLabel, type.axis).size.width.toDp() + spacing.tight }
        Row(modifier = Modifier.fillMaxWidth().padding(start = left, top = spacing.hair)) {
            labels.forEachIndexed { index, text ->
                Text(text, style = type.axis, color = if (index == labels.lastIndex) MaterialTheme.colorScheme.onSurface else accent, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

private const val TARGET_HEADROOM = 1.2
private const val NIGHT_BAR_SHARE = 0.56f
private const val NIGHT_PAST_ALPHA = 0.38f

enum class DayMark { HIT, MADE_UP, REST, MISS, TODAY, NONE }

@Composable
fun WeekDots(marks: List<DayMark>, modifier: Modifier = Modifier, labels: List<String>? = null) {
    if (labels == null) {
        Dots(marks, modifier)
        return
    }
    Column(modifier = modifier) {
        Dots(marks, Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach {
                Text(it, style = TaroTheme.text.axis, color = TaroTheme.colors.accent, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Dots(marks: List<DayMark>, modifier: Modifier) {
    val colors = TaroTheme.colors
    val growth = rememberBarGrowth(Unit, marks.size)
    Canvas(modifier = modifier.aspectRatio(marks.size.coerceAtLeast(1).toFloat())) {
        val slot = size.width / marks.size
        val r = minOf(slot, size.height) * DOT_SHARE / 2
        val line = r * 0.22f
        marks.forEachIndexed { index, mark ->
            val center = Offset(slot * (index + 0.5f), size.height / 2)
            scale(growth.bar(index), center) { when (mark) {
                DayMark.HIT -> drawCircle(colors.special, r, center)
                DayMark.MADE_UP -> {
                    drawCircle(colors.special, r, center)
                    drawCircle(colors.background, r * 0.55f, center)
                    drawCircle(colors.special, r * 0.32f, center)
                }
                DayMark.REST -> drawCircle(colors.special, r - line, center, style = Stroke(line * 1.3f))
                DayMark.MISS -> drawCircle(colors.accent.copy(alpha = 0.35f), r, center)
                DayMark.TODAY -> drawCircle(colors.goal, r - line, center, style = Stroke(line, pathEffect = PathEffect.dashPathEffect(floatArrayOf(line * 2f, line * 1.4f))))
                DayMark.NONE -> drawCircle(colors.accent.copy(alpha = 0.3f), r - line, center, style = Stroke(line))
            } }
        }
    }
}

data class StripSpan(val start: Long, val end: Long, val color: Color)

@Composable
fun DayStrip(start: Long, end: Long, now: Long?, spans: List<StripSpan>, dots: List<Long>, modifier: Modifier = Modifier) {
    val colors = TaroTheme.colors
    val lane = MaterialTheme.colorScheme.onSurface.copy(alpha = LANE_ALPHA)
    val ink = MaterialTheme.colorScheme.onSurface
    val context = LocalContext.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labelStyle = TaroTheme.text.axis.copy(color = colors.accent)
    val gap = TaroTheme.spacing.tight
    val hourFormat = remember {
        SimpleDateFormat(if (android.text.format.DateFormat.is24HourFormat(context)) "HH:mm" else "h a", Locale.getDefault())
    }
    val labelHeight = remember(labelStyle, density) { measurer.measure("12 am", labelStyle).size.height }
    val height = with(density) { (labelHeight * (1 + BAND_TO_LABEL)).toDp() } + gap
    val progress = rememberDrawIn(start)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val band = size.height - labelHeight - gap.toPx()
        val mid = band / 2
        val thick = band * LANE_SHARE
        val top = mid - thick / 2
        val length = (end - start).toFloat().coerceAtLeast(1f)
        fun x(at: Long) = ((at - start) / length * size.width).coerceIn(0f, size.width)
        val shape = Path().apply { addRoundRect(RoundRect(0f, top, size.width, top + thick, CornerRadius(thick / 2))) }
        clipRect(right = size.width * progress.value) {
            clipPath(shape) {
                val nowX = now?.takeIf { it in start..end }?.let { x(it) } ?: size.width
                drawRect(lane.copy(alpha = LANE_ALPHA * FUTURE_SHARE), topLeft = Offset(0f, top), size = Size(size.width, thick))
                drawRect(lane, topLeft = Offset(0f, top), size = Size(nowX, thick))
                spans.forEach { span ->
                    val left = x(span.start)
                    val width = maxOf(x(span.end) - left, thick * 0.3f)
                    drawRoundRect(span.color, topLeft = Offset(left, top), size = Size(width, thick), cornerRadius = CornerRadius(minOf(width / 2, thick * 0.3f)))
                }
            }
            dots.forEach { at ->
                drawCircle(colors.background, thick * 0.42f, Offset(x(at), mid))
                drawCircle(colors.special, thick * 0.3f, Offset(x(at), mid))
            }
            now?.takeIf { it in start..end }?.let {
                val nowX = x(it)
                drawLine(ink, Offset(nowX, top - thick * 0.35f), Offset(nowX, top + thick * 1.35f), strokeWidth = thick * 0.14f, cap = StrokeCap.Round)
            }
        }
        val tick = thick * 0.22f
        for (quarter in 0 until 4) {
            val at = start + (end - start) * quarter / 4
            val tickX = quarter * size.width / 4
            if (quarter > 0) drawLine(colors.accent.copy(alpha = 0.5f), Offset(tickX, top + thick + tick * 0.6f), Offset(tickX, top + thick + tick * 1.6f), strokeWidth = tick * 0.4f, cap = StrokeCap.Round)
            val layout = measurer.measure(hourFormat.format(Date(at)).lowercase(Locale.getDefault()), labelStyle)
            val labelX = if (quarter == 0) 0f else (tickX - layout.size.width / 2f).coerceAtMost(size.width - layout.size.width)
            drawText(layout, topLeft = Offset(labelX, size.height - labelHeight))
        }
    }
}

@Composable
fun Fact(value: String, label: String, color: Color, modifier: Modifier = Modifier, info: Info? = null, @DrawableRes icon: Int? = null, style: TextStyle = TaroTheme.text.fact, alignment: Alignment = Alignment.Center) {
    val type = TaroTheme.text
    val iconSize = with(LocalDensity.current) { (style.lineHeight.toDp() + type.note.lineHeight.toDp()) * FACT_ICON_SHARE }
    Box(contentAlignment = alignment, modifier = modifier) {
        InfoBox(info?.withIcon(icon), Modifier.clip(RoundedCornerShape(TaroTheme.spacing.gap)), color = color) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = TaroTheme.spacing.tight, vertical = TaroTheme.spacing.hair)) {
                if (icon != null) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier
                            .padding(end = TaroTheme.spacing.tight)
                            .size(iconSize),
                    )
                }
                Column(horizontalAlignment = if (icon != null || (alignment as? BiasAlignment)?.horizontalBias == -1f) Alignment.Start else Alignment.CenterHorizontally) {
                    RollingText(value, style.copy(color = color))
                    BasicText(
                        text = label,
                        style = type.note.copy(color = TaroTheme.colors.accent),
                        maxLines = 1,
                        autoSize = TextAutoSize.StepBased(maxFontSize = type.note.fontSize),
                    )
                }
            }
        }
    }
}

@Composable
fun LinkText(text: String, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, @DrawableRes icon: Int? = null, info: Info? = null, @DrawableRes trailingIcon: Int? = null) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val iconSize = with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }
    InfoBox(info?.withIcon(icon), modifier.clip(RoundedCornerShape(spacing.tight)), color = color, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = spacing.hair, vertical = spacing.tight)) {
            icon?.let { Icon(painterResource(it), contentDescription = null, tint = color, modifier = Modifier.padding(end = spacing.hair).size(iconSize)) }
            Text(text, style = type.meterValue, color = color, maxLines = 1)
            trailingIcon?.let { Icon(painterResource(it), contentDescription = null, tint = color, modifier = Modifier.padding(start = spacing.tight).size(iconSize)) }
        }
    }
}

@Composable
fun KeyFact(color: Color, value: String, label: String, modifier: Modifier = Modifier, info: Info? = null) {
    if (info != null) {
        InfoBox(info, modifier.clip(RoundedCornerShape(TaroTheme.spacing.tight)), color = color) {
            KeyFact(color, value, label, Modifier.padding(end = TaroTheme.spacing.tight))
        }
        return
    }
    val type = TaroTheme.text
    Row(verticalAlignment = Alignment.Top, modifier = modifier) {
        Canvas(
            modifier = Modifier
                .padding(top = TaroTheme.spacing.tight, end = TaroTheme.spacing.tight)
                .size(with(LocalDensity.current) { type.note.fontSize.toDp() * KEY_DOT_SHARE }),
        ) { drawCircle(color) }
        Column {
            Text(value, style = type.meterLabel, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
            Text(label, style = type.note, color = TaroTheme.colors.accent, maxLines = 1)
        }
    }
}

private const val LANE_ALPHA = 0.1f
private const val LANE_SHARE = 0.62f
private const val FUTURE_SHARE = 0.45f
private const val KEY_DOT_SHARE = 0.6f
private const val FACT_ICON_SHARE = 0.75f

@Composable
fun IconDisc(@DrawableRes icon: Int, color: Color) {
    val size = with(LocalDensity.current) { (TaroTheme.text.title.lineHeight.toDp() + TaroTheme.text.note.lineHeight.toDp()) * DISC_SHARE }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = DISC_ALPHA)),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(size * DISC_ICON_SHARE))
    }
}

@Composable
fun EditRow(@DrawableRes icon: Int, color: Color, label: String, value: String, onClick: () -> Unit, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        IconDisc(icon, color)
        Text(label, style = type.title, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, style = type.meterValue, color = valueColor, textAlign = TextAlign.End)
        Icon(
            painterResource(R.drawable.ic_edit),
            contentDescription = stringResource(R.string.meter_edit, label),
            tint = TaroTheme.colors.accent,
            modifier = Modifier.size(with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }),
        )
    }
}

@Composable
fun NavRow(@DrawableRes icon: Int, color: Color, title: String, note: String, onClick: () -> Unit) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.gap))
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        IconDisc(icon, color)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = type.title, color = MaterialTheme.colorScheme.onSurface)
            Text(note, style = type.note, color = TaroTheme.colors.accent)
        }
        Icon(
            painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = TaroTheme.colors.accent,
            modifier = Modifier.size(with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }),
        )
    }
}

@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.onSurface.copy(alpha = ROW_DIVIDER_ALPHA), modifier = modifier)
}

private const val DISC_SHARE = 0.9f
private const val DISC_ALPHA = 0.16f
private const val DISC_ICON_SHARE = 0.55f
private const val ROW_DIVIDER_ALPHA = 0.08f

@Composable
fun <T> TextTabs(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, color: Color, modifier: Modifier = Modifier) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = modifier) {
        options.forEach { (value, label) ->
            val active = value == selected
            val line = with(LocalDensity.current) { spacing.hair.toPx() }
            Text(
                text = label,
                style = type.meterValue,
                color = if (active) MaterialTheme.colorScheme.onSurface else TaroTheme.colors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(spacing.tight))
                    .clickable { onSelect(value) }
                    .padding(horizontal = spacing.hair, vertical = spacing.tight)
                    .drawBehind {
                        if (active) drawLine(color, Offset(0f, size.height + line), Offset(size.width, size.height + line), strokeWidth = line, cap = StrokeCap.Round)
                    },
            )
        }
    }
}

private const val FIELD_MIN_SHARE = 1.2f
private const val FIELD_LINE_ALPHA = 0.5f
private const val FIELD_LINE_SHARE = 0.75f
