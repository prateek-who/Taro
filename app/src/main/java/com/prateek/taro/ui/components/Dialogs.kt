package com.prateek.taro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.prateek.taro.ui.theme.TaroTheme
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.prateek.taro.R
import com.prateek.taro.ui.theme.TaroMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaroDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    dismissText: String? = stringResource(android.R.string.cancel),
    confirmEnabled: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, TaroMotion.snappy()) }

    BasicAlertDialog(onDismissRequest = onDismiss, properties = properties) {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    alpha = entrance.value.coerceIn(0f, 1f)
                    scaleX = 0.92f + 0.08f * entrance.value
                    scaleY = scaleX
                }
                .clip(RoundedCornerShape(28.dp))
                .background(TaroTheme.colors.dialogSurface)
                .padding(24.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Box(modifier = Modifier.weight(1f, fill = false)) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        content()
                    }
                }
            }
            if (confirmText != null || dismissText != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                ) {
                    if (dismissText != null) {
                        SecondaryButton(text = dismissText, onClick = onDismiss, modifier = Modifier.weight(1f))
                    }
                    if (confirmText != null) {
                        PrimaryButton(text = confirmText, onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun SingleChoiceDialog(
    title: String,
    entries: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    TaroDialog(title = title, onDismiss = onDismiss) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            entries.forEachIndexed { index, entry ->
                val selected = index == selectedIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) TaroTheme.colors.accentOpaque else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Text(
                        text = entry,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) {
                        Icon(painterResource(R.drawable.ic_tick), contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(android.R.string.ok),
) {
    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = confirmText,
        onConfirm = onConfirm,
    ) {
        Text(message)
    }
}

@Composable
fun taroTextFieldColors(): TextFieldColors {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accent = TaroTheme.colors.accent
    val fill = TaroTheme.colors.accentOpaque
    return TextFieldDefaults.colors(
        focusedContainerColor = fill,
        unfocusedContainerColor = fill,
        disabledContainerColor = fill,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        focusedLabelColor = accent,
        unfocusedLabelColor = accent,
        cursorColor = onSurface,
        selectionColors = TextSelectionColors(handleColor = onSurface, backgroundColor = accent.copy(alpha = 0.4f)),
    )
}

private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun TaroTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = FieldShape,
        colors = taroTextFieldColors(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier,
    )
}

@Composable
fun NumberField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    suffix: String? = null,
    large: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = { input ->
            val allowed: (Char) -> Boolean = if (decimal) { c -> c.isDigit() || c == '.' || c == ',' } else Char::isDigit
            onValueChange(input.copy(text = input.text.filter(allowed)))
        },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it, color = TaroTheme.colors.accent) } },
        singleLine = true,
        shape = FieldShape,
        colors = taroTextFieldColors(),
        textStyle = if (large) MaterialTheme.typography.headlineMedium else LocalTextStyle.current,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        modifier = modifier,
    )
}

fun String.selectedAll() = TextFieldValue(this, TextRange(length))

@Composable
fun NumberInputDialog(
    title: String,
    initial: String,
    hint: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    decimal: Boolean = false,
    supportingText: String? = null,
) {
    var value by remember { mutableStateOf(initial.selectedAll()) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm(value.text.trim()) },
    ) {
        Column {
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
            }
            NumberField(
                value = value,
                onValueChange = { value = it },
                label = hint,
                decimal = decimal,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
    }
}

@Composable
fun FeetInchesDialog(
    title: String,
    initialFeet: Int,
    initialInches: Int,
    onConfirm: (feet: Int, inches: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var feet by remember { mutableStateOf(initialFeet.toString().selectedAll()) }
    var inches by remember { mutableStateOf(initialInches.toString().selectedAll()) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm(feet.text.toIntOrNull() ?: 0, inches.text.toIntOrNull() ?: 0) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            NumberField(feet, { feet = it }, "ft", decimal = false, modifier = Modifier.weight(1f))
            NumberField(
                inches,
                { inches = it },
                "in",
                decimal = false,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
        }
    }
}

@Composable
fun HtmlDialog(title: String, html: String, onDismiss: () -> Unit) {
    val linkColor = TaroTheme.colors.accent
    val text = remember(html, linkColor) {
        AnnotatedString.fromHtml(html, linkStyles = TextLinkStyles(SpanStyle(color = linkColor)))
    }
    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = onDismiss,
        dismissText = null,
    ) {
        Text(text, modifier = Modifier.verticalScroll(rememberScrollState()))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    is24Hour: Boolean = true,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = is24Hour)
    TaroDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = { onConfirm(state.hour, state.minute) },
    ) {
        TimePicker(
            state = state,
            colors = TimePickerDefaults.colors(
                clockDialColor = TaroTheme.colors.accentOpaque,
                clockDialSelectedContentColor = TaroTheme.colors.background,
                selectorColor = MaterialTheme.colorScheme.onSurface,
                timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.onSurface,
                timeSelectorSelectedContentColor = TaroTheme.colors.background,
                timeSelectorUnselectedContainerColor = TaroTheme.colors.accentOpaque,
                periodSelectorBorderColor = Color.Transparent,
                periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.onSurface,
                periodSelectorSelectedContentColor = TaroTheme.colors.background,
                periodSelectorUnselectedContainerColor = TaroTheme.colors.accentOpaque,
            ),
        )
    }
}
