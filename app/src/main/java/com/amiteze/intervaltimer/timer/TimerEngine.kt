package com.amiteze.intervaltimer.timer

/**
 * Pure timer logic, no Android code, so it can be unit tested.
 *
 * All times are milliseconds from a monotonic clock
 * (SystemClock.elapsedRealtime() in the app). We never add up ticks: the
 * remaining time is always computed from "now", so it cannot drift even if
 * ticks are late (e.g. when the screen is off).
 */
class TimerEngine(val stages: List<Stage>) {

    /** Index of the current stage. Equals stages.size when finished. */
    var index: Int = 0
        private set

    /** Clock time at which the current stage (virtually) started. */
    private var stageStart: Long = 0L

    /** Clock time when paused, or null while running. */
    private var pausedAt: Long? = null

    val isPaused: Boolean get() = pausedAt != null
    val isFinished: Boolean get() = index >= stages.size
    val currentStage: Stage? get() = stages.getOrNull(index)

    fun start(now: Long) {
        index = 0
        stageStart = now
        pausedAt = null
    }

    fun pause(now: Long) {
        if (pausedAt == null && !isFinished) pausedAt = now
    }

    fun resume(now: Long) {
        val p = pausedAt ?: return
        stageStart += now - p // shift the start by the time spent paused
        pausedAt = null
    }

    /**
     * Moves forward through every stage whose time is up.
     * Returns how many stages ended (0 most of the time).
     */
    fun update(now: Long): Int {
        if (isPaused) return 0
        var ended = 0
        while (!isFinished && now - stageStart >= stages[index].durationMs) {
            stageStart += stages[index].durationMs // exact, no drift
            index++
            ended++
        }
        return ended
    }

    /** Jumps to the start of the next stage (or finishes). */
    fun skip(now: Long) {
        if (isFinished) return
        jumpTo(index + 1, now)
    }

    /**
     * Goes back: restarts the current stage if more than 3 s of it have run,
     * otherwise jumps to the start of the previous stage.
     */
    fun previous(now: Long) {
        if (isFinished) {
            jumpTo(stages.lastIndex, now)
            return
        }
        val target = if (elapsedInStage(now) > 3000 || index == 0) index else index - 1
        jumpTo(target, now)
    }

    private fun jumpTo(target: Int, now: Long) {
        index = target.coerceIn(0, stages.size)
        stageStart = now
        if (pausedAt != null) pausedAt = now // stay paused at the start of the stage
    }

    private fun elapsedInStage(now: Long): Long = (pausedAt ?: now) - stageStart

    /** Milliseconds left in the current stage. */
    fun stageRemainingMs(now: Long): Long {
        val stage = currentStage ?: return 0
        return (stage.durationMs - elapsedInStage(now)).coerceIn(0, stage.durationMs)
    }

    /** Milliseconds left in the whole workout. */
    fun totalRemainingMs(now: Long): Long {
        if (isFinished) return 0
        val later = stages.drop(index + 1).sumOf { it.durationMs }
        return stageRemainingMs(now) + later
    }
}
