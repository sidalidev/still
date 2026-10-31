package dev.sidali.still

import dev.sidali.still.Tokens.Mix
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper

/**
 * The breath, heard. Soft in and out tones on each phase, a chime at the end, and an optional
 * looping ambience (wind, rain, night) faded in and out at a low volume.
 *
 * Nothing plays when [enabled] is false or the phone is on silent or vibrate. Audio focus is asked
 * for, transiently, only while an ambience plays. Call [release] when the screen goes away.
 */
class Sound(ctx: Context) : BreathListener {

    private val app = ctx.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val main = Handler(Looper.getMainLooper())

    var enabled: Boolean = true
        set(value) {
            field = value
            onMain { if (value) syncAmbience() else silence() }
        }

    private val toneAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val ambienceAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(Mix.STREAMS)
        .setAudioAttributes(toneAttributes)
        .build()

    private val loaded = mutableSetOf<Int>()
    private val inId: Int
    private val outId: Int
    private val chimeId: Int
    private var toneStream = 0

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) synchronized(loaded) { loaded += id } }
        inId = pool.load(app, R.raw.breath_in, 1)
        outId = pool.load(app, R.raw.breath_out, 1)
        chimeId = pool.load(app, R.raw.chime, 1)
    }

    private val allowed: Boolean
        get() = enabled && audio?.ringerMode == AudioManager.RINGER_MODE_NORMAL

    override fun onPhase(phase: Phase, durationMillis: Long) {
        when (phase) {
            Phase.In -> tone(inId)
            Phase.Out -> tone(outId)
            Phase.Hold, Phase.Rest -> Unit // the previous tone fades out on its own
        }
        onMain { syncAmbience() } // follows the ringer if it changed mid-session
    }

    /** A single soft bell, for the end of a session. */
    fun chime() {
        play(chimeId, Mix.CHIME)
    }

    private fun tone(id: Int) {
        if (!allowed) return
        if (toneStream != 0) pool.stop(toneStream)
        toneStream = play(id, Mix.BREATH)
    }

    private fun play(id: Int, volume: Float): Int {
        if (!allowed || synchronized(loaded) { id !in loaded }) return 0
        return pool.play(id, volume, volume, 1, 0, 1f)
    }

    // Ambience. All of it runs on the main thread.

    private var ambienceName: String? = null
    private var player: MediaPlayer? = null
    private var level = 0f
    private var target = 0f
    private var releaseWhenSilent = false
    private var focus: AudioFocusRequest? = null
    private var focusLost = false

    /** Starts "wind", "rain" or "night" as a soft loop, or fades the current one out with null. */
    fun ambience(name: String?) = onMain {
        if (name == ambienceName && player != null) return@onMain
        ambienceName = name
        fadeOutAndRelease()
        if (name != null) syncAmbience()
    }

    private fun syncAmbience() {
        val name = ambienceName
        val wanted = name != null && allowed && !focusLost
        val p = player
        when {
            wanted && p == null -> start(name!!)
            wanted && p != null && !releaseWhenSilent -> {
                if (!p.isPlaying) p.start()
                fadeTo(Mix.AMBIENCE)
            }
            !wanted && p != null -> fadeTo(0f) // paused when silent, kept for a resume
        }
    }

    private fun start(name: String) {
        val res = when (name) {
            "wind" -> R.raw.amb_wind
            "rain" -> R.raw.amb_rain
            "night" -> R.raw.amb_night
            else -> return
        }
        val am = audio ?: return
        val request = focus ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(ambienceAttributes)
            .setOnAudioFocusChangeListener({ change -> onFocus(change) }, main)
            .build()
            .also { focus = it }
        if (am.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        val p = MediaPlayer.create(app, res, ambienceAttributes, am.generateAudioSessionId()) ?: return
        p.isLooping = true
        p.setVolume(0f, 0f)
        p.start()
        player = p
        level = 0f
        releaseWhenSilent = false
        fadeTo(Mix.AMBIENCE)
    }

    private fun onFocus(change: Int) {
        focusLost = change != AudioManager.AUDIOFOCUS_GAIN
        syncAmbience()
    }

    private fun fadeOutAndRelease() {
        if (player == null) return
        releaseWhenSilent = true
        fadeTo(0f)
    }

    private fun fadeTo(volume: Float) {
        target = volume
        main.removeCallbacks(fadeStep)
        main.post(fadeStep)
    }

    private val fadeStep = object : Runnable {
        override fun run() {
            val p = player ?: return
            val step = Mix.AMBIENCE * Mix.FADE_TICK / Mix.FADE_MILLIS
            level = if (level < target) minOf(target, level + step) else maxOf(target, level - step)
            p.setVolume(level, level)
            when {
                level != target -> main.postDelayed(this, Mix.FADE_TICK.toLong())
                level == 0f && releaseWhenSilent -> stopPlayer()
                level == 0f && p.isPlaying -> p.pause()
            }
        }
    }

    private fun stopPlayer() {
        main.removeCallbacks(fadeStep)
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        level = 0f
        releaseWhenSilent = false
        if (ambienceName == null) abandonFocus() else syncAmbience()
    }

    private fun abandonFocus() {
        focus?.let { audio?.abandonAudioFocusRequest(it) }
        focus = null
        focusLost = false
    }

    private fun silence() {
        if (toneStream != 0) pool.stop(toneStream)
        toneStream = 0
        if (player != null) fadeTo(0f)
    }

    /** Frees the players and gives audio focus back. The object is not usable afterwards. */
    fun release() = onMain {
        ambienceName = null
        main.removeCallbacksAndMessages(null)
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        abandonFocus()
        pool.release()
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post { block() }
    }
}
