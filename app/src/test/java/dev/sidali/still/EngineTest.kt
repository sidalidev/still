package dev.sidali.still

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class EngineTest {

    private fun total(steps: List<Step>) = steps.sumOf { it.millis }

    /** Splits a timeline into breaths, each starting at an In. */
    private fun breaths(steps: List<Step>): List<List<Step>> {
        val out = mutableListOf<MutableList<Step>>()
        for (s in steps) {
            if (s.phase == Phase.In) out += mutableListOf(s) else out.last() += s
        }
        return out
    }

    @Test fun calmIsFiveInFiveOut() {
        for (minutes in listOf(3, 5, 10)) {
            val steps = Patterns.steps(Mode.Calm, minutes)
            assertEquals(minutes * 60_000L, total(steps))
            assertEquals(minutes * 6, breaths(steps).size)
            breaths(steps).forEach { b ->
                assertEquals(listOf(Step(Phase.In, 5_000), Step(Phase.Out, 5_000)), b)
            }
        }
    }

    @Test fun trustBreathesLikeCalm() {
        assertEquals(Patterns.steps(Mode.Calm, 3), Patterns.steps(Mode.Trust, 3))
    }

    @Test fun everySessionIsWholeBreathsEndingOnAnOut() {
        for (mode in Mode.entries) for (minutes in listOf(0, 1, 3, 5, 7, 10, 15, 20)) {
            val steps = Patterns.steps(mode, minutes)
            assertTrue("$mode $minutes empty", steps.isNotEmpty())
            assertEquals(Phase.In, steps.first().phase)
            assertEquals(Phase.Out, steps.last().phase)
            breaths(steps).forEach { b ->
                assertEquals(Phase.In, b.first().phase)
                assertEquals(Phase.Out, b.last().phase)
                assertTrue(b.all { it.millis > 0 })
            }
        }
    }

    @Test fun sessionLengthIsTheNearestWholeBreath() {
        for (mode in Mode.entries) for (minutes in listOf(3, 5, 10, 15, 20)) {
            val steps = Patterns.steps(mode, minutes)
            val last = breaths(steps).last().sumOf { it.millis }
            val diff = abs(total(steps) - minutes * 60_000L)
            assertTrue("$mode $minutes off by $diff", diff <= (last + 1) / 2)
        }
    }

    @Test fun sleepStartsAtFourSix() {
        val first = breaths(Patterns.steps(Mode.Sleep, 10)).first()
        assertEquals(listOf(Step(Phase.In, 4_000), Step(Phase.Out, 6_000)), first)
    }

    @Test fun sleepSlowsDownSteadilyThenStaysAtFourSevenEight() {
        val bs = breaths(Patterns.steps(Mode.Sleep, 20))
        var at = 0L
        var previous: List<Step>? = null
        for (b in bs) {
            val inn = b.first { it.phase == Phase.In }.millis
            val hold = b.firstOrNull { it.phase == Phase.Hold }?.millis ?: 0L
            val out = b.first { it.phase == Phase.Out }.millis
            assertEquals(4_000L, inn)
            previous?.let { p ->
                val pHold = p.firstOrNull { it.phase == Phase.Hold }?.millis ?: 0L
                val pOut = p.first { it.phase == Phase.Out }.millis
                assertTrue("hold shrank at $at", hold >= pHold)
                assertTrue("out shrank at $at", out >= pOut)
                assertTrue("breath shrank at $at", b.sumOf { it.millis } >= p.sumOf { it.millis })
                // Smooth: no jump bigger than one breath's share of the ramp.
                assertTrue("hold jumped at $at", hold - pHold <= 7_000L * 19_000 / (8 * 60_000) + 1)
            }
            if (at >= 8 * 60_000L) {
                assertEquals(listOf(Step(Phase.In, 4_000), Step(Phase.Hold, 7_000), Step(Phase.Out, 8_000)), b)
            } else {
                assertTrue(hold < 7_000L && out < 8_000L)
            }
            previous = b
            at += b.sumOf { it.millis }
        }
        assertTrue(at > 8 * 60_000L)
        assertEquals(19_000L, bs.last().sumOf { it.millis })
    }

    @Test fun sleepHoldGrowsFromZero() {
        val bs = breaths(Patterns.steps(Mode.Sleep, 10))
        assertFalse(bs[0].any { it.phase == Phase.Hold })
        assertTrue(bs[1].first { it.phase == Phase.Hold }.millis in 1L..500L)
    }

    @Test fun stateAtBoundaries() {
        val steps = listOf(Step(Phase.In, 4_000), Step(Phase.Hold, 2_000), Step(Phase.Out, 4_000))
        val start = stateAt(steps, Mode.Sleep, 0)
        assertEquals(Phase.In, start.phase)
        assertEquals(0f, start.phaseProgress, 1e-6f)
        assertEquals(0f, start.fullness, 1e-6f)
        assertEquals(10_000L, start.totalMillis)
        assertFalse(start.finished)

        val mid = stateAt(steps, Mode.Sleep, 2_000)
        assertEquals(0.5f, mid.phaseProgress, 1e-6f)
        assertEquals(0.5f, mid.fullness, 1e-6f)

        // A phase owns its first instant.
        val hold = stateAt(steps, Mode.Sleep, 4_000)
        assertEquals(Phase.Hold, hold.phase)
        assertEquals(0f, hold.phaseProgress, 1e-6f)
        assertEquals(1f, hold.fullness, 1e-6f)

        val out = stateAt(steps, Mode.Sleep, 6_000)
        assertEquals(Phase.Out, out.phase)
        assertEquals(1f, out.fullness, 1e-6f)

        val nearEnd = stateAt(steps, Mode.Sleep, 9_999)
        assertEquals(Phase.Out, nearEnd.phase)
        assertFalse(nearEnd.finished)
        assertTrue(nearEnd.fullness < 0.001f)

        val end = stateAt(steps, Mode.Sleep, 10_000)
        assertTrue(end.finished)
        assertEquals(Phase.Out, end.phase)
        assertEquals(0f, end.fullness, 1e-6f)
        assertEquals(10_000L, end.elapsedMillis)

        val after = stateAt(steps, Mode.Sleep, 99_000)
        assertTrue(after.finished)
        assertEquals(10_000L, after.elapsedMillis)

        val before = stateAt(steps, Mode.Sleep, -50)
        assertEquals(Phase.In, before.phase)
        assertEquals(0L, before.elapsedMillis)
    }

    @Test fun fullnessEasesInAndOut() {
        // Slow at the ends, symmetric, monotonic.
        assertTrue(fullness(Phase.In, 0.1f) < 0.1f)
        assertTrue(fullness(Phase.In, 0.9f) > 0.9f)
        assertEquals(fullness(Phase.In, 0.3f), 1f - fullness(Phase.Out, 0.3f), 1e-6f)
        var last = -1f
        for (i in 0..100) {
            val f = fullness(Phase.In, i / 100f)
            assertTrue(f >= last)
            last = f
        }
        assertEquals(0f, fullness(Phase.Rest, 0.5f), 0f)
        assertEquals(1f, fullness(Phase.Hold, 0.5f), 0f)
    }

    @Test fun fullnessIsContinuousAcrossTheWholeSession() {
        val steps = Patterns.steps(Mode.Sleep, 10)
        var last = stateAt(steps, Mode.Sleep, 0).fullness
        var t = 0L
        while (t < total(steps)) {
            val f = stateAt(steps, Mode.Sleep, t).fullness
            assertTrue("jump at $t", abs(f - last) < 0.02f)
            last = f
            t += 16
        }
    }

    private class FakeClock(var now: Long = 1_000_000L) : () -> Long {
        override fun invoke() = now
    }

    @Test fun engineCallsTheListenerOncePerPhaseAndFinishes() {
        for (mode in Mode.entries) {
            val clock = FakeClock()
            val calls = mutableListOf<Pair<Phase, Long>>()
            val engine = Engine(mode, 10, { p, d -> calls += p to d }, clock)
            var ticks = 0
            while (engine.tick()) {
                clock.now += 16
                ticks++
                assertTrue("runaway", ticks < 10_000_000)
            }
            assertEquals(engine.steps.map { it.phase to it.millis }, calls)
            val s = engine.state.value
            assertTrue(s.finished)
            assertEquals(s.totalMillis, s.elapsedMillis)
            assertEquals(0f, s.fullness, 1e-6f)
            assertFalse(engine.tick())
            assertEquals(engine.steps.size, calls.size)
        }
    }

    @Test fun engineStateFollowsTheClock() {
        val clock = FakeClock()
        val engine = Engine(Mode.Calm, 3, { _, _ -> }, clock)
        assertFalse(engine.state.value.finished)
        engine.begin()
        clock.now += 7_500
        engine.tick()
        val s = engine.state.value
        assertEquals(Phase.Out, s.phase)
        assertEquals(0.5f, s.phaseProgress, 1e-6f)
        assertEquals(7_500L, s.elapsedMillis)
        assertEquals(180_000L, s.totalMillis)
    }

    @Test fun engineAnnouncesOnlyTheCurrentPhaseAfterAPause() {
        val clock = FakeClock()
        val calls = mutableListOf<Phase>()
        val engine = Engine(Mode.Calm, 3, { p, _ -> calls += p }, clock)
        engine.tick()
        clock.now += 32_000 // skips several phases, lands in an In
        engine.tick()
        assertEquals(listOf(Phase.In, Phase.In), calls)
        clock.now += 1_000_000
        assertFalse(engine.tick())
        assertEquals(2, calls.size)
        assertTrue(engine.state.value.finished)
    }
}
