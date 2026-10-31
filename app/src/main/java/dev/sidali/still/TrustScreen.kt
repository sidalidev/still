package dev.sidali.still

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign

/**
 * Trust, first step: one verse, read calmly, then one action. The Arabic, the English and the
 * reference come in one after another.
 */
@Composable
fun TrustScreen(verse: Verses.Verse, onBreathe: () -> Unit) {
    val c = Tokens.colors
    val reduceMotion = rememberReduceMotion()
    val shown = remember { MutableTransitionState(reduceMotion) }
    LaunchedEffect(Unit) { shown.targetState = true }
    Column(
        Modifier.fillMaxSize().background(c.page).windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = Tokens.gutter),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(vertical = Tokens.space32),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Tokens.space16),
            ) {
                SectionLabel(stringResource(R.string.verse_label))
                Card {
                    Reveal(shown, 0) {
                        Text(verse.arabic, style = ArabicStyle, color = c.text, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(Tokens.space20))
                    Reveal(shown, 1) {
                        Text(
                            verse.english, style = MaterialTheme.typography.bodyLarge, color = c.text,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(Tokens.space16))
                    Reveal(shown, 2) {
                        Text(
                            verse.ref, style = MaterialTheme.typography.labelMedium, color = c.secondaryText,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Text(
                    stringResource(R.string.verse_translation), style = MaterialTheme.typography.bodySmall,
                    color = c.secondaryText, textAlign = TextAlign.Center,
                )
            }
        }
        PrimaryButton(stringResource(R.string.breathe), onClick = onBreathe)
        Spacer(Modifier.height(Tokens.space16))
    }
}

/** The [order]-th block of the verse: fades in and rises a little, [order] steps after the first. */
@Composable
private fun ColumnScope.Reveal(state: MutableTransitionState<Boolean>, order: Int, content: @Composable () -> Unit) {
    val delay = order * Tokens.verseStaggerMs
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(Tokens.verseRevealMs, delayMillis = delay)) +
            slideInVertically(tween(Tokens.verseRevealMs, delayMillis = delay)) { it / Tokens.verseRiseDivisor },
    ) { content() }
}
