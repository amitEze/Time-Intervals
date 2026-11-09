package com.amiteze.intervaltimer.timer

import com.amiteze.intervaltimer.model.Workout

/**
 * Turns a [Workout] into the flat list of stages the timer runs.
 *
 * Rules:
 * - Start/End are added only when enabled and longer than 0 s.
 * - Each block adds Work (and Rest) for every round.
 * - The rest after the very last round of the last block is skipped.
 * - Stages of 0 s are skipped (e.g. a block with no rest).
 */
object StageBuilder {
    fun build(workout: Workout): List<Stage> {
        val stages = mutableListOf<Stage>()
        if (workout.warmupEnabled && workout.warmupSec > 0) {
            stages += Stage(StageType.START, workout.warmupSec)
        }

        val blocks = workout.blocks.filter { it.rounds > 0 }
        blocks.forEachIndexed { b, block ->
            for (r in 1..block.rounds) {
                val isLastRoundOfLastBlock = b == blocks.lastIndex && r == block.rounds
                if (block.workSec > 0) {
                    stages += Stage(StageType.WORK, block.workSec, b + 1, blocks.size, r, block.rounds)
                }
                if (!isLastRoundOfLastBlock && block.restSec > 0) {
                    stages += Stage(StageType.REST, block.restSec, b + 1, blocks.size, r, block.rounds)
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
