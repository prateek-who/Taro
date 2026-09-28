package com.nvllz.stepsy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nvllz.stepsy.ui.theme.StepsyTheme

@Composable
fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) onSurface else onSurface.copy(alpha = 0.12f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (selected) onSurface else StepsyTheme.colors.accent,
        ),
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        Text(label.uppercase(), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}
