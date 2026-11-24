package com.prateek.taro.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroTheme
import com.prateek.taro.util.Util
import java.time.LocalDate

private const val DISABLED_ALPHA = 0.35f
private const val STEPPER_ICON_SHARE = 0.55f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val shape = RoundedCornerShape(spacing.loose)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(TaroTheme.colors.accentOpaque)
            .then(if (selected) Modifier.cornerGlow(color).border(spacing.hair / 2, color, shape) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = spacing.hair, vertical = spacing.gap),
    ) {
        IconDisc(icon, color)
        BasicText(
            text = label,
            style = type.meterLabel.copy(
                color = if (selected) MaterialTheme.colorScheme.onSurface else TaroTheme.colors.accent,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(maxFontSize = type.meterLabel.fontSize),
            modifier = Modifier.padding(top = spacing.tight),
        )
    }
}

@Composable
fun <T> IconTileGrid(items: List<T>, columns: Int, modifier: Modifier = Modifier, tile: @Composable (T, Modifier) -> Unit) {
    val spacing = TaroTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.tight), modifier = modifier) {
        items.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.fillMaxWidth()) {
                row.forEach { tile(it, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun StepperButton(@DrawableRes icon: Int, label: String, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(TaroTheme.colors.accentOpaque)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
    ) {
        Icon(painterResource(icon), contentDescription = label, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(size * STEPPER_ICON_SHARE))
    }
}

@Composable
fun StepperField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    decimal: Boolean = false,
    minusEnabled: Boolean = true,
) {
    val type = TaroTheme.text
    val spacing = TaroTheme.spacing
    val button = with(LocalDensity.current) { type.value.lineHeight.toDp() }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.gap), modifier = modifier.fillMaxWidth()) {
        StepperButton(R.drawable.ic_remove, stringResource(R.string.stepper_less), minusEnabled, button, onMinus)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = type.value.copy(color = color, textAlign = TextAlign.End),
                cursorBrush = SolidColor(color),
                keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Text(unit, style = type.unit, color = TaroTheme.colors.accent, modifier = Modifier.weight(1f).padding(start = spacing.hair, bottom = spacing.hair))
        }
        StepperButton(R.drawable.ic_add, stringResource(R.string.stepper_more), true, button, onPlus)
    }
}

@Composable
fun WhenDialog(
    title: String,
    date: LocalDate,
    minuteOfDay: Int,
    timeLabel: String,
    onSave: (LocalDate, Int) -> Unit,
    onDismiss: () -> Unit,
    top: (@Composable () -> Unit)? = null,
) {
    var day by remember { mutableStateOf(date) }
    var minute by remember { mutableIntStateOf(minuteOfDay) }
    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(R.string.action_save),
        onConfirm = { onSave(day, minute) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TaroTheme.spacing.gap)) {
            top?.invoke()
            DateRow(label = stringResource(R.string.date_label), date = day, today = Util.logicalToday(), onChange = { day = it })
            TimeRow(label = timeLabel, minuteOfDay = minute, onChange = { minute = it })
        }
    }
}
