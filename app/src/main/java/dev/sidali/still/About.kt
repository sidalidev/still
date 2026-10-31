package dev.sidali.still

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * About and "What leaves your device" (the same activity with [EXTRA_PAGE]).
 * Every link leaves the app only on a tap, and opens in the browser or the mail app: Still itself
 * has no internet permission.
 */
class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val page = intent.getStringExtra(EXTRA_PAGE)
        setContent {
            StillTheme {
                if (page == PAGE_LEAVES) Leaves(onClose = ::finish)
                else About(onClose = ::finish)
            }
        }
    }

    companion object {
        private const val EXTRA_PAGE = "page"
        const val PAGE_LEAVES = "leaves"
        fun open(ctx: Context, page: String? = null) =
            ctx.startActivity(Intent(ctx, AboutActivity::class.java).putExtra(EXTRA_PAGE, page))
    }
}

private object Links {
    const val REPO = "https://github.com/sidalidev/still"
    const val REPORT = "$REPO/security/advisories/new"
    const val PRIVACY = "$REPO/blob/main/PRIVACY.md"
    const val CONTACT = "mailto:contact@sidali.dev"
    fun release(version: String) = "$REPO/releases/tag/v$version"
}

/** Opens [url] in the browser (or the mail app for mailto:). */
private fun openLink(ctx: Context, url: String) {
    val uri = Uri.parse(url)
    val intent = Intent(if (uri.scheme == "mailto") Intent.ACTION_SENDTO else Intent.ACTION_VIEW, uri)
    try { ctx.startActivity(intent) } catch (_: ActivityNotFoundException) {
        Toast.makeText(ctx, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
    }
}

/** SHA-256 of the certificate that signed the installed APK, read from Android, in lowercase hex. */
fun signingCertificateSha256(ctx: Context): String? = runCatching {
    @Suppress("DEPRECATION")
    val info = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    val cert = info.signingInfo?.apkContentsSigners?.firstOrNull() ?: return null
    MessageDigest.getInstance("SHA-256").digest(cert.toByteArray()).joinToString("") { "%02x".format(it) }
}.getOrNull()

/** First 8 and last 4 characters, enough to compare with SECURITY.md at a glance. */
fun shortFingerprint(sha256: String) = "${sha256.take(8)}…${sha256.takeLast(4)}"

/** The date of the latest file in docs/security/, set at build time, as "24 Oct 2026". */
fun securityReviewDate(): String =
    LocalDate.parse(BuildConfig.SECURITY_REVIEW).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

@Composable
internal fun Page(close: String, title: String, onClose: () -> Unit, content: @Composable () -> Unit) {
    Column(
        // Content scrolls below the status bar, not under it.
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom)).padding(horizontal = Tokens.gutter),
    ) {
        Spacer(Modifier.height(Tokens.space8))
        TextLink(close, onClose)
        Spacer(Modifier.height(Tokens.space16))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(Tokens.space24))
        Column(verticalArrangement = Arrangement.spacedBy(Tokens.cardGap)) { content() }
        Spacer(Modifier.height(Tokens.space24))
    }
}

@Composable
internal fun Hairline() {
    Spacer(Modifier.height(Tokens.space4))
    Box(Modifier.fillMaxWidth().height(Tokens.hairline).background(Tokens.colors.hairline))
    Spacer(Modifier.height(Tokens.space4))
}

/** A full-width row that does one thing when tapped. */
@Composable
private fun LinkRow(text: String, trailing: String? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = Tokens.touchTarget).pressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = Tokens.colors.accent, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = MaterialTheme.typography.bodyLarge, color = Tokens.colors.secondaryText)
    }
}

@Composable
private fun About(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val version = BuildConfig.VERSION_NAME
    val cert = remember { signingCertificateSha256(ctx) }

    Page(stringResource(R.string.close), stringResource(R.string.about_title), onClose) {
        Card {
            SectionLabel(stringResource(R.string.about_principles))
            Spacer(Modifier.height(Tokens.space12))
            for (line in listOf(R.string.principle_1, R.string.principle_2, R.string.principle_3, R.string.principle_4)) {
                Text(stringResource(line), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(Tokens.space8))
            }
            Hairline()
            LinkRow(stringResource(R.string.leaves_title), trailing = "›") { AboutActivity.open(ctx, AboutActivity.PAGE_LEAVES) }
        }

        Card {
            SectionLabel(stringResource(R.string.about_version))
            Spacer(Modifier.height(Tokens.space12))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.version_line, version), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(Tokens.space12))
                Capsule(stringResource(R.string.whats_new)) { openLink(ctx, Links.release(version)) }
            }
        }

        Card {
            SectionLabel(stringResource(R.string.about_security))
            Spacer(Modifier.height(Tokens.space12))
            Text(stringResource(R.string.security_review, securityReviewDate()), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(Tokens.space4))
            Text(stringResource(R.string.security_audit), style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText)
            Spacer(Modifier.height(Tokens.space12))
            Hairline()
            Row(Modifier.heightIn(min = Tokens.touchTarget), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.signing_certificate), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(
                    cert?.let { shortFingerprint(it) } ?: stringResource(R.string.unknown),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = Tokens.colors.secondaryText,
                )
            }
            Hairline()
            LinkRow(stringResource(R.string.report_issue)) { openLink(ctx, Links.REPORT) }
        }

        Card {
            SectionLabel(stringResource(R.string.about_links))
            Spacer(Modifier.height(Tokens.space4))
            LinkRow(stringResource(R.string.source_code)) { openLink(ctx, Links.REPO) }
            Hairline()
            LinkRow(stringResource(R.string.privacy_policy)) { openLink(ctx, Links.PRIVACY) }
            Hairline()
            LinkRow(stringResource(R.string.contact)) { openLink(ctx, Links.CONTACT) }
        }

        Card {
            Text(stringResource(R.string.nothing_stored), style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText)
        }

        // Footer: who made it, and the credits for the bundled fonts, as one block.
        Column(Modifier.fillMaxWidth().padding(top = Tokens.space8), verticalArrangement = Arrangement.spacedBy(Tokens.space4)) {
            for (line in listOf(R.string.made_by, R.string.credits_fonts)) Text(
                stringResource(line), style = MaterialTheme.typography.bodySmall, color = Tokens.colors.secondaryText,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }

}

@Composable
private fun Leaves(onClose: () -> Unit) {
    Page(stringResource(R.string.back), stringResource(R.string.leaves_title), onClose) {
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Check()
                Spacer(Modifier.width(Tokens.space12))
                Text(stringResource(R.string.leaves_nothing), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
        }
        Card {
            val items = listOf(
                R.string.leaves_sessions_title to R.string.leaves_sessions,
                R.string.leaves_backups_title to R.string.leaves_backups,
                R.string.leaves_links_title to R.string.leaves_links,
            )
            items.forEachIndexed { i, (title, body) ->
                if (i > 0) { Spacer(Modifier.height(Tokens.space12)); Hairline(); Spacer(Modifier.height(Tokens.space12)) }
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(Tokens.space4))
                Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = Tokens.colors.secondaryText)
            }
        }
    }
}
