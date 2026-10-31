package dev.sidali.still

import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Every text and background pair of the palette meets WCAG AA (4.5:1 for text, 3:1 for shapes). */
class ContrastTest {
    private fun channel(v: Long): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(argb: Long) =
        0.2126 * channel(argb shr 16 and 0xFF) + 0.7152 * channel(argb shr 8 and 0xFF) + 0.0722 * channel(argb and 0xFF)

    /** [fg] at [alpha] over [bg], as an opaque color. */
    private fun over(fg: Long, bg: Long, alpha: Double): Long {
        fun mix(shift: Int) = Math.round(((fg shr shift and 0xFF) * alpha + (bg shr shift and 0xFF) * (1 - alpha))).toLong()
        return (0xFFL shl 24) or (mix(16) shl 16) or (mix(8) shl 8) or mix(0)
    }

    private fun ratio(a: Long, b: Long): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    private fun text(name: String, fg: Long, bg: Long) =
        assertTrue("$name: %.2f".format(ratio(fg, bg)), ratio(fg, bg) >= 4.5)

    private fun shape(name: String, fg: Long, bg: Long) =
        assertTrue("$name: %.2f".format(ratio(fg, bg)), ratio(fg, bg) >= 3.0)

    @Test fun light() = with(Palette.Light) {
        for ((n, bg) in listOf("page" to PAGE, "card" to CARD, "raised" to RAISED)) {
            text("text on $n", TEXT, bg)
            text("secondary on $n", SECONDARY, bg)
            text("accent on $n", ACCENT, bg)
        }
        text("on accent", ON_ACCENT, ACCENT)
        text("danger on card", DANGER, CARD)
        text("on danger", ON_DANGER, DANGER)
        text("capsule", ACCENT, over(ACCENT, CARD, Tokens.tintAlpha.toDouble()))
        text("danger tint", DANGER, over(DANGER, CARD, Tokens.tintAlpha.toDouble()))
        shape("off ring", OUTLINE, CARD)
        shape("orb on page", ACCENT, PAGE)
        session(SESSION, ON_SESSION)
    }

    @Test fun dark() = with(Palette.Dark) {
        for ((n, bg) in listOf("page" to PAGE, "card" to CARD, "raised" to RAISED)) {
            text("text on $n", TEXT, bg)
            text("secondary on $n", SECONDARY, bg)
            text("accent on $n", ACCENT, bg)
        }
        text("on accent", ON_ACCENT, ACCENT)
        text("danger on card", DANGER, CARD)
        text("on danger", ON_DANGER, DANGER)
        text("capsule", ACCENT, over(ACCENT, CARD, Tokens.tintAlpha.toDouble()))
        text("danger tint", DANGER, over(DANGER, CARD, Tokens.tintAlpha.toDouble()))
        shape("off ring", OUTLINE, CARD)
        shape("orb on page", ACCENT, PAGE)
        session(SESSION, ON_SESSION)
    }

    /** Calm and Trust sessions: cream words on night blue, and the Stop pill (cream tint). */
    private fun session(bg: Long, ink: Long) {
        text("words on session", ink, bg)
        text("stop pill", ink, over(ink, bg, Tokens.tintAlpha.toDouble()))
        shape("progress ring", over(ink, bg, Tokens.coreAlpha.toDouble()), bg)
    }

    /** The icon and the app color: cream on Night blue. */
    @Test fun icon() = text("cream on night blue", Palette.CREAM, Palette.NIGHT)
}
