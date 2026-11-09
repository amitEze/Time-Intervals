package com.amiteze.intervaltimer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amiteze.intervaltimer.model.Workout
import com.amiteze.intervaltimer.timer.StageBuilder
import com.amiteze.intervaltimer.timer.formatMmSs

/** Home screen: the saved workouts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutListScreen(vm: MainViewModel) {
    val workouts by vm.workouts.collectAsStateWithLifecycle()
    val run by vm.run.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<Workout?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interval Timer") },
                actions = { TextButton(onClick = { vm.go(Screen.SETTINGS) }) { Text("Settings") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = vm::newWorkout) { Text("+  New workout") }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (run.active) {
                item {
                    // Banner to get back to a workout that is still running.
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { vm.go(Screen.RUN) },
                        colors = CardDefaults.cardColors(containerColor = stageColor(run.stage?.type), contentColor = Color.White),
                    ) {
                        Text(
                            "▶  ${run.workoutName}: ${run.stage?.name ?: "Done"} ${formatMmSs(run.stageRemainingMs)} — tap to open",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            if (workouts.isEmpty()) {
                item { Text("No workouts yet. Tap “New workout”.", modifier = Modifier.padding(16.dp)) }
            }
            items(workouts, key = { it.id }) { w ->
                WorkoutCard(
                    w,
                    onStart = { vm.start(w) },
                    onEdit = { vm.edit(w) },
                    onCopy = { vm.duplicate(w) },
                    onDelete = { toDelete = w },
                )
            }
        }
    }

    toDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete “${w.name}”?") },
            confirmButton = { TextButton(onClick = { vm.delete(w); toDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WorkoutCard(w: Workout, onStart: () -> Unit, onEdit: () -> Unit, onCopy: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Column(Modifier.padding(16.dp)) {
            Text(w.name, style = MaterialTheme.typography.titleLarge)
            Text(summary(w), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("▶  Start", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).height(48.dp)) { Text("Edit") }
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f).height(48.dp)) { Text("Copy") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f).height(48.dp)) { Text("Delete") }
            }
        }
    }
}

/** e.g. "5× (Fight 3:00, Rest 1:00), 8× (Burpees 0:30, Rest 0:15) · total 30:45" */
private fun summary(w: Workout): String {
    fun t(sec: Int) = "%d:%02d".format(sec / 60, sec % 60)
    val blocks = w.blocks.joinToString(", ") { b ->
        "${b.sets}× (" + b.stages.joinToString(", ") { "${it.name.ifBlank { if (it.rest) "Rest" else "Work" }} ${t(it.sec)}" } + ")"
    }
    return "$blocks · total ${formatMmSs(StageBuilder.totalSec(w) * 1000L)}"
}
