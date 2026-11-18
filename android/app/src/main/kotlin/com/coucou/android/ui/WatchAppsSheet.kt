package com.coucou.android.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.core.Tokens
import com.coucou.android.core.TypeScale
import com.coucou.android.core.WatchedAiApp

@Composable
internal fun WatchAppsSheet(model: AppModel, onDismiss: () -> Unit, onAccess: () -> Unit, onOverlay: () -> Unit) {
    val title = stringResource(R.string.watch_apps_title)
    val hasSelection = model.installedWatchApps.any { it.packageName in model.watchedPackages }
    CompositionLocalProvider(LocalTokens provides Tokens.DARK) {
        val t = tokens()
        BottomSheetHost(onDismiss, announce = title) {
            Text(title, style = TypeScale.TITLE.style(t.heroText.c()))
            val hint = when {
                model.installedWatchApps.isEmpty() -> R.string.watch_apps_empty
                model.watchAccess -> R.string.watch_apps_intro
                else -> R.string.watch_apps_privacy
            }
            Text(stringResource(hint), Modifier.padding(top = 6.dp, bottom = 14.dp), style = TypeScale.SECONDARY.style(t.heroTextDim.c()))
            if (model.installedWatchApps.isEmpty()) {
                Text(WatchedAiApp.entries.joinToString(" · ") { it.label }, style = TypeScale.BODY.style(t.heroText.c()))
            } else {
                Panel {
                    model.installedWatchApps.forEachIndexed { index, app ->
                        if (index > 0) RowDivider()
                        SettingSwitch(app.label, null, app.packageName in model.watchedPackages) { model.setWatched(app, it) }
                    }
                }
            }
            Spacer(Modifier.size(16.dp))
            when {
                !hasSelection -> Unit
                !model.watchAccess -> PillButton(
                    stringResource(R.string.watch_apps_allow), onAccess, Modifier.fillMaxWidth(), PillKind.PRIMARY, onDark = true,
                )
                !model.overlayPermission || !model.overlayWished -> PillButton(
                    stringResource(R.string.watch_apps_island), onOverlay, Modifier.fillMaxWidth(), PillKind.PRIMARY, onDark = true,
                )
                else -> Text(stringResource(R.string.watch_apps_ready), style = TypeScale.BODY.style(t.heroText.c()))
            }
            Spacer(Modifier.size(8.dp))
            PillButton(stringResource(R.string.action_close), onDismiss, Modifier.fillMaxWidth(), onDark = true)
        }
    }
}
