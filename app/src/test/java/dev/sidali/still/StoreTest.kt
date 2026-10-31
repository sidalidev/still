package dev.sidali.still

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class StoreTest {
    private val sessions = listOf(
        Session(1_761_900_000_000L, Mode.Calm, 5),
        Session(1_761_910_000_000L, Mode.Sleep, 15),
        Session(1_761_920_000_000L, Mode.Trust, 3),
    )

    @Test fun roundTrip() {
        assertEquals(sessions, Store.parse(Store.serialize(sessions)))
        assertEquals(emptyList<Session>(), Store.parse(Store.serialize(emptyList())))
    }

    @Test fun serializedIsPlainJson() {
        val json = Store.serialize(sessions)
        assertTrue(json.trimStart().startsWith("{"))
        assertTrue(json.trimEnd().endsWith("}"))
        assertTrue(json.contains("\"mode\": \"Sleep\""))
        assertEquals(json.count { it == '{' }, json.count { it == '}' })
        assertEquals(1, json.count { it == '[' })
        assertFalse(json.contains(",\n  ]"))
    }

    @Test fun parseToleratesWhitespaceKeyOrderAndBadEntries() {
        val json = """
            {"sessions":[
              {"minutes":10,"mode":"Calm","startedAt":42},
              {"startedAt": 7, "mode": "Unknown", "minutes": 3},
              {"startedAt": 8, "mode": "Sleep"},
              {"startedAt": 9, "mode": "Sleep", "minutes": -1},
              { "startedAt" : 10 , "mode" : "Trust" , "minutes" : 3 }
            ]}
        """
        assertEquals(listOf(Session(42, Mode.Calm, 10), Session(10, Mode.Trust, 3)), Store.parse(json))
    }

    @Test fun parseNeverThrowsOnGarbage() {
        for (bad in listOf("", "{", "not json", "{\"sessions\": [", "{\"sessions\": 3}", "[]")) {
            assertEquals(emptyList<Session>(), Store.parse(bad))
        }
    }

    @Test fun minutesTodayCountsOnlyTheLocalDay() {
        val zone = ZoneId.of("Europe/Paris")
        fun at(d: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 10, d, h, m).atZone(zone).toInstant().toEpochMilli()
        val list = listOf(
            Session(at(30, 23, 59), Mode.Sleep, 20), // yesterday
            Session(at(31, 0, 1), Mode.Sleep, 10),
            Session(at(31, 12), Mode.Calm, 5),
            Session(at(31, 23, 59), Mode.Trust, 3),
            Session(at(31, 23, 59) + 60_001, Mode.Calm, 5), // tomorrow
        )
        assertEquals(18, Store.minutesOnDay(list, at(31, 15), zone))
        assertEquals(20, Store.minutesOnDay(list, at(30, 8), zone))
        assertEquals(0, Store.minutesOnDay(emptyList(), at(31, 15), zone))
    }
}
