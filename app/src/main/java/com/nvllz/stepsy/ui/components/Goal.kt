package com.nvllz.stepsy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nvllz.stepsy.ui.theme.StepsyTheme
import com.nvllz.stepsy.util.Util

@Composable
fun GoalProgressBar(steps: Int, goal: Int, modifier: Modifier = Modifier) {
    if (goal <= 0) return
    val met = steps >= goal
    val fraction by animateFloatAsState((steps.toFloat() / goal).coerceIn(0f, 1f), tween(600), label = "goal progress")
    val fill by animateColorAsState(if (met) StepsyTheme.colors.goal else StepsyTheme.colors.accent, tween(600), label = "goal color")
    val multiplier = Util.goalMultiplier(steps, goal)

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(StepsyTheme.colors.accentOpaque)
                .semantics { contentDescription = "${(steps * 100L / goal)}%" },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(fill),
            )
        }
        if (multiplier != null) {
            Text(
                text = multiplier,
                color = StepsyTheme.colors.goal,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clip(CircleShape)
                    .background(StepsyTheme.colors.goal.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun GlowingIcon(icon: Int, lit: Boolean, litColor: Color, modifier: Modifier = Modifier) {
    val pulse = if (lit) {
        val transition = rememberInfiniteTransition(label = "icon pulse")
        transition.animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "icon scale").value
    } else {
        1f
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(32.dp)) {
        if (lit) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .scale(pulse)
                    .background(litColor.copy(alpha = 0.18f), CircleShape),
            )
        }
        Icon(painterResource(icon), contentDescription = null, tint = litColor, modifier = Modifier.size(24.dp))
    }
}
