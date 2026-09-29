package com.nvllz.stepsy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.stepsy.ui.theme.StepsyTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RangeChip(label: String, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val fill by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onSurface else StepsyTheme.colors.accentOpaque,
        label = "chip fill",
    )
    val ink by animateColorAsState(
        if (selected) StepsyTheme.colors.background else MaterialTheme.colorScheme.onSurface,
        label = "chip ink",
    )
    Text(
        text = label,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = ink,
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .clip(CircleShape)
            .background(fill)
            .combinedClickable(role = Role.RadioButton, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}
