package dev.sidali.still

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The few settings, in a sheet: how it feels, how long, and your data. Seven in all. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(prefs: Prefs, onClose: () -> Unit, onErased: () -> Unit) {
    val ctx = LocalContext.current
    val c = Tokens.colors
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var erasing by rememberSaveable { mutableStateOf(false) }
    fun close() = scope.launch { sheet.hide() }.invokeOnCompletion { onClose() }

    // Export: the system asks where to save still-sessions.json; the file is written off the main thread.
    val app = ctx.applicationContext
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(EXPORT_TYPE)) { uri ->
        if (uri != null) scope.launch {
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val json = Store.exportJson(app)
                    app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
                }.isSuccess
            }
            Toast.makeText(app, if (saved) R.string.export_saved else R.string.export_failed, Toast.LENGTH_SHORT).show()
        }
    }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheet, containerColor = c.page, contentColor = c.text) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = Tokens.gutter)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(Tokens.cardGap),
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(Tokens.space4))

            Card {
                SectionLabel(stringResource(R.string.settings_feel))
                Spacer(Modifier.height(Tokens.space4))
                SwitchRow(stringResource(R.string.sound), prefs.sound, { prefs.sound = it })
                Hairline()
                SwitchRow(stringResource(R.string.haptics), prefs.haptics, { prefs.haptics = it })
                Hairline()
                Spacer(Modifier.height(Tokens.space8))
                Text(stringResource(R.string.ambience), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Tokens.space8))
                // Ambience is a sound: with Sound off, it is shown dimmed and does not respond.
                Segmented(
                    Prefs.AMBIENCES.map { it to stringResource(ambienceName(it)) }, prefs.ambience, { prefs.ambience = it },
                    enabled = prefs.sound,
                )
                Spacer(Modifier.height(Tokens.space8))
                Text(stringResource(R.string.sound_silent_hint), style = MaterialTheme.typography.bodySmall, color = c.secondaryText)
            }

            Card {
                SectionLabel(stringResource(R.string.settings_length))
                Spacer(Modifier.height(Tokens.space12))
                Text(stringResource(R.string.calm_length), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Tokens.space8))
                Segmented(
                    Prefs.CALM_CHOICES.map { it to stringResource(R.string.minutes_short, it) }, prefs.calmMinutes, { prefs.calmMinutes = it },
                )
                Spacer(Modifier.height(Tokens.space16))
                Text(stringResource(R.string.sleep_length), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Tokens.space8))
                Segmented(
                    Prefs.SLEEP_CHOICES.map { it to stringResource(R.string.minutes_short, it) }, prefs.sleepMinutes, { prefs.sleepMinutes = it },
                )
            }

            Card {
                SectionLabel(stringResource(R.string.settings_data))
                Spacer(Modifier.height(Tokens.space4))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = Tokens.touchTarget).pressable {
                        try { export.launch(EXPORT_FILE) } catch (_: ActivityNotFoundException) {
                            Toast.makeText(ctx, R.string.export_failed, Toast.LENGTH_SHORT).show()
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.export), style = MaterialTheme.typography.bodyLarge, color = c.text, modifier = Modifier.weight(1f))
                    Text("›", style = MaterialTheme.typography.bodyLarge, color = c.secondaryText)
                }
                Spacer(Modifier.height(Tokens.space12))
                DangerButton(stringResource(R.string.erase), filled = false) { erasing = true }
            }
            Spacer(Modifier.height(Tokens.space16))
        }
    }

    if (erasing) EraseDialog(
        onCancel = { erasing = false },
        onErase = {
            erasing = false
            prefs.clear()
            scope.launch {
                withContext(Dispatchers.IO + NonCancellable) { Store.eraseAll(app) }
                Toast.makeText(app, R.string.erased, Toast.LENGTH_SHORT).show()
                onErased()
                close()
            }
        },
    )
}

private const val EXPORT_FILE = "still-sessions.json"
private const val EXPORT_TYPE = "application/json"

private fun ambienceName(name: String?) = when (name) {
    "wind" -> R.string.ambience_wind
    "rain" -> R.string.ambience_rain
    "night" -> R.string.ambience_night
    else -> R.string.ambience_none
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = Tokens.touchTarget), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        StillSwitch(checked, onChange)
    }
}

/**
 * A rounded segmented control: the chosen segment fills with the app color on a spring. Dimmed and
 * inert while not [enabled].
 */
@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, enabled: Boolean = true) {
    val c = Tokens.colors
    Row(
        Modifier.fillMaxWidth().graphicsLayer { alpha = if (enabled) 1f else Tokens.disabledAlpha }
            .clip(RoundedCornerShape(Tokens.buttonRadius)).background(c.raised).padding(Tokens.space4),
        horizontalArrangement = Arrangement.spacedBy(Tokens.space4),
    ) {
        for ((value, label) in options) {
            val on = value == selected
            val bg by animateColorAsState(if (on) c.accent else Color.Transparent, spring(stiffness = Tokens.selectStiffness), label = "seg")
            val fg by animateColorAsState(if (on) c.onAccent else c.text, spring(stiffness = Tokens.selectStiffness), label = "segText")
            Box(
                Modifier.weight(1f).height(Tokens.touchTarget - Tokens.space8)
                    .semantics { this.selected = on; if (!enabled) disabled() }
                    .then(if (enabled) Modifier.pressable(role = Role.RadioButton) { onSelect(value) } else Modifier)
                    .clip(RoundedCornerShape(Tokens.buttonRadius - Tokens.space4)).background(bg),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = MaterialTheme.typography.labelMedium, color = fg) }
        }
    }
}

/** Strong confirmation: type ERASE, then the filled red button wakes up. */
@Composable
private fun EraseDialog(onCancel: () -> Unit, onErase: () -> Unit) {
    val word = stringResource(R.string.erase_word)
    var typed by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onCancel) {
        Card {
            Text(stringResource(R.string.erase_title), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Tokens.space8))
            Text(stringResource(R.string.erase_body), style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText)
            Spacer(Modifier.height(Tokens.space16))
            OutlinedTextField(
                value = typed, onValueChange = { typed = it }, singleLine = true,
                placeholder = { Text(word) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                shape = RoundedCornerShape(Tokens.buttonRadius),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Tokens.space16))
            DangerButton(stringResource(R.string.erase_confirm), filled = true, enabled = typed.trim() == word, onClick = onErase)
            Spacer(Modifier.height(Tokens.space4))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TextLink(stringResource(R.string.cancel), onCancel) }
        }
    }
}
