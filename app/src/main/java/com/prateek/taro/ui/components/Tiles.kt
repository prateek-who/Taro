package com.prateek.taro.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
