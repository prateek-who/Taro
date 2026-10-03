package com.prateek.taro.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
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
fun SectionLabel(text: String, modifier: Modifier = Modifier, info: Info? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = if (action != null) 4.dp else 0.dp, top = if (action != null) 16.dp else 24.dp, bottom = if (action != null) 8.dp else 10.dp),
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
            action()
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
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxHeight()
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

@Composable
fun ActionChip(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    trailingIcon: Int? = null,
    info: Info? = null,
) {
    val state = rememberInfoState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), CircleShape)
            .infoClickable(state, info, onClick)
            .padding(start = if (icon != null) 10.dp else 14.dp, end = if (trailingIcon != null) 8.dp else 14.dp, top = 7.dp, bottom = 7.dp),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.padding(end = 6.dp).size(16.dp))
        }
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        if (trailingIcon != null) {
            Icon(painterResource(trailingIcon), contentDescription = null, tint = color, modifier = Modifier.padding(start = 2.dp).size(18.dp))
        }
        InfoPopup(state, info, color)
    }
}

@Composable
fun TileRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.height(IntrinsicSize.Min),
        content = content,
    )
}
