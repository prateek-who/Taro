package com.prateek.taro.ui

import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.foundation.layout.heightIn
import com.prateek.taro.ui.components.IconDisc
import com.prateek.taro.ui.components.BentoTile
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.prateek.taro.R
import androidx.compose.material3.IconButton
import com.prateek.taro.ui.theme.TaroMotion
import com.prateek.taro.ui.theme.TaroTheme

data class MenuEntry(val title: Int, val hint: Int, val icon: Int, val color: Color, val onClick: () -> Unit)

@Composable
fun AppOverflowMenu(center: (@Composable () -> Unit)? = null, actions: List<MenuEntry> = emptyList(), back: Boolean = false, title: String? = null, trailing: (@Composable () -> Unit)? = null, onBack: (() -> Unit)? = null) {
    val navigator = LocalNavigator.currentOrThrow

    val spacing = TaroTheme.spacing
    val padding = Modifier
        .fillMaxWidth()
        .padding(start = spacing.gap, top = spacing.hair, end = spacing.gap, bottom = spacing.tight)
        .heightIn(min = LocalMinimumInteractiveComponentSize.current)
    val plus: @Composable (Modifier) -> Unit = { modifier ->
        if (actions.isNotEmpty()) MenuButton(R.drawable.ic_add, stringResource(R.string.quick_add), actions, openRotation = 45f, modifier = modifier)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = padding) {
        if (back) BackButton { onBack?.invoke() ?: navigator.pop() }
        title?.let {
            Text(
                text = it,
                style = TaroTheme.text.greeting,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = center == null).padding(start = if (back) spacing.hair else spacing.tight),
            )
        }
        if (center != null || title == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).padding(horizontal = spacing.hair)) { center?.invoke() }
        }
        trailing?.invoke()
        plus(Modifier)
        if (back && center != null && trailing == null && actions.isEmpty()) Spacer(Modifier.size(LocalMinimumInteractiveComponentSize.current))
    }
}

@Composable
private fun BackButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(androidx.appcompat.R.string.abc_action_bar_up_description), tint = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MenuButton(icon: Int, description: String, entries: List<MenuEntry>, openRotation: Float, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (open) openRotation else 0f, TaroMotion.snappy(), label = "menu icon")
    val colors = TaroTheme.colors
    Box(modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.accentOpaque)
                .clickable(role = Role.Button) { open = true },
        ) {
            Icon(painter = painterResource(icon), contentDescription = description, modifier = Modifier.rotate(rotation))
        }
        MenuDropdown(open, onDismiss = { open = false }, entries = entries)
    }
}

@Composable
fun MenuDropdown(open: Boolean, onDismiss: () -> Unit, entries: List<MenuEntry>) {
    val spacing = TaroTheme.spacing
    DropdownMenu(
        expanded = open,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(spacing.loose + spacing.tight),
        containerColor = TaroTheme.colors.dialogSurface,
        shadowElevation = spacing.gap,
        modifier = Modifier.fillMaxWidth(MENU_SHARE),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.padding(horizontal = spacing.tight)) {
            entries.chunked(MENU_COLUMNS).forEachIndexed { rowIndex, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.tight), modifier = Modifier.height(IntrinsicSize.Min)) {
                    row.forEachIndexed { column, entry ->
                        val index = rowIndex * MENU_COLUMNS + column
                        val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                        AnimatedVisibility(
                            visibleState = visible,
                            enter = fadeIn(tween(MENU_FADE_MS, index * MENU_STAGGER_MS)) + scaleIn(TaroMotion.bouncy(), initialScale = MENU_START_SCALE),
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            MenuTile(entry) {
                                onDismiss()
                                entry.onClick()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuTile(entry: MenuEntry, onClick: () -> Unit) {
    val spacing = TaroTheme.spacing
    BentoTile(entry.color, tinted = true, onClick = onClick, padding = spacing.gap, modifier = Modifier.fillMaxSize()) {
        IconDisc(entry.icon, entry.color)
        Text(stringResource(entry.title), style = TaroTheme.text.title, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = spacing.tight))
        Text(stringResource(entry.hint), style = TaroTheme.text.note, color = TaroTheme.colors.accent, maxLines = MENU_HINT_LINES, overflow = TextOverflow.Ellipsis)
    }
}

private const val MENU_SHARE = 0.86f
private const val MENU_COLUMNS = 2
private const val MENU_FADE_MS = 200
private const val MENU_STAGGER_MS = 40
private const val MENU_START_SCALE = 0.85f
private const val MENU_HINT_LINES = 2
