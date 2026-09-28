package com.nvllz.stepsy.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.nvllz.stepsy.R

@Composable
fun AppOverflowMenu() {
    val navigator = LocalNavigator.currentOrThrow
    val rootNavigator = navigator.parent ?: navigator
    var open by remember { mutableStateOf(false) }
    val items = listOf(
        Triple(R.string.achievements_title, R.drawable.ic_small_trophy, AchievementsScreen),
        Triple(R.string.header_data_backup, R.drawable.ic_small_backup, BackupScreen),
        Triple(R.string.daily_goals, R.drawable.ic_small_target, DailyGoalsScreen),
        Triple(R.string.settings, R.drawable.ic_small_settings, SettingsScreen),
    )

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
        Box {
            IconButton(onClick = { open = true }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more_vert),
                    contentDescription = stringResource(androidx.appcompat.R.string.abc_action_menu_overflow_description),
                )
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                items.forEach { (label, icon, screen) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        leadingIcon = {
                            Icon(painterResource(icon), contentDescription = null)
                        },
                        onClick = {
                            open = false
                            rootNavigator.push(screen)
                        },
                    )
                }
            }
        }
    }
}
