package com.amiteze.intervaltimer.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerEngineTest {

    private val stages = listOf(
        Stage(StageType.WORK, 10),
        Stage(StageType.REST, 5),
        Stage(StageType.WORK, 10),
    )

    private fun started(at: Long = 1000) = TimerEngine(stages).apply { start(at) }

    @Test
    fun countsDownFromClockNotTicks() {
        val e = started()
        assertEquals(10_000, e.stageRemainingMs(1000))
        assertEquals(6_500, e.stageRemainingMs(4500))
        assertEquals(21_500, e.totalRemainingMs(4500))
    }

    @Test
    fun advancesOverMultipleStagesAfterLongGap() {
        val e = started()
        // Simulate no ticks for 17 s (e.g. screen off): work 10 + rest 5 + 2 s into work 2.
        assertEquals(2, e.update(18_000))
        assertEquals(2, e.index)
        assertEquals(8_000, e.stageRemainingMs(18_000))
    }

    @Test
    fun finishesAtExactEnd() {
        val e = started(0)
        e.update(24_999)
        assertFalse(e.isFinished)
        e.update(25_000)
        assertTrue(e.isFinished)
        assertEquals(0, e.totalRemainingMs(25_000))
    }

    @Test
    fun pauseFreezesTime() {
        val e = started(0)
        e.pause(3_000)
        assertEquals(7_000, e.stageRemainingMs(60_000))
        assertEquals(0, e.update(60_000))
        e.resume(60_000)
        assertEquals(7_000, e.stageRemainingMs(60_000))
        assertEquals(2_000, e.stageRemainingMs(65_000))
    }

    @Test
    fun skipAndPrevious() {
        val e = started(0)
        e.skip(2_000)
        assertEquals(1, e.index)
        assertEquals(5_000, e.stageRemainingMs(2_000))
        // Less than 3 s into the stage: previous goes back one stage.
        e.previous(3_000)
        assertEquals(0, e.index)
        assertEquals(10_000, e.stageRemainingMs(3_000))
        // More than 3 s in: previous restarts the current stage.
        e.previous(8_000)
        assertEquals(0, e.index)
        assertEquals(10_000, e.stageRemainingMs(8_000))
    }

    @Test
    fun skipPastLastStageFinishes() {
        val e = started(0)
        repeat(3) { e.skip(0) }
        assertTrue(e.isFinished)
        e.previous(0)
        assertEquals(2, e.index)
    }

    @Test
    fun previousWhilePausedStaysPaused() {
        val e = started(0)
        e.update(12_000)
        e.pause(12_000)
        e.previous(12_000)
        assertTrue(e.isPaused)
        assertEquals(0, e.index)
        assertEquals(10_000, e.stageRemainingMs(50_000))
    }
}
