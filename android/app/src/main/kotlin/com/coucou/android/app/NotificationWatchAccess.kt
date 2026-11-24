package com.coucou.android.app

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import com.coucou.android.BuildConfig

/** Shared UI can query availability without compiling the listener into Standard. */
object NotificationWatchAccess {
    val available get() = BuildConfig.NOTIFICATION_WATCH

    fun component(context: Context) = ComponentName(context, "com.coucou.android.app.WatchNotificationService")

    fun granted(context: Context): Boolean = available &&
        context.getSystemService(NotificationManager::class.java)?.isNotificationListenerAccessGranted(component(context)) == true
}
