package com.nvllz.stepsy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.R
import com.nvllz.stepsy.ui.theme.StepsyMotion
import com.nvllz.stepsy.ui.theme.StepsyTheme

private data class MenuEntry(val title: Int, val hint: Int, val icon: Int, val color: Color, val screen: Screen)

@Composable
fun AppOverflowMenu() {
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) 90f else 0f, StepsyMotion.snappy(), label = "menu icon")
    val colors = StepsyTheme.colors
    val entries = listOf(
        MenuEntry(R.string.achievements_title, R.string.menu_achievements_hint, R.drawable.ic_small_trophy, colors.special, AchievementsScreen),
        MenuEntry(R.string.daily_goals, R.string.menu_goals_hint, R.drawable.ic_small_target, colors.goal, DailyGoalsScreen),
        MenuEntry(R.string.header_data_backup, R.string.menu_backup_hint, R.drawable.ic_small_backup, colors.flame, BackupScreen),
        MenuEntry(R.string.settings, R.string.menu_settings_hint, R.drawable.ic_small_settings, MaterialTheme.colorScheme.onSurface, SettingsScreen),
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
        Box(modifier = Modifier.padding(top = 4.dp, end = 12.dp, bottom = 8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.accentOpaque)
                    .clickable(role = Role.Button) { open = true },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description),
                    modifier = Modifier.rotate(rotation),
                )
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = colors.dialogSurface,
                shadowElevation = 12.dp,
                modifier = Modifier.width(280.dp),
            ) {
                entries.forEachIndexed { index, entry ->
                    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                    AnimatedVisibility(
                        visibleState = visible,
                        enter = fadeIn(tween(220, index * 45)) + slideInHorizontally(tween(320, index * 45)) { it / 5 },
                    ) {
                        MenuRow(entry) {
                            open = false
                            rootNavigator.push(entry.screen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuRow(entry: MenuEntry, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(entry.color.copy(alpha = 0.14f)),
        ) {
            Icon(painterResource(entry.icon), contentDescription = null, tint = entry.color, modifier = Modifier.size(22.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
        ) {
            Text(stringResource(entry.title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(stringResource(entry.hint), fontSize = 12.sp, color = StepsyTheme.colors.accent)
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = StepsyTheme.colors.accent,
            modifier = Modifier.size(18.dp),
        )
    }
}
