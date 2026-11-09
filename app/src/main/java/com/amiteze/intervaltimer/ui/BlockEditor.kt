package com.amiteze.intervaltimer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.amiteze.intervaltimer.model.Block
import com.amiteze.intervaltimer.model.BlockStage
import com.amiteze.intervaltimer.timer.StageType

/**
 * Editor for one block: name, number of sets, and its list of stages.
 * Each set runs the stages in order.
 */
@Composable
fun BlockCard(
    index: Int, count: Int, block: Block,
    onChange: (Block) -> Unit, onMove: (Int) -> Unit, onRemove: () -> Unit,
) {
    val setStages: (List<BlockStage>) -> Unit = { onChange(block.copy(stages = it)) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Block ${index + 1}", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = block.name,
                onValueChange = { onChange(block.copy(name = it)) },
                label = { Text("Block name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Sets")
            Stepper(block.sets, { onChange(block.copy(sets = it)) })

            Text("Stages (run in order, every set)")
            block.stages.forEachIndexed { j, st ->
                key(st.id) {
                    StageCard(
                        stage = st, index = j, count = block.stages.size,
                        onChange = { ns -> setStages(block.stages.map { if (it.id == ns.id) ns else it }) },
                        onMove = { dir -> setStages(block.stages.move(j, j + dir)) },
                        onRemove = { setStages(block.stages.filterNot { it.id == st.id }) },
                    )
                }
            }
            OutlinedButton(onClick = { setStages(block.stages + nextStage(block.stages.lastOrNull())) },
                modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("+  Add stage") }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onMove(-1) }, enabled = index > 0, modifier = Modifier.weight(1f).height(48.dp)) { Text("↑ Up") }
                OutlinedButton(onClick = { onMove(1) }, enabled = index < count - 1, modifier = Modifier.weight(1f).height(48.dp)) { Text("↓ Down") }
                OutlinedButton(onClick = onRemove, enabled = count > 1, modifier = Modifier.weight(1.3f).height(48.dp)) { Text("Remove block") }
            }
        }
    }
}

/** A sensible next stage: Rest after work, Work after rest. */
private fun nextStage(last: BlockStage?): BlockStage =
    if (last == null || last.rest) BlockStage(name = "Work", sec = 30) else BlockStage(name = "Rest", sec = 15, rest = true)

/** One stage: name, Work/Rest switch (sets the color), duration, reorder/remove. */
@Composable
private fun StageCard(
    stage: BlockStage, index: Int, count: Int,
    onChange: (BlockStage) -> Unit, onMove: (Int) -> Unit, onRemove: () -> Unit,
) {
    val color = stageColor(if (stage.rest) StageType.REST else StageType.WORK)
    OutlinedCard(Modifier.fillMaxWidth(), border = BorderStroke(2.dp, color)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = stage.name,
                    onValueChange = { onChange(stage.copy(name = it)) },
                    label = { Text("Stage ${index + 1} name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                // Tap to switch between Work (red) and Rest (green).
                Button(
                    onClick = { onChange(stage.copy(rest = !stage.rest)) },
                    colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(56.dp).width(84.dp),
                ) { Text(if (stage.rest) "Rest" else "Work") }
            }
            DurationField(stage.sec) { onChange(stage.copy(sec = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onMove(-1) }, enabled = index > 0, modifier = Modifier.weight(1f).height(48.dp)) { Text("↑") }
                OutlinedButton(onClick = { onMove(1) }, enabled = index < count - 1, modifier = Modifier.weight(1f).height(48.dp)) { Text("↓") }
                OutlinedButton(onClick = onRemove, enabled = count > 1, modifier = Modifier.weight(1f).height(48.dp)) { Text("Remove") }
            }
        }
    }
}
