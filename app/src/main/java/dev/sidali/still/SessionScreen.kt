package dev.sidali.still

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A running session. Calm and Trust: the whole screen in night blue, the orb follows the breath and a
 * thin ring counts the session. Sleep: true black, a barely visible orb, no words after the first
 * breath, brightness lowered. Tap anywhere for a Stop pill, 3 s.
 *
 * [onFinish] gets the minutes kept (0 when too short to keep), whether to end quietly (the app was
 * left: no chime, straight Home) and the minutes breathed today, this session included. The session
 * is written off the main thread first.
 */
@Composable
fun SessionScreen(mode: Mode, minutes: Int, guides: Guides, ambience: String?, onFinish: (Int, Boolean, Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { Engine(mode, minutes, guides.listener(mode)) }
    val state by engine.state.collectAsState()
    val startedAt = remember { System.currentTimeMillis() }
    var ended by remember { mutableStateOf(false) }
    val finish by rememberUpdatedState(onFinish)

    fun end(complete: Boolean, quiet: Boolean) {
        if (ended) return
        ended = true
        engine.stop()
        guides.sound.ambience(null)
        val kept = if (complete) minutes else (engine.state.value.elapsedMillis / MINUTE).toInt()
        val app = ctx.applicationContext
        scope.launch {
            // Never cancelled halfway: a session that was breathed is always written.
            val today = withContext(Dispatchers.IO + NonCancellable) {
                if (kept >= 1) Store.add(app, Session(startedAt, mode, kept))
                Store.minutesToday(app)
            }
            finish(kept, quiet, today)
        }
    }

    LaunchedEffect(engine) {
        guides.sound.ambience(ambience)
        engine.start(this)
    }
    LaunchedEffect(state.finished) { if (state.finished) end(complete = true, quiet = false) }
    DisposableEffect(engine) {
        onDispose {
            engine.stop()
            if (!ended) guides.sound.ambience(null)
        }
    }
    // Leaving the app stops the session (it is kept if it lasted a minute).
    DisposableEffect(engine) {
        val lifecycle = (ctx.findActivity() as? ComponentActivity)?.lifecycle
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_STOP) end(complete = false, quiet = true) }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }
    BackHandler { end(complete = false, quiet = false) }

    val sleep = mode == Mode.Sleep
    Immersive(keepOn = true, dim = sleep)

    var stopShownAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(stopShownAt) {
        if (stopShownAt > 0) { delay(Tokens.stopPillMs); stopShownAt = 0 }
    }

    val c = Tokens.colors
    val bg = if (sleep) c.sleepPage else c.session
    val ink = if (sleep) c.sleepOrb else c.onSession
    val reduceMotion = rememberReduceMotion()
    // The engine eases the breath itself; this is only a light, lag-free smoothing.
    val fullness by animateFloatAsState(
        state.fullness,
        spring(dampingRatio = Tokens.breathDamping, stiffness = Tokens.breathStiffness),
        label = "breath",
    )

    // Sleep shows the words only during the first breath (in, then out).
    var phasesSeen by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.phase) { phasesSeen++ }
    val showWord = !sleep || phasesSeen <= FIRST_BREATH_PHASES

    // The ring counts the session, but only once it has started to move: a lone dot reads as a glitch.
    val progress = state.elapsedMillis.toFloat() / state.totalMillis.coerceAtLeast(1)
    val ringAlpha by animateFloatAsState(
        if (progress > Tokens.ringShowAt) 1f else 0f, tween(Tokens.wordFadeMs), label = "ring",
    )

    Box(
        Modifier.fillMaxSize().background(bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                stopShownAt = System.currentTimeMillis()
            },
    ) {
        // The orb is the still point, exactly centred; the word sits below it.
        Orb(
            fullness = fullness,
            color = ink,
            // Sleep: a flat core whose alpha glows slowly, no halos (they band against true black).
            glow = if (sleep) 0f else 1f,
            coreAlpha = if (sleep) sleepCoreAlpha(reduceMotion) else Tokens.coreAlpha,
            progress = if (sleep) null else progress,
            ringAlpha = ringAlpha,
            modifier = Modifier.align(Alignment.Center).travellingOrb().size(Tokens.orbSession),
        )
        Box(
            Modifier.align(Alignment.Center)
                .offset(y = Tokens.orbSession / 2 + Tokens.space48 + Tokens.touchTarget / 2)
                .height(Tokens.touchTarget),
            contentAlignment = Alignment.Center,
        ) {
            // The old word fades out before the new one fades in, so "in" and "out" never overlap.
            AnimatedContent(
                targetState = if (showWord) state.phase else null,
                transitionSpec = {
                    fadeIn(tween(Tokens.wordFadeMs / 2, delayMillis = Tokens.wordFadeMs / 2)) togetherWith
                        fadeOut(tween(Tokens.wordFadeMs / 2))
                },
                contentAlignment = Alignment.Center,
                label = "word",
            ) { p ->
                if (p != null) Text(
                    stringResource(phaseWord(p)),
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (sleep) ink.copy(alpha = Tokens.sleepWordAlpha) else ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        AnimatedVisibility(
            visible = stopShownAt > 0,
            enter = fadeIn(tween(Tokens.fadeInMs)) + scaleIn(spring(dampingRatio = Tokens.pressDamping, stiffness = Tokens.selectStiffness), initialScale = Tokens.pillEnterScale),
            exit = fadeOut(tween(Tokens.fadeOutMs)) + scaleOut(targetScale = Tokens.pillEnterScale),
            modifier = Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = Tokens.space48),
        ) {
            StopPill(color = if (sleep) ink.copy(alpha = Tokens.sleepTextAlpha) else ink) { end(complete = false, quiet = false) }
        }
    }
}

private const val MINUTE = 60_000L
private const val FIRST_BREATH_PHASES = 2

fun phaseWord(p: Phase) = when (p) {
    Phase.In -> R.string.breathe_in
    Phase.Hold -> R.string.hold
    Phase.Out -> R.string.breathe_out
    Phase.Rest -> R.string.rest
}

/** Tinted pill in the screen's ink: one verb. */
@Composable
private fun StopPill(color: Color, onClick: () -> Unit) {
    Box(
        Modifier.height(Tokens.pillHeight).pressable(onClick = onClick)
            .clip(RoundedCornerShape(percent = 50)).background(color.copy(alpha = color.alpha * Tokens.tintAlpha))
            .padding(horizontal = Tokens.pillPadding),
        contentAlignment = Alignment.Center,
    ) { Text(stringResource(R.string.stop), style = MaterialTheme.typography.labelLarge, color = color) }
}
