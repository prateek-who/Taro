package com.prateek.taro.ui.components

import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme
import kotlinx.coroutines.launch

@Composable
fun TaroNavigationBar(tabs: List<Tab>, pagerState: PagerState) {
    val scope = rememberCoroutineScope()
    NavigationBar(containerColor = TaroTheme.colors.background, tonalElevation = 0.dp) {
        tabs.forEachIndexed { index, tab ->
            val options = tab.options
            NavigationBarItem(
                selected = pagerState.targetPage == index,
                onClick = { scope.launch { pagerState.animateScrollToPage(index, animationSpec = TaroMotion.snappy()) } },
                icon = { options.icon?.let { Icon(it, contentDescription = null) } },
                label = { Text(options.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = TaroTheme.colors.accentOpaque,
                    unselectedIconColor = TaroTheme.colors.accent,
                    unselectedTextColor = TaroTheme.colors.accent,
                ),
            )
        }
    }
}

val LocalTabActive = compositionLocalOf { true }

@Composable
fun SwipeableTabs(tabs: List<Tab>, pagerState: PagerState, modifier: Modifier = Modifier) {
    val tabNavigator = LocalTabNavigator.current
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { tabNavigator.current = tabs[it] }
    }
    HorizontalPager(state = pagerState, key = { tabs[it].key }, modifier = modifier) { page ->
        val tab = tabs[page]
        CompositionLocalProvider(LocalTabActive provides (pagerState.settledPage == page)) {
            tabNavigator.saveableState("tab", tab) { tab.Content() }
        }
    }
}
