package dev.sidali.still

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StillTheme { App() } }
    }

    // The activity handles dark mode itself (no restart mid session): refresh the bar icons.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        enableEdgeToEdge()
    }
}

/** Where the app is. One screen at a time, one action per screen. */
private sealed interface Route {
    data object Home : Route
    data object Verse : Route
    data class Breathing(val mode: Mode, val minutes: Int, val id: Long = System.nanoTime()) : Route
    data class End(val minutes: Int, val today: Int) : Route
    data object SleepEnd : Route
}

/**
 * Keeps the route across process death. A running session is not restored (it stops when the app is
 * left anyway): it comes back as Home.
 */
private val RouteSaver = Saver<Route, String>(
    save = { r ->
        when (r) {
            Route.Verse -> "verse"
            is Route.End -> "end:${r.minutes}:${r.today}"
            Route.SleepEnd -> "sleepEnd"
            Route.Home, is Route.Breathing -> "home"
        }
    },
    restore = { s ->
        val parts = s.split(':')
        when (parts[0]) {
            "verse" -> Route.Verse
            "sleepEnd" -> Route.SleepEnd
            "end" -> Route.End(parts.getOrNull(1)?.toIntOrNull() ?: 0, parts.getOrNull(2)?.toIntOrNull() ?: 0)
            else -> Route.Home
        }
    },
)

/** Haptics and sound, heard together at each phase. Sleep has no breath tones: haptics only. */
class Guides(val haptics: Haptics, val sound: Sound) {
    fun listener(mode: Mode) = BreathListener { phase, millis ->
        haptics.onPhase(phase, millis)
        if (mode != Mode.Sleep) sound.onPhase(phase, millis)
    }
}

/** Settings are read off the main thread first; the launch window shows the page colour meanwhile. */
@Composable
private fun App() {
    val ctx = LocalContext.current
    val prefs by produceState<Prefs?>(null) {
        val app = ctx.applicationContext
        value = withContext(Dispatchers.IO) { Prefs(app) }
    }
    prefs?.let { AppContent(it) }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun AppContent(prefs: Prefs) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val haptics = remember { Haptics(view, prefs.haptics) }
    val sound = remember { Sound(ctx).apply { enabled = prefs.sound } }
    val guides = remember { Guides(haptics, sound) }
    LaunchedEffect(prefs.haptics) { haptics.enabled = prefs.haptics }
    LaunchedEffect(prefs.sound) { sound.enabled = prefs.sound }
    DisposableEffect(Unit) { onDispose { sound.release() } }

    var route by rememberSaveable(stateSaver = RouteSaver) { mutableStateOf<Route>(Route.Home) }
    var selected by rememberSaveable { mutableStateOf(Mode.Calm) }
    var today by remember { mutableIntStateOf(0) }
    var dataVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(route, dataVersion) {
        if (route == Route.Home) today = withContext(Dispatchers.IO) { Store.minutesToday(ctx) }
    }

    // Predictive back: from any screen but Home, back goes Home (a session stops and is kept if it was long enough).
    BackHandler(enabled = route == Route.Verse || route is Route.End || route == Route.SleepEnd) { route = Route.Home }

    // The pages crossfade; the orb travels between Home, the session and the end (Modifier.travellingOrb).
    SharedTransitionLayout {
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            val ms = if (initialState is Route.Breathing || targetState is Route.Breathing) Tokens.sessionFadeMs else Tokens.screenFadeMs
            fadeIn(tween(ms)) togetherWith fadeOut(tween(ms))
        },
        label = "route",
    ) { r ->
        CompositionLocalProvider(LocalSharedScope provides this@SharedTransitionLayout, LocalScreenScope provides this) {
        when (r) {
            Route.Home -> HomeScreen(
                prefs = prefs,
                selected = selected,
                today = today,
                onSelect = { selected = it },
                onBegin = {
                    route = if (selected == Mode.Trust) Route.Verse else Route.Breathing(selected, prefs.minutes(selected))
                },
                onErased = { dataVersion++ },
            )
            Route.Verse -> TrustScreen(
                verse = remember { Verses.forToday(LocalDate.now().dayOfYear) },
                onBreathe = { route = Route.Breathing(Mode.Trust, prefs.minutes(Mode.Trust)) },
            )
            is Route.Breathing -> SessionScreen(
                mode = r.mode, minutes = r.minutes, guides = guides, ambience = prefs.ambience,
                onFinish = { minutes, quiet, todayNow ->
                    route = when {
                        quiet || minutes < 1 -> Route.Home
                        r.mode == Mode.Sleep -> Route.SleepEnd
                        else -> Route.End(minutes, todayNow)
                    }
                },
            )
            is Route.End -> EndScreen(minutes = r.minutes, today = r.today, guides = guides, onDone = { route = Route.Home })
            Route.SleepEnd -> SleepEndScreen(onDone = { route = Route.Home })
        }
        }
    }
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * While on screen: hides the system bars, optionally keeps the screen on ([keepOn]) and lowers the
 * window brightness ([dim], Sleep). Everything is restored when the last screen asking for it leaves.
 *
 * Screens overlap during a transition (the new one arrives before the old one leaves), so each wish
 * is counted: the Sleep session handing over to the Sleep end never flashes the bars or the brightness.
 */
@Composable
fun Immersive(keepOn: Boolean, dim: Boolean) {
    val view = LocalView.current
    DisposableEffect(keepOn, dim) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        WindowWishes.hidden++
        if (keepOn) WindowWishes.keepOn++
        if (dim) WindowWishes.dim++
        WindowWishes.apply(view, window, controller)
        onDispose {
            WindowWishes.hidden--
            if (keepOn) WindowWishes.keepOn--
            if (dim) WindowWishes.dim--
            WindowWishes.apply(view, window, controller)
        }
    }
}

/** How many screens on display want the bars hidden, the screen kept on, the brightness lowered. Main thread only. */
private object WindowWishes {
    var hidden = 0
    var keepOn = 0
    var dim = 0

    fun apply(view: View, window: Window?, controller: WindowInsetsControllerCompat?) {
        view.keepScreenOn = keepOn > 0
        if (hidden > 0) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        if (window != null) {
            val wanted = if (dim > 0) Tokens.sleepBrightness else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            if (window.attributes.screenBrightness != wanted) {
                window.attributes = window.attributes.apply { screenBrightness = wanted }
            }
        }
    }
}
