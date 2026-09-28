package com.nvllz.stepsy.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nvllz.stepsy.R
import com.nvllz.stepsy.ui.theme.StepsyTheme

@Composable
fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StepsyTheme.colors.background),
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

@Composable
fun StepsySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Icon(
                    painter = painterResource(R.drawable.ic_tick),
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            checkedIconColor = MaterialTheme.colorScheme.primary,
            checkedBorderColor = MaterialTheme.colorScheme.primary,
            uncheckedThumbColor = StepsyTheme.colors.accent,
            uncheckedTrackColor = StepsyTheme.colors.accentOpaque,
            uncheckedBorderColor = StepsyTheme.colors.accent,
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
            style = MaterialTheme.typography.bodyLarge,
        )
        StepsySwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
fun StepsySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = StepsyTheme.colors.accent,
            activeTrackColor = StepsyTheme.colors.accent,
            inactiveTrackColor = StepsyTheme.colors.accentOpaque,
            activeTickColor = MaterialTheme.colorScheme.primary,
            inactiveTickColor = StepsyTheme.colors.accent,
        ),
    )
}

@Composable
fun <T> ToggleGroup(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = value == selected,
                onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = StepsyTheme.colors.accent,
                    inactiveContainerColor = StepsyTheme.colors.accentOpaque,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                    inactiveBorderColor = StepsyTheme.colors.accent,
                ),
                label = { Text(label) },
            )
        }
    }
}
