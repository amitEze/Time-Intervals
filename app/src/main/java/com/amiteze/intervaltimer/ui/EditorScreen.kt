package com.amiteze.intervaltimer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amiteze.intervaltimer.model.Block
import com.amiteze.intervaltimer.model.Workout
import com.amiteze.intervaltimer.timer.StageBuilder
import com.amiteze.intervaltimer.timer.formatMmSs

/** Create / edit a workout: name, Start, blocks, End. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: MainViewModel) {
    val draft by vm.draft.collectAsStateWithLifecycle()
    val w = draft ?: return
    val set: (Workout) -> Unit = vm::updateDraft

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit workout") },
                navigationIcon = { TextButton(onClick = vm::cancelEdit) { Text("Cancel") } },
                actions = { TextButton(onClick = vm::saveDraft) { Text("Save") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding()
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = w.name,
                onValueChange = { set(w.copy(name = it)) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Total: ${formatMmSs(StageBuilder.totalSec(w) * 1000L)}", style = MaterialTheme.typography.titleMedium)

            OptionalStage("Start (warm-up)", w.warmupEnabled, w.warmupSec,
                onEnabled = { set(w.copy(warmupEnabled = it)) }, onSeconds = { set(w.copy(warmupSec = it)) })

            w.blocks.forEachIndexed { i, block ->
                key(block.id) {
                    BlockCard(
                        index = i, count = w.blocks.size, block = block,
                        onChange = { nb -> set(w.copy(blocks = w.blocks.map { if (it.id == nb.id) nb else it })) },
                        onMove = { dir -> set(w.copy(blocks = w.blocks.move(i, i + dir))) },
                        onRemove = { set(w.copy(blocks = w.blocks.filterNot { it.id == block.id })) },
                    )
                }
            }
            OutlinedButton(onClick = { vm.addBlock() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("+  Add block")
            }

            OptionalStage("End (cool-down)", w.cooldownEnabled, w.cooldownSec,
                onEnabled = { set(w.copy(cooldownEnabled = it)) }, onSeconds = { set(w.copy(cooldownSec = it)) })

            Button(onClick = vm::saveDraft, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Save") }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Start or End: an on/off switch plus a duration. */
@Composable
private fun OptionalStage(title: String, enabled: Boolean, seconds: Int, onEnabled: (Boolean) -> Unit, onSeconds: (Int) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = enabled, onCheckedChange = onEnabled)
            }
            if (enabled) {
                Spacer(Modifier.height(8.dp))
                DurationField(seconds, onSeconds)
            }
        }
    }
}

@Composable
private fun BlockCard(
    index: Int, count: Int, block: Block,
    onChange: (Block) -> Unit, onMove: (Int) -> Unit, onRemove: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Block ${index + 1}", style = MaterialTheme.typography.titleMedium)
            Text("Work")
            DurationField(block.workSec) { onChange(block.copy(workSec = it)) }
            Text("Rest")
            DurationField(block.restSec) { onChange(block.copy(restSec = it)) }
            Text("Rounds")
            Stepper(block.rounds, { onChange(block.copy(rounds = it)) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onMove(-1) }, enabled = index > 0, modifier = Modifier.weight(1f).height(48.dp)) { Text("↑ Up") }
                OutlinedButton(onClick = { onMove(1) }, enabled = index < count - 1, modifier = Modifier.weight(1f).height(48.dp)) { Text("↓ Down") }
                OutlinedButton(onClick = onRemove, enabled = count > 1, modifier = Modifier.weight(1f).height(48.dp)) { Text("Remove") }
            }
        }
    }
}

/** Returns a copy of the list with the item at [from] moved to [to]. */
private fun <T> List<T>.move(from: Int, to: Int): List<T> {
    if (to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
