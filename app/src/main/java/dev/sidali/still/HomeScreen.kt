package dev.sidali.still

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

@Composable
fun HomeScreen(
    prefs: Prefs,
    selected: Mode,
    today: Int,
    onSelect: (Mode) -> Unit,
    onBegin: () -> Unit,
    onErased: () -> Unit,
) {
    val ctx = LocalContext.current
    val c = Tokens.colors
    val reduceMotion = rememberReduceMotion()
    var settings by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(c.page).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Tokens.gutter),
    ) {
        Spacer(Modifier.height(Tokens.space16))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displayLarge, modifier = Modifier.weight(1f))
            SettingsButton { settings = true }
        }
        Spacer(Modifier.height(Tokens.space4))
        Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyLarge, color = c.secondaryText)

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Orb(fullness = idleFullness(reduceMotion), color = c.accent, modifier = Modifier.travellingOrb().size(Tokens.orbHome))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Tokens.space12)) {
            for (mode in Mode.entries) ModeCard(
                mode = mode, minutes = prefs.minutes(mode), selected = mode == selected,
                modifier = Modifier.weight(1f), onClick = { onSelect(mode) },
            )
        }
        Spacer(Modifier.height(Tokens.space24))
        PrimaryButton(stringResource(R.string.begin), onClick = onBegin)
        Spacer(Modifier.height(Tokens.space8))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Today's minutes only once there are some: a zero would read as a quiet nag.
            Box(Modifier.weight(1f)) {
                if (today > 0) Text(
                    stringResource(R.string.today, today), style = MaterialTheme.typography.bodyMedium, color = c.secondaryText,
                )
            }
            TextLink(stringResource(R.string.about)) { AboutActivity.open(ctx) }
        }
        Spacer(Modifier.height(Tokens.space8))
    }

    if (settings) SettingsSheet(prefs = prefs, onClose = { settings = false }, onErased = onErased)
}

@Composable
private fun ModeCard(mode: Mode, minutes: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Tokens.colors
    // Selection shows by colour only, so the three cards always line up. The press gives the scale.
    val bg by animateColorAsState(if (selected) c.accent else c.card, spring(stiffness = Tokens.selectStiffness), label = "bg")
    val fg by animateColorAsState(if (selected) c.onAccent else c.text, spring(stiffness = Tokens.selectStiffness), label = "fg")
    val sub by animateColorAsState(if (selected) c.onAccent else c.secondaryText, spring(stiffness = Tokens.selectStiffness), label = "sub")
    Column(
        modifier.height(Tokens.modeCardHeight)
            .semantics { this.selected = selected }
            .pressable(role = Role.RadioButton, onClick = onClick)
            .clip(RoundedCornerShape(Tokens.cardRadius)).background(bg)
            .padding(Tokens.space16),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ModeGlyph(mode, fg)
        Column {
            Text(stringResource(modeName(mode)), style = MaterialTheme.typography.titleMedium, color = fg)
            Text(stringResource(R.string.minutes_short, minutes), style = MaterialTheme.typography.bodySmall, color = sub)
        }
    }
}

fun modeName(mode: Mode) = when (mode) {
    Mode.Calm -> R.string.mode_calm
    Mode.Sleep -> R.string.mode_sleep
    Mode.Trust -> R.string.mode_trust
}

/** Rounded line glyphs on a 24 grid: a breath wave, a crescent, a rising sun. */
@Composable
private fun ModeGlyph(mode: Mode, color: Color) {
    val w = Tokens.iconStroke
    Canvas(Modifier.size(Tokens.icon)) {
        val u = size.minDimension / GRID
        val stroke = Stroke(w.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (mode) {
            Mode.Calm -> for (y in listOf(9f, 15f)) drawPath(wave(u, y), color, style = stroke)
            Mode.Sleep -> drawPath(crescent(u), color, style = stroke)
            Mode.Trust -> {
                // A sunrise: half a sun on the horizon, three short rays.
                drawArc(color, 180f, 180f, false, Offset(7 * u, 11 * u), Size(10 * u, 10 * u), style = stroke)
                drawLine(color, Offset(3 * u, 16 * u), Offset(21 * u, 16 * u), stroke.width, StrokeCap.Round)
                for ((from, to) in SUN_RAYS) {
                    drawLine(color, Offset(from.first * u, from.second * u), Offset(to.first * u, to.second * u), stroke.width, StrokeCap.Round)
                }
            }
        }
    }
}

private const val GRID = 24f
/** Sunrise rays, on the 24 grid: up, up-left, up-right, all pointing away from the sun's centre (12, 16). */
private val SUN_RAYS = listOf(
    (12f to 4f) to (12f to 6.5f),
    (5.5f to 9.5f) to (7.2f to 11.2f),
    (18.5f to 9.5f) to (16.8f to 11.2f),
)

private fun DrawScope.wave(u: Float, y: Float) = Path().apply {
    moveTo(3 * u, y * u)
    cubicTo(6 * u, (y - 3) * u, 9 * u, (y - 3) * u, 12 * u, y * u)
    cubicTo(15 * u, (y + 3) * u, 18 * u, (y + 3) * u, 21 * u, y * u)
}

private fun DrawScope.crescent(u: Float) = Path().apply {
    moveTo(15 * u, 3.5f * u)
    cubicTo(8 * u, 3 * u, 3.5f * u, 8 * u, 3.5f * u, 12.5f * u)
    cubicTo(3.5f * u, 17 * u, 7.5f * u, 20.5f * u, 12 * u, 20.5f * u)
    cubicTo(16 * u, 20.5f * u, 19 * u, 18.5f * u, 20.5f * u, 15 * u)
    cubicTo(14 * u, 16.5f * u, 10 * u, 10 * u, 15 * u, 3.5f * u)
    close()
}

/** Round button with a "tune" glyph: two rounded sliders. */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    val c = Tokens.colors
    val desc = stringResource(R.string.settings)
    val w = Tokens.iconStroke
    Box(
        Modifier.size(Tokens.iconButton).semantics { contentDescription = desc }
            .pressable(onClick = onClick).clip(CircleShape).background(c.card),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(Tokens.icon)) {
            val u = size.minDimension / GRID
            val sw = w.toPx()
            drawLine(c.text, Offset(4 * u, 8 * u), Offset(20 * u, 8 * u), sw, StrokeCap.Round)
            drawLine(c.text, Offset(4 * u, 16 * u), Offset(20 * u, 16 * u), sw, StrokeCap.Round)
            for ((x, y) in listOf(15f to 8f, 9f to 16f)) {
                drawCircle(c.card, radius = 3 * u, center = Offset(x * u, y * u))
                drawCircle(c.text, radius = 3 * u, center = Offset(x * u, y * u), style = Stroke(sw))
            }
        }
    }
}
