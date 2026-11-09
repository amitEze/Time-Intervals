package com.amiteze.intervaltimer.model

import java.util.UUID

/**
 * One block of intervals, e.g. "5 rounds of 3 min work / 1 min rest".
 * All durations are in seconds.
 */
data class Block(
    val id: String = UUID.randomUUID().toString(),
    val workSec: Int = 30,
    val restSec: Int = 15,
    val rounds: Int = 8,
)

/**
 * A saved workout: optional warm-up ("Start"), one or more blocks that run
 * in order, and an optional cool-down ("End").
 */
data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New workout",
    val warmupEnabled: Boolean = false,
    val warmupSec: Int = 60,
    val blocks: List<Block> = listOf(Block()),
    val cooldownEnabled: Boolean = false,
    val cooldownSec: Int = 60,
)
