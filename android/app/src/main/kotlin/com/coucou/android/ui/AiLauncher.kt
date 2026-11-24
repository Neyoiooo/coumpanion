package com.coucou.android.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.app.AvatarArtwork
import com.coucou.android.app.ProviderLogos
import com.coucou.android.core.AiProvider
import com.coucou.android.core.AvatarMood
import com.coucou.android.core.ManualAiSession
import com.coucou.android.core.ProviderAppearance
import com.coucou.android.core.TypeScale
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AiLauncher(model: AppModel, onOverlay: () -> Unit) {
    val t = tokens()
    val active = model.manualTracking
    val now by trackingClock(active)
    val all = model.aiProviders
    val installed = model.installedWatchApps.map { it.packageName }
    var editing by remember { mutableStateOf<AiProvider?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ai_launcher), style = TypeScale.TITLE.style(t.text.c()))
            TextButton(onClick = { editing = null; editorOpen = true }) { Text(stringResource(R.string.ai_add_provider)) }
        }
        Text(stringResource(R.string.manual_ai_unconfirmed), style = TypeScale.SECONDARY.style(t.textDim.c()))
        val ordered = all.sortedBy { provider ->
            when {
                active.any { it.provider.id == provider.id } -> 0
                provider.website != null || provider.packageName in installed || provider.isCustom -> 1
                else -> 2
            }
        }
        ordered.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { provider ->
                    val session = active.firstOrNull { it.provider.id == provider.id }
                    AiProviderCard(model, provider, session, session?.let { elapsedTracking(it, now) }, Modifier.weight(1f),
                        onEdit = { editing = provider; editorOpen = true })
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
        if (active.isNotEmpty() && (!model.overlayPermission || !model.overlayWished)) {
            PillButton(stringResource(R.string.watch_apps_island), onOverlay, Modifier.fillMaxWidth())
        }
    }
    if (editorOpen) ProviderEditor(model, editing, onClose = { editorOpen = false })
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AiProviderCard(
    model: AppModel,
    provider: AiProvider,
    session: ManualAiSession?,
    elapsed: String?,
    modifier: Modifier,
    onEdit: () -> Unit,
) {
    val t = tokens()
    val context = LocalContext.current
    var actions by remember(provider.id) { mutableStateOf(false) }
    val actionTitle = stringResource(R.string.ai_provider_actions)
    val availability = providerAvailability(model, provider)
    Box(modifier) {
        Panel {
            Column(
                Modifier.fillMaxWidth().combinedClickable(
                    role = Role.Button,
                    onClick = {
                        val auto = provider.autoTrack ?: model.settings.autoTrackLaunches
                        model.launchProvider(context, provider, track = session == null && auto)
                    },
                    onLongClickLabel = actionTitle,
                    onLongClick = { actions = true },
                ).padding(start = 10.dp, end = 6.dp, bottom = 10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    ProviderAvatar(provider.id, provider.label, sessionMood(session), Modifier.size(48.dp), provider = provider)
                    TextButton(onClick = { actions = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("⋯", style = TypeScale.TITLE.style(t.text.c()))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProviderLogo(provider, Modifier.size(17.dp))
                    Text(provider.label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = TypeScale.BODY.style(t.text.c()))
                }
                Text(stringResource(availability), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = TypeScale.SECONDARY.style(t.textDim.c()))
                Text(when {
                    session == null -> stringResource(R.string.ai_no_timer)
                    session.paused -> stringResource(R.string.ai_timer_status, stringResource(R.string.ai_timer_paused), elapsed.orEmpty())
                    else -> stringResource(R.string.ai_timer_status, stringResource(R.string.manual_ai_tracking), elapsed.orEmpty())
                }, style = TypeScale.SECONDARY.style(t.text.c()))
            }
        }
        DropdownMenu(expanded = actions, onDismissRequest = { actions = false }) {
            ProviderAction(R.string.ai_open_provider) {
                actions = false
                model.launchProvider(context, provider, track = false)
            }
            if (session == null && availability != R.string.ai_not_installed) {
                ProviderAction(R.string.ai_start_timer) { actions = false; model.setManualTracking(provider.id, true) }
            }
            if (session != null) {
                ManualTimerActions(session, model, close = { actions = false })
            }
            if (provider.isCustom) {
                ProviderAction(R.string.ai_edit_provider) { actions = false; onEdit() }
            }
        }
    }
}

@Composable
internal fun ManualTimerActions(
    session: ManualAiSession,
    model: AppModel,
    close: () -> Unit,
) {
    ManualTimerActions(session, close, model::pauseTracking, model::restartTracking, model::markTrackingDone,
        stop = { model.setManualTracking(it, false) })
}

@Composable
internal fun ManualTimerActions(
    session: ManualAiSession,
    close: () -> Unit,
    pause: (String, Boolean) -> Unit,
    restart: (String) -> Unit,
    done: (String) -> Unit,
    stop: (String) -> Unit,
) {
    val id = session.provider.id
    ProviderAction(if (session.paused) R.string.ai_resume_timer else R.string.ai_pause_timer) { close(); pause(id, !session.paused) }
    ProviderAction(R.string.ai_restart_timer) { close(); restart(id) }
    ProviderAction(R.string.ai_mark_done) { close(); done(id) }
    ProviderAction(R.string.chat_stop) { close(); stop(id) }
}

@Composable
private fun ProviderAction(label: Int, action: () -> Unit) =
    DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = action)

internal fun sessionMood(session: ManualAiSession?): AvatarMood =
    if (session == null || session.paused) AvatarMood.IDLE else AvatarMood.TRACKING

internal fun providerAvailability(model: AppModel, provider: AiProvider): Int = when {
    provider.website != null -> R.string.ai_website
    model.installedWatchApps.any { it.packageName == provider.packageName } -> R.string.ai_installed
    provider.isCustom -> R.string.ai_app_availability_unknown
    else -> R.string.ai_not_installed
}

@Composable
private fun ProviderLogo(provider: AiProvider, modifier: Modifier) {
    val context = LocalContext.current.applicationContext
    val appearance = remember(provider) { ProviderAppearance.forProvider(provider) }
    val bitmap = remember(context, appearance) { AvatarArtwork.providerLogo(context, appearance)?.asImageBitmap() }
    bitmap?.let { Image(it, contentDescription = null, modifier) }
}

@Composable
internal fun ProviderChoiceList(model: AppModel, onPick: (AiProvider) -> Unit) {
    val providers = model.aiProviders.filter { providerAvailability(model, it) != R.string.ai_not_installed }
    if (providers.isEmpty()) {
        Text(stringResource(R.string.ai_no_providers))
        return
    }
    Panel {
        providers.forEachIndexed { index, provider ->
            if (index > 0) RowDivider()
            ListRow(provider.label, hint = stringResource(providerAvailability(model, provider)),
                leading = { ProviderAvatar(provider.id, provider.label, AvatarMood.IDLE, Modifier.size(42.dp), provider = provider) },
                onClick = { onPick(provider) })
        }
    }
}

@Composable
private fun ProviderEditor(model: AppModel, current: AiProvider?, onClose: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(current?.label.orEmpty()) }
    var native by remember { mutableStateOf(current?.packageName != null) }
    var target by remember { mutableStateOf(current?.packageName ?: current?.website.orEmpty()) }
    var color by remember { mutableStateOf(current?.accent?.let { String.format(Locale.ROOT, "%06X", it) }.orEmpty()) }
    var auto by remember { mutableStateOf(current?.autoTrack ?: model.settings.autoTrackLaunches) }
    var image by remember { mutableStateOf<Uri?>(null) }
    var clearLogo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    var removing by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { image = uri; clearLogo = false }
    }
    fun validate(logo: String?): AiProvider? {
        val hex = color.trim().removePrefix("#")
        val accent = if (hex.isEmpty()) null else hex.takeIf { it.length == 6 }?.toIntOrNull(16) ?: return null
        return if (current == null) AiProvider.custom(name, native, target, accent, auto, logo)
        else AiProvider.custom(name, native, target, accent, auto, logo, current.id)
    }
    if (removing && current != null) {
        AlertDialog(onDismissRequest = { removing = false }, title = { Text(stringResource(R.string.ai_remove_provider)) },
            text = { Text(stringResource(R.string.ai_remove_provider_detail, current.label)) },
            confirmButton = { TextButton(onClick = { model.removeAiProvider(current.id); onClose() }) { Text(stringResource(R.string.ai_remove_provider)) } },
            dismissButton = { TextButton(onClick = { removing = false }) { Text(stringResource(android.R.string.cancel)) } })
        return
    }
    AlertDialog(
        onDismissRequest = { if (!saving) onClose() },
        title = { Text(stringResource(if (current == null) R.string.ai_add_provider else R.string.ai_edit_provider)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it.take(64) }, singleLine = true, label = { Text(stringResource(R.string.ai_provider_name)) })
                Row {
                    TextButton(onClick = { if (native) target = ""; native = false }) {
                        Text(stringResource(if (native) R.string.ai_website else R.string.ai_website_selected))
                    }
                    TextButton(onClick = { if (!native) target = ""; native = true }) {
                        Text(stringResource(if (native) R.string.ai_native_selected else R.string.ai_native))
                    }
                }
                OutlinedTextField(target, { target = it.take(2_048) }, singleLine = true,
                    label = { Text(stringResource(if (native) R.string.ai_package_name else R.string.ai_https_url)) })
                OutlinedTextField(color, { color = it.take(7) }, singleLine = true,
                    label = { Text(stringResource(R.string.ai_hex_color)) })
                SettingSwitch(stringResource(R.string.ai_auto_track), null, auto) { auto = it }
                TextButton(onClick = { picker.launch("image/*") }) {
                    Text(stringResource(if (image == null) R.string.ai_logo_choose else R.string.ai_logo_chosen))
                }
                if (image != null || current?.logoToken != null && !clearLogo) {
                    TextButton(onClick = { image = null; clearLogo = true }) { Text(stringResource(R.string.ai_logo_reset)) }
                }
                if (current != null) {
                    TextButton(onClick = { removing = true }, enabled = !saving) { Text(stringResource(R.string.ai_remove_provider)) }
                }
                error?.let { Text(stringResource(it)) }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                val oldToken = current?.logoToken?.takeUnless { clearLogo }
                if (validate(oldToken) == null) { error = R.string.ai_invalid_provider; return@TextButton }
                saving = true
                scope.launch {
                    val selected = image
                    val token = if (selected == null) oldToken else withContext(Dispatchers.IO) { ProviderLogos.import(context, selected) }
                    val provider = validate(token)
                    if (selected != null && token == null) {
                        error = R.string.ai_logo_invalid
                    } else if (provider == null || !model.saveAiProvider(provider)) {
                        if (token != oldToken) ProviderLogos.forget(context, token)
                        error = R.string.ai_provider_duplicate
                    } else {
                        onClose()
                    }
                    saving = false
                }
            }) { Text(stringResource(R.string.ai_save_provider)) }
        },
        dismissButton = { TextButton(enabled = !saving, onClick = onClose) { Text(stringResource(android.R.string.cancel)) } },
    )
}


@Composable
internal fun AiTrackingPage(model: AppModel, onBack: () -> Unit, onOverlay: () -> Unit) {
    val context = LocalContext.current
    val active = model.manualTracking
    val now by trackingClock(active)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Gutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(Gap),
    ) {
        ScreenTitle(stringResource(R.string.ai_quick_title), onBack)
        Text(stringResource(R.string.manual_ai_unconfirmed))
        if (active.isNotEmpty()) {
            SectionTitle(stringResource(R.string.ai_quick_count, active.size))
            Panel {
                active.forEachIndexed { index, session ->
                    if (index > 0) RowDivider()
                    var open by remember(session.id) { mutableStateOf(false) }
                    ListRow(session.provider.label,
                        hint = stringResource(R.string.ai_timer_status,
                            stringResource(if (session.paused) R.string.ai_timer_paused else R.string.manual_ai_tracking),
                            elapsedTracking(session, now)),
                        leading = { ProviderAvatar(session.provider.id, session.provider.label, sessionMood(session),
                            Modifier.size(40.dp), provider = session.provider) },
                        onClick = { model.launchProvider(context, session.provider, track = false) },
                        trailing = {
                            Box {
                                TextButton(onClick = { open = true }) { Text(stringResource(R.string.ai_provider_actions)) }
                                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                                    ManualTimerActions(session, model, close = { open = false })
                                }
                            }
                        })
                }
            }
            if (!model.overlayWished || !model.overlayPermission) {
                PillButton(stringResource(R.string.watch_apps_island), onOverlay, Modifier.fillMaxWidth())
            }
        }
        SectionTitle(stringResource(R.string.ai_start_timer))
        ProviderChoiceList(model) { model.launchProvider(context, it, track = true) }
    }
}
