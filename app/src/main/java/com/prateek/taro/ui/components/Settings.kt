package com.prateek.taro.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.prateek.taro.ui.theme.TaroTheme

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(TaroTheme.spacing.wide))
            .background(TaroTheme.colors.accentOpaque)
            .padding(horizontal = TaroTheme.spacing.tight, vertical = TaroTheme.spacing.hair),
    ) {
        CompositionLocalProvider(LocalSettingsTint provides tint) { content() }
    }
}

val LocalSettingsTint = compositionLocalOf<Color?> { null }

@Composable
fun TaroSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = LocalSettingsTint.current ?: TaroTheme.colors.goal,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = TaroTheme.colors.background,
            checkedTrackColor = color,
            checkedBorderColor = color,
            uncheckedThumbColor = TaroTheme.colors.accent,
            uncheckedTrackColor = TaroTheme.colors.accentOpaque,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

@Composable
fun SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = TaroTheme.text.meterLabel,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TaroSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun TaroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    color: Color = LocalSettingsTint.current ?: TaroTheme.colors.goal,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.onSurface,
            activeTrackColor = color,
            inactiveTrackColor = TaroTheme.colors.accentOpaque,
            activeTickColor = MaterialTheme.colorScheme.primary,
            inactiveTickColor = TaroTheme.colors.accent,
        ),
    )
}
