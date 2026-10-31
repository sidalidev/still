package dev.sidali.still

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToLong

/** The breathing patterns of each mode, as a full timeline of steps. */
object Patterns {
    /** Calm and Trust: in 5 s, out 5 s, six breaths a minute. */
    const val CALM_IN = 5_000L
    const val CALM_OUT = 5_000L

    /** Sleep starts at in 4, out 6... */
    const val SLEEP_IN = 4_000L
    const val SLEEP_OUT_START = 6_000L

    /** ...and slows down to 4-7-8 over the first eight minutes. */
    const val SLEEP_HOLD_END = 7_000L
    const val SLEEP_OUT_END = 8_000L
    const val SLEEP_RAMP = 8 * 60_000L

    /**
     * Every step of a session of about [minutes]. The session is rounded to whole breaths, so it
     * always ends on an Out, never mid-breath, and holds at least one breath.
     */
    fun steps(mode: Mode, minutes: Int): List<Step> {
        val target = minutes.coerceAtLeast(0) * 60_000L
        val steps = ArrayList<Step>()
        var at = 0L
        while (true) {
            val breath = breathAt(mode, at)
            val length = breath.sumOf { it.millis }
            // Keep a breath when at least half of it fits: the session lands on the nearest whole breath.
            if (steps.isNotEmpty() && target - at < (length + 1) / 2) break
            steps += breath
            at += length
        }
        return steps
    }

    /** The breath that starts [at] milliseconds into a session. */
    private fun breathAt(mode: Mode, at: Long): List<Step> = when (mode) {
        Mode.Calm, Mode.Trust -> listOf(Step(Phase.In, CALM_IN), Step(Phase.Out, CALM_OUT))
        Mode.Sleep -> {
            val t = (at.toDouble() / SLEEP_RAMP).coerceIn(0.0, 1.0)
            val hold = (SLEEP_HOLD_END * t).roundToLong()
            val out = (SLEEP_OUT_START + (SLEEP_OUT_END - SLEEP_OUT_START) * t).roundToLong()
            buildList {
                add(Step(Phase.In, SLEEP_IN))
                if (hold > 0) add(Step(Phase.Hold, hold))
                add(Step(Phase.Out, out))
            }
        }
    }
}

/** Smooth start and end, the shape of a calm breath. */
internal fun easeInOut(p: Float): Float = ((1 - cos(PI * p.coerceIn(0f, 1f))) / 2).toFloat()

/** What a session of [steps] looks like [elapsedMillis] after its start. Pure. */
fun stateAt(steps: List<Step>, mode: Mode, elapsedMillis: Long): BreathState {
    val total = steps.sumOf { it.millis }
    if (steps.isEmpty() || elapsedMillis >= total) {
        return BreathState(mode, Phase.Out, 1f, 0f, total, total, finished = true)
    }
    val elapsed = elapsedMillis.coerceAtLeast(0)
    var start = 0L
    for (step in steps) {
        val end = start + step.millis
        if (elapsed < end) {
            val progress = if (step.millis == 0L) 1f else (elapsed - start).toFloat() / step.millis
            return BreathState(mode, step.phase, progress, fullness(step.phase, progress), elapsed, total, false)
        }
        start = end
    }
    return BreathState(mode, Phase.Out, 1f, 0f, total, total, finished = true)
}

/** Orb size for a [phase] at [progress]: grows on In, rests full on Hold, shrinks on Out, rests empty on Rest. */
internal fun fullness(phase: Phase, progress: Float): Float = when (phase) {
    Phase.In -> easeInOut(progress)
    Phase.Hold -> 1f
    Phase.Out -> 1f - easeInOut(progress)
    Phase.Rest -> 0f
}

/** Index of the step running [elapsedMillis] into [steps], or -1 once the session is over. */
internal fun stepIndexAt(steps: List<Step>, elapsedMillis: Long): Int {
    var end = 0L
    steps.forEachIndexed { i, step ->
        end += step.millis
        if (elapsedMillis < end) return i
    }
    return -1
}

/**
 * Runs a session in real time. Uses a monotonic clock (elapsed realtime by default, injected in
 * tests), emits a [BreathState] about every frame and tells [listener] when each phase starts.
 */
class Engine(
    val mode: Mode,
    val minutes: Int,
    private val listener: BreathListener,
    private val clock: () -> Long = { SystemClock.elapsedRealtime() },
) {
    val steps: List<Step> = Patterns.steps(mode, minutes)
    private val _state = MutableStateFlow(stateAt(steps, mode, 0))
    val state: StateFlow<BreathState> = _state.asStateFlow()

    private var job: Job? = null
    private var startedAt = -1L
    private var lastStep = -1
    private var done = false

    /** Starts the session now and ticks about every frame until it ends or [stop] is called. */
    fun start(scope: CoroutineScope) {
        if (job != null || done) return
        begin()
        job = scope.launch {
            while (isActive && tick()) delay(FRAME)
            job = null
        }
    }

    /** Stops ticking. The state keeps its last value. */
    fun stop() {
        job?.cancel()
        job = null
    }

    /** Marks the start of the session on the clock. [start] does it; tests call it directly. */
    internal fun begin() {
        if (startedAt < 0) startedAt = clock()
    }

    /**
     * Reads the clock once, updates [state] and calls the listener when a new phase has begun.
     * Returns false once the session has finished.
     */
    internal fun tick(): Boolean {
        if (done) return false
        begin()
        val elapsed = clock() - startedAt
        val index = stepIndexAt(steps, elapsed)
        if (index > lastStep) {
            // If frames were skipped (app paused), only the phase running now is announced:
            // a burst of stale haptics would be worse than a missed one.
            lastStep = index
            listener.onPhase(steps[index].phase, steps[index].millis)
        }
        val now = stateAt(steps, mode, elapsed)
        _state.value = now
        if (now.finished) {
            done = true
        }
        return !done
    }

    private companion object {
        const val FRAME = 16L
    }
}
