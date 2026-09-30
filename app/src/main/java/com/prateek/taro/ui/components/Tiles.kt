package com.prateek.taro.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prateek.taro.ui.theme.TaroTheme

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, info: Info? = null, action: Pair<String, () -> Unit>? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = if (action != null) 16.dp else 24.dp, bottom = if (action != null) 2.dp else 10.dp),
    ) {
        InfoBox(info, Modifier.clip(RoundedCornerShape(8.dp)), color = MaterialTheme.colorScheme.onSurface) {
            Text(
                text = text.uppercase(),
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                color = TaroTheme.colors.accent,
            )
        }
        if (action != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = action.first,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = action.second)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    info: Info? = null,
) {
    val state = rememberInfoState()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(TaroTheme.colors.accentOpaque)
            .infoClickable(state, info, onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        InfoPopup(state, info, if (color == MaterialTheme.colorScheme.onSurface) TaroTheme.colors.accent else color)
        Text(label.uppercase(), fontSize = 11.sp, letterSpacing = 1.5.sp, color = TaroTheme.colors.accent)
        RollingText(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, color = color),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun TintChip(text: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, info: Info? = null) {
    val state = rememberInfoState()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.14f))
            .infoClickable(state, info, onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        InfoPopup(state, info, color)
    }
}
