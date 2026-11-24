package com.coucou.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.core.ManualAiSession
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
                    SettingSwitch(app.label, null, active.any { it.app == app }) { model.setManualTracking(app, it) }
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
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(session.app.label, style = TypeScale.BODY.style(t.text.c()))
                            Text(elapsedTracking(session, now), style = TypeScale.SECONDARY.style(t.textDim.c()))
                        }
                        StopTrackingButton(session) { model.setManualTracking(session.app, false) }
                    }
                }
            }
        }
        if (active.isNotEmpty() && (!model.overlayPermission || !model.overlayWished)) {
            PillButton(stringResource(R.string.watch_apps_island), onOverlay, Modifier.fillMaxWidth())
        }
    }
}

/** A visible panel updates once per displayed minute. There is no background timer or write. */
@Composable
internal fun trackingClock(sessions: List<ManualAiSession>): androidx.compose.runtime.State<Long> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    return produceState(System.currentTimeMillis(), lifecycle, sessions) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                val now = System.currentTimeMillis()
                value = now
                val nextMinute = sessions.minOf { 60_000 - ((now - it.startedAtMs).coerceAtLeast(0) % 60_000) }
                delay(nextMinute)
            }
        }
    }
}

@Composable
internal fun elapsedTracking(session: ManualAiSession, nowMs: Long): String {
    val minutes = session.elapsedMinutes(nowMs)
    return when {
        minutes < 1 -> stringResource(R.string.ago_less_than_minute)
        minutes < 60 -> stringResource(R.string.ago_minutes, minutes)
        else -> stringResource(R.string.ago_hours_minutes, minutes / 60, minutes % 60)
    }
}

@Composable
internal fun StopTrackingButton(session: ManualAiSession, onStop: () -> Unit) {
    val description = stringResource(R.string.ai_stop_provider, session.app.label)
    TextButton(onClick = onStop, modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = description }) {
        Text(stringResource(R.string.chat_stop), color = tokens().text.c())
    }
}
