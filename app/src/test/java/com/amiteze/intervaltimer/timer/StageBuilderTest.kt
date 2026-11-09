package com.amiteze.intervaltimer.timer

import com.amiteze.intervaltimer.model.Block
import com.amiteze.intervaltimer.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Test

class StageBuilderTest {

    private val twoBlocks = Workout(
        warmupEnabled = true, warmupSec = 60,
        blocks = listOf(Block(workSec = 180, restSec = 60, rounds = 5), Block(workSec = 30, restSec = 15, rounds = 8)),
        cooldownEnabled = true, cooldownSec = 120,
    )

    @Test
    fun buildsStagesInOrder() {
        val stages = StageBuilder.build(twoBlocks)
        // 1 start + (5 work + 5 rest) + (8 work + 7 rest) + 1 end
        assertEquals(1 + 10 + 15 + 1, stages.size)
        assertEquals(StageType.START, stages.first().type)
        assertEquals(StageType.END, stages.last().type)
        assertEquals(Stage(StageType.WORK, 180, 1, 2, 1, 5), stages[1])
        assertEquals(Stage(StageType.REST, 60, 1, 2, 5, 5), stages[10]) // rest between blocks stays
        assertEquals(Stage(StageType.WORK, 30, 2, 2, 1, 8), stages[11])
    }

    @Test
    fun skipsRestAfterLastRoundOfLastBlock() {
        val stages = StageBuilder.build(twoBlocks)
        val beforeEnd = stages[stages.size - 2]
        assertEquals(Stage(StageType.WORK, 30, 2, 2, 8, 8), beforeEnd)
    }

    @Test
    fun disabledOrZeroStagesAreLeftOut() {
        val w = Workout(
            warmupEnabled = false, warmupSec = 60,
            blocks = listOf(Block(workSec = 20, restSec = 0, rounds = 3)),
            cooldownEnabled = true, cooldownSec = 0,
        )
        val stages = StageBuilder.build(w)
        assertEquals(listOf(StageType.WORK, StageType.WORK, StageType.WORK), stages.map { it.type })
    }

    @Test
    fun totalSecondsIsSumOfStages() {
        // 60 + 5*180 + 5*60 + 8*30 + 7*15 + 120
        assertEquals(60 + 900 + 300 + 240 + 105 + 120, StageBuilder.totalSec(twoBlocks))
    }

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("03:00", formatMmSs(180_000))
        assertEquals("00:05", formatMmSs(4_001))
        assertEquals("00:01", formatMmSs(1))
        assertEquals("00:00", formatMmSs(0))
    }
}
