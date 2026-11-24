package com.coucou.android.app

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.coucou.android.R
import com.coucou.android.core.LaunchScreen

/**
 * Shows the manual timer count or desktop connection headline. Tapping opens the in-app session picker.
 * The system requests a refresh when the shade opens. The tile itself does not run a timer.
 */
class GlanceTile : TileService() {
    override fun onStartListening() = paint(this)

    // Below Android 14 only the Intent form exists; lint flags the call although it is behind the version check.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        val open = Intent(this, LaunchActivity::class.java)
            .putExtra(LaunchActivity.EXTRA_SCREEN, LaunchScreen.AI_TRACKING.key)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(open)
        }
    }

    companion object {
        /** Asks the system to refresh the tile if it is on screen (it ignores this when the tile is not added). */
        fun requestUpdate(context: android.content.Context) {
            runCatching { requestListeningState(context, ComponentName(context, GlanceTile::class.java)) }
        }

        private fun paint(tile: TileService) {
            val t = tile.qsTile ?: return
            val model = (tile.applicationContext as CoucouApp).model
            val g = model.glance()
            val count = model.manualTracking.size
            t.label = tile.getString(R.string.app_name)
            t.subtitle = if (count > 0) tile.getString(R.string.ai_quick_count, count) else g.headline
            t.state = if (count > 0 || g.connected) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            t.updateTile()
        }
    }
}
