package com.coucou.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.coucou.android.R
import com.coucou.android.app.AppModel
import com.coucou.android.core.AiActivitySource
import com.coucou.android.core.AiSessionResult
import com.coucou.android.core.WatchedActivity
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

private data class HistoryRow(
    val id: String,
    val providerId: String,
    val providerLabel: String,
    val start: Long?,
    val end: Long,
    val duration: Long?,
    val source: AiActivitySource,
    val status: Int,
    val notification: WatchedActivity? = null,
)

@Composable
internal fun AiSessionHistoryScreen(model: AppModel, onBack: () -> Unit) {
    val entries = model.aiSessionHistory.map {
        HistoryRow(it.id, it.providerId, it.providerLabel, it.startedAtMs, it.endedAtMs, it.durationMs,
            it.source, it.result.labelRes())
    }
    val reported = model.watchedActivity.mapIndexed { i, entry ->
        HistoryRow("report:$i", entry.app.packageName, entry.app.label, null, entry.atMs, null,
            AiActivitySource.NOTIFICATION, entry.event.labelRes(), entry)
    }
    val rows = (entries + reported).sortedByDescending { it.end }
    var provider by remember { mutableStateOf<String?>(null) }
    var days by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf<Int?>(null) }
    var clear by remember { mutableStateOf(false) }
    val zone = ZoneId.systemDefault()
    val earliest = LocalDate.now(zone).minusDays((days - 1).coerceAtLeast(0).toLong()).atStartOfDay(zone).toInstant()
    val visible = rows.filter {
        (provider == null || provider == it.providerId) && (status == null || status == it.status) &&
            (days == 0 || Instant.ofEpochMilli(it.end) >= earliest)
    }
    val all = stringResource(R.string.ai_history_all)
    val dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    LazyColumn(Modifier.padding(horizontal = Gutter), contentPadding = PaddingValues(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ScreenTitle(stringResource(R.string.ai_history), onBack) }
        item {
            Panel {
                HistoryChoice(stringResource(R.string.ai_history_provider), provider,
                    listOf(null to all) + rows.distinctBy { it.providerId }.map { it.providerId to it.providerLabel }) { provider = it }
                HistoryChoice(stringResource(R.string.ai_history_date), days,
                    listOf(0 to all, 1 to stringResource(R.string.ai_history_today), 7 to stringResource(R.string.ai_history_week))) { days = it }
                HistoryChoice(stringResource(R.string.ai_history_status), status,
                    listOf(null to all) + rows.map { it.status }.distinct().map { it to stringResource(it) }) { status = it }
            }
        }
        if (visible.isEmpty()) item { Text(stringResource(R.string.ai_history_empty)) }
        items(visible, key = { it.id }) { row ->
            Panel {
                ListRow(row.providerLabel,
                    hint = stringResource(R.string.ai_timer_status, stringResource(row.source.labelRes()), stringResource(row.status)),
                    trailing = {
                        TextButton(onClick = {
                            val notification = row.notification
                            if (notification != null) model.removeNotificationHistory(notification)
                            else model.clearSessionHistory(row.id)
                        }) { Text(stringResource(R.string.ai_history_delete)) }
                    })
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                    row.start?.let {
                        Text(stringResource(R.string.ai_history_started, dateFormat.format(Date(it))))
                    }
                    Text(stringResource(if (row.start == null) R.string.ai_history_received else R.string.ai_history_ended,
                        dateFormat.format(Date(row.end))))
                    row.duration?.let { Text(stringResource(R.string.ai_history_duration, it / 60_000, it / 1000 % 60)) }
                }
            }
        }
        if (rows.isNotEmpty()) item {
            PillButton(stringResource(R.string.ai_history_clear), { clear = true }, Modifier.fillMaxWidth())
        }
    }
    if (clear) AlertDialog(
        onDismissRequest = { clear = false },
        title = { Text(stringResource(R.string.ai_history_clear)) },
        text = { Text(stringResource(R.string.ai_history_clear_detail)) },
        confirmButton = { TextButton(onClick = { model.clearAllAiHistory(); clear = false }) { Text(stringResource(R.string.ai_history_delete)) } },
        dismissButton = { TextButton(onClick = { clear = false }) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun <T> HistoryChoice(title: String, selected: T, options: List<Pair<T, String>>, onChoose: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ListRow(title, hint = options.firstOrNull { it.first == selected }?.second, onClick = { open = true },
        trailing = {
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(text = { Text(label) }, onClick = { onChoose(value); open = false })
                }
            }
        })
}

private fun AiSessionResult.labelRes(): Int = when (this) {
    AiSessionResult.MANUAL_DONE -> R.string.ai_history_manual_done
    AiSessionResult.CANCELLED -> R.string.ai_history_cancelled
    AiSessionResult.EXPIRED -> R.string.ai_history_expired
    AiSessionResult.UNAVAILABLE -> R.string.ai_history_unavailable
    AiSessionResult.DESKTOP_DONE -> R.string.notif_finished
    AiSessionResult.DESKTOP_ERROR -> R.string.notif_error
}

private fun AiActivitySource.labelRes(): Int = when (this) {
    AiActivitySource.MANUAL -> R.string.manual_ai_title
    AiActivitySource.NOTIFICATION -> R.string.ai_history_source_report
    AiActivitySource.DESKTOP -> R.string.ai_history_source_desktop
    AiActivitySource.PROVIDER -> R.string.ai_history_source_provider
}
