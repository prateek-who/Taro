package com.prateek.taro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatPill(icon: Int, text: String, color: Color, lit: Boolean, modifier: Modifier = Modifier, info: Info? = null) {
    val state = rememberInfoState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .infoClickable(state, info)
            .padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        GlowingIcon(icon = icon, lit = lit, litColor = color, modifier = Modifier.padding(end = 4.dp))
        RollingText(text, TextStyle(fontSize = 16.sp, color = color, fontFamily = MaterialTheme.typography.bodyLarge.fontFamily))
        InfoPopup(state, info, color)
    }
}

private const val PILL_BORDER_ALPHA = 0.16f
