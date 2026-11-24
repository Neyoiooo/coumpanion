package com.coucou.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.core.IconKind
import com.coucou.android.core.Spacing
import com.coucou.android.core.Tokens
import com.coucou.android.core.TypeScale
import com.coucou.android.link.PairingPayload

/**
 * The server pairing steps and a paste button for same-phone setup. A QR scanner is available for a second screen.
 */
@Composable
fun PairingScreen(
    model: AppModel, onScan: () -> Unit, onPaste: () -> Unit, onAbout: () -> Unit, onWatchApps: () -> Unit,
    onOverlay: () -> Unit, showPairing: Boolean, onShowPairing: (Boolean) -> Unit,
    onActivity: () -> Unit = onAbout,
) {
    val t = tokens()
    androidx.activity.compose.BackHandler(enabled = showPairing) { onShowPairing(false) }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState())
            .padding(start = Gutter + 8.dp, end = Gutter + 8.dp, bottom = BarClearance),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.size(20.dp))
        if (!showPairing) {
            model.message?.let { MessagePanel(it) { model.message = null }; Spacer(Modifier.size(Gap)) }
            CoupiWelcome(model.manualTracking.size, false, onHistory = onActivity)
            Spacer(Modifier.size(Gap))
            AiLauncher(model, onOverlay)
            if (com.coucou.android.app.NotificationWatchAccess.available) {
                Spacer(Modifier.size(Gap))
                PillButton(stringResource(R.string.watch_apps_title), onWatchApps, Modifier.fillMaxWidth())
            }
            if (model.watchedActivity.isNotEmpty()) {
                Spacer(Modifier.size(Gap))
                WatchedActivityPanel(model, limit = 3)
            }
            Spacer(Modifier.size(Gap))
            PillButton(stringResource(R.string.pairing_headline), { onShowPairing(true) }, Modifier.fillMaxWidth())
            return@Column
        }
        ProviderAvatar("coumpanion", "", com.coucou.android.core.AvatarMood.IDLE, Modifier.size(96.dp))
        Spacer(Modifier.size(20.dp))
        Text(stringResource(R.string.pairing_headline), style = TypeScale.DISPLAY.style(t.text.c()).copy(textAlign = TextAlign.Center))
        Text(
            stringResource(
                R.string.pairing_help,
                stringResource(R.string.pair_paste_short),
                stringResource(R.string.pair_clipboard),
                stringResource(R.string.pair_button),
            ),
            Modifier.padding(top = 10.dp, bottom = 28.dp),
            style = TypeScale.BODY.style(t.textDim.c()),
        )
        model.message?.let { MessagePanel(it) { model.message = null }; Spacer(Modifier.size(Gap)) }
        PillButton(
            stringResource(R.string.pair_paste_short), onPaste, Modifier.fillMaxWidth(), PillKind.PRIMARY,
        )
        Spacer(Modifier.size(Gap))
        PillButton(
            stringResource(R.string.scan_button), onScan, Modifier.fillMaxWidth(),
            icon = { CoucouIcon(IconKind.SCAN, tint = t.text.c(), size = 20.dp) },
        )
        Spacer(Modifier.size(32.dp))
        Text(stringResource(R.string.pairing_credit), style = TypeScale.SECONDARY.style(t.textFaint.c()).copy(textAlign = TextAlign.Center))
        Text(
            stringResource(R.string.pairing_about),
            Modifier.heightIn(min = Spacing.MIN_TOUCH.dp).clickable(role = Role.Button, onClick = onAbout).padding(horizontal = 16.dp, vertical = 14.dp),
            style = TypeScale.SECONDARY.style(t.textDim.c()).copy(textDecoration = TextDecoration.Underline),
        )
    }
}

/** The pairing link as text, for people who cannot scan. A link that is not a Coucou one is refused here, with the same words as everywhere. */
@Composable
internal fun PairingPasteSheet(model: AppModel, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val white = Color(tokens().heroText)
    val dim = Color(tokens().heroTextDim)
    fun pairWith(link: String) {
        if (PairingPayload.parse(link) != null) { model.pair(link); onDismiss() } else bad = true
    }
    BottomSheetHost(onDismiss) {
        Text(stringResource(R.string.paste_title), style = TypeScale.TITLE.style(white))
        Text(stringResource(R.string.paste_hint), Modifier.padding(top = 4.dp, bottom = 12.dp), style = TypeScale.SECONDARY.style(dim))
        OutlinedTextField(
            text, { text = it; bad = false }, Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp),
            placeholder = { Text(stringResource(R.string.paste_title), color = Color(Tokens.DARK.textFaint)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = white, unfocusedTextColor = white, cursorColor = white,
                focusedBorderColor = white, unfocusedBorderColor = Color(tokens().heroText).copy(alpha = 0.25f),
            ),
        )
        if (bad) Text(stringResource(R.string.msg_bad_link), Modifier.padding(top = 8.dp), style = TypeScale.SECONDARY.style(Color(Tokens.DARK.danger)))
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                stringResource(R.string.pair_clipboard),
                { text = clipboard.getText()?.text.orEmpty().trim(); bad = false }, Modifier.weight(1f), onDark = true,
            )
            PillButton(stringResource(R.string.pair_button), { pairWith(text.trim()) }, Modifier.weight(1f), PillKind.PRIMARY, enabled = text.isNotBlank(), onDark = true)
        }
    }
}

/** A short notice with a Close button, in a panel. */
@Composable
fun MessagePanel(text: String, onClose: () -> Unit) {
    val t = tokens()
    Panel {
        Row(Modifier.padding(start = Spacing.INSIDE.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, Modifier.weight(1f).padding(vertical = 12.dp), style = TypeScale.BODY.style(t.text.c()))
            Text(
                stringResource(R.string.action_close),
                Modifier.heightIn(min = Spacing.MIN_TOUCH.dp).clickable(role = Role.Button, onClick = onClose).padding(horizontal = 16.dp, vertical = 14.dp),
                style = TypeScale.LABEL.style(t.textDim.c()),
            )
        }
    }
}
