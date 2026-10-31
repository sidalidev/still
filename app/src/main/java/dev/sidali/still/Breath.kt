package dev.sidali.still

// Shared model of a breathing session. The engine (Engine.kt), the guides (Haptics.kt, Sound.kt)
// and the screens all speak this language.

enum class Phase { In, Hold, Out, Rest }

enum class Mode { Calm, Sleep, Trust }

/** One step of the breath: which phase, and how long it lasts. */
data class Step(val phase: Phase, val millis: Long)

/** What the screens draw at a given instant of a session. */
data class BreathState(
    val mode: Mode,
    val phase: Phase,
    /** 0 at the start of the current phase, 1 at its end. */
    val phaseProgress: Float,
    /** Orb size target, 0 (fully out) to 1 (fully in). */
    val fullness: Float,
    val elapsedMillis: Long,
    val totalMillis: Long,
    val finished: Boolean,
)

/** Receives each phase change, to play a haptic or a sound in time with the orb. */
fun interface BreathListener {
    fun onPhase(phase: Phase, durationMillis: Long)
}

/** A finished (or stopped) session, as stored on the phone. */
data class Session(val startedAt: Long, val mode: Mode, val minutes: Int)
