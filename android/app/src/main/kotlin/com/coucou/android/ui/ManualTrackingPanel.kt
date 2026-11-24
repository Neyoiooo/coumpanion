package com.coucou.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.app.SessionClock
import com.coucou.android.core.AvatarMood
import com.coucou.android.core.ManualAiSession
import com.coucou.android.core.SessionTime
import com.coucou.android.core.WatchedAiApp
import com.coucou.android.core.TypeScale
import kotlinx.coroutines.delay

@Composable
internal fun ManualTrackingPanel(model: AppModel, onOverlay: () -> Unit, showPicker: Boolean = true) {
    val active = model.manualTracking
    val t = tokens()
    if (model.installedWatchApps.isEmpty() && active.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.manual_ai_title), style = TypeScale.BODY.style(t.text.c()))
        Text(stringResource(R.string.manual_ai_unconfirmed), style = TypeScale.SECONDARY.style(t.textDim.c()))
        if (showPicker) {
            Panel {
                model.installedWatchApps.forEachIndexed { index, app ->
                    if (index > 0) RowDivider()
                    val tracking = active.any { it.provider.id == app.packageName }
                    ProviderSwitch(app, tracking, if (tracking) AvatarMood.TRACKING else AvatarMood.IDLE) {
                        model.setManualTracking(app, it)
                    }
                }
            }
        } else if (active.isNotEmpty()) {
            val now by trackingClock(active)
            Panel {
                active.forEachIndexed { index, session ->
                    if (index > 0) RowDivider()
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ProviderAvatar(session.provider.id, session.provider.label, sessionMood(session), Modifier.size(44.dp), provider = session.provider)
                        Column(Modifier.weight(1f)) {
                            Text(session.provider.label, style = TypeScale.BODY.style(t.text.c()))
                            Text(stringResource(R.string.ai_timer_status, stringResource(if (session.paused) R.string.ai_timer_paused else R.string.manual_ai_tracking), elapsedTracking(session, now)), style = TypeScale.SECONDARY.style(t.textDim.c()))
                        }
                        StopTrackingButton(session) { model.setManualTracking(session.provider.id, false) }
                    }
                }
            }
        }
        if (active.isNotEmpty() && (!model.overlayPermission || !model.overlayWished)) {
            PillButton(stringResource(R.string.watch_apps_island), onOverlay, Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun ProviderSwitch(app: WatchedAiApp, checked: Boolean, mood: AvatarMood, onChange: (Boolean) -> Unit) {
    ListRow(
        app.label,
        leading = { ProviderAvatar(app.packageName, app.label, mood, Modifier.size(42.dp)) },
        trailing = { CoucouSwitch(checked, onChange, label = app.label) },
    )
}

/** A visible panel updates once per displayed minute. There is no background timer or write. */
@Composable
internal fun trackingClock(sessions: List<ManualAiSession>): androidx.compose.runtime.State<SessionTime> {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val visible = lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val context = LocalContext.current
    val clock = remember(context) { SessionClock(context) }
    return produceState(clock.now(), visible, sessions) {
        value = clock.now()
        val running = sessions.filterNot { it.paused }
        if (!visible || running.isEmpty()) return@produceState
        while (true) {
            val now = clock.now()
            value = now
            delay(running.minOf { 60_000 - (it.elapsed(now) % 60_000) })
        }
    }
}

@Composable
internal fun elapsedTracking(session: ManualAiSession, now: SessionTime): String {
    val minutes = session.elapsed(now) / 60_000
    return when {
        minutes < 1 -> stringResource(R.string.ai_duration_under_minute)
        minutes < 60 -> stringResource(R.string.ai_duration_minutes, minutes)
        else -> stringResource(R.string.ai_duration_hours, minutes / 60, minutes % 60)
    }
}

@Composable
internal fun StopTrackingButton(session: ManualAiSession, onStop: () -> Unit) {
    val description = stringResource(R.string.ai_stop_provider, session.provider.label)
    TextButton(onClick = onStop, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = description }) {
        Text(stringResource(R.string.chat_stop), color = tokens().text.c())
    }
}
