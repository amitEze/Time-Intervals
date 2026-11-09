package com.amiteze.intervaltimer.timer

/** The kind of stage; the run screen picks its color from this. */
enum class StageType(val label: String) {
    START("Start"),
    WORK("Work"),
    REST("Rest"),
    END("End"),
}

/**
 * One continuous countdown in a workout.
 * [name] is what the run screen shows (e.g. "Jab-cross").
 * [block] and [set] are 1-based; they are 0 for Start and End stages.
 */
data class Stage(
    val type: StageType,
    val durationSec: Int,
    val name: String = type.label,
    val block: Int = 0,
    val blockCount: Int = 0,
    val blockName: String = "",
    val set: Int = 0,
    val setCount: Int = 0,
) {
    val durationMs: Long get() = durationSec * 1000L
}

/** Formats milliseconds as MM:SS, rounding up so "00:00" only shows at the very end. */
fun formatMmSs(ms: Long): String {
    val totalSec = ((ms.coerceAtLeast(0) + 999) / 1000)
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

/** e.g. "Set 3/5 · Boxing 1/2", or "" for Start/End. */
fun Stage.progressText(): String {
    if (set == 0) return ""
    val block = if (blockCount > 1 || blockName.isNotBlank()) {
        " · " + blockName.ifBlank { "Block" } + if (blockCount > 1) " $block/$blockCount" else ""
    } else ""
    return "Set $set/$setCount$block"
}
