package com.coucou.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.core.TypeScale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The notification log is independent of overlay permission and desktop pairing. */
@Composable
internal fun WatchedActivityPanel(model: AppModel, limit: Int = 10) {
    val activity = model.watchedActivity
    if (activity.isEmpty()) return
    val t = tokens()
    val format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.watch_apps_title) + " · " + stringResource(R.string.history_title),
                Modifier.weight(1f), style = TypeScale.BODY.style(t.text.c()))
            TextButton(onClick = model::clearWatchedActivity, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.history_clear))
            }
        }
        Panel {
            activity.take(limit).forEachIndexed { index, entry ->
                if (index > 0) RowDivider()
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .clickable(role = Role.Button) { model.openWatchedApp(entry.app.packageName) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(entry.app.label, style = TypeScale.BODY.style(t.text.c()))
                        Text(stringResource(R.string.watch_apps_update), style = TypeScale.SECONDARY.style(t.textDim.c()))
                    }
                    Text(format.format(Instant.ofEpochMilli(entry.atMs)), style = TypeScale.SECONDARY.style(t.textDim.c()))
                }
            }
        }
    }
}
