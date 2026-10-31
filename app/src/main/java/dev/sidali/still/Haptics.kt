package dev.sidali.still

import dev.sidali.still.Tokens.Feel
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibrationEffect.Composition
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The breath, felt. A slow rising texture while breathing in, nothing on a hold, a slow falling
 * texture while breathing out. Rich primitives when the motor has them, an amplitude ramp otherwise.
 *
 * Breath textures use the media usage (the system "media vibration" setting applies); taps go
 * through the view so the system "touch feedback" setting applies. Nothing plays when [enabled] is
 * false or the view has haptic feedback turned off.
 */
class Haptics(private val view: View, enabled: Boolean) : BreathListener {

    var enabled: Boolean = enabled
        set(value) {
            field = value
            if (!value) cancel()
        }

    private val vibrator: Vibrator? =
        view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.takeIf { it.hasVibrator() }

    private val rich: Boolean = vibrator?.areAllPrimitivesSupported(
        Composition.PRIMITIVE_SLOW_RISE, Composition.PRIMITIVE_QUICK_FALL, Composition.PRIMITIVE_LOW_TICK,
    ) == true

    private val riseMillis: Int
    private val fallMillis: Int
    private val tickMillis: Int

    init {
        val d = if (rich) vibrator!!.getPrimitiveDurations(
            Composition.PRIMITIVE_SLOW_RISE, Composition.PRIMITIVE_QUICK_FALL, Composition.PRIMITIVE_LOW_TICK,
        ) else IntArray(3)
        riseMillis = d[0].takeIf { it > 0 } ?: Feel.RISE_GUESS
        fallMillis = d[1].takeIf { it > 0 } ?: Feel.FALL_GUESS
        tickMillis = d[2].takeIf { it > 0 } ?: Feel.TICK_GUESS
    }

    private val allowed get() = enabled && vibrator != null && view.isHapticFeedbackEnabled

    override fun onPhase(phase: Phase, durationMillis: Long) {
        if (!allowed) return
        when (phase) {
            Phase.In -> breathe(if (rich) riseComposition(durationMillis) else rampWaveform(durationMillis, rising = true))
            Phase.Out -> breathe(if (rich) fallComposition(durationMillis) else rampWaveform(durationMillis, rising = false))
            Phase.Hold, Phase.Rest -> vibrator?.cancel()
        }
    }

    /** A light confirmation for an in-app tap. */
    fun tap() {
        if (enabled) view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    /** End of a session: a gentle double low tick. */
    fun done() {
        if (!allowed) return
        val v = vibrator ?: return
        if (rich) {
            val effect = VibrationEffect.startComposition()
                .addPrimitive(Composition.PRIMITIVE_LOW_TICK, Feel.DONE_FIRST)
                .addPrimitive(Composition.PRIMITIVE_LOW_TICK, Feel.DONE_SECOND, Feel.DONE_GAP)
                .compose()
            vibrate(v, effect, touch = true)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        }
    }

    /** Stops whatever texture is playing. */
    fun cancel() {
        vibrator?.cancel()
    }

    // In: slow rises, evenly spaced, each a little stronger, over the first part of the phase.
    private fun riseComposition(durationMillis: Long): VibrationEffect {
        val span = (durationMillis * Feel.SPAN).toLong()
        val count = (span / Feel.RISE_EVERY).toInt().coerceIn(1, Feel.MAX_PRIMITIVES)
        val slot = span / count
        val gap = (slot - riseMillis).coerceIn(0L, Feel.MAX_DELAY.toLong()).toInt()
        val c = VibrationEffect.startComposition()
        for (i in 0 until count) {
            val t = if (count == 1) 1f else i / (count - 1f)
            val scale = Feel.RISE_FROM + (Feel.RISE_TO - Feel.RISE_FROM) * t
            c.addPrimitive(Composition.PRIMITIVE_SLOW_RISE, scale, if (i == 0) 0 else gap)
        }
        return c.compose()
    }

    // Out: one soft fall, then low ticks that fade and drift apart, like air running out.
    private fun fallComposition(durationMillis: Long): VibrationEffect {
        val span = (durationMillis * Feel.SPAN).toLong() - fallMillis
        val c = VibrationEffect.startComposition()
            .addPrimitive(Composition.PRIMITIVE_QUICK_FALL, Feel.FALL_SCALE)
        val ticks = (span / Feel.TICK_EVERY).toInt().coerceIn(0, Feel.MAX_PRIMITIVES - 1)
        if (ticks > 0) {
            // Gaps grow along the phase: the k-th tick lands at span * (k / ticks)^0.7.
            var previous = 0L
            for (k in 1..ticks) {
                val t = k / ticks.toFloat()
                val at = (span * t.pow(Feel.TICK_EASE)).toLong()
                val gap = (at - previous - tickMillis).coerceIn(0L, Feel.MAX_DELAY.toLong()).toInt()
                previous = at
                val scale = Feel.TICK_FROM + (Feel.TICK_TO - Feel.TICK_FROM) * t
                c.addPrimitive(Composition.PRIMITIVE_LOW_TICK, scale, gap)
            }
        }
        return c.compose()
    }

    // Fallback: a smooth amplitude ramp, or one soft pulse when the motor has no amplitude control.
    private fun rampWaveform(durationMillis: Long, rising: Boolean): VibrationEffect {
        val v = vibrator!!
        if (!v.hasAmplitudeControl()) {
            return VibrationEffect.createOneShot(Feel.PULSE_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        val span = (durationMillis * Feel.SPAN).toLong()
        val steps = (span / Feel.RAMP_STEP).toInt().coerceIn(2, Feel.RAMP_MAX_STEPS)
        val each = span / steps
        val timings = LongArray(steps) { each }
        val amplitudes = IntArray(steps) { i ->
            val t = i / (steps - 1f)
            val x = if (rising) t else 1f - t
            (Feel.AMP_FLOOR + (Feel.AMP_PEAK - Feel.AMP_FLOOR) * x * x).roundToInt().coerceIn(1, 255)
        }
        return VibrationEffect.createWaveform(timings, amplitudes, -1)
    }

    private fun breathe(effect: VibrationEffect) {
        val v = vibrator ?: return
        vibrate(v, effect, touch = false)
    }

    private fun vibrate(v: Vibrator, effect: VibrationEffect, touch: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val usage = if (touch) VibrationAttributes.USAGE_TOUCH else VibrationAttributes.USAGE_MEDIA
            v.vibrate(effect, VibrationAttributes.createForUsage(usage))
        } else {
            val usage = if (touch) AudioAttributes.USAGE_ASSISTANCE_SONIFICATION else AudioAttributes.USAGE_MEDIA
            @Suppress("DEPRECATION")
            v.vibrate(effect, AudioAttributes.Builder().setUsage(usage).build())
        }
    }
}
