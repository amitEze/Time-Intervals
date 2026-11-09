package com.amiteze.intervaltimer.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues

/**
 * A number box (max 2 digits). Keeps its own text so the user can clear it
 * while typing; reports the number (clamped to 0..[max]).
 */
@Composable
fun NumberField(value: Int, onChange: (Int) -> Unit, label: String, max: Int, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(value.toString()) }
    // If the value changes from outside (e.g. blocks reordered), show it.
    LaunchedEffect(value) { if ((text.toIntOrNull() ?: 0) != value) text = value.toString() }
    OutlinedTextField(
        value = text,
        onValueChange = { new ->
            val digits = new.filter { it.isDigit() }.take(2)
            val n = (digits.toIntOrNull() ?: 0).coerceIn(0, max)
            text = if ((digits.toIntOrNull() ?: 0) > max) n.toString() else digits
            onChange(n)
        },
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier.width(96.dp),
    )
}

/** Minutes + seconds input for a duration stored in seconds. */
@Composable
fun DurationField(seconds: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberField(seconds / 60, { m -> onChange(m * 60 + seconds % 60) }, "min", 99)
        Text(" : ", style = MaterialTheme.typography.headlineSmall)
        NumberField(seconds % 60, { s -> onChange((seconds / 60) * 60 + s) }, "sec", 59)
    }
}

/** Big − / + buttons around a number (used for sets). */
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int = 1, max: Int = 99) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(onClick = { onChange((value - 1).coerceAtLeast(min)) }, modifier = Modifier.size(56.dp), contentPadding = PaddingValues(0.dp)) {
            Text("−", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp).padding(horizontal = 4.dp),
        )
        FilledTonalButton(onClick = { onChange((value + 1).coerceAtMost(max)) }, modifier = Modifier.size(56.dp), contentPadding = PaddingValues(0.dp)) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.width(8.dp))
    }
}
