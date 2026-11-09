package com.amiteze.intervaltimer.ui

import androidx.compose.ui.graphics.Color
import com.amiteze.intervaltimer.timer.StageType

/** Background color of the run screen for each stage. Change them here. */
fun stageColor(type: StageType?): Color = when (type) {
    StageType.START -> Color(0xFF1E88E5) // blue
    StageType.WORK -> Color(0xFFE53935)  // red
    StageType.REST -> Color(0xFF43A047)  // green
    StageType.END -> Color(0xFF8E24AA)   // purple
    null -> Color(0xFF37474F)            // finished: dark grey
}
