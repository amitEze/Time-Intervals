package com.amiteze.intervaltimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.amiteze.intervaltimer.service.TimerService
import com.amiteze.intervaltimer.timer.formatMmSs
import com.amiteze.intervaltimer.timer.progressText
import androidx.compose.ui.text.style.TextAlign

/** Full-screen countdown. The background color follows the stage. */
@Composable
fun RunScreen(vm: MainViewModel) {
    val s by vm.run.collectAsStateWithLifecycle()
    val stage = s.stage
    val running = s.active && !s.finished

    // Keep the screen on while the workout runs.
    val view = LocalView.current
    DisposableEffect(running) {
        view.keepScreenOn = running
        onDispose { view.keepScreenOn = false }
    }

    val white = Color.White
    Column(
        modifier = Modifier.fillMaxSize().background(stageColor(stage?.type)).systemBarsPadding().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(s.workoutName, color = white, style = MaterialTheme.typography.titleMedium)

        if (!running) {
            Spacer(Modifier.weight(1f))
            Text(if (s.finished) "Done!" else "Not running", color = white, fontSize = 56.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Button(onClick = vm::closeRun, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text("Close") }
            return@Column
        }

        Spacer(Modifier.height(16.dp))
        Text(
            (stage?.name ?: "") + if (s.paused) " (paused)" else "",
            color = white, fontSize = 44.sp, fontWeight = FontWeight.Bold,
        )
        if (stage != null && stage.set > 0) {
            Text(stage.progressText(), color = white, fontSize = 22.sp, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.weight(1f))
        Text(
            formatMmSs(s.stageRemainingMs),
            color = white, fontSize = 104.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, maxLines = 1,
        )
        Text("Total left  ${formatMmSs(s.totalRemainingMs)}", color = white, fontSize = 22.sp)
        s.nextStage?.let { Text("Next: ${it.name} ${formatMmSs(it.durationMs)}", color = white.copy(alpha = 0.8f), fontSize = 18.sp) }
        Spacer(Modifier.weight(1f))

        val light = ButtonDefaults.buttonColors(containerColor = white, contentColor = Color.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { vm.timerAction(TimerService.ACTION_PREVIOUS) }, colors = light,
                modifier = Modifier.weight(1f).height(72.dp)) { Text("⏮ Prev") }
            Button(onClick = { vm.timerAction(TimerService.ACTION_PAUSE_RESUME) }, colors = light,
                modifier = Modifier.weight(1.4f).height(72.dp)) {
                Text(if (s.paused) "▶ Resume" else "⏸ Pause", fontSize = 20.sp)
            }
            Button(onClick = { vm.timerAction(TimerService.ACTION_SKIP) }, colors = light,
                modifier = Modifier.weight(1f).height(72.dp)) { Text("Skip ⏭") }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { vm.go(Screen.LIST) }, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("Back", color = white)
            }
            OutlinedButton(onClick = { vm.timerAction(TimerService.ACTION_STOP) }, modifier = Modifier.weight(1f).height(56.dp)) {
                Text("■ Stop", color = white)
            }
        }
    }
}
