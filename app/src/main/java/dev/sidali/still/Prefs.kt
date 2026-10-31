package dev.sidali.still

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The few settings, in SharedPreferences "still", observable by the screens. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    private var _sound by mutableStateOf(sp.getBoolean(SOUND, true))
    private var _haptics by mutableStateOf(sp.getBoolean(HAPTICS, true))
    private var _ambience by mutableStateOf(sp.getString(AMBIENCE, null))
    private var _calm by mutableIntStateOf(sp.getInt(CALM, CALM_DEFAULT))
    private var _sleep by mutableIntStateOf(sp.getInt(SLEEP, SLEEP_DEFAULT))

    // Each setter updates the screens and writes through.
    var sound: Boolean get() = _sound
        set(v) { _sound = v; sp.edit().putBoolean(SOUND, v).apply() }
    var haptics: Boolean get() = _haptics
        set(v) { _haptics = v; sp.edit().putBoolean(HAPTICS, v).apply() }
    var ambience: String? get() = _ambience
        set(v) { _ambience = v; sp.edit().putString(AMBIENCE, v).apply() }
    var calmMinutes: Int get() = _calm
        set(v) { _calm = v; sp.edit().putInt(CALM, v).apply() }
    var sleepMinutes: Int get() = _sleep
        set(v) { _sleep = v; sp.edit().putInt(SLEEP, v).apply() }

    /** Back to the defaults (Erase everything). */
    fun clear() {
        sp.edit().clear().apply()
        _sound = true; _haptics = true; _ambience = null; _calm = CALM_DEFAULT; _sleep = SLEEP_DEFAULT
    }

    /** Session length for [mode], in minutes. */
    fun minutes(mode: Mode) = when (mode) {
        Mode.Calm -> calmMinutes
        Mode.Sleep -> sleepMinutes
        Mode.Trust -> TRUST_MINUTES
    }

    companion object {
        const val NAME = "still"
        const val SOUND = "sound"
        const val HAPTICS = "haptics"
        const val AMBIENCE = "ambience"
        const val CALM = "calm_minutes"
        const val SLEEP = "sleep_minutes"
        const val CALM_DEFAULT = 5
        const val SLEEP_DEFAULT = 10
        const val TRUST_MINUTES = 3
        val CALM_CHOICES = listOf(3, 5, 10)
        val SLEEP_CHOICES = listOf(10, 15, 20)
        val AMBIENCES = listOf(null, "wind", "rain", "night")
    }
}
