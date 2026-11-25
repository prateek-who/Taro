package com.prateek.taro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroTheme

@Composable
fun SectionHeader(text: String, topPadding: Dp = TaroTheme.spacing.wide) {
    Text(
        text = text.uppercase(),
        style = TaroTheme.text.label,
        color = TaroTheme.colors.accent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = TaroTheme.spacing.tight, end = TaroTheme.spacing.tight, top = topPadding, bottom = TaroTheme.spacing.tight),
    )
}

@Composable
fun PreferenceRow(
    @DrawableRes icon: Int,
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null,
    value: String? = null,
    color: Color? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val spacing = TaroTheme.spacing
    val type = TaroTheme.text
    val tint = color ?: LocalSettingsTint.current ?: TaroTheme.colors.goal
    val iconSize = with(LocalDensity.current) { type.meterValue.lineHeight.toDp() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.gap),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(RoundedCornerShape(spacing.gap))
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = spacing.tight, vertical = spacing.gap),
    ) {
        IconDisc(icon, tint)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = type.title, color = MaterialTheme.colorScheme.onSurface)
            if (!summary.isNullOrEmpty()) {
                Text(summary, style = type.note, color = TaroTheme.colors.accent, modifier = Modifier.padding(top = spacing.hair))
            }
        }
        if (value != null) {
            Text(value, style = type.meterValue, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, textAlign = TextAlign.End)
            if (onClick != null) Icon(painterResource(R.drawable.ic_edit), contentDescription = null, tint = TaroTheme.colors.accent, modifier = Modifier.size(iconSize))
        }
        trailing()
        if (showChevron && value == null) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = TaroTheme.colors.accent, modifier = Modifier.size(iconSize))
        }
    }
}

@Composable
fun PreferenceDivider() {
    HorizontalDivider(
        thickness = Dp.Hairline,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA),
        modifier = Modifier.padding(horizontal = TaroTheme.spacing.tight),
    )
}

private const val DISABLED_ALPHA = 0.4f
private const val DIVIDER_ALPHA = 0.08f
