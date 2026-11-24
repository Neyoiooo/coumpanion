package com.coucou.android.app

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import com.coucou.android.core.SessionTime

internal class SessionClock(context: Context) {
    private val bootCount = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1) }
        .getOrDefault(-1)

    fun now() = SessionTime(System.currentTimeMillis(), SystemClock.elapsedRealtime(), bootCount)
}
