package com.coucou.android.app

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Process
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Android binds this service after the user grants notification access. */
class WatchNotificationService : NotificationListenerService() {
    private val model get() = (application as CoucouApp).model
    private val seen = LinkedHashMap<String, Int>()
    private val screen by lazy { ScreenWatch(this) { on -> if (!on) model.clearWatchedUpdate() } }

    override fun onListenerConnected() {
        screen.start()
    }

    override fun onListenerDisconnected() {
        clear()
    }

    override fun onDestroy() {
        clear()
        super.onDestroy()
    }

    private fun clear() {
        screen.stop()
        seen.clear()
        model.clearWatchedUpdate()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.user != Process.myUserHandle()) return
        // Check the package and the user's selection before reading notification contents.
        val app = model.watchedApp(sbn.packageName) ?: return
        if (!accessGranted(this)) return
        val notification = sbn.notification
        if (notification.visibility == Notification.VISIBILITY_SECRET ||
            notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        // Only the title is needed. Leave message bodies, actions, and extras from other apps alone.
        val title = runCatching {
            com.coucou.android.core.WatchedAiApp.shortTitle(notification.extras.getCharSequence(Notification.EXTRA_TITLE))
        }.getOrDefault("")
        if (seen.put(sbn.key, title.hashCode()) == title.hashCode()) return
        if (seen.size > 64) seen.remove(seen.keys.first())
        model.showWatchedUpdate(app, title)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        seen.remove(sbn.key)
    }

    companion object {
        fun component(context: Context) = ComponentName(context, WatchNotificationService::class.java)

        fun accessGranted(context: Context): Boolean =
            context.getSystemService(NotificationManager::class.java)?.isNotificationListenerAccessGranted(component(context)) == true
    }
}
