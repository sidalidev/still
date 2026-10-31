package dev.sidali.still

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class AboutTest {
    @Test fun fingerprintShowsFirstEightAndLastFour() =
        assertEquals("b2d10abf…c2d3", shortFingerprint("b2d10abfec2170c29d4c6f957bbb353608f9cf5591d8b3cd01b4694cc8d8c2d3"))

    @Test fun reviewDateIsTheLatestReview() {
        val latest = File("../docs/security").list()!!.filter { Regex("""\d{4}-\d{2}-\d{2}\.md""").matches(it) }.max()
        assertEquals(latest.removeSuffix(".md"), BuildConfig.SECURITY_REVIEW)
        assertTrue(securityReviewDate(), Regex("""\d{1,2} [A-Z][a-z]{2} \d{4}""").matches(securityReviewDate()))
    }

    @Test fun securityMdHasTheSameShortFingerprint() {
        val md = File("../SECURITY.md").readText()
        // Skipped until the release key exists and its fingerprint is published in SECURITY.md.
        val match = Regex("`([0-9a-f]{64})`").find(md)
        assumeTrue("No signing fingerprint in SECURITY.md yet", match != null)
        val full = match!!.groupValues[1]
        assertTrue(md.contains(shortFingerprint(full)))
    }
}
