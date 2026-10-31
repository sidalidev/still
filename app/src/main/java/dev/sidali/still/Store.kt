package dev.sidali.still

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * The user's sessions, and nothing else, in a small JSON file in the app's private storage.
 * The format is ours and tiny, so it is written and read by hand: no dependency, and the logic is
 * testable without a device.
 */
object Store {
    private const val FILE = "sessions.json"
    private const val VERSION = 1
    private val lock = Any()

    fun load(ctx: Context): List<Session> = synchronized(lock) {
        val f = file(ctx)
        if (!f.exists()) emptyList() else runCatching { parse(f.readText()) }.getOrDefault(emptyList())
    }

    fun add(ctx: Context, s: Session) = synchronized(lock) {
        write(ctx, load(ctx) + s)
    }

    fun minutesToday(ctx: Context): Int =
        minutesOnDay(load(ctx), System.currentTimeMillis(), ZoneId.systemDefault())

    /** Everything stored, as JSON the user can keep or read elsewhere. */
    fun exportJson(ctx: Context): String = serialize(load(ctx))

    /** Erases every session, in one gesture. */
    fun eraseAll(ctx: Context) = synchronized(lock) {
        file(ctx).delete()
        tmp(ctx).delete()
        Unit
    }

    private fun file(ctx: Context) = File(ctx.filesDir, FILE)
    private fun tmp(ctx: Context) = File(ctx.filesDir, "$FILE.tmp")

    /** Writes to a temporary file first, then renames it, so a crash never leaves half a file. */
    private fun write(ctx: Context, sessions: List<Session>) {
        val t = tmp(ctx)
        t.writeText(serialize(sessions))
        if (!t.renameTo(file(ctx))) {
            file(ctx).writeText(serialize(sessions))
            t.delete()
        }
    }

    // Pure helpers, tested in StoreTest.

    internal fun serialize(sessions: List<Session>): String = buildString {
        append("{\n  \"app\": \"Still\",\n  \"version\": ").append(VERSION).append(",\n  \"sessions\": [")
        sessions.forEachIndexed { i, s ->
            append(if (i == 0) "\n" else ",\n")
            append("    {\"startedAt\": ").append(s.startedAt)
            append(", \"mode\": \"").append(s.mode.name)
            append("\", \"minutes\": ").append(s.minutes).append('}')
        }
        append(if (sessions.isEmpty()) "]\n}\n" else "\n  ]\n}\n")
    }

    private val objectRe = Regex("""\{[^{}\[\]]*\}""")
    private val startedRe = Regex(""""startedAt"\s*:\s*(-?\d+)""")
    private val modeRe = Regex(""""mode"\s*:\s*"([A-Za-z]+)"""")
    private val minutesRe = Regex(""""minutes"\s*:\s*(-?\d+)""")

    /** Reads what [serialize] wrote. Entries that cannot be read are skipped, never fatal. */
    internal fun parse(json: String): List<Session> {
        val start = json.indexOf("\"sessions\"").takeIf { it >= 0 } ?: return emptyList()
        val open = json.indexOf('[', start).takeIf { it >= 0 } ?: return emptyList()
        val close = json.indexOf(']', open).takeIf { it >= 0 } ?: return emptyList()
        return objectRe.findAll(json.substring(open + 1, close)).mapNotNull { m ->
            val o = m.value
            val startedAt = startedRe.find(o)?.groupValues?.get(1)?.toLongOrNull() ?: return@mapNotNull null
            val mode = modeRe.find(o)?.groupValues?.get(1)
                ?.let { name -> Mode.entries.firstOrNull { it.name == name } } ?: return@mapNotNull null
            val minutes = minutesRe.find(o)?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null
            if (minutes < 0) null else Session(startedAt, mode, minutes)
        }.toList()
    }

    /** Minutes breathed on the local day of [now], in [zone]. */
    internal fun minutesOnDay(sessions: List<Session>, now: Long, zone: ZoneId): Int {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return sessions.filter { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() == today }
            .sumOf { it.minutes }
    }
}
