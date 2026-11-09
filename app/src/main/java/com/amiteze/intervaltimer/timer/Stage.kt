package com.amiteze.intervaltimer.timer

/** The kind of stage; the run screen picks its color and label from this. */
enum class StageType(val label: String) {
    START("Start"),
    WORK("Work"),
    REST("Rest"),
    END("End"),
}

/**
 * One continuous countdown in a workout.
 * [block] and [round] are 1-based; they are 0 for Start and End stages.
 */
data class Stage(
    val type: StageType,
    val durationSec: Int,
    val block: Int = 0,
    val blockCount: Int = 0,
    val round: Int = 0,
    val roundCount: Int = 0,
) {
    val durationMs: Long get() = durationSec * 1000L
}

/** Formats milliseconds as MM:SS, rounding up so "00:00" only shows at the very end. */
fun formatMmSs(ms: Long): String {
    val totalSec = ((ms.coerceAtLeast(0) + 999) / 1000)
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}
