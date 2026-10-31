package dev.sidali.still

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * The one place for colors, sizes, type, springs and the shared components.
 * Colors are the sidali.dev palette ("Miel du Champsaur"), seeded on Still's own color, Night blue
 * #34508A: the same blue as the app icon. Screens never pick a raw color: they ask Tokens.colors for a role.
 */
object Tokens {
    // 4 dp grid
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val space48 = 48.dp
    val space64 = 64.dp

    val gutter = 24.dp
    val cardRadius = 24.dp
    val cardPadding = 20.dp
    val cardGap = 14.dp
    val buttonRadius = 14.dp
    val buttonHeight = 54.dp
    val textWidth = 320.dp
    val check = 28.dp
    val hairline = 1.dp
    /** Smallest height of anything tappable. */
    val touchTarget = 48.dp
    /** Height of a mode card on Home. */
    val modeCardHeight = 112.dp
    /** Small round icon buttons (settings). */
    val iconButton = 44.dp
    val icon = 22.dp
    val iconStroke = 2.1.dp
    /** Orb boxes: at rest on Home, and during a session. */
    val orbHome = 220.dp
    val orbSession = 300.dp
    /** The thin ring that counts the session. */
    val ringStroke = 3.dp
    /** Outline of a quiet destructive button. */
    val buttonBorder = 1.5.dp
    /** Stop pill. */
    val pillHeight = 48.dp
    val pillPadding = 28.dp

    /** Tinted (secondary) buttons: the role color at 12 %. */
    const val tintAlpha = 0.12f
    /** A disabled action. */
    const val disabledAlpha = 0.38f

    const val pressedScale = 0.97f
    const val pressedAlpha = 0.85f
    /** Press feedback: quick and slightly bouncy, interruptible. */
    const val pressDamping = 0.55f
    const val pressStiffness = 900f
    /** Selection (mode cards, segments): colour only, on a spring. */
    const val selectStiffness = 500f
    /** The Stop pill grows in from a little smaller. */
    const val pillEnterScale = 0.9f
    /**
     * The engine already eases the breath every frame (slow in, slow out). The orb adds only a very
     * light, critically damped smoothing, so it never lags the haptics and the sound.
     */
    const val breathDamping = 1f
    const val breathStiffness = 400f
    /** The orb travels between Home, the session and the end on this spring. */
    const val orbTravelDamping = 0.85f
    const val orbTravelStiffness = 120f
    /** Orb size at fully out, as a share of fully in. */
    const val orbMinScale = 0.55f
    /** The orb at rest on Home breathes very slowly between these fullness values. */
    const val idleLow = 0.25f
    const val idleHigh = 0.75f
    const val idleBreathMs = 5000
    /** Halo rings around the orb, flat circles at decreasing alpha. */
    const val haloAlpha = 0.16f
    const val haloStep = 0.16f
    const val coreAlpha = 0.9f
    /**
     * Sleep: the orb is barely there, a flat core with no halos (halos band against true black). Its
     * alpha glows slowly between these two values. The words stay dimmer than the orb.
     */
    const val sleepCoreLow = 0.10f
    const val sleepCoreHigh = 0.16f
    const val sleepGlowMs = 7000
    const val sleepBrightness = 0.01f
    /** "Sleep well." and the Stop pill in Sleep. */
    const val sleepTextAlpha = 0.16f
    /** The breath word in Sleep: below the orb's dimmest glow, so the orb stays the guide. */
    const val sleepWordAlpha = 0.08f
    /** Track of the progress ring. */
    const val ringTrackAlpha = 0.18f
    /** The progress ring stays hidden until the session is this far along, then fades in. */
    const val ringShowAt = 0.01f

    const val fadeInMs = 300
    const val fadeOutMs = 200
    const val wordFadeMs = 700
    const val screenFadeMs = 450
    /** Cream page to night blue session and back: a slower crossfade under the travelling orb. */
    const val sessionFadeMs = 600
    /** Trust: the Arabic, the English and the reference come in one after another. */
    const val verseRevealMs = 600
    const val verseStaggerMs = 400
    /** Each verse block rises by this share of its own height as it fades in. */
    const val verseRiseDivisor = 12
    /** End: the title settles in on a soft spring. */
    const val endTitleScale = 0.92f
    const val endTitleDamping = 0.7f
    const val endTitleStiffness = 300f
    /** How long the Stop pill stays after a tap. */
    const val stopPillMs = 3000L

    /** The roles the screens use. */
    val colors: Roles
        @Composable @ReadOnlyComposable get() = LocalRoles.current

    /** The shape of the haptic breath (Haptics.kt). Times in milliseconds, scales from 0 to 1. */
    object Feel {
        const val SPAN = 0.85f // textures end a little before the phase, so the turn feels quiet
        const val MAX_PRIMITIVES = 12
        const val MAX_DELAY = 1000

        const val RISE_GUESS = 500
        const val FALL_GUESS = 100
        const val TICK_GUESS = 20

        const val RISE_EVERY = 900L
        const val RISE_FROM = 0.15f
        const val RISE_TO = 0.55f

        const val FALL_SCALE = 0.45f
        const val TICK_EVERY = 550L
        const val TICK_EASE = 0.7f
        const val TICK_FROM = 0.4f
        const val TICK_TO = 0.08f

        const val DONE_FIRST = 0.5f
        const val DONE_SECOND = 0.35f
        const val DONE_GAP = 140

        const val PULSE_MILLIS = 18L
        const val RAMP_STEP = 50L
        const val RAMP_MAX_STEPS = 200
        const val AMP_FLOOR = 8f
        const val AMP_PEAK = 90f
    }

    /** The sound mix (Sound.kt). Volumes from 0 to 1, times in milliseconds. */
    object Mix {
        const val STREAMS = 3
        const val BREATH = 0.55f
        const val CHIME = 0.7f
        const val AMBIENCE = 0.28f
        const val FADE_MILLIS = 1800f
        const val FADE_TICK = 40f
    }
}

/**
 * The palette, as plain ARGB values so a unit test can check every text and background pair against
 * WCAG AA (ContrastTest). These and the launch window colors in res/ are the only literal colors.
 */
object Palette {
    // sidali.dev family
    const val NIGHT = 0xFF34508AL
    const val INK = 0xFF1F2A22L
    const val CREAM = 0xFFFBF7EFL
    /** True black: OLED pixels off, for Sleep. */
    const val BLACK = 0xFF000000L

    object Light {
        const val PAGE = CREAM
        const val CARD = 0xFFFFFFFFL
        const val RAISED = 0xFFF2ECE0L
        const val HAIRLINE = 0xFFE6DECFL
        const val OUTLINE = 0xFF7D8C82L
        const val TEXT = INK
        const val SECONDARY = 0xFF4A5A4FL
        const val ACCENT = NIGHT
        const val ON_ACCENT = CREAM
        const val DANGER = 0xFFB3261EL
        const val ON_DANGER = 0xFFFFFFFFL
        /** Calm and Trust sessions: the whole screen in night blue, cream orb and words. */
        const val SESSION = NIGHT
        const val ON_SESSION = CREAM
    }

    object Dark {
        const val PAGE = 0xFF12201AL
        const val CARD = 0xFF1C2E26L
        const val RAISED = 0xFF263B31L
        const val HAIRLINE = 0xFF2E4539L
        const val OUTLINE = 0xFF7F978AL
        const val TEXT = 0xFFF3F0E8L
        const val SECONDARY = 0xFF9FB3A8L
        /** Night blue, lighter so it reads on the dark page (same hue). */
        const val ACCENT = 0xFFA9BFEAL
        const val ON_ACCENT = 0xFF12201AL
        const val DANGER = 0xFFFFB4ABL
        const val ON_DANGER = 0xFF690005L
        /** A deeper night blue, softer on the eyes in a dark room. */
        const val SESSION = 0xFF17233DL
        const val ON_SESSION = CREAM
    }

    object Sleep {
        const val PAGE = BLACK
        const val ORB = 0xFF8FA8DCL
    }
}

/** What each color is for. */
@Immutable
class Roles(
    /** Page behind the cards. */
    val page: Color,
    /** Cards. */
    val card: Color,
    /** One step above a card: the segmented track. */
    val raised: Color,
    /** Dividers. */
    val hairline: Color,
    /** Something that is off: an empty ring. */
    val outline: Color,
    val text: Color,
    val secondaryText: Color,
    /** The one filled action per screen, links, tinted buttons and the orb at rest. */
    val accent: Color,
    val onAccent: Color,
    /** Switches on, checks. */
    val success: Color,
    val onSuccess: Color,
    /** Destructive actions (Erase everything). */
    val danger: Color,
    val onDanger: Color,
    /** Calm and Trust sessions. */
    val session: Color,
    val onSession: Color,
    /** Sleep: black page and a dim orb. */
    val sleepPage: Color,
    val sleepOrb: Color,
)

private val LocalRoles = staticCompositionLocalOf<Roles> { error("StillTheme is missing") }

private fun c(argb: Long) = Color(argb)

private fun roles(dark: Boolean): Roles = if (dark) with(Palette.Dark) {
    Roles(
        page = c(PAGE), card = c(CARD), raised = c(RAISED), hairline = c(HAIRLINE), outline = c(OUTLINE),
        text = c(TEXT), secondaryText = c(SECONDARY), accent = c(ACCENT), onAccent = c(ON_ACCENT),
        success = c(ACCENT), onSuccess = c(ON_ACCENT), danger = c(DANGER), onDanger = c(ON_DANGER),
        session = c(SESSION), onSession = c(ON_SESSION), sleepPage = c(Palette.Sleep.PAGE), sleepOrb = c(Palette.Sleep.ORB),
    )
} else with(Palette.Light) {
    Roles(
        page = c(PAGE), card = c(CARD), raised = c(RAISED), hairline = c(HAIRLINE), outline = c(OUTLINE),
        text = c(TEXT), secondaryText = c(SECONDARY), accent = c(ACCENT), onAccent = c(ON_ACCENT),
        success = c(ACCENT), onSuccess = c(ON_ACCENT), danger = c(DANGER), onDanger = c(ON_DANGER),
        session = c(SESSION), onSession = c(ON_SESSION), sleepPage = c(Palette.Sleep.PAGE), sleepOrb = c(Palette.Sleep.ORB),
    )
}

/** The Material scheme behind the few Material components (switch, text field), from the same roles. */
private fun scheme(r: Roles, dark: Boolean) = if (dark) darkColorScheme(
    primary = r.accent, onPrimary = r.onAccent, primaryContainer = r.raised, onPrimaryContainer = r.text,
    secondary = r.accent, onSecondary = r.onAccent, tertiary = r.accent, onTertiary = r.onAccent,
    background = r.page, onBackground = r.text, surface = r.page, onSurface = r.text,
    surfaceVariant = r.raised, onSurfaceVariant = r.secondaryText, surfaceContainerHighest = r.raised,
    surfaceContainer = r.card, surfaceContainerHigh = r.raised, surfaceContainerLow = r.card,
    outline = r.outline, outlineVariant = r.hairline, error = r.danger, onError = r.onDanger,
) else lightColorScheme(
    primary = r.accent, onPrimary = r.onAccent, primaryContainer = r.raised, onPrimaryContainer = r.text,
    secondary = r.accent, onSecondary = r.onAccent, tertiary = r.accent, onTertiary = r.onAccent,
    background = r.page, onBackground = r.text, surface = r.page, onSurface = r.text,
    surfaceVariant = r.raised, onSurfaceVariant = r.secondaryText, surfaceContainerHighest = r.raised,
    surfaceContainer = r.card, surfaceContainerHigh = r.raised, surfaceContainerLow = r.card,
    outline = r.outline, outlineVariant = r.hairline, error = r.danger, onError = r.onDanger,
)

/*
 * Type: Bricolage Grotesque for titles (700, tracking -2 %), Figtree for everything else. Both are
 * variable fonts under the SIL Open Font License, bundled in res/font (licenses in assets/licenses).
 * Sizes are in sp, so the system text size is respected.
 */
@OptIn(ExperimentalTextApi::class)
private fun bricolage(opticalSize: Float) = FontFamily(
    Font(
        R.font.bricolage_grotesque, FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontWeight.Bold, FontStyle.Normal, FontVariation.Setting("opsz", opticalSize), FontVariation.width(100f),
        ),
    ),
)

private val Display = bricolage(48f)
private val Title = bricolage(18f)

@OptIn(ExperimentalTextApi::class)
private fun figtree(w: FontWeight) = Font(R.font.figtree, w, variationSettings = FontVariation.Settings(w, FontStyle.Normal))
private val Body = FontFamily(figtree(FontWeight.Normal), figtree(FontWeight.Medium), figtree(FontWeight.SemiBold), figtree(FontWeight.Bold))

private val TitleTracking = (-0.02).em

private val StillType = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking, lineHeight = 60.sp),
    displaySmall = TextStyle(fontFamily = Display, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking, lineHeight = 40.sp),
    titleMedium = TextStyle(fontFamily = Title, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = TitleTracking, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 17.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 15.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Body, fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontFamily = Body, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontFamily = Body, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em),
)

/** Arabic verse text: Amiri Quran (SIL Open Font License, bundled), large and airy, read right to left. */
val ArabicStyle = TextStyle(
    fontFamily = FontFamily(Font(R.font.amiri_quran)),
    fontSize = 32.sp, lineHeight = 64.sp, textDirection = TextDirection.Rtl, textAlign = TextAlign.Center,
)

@Composable
fun StillTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val r = remember(dark) { roles(dark) }
    val s = remember(r, dark) { scheme(r, dark) }
    val shapes = Shapes(large = RoundedCornerShape(Tokens.cardRadius))
    CompositionLocalProvider(LocalRoles provides r) {
        MaterialTheme(colorScheme = s, typography = StillType, shapes = shapes) {
            Surface(color = r.page, contentColor = r.text, content = content)
        }
    }
}

/** The tick for picking one of a few: a segment tick where the system has it. */
val selectHaptic: Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.SEGMENT_TICK
    else HapticFeedbackConstants.CLOCK_TICK

/**
 * Apple-like press: shrinks to 0.97 on a spring from the first frame, no Material ripple, and a light
 * tick through the view (so the system "touch feedback" setting is respected). A radio button ticks
 * like a segment.
 */
fun Modifier.pressable(
    role: Role = Role.Button,
    haptic: Int = if (role == Role.RadioButton) selectHaptic else HapticFeedbackConstants.CLOCK_TICK,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val view = LocalView.current
    val s by animateFloatAsState(
        if (pressed) Tokens.pressedScale else 1f,
        spring(dampingRatio = Tokens.pressDamping, stiffness = Tokens.pressStiffness),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s; alpha = if (pressed) Tokens.pressedAlpha else 1f }
        .clickable(interactionSource = src, indication = null, role = role, onClickLabel = onClickLabel) {
            view.performHapticFeedback(haptic)
            onClick()
        }
}

/** Rounded card, no shadow. */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(Tokens.cardRadius)).background(Tokens.colors.card).padding(Tokens.cardPadding),
        content = content,
    )
}

/** Small label at the top of a card, in sentence case. */
@Composable
fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Tokens.colors.secondaryText)
}

/** Large filled action: one per screen. */
@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(Tokens.buttonHeight)
            .pressable(haptic = HapticFeedbackConstants.CONFIRM, onClick = onClick)
            .clip(RoundedCornerShape(Tokens.buttonRadius)).background(Tokens.colors.accent),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Tokens.colors.onAccent, style = MaterialTheme.typography.labelLarge) }
}

/**
 * Destructive action in the semantic red: a quiet outline ([filled] false) or filled, kept for the
 * final confirmation. Dimmed and inert while not [enabled].
 */
@Composable
fun DangerButton(text: String, filled: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Tokens.colors
    Box(
        modifier.fillMaxWidth().height(Tokens.buttonHeight)
            .then(if (enabled) Modifier.pressable(haptic = if (filled) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.CLOCK_TICK, onClick = onClick) else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else Tokens.disabledAlpha }
            .clip(RoundedCornerShape(Tokens.buttonRadius))
            .then(
                if (filled) Modifier.background(c.danger)
                else Modifier.border(Tokens.buttonBorder, c.danger, RoundedCornerShape(Tokens.buttonRadius)),
            ),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (filled) c.onDanger else c.danger, style = MaterialTheme.typography.labelLarge) }
}

/** Small tinted capsule (the accent at 12 %), for an action inside a row. */
@Composable
fun Capsule(text: String, onClick: () -> Unit) {
    Box(
        Modifier.heightIn(min = Tokens.touchTarget).pressable(onClick = onClick), contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(Tokens.colors.accent.copy(alpha = Tokens.tintAlpha))
                .padding(horizontal = Tokens.space16, vertical = Tokens.space8),
        ) { Text(text, color = Tokens.colors.accent, style = MaterialTheme.typography.labelMedium) }
    }
}

/** Plain text link, centered. */
@Composable
fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text, style = MaterialTheme.typography.labelMedium, color = Tokens.colors.accent,
        modifier = Modifier.pressable(onClick = onClick).padding(vertical = Tokens.space8),
    )
}

/** Switch in the app color, with a tick as it flips. */
@Composable
fun StillSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    val view = LocalView.current
    Switch(
        checked = checked,
        onCheckedChange = onChange?.let { f -> { v: Boolean -> toggleHaptic(view, v); f(v) } },
        colors = SwitchDefaults.colors(
            checkedTrackColor = Tokens.colors.success, checkedThumbColor = Tokens.colors.onSuccess,
            checkedIconColor = Tokens.colors.success, checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = Tokens.colors.raised, uncheckedThumbColor = Tokens.colors.outline,
            uncheckedBorderColor = Tokens.colors.outline,
        ),
    )
}

/** Toggle feel: distinct on and off ticks where the system has them. */
fun toggleHaptic(view: android.view.View, on: Boolean) {
    view.performHapticFeedback(
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> HapticFeedbackConstants.CLOCK_TICK
            on -> HapticFeedbackConstants.TOGGLE_ON
            else -> HapticFeedbackConstants.TOGGLE_OFF
        },
    )
}

/** Round green check. */
@Composable
fun Check() {
    val bg = Tokens.colors.success
    val ink = Tokens.colors.onSuccess
    Canvas(Modifier.size(Tokens.check)) {
        drawCircle(bg)
        val u = size.minDimension / 28f
        drawLine(ink, Offset(8 * u, 14.5f * u), Offset(12.5f * u, 19 * u), 3f * u, StrokeCap.Round)
        drawLine(ink, Offset(12.5f * u, 19 * u), Offset(20 * u, 9.5f * u), 3f * u, StrokeCap.Round)
    }
}

/** Empty ring, same size as [Check]: something that is off. */
@Composable
fun EmptyCheck() {
    val ring = Tokens.colors.outline
    Canvas(Modifier.size(Tokens.check)) {
        val w = 2f * size.minDimension / 28f
        drawCircle(ring, radius = (size.minDimension - w) / 2, style = Stroke(w))
    }
}
