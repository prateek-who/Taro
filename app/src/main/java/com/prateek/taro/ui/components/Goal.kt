package com.prateek.taro.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prateek.taro.ui.theme.rememberAnimationsEnabled

@Composable
fun GlowingIcon(icon: Int, lit: Boolean, litColor: Color, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(32.dp)) {
        if (lit) {
            val pulse = if (rememberAnimationsEnabled()) {
                rememberInfiniteTransition(label = "icon pulse")
                    .animateFloat(1f, 1.12f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "icon scale")
            } else {
                null
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .graphicsLayer {
                        val scale = pulse?.value ?: 1f
                        scaleX = scale
                        scaleY = scale
                    }
                    .background(litColor.copy(alpha = 0.18f), CircleShape),
            )
        }
        Icon(painterResource(icon), contentDescription = null, tint = litColor, modifier = Modifier.size(24.dp))
    }
}
