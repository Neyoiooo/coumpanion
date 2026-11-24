package com.coucou.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.core.TypeScale

@Composable
internal fun ManualTrackingPanel(model: AppModel, onOverlay: () -> Unit, showPicker: Boolean = true) {
    val active = model.manualTracking
    val t = tokens()
    if (model.installedWatchApps.isEmpty() && active == null) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.manual_ai_title), style = TypeScale.BODY.style(t.text.c()))
        Text(stringResource(R.string.manual_ai_unconfirmed), style = TypeScale.SECONDARY.style(t.textDim.c()))
        if (showPicker) {
            Panel {
                model.installedWatchApps.forEachIndexed { index, app ->
                    if (index > 0) RowDivider()
                    SettingSwitch(app.label, null, active?.app == app) { model.setManualTracking(app, it) }
                }
            }
        } else if (active != null) {
            Text(active.app.label, style = TypeScale.BODY.style(t.text.c()))
            PillButton(stringResource(R.string.chat_stop), { model.setManualTracking(active.app, false) }, Modifier.fillMaxWidth())
        }
        if (active != null && (!model.overlayPermission || !model.overlayWished)) {
            PillButton(stringResource(R.string.watch_apps_island), onOverlay, Modifier.fillMaxWidth())
        }
    }
}
