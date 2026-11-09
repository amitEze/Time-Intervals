package com.amiteze.intervaltimer.timer

import com.amiteze.intervaltimer.model.Block
import com.amiteze.intervaltimer.model.BlockStage
import com.amiteze.intervaltimer.model.Workout
import org.junit.Assert.assertEquals
import org.junit.Test

class StageBuilderTest {

    private fun work(name: String, sec: Int) = BlockStage(name = name, sec = sec)
    private fun rest(sec: Int) = BlockStage(name = "Rest", sec = sec, rest = true)

    private val twoBlocks = Workout(
        warmupEnabled = true, warmupSec = 60,
        blocks = listOf(
            Block(name = "Boxing", sets = 5, stages = listOf(work("Fight", 180), rest(60))),
            Block(name = "Circuit", sets = 2, stages = listOf(work("Squats", 30), work("Push-ups", 20), rest(15))),
        ),
        cooldownEnabled = true, cooldownSec = 120,
    )

    @Test
    fun buildsStagesInOrder() {
        val stages = StageBuilder.build(twoBlocks)
        // 1 start + 5*(2) + (3 + 2 without final rest) + 1 end
        assertEquals(1 + 10 + 5 + 1, stages.size)
        assertEquals(StageType.START, stages.first().type)
        assertEquals(StageType.END, stages.last().type)
        assertEquals(Stage(StageType.WORK, 180, "Fight", 1, 2, "Boxing", 1, 5), stages[1])
        assertEquals(Stage(StageType.REST, 60, "Rest", 1, 2, "Boxing", 5, 5), stages[10]) // rest between blocks stays
        assertEquals(listOf("Squats", "Push-ups", "Rest", "Squats", "Push-ups"), stages.subList(11, 16).map { it.name })
        assertEquals(Stage(StageType.WORK, 20, "Push-ups", 2, 2, "Circuit", 2, 2), stages[15])
    }

    @Test
    fun onlyTrailingRestsOfTheVeryLastSetAreSkipped() {
        val w = Workout(blocks = listOf(Block(sets = 2, stages = listOf(rest(5), work("A", 10), rest(5), rest(3)))))
        val names = StageBuilder.build(w).map { "${it.name}${it.durationSec}" }
        assertEquals(listOf("Rest5", "A10", "Rest5", "Rest3", "Rest5", "A10"), names)
    }

    @Test
    fun disabledOrZeroStagesAreLeftOut() {
        val w = Workout(
            warmupEnabled = false, warmupSec = 60,
            blocks = listOf(
                Block(sets = 3, stages = listOf(work("Work", 20), rest(0))),
                Block(sets = 4, stages = listOf(work("Nothing", 0))), // empty block is ignored
            ),
            cooldownEnabled = true, cooldownSec = 0,
        )
        val stages = StageBuilder.build(w)
        assertEquals(3, stages.size)
        assertEquals(1, stages[0].blockCount)
    }

    @Test
    fun blankNamesGetADefault() {
        val w = Workout(blocks = listOf(Block(sets = 1, stages = listOf(BlockStage(name = " ", sec = 5), BlockStage(name = "", sec = 5, rest = true), work("x", 1)))))
        assertEquals(listOf("Work", "Rest", "x"), StageBuilder.build(w).map { it.name })
    }

    @Test
    fun totalSecondsIsSumOfStages() {
        // 60 + 5*(180+60) + (30+20+15) + (30+20) + 120
        assertEquals(60 + 1200 + 65 + 50 + 120, StageBuilder.totalSec(twoBlocks))
    }

    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("03:00", formatMmSs(180_000))
        assertEquals("00:05", formatMmSs(4_001))
        assertEquals("00:01", formatMmSs(1))
        assertEquals("00:00", formatMmSs(0))
    }
}
