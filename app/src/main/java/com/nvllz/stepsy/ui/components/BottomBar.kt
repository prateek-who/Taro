package com.nvllz.stepsy.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import com.nvllz.stepsy.ui.theme.StepsyTheme

@Composable
fun StepsyNavigationBar(tabs: List<Tab>) {
    val tabNavigator = LocalTabNavigator.current
    NavigationBar(containerColor = StepsyTheme.colors.background, tonalElevation = 0.dp) {
        tabs.forEach { tab ->
            val options = tab.options
            NavigationBarItem(
                selected = tabNavigator.current.key == tab.key,
                onClick = { tabNavigator.current = tab },
                icon = { options.icon?.let { Icon(it, contentDescription = null) } },
                label = { Text(options.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = StepsyTheme.colors.accentOpaque,
                    unselectedIconColor = StepsyTheme.colors.accent,
                    unselectedTextColor = StepsyTheme.colors.accent,
                ),
            )
        }
    }
}
