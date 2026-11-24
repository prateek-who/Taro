package com.prateek.taro.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroTheme

@Composable
fun EventCard(color: Color, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    BentoTile(color = color, tinted = true, onClick = onClick, padding = TaroTheme.spacing.gap, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.padding(horizontal = TaroTheme.spacing.hair)) { content() }
    }
}

@Composable
fun CardHead(title: String, value: String? = null, valueColor: Color = TaroTheme.colors.accent, found: Boolean = false) {
    val type = TaroTheme.text
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = type.meterLabel,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (found) {
            Text(
                text = stringResource(R.string.day_found).uppercase(),
                style = type.axis,
                color = TaroTheme.colors.accent,
                modifier = Modifier.padding(start = TaroTheme.spacing.tight),
            )
        }
        Box(modifier = Modifier.weight(1f))
        value?.let { Text(it, style = type.meterLabel, color = valueColor, maxLines = 1, modifier = Modifier.padding(start = TaroTheme.spacing.tight)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CardStats(stats: List<Triple<Color, String, String>>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.loose),
        verticalArrangement = Arrangement.spacedBy(TaroTheme.spacing.tight),
        modifier = Modifier.padding(top = TaroTheme.spacing.gap, bottom = TaroTheme.spacing.tight),
    ) {
        stats.forEach { (color, value, label) -> KeyFact(color, value, label) }
    }
}

@Composable
fun CardActions(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(TaroTheme.spacing.loose), modifier = Modifier.padding(top = TaroTheme.spacing.hair)) { content() }
}

@Composable
fun ChangeOrRemove(expanded: Boolean, onChange: () -> Unit, onRemove: () -> Unit) {
    AnimatedVisibility(visible = expanded) {
        CardActions {
            LinkText(stringResource(R.string.day_change), MaterialTheme.colorScheme.onSurface, onClick = onChange, icon = R.drawable.ic_edit)
            LinkText(stringResource(R.string.day_remove), MaterialTheme.colorScheme.error, onClick = onRemove, icon = R.drawable.ic_delete)
        }
    }
}
