package dev.sidali.still

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * The orb: the whole interface. A flat core and three halo rings at decreasing alpha (no gradient),
 * sized by [fullness] (0 fully out, 1 fully in). [glow] dims the halos (0 draws none); [coreAlpha] is
 * the core's share of [color]'s alpha; [progress], when given, draws a thin rounded ring that counts
 * the session, at [ringAlpha].
 */
@Composable
fun Orb(
    fullness: Float,
    color: Color,
    modifier: Modifier = Modifier,
    glow: Float = 1f,
    coreAlpha: Float = Tokens.coreAlpha,
    progress: Float? = null,
    ringAlpha: Float = 1f,
    description: String? = null,
) {
    val desc = description ?: stringResource(R.string.orb_description)
    val ringWidth = Tokens.ringStroke
    Canvas(modifier.semantics { contentDescription = desc }) {
        val center = Offset(size.width / 2, size.height / 2)
        val ring = ringWidth.toPx()
        val outer = size.minDimension / 2 - ring
        // The largest halo, fully in, just touches the ring's inner edge.
        val full = outer / (1f + Tokens.haloStep * HALOS) * ORB_ROOM
        val r = full * (Tokens.orbMinScale + (1f - Tokens.orbMinScale) * fullness.coerceIn(0f, 1f))
        if (glow > 0f) for (i in HALOS downTo 1) {
            drawCircle(
                color.copy(alpha = color.alpha * Tokens.haloAlpha * glow * (HALOS + 1 - i) / HALOS),
                radius = r * (1f + Tokens.haloStep * i), center = center,
            )
        }
        drawCircle(color.copy(alpha = color.alpha * coreAlpha), radius = r, center = center)
        if (progress != null && ringAlpha > 0f) {
            val d = outer * 2
            val topLeft = Offset(center.x - outer, center.y - outer)
            drawCircle(color.copy(alpha = color.alpha * Tokens.ringTrackAlpha * ringAlpha), radius = outer, center = center, style = Stroke(ring))
            drawArc(
                color.copy(alpha = color.alpha * Tokens.coreAlpha * ringAlpha), -90f, 360f * progress.coerceIn(0f, 1f), false,
                topLeft = topLeft, size = Size(d, d), style = Stroke(ring, cap = StrokeCap.Round),
            )
        }
    }
}

private const val HALOS = 3
/** Share of the room the orb may fill, leaving air between the halos and the ring. */
private const val ORB_ROOM = 0.94f

/** True when the system "Remove animations" setting is on. */
@Composable
fun rememberReduceMotion(): Boolean {
    val ctx = LocalContext.current
    return remember {
        Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** A very slow rise and fall, for the orb at rest. Still when animations are off. */
@Composable
fun idleFullness(reduceMotion: Boolean): Float {
    if (reduceMotion) return (Tokens.idleLow + Tokens.idleHigh) / 2
    val t = rememberInfiniteTransition(label = "idle")
    val f by t.animateFloat(
        Tokens.idleLow, Tokens.idleHigh,
        infiniteRepeatable(tween(Tokens.idleBreathMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "idle",
    )
    return f
}

/** The Sleep orb's core alpha: a slow glow between dim and dimmer. Steady when animations are off. */
@Composable
fun sleepCoreAlpha(reduceMotion: Boolean): Float {
    if (reduceMotion) return (Tokens.sleepCoreLow + Tokens.sleepCoreHigh) / 2
    val t = rememberInfiniteTransition(label = "glow")
    val g by t.animateFloat(
        Tokens.sleepCoreLow, Tokens.sleepCoreHigh,
        infiniteRepeatable(tween(Tokens.sleepGlowMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    return g
}

/*
 * The orb travels: Home, the session and the end share one orb, which moves and grows between them
 * on a spring instead of crossfading. App() provides the two scopes; without them (a preview, About)
 * the modifier does nothing.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalScreenScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** Marks this orb as the travelling one. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.travellingOrb(): Modifier {
    val shared = LocalSharedScope.current ?: return this
    val screen = LocalScreenScope.current ?: return this
    return with(shared) {
        this@travellingOrb.sharedElement(
            rememberSharedContentState(ORB_KEY), screen,
            boundsTransform = { _, _ -> spring(dampingRatio = Tokens.orbTravelDamping, stiffness = Tokens.orbTravelStiffness) },
        )
    }
}

private const val ORB_KEY = "orb"
