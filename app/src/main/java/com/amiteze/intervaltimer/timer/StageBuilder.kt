package com.amiteze.intervaltimer.timer

import com.amiteze.intervaltimer.model.Workout

/**
 * Turns a [Workout] into the flat list of stages the timer runs.
 *
 * Rules:
 * - Start/End are added only when enabled and longer than 0 s.
 * - Each block runs all its stages, in order, once per set.
 * - Rest stages at the very end of the last set of the last block are
 *   skipped (no point resting when the workout is over).
 * - Stages of 0 s are skipped.
 */
object StageBuilder {
    fun build(workout: Workout): List<Stage> {
        val stages = mutableListOf<Stage>()
        if (workout.warmupEnabled && workout.warmupSec > 0) {
            stages += Stage(StageType.START, workout.warmupSec)
        }

        val blocks = workout.blocks.filter { b -> b.sets > 0 && b.stages.any { it.sec > 0 } }
        blocks.forEachIndexed { bi, block ->
            val steps = block.stages.filter { it.sec > 0 }
            for (set in 1..block.sets) {
                val isLastSet = bi == blocks.lastIndex && set == block.sets
                // In the very last set, drop trailing rest stages.
                val run = if (isLastSet) steps.dropLastWhile { it.rest } else steps
                run.forEach { st ->
                    stages += Stage(
                        type = if (st.rest) StageType.REST else StageType.WORK,
                        durationSec = st.sec,
                        name = st.name.ifBlank { if (st.rest) "Rest" else "Work" },
                        block = bi + 1, blockCount = blocks.size, blockName = block.name,
                        set = set, setCount = block.sets,
                    )
                }
            }
        }

        if (workout.cooldownEnabled && workout.cooldownSec > 0) {
            stages += Stage(StageType.END, workout.cooldownSec)
        }
        return stages
    }

    /** Total length of a workout in seconds. */
    fun totalSec(workout: Workout): Int = build(workout).sumOf { it.durationSec }
}
