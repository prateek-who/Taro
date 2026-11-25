package com.prateek.taro.ui.components

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.prateek.taro.ui.theme.TaroTheme

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Int? = null,
    tint: Color? = null,
) {
    val fill = tint?.copy(alpha = 0.16f) ?: MaterialTheme.colorScheme.onSurface
    val ink = tint ?: TaroTheme.colors.background
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = TaroTheme.spacing.loose + TaroTheme.spacing.tight),
        colors = ButtonDefaults.buttonColors(
            containerColor = fill,
            contentColor = ink,
            disabledContainerColor = fill.copy(alpha = 0.2f),
            disabledContentColor = ink.copy(alpha = 0.6f),
        ),
        modifier = modifier.heightIn(min = TaroTheme.spacing.wide * 2),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.padding(end = TaroTheme.spacing.tight).size(TaroTheme.spacing.loose + TaroTheme.spacing.hair))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = TaroTheme.spacing.loose + TaroTheme.spacing.tight),
        colors = ButtonDefaults.buttonColors(
            containerColor = TaroTheme.colors.accentOpaque,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = null,
        modifier = modifier.heightIn(min = TaroTheme.spacing.wide * 2),
    ) {
        if (icon != null) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.padding(end = TaroTheme.spacing.tight).size(TaroTheme.spacing.loose + TaroTheme.spacing.hair))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatRow(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, info: Info? = null) {
    val state = rememberInfoState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .infoClickable(state, info, onClick)
            .padding(horizontal = TaroTheme.spacing.loose, vertical = TaroTheme.spacing.gap + TaroTheme.spacing.hair),
    ) {
        InfoPopup(state, info, TaroTheme.colors.accent)
        Text(label, style = TaroTheme.text.meterLabel, color = TaroTheme.colors.accent, modifier = Modifier.weight(1f).padding(end = TaroTheme.spacing.gap))
        Text(value, style = TaroTheme.text.meterValue, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
}
