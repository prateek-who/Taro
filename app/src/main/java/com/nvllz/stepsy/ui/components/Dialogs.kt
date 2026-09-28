package com.nvllz.stepsy.ui.components

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nvllz.stepsy.ui.theme.StepsyTheme

@Composable
fun StepsyDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    onConfirm: () -> Unit = {},
    dismissText: String? = stringResource(android.R.string.cancel),
    confirmEnabled: Boolean = true,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = properties,
        containerColor = StepsyTheme.colors.dialogSurface,
        title = { Text(title) },
        text = content,
        confirmButton = {
            if (confirmText != null) {
                TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                    Text(
                        text = confirmText,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (confirmEnabled) 1f else 0.38f),
                    )
                }
            }
        },
        dismissButton = {
            if (dismissText != null) {
                TextButton(onClick = onDismiss) {
                    Text(dismissText, color = StepsyTheme.colors.accent)
                }
            }
        },
    )
}

@Composable
fun SingleChoiceDialog(
    title: String,
    entries: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    StepsyDialog(title = title, onDismiss = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            entries.forEachIndexed { index, entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onSurface),
                    )
                    Text(entry, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun MessageDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    StepsyDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = onConfirm,
    ) {
        Text(message)
    }
}

@Composable
fun stepsyTextFieldColors(): TextFieldColors {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val accent = StepsyTheme.colors.accent
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = onSurface,
        unfocusedBorderColor = accent,
        focusedLabelColor = onSurface,
        unfocusedLabelColor = accent,
        cursorColor = onSurface,
        selectionColors = TextSelectionColors(handleColor = onSurface, backgroundColor = accent.copy(alpha = 0.4f)),
    )
}

@Composable
fun NumberField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val allowed: (Char) -> Boolean = if (decimal) { c -> c.isDigit() || c == '.' || c == ',' } else Char::isDigit
            onValueChange(input.copy(text = input.text.filter(allowed)))
        },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = stepsyTextFieldColors(),
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

    StepsyDialog(
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

    StepsyDialog(
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
    val linkColor = StepsyTheme.colors.accent
    val text = remember(html, linkColor) {
        AnnotatedString.fromHtml(html, linkStyles = TextLinkStyles(SpanStyle(color = linkColor)))
    }
    StepsyDialog(
        title = title,
        onDismiss = onDismiss,
        confirmText = stringResource(android.R.string.ok),
        onConfirm = onDismiss,
        dismissText = null,
    ) {
        Text(text, modifier = Modifier.verticalScroll(rememberScrollState()))
    }
}
