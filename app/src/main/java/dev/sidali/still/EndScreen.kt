package dev.sidali.still

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

/**
 * The end of Calm or Trust: a soft chime, a closing tap, the orb still breathing slowly, the minutes of
 * this session and of today, and one way back. No streaks.
 */
@Composable
fun EndScreen(minutes: Int, today: Int, guides: Guides, onDone: () -> Unit) {
    val c = Tokens.colors
    val reduceMotion = rememberReduceMotion()
    val title = remember { MutableTransitionState(reduceMotion) }
    LaunchedEffect(Unit) { title.targetState = true }
    LaunchedEffect(Unit) {
        guides.sound.chime()
        guides.haptics.done()
    }
    Column(
        Modifier.fillMaxSize().background(c.page).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Tokens.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Orb(fullness = idleFullness(reduceMotion), color = c.accent, modifier = Modifier.travellingOrb().size(Tokens.orbHome))
                Spacer(Modifier.height(Tokens.space32))
                AnimatedVisibility(
                    visibleState = title,
                    enter = scaleIn(
                        spring(dampingRatio = Tokens.endTitleDamping, stiffness = Tokens.endTitleStiffness),
                        initialScale = Tokens.endTitleScale,
                    ) + fadeIn(tween(Tokens.fadeInMs)),
                ) {
                    Text(stringResource(R.string.end_title), style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(Tokens.space8))
                Text(
                    pluralStringResource(R.plurals.end_minutes, minutes, minutes),
                    style = MaterialTheme.typography.bodyLarge, color = c.secondaryText, textAlign = TextAlign.Center,
                )
                if (today > 0) Spacer(Modifier.height(Tokens.space4))
                if (today > 0) Text(
                    stringResource(R.string.today, today),
                    style = MaterialTheme.typography.bodyLarge, color = c.secondaryText, textAlign = TextAlign.Center,
                )
            }
        }
        PrimaryButton(stringResource(R.string.done), onClick = onDone)
        Spacer(Modifier.height(Tokens.space16))
    }
}

/**
 * After Sleep: nothing bright. Black, bars hidden, the brightness kept low, the screen free to turn
 * off on its own, two dim words. A tap goes Home.
 */
@Composable
fun SleepEndScreen(onDone: () -> Unit) {
    val c = Tokens.colors
    Immersive(keepOn = false, dim = true)
    Box(
        Modifier.fillMaxSize().background(c.sleepPage)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.sleep_end), style = MaterialTheme.typography.titleMedium,
            color = c.sleepOrb.copy(alpha = Tokens.sleepTextAlpha),
        )
    }
}
