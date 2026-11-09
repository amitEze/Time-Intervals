package com.amiteze.intervaltimer.service

import com.amiteze.intervaltimer.timer.Stage

/** Everything the run screen and notification need to show. Published by [TimerService]. */
data class RunState(
    /** True while a workout is loaded (running, paused or just finished). */
    val active: Boolean = false,
    val workoutName: String = "",
    val stage: Stage? = null,
    val nextStage: Stage? = null,
    val stageIndex: Int = 0,
    val stageCount: Int = 0,
    val stageRemainingMs: Long = 0,
    val totalRemainingMs: Long = 0,
    val paused: Boolean = false,
    val finished: Boolean = false,
)
