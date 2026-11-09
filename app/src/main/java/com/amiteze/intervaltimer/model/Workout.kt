package com.amiteze.intervaltimer.model

import java.util.UUID

/**
 * One timed step inside a block, e.g. "Jab-cross 0:30" or "Rest 0:15".
 * [rest] only picks the color (green instead of red) and lets the very last
 * rest of a workout be skipped.
 */
data class BlockStage(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Work",
    val sec: Int = 30,
    val rest: Boolean = false,
)

/**
 * A block runs its stages in order, [sets] times.
 * Example: 5 sets of [Work 3:00, Rest 1:00].
 */
data class Block(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sets: Int = 8,
    val stages: List<BlockStage> = listOf(
        BlockStage(name = "Work", sec = 30),
        BlockStage(name = "Rest", sec = 15, rest = true),
    ),
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

/** Copy with fresh ids (used by "Copy" and "Add block"). */
fun Block.withNewIds(): Block = copy(
    id = UUID.randomUUID().toString(),
    stages = stages.map { it.copy(id = UUID.randomUUID().toString()) },
)
